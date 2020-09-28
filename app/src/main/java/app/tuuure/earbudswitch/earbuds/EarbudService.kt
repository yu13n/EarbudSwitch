package app.tuuure.earbudswitch.earbuds

import android.app.*
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import app.tuuure.earbudswitch.CancelAdvertiseEvent
import app.tuuure.earbudswitch.DisconnectEvent
import app.tuuure.earbudswitch.R
import app.tuuure.earbudswitch.nearby.ble.BleAdvertiser
import app.tuuure.earbudswitch.utils.Preferences
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

class EarbudService : Service() {
    companion object {
        private const val PACKNAME = "app.tuuure.earbudswitch"
        private const val CHANNEL_ID = "$PACKNAME.EarbudService"
        private const val CHANNEL_NAME = "Foreground Service"
        private const val NOTIFICATION_ID = 2
    }

    private var notificationBuilder: NotificationCompat.Builder =
        NotificationCompat.Builder(this, CHANNEL_ID)
    private lateinit var notificationManager: NotificationManager
    private lateinit var pendIntent: PendingIntent
    private lateinit var actionBuilder: NotificationCompat.Action.Builder

    private lateinit var key: String
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bleAdvertiser: BleAdvertiser

    private val deviceList: HashSet<BluetoothDevice> = HashSet(2)

    override fun onCreate() {
        super.onCreate()

        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        registerNotificationChannel()
        pendIntent = PendingIntent.getBroadcast(
            this@EarbudService,
            0,
            Intent(CHANNEL_ID),
            PendingIntent.FLAG_CANCEL_CURRENT
        )
        actionBuilder = NotificationCompat.Action.Builder(null, "Stop", pendIntent)
        notificationBuilder.setSmallIcon(R.drawable.ic_notify)
            .setColor(getColor(R.color.colorAccent))
            .setContentText(getString(R.string.notification_blank_content))
        notificationBuilder.addAction(actionBuilder.build())
        startForeground(NOTIFICATION_ID, notificationBuilder.build())

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        key = Preferences.getInstance(this@EarbudService).getKey()
        bleAdvertiser = BleAdvertiser(this@EarbudService, key)

        //注册用于关闭服务的receiver
        val intentFilter = IntentFilter()
        intentFilter.addAction(CHANNEL_ID)
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(receiver, intentFilter)

        EventBus.getDefault().register(this@EarbudService)
    }

    private fun updateDeviceList(device: BluetoothDevice): Boolean {
        val result = !deviceList.contains(device)
        if (result) {
            deviceList.add(device)
        }
        return result
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val device =
            bluetoothAdapter.getRemoteDevice(intent!!.getStringExtra(BluetoothDevice.EXTRA_DEVICE))
        val state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1)
        if (state == BluetoothProfile.STATE_CONNECTED) {
            val restartNeeded = updateDeviceList(device)
            if (deviceList.size != 0) {
                if (restartNeeded) {
                    bleAdvertiser.advertise(deviceList)
                }
            } else {
                stopSelf()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    @Subscribe
    fun onCancelAdvertise(event: CancelAdvertiseEvent) {
        val device = bluetoothAdapter.getRemoteDevice(event.device)
        val restartNeeded = deviceList.contains(device)
        deviceList.remove(device)

        if (deviceList.size != 0) {
            if (restartNeeded) {
                bleAdvertiser.advertise(deviceList)
            }
        } else {
            bleAdvertiser.stopAdvertise()
            stopSelf()
        }
    }

    @Subscribe
    fun onDisconnectEvent(event: DisconnectEvent) {
        val device = bluetoothAdapter.getRemoteDevice(event.device)
        EarbudManager.disconnectEBS(this, device)
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)

        if (this@EarbudService::bleAdvertiser.isInitialized) {
            bleAdvertiser.stopAdvertise()
        }

        try {
            unregisterReceiver(receiver)
        } catch (ignored: IllegalArgumentException) {
        }
        stopForeground(true)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun registerNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = notificationManager.getNotificationChannel(CHANNEL_ID)
            if (notificationChannel == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW
                )
                channel.enableLights(false)
                channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                channel.setShowBadge(false)
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    // 监听蓝牙关闭与自定义广播，用于关闭service
    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)
                    // Bluetooth Turning off
                    if (state == BluetoothAdapter.STATE_OFF) {
                        stopSelf()
                    }
                }
                CHANNEL_ID -> stopSelf()
            }
        }
    }
}