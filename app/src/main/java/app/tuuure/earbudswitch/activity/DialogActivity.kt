package app.tuuure.earbudswitch.activity

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import app.tuuure.earbudswitch.R
import app.tuuure.earbudswitch.ScanResultEvent
import app.tuuure.earbudswitch.earbuds.EarbudManager
import app.tuuure.earbudswitch.nearby.ble.BleScanner
import app.tuuure.earbudswitch.recyclerList.ListItem
import app.tuuure.earbudswitch.recyclerList.ScanListAdapter
import app.tuuure.earbudswitch.utils.ComponentEnableSettings
import app.tuuure.earbudswitch.utils.Preferences
import kotlinx.android.synthetic.main.activity_dialog.*
import org.greenrobot.eventbus.Subscribe
import java.util.*

class DialogActivity : AppCompatActivity() {
    companion object {
        const val REQUEST_CODE_WELCOME = 9
        const val KEY_EXTRA = "keyExtra"
    }

    private lateinit var key: String
    private lateinit var adapter: ScanListAdapter
    private lateinit var bleScanner: BleScanner
    private lateinit var earbudManager: EarbudManager
    private val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action ?: return
            if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)
                if (state == BluetoothAdapter.STATE_ON) {
                    initObject()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dialog)

        setSupportActionBar(tb_dialog)
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT));

        adapter = ScanListAdapter(this);
        rv_dialog.layoutManager = LinearLayoutManager(this)
        rv_dialog.adapter = adapter
    }

    private fun turnOnHint(isEnabled: Boolean) {
        rv_dialog.visibility = if (isEnabled) View.VISIBLE else View.GONE
        bth_off_image.visibility = if (isEnabled) View.GONE else View.VISIBLE
        bth_off_text.visibility = if (isEnabled) View.GONE else View.VISIBLE
    }

    fun initObject() {
        key = Preferences.getInstance(this).getKey()
        if (key.isEmpty()
            || checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED
            || checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED
        ) {
            ComponentEnableSettings.setEnableSettings(this, false)
            val intent = Intent(this, IntroActivity::class.java)
            startActivity(intent)
            return
        } else {
            ComponentEnableSettings.setEnableSettings(this, true)
        }

        if (!this@DialogActivity::bleScanner.isInitialized) {
            bleScanner = BleScanner(this, key)
        }
        if (!this@DialogActivity::earbudManager.isInitialized) {
            earbudManager = EarbudManager(this)
        }

        val data: LinkedList<ListItem> = LinkedList()
        val devices = EarbudManager.getAudioDevices()
        for (d in devices.entries) {
            data.add(ListItem(d.key, d.value))
        }
        adapter.updateData(data)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu_dialog, menu)
        val menuSettings = menu!!.findItem(R.id.app_bar_settings)
        menuSettings.setOnMenuItemClickListener {
            val intent = Intent(this, SettingsActivity::class.java).apply {
                putExtra(SettingsActivity.KEY_EXTRA, key)
            }
            startActivity(intent)
            return@setOnMenuItemClickListener true
        }
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if ((resultCode == RESULT_OK) and (requestCode == REQUEST_CODE_WELCOME)) {
            key = UUID.randomUUID().toString()
            Preferences.getInstance(this).putKey(key)
            val intent = Intent(this@DialogActivity, SettingsActivity::class.java).apply {
                putExtra(KEY_EXTRA, key)
            }
            startActivity(intent)
        }
    }

//    @Subscribe
//    fun addServer(resultEvent: ScanResultEvent) {
//        if (adapter.data.isNotEmpty()) {
//            adapter.setServer(resultEvent.server, resultEvent.devices)
//        }
//    }

    override fun onResume() {
        val intentFilter = IntentFilter()
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(receiver, intentFilter)

        super.onResume()
    }

    override fun onPause() {
        try {
            if (this@DialogActivity::bleScanner.isInitialized) {
                bleScanner.stopScan()
            }
            unregisterReceiver(receiver)
        } catch (ignored: IllegalArgumentException) {
        }
        super.onPause()
    }
}