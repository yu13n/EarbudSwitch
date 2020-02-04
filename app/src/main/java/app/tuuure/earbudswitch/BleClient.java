package app.tuuure.earbudswitch;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

import java.util.UUID;

import static app.tuuure.earbudswitch.ConvertUtils.*;

public class BleClient {
    static final String TAG = "BleClient";

    private Context mContext;
    private BluetoothDevice bluetoothDevice;
    private UUID deviceUUID;
    private String authKey;
    private BluetoothGattService bluetoothGattService;
    BluetoothGatt bluetoothGatt;


    BleClient(Context context, BluetoothDevice targetDevice) {
        mContext = context;
        authKey = mContext.getSharedPreferences(mContext.getString(R.string.app_title), Context.MODE_PRIVATE).getString("key", "114514");
        Log.d(TAG, authKey);
        bluetoothDevice = targetDevice;
        deviceUUID = UUID.fromString(md5code32(bluetoothDevice.getAddress()));
        Log.d(TAG, deviceUUID.toString());
    }


    BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            Log.d(TAG, "StateChange " + status + "_" + newState);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    bluetoothGatt.discoverServices();
                }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            Log.d(TAG, "Connected");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                bluetoothGattService = bluetoothGatt.getService(deviceUUID);
                BluetoothGattCharacteristic gattCharacteristic = bluetoothGattService.getCharacteristic(deviceUUID);
                bluetoothGatt.readCharacteristic(gattCharacteristic);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            UUID saltUUID = bytesToUUID(characteristic.getValue());
            Log.d(TAG, "Read" + saltUUID.toString());

            byte[] authCode = hmacMD5(saltUUID.toString(), authKey);
            characteristic.setValue(authCode);
            Log.d(TAG, "Write" + (bluetoothGatt.writeCharacteristic(characteristic) ? "success" : "fail"));
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.d(TAG, "Characteristic Writed");
            super.onCharacteristicWrite(gatt, characteristic, status);
            ProfileManager.connect(mContext,bluetoothDevice);
        }
    };

}
