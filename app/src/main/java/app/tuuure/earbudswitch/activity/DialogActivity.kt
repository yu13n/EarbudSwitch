package app.tuuure.earbudswitch.activity

import android.Manifest
import android.bluetooth.*
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
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import app.tuuure.earbudswitch.*
import app.tuuure.earbudswitch.earbuds.EarbudManager
import app.tuuure.earbudswitch.nearby.ble.BleConnecter
import app.tuuure.earbudswitch.nearby.ble.BleScanner
import app.tuuure.earbudswitch.recyclerList.ListItem
import app.tuuure.earbudswitch.recyclerList.ScanListAdapter
import app.tuuure.earbudswitch.utils.ComponentEnableSettings
import app.tuuure.earbudswitch.utils.CryptoConvert.Companion.bytesToUUID
import app.tuuure.earbudswitch.utils.CryptoConvert.Companion.randomBytes
import app.tuuure.earbudswitch.utils.Preferences
import kotlinx.android.synthetic.main.activity_dialog.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

class DialogActivity : AppCompatActivity() {
    companion object {
        const val REQUEST_CODE_WELCOME = 9
        const val KEY_EXTRA = "keyExtra"
    }

    private lateinit var key: String
    private lateinit var adapter: ScanListAdapter
    private lateinit var bleScanner: BleScanner
    private lateinit var bleConnecter: BleConnecter
    private val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action ?: return
            when (action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)
                    turnOnHint(state == BluetoothAdapter.STATE_ON)
                    if (state == BluetoothAdapter.STATE_ON) {
                        initObject()
                    }
                }
                BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED, BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1)
                    val device: BluetoothDevice =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)!!
                    EventBus.getDefault().post(
                        RefreshEvent(
                            device.address,
                            state == BluetoothProfile.STATE_CONNECTING
                        )
                    )
                    if (state == BluetoothProfile.STATE_CONNECTED || state == BluetoothProfile.STATE_DISCONNECTED)
                        updateSelected()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dialog)

        setSupportActionBar(tb_dialog)
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val clickListener = View.OnClickListener {
            BluetoothAdapter.getDefaultAdapter().enable()
            Toast.makeText(this, getString(R.string.toast_bth_on), Toast.LENGTH_SHORT).show()
        }

        bth_off_image.setOnClickListener(clickListener)
        bth_off_text.setOnClickListener(clickListener)

        adapter = ScanListAdapter(this)
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
            startActivityForResult(intent, REQUEST_CODE_WELCOME)
        } else {
            ComponentEnableSettings.setEnableSettings(this, true)
        }

        turnOnHint(bluetoothAdapter.isEnabled)

        if (!bluetoothAdapter.isEnabled) {
            return
        }
        if (!this@DialogActivity::bleScanner.isInitialized) {
            bleScanner = BleScanner(this, key)
        }
        if (!this@DialogActivity::bleConnecter.isInitialized) {
            bleConnecter = BleConnecter(this, key)
        }

        val devices = EarbudManager.getAudioDevices()
        val data: LinkedHashSet<ListItem> = LinkedHashSet(devices.size)
        for (d in devices) {
            data.add(ListItem(d.name, d.address))
        }
        adapter.updateData(data)

        bleScanner.scan(devices)

        updateSelected()
    }

    private fun updateSelected() {
        val proxyListener = object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
                proxy!!.connectedDevices.also {
                    val devices = ArrayList<String>(it.size)
                    it.forEach {
                        devices.add(it.address)
                    }
                    adapter.setSelected(profile, devices)
                }
                BluetoothAdapter.getDefaultAdapter().closeProfileProxy(profile, proxy)
            }

            override fun onServiceDisconnected(profile: Int) {}
        }
        bluetoothAdapter.getProfileProxy(this, proxyListener, BluetoothProfile.A2DP)
        bluetoothAdapter.getProfileProxy(this, proxyListener, BluetoothProfile.HEADSET)
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
            ComponentEnableSettings.setEnableSettings(this, true)
            key = bytesToUUID(randomBytes(16)).toString()
            Preferences.getInstance(this).putKey(key)
            val intent = Intent(this@DialogActivity, SettingsActivity::class.java).apply {
                putExtra(KEY_EXTRA, key)
            }
            startActivity(intent)
        }
    }

    @Subscribe
    fun onSetFreshEvent(paramEvent: RefreshEvent) {
        adapter.setFresh(paramEvent.device, paramEvent.isFreshing)
    }

    @Subscribe
    fun scanForServer(paramEvent: ScanEvent) {
        if (this@DialogActivity::bleScanner.isInitialized) {
            bleScanner.scan(paramEvent.devices)
        }
    }

    @Subscribe
    fun connectGatt(paramEvent: ConnectGattEvent) {
        if (this@DialogActivity::bleConnecter.isInitialized) {
            bleScanner.stopScan()
            bleConnecter.connect(paramEvent.server, paramEvent.device)
        }
    }

    @Subscribe
    fun onScanResult(resultEvent: ScanResultEvent) {
        if (adapter.data.isNotEmpty()) {
            adapter.setServer(
                if (resultEvent.isFound) resultEvent.server else "",
                resultEvent.devices
            )
        }
    }

    override fun onResume() {
        super.onResume()

        registerReceiver(receiver, IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
            addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
        })
        EventBus.getDefault().register(this)

        initObject()
    }

    override fun onPause() {

        EventBus.getDefault().unregister(this)

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