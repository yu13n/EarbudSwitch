package app.tuuure.earbudswitch

import android.bluetooth.le.BluetoothLeScanner
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import app.tuuure.earbudswitch.nearby.ble.BleScanner

@RequiresApi(Build.VERSION_CODES.O)
class RadarReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_SERVER_MONITOR = "app.tuuure.earbudswitch.SERVER_MONITOR"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == ACTION_SERVER_MONITOR) {
            val bleCallbackType = intent.getIntExtra(BluetoothLeScanner.EXTRA_CALLBACK_TYPE, -1)
            val bleErrorCode =
                intent.getIntExtra(BluetoothLeScanner.EXTRA_ERROR_CODE, BleScanner.SCAN_NO_ERROR)

//            if (bleErrorCode != BleScanner.NO_ERROR) {
//                Preferences.getInstance(context!!)
//                    .putRecord(
//                        Preferences.RECORD_TYPE.BleError,
//                        BleScanner.codeStrize(bleErrorCode)
//                    )
//            } else {
//                val scanResults: java.util.ArrayList<ScanResult> =
//                    intent.getParcelableArrayListExtra(
//                        BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT
//                    )!!
//                val address: String? =
//                    if (scanResults.isNullOrEmpty()) null else scanResults[0].device.address
//
//                scanResults[0].device.address
//                when (bleCallbackType) {
//                    ScanSettings.CALLBACK_TYPE_FIRST_MATCH -> Preferences.getInstance(context!!)
//                        .putRecord(Preferences.RECORD_TYPE.TargetFound, "address: $address")
//                    ScanSettings.CALLBACK_TYPE_MATCH_LOST -> Preferences.getInstance(context!!)
//                        .putRecord(Preferences.RECORD_TYPE.TargetLost, "address: $address")
//                    else -> Preferences.getInstance(context!!)
//                        .putRecord(
//                            Preferences.RECORD_TYPE.BleError,
//                            "unknown, ErrorCode: $bleErrorCode, CallbackType: $bleCallbackType"
//                        )
//                }
//            }
        }
    }
}