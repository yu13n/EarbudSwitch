package app.tuuure.earbudswitch

import android.bluetooth.BluetoothDevice

data class ScanEvent(val devices: Collection<BluetoothDevice>)

data class ScanResultEvent(
    val server: String,
    val devices: Collection<String>,
    val isFound: Boolean = true
)

data class DisconnectEvent(val device: String)

data class CancelAdvertiseEvent(val device: String)

data class ConnectGattEvent(val server: String, val device: String)

data class RefreshEvent(val device: String, val isFreshing: Boolean)