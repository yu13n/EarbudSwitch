package app.tuuure.earbudswitch.earbuds

import android.annotation.SuppressLint
import android.app.*
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import app.tuuure.earbudswitch.*
import app.tuuure.earbudswitch.nearby.ble.BleAdvertiser
import app.tuuure.earbudswitch.nearby.ble.BleScanner
import com.drake.channel.AndroidScope
import com.drake.channel.receiveEvent
import com.drake.channel.sendEvent
import kotlinx.coroutines.*
import java.util.*

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

    private val scrolls = ArrayList<AndroidScope>()
    private lateinit var bleScanner: BleScanner
    private lateinit var bleAdvertiser: BleAdvertiser
    private lateinit var earbudManager: EarbudManager

    override fun onCreate() {
        super.onCreate()
        bleScanner = BleScanner(this@EarbudService, false)
        bleAdvertiser = BleAdvertiser(this@EarbudService, false)
        earbudManager = EarbudManager(this@EarbudService)

        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        registerNotificationChannel()

        pendIntent = PendingIntent.getBroadcast(
            this,
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

        //注册用于关闭服务的receiver
        val intentFilter = IntentFilter()
        intentFilter.addAction(CHANNEL_ID)
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(receiver, intentFilter)

        scrolls.add(receiveEvent<ParamTarget>(EventTag.TYPE_RECEIVER + EventTag.DISCONNECT) {
            //TODO:去除目标设备
            stopSelf()
        })
        scrolls.add(receiveEvent<ParamUnit>(EventTag.STOP_SERVICE) {
            stopSelf()
        })
    }

    inner class LocalBinder : Binder() {
        val service: EarbudService
            get() = this@EarbudService
    }

    private val binder = LocalBinder()

    @SuppressLint("RestrictedApi")
    override fun onBind(intent: Intent?): IBinder? {
        notificationBuilder.mActions.clear()
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())

        bleScanner.setBind()
        bleAdvertiser.isBind = true
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        CoroutineScope(Dispatchers.Default).launch {
            delay(2000)
            val device: BluetoothDevice =
                intent!!.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)!!
            when (intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1)) {
                BluetoothA2dp.STATE_CONNECTED -> {
                    sendEvent(ParamDevices(listOf(device.address)), EventTag.ADVERTISE)
                    Log.d("TAG", "sendAdvertise")
                }
                BluetoothA2dp.STATE_CONNECTING -> {
                    sendEvent(ParamDevices(listOf(device.address)), EventTag.SCAN)
                    Log.d("TAG", "sendScan")
                }
                else -> {
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        stopSelf()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        earbudManager.unregister()
        bleScanner.unregister()
        bleAdvertiser.unregister()

        for (scope in scrolls) {
            scope.cancel()
        }

        try {
            unregisterReceiver(receiver)
        } catch (ignored: IllegalArgumentException) {
        }
        stopForeground(true)
        super.onDestroy()
    }

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
            Log.d("TAG", intent.action.toString())
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