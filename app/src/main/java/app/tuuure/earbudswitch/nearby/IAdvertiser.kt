package app.tuuure.earbudswitch.nearby

import android.bluetooth.BluetoothDevice
import android.content.Context

interface IAdvertiser {
    val context: Context
    var key: String

    fun advertise(devices: Collection<BluetoothDevice>)
    fun stopAdvertise()
}