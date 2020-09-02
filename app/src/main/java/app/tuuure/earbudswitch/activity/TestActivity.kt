package app.tuuure.earbudswitch.activity

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothProfile
import android.content.*
import android.content.res.Configuration
import android.os.Bundle
import android.os.IBinder
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.Lifecycle
import app.tuuure.earbudswitch.*
import app.tuuure.earbudswitch.earbuds.EarbudManager
import app.tuuure.earbudswitch.earbuds.EarbudService
import com.drake.channel.receiveEvent
import com.drake.channel.sendEvent
import kotlinx.android.synthetic.main.activity_test.*
import kotlin.properties.Delegates

class TestActivity : AppCompatActivity() {
    private var service: EarbudService? = null
    private var isBind = false

    private var conn = object : ServiceConnection {
        override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
            isBind = true
            val myBinder = p1 as EarbudService.LocalBinder
            service = myBinder.service
            setCardClickable(listOf(step_choose), true)
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            step_stop.performClick()
            isBind = false
        }
    }

    private var target: String = ""

    @SuppressLint("SetTextI18n")
    private val clickListener = View.OnClickListener { view ->
        setCardClickable(listOf(view as CardView), false)

        when (view) {
            step_start -> {
                val intent = Intent(this, EarbudService::class.java)
                bindService(intent, conn, Context.BIND_AUTO_CREATE)
            }
            step_choose -> {
                val devices = EarbudManager.getAudioDevices()
                val names = devices.keys.toTypedArray()
                var target: String = devices.get(names[0])!!
                val dialogBuilder = AlertDialog.Builder(this)
                    .setSingleChoiceItems(
                        names, 0
                    ) { _, which ->
                        target = devices[names[which]]!!
                    }
                    .setPositiveButton(
                        getString(R.string.test_choose)
                    ) { _, _ ->
                        this@TestActivity.target = target
                        val device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(target)
                        text_choose.text = "${getString(R.string.test_choose)}\n${device.name}\n" +
                                device.address
                        if (BluetoothAdapter.getDefaultAdapter()
                                .getProfileConnectionState(BluetoothProfile.A2DP) == BluetoothProfile.STATE_CONNECTED
                        ) {
                            setCardClickable(listOf(step_advertise), true)
                        } else {
                            setCardClickable(listOf(step_scan), true)
                        }
                    }
                dialogBuilder.show()
            }
            step_scan -> {
                sendEvent(ParamDevices(setOf(target)), EventTag.SCAN)
            }
            step_connect_gatt -> {
                sendEvent(paramServer, EventTag.CONNECT_GATT)
            }
            step_write -> {
                sendEvent(ParamUnit(), EventTag.WRITE)
            }
            step_connect -> {
                sendEvent(ParamTarget(target), EventTag.CONNECT)
                val intentFilter = IntentFilter()
                intentFilter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
                registerReceiver(receiver, intentFilter)
            }
            step_advertise -> {
                sendEvent(ParamDevices(setOf(target)), EventTag.ADVERTISE)
            }
            step_written -> {
                //setCardClickable(setOf(step_disconnect), true)
            }
            step_disconnect -> {
                sendEvent(ParamUnit(), EventTag.DISCONNECT)
            }
            step_stop -> {
                if (isBind)
                    unbindService(conn)
                isBind = false
                try {
                    unregisterReceiver(receiver)
                } catch (ignored: IllegalArgumentException) {
                }
            }
        }
    }

    private var nightMode by Delegates.notNull<Boolean>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)
    }

    private lateinit var paramServer: ParamServer

    @SuppressLint("SetTextI18n")
    override fun onResume() {
        super.onResume()

        nightMode =
            Configuration.UI_MODE_NIGHT_YES == resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK

        setCardClickable(listOf(step_start), true)

        receiveEvent<ParamServer>(
            lifecycleEvent = Lifecycle.Event.ON_PAUSE,
            tags = arrayOf(EventTag.TYPE_RESULT + EventTag.SCAN)
        ) {
            paramServer = it
            //text_scan.text = "${getString(R.string.test_scan)}\n${it.server}"
            text_scan.text = getString(R.string.test_scan) + it.server
            setCardClickable(listOf(step_connect_gatt), true)
        }
        receiveEvent<ParamString>(
            lifecycleEvent = Lifecycle.Event.ON_PAUSE,
            tags = arrayOf(EventTag.TYPE_RESULT + EventTag.WRITE)
        ) {
            text_write.text = "${getString(R.string.test_write)}\n${it.content}"
            setCardClickable(listOf(step_connect), true)
        }
        receiveEvent<ParamString>(
            lifecycleEvent = Lifecycle.Event.ON_PAUSE,
            tags = arrayOf(EventTag.TYPE_RESULT + EventTag.WRITTEN)
        ) {
            text_written.text = "${getString(R.string.test_written)}\n${it.content}"
            setCardClickable(listOf(step_disconnect), true)
        }
    }


    @SuppressLint("PrivateResource")
    private fun setCardClickable(views: List<CardView>, b: Boolean) {
        val listener: View.OnClickListener?
        val bgColor: Int
        if (b) {
            bgColor = getColor(R.color.cardview_clicked_background)
            listener = clickListener
        } else {
            listener = null
            if (nightMode) {
                bgColor = getColor(androidx.cardview.R.color.cardview_dark_background)
            } else {
                bgColor = getColor(androidx.cardview.R.color.cardview_light_background)
            }
        }
        for (v in views) {
            v.setCardBackgroundColor(bgColor)
            v.setOnClickListener(listener)
            v.isClickable = b
        }
    }

    override fun onPause() {
        step_stop.performClick()
        super.onPause()
    }

    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED) {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)
                if (state == BluetoothAdapter.STATE_CONNECTED) {
                    setCardClickable(listOf(step_advertise), true)
                    unregisterReceiver(this)
                }
            }
        }
    }
}