package app.tuuure.earbudswitch.utils

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONException
import java.util.*

class Preferences private constructor(context: Context) {
    private val SP_SETTINGS_NAME = "Settings"
    private val sp: SharedPreferences = context.applicationContext.getSharedPreferences(
        SP_SETTINGS_NAME,
        MODE_PRIVATE
    )
    private var editor: SharedPreferences.Editor = sp.edit()

    companion object : SingletonHolder<Preferences, Context>(::Preferences)

    private val KEY_NAME = "key"

    fun putKey(key: String?) {
        CoroutineScope(Dispatchers.IO).launch {
            editor.putString(KEY_NAME, key)
            editor.apply()
        }
    }

    fun getKey(): String =
        sp.getString(KEY_NAME, "") as String

    private val RESTRICT_MODE_NAME = "restrict"

    enum class RestrictMode {
        ALLOW, //ALLOW
        BLOCK  //BLOCK
    }

    fun putRestrictMode(restrictMode: RestrictMode) {
        CoroutineScope(Dispatchers.IO).launch {
            editor.putString(RESTRICT_MODE_NAME, restrictMode.toString())
            editor.apply()
        }
    }

    fun getRestrictMode(): String =
        try {
            RestrictMode.valueOf(sp.getString(RESTRICT_MODE_NAME, "").toString()).toString()
        } catch (e: IllegalArgumentException) {
            RestrictMode.BLOCK.toString()
        }

    private fun putRestrictItem(address: MutableSet<String>) {
        editor.putStringSet(getRestrictMode().toLowerCase(Locale.ROOT), address)
        editor.apply()
    }

    fun addRestrictItem(address: String) {
        val list = HashSet(getRestrictItem())
        list.add(address)
        putRestrictItem(list)
    }

    fun delRestrictItem(address: String) {
        val list = HashSet(getRestrictItem())
        list.remove(address)
        putRestrictItem(list)
    }

    fun getRestrictItem(): MutableSet<String> {
        return getRestrictItem(getRestrictMode().toLowerCase(Locale.ROOT))
    }

    private fun getRestrictItem(restrictList: String): MutableSet<String> =
        sp.getStringSet(restrictList, HashSet()) as MutableSet<String>

    fun checkRestricted(address: String): Boolean =
        (getRestrictMode() == RestrictMode.ALLOW.toString()) xor (address in getRestrictItem())

    private val TWS_NAME = "twsDevices"

    fun getTwsDevices(): JSONArray =
        try {
            JSONArray(sp.getString(TWS_NAME, ""))
        } catch (e: JSONException) {
            JSONArray()
        }

    fun putTwsDevices(jsonArray: JSONArray) {
        CoroutineScope(Dispatchers.IO).launch {
            editor.putString(TWS_NAME, jsonArray.toString())
            editor.apply()
        }
    }
}
