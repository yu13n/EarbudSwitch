package app.tuuure.earbudswitch.nearby.ble

import android.bluetooth.*
import android.content.Context
import android.util.Log
import app.tuuure.earbudswitch.nearby.IConnecter
import app.tuuure.earbudswitch.utils.CryptoConvert
import java.nio.ByteBuffer
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
                    if (newState == BluetoothGatt.STATE_CONNECTED) {
                        gatt.discoverServices()
                    }
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                super.onServicesDiscovered(gatt, status)
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    val uuid = CryptoConvert.bytesToUUID(CryptoConvert.md5code32(device))

                    val bluetoothGattService = gatt.getService(uuid)
                    val gattCharacteristic = bluetoothGattService.getCharacteristic(uuid)
                    gatt.setCharacteristicNotification(gattCharacteristic, true);

                    val descriptor =
                        gattCharacteristic.getDescriptor(UUID.fromString(DescriptorUUID))
                    descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    gatt.writeDescriptor(descriptor)

                    val authCode = CryptoConvert.tOTPGenerater(key)
                    Log.d("TAG", authCode.toString())
                    gattCharacteristic.value = ByteBuffer.allocate(8).putInt(authCode).array()
                    gatt.writeCharacteristic(gattCharacteristic)
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

                //TODO 验证成功，连接耳机

                super.onCharacteristicChanged(gatt, characteristic)
            }
        }

        bluetoothGatt = bluetoothAdapter.getRemoteDevice(server)!!
            .connectGatt(context, false, bluetoothGattCallback)!!
    }
}