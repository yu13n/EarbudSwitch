package app.tuuure.earbudswitch.nearby.ble

import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import app.tuuure.earbudswitch.*
import app.tuuure.earbudswitch.utils.CryptoConvert.Companion.bytesToUUID
import app.tuuure.earbudswitch.utils.CryptoConvert.Companion.md5code32
import app.tuuure.earbudswitch.utils.CryptoConvert.Companion.tOTPGenerater
import app.tuuure.earbudswitch.utils.Preferences
import com.drake.channel.AndroidScope
import com.drake.channel.receiveEvent
import com.drake.channel.sendEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.util.*
import kotlin.collections.ArrayList


class BleScanner constructor(private val context: Context, var isBind: Boolean) {
    companion object {
        private const val DescriptorUUID = "00002902-0000-1000-8000-00805f9b34fb"
    }

    private val bluetoothAdapter: BluetoothAdapter
    private val bluetoothLeScanner: BluetoothLeScanner
    val deviceMap: HashMap<ParcelUuid, String> = HashMap(3)
    private lateinit var key: String

    private val scanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val server = result.device!!
            val scanRecord = result.scanRecord!!

            Log.d("TAG", "result ${server.address}")
            val deviceList = deviceMap.filterKeys { it in scanRecord.serviceUuids }

            if (deviceList.isNotEmpty()) {
                stopScan()
                if (isBind) {
                    sendEvent(
                        ParamServer(server.address, deviceList.values),
                        EventTag.TYPE_RESULT + EventTag.SCAN
                    )
                } else {
                    connectServer(server.address, deviceList.values)
                }
            } else {
                return
            }
            super.onScanResult(callbackType, result)
        }
    }

    private val scrolls = ArrayList<AndroidScope>()

    init {
//        CoroutineScope(Dispatchers.IO).launch {
//            key = Preferences.getKey()
//        }
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
//        scanCallback = object : ScanCallback() {
//            override fun onScanResult(callbackType: Int, result: ScanResult) {
//                val server = result.device!!
//                val scanRecord = result.scanRecord!!
//
//                val deviceList = deviceMap.filterKeys { it in scanRecord.serviceUuids }
//
//                if (deviceList.isNotEmpty()) {
//                    stopScan()
//                    if (isBind) {
//                        sendEvent(
//                            ParamServer(server.address, deviceList.values),
//                            EventTag.TYPE_RESULT + EventTag.SCAN
//                        )
//                    } else {
//                        connectServer(server.address, deviceList.values)
//                    }
//                } else {
//                    return
//                }
//                super.onScanResult(callbackType, result)
//            }
//        }
        scrolls.add(receiveEvent<ParamDevices>(EventTag.SCAN) {
            Log.d("TAG", "scan")
            scan(it.devices)
        })
    }

    fun setBind() {
        isBind = true
        scrolls.add(receiveEvent<ParamServer>(EventTag.CONNECT_GATT) {
            connectServer(it.server, it.devices)
        })
        scrolls.add(receiveEvent<ParamUnit>(EventTag.WRITE) {
            writeCharacteristic()
        })
    }

    fun unregister() {
        for (scope in scrolls) {
            scope.cancel()
        }
    }

    fun scan(devices: Collection<String>) {
        stopScan()

        if (devices.isEmpty()) {
            return
        }

        val filters = ArrayList<ScanFilter>(devices.size)
        for (address in devices) {
            val hashedData: ByteArray = md5code32(address)
            val parcelUuid = ParcelUuid(bytesToUUID(hashedData))
            deviceMap[parcelUuid] = address
            val filter = ScanFilter.Builder()
                .setServiceUuid(parcelUuid)
                .build()
            filters.add(filter)
        }

        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
            .setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
            .setReportDelay(0)
            .build()
        bluetoothLeScanner.startScan(filters, scanSettings, scanCallback)
//        CoroutineScope(Dispatchers.Default).launch {
//            //延时5秒自动停止
//            delay(3000)
//            stopScan()
//            //TODO: 告知Service扫描已被取消
//        }
    }

    fun stopScan() {
        bluetoothLeScanner.stopScan(scanCallback)
    }

    private var bluetoothGatt: BluetoothGatt? = null
    private lateinit var gattCharacteristic: BluetoothGattCharacteristic
    private var authCode: Int = 0

    fun connectServer(server: String, devices: Collection<String>) {
        stopScan()
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
                    val uuid = bytesToUUID(md5code32(devices.random()))

                    val bluetoothGattService = gatt.getService(uuid)
                    gattCharacteristic = bluetoothGattService.getCharacteristic(uuid)
                    gatt.setCharacteristicNotification(gattCharacteristic, true);
                    if (isBind) {
                        sendEvent(ParamUnit(), EventTag.TYPE_RESULT + EventTag.CONNECT_GATT)
                    } else {
                        authCode = tOTPGenerater(key)
                        Log.d("TAG", authCode.toString())
                        gattCharacteristic.value = ByteBuffer.allocate(8).putInt(authCode).array()
                        gatt.writeCharacteristic(gattCharacteristic)
                    }
                }
            }

            override fun onCharacteristicWrite(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                status: Int
            ) {
                super.onCharacteristicWrite(gatt, characteristic, status)
                Log.d("TAG", "WRite")
                val descriptor =
                    gattCharacteristic.getDescriptor(UUID.fromString(DescriptorUUID))
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                gatt.writeDescriptor(descriptor)
                if (isBind) {
                    sendEvent(
                        ParamString(authCode.toString()),
                        EventTag.TYPE_RESULT + EventTag.WRITE
                    )
                }
            }

            override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic
            ) {
                Log.d("TAG", "Changed")
                if (isBind) {
                    sendEvent(
                        ParamUnit(),
                        EventTag.TYPE_RESULT + EventTag.NOTIFIED
                    )
                } else {
                    sendEvent(ParamTarget(deviceMap.values.random()), EventTag.CONNECT)
                }
                gatt.disconnect()
                gatt.close()
                super.onCharacteristicChanged(gatt, characteristic)
            }
        }

        bluetoothGatt = bluetoothAdapter.getRemoteDevice(server)!!
            .connectGatt(context, false, bluetoothGattCallback)!!
    }

    fun writeCharacteristic() {
        authCode = tOTPGenerater(key)
        gattCharacteristic.value = ByteBuffer.allocate(8).putInt(authCode).array()
        bluetoothGatt!!.writeCharacteristic(gattCharacteristic)
    }
}