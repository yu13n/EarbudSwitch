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

import static app.tuuure.earbudswitch.ConvertUtils.md5code32;
import static app.tuuure.earbudswitch.ConvertUtils.uuidToBytes;

public class BleServer {
    public static final String TAG = "BleServer";

    private Context mContext;
    private BluetoothAdapter bluetoothAdapter;
    private UUID deviceUUID;
    private UUID saltUUID;
    private byte[] authCode;
    private BluetoothDevice bluetoothDevice;
    private BluetoothLeAdvertiser bluetoothLeAdvertiser;
    private BluetoothGattServer gattServer;

    private A2dpManager a2dpManager;

    BleServer(Context context, BluetoothDevice device, BluetoothAdapter adapter, A2dpManager manager) {
        mContext = context;
        bluetoothAdapter = adapter;
        a2dpManager = manager;
        bluetoothDevice = device;
        deviceUUID = md5code32(bluetoothDevice.getAddress());
        Log.d(TAG, "deviceUUID: " + deviceUUID.toString());
        saltUUID = UUID.randomUUID();
        Log.d(TAG, "saltUUID: " + saltUUID.toString());
        String auth = mContext.getSharedPreferences(mContext.getString(R.string.app_title), Context.MODE_PRIVATE).getString("key", "114514");
        Log.d(TAG, "auth: " + auth);
        authCode = uuidToBytes(md5code32(saltUUID.toString() + auth));
        openGattServer();
        advertise();
    }

    //广播Callback
    private final AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            Log.d(TAG, "advertising");
        }

        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            Log.d(TAG, "advertise error: " + errorCode);
        }
    };

    void advertise() {
        bluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
        AdvertiseSettings advertiseSettings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setConnectable(true)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_LOW)
                .build();
        AdvertiseData advertiseData = new AdvertiseData.Builder()
                .addServiceUuid(new ParcelUuid(deviceUUID))
                .setIncludeDeviceName(true)
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
            gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, uuidToBytes(saltUUID));
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
        }

        //特征值写入回调
        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            if (Arrays.equals(value, authCode)) {
                //验证通过，则断开耳机
                a2dpManager.disconnect(bluetoothDevice);
            }
            if (responseNeeded) {
                gattServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        null);
            }
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
        }
    };

    void openGattServer() {
        BluetoothGattService gattService = new BluetoothGattService(deviceUUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);
        BluetoothGattCharacteristic gattCharacteristic = new BluetoothGattCharacteristic(deviceUUID,
                BluetoothGattCharacteristic.PROPERTY_READ |
                        BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ |
                        BluetoothGattCharacteristic.PERMISSION_WRITE);

        gattService.addCharacteristic(gattCharacteristic);

        gattServer = ((BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE))
                .openGattServer(mContext, gattServerCallback);
        boolean result = gattServer.addService(gattService);
    }

    void stopGattServer() {
        gattServer.close();
    }
}
