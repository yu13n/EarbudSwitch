package app.tuuure.earbudswitch.nearby.ble

import android.bluetooth.*
import android.content.Context
import android.util.Log
import app.tuuure.earbudswitch.RefreshEvent
import app.tuuure.earbudswitch.earbuds.EarbudManager
import app.tuuure.earbudswitch.nearby.IConnecter
import app.tuuure.earbudswitch.utils.CryptoConvert
import org.greenrobot.eventbus.EventBus
import java.util.*

class BleConnecter constructor(
    override val context: Context,
    override var key: String,
) : IConnecter {
    companion object {
        private const val DescriptorUUID = "00002902-0000-1000-8000-00805f9b34fb"
    }

    private val bluetoothAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private lateinit var bluetoothGatt: BluetoothGatt

    override fun connect(server: String, device: String) {
        val bluetoothGattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                super.onConnectionStateChange(gatt, status, newState)
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    when (newState) {
                        BluetoothGatt.STATE_CONNECTED -> {
                            EventBus.getDefault().post(
                                RefreshEvent(
                                    device,
                                    true
                                )
                            )
                            gatt.discoverServices()
                        }
                        BluetoothGatt.STATE_DISCONNECTED ->
                            EventBus.getDefault().post(
                                RefreshEvent(
                                    device,
                                    false
                                )
                            )
                    }
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                super.onServicesDiscovered(gatt, status)
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    val uuid = CryptoConvert.bytesToUUID(CryptoConvert.md5code32(device))

                    val bluetoothGattService = gatt.getService(uuid)
                    val gattCharacteristic = bluetoothGattService.getCharacteristic(uuid)
                    gatt.readCharacteristic(gattCharacteristic)

                    gatt.setCharacteristicNotification(gattCharacteristic, true)
                    val descriptor =
                        gattCharacteristic.getDescriptor(UUID.fromString(DescriptorUUID))
                    descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    gatt.writeDescriptor(descriptor)
                }
            }

            override fun onCharacteristicRead(
                gatt: BluetoothGatt?,
                characteristic: BluetoothGattCharacteristic?,
                status: Int
            ) {
                super.onCharacteristicRead(gatt, characteristic, status)
                val salt = characteristic?.value
                if (salt != null) {
                    val authCode = CryptoConvert.otpGenerater(key, salt)
                    Log.d("TAG", authCode.toString())
                    characteristic.value = authCode
                    gatt?.writeCharacteristic(characteristic)
                }
            }

            override fun onCharacteristicWrite(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                status: Int
            ) {
                super.onCharacteristicWrite(gatt, characteristic, status)
                Log.d("TAG", "WRite")
            }

            override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic
            ) {
                Log.d("TAG", "Changed")
                gatt.disconnect()
                gatt.close()

                EarbudManager.connectEBS(context, device)

                super.onCharacteristicChanged(gatt, characteristic)
            }
        }

        bluetoothGatt = bluetoothAdapter.getRemoteDevice(server)!!
            .connectGatt(context, false, bluetoothGattCallback)!!
    }
}