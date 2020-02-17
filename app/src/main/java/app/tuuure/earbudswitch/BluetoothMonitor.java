package app.tuuure.earbudswitch;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class BluetoothMonitor extends BroadcastReceiver {
    final static String TAG = "BluetoothMonitor";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "Broadcast Captured, action: " + action);
        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        if (BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED.equals(action)) {
            int state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1);
            switch (state) {
                case BluetoothProfile.STATE_CONNECTED:
                    Intent service = new Intent(context, EarbudService.class);
                    service.putExtra(BluetoothDevice.EXTRA_DEVICE, device);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(service);
                    } else {
                        context.startService(service);
                    }
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    Intent stopIntent = new Intent();
                    stopIntent.setAction(EarbudService.CHANNEL_ID);
                    context.sendBroadcast(stopIntent);
                    break;
            }
        }
    }
}
