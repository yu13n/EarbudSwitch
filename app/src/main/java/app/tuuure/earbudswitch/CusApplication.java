package app.tuuure.earbudswitch;

import android.app.Application;
import android.content.Context;

import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.analytics.Analytics;
import com.microsoft.appcenter.crashes.Crashes;

public class CusApplication extends Application {
    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        AppCenter.start(this, "", Analytics.class, Crashes.class);
    }

    public static Context getContext() {
        return context;
    }
}