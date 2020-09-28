package app.tuuure.earbudswitch.earbuds

import android.bluetooth.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import app.tuuure.earbudswitch.CancelAdvertiseEvent
import app.tuuure.earbudswitch.utils.Preferences
import org.greenrobot.eventbus.EventBus

class EarbudReceiver : BroadcastReceiver() {
    fun startService(context: Context?, device: BluetoothDevice, state: Int) {
        val service = Intent(context, EarbudService::class.java)
        service.putExtra(BluetoothDevice.EXTRA_DEVICE, device.address)
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
        if (BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED == intent.action || BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED == intent.action) {
            val state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1)
            when (state) {
                BluetoothProfile.STATE_CONNECTED -> {
                    if (!Preferences.getInstance(context!!).checkRestricted(device.address)) {
                        //Log.d("TAG", "CONNECTED")
                        // 已连接到设备，开始广播
                        startService(context, device, state)
                    }
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    //Log.d("TAG", "DISCONNECTED")
                    EventBus.getDefault().post(CancelAdvertiseEvent(device.address))
                }
            }
        }
    }
}