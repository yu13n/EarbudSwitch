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
import android.util.ArrayMap;
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

import com.microsoft.appcenter.analytics.Analytics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import app.tuuure.earbudswitch.Utils.ProfileManager;

import static app.tuuure.earbudswitch.Utils.ConvertUtils.*;

public class DevicesFrag extends Fragment {
    private final static String TAG = "FragDevice";
    private DialogActivity mContext;
    private RecyclerView rvDialog;
    private DevicesAdapter rvAdapter;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private ArrayMap<UUID, String> boundeDevices = new ArrayMap<>(10);
    private BleClient client;

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) {
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
                    Analytics.trackEvent("ManuAdvertise");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        mContext.startForegroundService(service);
                    } else {
                        mContext.startService(service);
                    }
                    return;
                }
                Log.d(TAG, item.budsName + item.serverAddress);
                if (item.serverAddress == null || item.serverAddress.isEmpty()) {
                    Analytics.trackEvent("NormalConnect");
                    ProfileManager.connect(mContext, device);
                } else {
                    Analytics.trackEvent("EBSConnect");

                    rvAdapter.setRefreshPosition(device);
                    client = new BleClient(mContext, device);
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
                    Analytics.trackEvent("ManuDisconnect");
                    ProfileManager.disconnect(mContext, device);
                }
            }
        });
        rvDialog.setAdapter(rvAdapter);
    }

    private long startTime;
    private boolean isFirstFind = true;

    @Override
    public void onResume() {
        super.onResume();
        startTime = System.currentTimeMillis();

        rvAdapter.setRefreshPosition(null);
        //注册监听蓝牙状态的receiver
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        intentFilter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
        mContext.registerReceiver(receiver, intentFilter);

        rvAdapter.devicesReset(bluetoothAdapter.getBondedDevices());

//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                ArrayList<RecycleItem> devices = rvAdapter.getData();
//                if (devices != null && !devices.isEmpty()) {
//                    for (RecycleItem item : devices) {
//                        boundeDevices.put(md5code32(item.budsAddress), item.budsAddress);
//                    }
//                    if (bluetoothAdapter.isEnabled() && mContext.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
//                        scanBle();
//                    }
//                }
//            }
//        }).start();
        ArrayList<RecycleItem> devices = rvAdapter.getData();
        if (devices != null && !devices.isEmpty()) {
            for (RecycleItem item : devices) {
                boundeDevices.put(md5code32(item.budsAddress), item.budsAddress);
            }
            if (bluetoothAdapter.isEnabled() && mContext.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                scanBle();
            }
        }
        bluetoothAdapter.getProfileProxy(mContext, proxyListener, BluetoothProfile.A2DP);
        bluetoothAdapter.getProfileProxy(mContext, proxyListener, BluetoothProfile.HEADSET);

        rvAdapter.startTimer();
    }

    @Override
    public void onPause() {
        if (bluetoothLeScanner != null) {
            if (bluetoothAdapter.isEnabled())
                bluetoothLeScanner.stopScan(scanCallback);
            bluetoothLeScanner = null;
        }

        rvAdapter.stopTimer();

        if (client != null && client.receiver != null) {
            try {
                mContext.unregisterReceiver(client.receiver);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }


        try {
            mContext.unregisterReceiver(receiver);
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
            List<ParcelUuid> parcelUuids = result.getScanRecord().getServiceUuids();
            byte[] data = result.getScanRecord().getManufacturerSpecificData(0xEB55);
            if (data != null) {
                parcelUuids = new ArrayList<>(1);
                parcelUuids.add(new ParcelUuid(bytesToUUID(data)));
            }

            if (parcelUuids == null || parcelUuids.isEmpty()) {
                return;
            }
            for (ParcelUuid uuid : parcelUuids) {
                if (boundeDevices.containsKey(uuid.getUuid())) {
                    String budsAddress = boundeDevices.get(uuid.getUuid());
                    BluetoothDevice target = bluetoothAdapter.getRemoteDevice(budsAddress);
                    if (target != null) {
                        String budsName = target.getName();
                        if (budsName != null) {
                            RecycleItem item = new RecycleItem(budsName, budsAddress, server.getAddress(), System.currentTimeMillis());
                            rvAdapter.setConnectable(item);

                            Log.d(TAG, String.format("Device %1$s discoverd, Server %2$s", budsName, server.getAddress()));
                            if (isFirstFind) {
                                long time = System.currentTimeMillis() - startTime;
                                Map<String, String> properties = new HashMap<>();
                                properties.put("Time", String.valueOf(time));
                                Log.d(TAG, String.format("After %1$d ms", time));
                                Analytics.trackEvent("ScanTime", properties);
                                isFirstFind = false;
                            }
                        }
                    }
                }
            }
            super.onScanResult(callbackType, result);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.d(TAG, "Scan Failed" + errorCode);
            super.onScanFailed(errorCode);
        }
    };

    private void scanBle() {
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();

        ArrayList<ScanFilter> filters = new ArrayList<>(boundeDevices.size());

        if (boundeDevices.isEmpty()) {
            return;
        }

        for (UUID uuid : boundeDevices.keySet()) {
            ScanFilter filter = new ScanFilter.Builder()
                    .setServiceUuid(new ParcelUuid(uuid))
                    .build();
            filters.add(filter);
            byte[] data = uuidToBytes(uuid);
            ScanFilter filter2 = new ScanFilter.Builder()
                    .setManufacturerData(0xEB55, data)
                    .build();
            filters.add(filter2);
        }

        ScanSettings scanSettings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                .setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
                .setReportDelay(0)
                .build();

        bluetoothLeScanner.startScan(filters, scanSettings, scanCallback);
        Log.d(TAG, "Scanning");
    }
}
