package app.tuuure.earbudswitch.activity

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import app.tuuure.earbudswitch.R
import app.tuuure.earbudswitch.earbuds.EarbudManager.Companion.getAudioDevices
import app.tuuure.earbudswitch.recyclerList.FilterListAdapter
import app.tuuure.earbudswitch.recyclerList.ListItem
import app.tuuure.earbudswitch.utils.ComponentEnableSettings
import app.tuuure.earbudswitch.utils.CryptoConvert.Companion.bytesToUUID
import app.tuuure.earbudswitch.utils.CryptoConvert.Companion.randomBytes
import app.tuuure.earbudswitch.utils.KeyQRCode
import app.tuuure.earbudswitch.utils.KeyQRCode.Companion.createQRCode
import app.tuuure.earbudswitch.utils.Preferences
import com.king.zxing.CaptureActivity
import com.king.zxing.Intents
import kotlinx.android.synthetic.main.activity_settings.*
import kotlinx.coroutines.*
import java.util.*


class SettingsActivity : AppCompatActivity() {
    companion object {
        const val REQUEST_CODE_WELCOME = 9
        const val KEY_EXTRA = "keyExtra"
        private const val REQUEST_CODE_SCAN = 12
    }

    private var key: String? = null
    private lateinit var adapterFilter: FilterListAdapter

    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action ?: return
            if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)
                if (state == BluetoothAdapter.STATE_ON) {
                    updateList()
                    turnOnHint(true)
                }
            }
        }
    }

    private fun updateList() {
        CoroutineScope(Dispatchers.IO).launch {
            val data: LinkedList<ListItem> = LinkedList()
            val devices = getAudioDevices()
            val restrictItems = Preferences.getInstance(this@SettingsActivity).getRestrictItem()
            for (d in devices) {
                val isChecked = restrictItems.contains(d.address)
                if (isChecked) {
                    data.addFirst(ListItem(d.name, d.address, isChecked))
                } else {
                    data.addLast(ListItem(d.name, d.address, isChecked))
                }
            }
            withContext(Dispatchers.Main){
                adapterFilter.updateData(data)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        key = intent.getStringExtra(KEY_EXTRA)
        initView()
        setSupportActionBar(toolbar)
    }

    override fun onResume() {
        CoroutineScope(Dispatchers.IO).launch {
            if (key.isNullOrEmpty())
                key = Preferences.getInstance(this@SettingsActivity).getKey()
            if (key!!.isEmpty()
                || checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED
                || checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED
            ) {
                ComponentEnableSettings.setEnableSettings(this@SettingsActivity, false)
                val intent = Intent()
                intent.setClass(this@SettingsActivity, IntroActivity::class.java)
                startActivityForResult(intent, REQUEST_CODE_WELCOME)
            } else {
                updateQRCode()
            }
        }

        val isEnabled: Boolean = BluetoothAdapter.getDefaultAdapter().isEnabled
        turnOnHint(isEnabled)

        val intentFilter = IntentFilter()
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(receiver, intentFilter)
        super.onResume()
    }

    override fun onPause() {
        try {
            unregisterReceiver(receiver)
        } catch (ignored: IllegalArgumentException) {
        }
        super.onPause()
    }

    private fun initView() {
        filterModeSpinner.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(
                adapterView: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                val mode = adapterView.getItemAtPosition(position).toString()
                Preferences.getInstance(this@SettingsActivity).putRestrictMode(
                    if (mode == getString(R.string.filter_mode_allow))
                        Preferences.RestrictMode.ALLOW
                    else
                        Preferences.RestrictMode.BLOCK
                )
                updateList()
            }

            override fun onNothingSelected(adapterView: AdapterView<*>?) {}
        }

        adapterFilter = FilterListAdapter(this)
        rc_devices.layoutManager = LinearLayoutManager(this)
        rc_devices.adapter = adapterFilter

        CoroutineScope(Dispatchers.IO).launch {
            val modeId =
                if (Preferences.getInstance(this@SettingsActivity)
                        .getRestrictMode() == Preferences.RestrictMode.ALLOW.toString()
                ) R.string.filter_mode_allow else R.string.filter_mode_block
            val mode = getString(modeId)
            withContext(Dispatchers.Main) {
                for (i in filterModeSpinner.count - 1 downTo 0) {
                    if (mode == filterModeSpinner.getItemAtPosition(i)) {
                        filterModeSpinner.setSelection(i, false)
                        break
                    }
                }
            }
        }

        swipeRefresh.setOnRefreshListener {
            CoroutineScope(Dispatchers.IO).launch {
                updateList()
                withContext(Dispatchers.Main) {
                    swipeRefresh.isRefreshing = false
                }
            }
        }

        buttonCamera.setOnClickListener {
            startActivityForResult(
                Intent(this, CaptureActivity::class.java),
                REQUEST_CODE_SCAN
            )
        }

        buttonRefresh.setOnClickListener {
            key = bytesToUUID(randomBytes(16)).toString()
            updateQRCode()
            Preferences.getInstance(this@SettingsActivity).putKey(key)
        }

        val clickListener = View.OnClickListener {
            BluetoothAdapter.getDefaultAdapter().enable()
            Toast.makeText(this, getString(R.string.toast_bth_on), Toast.LENGTH_SHORT).show()
        }

        bth_off_image.setOnClickListener(clickListener)
        bth_off_text.setOnClickListener(clickListener)
    }

    private fun turnOnHint(isEnabled: Boolean) {
        swipeRefresh.visibility = if (isEnabled) View.VISIBLE else View.GONE
        bth_off_image.visibility = if (isEnabled) View.GONE else View.VISIBLE
        bth_off_text.visibility = if (isEnabled) View.GONE else View.VISIBLE
    }

    private fun updateQRCode() {
        GlobalScope.launch(Dispatchers.Default) {
            val nightMode =
                Configuration.UI_MODE_NIGHT_YES == resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            val bitmap = createQRCode(key!!, nightMode, 1200)
            withContext(Dispatchers.Main) {
                text_key.text = key
                image_qr_code.setImageBitmap(bitmap)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_about) {
            Toast.makeText(this, "About", Toast.LENGTH_SHORT).show()
        }
        return super.onOptionsItemSelected(item)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                REQUEST_CODE_SCAN -> {
                    val result = data!!.getStringExtra(Intents.Scan.RESULT)!!
                    val content: String = KeyQRCode.deprefix(result)
                    if (key == content) {
                        Toast.makeText(this, getString(R.string.toast_same_key), Toast.LENGTH_LONG)
                            .show()
                    } else {
                        key = content
                        updateQRCode()
                        Preferences.getInstance(this@SettingsActivity).putKey(key)
                    }
                }
                REQUEST_CODE_WELCOME -> {
                    key = bytesToUUID(randomBytes(16)).toString()
                    Preferences.getInstance(this@SettingsActivity).putKey(key)
                    updateQRCode()
                }
            }
        }
    }
}