package app.tuuure.earbudswitch.nearby

import android.content.Context

interface IAdvertiser {
    val context: Context
    var key: String

    fun advertise(devices: Collection<String>)
    fun stopAdvertise()
}