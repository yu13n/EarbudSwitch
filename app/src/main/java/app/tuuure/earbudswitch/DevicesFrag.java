package app.tuuure.earbudswitch;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
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
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static app.tuuure.earbudswitch.ConvertUtils.md5code32;

public class DevicesFrag extends Fragment {
    final static String TAG = "FragDevice";
    private DialogActivity mContext;
    private RecyclerView rvDialog;
    private DevicesAdapter rvAdapter;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private HashMap<String, String> boundeDevices = new HashMap<>(10);

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) {
                Log.e(TAG, "Receiver action null");
                return;
            }
            int state;
            switch (action) {
                case BluetoothAdapter.ACTION_STATE_CHANGED:
                    state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                    switch (state) {
                        case BluetoothAdapter.STATE_TURNING_OFF:
                        case BluetoothAdapter.STATE_OFF:
                            mContext.setItemSwitchChecked(false);
                            rvAdapter.devicesReset(null);
                            if (bluetoothLeScanner != null) {
                                if (bluetoothAdapter.isEnabled())
                                    bluetoothLeScanner.stopScan(scanCallback);
                                bluetoothLeScanner = null;
                            }
                            break;
                        case BluetoothAdapter.STATE_ON:
                            mContext.setItemSwitchChecked(true);
                            rvAdapter.devicesReset(bluetoothAdapter.getBondedDevices());
                            if (mContext.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        scanBle();
                                    }
                                }).start();
                            }
                            break;
                    }
                    break;
                case BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED:
                    state = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, -1);
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    switch (state) {
                        case BluetoothAdapter.STATE_CONNECTED:
                            rvAdapter.setConnected(device, true);
                            break;
                        case BluetoothAdapter.STATE_DISCONNECTED:
                            rvAdapter.setConnected(device, false);
                            break;
                        case BluetoothAdapter.STATE_CONNECTING:
                            rvAdapter.setRefreshPosition(device);
                    }
                    break;
            }
        }
    };

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mContext = (DialogActivity) context;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_device, container, false);
        rvDialog = view.findViewById(R.id.rv_dialog);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        LinearLayoutManager mLinearLayoutManager = new LinearLayoutManager(mContext);
        mLinearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        rvDialog.setLayoutManager(mLinearLayoutManager);
        rvDialog.setItemAnimator(new DefaultItemAnimator());
        rvAdapter = new DevicesAdapter();
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        rvAdapter.setOnItemClickListener(new DevicesAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                RecycleItem item = (RecycleItem) rvAdapter.bondedDevices.toArray()[position];
                BluetoothDevice device = bluetoothAdapter.getRemoteDevice(item.budsAddress);
                if (item.isConnected) {
                    Intent service = new Intent(mContext, EarbudService.class);
                    service.putExtra(BluetoothDevice.EXTRA_DEVICE, device);
                    //TODO: ManuAdvertise
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        mContext.startForegroundService(service);
                    } else {
                        mContext.startService(service);
                    }
                    return;
                }
                Log.d(TAG, item.budsName + item.serverAddress);
                if (item.serverAddress == null || item.serverAddress.isEmpty()) {
                    //TODO: NormalConnect
                    ProfileManager.connect(mContext, device);
                } else {
                    //TODO: EBSConnect
                    BleClient client = new BleClient(mContext, bluetoothAdapter.getRemoteDevice(item.budsAddress));
                    client.bluetoothGatt = bluetoothAdapter.getRemoteDevice(item.serverAddress)
                            .connectGatt(mContext, false, client.bluetoothGattCallback);
                }
            }
        });

        rvAdapter.setOnItemLongClickListener(new DevicesAdapter.OnItemLongClickListener() {
            @Override
            public void onItemLongClickListener(View view, int position) {
                RecycleItem item = (RecycleItem) rvAdapter.bondedDevices.toArray()[position];
                BluetoothDevice device = bluetoothAdapter.getRemoteDevice(item.budsAddress);
                if (item.isConnected) {
                    //TODO: ManuDisconnect
                    ProfileManager.disconnect(mContext, device);
                }
            }
        });
        rvDialog.setAdapter(rvAdapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        rvAdapter.setRefreshPosition(null);
        //注册监听蓝牙状态的receiver
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        intentFilter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
        mContext.registerReceiver(receiver, intentFilter);
        Log.d(TAG, "Receiver registered");

        rvAdapter.devicesReset(bluetoothAdapter.getBondedDevices());

        bluetoothAdapter.getProfileProxy(mContext, proxyListener, BluetoothProfile.A2DP);
        bluetoothAdapter.getProfileProxy(mContext, proxyListener, BluetoothProfile.HEADSET);

        new Thread(new Runnable() {
            @Override
            public void run() {
                ArrayList<RecycleItem> devices = rvAdapter.getData();
                if (devices != null && !devices.isEmpty()) {
                    for (RecycleItem item : devices) {
                        boundeDevices.put(md5code32(item.budsAddress), item.budsAddress);
                    }
                    if (bluetoothAdapter.isEnabled() && mContext.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        scanBle();
                    }
                }
            }
        }).start();
    }

    @Override
    public void onPause() {
        if (bluetoothLeScanner != null) {
            if (bluetoothAdapter.isEnabled())
                bluetoothLeScanner.stopScan(scanCallback);
            bluetoothLeScanner = null;
        }

        try {
            mContext.unregisterReceiver(receiver);
            Log.d(TAG, "Receiver unregistered");
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        super.onPause();
    }

    private BluetoothProfile.ServiceListener proxyListener = new BluetoothProfile.ServiceListener() {
        @Override
        public void onServiceDisconnected(int profile) {
        }

        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            List<BluetoothDevice> devices = proxy.getConnectedDevices();
            if (devices != null && !devices.isEmpty()) {
                for (BluetoothDevice device : devices) {
                    rvAdapter.setConnected(device, true);
                }
            }
            bluetoothAdapter.closeProfileProxy(profile, proxy);
        }
    };

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

    private void scanBle() {
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
}
