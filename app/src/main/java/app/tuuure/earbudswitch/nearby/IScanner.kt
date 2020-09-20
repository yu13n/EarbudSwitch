package app.tuuure.earbudswitch.nearby

import android.content.Context

interface IScanner {
    val context: Context
    var key: String

    fun scan(devices: Collection<String>)
    fun stopScan()
}