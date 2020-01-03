package app.tuuure.earbudswitch;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.ParcelUuid;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static app.tuuure.earbudswitch.ConvertUtils.md5code32;
import static app.tuuure.earbudswitch.ConvertUtils.uuidToBytes;

public class BleClient {
    public static final String TAG = "BleClient";

    private Context mContext;
    BluetoothManager bluetoothManager;
    BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice bluetoothDevice;
    private UUID deviceUUID;
    private String auth;
    private BluetoothLeScanner bluetoothLeScanner;
    private BluetoothGattService bluetoothGattService;
    private BluetoothGatt bluetoothGatt;
    private A2dpManager a2dpManager;

    Boolean isSetupNeeded = true;

    public BleClient(Context context) {
        mContext = context;
        initBluetooth();
        auth = "";  //TODO: acquire from sharepreference
    }

    void initBluetooth(){
        if(mContext.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            //初始化蓝牙服务
            bluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
            if(bluetoothManager != null && mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)){
                bluetoothAdapter = bluetoothManager.getAdapter();
                if(bluetoothAdapter.isEnabled()){
                    //a2dpManager = new A2dpManager(mContext, bluetoothAdapter);
                    isSetupNeeded = false;
                }
            }
        }
    }

    void setTargetDevice(BluetoothDevice device){
        bluetoothDevice = device;
        String deviceName = bluetoothDevice.getName();
        if (deviceName == null) {
            deviceUUID = md5code32(bluetoothDevice.getAddress());
        } else {
            deviceUUID = md5code32(bluetoothDevice.getName() + bluetoothDevice.getAddress());
        }
    }

    private BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            Log.d(TAG,"StateChange " + status +"_"+newState);
            if(status == BluetoothGatt.GATT_SUCCESS){
                if(newState == BluetoothProfile.STATE_CONNECTED){
                    bluetoothGatt.discoverServices();
                }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            Log.d("TEST","Connected");
            if(status == BluetoothGatt.GATT_SUCCESS){
                bluetoothGattService = bluetoothGatt.getService(deviceUUID);
                BluetoothGattCharacteristic gattCharacteristic = bluetoothGattService.getCharacteristic(deviceUUID);
                bluetoothGatt.readCharacteristic(gattCharacteristic);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            Log.d("TEST","Read");
            UUID saltUUID = UUID.nameUUIDFromBytes(characteristic.getValue());
            Log.d("Read", saltUUID.toString());
            Log.d(TAG,"Read complete");

            byte[] authCode = uuidToBytes(md5code32(saltUUID.toString() + auth));
            characteristic.setValue(authCode);
            if(bluetoothGatt.writeCharacteristic(characteristic)){
                Log.d(TAG,"Write");
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.d("TEST","Characteristic Writed");
            super.onCharacteristicWrite(gatt, characteristic, status);
        }
    };

    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            Log.d("TEST","Discovered " + result.getDevice().getName());

            bluetoothLeScanner.stopScan(scanCallback);
            bluetoothGatt = result.getDevice().connectGatt(mContext,false,bluetoothGattCallback);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
        }
    };

    public void scanBle(){
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();

        List<ScanFilter> filters = new ArrayList<>();
        ScanFilter scanFilter = new ScanFilter.Builder()
                .setServiceUuid(new ParcelUuid(deviceUUID))
                .build();
        filters.add(scanFilter);

        ScanSettings scanSettings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .build();

        bluetoothLeScanner.startScan(filters, scanSettings, scanCallback);
        Log.d("TEST","Scanning");
    }
}
