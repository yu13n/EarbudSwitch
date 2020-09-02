package app.tuuure.earbudswitch

import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONException
import java.util.*

object SPreferences {
    private const val SP_SETTINGS_NAME = "Settings"
    private val sp: SharedPreferences = EBSApp.getContext().getSharedPreferences(
            SP_SETTINGS_NAME,
            MODE_PRIVATE
    );
    private var editor: SharedPreferences.Editor = sp.edit()

    private const val KEY_NAME = "key"

    fun putKey(key: String?) {
        CoroutineScope(Dispatchers.IO).launch {
            editor.putString(KEY_NAME, key)
            editor.apply()
        }
    }

    suspend fun getKey(): String =
            sp.getString(KEY_NAME, "") as String

    private const val RESTRICT_MODE_NAME = "restrict"

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

    suspend fun addRestrictItem(address: MutableCollection<String>) {
        val list = HashSet(getRestrictItem())
        list.addAll(address)
        putRestrictItem(list)
    }

    suspend fun delRestrictItem(address: MutableCollection<String>) {
        val list = HashSet(getRestrictItem())
        list.removeAll(address)
        putRestrictItem(list)
    }

    fun getRestrictItem(): MutableSet<String> {
        return getRestrictItem(getRestrictMode().toLowerCase(Locale.ROOT))
    }

    private fun getRestrictItem(restrictList: String): MutableSet<String> =
            sp.getStringSet(restrictList, HashSet()) as MutableSet<String>

    fun checkRestricted(address: String): Boolean =
            (getRestrictMode() == RestrictMode.ALLOW.toString()) xor (address in getRestrictItem())

    private const val TWS_NAME = "twsDevices"

    fun getTwsDevices(): JSONArray =
            try {
                JSONArray(sp.getString(TWS_NAME, ""))
            } catch (e: JSONException) {
                JSONArray()
            }

    suspend fun putTwsDevices(jsonArray: JSONArray) {
        CoroutineScope(Dispatchers.IO).launch {
            editor.putString(TWS_NAME, jsonArray.toString())
            editor.apply()
        }
    }
}