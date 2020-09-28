package app.tuuure.earbudswitch.nearby.ble

import android.bluetooth.*
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import app.tuuure.earbudswitch.DisconnectEvent
import app.tuuure.earbudswitch.nearby.IAdvertiser
import app.tuuure.earbudswitch.utils.CryptoConvert.Companion.bytesToUUID
import app.tuuure.earbudswitch.utils.CryptoConvert.Companion.md5code32
import app.tuuure.earbudswitch.utils.CryptoConvert.Companion.otpGenerater
import app.tuuure.earbudswitch.utils.CryptoConvert.Companion.randomBytes
import org.greenrobot.eventbus.EventBus
import java.util.*

class BleAdvertiser(override val context: Context, override var key: String) : IAdvertiser {
    companion object {
        private const val DescriptorUUID = "00002902-0000-1000-8000-00805f9b34fb"
    }

    private val bluetoothLeAdvertiser: BluetoothLeAdvertiser =
        BluetoothAdapter.getDefaultAdapter().bluetoothLeAdvertiser
    private lateinit var gattServer: BluetoothGattServer
    private val callbacks = ArrayList<AdvertiseCallback>(2)
    private val deviceMap: HashMap<UUID, String> = HashMap(2)
    private var authCode: ByteArray? = null

    var gattServerCallback: BluetoothGattServerCallback = object : BluetoothGattServerCallback() {

        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            Log.d("ConnectionState", "${device.address} $newState")
            super.onConnectionStateChange(device, status, newState)
        }

        override fun onCharacteristicReadRequest(
            device: BluetoothDevice?,
            requestId: Int,
            offset: Int,
            characteristic: BluetoothGattCharacteristic?
        ) {
            val salt = randomBytes(8)

            gattServer.sendResponse(
                device,
                requestId,
                BluetoothGatt.GATT_SUCCESS,
                0,
                salt
            )

            authCode = otpGenerater(key, salt)

            super.onCharacteristicReadRequest(device, requestId, offset, characteristic)
        }

        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray
        ) {
            if (responseNeeded) {
                gattServer.sendResponse(
                    device,
                    requestId,
                    BluetoothGatt.GATT_SUCCESS,
                    0,
                    null
                )
            }

            val target = deviceMap[characteristic.uuid]!!

            Log.d("value", value.toString())
            Log.d("authCode", authCode.toString())

            if (value.contentEquals(authCode)) {
                EventBus.getDefault().post(DisconnectEvent(target))

                characteristic.setValue("")
                gattServer.notifyCharacteristicChanged(device, characteristic, false)
            } else {
                gattServer.cancelConnection(device)
            }
            super.onCharacteristicWriteRequest(
                device,
                requestId,
                characteristic,
                preparedWrite,
                responseNeeded,
                offset,
                value
            )
        }

        override fun onDescriptorWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            descriptor: BluetoothGattDescriptor,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray
        ) {
            if (responseNeeded) {
                gattServer.sendResponse(
                    device,
                    requestId,
                    BluetoothGatt.GATT_SUCCESS,
                    0,
                    null
                )
            }
            super.onDescriptorWriteRequest(
                device,
                requestId,
                descriptor,
                preparedWrite,
                responseNeeded,
                offset,
                value
            )
        }
    }

    private val advertiseSettings = AdvertiseSettings.Builder()
        .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)
        .setConnectable(true)
        .setTimeout(0)
        .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
        .build()

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            super.onStartSuccess(settingsInEffect)
            callbacks.add(this)
        }
    }

    override fun advertise(devices: Collection<BluetoothDevice>) {
        stopAdvertise()

        val bluetoothManager =
            context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        gattServer = bluetoothManager.openGattServer(context, gattServerCallback)

        for (d in devices) {
            val uuid = bytesToUUID(md5code32(d.address))
            Log.d("Advertise", uuid.toString())
            deviceMap[uuid] = d.address

            val gattService = BluetoothGattService(uuid, BluetoothGattService.SERVICE_TYPE_PRIMARY)
            val gattCharacteristic = BluetoothGattCharacteristic(
                uuid,
                BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ or BluetoothGattCharacteristic.PERMISSION_WRITE
            )
            val descriptor = BluetoothGattDescriptor(
                UUID.fromString(DescriptorUUID),
                BluetoothGattDescriptor.PERMISSION_WRITE
            )
            gattCharacteristic.addDescriptor(descriptor)
            gattService.addCharacteristic(gattCharacteristic)
            gattServer.addService(gattService)

            val advertiseData = AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .setIncludeTxPowerLevel(false)
                .addServiceUuid(ParcelUuid(uuid))
                .build()

            bluetoothLeAdvertiser.startAdvertising(
                advertiseSettings,
                advertiseData,
                advertiseCallback
            )
        }
    }

    override fun stopAdvertise() {
        if (callbacks.isNotEmpty()) {
            for (callback in callbacks) {
                bluetoothLeAdvertiser.stopAdvertising(callback)
            }
            gattServer.clearServices()
            deviceMap.clear()
            callbacks.clear()
            gattServer.close()
        }
    }
}