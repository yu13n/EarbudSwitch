package app.tuuure.earbudswitch.earbuds

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import java.lang.reflect.Method

class EarbudManager {
    companion object {
        @JvmStatic
        fun getAudioDevices(): List<BluetoothDevice> =
            BluetoothAdapter.getDefaultAdapter().bondedDevices.filterNot {
                it.name.isNullOrEmpty()
                        || it.bluetoothClass.majorDeviceClass != BluetoothClass.Device.Major.AUDIO_VIDEO
            }

        // Android 4.0.4_r2.1
        @SuppressLint("DiscouragedPrivateApi")
        private val a2dpDisconnectMethod: Method =
            BluetoothA2dp::class.java.getDeclaredMethod("disconnect", BluetoothDevice::class.java)
        private val headsetDisconnectMethod: Method =
            BluetoothHeadset::class.java.getDeclaredMethod(
                "disconnect",
                BluetoothDevice::class.java
            )

        // Android 4.0.4_r2.1
        @SuppressLint("DiscouragedPrivateApi")
        private val a2dpConnectMethod: Method =
            BluetoothA2dp::class.java.getDeclaredMethod("connect", BluetoothDevice::class.java)
        private val headsetConnectMethod: Method =
            BluetoothHeadset::class.java.getDeclaredMethod("connect", BluetoothDevice::class.java)

        private enum class OPERATION {
            connect,
            disconnect
        }

        @JvmStatic
        private fun profileAgent(context: Context, op: OPERATION, device: BluetoothDevice) {
            val proxyListener = object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
                    when (profile) {
                        BluetoothProfile.A2DP -> {
                            val bluetoothA2dp = proxy as BluetoothA2dp
                            when (op) {
                                OPERATION.connect -> {
                                    if (!bluetoothA2dp.connectedDevices.contains(device))
                                        a2dpConnectMethod.invoke(bluetoothA2dp, device)
                                }
                                OPERATION.disconnect -> {
                                    if (bluetoothA2dp.connectedDevices.contains(device)) {
                                        a2dpDisconnectMethod.invoke(bluetoothA2dp, device)
                                    }
                                }
                            }
                        }
                        BluetoothProfile.HEADSET -> {
                            val bluetoothHeadset = proxy as BluetoothHeadset
                            when (op) {
                                OPERATION.connect -> {
                                    if (!bluetoothHeadset.connectedDevices.contains(device))
                                        headsetConnectMethod.invoke(bluetoothHeadset, device)
                                }
                                OPERATION.disconnect -> {
                                    if (bluetoothHeadset.connectedDevices.contains(device)) {
                                        headsetDisconnectMethod.invoke(bluetoothHeadset, device)
                                    }
                                }
                            }
                        }
                    }
                    BluetoothAdapter.getDefaultAdapter().closeProfileProxy(profile, proxy)
                }

                override fun onServiceDisconnected(profile: Int) {}
            }
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            bluetoothAdapter.getProfileProxy(context, proxyListener, BluetoothProfile.A2DP)
            bluetoothAdapter.getProfileProxy(context, proxyListener, BluetoothProfile.HEADSET)
        }

        @JvmStatic
        fun connectEBS(context: Context, device: BluetoothDevice) {
            profileAgent(context, OPERATION.connect, device)
        }

        @JvmStatic
        fun connectEBS(context: Context, device: String) {
            profileAgent(
                context,
                OPERATION.connect,
                BluetoothAdapter.getDefaultAdapter().getRemoteDevice(device)
            )
        }

        @JvmStatic
        fun disconnectEBS(context: Context, device: BluetoothDevice) {
            profileAgent(context, OPERATION.disconnect, device)
        }

        @JvmStatic
        fun disconnectEBS(context: Context, device: String) {
            profileAgent(
                context,
                OPERATION.disconnect,
                BluetoothAdapter.getDefaultAdapter().getRemoteDevice(device)
            )
        }
    }
}