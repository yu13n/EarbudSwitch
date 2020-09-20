package app.tuuure.earbudswitch.nearby

import android.content.Context

interface IConnecter {
    val context: Context
    var key: String

    fun connect(server: String, device: String)
}