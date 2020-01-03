package app.tuuure.earbudswitch;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import java.util.UUID;

public class EarbudsManager extends Service {
    public static final String CHANNEL_ID = "app.tuuure.earbudswitch.EarbudsManager";
    public static final String CHANNEL_NAME = "Foreground Service";
    public static final String TAG = "EarbudsManager";

    private UUID authCode;
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice bluetoothDevice;

    // 监听蓝牙关闭与自定义广播，用于关闭service
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) {
                Log.e(TAG, "Receiver action null");
                return;
            }
            switch (action) {
                case BluetoothAdapter.ACTION_STATE_CHANGED:
                    int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                    if (state != BluetoothAdapter.STATE_OFF) {
                        break;
                    }
                    Log.d(TAG, "Bluetooth Turning off");
                case CHANNEL_ID:
                    Log.d(TAG, "Service stop self");
                    stopSelf();
                    break;
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //拉起通知
        registerNotificationChannel();
        int notifyId = (int) System.currentTimeMillis();
        PendingIntent pendIntent = PendingIntent.getBroadcast(this, 0, new Intent(CHANNEL_ID), PendingIntent.FLAG_CANCEL_CURRENT);
        NotificationCompat.Action.Builder actionBuilder = new NotificationCompat.Action.Builder(R.drawable.ic_launcher_background, "Stop", pendIntent);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID);
        builder.setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getResources().getString(R.string.app_name))
                .setContentText("Foreground service running")
                .addAction(actionBuilder.build());
        startForeground(notifyId, builder.build());
        Log.d(TAG, "Foreground service running");

        //注册用于关闭服务的receiver
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(CHANNEL_ID);
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(receiver, intentFilter);
        Log.d(TAG, "Receiver registered");

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //判断权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Intent activityIntent = new Intent(getApplicationContext(), MainActivity.class);
            activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activityIntent.putExtra(TAG, true);
            startActivity(activityIntent);
            stopSelf();
        }

        bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {

        unregisterReceiver(receiver);
        Log.d(TAG, "Receiver unregistered");

        stopForeground(true);
        Log.d(TAG, "Foreground service terminated");

        super.onDestroy();
    }

    private void registerNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager == null) {
                return;
            }
            NotificationChannel notificationChannel = notificationManager.getNotificationChannel(CHANNEL_ID);
            if (notificationChannel == null) {
                NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW);
                channel.enableLights(false);
                channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
                channel.setShowBadge(true);
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

}
