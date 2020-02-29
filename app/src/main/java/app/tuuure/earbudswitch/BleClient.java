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
import java.util.UUID;

import app.tuuure.earbudswitch.Utils.ProfileManager;
import app.tuuure.earbudswitch.Utils.SharedPreferencesUtils;
import app.tuuure.earbudswitch.Utils.TwsUtils;

import static app.tuuure.earbudswitch.Utils.ConvertUtils.*;
import static java.lang.Class.forName;

class BleClient {
    private static final String TAG = "BleClient";

    private Context mContext;
    private BluetoothDevice bluetoothDevice;
    private UUID deviceUUID;
    private String authKey;
    private BluetoothGattService bluetoothGattService;
    private boolean aggressiveMode;
    private BluetoothAdapter bluetoothAdapter;
    BluetoothGatt bluetoothGatt;


    BleClient(Context context, BluetoothDevice targetDevice) {
        mContext = context;
        authKey = SharedPreferencesUtils.getInstance().getKey();
        aggressiveMode = SharedPreferencesUtils.getInstance().getAggre();
        bluetoothDevice = targetDevice;
        deviceUUID = md5code32(bluetoothDevice.getAddress());
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
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
                    TwsUtils.isTWS(mContext, new String[]{bluetoothDevice.getAddress()}, new TwsUtils.Callback() {
                        @Override
                        public void notice() {
                            String macAddress = TwsUtils.getTWS(bluetoothDevice.getAddress());
                            if (!macAddress.isEmpty()) {
                                ProfileManager.connect(mContext, bluetoothAdapter.getRemoteDevice(macAddress));
                            }
                        }
                    });
                }
                mContext.unregisterReceiver(receiver);
            }
        }
    };

    private void connectDevice() {
        if (bluetoothAdapter.isMultipleAdvertisementSupported()) {
            // 可能是TWS+
            if (TwsUtils.isTWSContain(bluetoothDevice.getAddress())) {
                //目标设备是TWS+设备
                String macAddress = TwsUtils.getTWS(bluetoothDevice.getAddress());
                if (!macAddress.isEmpty()) {
                    ProfileManager.connect(mContext, bluetoothAdapter.getRemoteDevice(macAddress));
                }
            } else {
                //未知设备，注册广播，等待连接后检测
                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
                mContext.registerReceiver(receiver, intentFilter);
            }
        }

        ProfileManager.connect(mContext, bluetoothDevice);
    }
}
