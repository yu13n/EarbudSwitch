package app.tuuure.earbudswitch.earbuds

import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import app.tuuure.earbudswitch.EventTag
import app.tuuure.earbudswitch.ParamTarget
import app.tuuure.earbudswitch.utils.SPreferences
import com.drake.channel.sendEvent

class EarbudReceiver : BroadcastReceiver() {
    fun startService(context: Context?, device: BluetoothDevice, state: Int) {
        val service = Intent(context, EarbudService::class.java)
        service.putExtra(BluetoothDevice.EXTRA_DEVICE, device)
        service.putExtra(BluetoothProfile.EXTRA_STATE, state)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            context!!.startForegroundService(service)
        else
            context!!.startService(service)
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        val device: BluetoothDevice? = intent?.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        if (device == null || device.name == null || device.name.isEmpty() || device.bluetoothClass.majorDeviceClass != BluetoothClass.Device.Major.AUDIO_VIDEO)
            return
        if (BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED == intent.action) {
            val state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1)
            when (state) {
                BluetoothA2dp.STATE_CONNECTED -> {
                    if (!SPreferences.checkRestricted(device.address)) {
                        startService(context, device, state)
                        Log.d("TAG","startService")
                    }
                }
                BluetoothA2dp.STATE_CONNECTING -> {
                    startService(context, device, state)
                }
                BluetoothA2dp.STATE_DISCONNECTED -> {
                    sendEvent(
                        ParamTarget(device.address),
                        EventTag.TYPE_RESULT + EventTag.DISCONNECT
                    )
                }
            }
        }
    }
}