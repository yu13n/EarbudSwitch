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

import com.google.firebase.analytics.FirebaseAnalytics;

public class EarbudService extends Service {
    private static final String PACKNAME = "app.tuuure.earbudswitch";
    static final String CHANNEL_ID = PACKNAME + ".EarbudService";
    static final String CHANNEL_NAME = "Foreground Service";
    static final String TAG = "EarbudService";
    static final int NOTIFICATION_ID = 1030;

    private Context mContext;
    private BleServer server;

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
                        state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                        if (state != BluetoothAdapter.STATE_OFF) {
                            break;
                        }
                        Log.d(TAG, "Bluetooth Turning off");
                    }
                case BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED:
                    if (state == -1) {
                        state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1);
                        if (state != BluetoothProfile.STATE_DISCONNECTED) {
                            break;
                        }
                        Log.d(TAG, "Bluetooth Turning off");
                    }
                case CHANNEL_ID:
                    Log.d(TAG, "Service stop self");
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
        //拉起通知
        registerNotificationChannel();

        // FirebaseAnalytics
        FirebaseAnalytics.getInstance(this).logEvent("ServiceStart", null);

        mContext = this;

        //注册用于关闭服务的receiver
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(CHANNEL_ID);
        intentFilter.addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(receiver, intentFilter);
        Log.d(TAG, "Receiver registered");

        initBluetooth();

    }


    private void initBluetooth() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
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
        } else {
            Toast.makeText(this, getString(R.string.request_permission), Toast.LENGTH_SHORT).show();
            Intent activityIntent = new Intent(getApplicationContext(), DialogActivity.class);
            activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(activityIntent);
            stopSelf();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        BluetoothDevice bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

        PendingIntent pendIntent = PendingIntent.getBroadcast(this, 0, new Intent(CHANNEL_ID), PendingIntent.FLAG_CANCEL_CURRENT);
        NotificationCompat.Action.Builder actionBuilder = new NotificationCompat.Action.Builder(R.drawable.ac_key, "Stop", pendIntent);

        String name = bluetoothDevice.getName();
        if (name == null || name.isEmpty()) {
            name = getString(R.string.unknown_device);
        }
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID);
        notificationBuilder.setSmallIcon(R.drawable.ic_notify_advertise)
                .setColor(getColor(R.color.color_theme))
                .setContentText(String.format(getString(R.string.notification_content), name))
                .addAction(actionBuilder.build());
        startForeground(NOTIFICATION_ID, notificationBuilder.build());

        //启动广播server
        server = new BleServer(mContext, bluetoothDevice);

        return START_REDELIVER_INTENT;
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
