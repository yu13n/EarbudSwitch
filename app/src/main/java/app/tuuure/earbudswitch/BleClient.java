package app.tuuure.earbudswitch;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
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

class BleClient {
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
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    ProfileManager.connect(mContext, bluetoothDevice);
                    for (BluetoothDevice device : BluetoothAdapter.getDefaultAdapter().getBondedDevices()) {
                        if (BluetoothClass.Device.Major.AUDIO_VIDEO != device.getBluetoothClass().getMajorDeviceClass()
                                || device.getName() == null || device.getName().isEmpty())
                            continue;
                        if (!device.getAddress().equals(bluetoothDevice.getAddress())) {
                            int i, min = Math.min(device.getName().length(), bluetoothDevice.getName().length());
                            for (i = 0; i < min; i++) {
                                if (device.getName().charAt(i) != bluetoothDevice.getName().charAt(i)) {
                                    break;
                                }
                            }
                            if (Math.max(device.getName().length(), bluetoothDevice.getName().length()) <= 3 + i) {
                                ProfileManager.connect(mContext, device);
                            }
                        }
                    }

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
            ProfileManager.disconnect(mContext, null); //Disconnect any connected device
        }
    };

}
