package app.tuuure.earbudswitch;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Icon;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.List;

public class QuickSetting extends TileService {
    static final String TAG = "QuickSetting";
    static final int BATTERY_LEVEL_UNKNOWN = -1;

    private Tile tile;
    private BluetoothAdapter bluetoothAdapter;
    private Context mContext;

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
                        case BluetoothAdapter.STATE_OFF:
                        case BluetoothAdapter.STATE_TURNING_OFF:
                            offState();
                            break;
                        case BluetoothAdapter.STATE_TURNING_ON:
                            turningOnState();
                            break;
                        case BluetoothAdapter.STATE_ON:
                            onState();
                            break;
                    }
                    break;
                case BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED:
                    state = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, -1);
                    switch (state) {
                        case BluetoothAdapter.STATE_DISCONNECTED:
                        case BluetoothAdapter.STATE_DISCONNECTING:
                            onState();
                            break;
                        case BluetoothAdapter.STATE_CONNECTING:
                            connectingState();
                            break;
                        case BluetoothAdapter.STATE_CONNECTED:
                            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                            connectedState(device);
                            break;
                    }
                    break;
            }
            tile.updateTile();
        }
    };

    private BluetoothProfile.ServiceListener a2dpListener = new BluetoothProfile.ServiceListener() {
        @Override
        public void onServiceDisconnected(int profile) {
        }

        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            BluetoothA2dp bluetoothA2dp = (BluetoothA2dp) proxy;
            List<BluetoothDevice> devices = bluetoothA2dp.getConnectedDevices();
            if (devices != null && !devices.isEmpty()) {
                connectedState(devices.get(0));
            }
            bluetoothAdapter.closeProfileProxy(BluetoothProfile.A2DP, bluetoothA2dp);
        }
    };

    private void offState() {
        tile.setLabel(getString(R.string.app_name));
        tile.setSubtitle(null);
        tile.setIcon(Icon.createWithResource(mContext, R.drawable.ic_qs_bluetooth));
        tile.setState(Tile.STATE_INACTIVE);
        tile.updateTile();
    }

    private void turningOnState() {
        tile.setLabel(getString(R.string.app_name));
        tile.setSubtitle(getString(R.string.turning_on));
        tile.setIcon(Icon.createWithResource(mContext, R.drawable.ic_qs_bluetooth_connect));
        tile.setState(Tile.STATE_ACTIVE);
        tile.updateTile();
    }

    private void onState() {
        tile.setLabel(getString(R.string.app_name));
        tile.setSubtitle(null);
        tile.setIcon(Icon.createWithResource(mContext, R.drawable.ic_qs_bluetooth));
        tile.setState(Tile.STATE_ACTIVE);
        tile.updateTile();
    }

    private void connectingState() {
        tile.setLabel(getString(R.string.app_name));
        tile.setSubtitle(getString(R.string.connecting_device));
        tile.setIcon(Icon.createWithResource(mContext, R.drawable.ic_qs_bluetooth_connect));
        tile.setState(Tile.STATE_ACTIVE);
        tile.updateTile();
    }

    private void connectedState(final BluetoothDevice device) {
        tile.setLabel(device.getName());
        tile.setSubtitle(null);
        try {
            Method connectMethod = BluetoothDevice.class.getMethod("getBatteryLevel");
            int batteryLevel = (int) connectMethod.invoke(device);
            Log.d(TAG, "getBatteryLevel" + String.valueOf(batteryLevel));
            if (batteryLevel != BATTERY_LEVEL_UNKNOWN) {
                tile.setSubtitle(String.format(getString(R.string.battery_level), batteryLevel));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        tile.setIcon(Icon.createWithResource(mContext, R.drawable.ic_qs_bluetooth_connect));
        tile.setState(Tile.STATE_ACTIVE);
        tile.updateTile();
    }

    @Override
    public void onClick() {
        super.onClick();
        // 点击的时候
        if (bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.disable();
        } else {
            bluetoothAdapter.enable();
        }
        Log.d(TAG, "onClick");
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        // 打开下拉通知栏的时候调用,当快速设置按钮并没有在编辑栏拖到设置栏中不会调用
        //在TleAdded之后会调用一次
        mContext = this;
        tile = getQsTile();
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter.isEnabled()) {
            onState();
            bluetoothAdapter.getProfileProxy(this, a2dpListener, BluetoothProfile.A2DP);
        } else {
            offState();
        }

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        intentFilter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
        registerReceiver(receiver, intentFilter);
        Log.d(TAG, "onStartListening");
    }

    @Override
    public void onStopListening() {
        // 关闭下拉通知栏的时候调用,当快速设置按钮并没有在编辑栏拖到设置栏中不会调用
        // 在onTileRemoved移除之前也会调用移除
        Log.d(TAG, "onStopListening");
        unregisterReceiver(receiver);
        super.onStopListening();
    }
}
