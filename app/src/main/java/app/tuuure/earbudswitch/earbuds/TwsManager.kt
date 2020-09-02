package app.tuuure.earbudswitch.earbuds

import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Build
import app.tuuure.earbudswitch.SPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import java.lang.reflect.Method

class TwsManager(context: Context) {
    /*
    public static final int SOURCE_CODEC_TYPE_SBC = 0;
    public static final int SOURCE_CODEC_TYPE_AAC = 1;
    public static final int SOURCE_CODEC_TYPE_APTX = 2;
    public static final int SOURCE_CODEC_TYPE_APTX_HD = 3;
    public static final int SOURCE_CODEC_TYPE_LDAC = 4;
    public static final int SOURCE_CODEC_TYPE_MAX = 5;
    public static final int CHANNEL_MODE_NONE = 0;
    public static final int CHANNEL_MODE_MONO = 0x1 << 0;
    public static final int CHANNEL_MODE_STEREO = 0x1 << 1;
 */
    private lateinit var BluetoothCodecConfig: Class<*>
    private var SOURCE_CODEC_TYPE_APTX_TWSP: Int = 0
    private lateinit var BluetoothCodecStatus: Class<*>
    private lateinit var getCodecConfig: Method
    private lateinit var getCodecStatus: Method
    private lateinit var getCodecTypegetCodecType: Method
    private lateinit var getChannelMode: Method
    private var CHANNEL_MODE_MONO: Int = 0

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothA2dp: BluetoothA2dp
    var isSupported = true

    fun isTwsDevice(devices: Collection<BluetoothDevice>): Boolean {
        if (!isSupported)
            return false
        else {
            for (d in devices) {
                val codecStatus = getCodecStatus.invoke(bluetoothA2dp, d)
                val codecConfig = getCodecConfig.invoke(codecStatus)
                val codecType = getCodecTypegetCodecType.invoke(codecConfig) as Int
                val channelMode = getChannelMode.invoke(codecConfig) as Int
                if (channelMode != CHANNEL_MODE_MONO || codecType != SOURCE_CODEC_TYPE_APTX_TWSP) {
                    return false
                }
            }
            return true
        }
    }

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            try {
                BluetoothCodecConfig = Class.forName("android.bluetooth.BluetoothCodecConfig")
                for (field in BluetoothCodecConfig.declaredFields) {
                    val name = field.name
                    if (name.contains("APTX") && name.contains("TWS")) {
                        SOURCE_CODEC_TYPE_APTX_TWSP = field.getInt(BluetoothCodecConfig)
                        break
                    }
                }

                getCodecStatus = BluetoothA2dp::class.java.getDeclaredMethod(
                    "getCodecStatus",
                    BluetoothDevice::class.java
                )
                BluetoothCodecStatus = Class.forName("android.bluetooth.BluetoothCodecStatus")
                getCodecConfig = BluetoothCodecStatus.getDeclaredMethod("getCodecConfig")
                getCodecTypegetCodecType = BluetoothCodecConfig.getDeclaredMethod("getCodecType")
                getChannelMode = BluetoothCodecConfig.getDeclaredMethod("getChannelMode")
                CHANNEL_MODE_MONO = BluetoothCodecConfig.getDeclaredField("CHANNEL_MODE_MONO")
                    .getInt(BluetoothCodecConfig)

            } catch (e: ClassNotFoundException) {
                isSupported = false
            } catch (e: IllegalAccessException) {
                isSupported = false
            }
            if (isSupported) {
                val proxyListener = object : BluetoothProfile.ServiceListener {
                    override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
                        if (profile == BluetoothProfile.A2DP) bluetoothA2dp = proxy as BluetoothA2dp
                    }

                    override fun onServiceDisconnected(profile: Int) {
                    }
                }
                bluetoothAdapter.getProfileProxy(context, proxyListener, BluetoothProfile.A2DP)
            }
        } else {
            isSupported = false
        }
    }

    fun isContains(
        array: String,
        devices: Collection<BluetoothDevice>
    ): Collection<BluetoothDevice> = devices.filter { array.contains(it.address) }

    fun getTwsDevice(device: BluetoothDevice): BluetoothDevice? {
        val array: JSONArray = SPreferences.getTwsDevices()
        if (isContains(array.toString(), setOf(device)).isNotEmpty()) return null

        var address: String? = null
        for (i in array.length() - 1 downTo 0) {
            try {
                val item = array.getJSONArray(i)
                if (item.toString().contains(device.address)) {
                    for (k in array.length() - 1 downTo 0) {
                        address = item.getString(k)
                        if (device.address != address) {
                            break
                        }
                    }
                    break
                }
            } catch (ignored: JSONException) {
            }
        }
        return try {
            BluetoothAdapter.getDefaultAdapter().getRemoteDevice(address)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    fun addDevices(devices: Collection<BluetoothDevice>) {
        CoroutineScope(Dispatchers.Default).launch {
            val array: JSONArray = SPreferences.getTwsDevices()
            if ((devices.size == 2) and (isContains(array.toString(), devices).size == 2)) {
                val item = JSONArray()
                for (d in devices) {
                    item.put(d.address)
                }
                array.put(item)
                withContext(Dispatchers.IO) {
                    SPreferences.putTwsDevices(array)
                }
            }
        }
    }

    fun removeDevices(devices: Collection<BluetoothDevice>) {
        CoroutineScope(Dispatchers.Default).launch {
            val array: JSONArray = SPreferences.getTwsDevices()
            val devicesVailed = isContains(array.toString(), devices) as MutableSet<BluetoothDevice>
            for (i in array.length() - 1 downTo 0) {
                if (devicesVailed.isEmpty())
                    break
                try {
                    val item = array.getJSONArray(i)
                    for (d in devicesVailed) {
                        if (item.toString().contains(d.address)) {
                            array.remove(i)
                            devicesVailed.remove(d)
                            break
                        }
                    }
                } catch (ignored: JSONException) {
                }
            }
            SPreferences.putTwsDevices(array)
        }
    }
}