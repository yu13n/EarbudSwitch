package app.tuuure.earbudswitch;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.microsoft.appcenter.analytics.Analytics;

public class EarbudService extends Service {
    private static final String PACKNAME = "app.tuuure.earbudswitch";
    static final String CHANNEL_ID = PACKNAME + ".EarbudService";
    static final String CHANNEL_NAME = "Foreground Service";
    static final String TAG = "EarbudService";
    static final int NOTIFICATION_ID = 30;

    private BleServer server;
    private NotificationCompat.Builder notificationBuilder;
    private NotificationManager notificationManager;

    // 监听蓝牙关闭与自定义广播，用于关闭service
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) {
                return;
            }
            int state = -1;
            switch (action) {
                case BluetoothAdapter.ACTION_STATE_CHANGED:
                    if (state == -1) {
                        // Bluetooth Turning off
                        state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                        if (state != BluetoothAdapter.STATE_OFF) {
                            break;
                        }
                    }
                case BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED:
                    if (state == -1) {
                        // Bluetooth Device disconnect
                        state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1);
                        if (state != BluetoothProfile.STATE_DISCONNECTED) {
                            break;
                        } else {
                            server.disconnectNotify();
                        }
                    }
                case CHANNEL_ID:
                    // Service stop self
                    server.stopAdvertise();
                    server.stopGattServer();
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
        Log.d(TAG, "Service start");

        notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID);
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        //注册通知渠道
        registerNotificationChannel();

        Analytics.trackEvent("ServiceStart");

        PendingIntent pendIntent = PendingIntent.getBroadcast(this, 0, new Intent(CHANNEL_ID), PendingIntent.FLAG_CANCEL_CURRENT);
        NotificationCompat.Action.Builder actionBuilder = new NotificationCompat.Action.Builder(R.drawable.ac_key, "Stop", pendIntent);

        notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID);
        notificationBuilder.setSmallIcon(R.drawable.ic_notify_advertise)
                .setColor(getColor(R.color.color_theme))
                .setContentText(String.format(getString(R.string.notification_content), getString(R.string.unknown_device)))
                .addAction(actionBuilder.build());
        startForeground(NOTIFICATION_ID, notificationBuilder.build());

        //注册用于关闭服务的receiver
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(CHANNEL_ID);
        intentFilter.addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(receiver, intentFilter);

        //初始化蓝牙
        initBluetooth();

        //初始化广播server
        server = new BleServer(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        BluetoothDevice bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

        String name = bluetoothDevice.getName();
        if (name == null || name.isEmpty()) {
            name = getString(R.string.unknown_device);
        }
        notificationBuilder.setContentText(String.format(getString(R.string.notification_content), name));
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());

        server.addDevice(bluetoothDevice);

        return START_REDELIVER_INTENT;
    }

    private void initBluetooth() {
        //初始化蓝牙服务
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null && getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
            if (!bluetoothAdapter.isEnabled()) {
                bluetoothAdapter.enable();
            }
        } else {
            Toast.makeText(this, getString(R.string.unsupport_ble), Toast.LENGTH_SHORT).show();
            stopSelf();
        }
    }

    @Override
    public void onDestroy() {

        try {
            unregisterReceiver(receiver);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }

        stopForeground(true);

        super.onDestroy();
    }

    private void registerNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = notificationManager.getNotificationChannel(CHANNEL_ID);
            if (notificationChannel == null) {
                NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW);
                channel.enableLights(false);
                channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
                channel.setShowBadge(false);
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}
