package app.tuuure.earbudswitch;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static app.tuuure.earbudswitch.ConvertUtils.*;
import static java.lang.Class.forName;

class BleClient {
    private static final String TAG = "BleClient";

    private Context mContext;
    private BluetoothDevice bluetoothDevice;
    private UUID deviceUUID;
    private String authKey;
    private BluetoothGattService bluetoothGattService;
    private boolean aggressiveMode;
    BluetoothGatt bluetoothGatt;


    BleClient(Context context, BluetoothDevice targetDevice) {
        mContext = context;
        authKey = mContext.getSharedPreferences(mContext.getString(R.string.app_title), Context.MODE_PRIVATE).getString("key", "114514");
        aggressiveMode = mContext.getSharedPreferences(mContext.getString(R.string.app_title), Context.MODE_PRIVATE).getBoolean("aggressive", true);
        bluetoothDevice = targetDevice;
        deviceUUID = UUID.nameUUIDFromBytes(bluetoothDevice.getAddress().getBytes(StandardCharsets.UTF_8));
    }


    BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    bluetoothGatt.discoverServices();
                }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                bluetoothGattService = bluetoothGatt.getService(deviceUUID);
                BluetoothGattCharacteristic gattCharacteristic = bluetoothGattService.getCharacteristic(deviceUUID);
                bluetoothGatt.readCharacteristic(gattCharacteristic);

                bluetoothGatt.setCharacteristicNotification(gattCharacteristic, true);
                BluetoothGattDescriptor descriptor = gattCharacteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                gatt.writeDescriptor(descriptor);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            UUID saltUUID = bytesToUUID(characteristic.getValue());

            byte[] authCode = hmacMD5(saltUUID.toString(), authKey);
            characteristic.setValue(authCode);
            bluetoothGatt.writeCharacteristic(characteristic);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.d(TAG, "Characteristic Writed");
            super.onCharacteristicWrite(gatt, characteristic, status);
            ProfileManager.disconnect(mContext, null); //Disconnect any connected device
            if (aggressiveMode)
                connectDevice();
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            if (!aggressiveMode)
                connectDevice();
            bluetoothGatt.disconnect();
            bluetoothGatt.close();
            super.onCharacteristicChanged(gatt, characteristic);
        }
    };

    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED.equals(action)) {
                int state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1);
                if (state == BluetoothProfile.STATE_CONNECTED) {
                    final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

                    BluetoothProfile.ServiceListener proxyListener = new BluetoothProfile.ServiceListener() {
                        @Override
                        public void onServiceDisconnected(int profile) {
                        }

                        @Override
                        public void onServiceConnected(int profile, BluetoothProfile proxy) {
                            if (profile != BluetoothProfile.A2DP) {
                                return;
                            }
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                                bluetoothAdapter.closeProfileProxy(profile, proxy);
                                return;
                            }
                            for (BluetoothDevice device : proxy.getConnectedDevices()) {
                                /*
                                    public static final int SOURCE_CODEC_TYPE_SBC = 0;
                                    public static final int SOURCE_CODEC_TYPE_AAC = 1;
                                    public static final int SOURCE_CODEC_TYPE_APTX = 2;
                                    public static final int SOURCE_CODEC_TYPE_APTX_HD = 3;
                                    public static final int SOURCE_CODEC_TYPE_LDAC = 4;
                                    public static final int SOURCE_CODEC_TYPE_MAX = 5;

                                    public static final int CHANNEL_MODE_NONE = 0;
                                    public static final int CHANNEL_MODE_MONO = 0x1 << 0;
                                    public static final int CHANNEL_MODE_STEREO = 0x1 << 1;
                                 */
                                try {
                                    //通过反射获取BluetoothA2dp中connect方法（hide的），进行连接。
                                    Class<?> BluetoothCodecStatus = (Class<?>) forName("android.bluetooth.BluetoothCodecStatus");
                                    Class<?> BluetoothCodecConfig = (Class<?>) forName("android.bluetooth.BluetoothCodecConfig");

                                    Method method = BluetoothA2dp.class.getDeclaredMethod("getCodecStatus", BluetoothDevice.class);
                                    Object codecStatus = method.invoke(proxy, device);
                                    Method getCodecConfig = BluetoothCodecStatus.getDeclaredMethod("getCodecConfig");
                                    Object codecConfig = getCodecConfig.invoke(codecStatus);
                                    Method getCodecTypegetCodecType = BluetoothCodecConfig.getDeclaredMethod("getCodecType");
                                    int codecType = (int) getCodecTypegetCodecType.invoke(codecConfig);
                                    Method getChannelMode = BluetoothCodecConfig.getDeclaredMethod("getChannelMode");
                                    int channelMode = (int) getChannelMode.invoke(codecConfig);

                                    Field CHANNEL_MODE_MONO = BluetoothCodecConfig.getDeclaredField("CHANNEL_MODE_MONO");
                                    if (channelMode != CHANNEL_MODE_MONO.getInt(codecConfig)) {
                                        bluetoothAdapter.closeProfileProxy(profile, proxy);
                                        return;
                                    }
                                    Field SOURCE_CODEC_TYPE_APTX_TWSP = null;
                                    for (Field field : BluetoothCodecConfig.getDeclaredFields()) {
                                        String name = field.getName();
                                        if (name.contains("APTX") && name.contains("TWS")) {
                                            SOURCE_CODEC_TYPE_APTX_TWSP = field;
                                            break;
                                        }
                                    }
                                    if (SOURCE_CODEC_TYPE_APTX_TWSP == null || (int) SOURCE_CODEC_TYPE_APTX_TWSP.get(codecConfig) != codecType) {
                                        bluetoothAdapter.closeProfileProxy(profile, proxy);
                                        return;
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    bluetoothAdapter.closeProfileProxy(profile, proxy);
                                    return;
                                }
                            }
                            bluetoothAdapter.closeProfileProxy(profile, proxy);
                            twsConnect();
                        }
                    };
                    mContext.unregisterReceiver(receiver);
                    bluetoothAdapter.getProfileProxy(mContext, proxyListener, BluetoothProfile.A2DP);
                }
            }
        }
    };

    private void connectDevice() {
        //注册监听蓝牙状态的receiver
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
        mContext.registerReceiver(receiver, intentFilter);

        ProfileManager.connect(mContext, bluetoothDevice);
    }

    private void twsConnect() {
        final String origin = bluetoothDevice.getName().toLowerCase().replaceAll("[lr ]", "");
        for (BluetoothDevice device : BluetoothAdapter.getDefaultAdapter().getBondedDevices()) {
            if (!device.getBluetoothClass().hasService(BluetoothClass.Service.AUDIO)
                    || device.getName() == null || device.getName().isEmpty())
                continue;
            if (!device.getAddress().equals(bluetoothDevice.getAddress())) {
                if (origin.equals(device.getName().toLowerCase().replaceAll("[lr ]", "")))
                    ProfileManager.connect(mContext, device);
            }
        }
    }
}
