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
import app.tuuure.earbudswitch.utils.KeyQRCode
import app.tuuure.earbudswitch.utils.KeyQRCode.Companion.createQRCode
import app.tuuure.earbudswitch.recyclerList.ListAdapter
import app.tuuure.earbudswitch.R
import app.tuuure.earbudswitch.utils.SPreferences
import app.tuuure.earbudswitch.earbuds.EarbudManager.Companion.getAudioDevices
import com.king.zxing.CaptureActivity
import com.king.zxing.Intents
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import java.util.*


class MainActivity : AppCompatActivity() {
    companion object {
        private const val REQUEST_CODE_WELCOME = 9
        private const val REQUEST_CODE_PERMISSION = 10
        private const val REQUEST_CODE_SCAN = 12
    }

    private lateinit var key: String
    private lateinit var adapter: ListAdapter

    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action ?: return
            if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)
                if (state == BluetoothAdapter.STATE_ON) {
                    CoroutineScope(Dispatchers.IO).launch {
                        adapter.updateData(getAudioDevices())
                    }
                    turnOnHint(true)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initView()
        setSupportActionBar(toolbar)
    }

    override fun onResume() {

        CoroutineScope(Dispatchers.IO).launch {
            key = SPreferences.getKey()
            if (key.isEmpty()) {
                val intent = Intent()
                intent.setClass(this@MainActivity, IntroActivity::class.java)
                startActivityForResult(intent, REQUEST_CODE_WELCOME)
            } else if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED
                || checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED
            ) {
                val intent = Intent()
                intent.setClass(this@MainActivity, IntroActivity::class.java)
                startActivityForResult(intent, REQUEST_CODE_PERMISSION)
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
                SPreferences.putRestrictMode(
                    if (mode == getString(R.string.filter_mode_allow))
                        SPreferences.RestrictMode.ALLOW
                    else
                        SPreferences.RestrictMode.BLOCK
                )
                CoroutineScope(Dispatchers.IO).launch {
                    adapter.updateData(getAudioDevices())
                }
            }

            override fun onNothingSelected(adapterView: AdapterView<*>?) {}
        }

        adapter = ListAdapter(this);
        rc_devices.layoutManager = LinearLayoutManager(this)
        rc_devices.adapter = adapter

        CoroutineScope(Dispatchers.IO).launch {
            val modeId =
                if (SPreferences.getRestrictMode() == SPreferences.RestrictMode.ALLOW.toString()) R.string.filter_mode_allow else R.string.filter_mode_block
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
                adapter.updateData(getAudioDevices())
                withContext(Dispatchers.Main) {
                    swipeRefresh.isRefreshing = false
                }
            }
        }

        toolbar.setOnLongClickListener {
            val intent = Intent()
            intent.setClass(this, TestActivity::class.java)
            startActivity(intent)
            true
        }

        buttonCamera.setOnClickListener {
            startActivityForResult(
                Intent(this, CaptureActivity::class.java),
                REQUEST_CODE_SCAN
            )
        }

        buttonRefresh.setOnClickListener {
            key = UUID.randomUUID().toString()
            updateQRCode()
            SPreferences.putKey(key)
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
            val bitmap = createQRCode(key, nightMode, 1200)
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
        Toast.makeText(this, "Add", Toast.LENGTH_SHORT).show();
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
                        SPreferences.putKey(key)
                    }
                }
                REQUEST_CODE_WELCOME -> {
                    key = UUID.randomUUID().toString()
                    SPreferences.putKey(key)
                    updateQRCode()
                }
            }
        }
    }
}