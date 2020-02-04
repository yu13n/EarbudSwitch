package app.tuuure.earbudswitch;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.provider.Settings;
import android.util.ArraySet;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import static app.tuuure.earbudswitch.ConvertUtils.md5code32;


public class BHTDialog extends AppCompatActivity {
    final String TAG = "EBSDialog";
    RecyclerView rvDialog;
    DevicesAdapter rvAdapter;
    private Context mContext;
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    HashMap<String, String> boundeDevices = new HashMap<>(10);
    BleClient client;
    Toolbar toolbar;

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) {
                Log.e(TAG, "Receiver action null");
                return;
            }
            if (BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED.equals(action)) {
                int state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1);
                switch (state) {
                    case BluetoothProfile.STATE_CONNECTED:
                        finish();
                        break;
                    case BluetoothProfile.STATE_DISCONNECTED:
                        scanBle();
                        break;
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_bht);
        mContext = this;

        Window window = getWindow();
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        toolbar = findViewById(R.id.tb_dialog);
        setSupportActionBar(toolbar);

        rvDialog = findViewById(R.id.rv_dialog);
        LinearLayoutManager mLinearLayoutManager = new LinearLayoutManager(this);
        mLinearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        rvDialog.setLayoutManager(mLinearLayoutManager);
        rvDialog.setItemAnimator(new DefaultItemAnimator());
        rvAdapter = new DevicesAdapter();
        rvAdapter.setOnItemClickListener(new DevicesAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                RecycleItem item = (RecycleItem) rvAdapter.bondedDevices.toArray()[position];
                BluetoothDevice device = bluetoothAdapter.getRemoteDevice(item.budsAddress);
                Log.d(TAG, item.budsName + item.serverAddress);
                if (item.serverAddress == null || item.serverAddress.isEmpty()) {
                    ProfileManager.connect(mContext, device);
                } else {
                    client = new BleClient(mContext, bluetoothAdapter.getRemoteDevice(item.budsAddress));
                    client.bluetoothGatt = bluetoothAdapter.getRemoteDevice(item.serverAddress)
                            .connectGatt(mContext, false, client.bluetoothGattCallback);
                }
            }
        });
        rvDialog.setAdapter(rvAdapter);

        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null || !getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, getText(R.string.unsupport_ble), Toast.LENGTH_SHORT).show();
            finish();
        }
        bluetoothAdapter = bluetoothManager.getAdapter();
        if (!bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.enable();
            Toast.makeText(this, getText(R.string.toast_enable_bluetooth), Toast.LENGTH_SHORT).show();
        }
    }

    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice server = result.getDevice();
            String budsAddress = null;
            String budsName = null;
            List<ParcelUuid> serviceData = result.getScanRecord().getServiceUuids();

            if (serviceData.isEmpty()) {
                Log.d(TAG, "Empty ServiceData");
                super.onScanResult(callbackType, result);
            }

            for (ParcelUuid uuid : serviceData) {
                budsAddress = boundeDevices.get(uuid.toString());
                if (budsAddress != null) {
                    BluetoothDevice target = bluetoothAdapter.getRemoteDevice(budsAddress);
                    if (target != null) {
                        budsName = target.getName();
                        break;
                    }
                }
            }
            if (budsAddress != null && budsName != null) {
                RecycleItem item = new RecycleItem(budsName, budsAddress, server.getAddress());
                rvAdapter.setConnectable(item);
                Log.d(TAG, "Discovered " + budsName + " Server " + server.getAddress());
            }

            super.onScanResult(callbackType, result);
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

    void scanBle() {
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();

        List<ScanFilter> filters = new ArrayList<>(10);
        ScanFilter.Builder builder = new ScanFilter.Builder();

        if (boundeDevices.isEmpty()) {
            Log.d(TAG, "Empty Bounded Audio Devices");
            return;
        }

        for (String key : boundeDevices.keySet()) {
            Log.d(TAG, "Ble Scan filter: " + key);
            ScanFilter filter = builder
                    .setServiceUuid(ParcelUuid.fromString(key))
                    .build();
            filters.add(filter);
        }

        ScanSettings scanSettings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                .build();

        bluetoothLeScanner.startScan(filters, scanSettings, scanCallback);
        Log.d(TAG, "Scanning");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_quicksetting, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_system_settings) {
            startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        bluetoothLeScanner.stopScan(scanCallback);
        unregisterReceiver(receiver);
        Log.d(TAG, "Receiver unregistered");
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //注册监听蓝牙状态的receiver
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
        registerReceiver(receiver, intentFilter);
        Log.d(TAG, "Receiver registered");

        Set<BluetoothDevice> devices = new ArraySet<>();
        for (BluetoothDevice device : bluetoothAdapter.getBondedDevices()) {
            if (BluetoothClass.Device.Major.AUDIO_VIDEO == device.getBluetoothClass().getMajorDeviceClass()) {
                devices.add(device);
                boundeDevices.put(md5code32(device.getAddress()), device.getAddress());
            }
        }
        rvAdapter.devicesReset(devices);
        scanBle();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
