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
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.List;

import static app.tuuure.earbudswitch.ProfileManager.getConnectionState;

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
                case "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED":
                    Log.d(TAG, "reflection succeed");
                    bluetoothAdapter.getProfileProxy(mContext, a2dpListener, BluetoothProfile.A2DP);
                    break;
            }
        }
    };

    private BluetoothProfile.ServiceListener a2dpListener = new BluetoothProfile.ServiceListener() {
        @Override
        public void onServiceDisconnected(int profile) {
            Log.d(TAG, "ServiceDisconnected");
        }

        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            Log.d(TAG, "ServiceConnected");
            BluetoothA2dp bluetoothA2dp = (BluetoothA2dp) proxy;
            List<BluetoothDevice> devices = bluetoothA2dp.getConnectedDevices();
            if (devices != null && !devices.isEmpty()) {
                Log.d(TAG, "ConnectedDevice" + devices.get(0).getName());
                connectedState(devices.get(0));
            }
            bluetoothAdapter.closeProfileProxy(BluetoothProfile.A2DP, bluetoothA2dp);
        }
    };

    private void updateTile(String label, String subtitle, Icon icon, int state) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.setLabel(label);
            tile.setSubtitle(subtitle);
        } else {
            if (subtitle != null) {
                tile.setLabel(subtitle);
            } else {
                tile.setLabel(label);
            }
        }
        tile.setIcon(icon);
        tile.setState(state);
        tile.updateTile();
    }

    private void offState() {
        updateTile(
                getString(R.string.app_name),
                null,
                Icon.createWithResource(mContext, R.drawable.ic_qs_bluetooth),
                Tile.STATE_INACTIVE
        );
    }

    private void turningOnState() {
        updateTile(
                getString(R.string.app_name),
                getString(R.string.turning_on),
                Icon.createWithResource(mContext, R.drawable.ic_qs_bluetooth_connect),
                Tile.STATE_ACTIVE
        );
    }

    private void onState() {
        updateTile(
                getString(R.string.app_name),
                null,
                Icon.createWithResource(mContext, R.drawable.ic_qs_bluetooth),
                Tile.STATE_ACTIVE
        );
    }

    private void connectingState() {
        updateTile(
                getString(R.string.app_name),
                getString(R.string.connecting_device),
                Icon.createWithResource(mContext, R.drawable.ic_qs_bluetooth_connect),
                Tile.STATE_ACTIVE
        );
    }

    private void connectedState(final BluetoothDevice device) {
        String subtitle = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            try {
                Method connectMethod = BluetoothDevice.class.getMethod("getBatteryLevel");
                int batteryLevel = (int) connectMethod.invoke(device);
                Log.d(TAG, "getBatteryLevel" + String.valueOf(batteryLevel));
                if (batteryLevel != BATTERY_LEVEL_UNKNOWN) {
                    subtitle = String.format(getString(R.string.battery_level), batteryLevel);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        updateTile(
                device.getName(),
                subtitle,
                Icon.createWithResource(mContext, R.drawable.ic_qs_bluetooth_connect),
                Tile.STATE_ACTIVE
        );
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

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        intentFilter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
        intentFilter.addAction("android.bluetooth.device.action.BATTERY_LEVEL_CHANGED");
        registerReceiver(receiver, intentFilter);
        Log.d(TAG, "onStartListening");

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter.isEnabled()) {
            switch (getConnectionState()) {
                case BluetoothAdapter.STATE_CONNECTING:
                    connectingState();
                    break;
                case BluetoothAdapter.STATE_CONNECTED:
                    bluetoothAdapter.getProfileProxy(this, a2dpListener, BluetoothProfile.A2DP);
                    break;
                default:
                    onState();
                    break;
            }
        } else {
            offState();
        }
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
