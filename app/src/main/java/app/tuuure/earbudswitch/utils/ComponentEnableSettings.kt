package app.tuuure.earbudswitch.utils

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import app.tuuure.earbudswitch.earbuds.EarbudReceiver

class ComponentEnableSettings {
    companion object {
        @JvmStatic
        fun setEnableSettings(context: Context, isInited: Boolean) {
            context.packageManager.setComponentEnabledSetting(
                ComponentName(
                    context,
                    EarbudReceiver::class.java
                ),
                if (isInited) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        }
    }
}