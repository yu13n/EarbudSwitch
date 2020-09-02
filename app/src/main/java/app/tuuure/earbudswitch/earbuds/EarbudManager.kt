package app.tuuure.earbudswitch.earbuds

import android.bluetooth.*
import androidx.collection.ArrayMap
import app.tuuure.earbudswitch.EventTag
import app.tuuure.earbudswitch.ParamTarget
import app.tuuure.earbudswitch.ParamUnit
import com.drake.channel.AndroidScope
import com.drake.channel.receiveEvent
import kotlinx.coroutines.cancel
import java.lang.reflect.Method

class EarbudManager(context: EarbudService) {
    companion object {
        @JvmStatic
        fun getAudioDevices(): MutableMap<String, String> {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            val devices = ArrayMap<String, String>()
            for (d in bluetoothAdapter.bondedDevices) {
                if ((d.name != null) and
                    (d.name.isNotEmpty()) and
                    (d.bluetoothClass.majorDeviceClass == BluetoothClass.Device.Major.AUDIO_VIDEO)
                )
                    devices.put(d.name, d.address)
            }
            return devices
        }

    }

    private val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private lateinit var bluetoothA2dp: BluetoothA2dp
    private lateinit var bluetoothHeadset: BluetoothHeadset

    // Android 4.0.4_r2.1
    private val a2dpDisconnectMethod: Method =
        BluetoothA2dp::class.java.getDeclaredMethod("disconnect", BluetoothDevice::class.java)
    private val headsetDisconnectMethod: Method =
        BluetoothHeadset::class.java.getDeclaredMethod("disconnect", BluetoothDevice::class.java)

    // Android 4.0.4_r2.1
    private val a2dpConnectMethod: Method =
        BluetoothA2dp::class.java.getDeclaredMethod("connect", BluetoothDevice::class.java)
    private val headsetConnectMethod: Method =
        BluetoothHeadset::class.java.getDeclaredMethod("connect", BluetoothDevice::class.java)

    init {
        val proxyListener = object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
                when (profile) {
                    BluetoothProfile.A2DP ->
                        bluetoothA2dp = proxy as BluetoothA2dp
                    BluetoothProfile.HEADSET ->
                        bluetoothHeadset = proxy as BluetoothHeadset
                    else ->
                        return
                }
            }

            override fun onServiceDisconnected(profile: Int) {
            }
        }
        bluetoothAdapter.getProfileProxy(context, proxyListener, BluetoothProfile.A2DP)
        bluetoothAdapter.getProfileProxy(context, proxyListener, BluetoothProfile.HEADSET)
    }

    private val scrolls = ArrayList<AndroidScope>()

    init {
        scrolls.add(receiveEvent<ParamTarget>(EventTag.CONNECT) {
            connect(bluetoothAdapter.getRemoteDevice(it.target))
        })
        scrolls.add(receiveEvent<ParamUnit>(EventTag.DISCONNECT) {
            disconnect(null)
        })
    }

    fun unregister() {
        bluetoothAdapter.closeProfileProxy(BluetoothProfile.A2DP, bluetoothA2dp)
        bluetoothAdapter.closeProfileProxy(BluetoothProfile.HEADSET, bluetoothHeadset)
        for (scope in scrolls) {
            scope.cancel()
        }
    }

    fun connect(device: BluetoothDevice) {
        a2dpConnectMethod.invoke(bluetoothA2dp, device)
        headsetConnectMethod.invoke(bluetoothHeadset, device)
    }

    fun disconnect(device: BluetoothDevice?) {
        if (device == null) {
            val devices = bluetoothA2dp.connectedDevices
            devices.addAll(bluetoothHeadset.connectedDevices)
            for (d in devices) {
                a2dpDisconnectMethod.invoke(bluetoothA2dp, d)
                headsetDisconnectMethod.invoke(bluetoothHeadset, d)
            }
        } else {
            a2dpDisconnectMethod.invoke(bluetoothA2dp, device)
            headsetDisconnectMethod.invoke(bluetoothHeadset, device)
        }

    }

    fun getConnectedA2dpDevices(): MutableSet<BluetoothDevice>? {
        return bluetoothA2dp.connectedDevices?.toMutableSet()
    }
}