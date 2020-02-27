package app.tuuure.earbudswitch;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.analytics.Analytics;
import com.microsoft.appcenter.crashes.Crashes;

public class DialogActivity extends AppCompatActivity {
    final static String TAG = "EBSDialog";

    private final static int NUM_PAGES = 2;
    private ViewPager2 viewPager;
    boolean isPermissionGranted;
    int accentColor;
    int backgroundColor;
    boolean nightMode;
    private Switch itemSwitch;
    private BluetoothAdapter bluetoothAdapter;

    void setItemSwitchChecked(boolean b) {
        itemSwitch.setChecked(b);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dialog);

        Toolbar toolbar = findViewById(R.id.tb_dialog);

        setSupportActionBar(toolbar);

        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        AppCenter.start(getApplication(), "", Analytics.class, Crashes.class);

        viewPager = findViewById(R.id.viewpage);
        PaperAdapter paperAdapter = new PaperAdapter(this);
        viewPager.setAdapter(paperAdapter);
        viewPager.setPageTransformer(new DepthPageTransformer());

        Analytics.trackEvent("APP_OPEN");
    }

    @Override
    protected void onStart() {
        super.onStart();
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null || !getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, getText(R.string.unsupport_ble), Toast.LENGTH_SHORT).show();
            finish();
        }
        bluetoothAdapter = bluetoothManager.getAdapter();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        nightMode = Configuration.UI_MODE_NIGHT_YES == (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK);
        TypedArray array = obtainStyledAttributes(android.R.style.Theme_DeviceDefault, new int[]{
                android.R.attr.colorAccent,
                android.R.attr.colorBackground
        });
        accentColor = array.getColor(0, getColor(R.color.color_theme));
        backgroundColor = array.getColor(1, Color.DKGRAY);
        array.recycle();

        if (!bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.enable();
        }


        findViewById(R.id.dialog_conlayout).setBackgroundColor(backgroundColor);

        isPermissionGranted = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (!isPermissionGranted) {
            Toast.makeText(this, getString(R.string.request_permission), Toast.LENGTH_SHORT).show();
            viewPager.setCurrentItem(1);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.menu_dialog, menu);
        final MenuItem menuSwitch = menu.findItem(R.id.app_bar_switch);
        final MenuItem menuSettings = menu.findItem(R.id.app_bar_button);

        menuSettings.getActionView().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
                finish();
            }
        });
        itemSwitch = menuSwitch.getActionView().findViewById(R.id.item_switch);
        itemSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (bluetoothAdapter.isEnabled()) {
                    bluetoothAdapter.disable();
                } else {
                    bluetoothAdapter.enable();
                }
            }
        });
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels);
                if (positionOffset == 0) {
                    if (position == 0) {
                        menuSwitch.setVisible(true);
                        menuSettings.setVisible(false);
                    } else {
                        menuSwitch.setVisible(false);
                        menuSettings.setVisible(true);
                    }
                } else if (positionOffset <= 0.5) {
                    float alpha = (float) (Math.cos((2 - 2 * positionOffset) * 3.14) / 2 + 0.5);
                    menuSwitch.getActionView().setAlpha(alpha);
                    menuSwitch.setVisible(true);
                    menuSettings.setVisible(false);
                } else {
                    float alpha = (float) (Math.cos(2 * positionOffset * 3.14) / 2 + 0.5);
                    menuSettings.getActionView().setAlpha(alpha);
                    menuSwitch.setVisible(false);
                    menuSettings.setVisible(true);
                }
            }
        });
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        itemSwitch.setChecked(bluetoothAdapter.isEnabled());
        setSwitchAccent(new Switch[]{itemSwitch});
        MenuItem menuSwitch = menu.findItem(R.id.app_bar_switch);
        MenuItem menuSettings = menu.findItem(R.id.app_bar_button);
        menuSwitch.setVisible(viewPager.getCurrentItem() == 0);
        menuSettings.setVisible(viewPager.getCurrentItem() == 1);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onBackPressed() {
        if (viewPager.getCurrentItem() == 0 || !isPermissionGranted) {
            super.onBackPressed();
        } else {
            viewPager.setCurrentItem(viewPager.getCurrentItem() - 1);
        }
    }

    void setSwitchAccent(Switch[] sw) {
        int thumbOffColor;
        int trackOffColor;
        int trackOnColor = Color.argb(77, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor));

        if (nightMode) {
            thumbOffColor = getColor(R.color.color_switch_thumb_off_night);
            trackOffColor = getColor(R.color.color_switch_track_off_night);
        } else {
            thumbOffColor = getColor(R.color.color_switch_thumb_off_day);
            trackOffColor = getColor(R.color.color_switch_track_off_day);
        }
        ColorStateList thumbStates = new ColorStateList(
                new int[][]{
                        {android.R.attr.state_checked, android.R.attr.state_enabled},
                        {-android.R.attr.state_checked, android.R.attr.state_enabled},
                        {}
                },
                new int[]{accentColor, thumbOffColor, thumbOffColor}
        );

        ColorStateList trackStates = new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_checked},
                        new int[]{-android.R.attr.state_checked}
                },
                new int[]{trackOnColor, trackOffColor, trackOffColor}
        );
        for (Switch v : sw) {
            v.setThumbTintList(thumbStates);
            v.setTrackTintList(trackStates);
            v.setTrackTintMode(PorterDuff.Mode.OVERLAY);
        }
    }

    private class PaperAdapter extends FragmentStateAdapter {
        PaperAdapter(FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            Fragment frag;

            switch (position) {
                case 0:
                    frag = new DevicesFrag();
                    break;
                case 1:
                default:
                    frag = new SettingsFrag();
                    break;
            }
            return frag;
        }

        @Override
        public int getItemCount() {
            return NUM_PAGES;
        }
    }

    public class DepthPageTransformer implements ViewPager2.PageTransformer {
        @Override
        public void transformPage(View page, float position) {

            //page.setCameraDistance(12000);
            page.setBackgroundColor(backgroundColor);
            if (position <= -1) {     // [-Infinity,-1)
                page.setAlpha(0);
            } else if (position <= 0) {    // (-1,0)
                page.setAlpha(1);
                page.setTranslationX(-position * page.getWidth());
            } else if (position <= 1) {    // (0,1]
                page.setAlpha(1);
                page.setTranslationX(0f);
            } else {
                page.setAlpha(0);
            }
        }
    }

}
