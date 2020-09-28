package app.tuuure.earbudswitch.nearby.ble

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.ParcelUuid
import android.widget.Toast
import androidx.annotation.RequiresApi
import app.tuuure.earbudswitch.RadarReceiver
import app.tuuure.earbudswitch.ScanResultEvent
import app.tuuure.earbudswitch.nearby.IScanner
import app.tuuure.earbudswitch.utils.CryptoConvert.Companion.bytesToUUID
import app.tuuure.earbudswitch.utils.CryptoConvert.Companion.md5code32
import org.greenrobot.eventbus.EventBus
import java.util.*
import kotlin.collections.ArrayList


class BleScanner constructor(
    override val context: Context,
    override var key: String,
    isPersisted: Boolean = false
) : IScanner {
    companion object {
        private const val REQUEST_CODE = 1
        const val SCAN_NO_ERROR = 0
        const val SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES = 5
        const val SCAN_FAILED_SCANNING_TOO_FREQUENTLY = 6

        @JvmStatic
        fun codeStrize(code: Int): String = when (code) {
            SCAN_NO_ERROR -> "SCAN_NO_ERROR"
            ScanCallback.SCAN_FAILED_ALREADY_STARTED -> "SCAN_FAILED_ALREADY_STARTED"
            ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> "SCAN_FAILED_APPLICATION_REGISTRATION_FAILED"
            ScanCallback.SCAN_FAILED_INTERNAL_ERROR -> "SCAN_FAILED_INTERNAL_ERROR"
            ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED -> "SCAN_FAILED_FEATURE_UNSUPPORTED"
            SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES -> "SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES"
            SCAN_FAILED_SCANNING_TOO_FREQUENTLY -> "SCAN_FAILED_SCANNING_TOO_FREQUENTLY"
            else -> "SCAN_UNKNOWN_RESULT_CODE, $code"
        }
    }

    private val isPersisted = isPersisted and (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)

    private val bluetoothLeScanner: BluetoothLeScanner =
        BluetoothAdapter.getDefaultAdapter().bluetoothLeScanner
    val deviceMap: HashMap<ParcelUuid, String> = HashMap(3)

    private val scanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val server = result.device!!
            val scanRecord = result.scanRecord!!
            val deviceList = deviceMap.filterKeys { it in scanRecord.serviceUuids }

            when (callbackType) {
                ScanSettings.CALLBACK_TYPE_MATCH_LOST -> {
                    EventBus.getDefault()
                        .post(ScanResultEvent(server.address, deviceList.values, isFound = false))
                }
                ScanSettings.CALLBACK_TYPE_FIRST_MATCH, ScanSettings.CALLBACK_TYPE_ALL_MATCHES -> {
                    EventBus.getDefault()
                        .post(ScanResultEvent(server.address, deviceList.values, isFound = true))
                }
            }
            super.onScanResult(callbackType, result)
        }

        override fun onScanFailed(errorCode: Int) {
            Toast.makeText(context, codeStrize(errorCode), Toast.LENGTH_LONG).show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private val pendingIntent: PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, RadarReceiver::class.java).apply {
                action = RadarReceiver.ACTION_SERVER_MONITOR
            },
            PendingIntent.FLAG_UPDATE_CURRENT
        )


    @SuppressLint("NewApi") //初始化isPersisted时，已解决兼容问题
    override fun scan(devices: Collection<BluetoothDevice>) {
        stopScan()

        if (devices.isEmpty()) {
            return
        }

        val filters = ArrayList<ScanFilter>(devices.size)
        for (d in devices) {
            val hashedData: ByteArray = md5code32(d.address)
            val parcelUuid = ParcelUuid(bytesToUUID(hashedData))
            deviceMap[parcelUuid] = d.address
            val filter = ScanFilter.Builder()
                .setServiceUuid(parcelUuid)
                .build()
            filters.add(filter)
        }

        val scanSettings = ScanSettings.Builder().apply {
            setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
            if (isPersisted) {
                setCallbackType(ScanSettings.CALLBACK_TYPE_FIRST_MATCH or ScanSettings.CALLBACK_TYPE_MATCH_LOST)
                setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
            } else {
                setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
            }
        }.build()

        if (isPersisted) {
            bluetoothLeScanner.startScan(filters, scanSettings, pendingIntent)
        } else {
            bluetoothLeScanner.startScan(filters, scanSettings, scanCallback)
        }

    }


    @SuppressLint("NewApi") //初始化isPersisted时，已解决兼容问题
    override fun stopScan() {
        if (isPersisted) {
            bluetoothLeScanner.stopScan(pendingIntent)
        } else {
            bluetoothLeScanner.stopScan(scanCallback)
        }
    }
}