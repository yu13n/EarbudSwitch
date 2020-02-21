package app.tuuure.earbudswitch;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
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
import android.util.Log;

import java.util.Arrays;
import java.util.UUID;

import static app.tuuure.earbudswitch.ConvertUtils.*;

class BleServer {
    private static final String TAG = "BleServer";

    private Context mContext;
    private BluetoothAdapter bluetoothAdapter;
    private UUID deviceUUID;
    private UUID saltUUID;
    private byte[] authCode;
    private BluetoothDevice bluetoothDevice;
    private BluetoothLeAdvertiser bluetoothLeAdvertiser;
    private BluetoothGattServer gattServer;

    BleServer(Context context, BluetoothDevice device) {
        mContext = context;
        bluetoothDevice = device;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        deviceUUID = UUID.fromString(md5code32(bluetoothDevice.getAddress()));
        Log.d(TAG, "deviceUUID: " + deviceUUID.toString());
        saltUUID = UUID.randomUUID();
        Log.d(TAG, "saltUUID: " + saltUUID.toString());
        String authKey = mContext.getSharedPreferences(mContext.getString(R.string.app_title), Context.MODE_PRIVATE).getString("key", "114514");
        authCode = hmacMD5(saltUUID.toString(), authKey);
        openGattServer();
        advertise();
    }

    //广播Callback
    private final AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
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

    private void advertise() {
        bluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
        AdvertiseSettings advertiseSettings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)
                .setConnectable(true)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_ULTRA_LOW)
                .build();
        AdvertiseData advertiseData = new AdvertiseData.Builder()
                .addServiceUuid(new ParcelUuid(deviceUUID))
                .build();
        bluetoothLeAdvertiser.startAdvertising(advertiseSettings, advertiseData, advertiseCallback);
    }

    void stopAdvertise() {
        if (bluetoothLeAdvertiser != null) {
            bluetoothLeAdvertiser.stopAdvertising(advertiseCallback);
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
            Log.d("Receive", bytesToUUID(value).toString());
            Log.d("Receive", bytesToUUID(authCode).toString());
            if (Arrays.equals(value, authCode)) {
                //验证通过，则断开耳机
                Log.d(TAG, "Autherized. Earbuds Disconnecting...");

                ProfileManager.disconnect(mContext, bluetoothDevice);
            }
            if (responseNeeded) {
                gattServer.sendResponse(
                        device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        null
                );
            }
            gattServer.cancelConnection(device);
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
        }
    };

    private void openGattServer() {
        BluetoothGattService gattService = new BluetoothGattService(deviceUUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);
        BluetoothGattCharacteristic gattCharacteristic = new BluetoothGattCharacteristic(deviceUUID,
                BluetoothGattCharacteristic.PROPERTY_READ |
                        BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ |
                        BluetoothGattCharacteristic.PERMISSION_WRITE);

        gattService.addCharacteristic(gattCharacteristic);
        BluetoothManager bluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        gattServer = bluetoothManager.openGattServer(mContext, gattServerCallback);
        boolean result = gattServer.addService(gattService);
    }

    void stopGattServer() {
        gattServer.close();
    }
}
