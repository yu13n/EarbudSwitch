package app.tuuure.earbudswitch;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.os.ParcelUuid;
import android.util.ArraySet;
import android.util.Log;
import android.widget.Toast;

import com.microsoft.appcenter.analytics.Analytics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import app.tuuure.earbudswitch.Utils.ProfileManager;
import app.tuuure.earbudswitch.Utils.SharedPreferencesUtils;
import app.tuuure.earbudswitch.Utils.TwsUtils;

import static app.tuuure.earbudswitch.Utils.ConvertUtils.hmacMD5;
import static app.tuuure.earbudswitch.Utils.ConvertUtils.md5code32;
import static app.tuuure.earbudswitch.Utils.ConvertUtils.uuidToBytes;

class BleServer {
    private static final String TAG = "BleServer";

    private Context mContext;
    private BluetoothAdapter bluetoothAdapter;
    private UUID saltUUID;
    private ArrayList<AdvertiseCallback> callbacks = new ArrayList<>(2);
    private Set<BluetoothDevice> devices = new ArraySet<>(2);
    private byte[] authCode;
    private BluetoothLeAdvertiser bluetoothLeAdvertiser = null;
    private BluetoothGattServer gattServer;

    BleServer(Context context) {
        mContext = context;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        saltUUID = UUID.randomUUID();
        Log.d(TAG, "saltUUID: " + saltUUID.toString());
        String authKey = SharedPreferencesUtils.getInstance().getKey();
        authCode = hmacMD5(uuidToBytes(saltUUID), authKey);
        openGattServer();
    }

    private void openGattServer() {
        BluetoothManager bluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        gattServer = bluetoothManager.openGattServer(mContext, gattServerCallback);
    }

    private ConcurrentLinkedQueue<BluetoothDevice> queue = new ConcurrentLinkedQueue<>();

    void addDevice(BluetoothDevice device) {
        devices.add(device);

        if (devices.size() == 2) {
            TwsUtils.isTWS(mContext, devices, new TwsUtils.Callback() {
                @Override
                public void notice() {
                    TwsUtils.putTWS(devices);
                }
            });
        }

        boolean isEmpty = queue.isEmpty();
        queue.offer(device);
        if (isEmpty) {
            stopAdvertise();
            addService(device);
        }
    }

    private void addService(BluetoothDevice device) {
        if (device == null) {
            advertise();
            return;
        }
        UUID uuid = md5code32(device.getAddress());
        BluetoothGattService gattService = new BluetoothGattService(uuid, BluetoothGattService.SERVICE_TYPE_PRIMARY);
        BluetoothGattCharacteristic gattCharacteristic = new BluetoothGattCharacteristic(uuid,
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
        BluetoothGattDescriptor descriptor = new BluetoothGattDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"), BluetoothGattDescriptor.PERMISSION_WRITE);
        gattCharacteristic.addDescriptor(descriptor);
        gattService.addCharacteristic(gattCharacteristic);
        gattServer.addService(gattService);
    }

    private void advertise() {
        bluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
        AdvertiseSettings advertiseSettings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)
                .setConnectable(true)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_LOW)
                .build();

        if (!devices.isEmpty()) {
            for (BluetoothDevice device : devices) {
                if (callbacks.size() == 1 && !bluetoothAdapter.isMultipleAdvertisementSupported()) {
                    Toast.makeText(mContext, "不支持多广播", Toast.LENGTH_SHORT).show();
                    Analytics.trackEvent("Not Support MultipleAdvertisement");
                    break;
                }

                AdvertiseData advertiseData = new AdvertiseData.Builder()
                        .setIncludeDeviceName(false)
                        .setIncludeTxPowerLevel(false)
                        .addServiceUuid(new ParcelUuid(md5code32(device.getAddress())))
                        //.addManufacturerData(0xeb55,)
                        .build();

                AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
                    @Override
                    public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                        Log.d(TAG, "advertising");
                        super.onStartSuccess(settingsInEffect);
                    }

                    @Override
                    public void onStartFailure(int errorCode) {
                        Log.d(TAG, "advertise error: " + errorCode);
                        super.onStartFailure(errorCode);
                    }
                };

                bluetoothLeAdvertiser.startAdvertising(advertiseSettings, advertiseData, advertiseCallback);
                callbacks.add(advertiseCallback);
            }
        }
    }

    void stopAdvertise() {
        if (bluetoothLeAdvertiser != null) {
            if (!callbacks.isEmpty()) {
                for (AdvertiseCallback callback : callbacks) {
                    bluetoothLeAdvertiser.stopAdvertising(callback);
                }
                callbacks.clear();
            }
            bluetoothLeAdvertiser = null;
        }
    }

    private BluetoothDevice clientDevice;
    private BluetoothGattCharacteristic clientCharacteristic;

    void disconnectNotify() {
        if (clientDevice != null && clientCharacteristic != null) {
            clientCharacteristic.setValue("");
            gattServer.notifyCharacteristicChanged(clientDevice, clientCharacteristic, false);
            gattServer.cancelConnection(clientDevice);
        }
    }

    private BluetoothGattServerCallback gattServerCallback = new BluetoothGattServerCallback() {
        //设备连接/断开连接回调
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            super.onConnectionStateChange(device, status, newState);
        }

        //添加本地服务回调
        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            super.onServiceAdded(status, service);
            queue.poll();
            addService(queue.peek());
        }

        //特征值读取回调
        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            Log.d(TAG, "onCharacteristicReadRequest");
            gattServer.sendResponse(
                    device,
                    requestId,
                    BluetoothGatt.GATT_SUCCESS,
                    0,
                    uuidToBytes(saltUUID)
            );
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
        }

        //特征值写入回调
        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            Log.d(TAG, "onCharacteristicWriteRequest");

            if (responseNeeded) {
                gattServer.sendResponse(
                        device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        null
                );
            }

            if (Arrays.equals(authCode, value)) {
                //验证通过，则断开耳机
                Log.d(TAG, "Autherized. Earbuds Disconnecting...");
                clientDevice = device;
                clientCharacteristic = characteristic;
                ProfileManager.disconnect(mContext, null);
            } else {
                gattServer.cancelConnection(device);
            }

            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
        }

        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            if (responseNeeded) {
                gattServer.sendResponse(
                        device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        null
                );
            }
            super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);
        }
    };

    void stopGattServer() {
        gattServer.close();
    }
}
