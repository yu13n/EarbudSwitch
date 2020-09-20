package app.tuuure.earbudswitch.nearby.ble

import android.bluetooth.*
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import app.tuuure.earbudswitch.*
import app.tuuure.earbudswitch.utils.CryptoConvert.Companion.bytesToUUID
import app.tuuure.earbudswitch.utils.CryptoConvert.Companion.md5code32
import app.tuuure.earbudswitch.utils.CryptoConvert.Companion.tOTPChecker
import app.tuuure.earbudswitch.utils.SPreferences
import com.drake.channel.AndroidScope
import com.drake.channel.receiveEvent
import com.drake.channel.sendEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.util.*

class BleAdvertiser(private val context: Context, var isBind: Boolean) {
    companion object {
        private const val DescriptorUUID = "00002902-0000-1000-8000-00805f9b34fb"
    }

    private val bluetoothAdapter: BluetoothAdapter
    private val bluetoothLeAdvertiser: BluetoothLeAdvertiser
    private lateinit var gattServer: BluetoothGattServer
    private val callbacks = ArrayList<AdvertiseCallback>(2)
    private val deviceMap: HashMap<UUID, String> = HashMap(2)
    private lateinit var key: String

    private val scrolls = ArrayList<AndroidScope>()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            key = SPreferences.getKey()
        }
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        bluetoothLeAdvertiser = bluetoothAdapter.bluetoothLeAdvertiser;

        scrolls.add(receiveEvent<ParamDevices>(EventTag.ADVERTISE) {
            Log.d("TAG", "advertise")
            advertise(it.devices)
        })
    }

    fun unregister() {
        for (scope in scrolls) {
            scope.cancel()
        }
    }

    var gattServerCallback: BluetoothGattServerCallback = object : BluetoothGattServerCallback() {
        //设备连接/断开连接回调
        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            Log.d("TAG", "connected")
            super.onConnectionStateChange(device, status, newState)
        }

        //特征值写入回调
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
            CoroutineScope(Dispatchers.Default).launch {
                val receivedCode = ByteBuffer.wrap(value).int
                Log.d("TAG", receivedCode.toString())
                sendEvent(
                    ParamString(receivedCode.toString()),
                    EventTag.TYPE_RESULT + EventTag.WRITTEN
                )
                val target = deviceMap[characteristic.uuid]!!
                val authList = tOTPChecker(key)
                for (d in authList) {
                    Log.d("TAG", d.toString())
                }
                if (receivedCode in authList) {
                    if (!isBind) {
                        sendEvent(ParamUnit(), EventTag.DISCONNECT)
                        characteristic.setValue("");
                        gattServer.notifyCharacteristicChanged(device, characteristic, false);
                    }
                } else {
                    gattServer.cancelConnection(device)
                }
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
                );
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
        .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_LOW)
        .build()

    fun advertise(devices: Collection<String>) {
        stopAdvertise()

        val bluetoothManager =
            context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        gattServer = bluetoothManager.openGattServer(context, gattServerCallback)

        for (address in devices) {
            val uuid = bytesToUUID(md5code32(address))
            deviceMap[uuid] = address

            val gattService = BluetoothGattService(uuid, BluetoothGattService.SERVICE_TYPE_PRIMARY)
            val gattCharacteristic = BluetoothGattCharacteristic(
                uuid,
                BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ or BluetoothGattCharacteristic.PERMISSION_WRITE
            )
            val descriptor = BluetoothGattDescriptor(
                UUID.fromString(DescriptorUUID),
                BluetoothGattDescriptor.PERMISSION_WRITE
            );
            gattCharacteristic.addDescriptor(descriptor);
            gattService.addCharacteristic(gattCharacteristic)
            gattServer.addService(gattService)

            val advertiseData = AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .setIncludeTxPowerLevel(false)
                .addServiceUuid(ParcelUuid(uuid))
                .build()
            val advertiseCallback = object : AdvertiseCallback() {
                override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                    callbacks.add(this)
                    super.onStartSuccess(settingsInEffect)
                }
            }
            bluetoothLeAdvertiser.startAdvertising(
                advertiseSettings,
                advertiseData,
                advertiseCallback
            )
        }
    }

    fun stopAdvertise() {
        if (callbacks.isNotEmpty()) {
            for (callback in callbacks) {
                bluetoothLeAdvertiser.stopAdvertising(callback)
            }
            callbacks.clear()
            gattServer.clearServices()
            gattServer.close()
        }
    }
}