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

import java.util.UUID;

public class EarbudsManager extends Service {
    private static final String PACKNAME = "app.tuuure.earbudswitch";
    static final String CHANNEL_ID = PACKNAME + ".EarbudsManager";
    static final String CHANNEL_NAME = "Foreground Service";
    static final String TAG = "EarbudsManager";
    static final int ACTION_SCAN = 11;
    static final int ACTION_ADVERTISE = 12;

    private Context mContext;
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BleClient client;
    private BleServer server;
    private BluetoothDevice bluetoothDevice;
    private int ops;
    A2dpManager a2dpManager;

    // 监听蓝牙关闭与自定义广播，用于关闭service
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) {
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
                case BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED:
                    if (BluetoothProfile.STATE_CONNECTED == intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1) && ops == ACTION_SCAN) {
                        stopSelf();
                    }
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

    private BluetoothProfile.ServiceListener a2dpListener = new BluetoothProfile.ServiceListener() {
        @Override
        public void onServiceDisconnected(int profile) {
            if (profile == BluetoothProfile.A2DP) {
                a2dpManager.setAd2p(null);
            }
        }

        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            if (profile == BluetoothProfile.A2DP) {
                a2dpManager.setAd2p((BluetoothA2dp) proxy);//转换
                switch (ops) {
                    case ACTION_SCAN:
                        client = new BleClient(mContext, bluetoothDevice, bluetoothAdapter, a2dpManager);
                        break;
                    case ACTION_ADVERTISE:
                        server = new BleServer(mContext, bluetoothDevice, bluetoothAdapter, a2dpManager);
                        break;
                }
            }
        }
    };

    private void initBluetooth() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            //初始化蓝牙服务
            bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (bluetoothManager != null && getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                bluetoothAdapter = bluetoothManager.getAdapter();
                if (!bluetoothAdapter.isEnabled()) {
                    bluetoothAdapter.enable();
                }
            } else {
                Toast.makeText(this, getString(R.string.unsupport_ble), Toast.LENGTH_SHORT).show();
                stopSelf();
            }
        } else {
            Toast.makeText(this, getString(R.string.request_permission), Toast.LENGTH_SHORT).show();
            Intent activityIntent = new Intent(getApplicationContext(), MainActivity.class);
            activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(activityIntent);
            stopSelf();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        ops = intent.getIntExtra("ops", 0);
        Log.d(TAG, String.valueOf(ops));
        if (ops == 0) {
            stopSelf();
        }
        bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        a2dpManager = new A2dpManager(this, bluetoothAdapter, a2dpListener);
        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        a2dpManager.destroy();

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
