package app.tuuure.earbudswitch;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.appcompat.widget.Toolbar;

import android.Manifest;
import android.animation.LayoutTransition;
import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private static final String SHORTCUT_ID = "bhtdialog";
    private static final int REQUEST_PERMISSION_CODE = 14;
    private static final int REQUEST_BLUETOOTH_CODE = 15;

    private Switch swLocation;
    private Switch swBluetooth;
    private Switch swAdvertise;
    private EditText etKey;
    private SharedPreferences sp;

    private BluetoothAdapter bluetoothAdapter;
    private Boolean isPermissionGranted = false;
    private Boolean isBluetoothOn = false;
    private ComponentName monitor;

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) {
                Log.e(TAG, "Receiver action null");
                return;
            }
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                isBluetoothOn = state == BluetoothAdapter.STATE_ON;
                swBluetooth.setChecked(isBluetoothOn);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sp = getSharedPreferences(getString(R.string.app_title), MODE_PRIVATE);

        swLocation = findViewById(R.id.sw_location);
        swBluetooth = findViewById(R.id.sw_bht_state);
        swAdvertise = findViewById(R.id.sw_enable);
        etKey = findViewById(R.id.et_key);
        ConstraintLayout mainLayout = findViewById(R.id.main_layout);

        Toolbar toolbar = findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);

        String key = sp.getString("key", "");
        saveKey(key);

        setEditTextListener();

        //Setup Transition Animation
        LayoutTransition layoutTransition = mainLayout.getLayoutTransition();
        layoutTransition.enableTransitionType(LayoutTransition.CHANGING);
        mainLayout.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if (oldBottom != 0 && bottom != 0 && bottom < oldBottom) {
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            etKey.requestFocus();
                        }
                    }, 200);
                }
            }
        });

        setSwitchOnCheckedChangeListener();

        monitor = new ComponentName(getPackageName(), BluetoothMonitor.class.getName());

        swLocation.setChecked(isPermissionGranted);
        swBluetooth.setChecked(isBluetoothOn);
        swAdvertise.setChecked(getPackageManager().getComponentEnabledSetting(monitor) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED);

        isPermissionGranted = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null && getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            bluetoothAdapter = bluetoothManager.getAdapter();
            if (bluetoothAdapter.isEnabled()) {
                isBluetoothOn = true;
            }
        } else {
            Toast.makeText(this, getText(R.string.unsupport_ble), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setEditTextListener() {
        etKey.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    switch (event.getKeyCode()) {
                        case KeyEvent.KEYCODE_ENTER:
                        case KeyEvent.KEYCODE_NUMPAD_ENTER:
                            Log.d(TAG, "Enter up");
                            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), 0);
                            etKey.clearFocus();
                            etKey.setCursorVisible(false);
                            saveKey(etKey.getText().toString());
                            return true;
                    }
                }
                return false;
            }
        });
        etKey.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                Log.d(TAG, "in");
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    Log.d(TAG, "Action Done");
                    saveKey(etKey.getText().toString());
                    return false;
                }
                return false;
            }
        });
        etKey.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus) {
                    etKey.setFocusableInTouchMode(true);
                    etKey.setFocusable(true);
                    etKey.setCursorVisible(true);
                } else {
                    Log.d(TAG, "lost Focus");
                    etKey.clearFocus();
                    etKey.setCursorVisible(false);
                }
            }
        });
    }

    private void setSwitchOnCheckedChangeListener() {
        CompoundButton.OnCheckedChangeListener swListener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                switch (buttonView.getId()) {
                    case R.id.sw_location:
                        buttonView.setClickable(!isChecked);
                        if (isChecked && !isPermissionGranted) {
                            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSION_CODE);
                        }
                        break;
                    case R.id.sw_bht_state:
                        buttonView.setClickable(!isChecked);
                        if (isChecked && !isBluetoothOn) {
                            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                            startActivityForResult(intent, REQUEST_BLUETOOTH_CODE);
                        }
                        break;
                    case R.id.sw_enable:
                        getPackageManager().setComponentEnabledSetting(
                                monitor,
                                isChecked ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                                PackageManager.DONT_KILL_APP);
                        break;
                }
            }
        };
        swLocation.setOnCheckedChangeListener(swListener);
        swBluetooth.setOnCheckedChangeListener(swListener);
        swAdvertise.setOnCheckedChangeListener(swListener);
    }

    private void saveKey(String key) {
        SharedPreferences.Editor editor = sp.edit();
        if (key.isEmpty()) {
            Random r = new Random();
            key = String.valueOf(r.nextInt(900000) + 100000);
            etKey.setText(key);
        }
        editor.putString("key", key);
        editor.apply();
        etKey.setText(key);
    }

    @TargetApi(25)
    private void registerShortCut() {
        ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);
        List<ShortcutInfo> infos = new ArrayList<>();

        // 按下返回button跳转的activity
        Intent intent = new Intent(this, BHTDialog.class);
        intent.setAction(Intent.ACTION_VIEW);
        ShortcutInfo info = new ShortcutInfo.Builder(this, SHORTCUT_ID)
                .setShortLabel(getString(R.string.shortcut_title))
                .setLongLabel(getString(R.string.shortcut_title))
                .setIcon(Icon.createWithResource(this, R.mipmap.ic_bluetooth))
                .setIntent(intent)
                .build();
        infos.add(info);

        shortcutManager.setDynamicShortcuts(infos);
    }

    private void setSwitchAccent(Switch v, int[] thumbColor, int[] trackColor) {
        ColorStateList thumbStates = new ColorStateList(
                new int[][]{
                        {android.R.attr.state_checked, android.R.attr.state_enabled},
                        {-android.R.attr.state_checked, android.R.attr.state_enabled},
                        {}
                },
                thumbColor
        );
        v.setThumbTintList(thumbStates);

        ColorStateList trackStates = new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_checked},
                        new int[]{-android.R.attr.state_checked}
                },
                trackColor
        );
        v.setTrackTintList(trackStates);
        v.setTrackTintMode(PorterDuff.Mode.OVERLAY);
    }

    @Override
    protected void onResume() {
        super.onResume();

        int thumbOffColor;
        int trackOffColor;

        if (Configuration.UI_MODE_NIGHT_YES == (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)) {
            Log.d(TAG, "night");
            // Night mode is active, we're using dark theme
            thumbOffColor = getColor(R.color.color_switch_thumb_off_night);
            trackOffColor = getColor(R.color.color_switch_track_off_night);
        } else {
            // Night mode is not active, Using the light theme
            Log.d(TAG, "Day");
            thumbOffColor = getColor(R.color.color_switch_thumb_off_day);
            trackOffColor = getColor(R.color.color_switch_track_off_day);
        }

        TypedArray array = obtainStyledAttributes(android.R.style.Theme_DeviceDefault, new int[]{
                android.R.attr.colorAccent
        });
        int thumbOnColor = array.getColor(0, getColor(R.color.color_theme));
        array.recycle();
        int[] thumbColor = {thumbOnColor, thumbOffColor, thumbOffColor};
        int trackOnColor = Color.argb(77, Color.red(thumbOnColor), Color.green(thumbOnColor), Color.blue(thumbOnColor));
        int[] trackColor = {trackOnColor, trackOffColor, trackOffColor};
        setSwitchAccent(swLocation, thumbColor, trackColor);
        setSwitchAccent(swBluetooth, thumbColor, trackColor);
        setSwitchAccent(swAdvertise, thumbColor, trackColor);

        isPermissionGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        swLocation.setChecked(isPermissionGranted);

        isBluetoothOn = bluetoothAdapter.isEnabled();
        swBluetooth.setChecked(isBluetoothOn);

        //注册监听蓝牙状态的receiver
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(receiver, intentFilter);
        Log.d(TAG, "Receiver registered");
    }

    @Override
    protected void onPause() {
        saveKey(etKey.getText().toString());
        unregisterReceiver(receiver);
        if (isPermissionGranted) {
            ComponentName tile = new ComponentName(getPackageName(), QuickSetting.class.getName());
            getPackageManager().setComponentEnabledSetting(tile, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                registerShortCut();
            }
        }
        Log.d(TAG, "Receiver unregistered");
        super.onPause();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_PERMISSION_CODE) {
            isPermissionGranted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            swLocation.setChecked(isPermissionGranted);
            if (!isPermissionGranted) {
                Toast.makeText(this, getText(R.string.request_permission), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_BLUETOOTH_CODE) {
            isBluetoothOn = resultCode == Activity.RESULT_OK;
            swBluetooth.setChecked(isBluetoothOn);
        }
    }

}
