package app.tuuure.earbudswitch;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Icon;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;

public class QuickSetting extends TileService {
    static final String TAG = "QuickSetting";
    BluetoothAdapter bluetoothAdapter;
    Tile tile;

    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                switch (state) {
                    case BluetoothAdapter.STATE_ON:
                        tile.setState(Tile.STATE_ACTIVE);
                        tile.setSubtitle(null);
                        tile.setIcon(Icon.createWithResource(getApplicationContext(), R.drawable.ic_bluetooth));
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        tile.setState(Tile.STATE_ACTIVE);
                        tile.setSubtitle("Turning on ...");
                        tile.setIcon(Icon.createWithResource(getApplicationContext(), R.drawable.ic_bluetooth_enable));
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                    case BluetoothAdapter.STATE_OFF:
                        tile.setState(Tile.STATE_INACTIVE);
                        tile.setSubtitle(null);
                        tile.setIcon(Icon.createWithResource(getApplicationContext(), R.drawable.ic_bluetooth));
                        break;
                }
                tile.updateTile();
            }
        }
    };

    @Override
    public void onClick() {
        super.onClick();
        // 点击的时候
        Log.d(TAG, "onClick");

        if (Tile.STATE_INACTIVE == tile.getState()) {
            tile.setState(Tile.STATE_ACTIVE);
            tile.setSubtitle("Turning on ...");
            tile.setIcon(Icon.createWithResource(getApplicationContext(), R.drawable.ic_bluetooth_enable));

            bluetoothAdapter.enable();
        } else {
            tile.setState(Tile.STATE_INACTIVE);
            tile.setSubtitle(null);
            tile.setIcon(Icon.createWithResource(getApplicationContext(), R.drawable.ic_bluetooth));

            bluetoothAdapter.disable();
        }
        tile.updateTile();
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        // 打开下拉通知栏的时候调用,当快速设置按钮并没有在编辑栏拖到设置栏中不会调用
        //在TleAdded之后会调用一次
        Log.d(TAG, "onStartListening");
        tile = getQsTile();
        tile.setIcon(Icon.createWithResource(getApplicationContext(), R.drawable.ic_bluetooth));
        BluetoothManager bluetoothManager = (BluetoothManager) getApplicationContext().getSystemService(BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            bluetoothAdapter = bluetoothManager.getAdapter();
            if (bluetoothAdapter != null) {
                tile.setState(bluetoothAdapter.isEnabled() ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
                tile.updateTile();

                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
                registerReceiver(receiver, intentFilter);
                Log.d(TAG, "Receiver registered");
                return;
            }
        }
        tile.setState(Tile.STATE_UNAVAILABLE);
        tile.updateTile();
    }

    @Override
    public void onStopListening() {
        Log.d(TAG, "Receiver unregistered");
        unregisterReceiver(receiver);
        // 关闭下拉通知栏的时候调用,当快速设置按钮并没有在编辑栏拖到设置栏中不会调用
        // 在onTileRemoved移除之前也会调用移除
        Log.d(TAG, "onStopListening");
        super.onStopListening();
    }
}
