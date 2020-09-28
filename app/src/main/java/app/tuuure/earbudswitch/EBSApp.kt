package app.tuuure.earbudswitch

import android.app.Application
import app.tuuure.earbudswitch.activity.DialogActivity
import app.tuuure.earbudswitch.earbuds.EarbudService
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.meta.SimpleSubscriberInfo
import org.greenrobot.eventbus.meta.SubscriberMethodInfo


class EBSApp : Application() {
    override fun onCreate() {
        super.onCreate()
        EventBus.builder().addIndex {
            val infos = arrayOf(
                SubscriberMethodInfo("onDisconnectEvent", DisconnectEvent::class.java),
                SubscriberMethodInfo("onCancelAdvertise", CancelAdvertiseEvent::class.java)
            )
            SimpleSubscriberInfo(EarbudService::class.java, true, infos)
        }

        EventBus.builder().addIndex {
            val infos = arrayOf(
                SubscriberMethodInfo("onScanResult", ScanResultEvent::class.java),
                SubscriberMethodInfo("connectGatt", ConnectGattEvent::class.java),
                SubscriberMethodInfo("scanForServer", ScanEvent::class.java),
                SubscriberMethodInfo("onSetFreshEvent", RefreshEvent::class.java)
            )
            SimpleSubscriberInfo(DialogActivity::class.java, true, infos)
        }
    }
}