package app.tuuure.earbudswitch;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.animation.LayoutTransition;
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
import android.os.Bundle;
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

import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private static final int REQUEST_PERMISSION_CODE = 14;
    private static final int REQUEST_BLUETOOTH_CODE = 15;

    private Switch swLocation;
    private Switch swBluetooth;
    private EditText etKey;
    private SharedPreferences sp;

    private BluetoothAdapter bluetoothAdapter;
    private Boolean isPermissionGranted = false;
    private Boolean isBluetoothOn = false;

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
        final ComponentName monitor = new ComponentName(getPackageName(), BluetoothMonitor.class.getName());

        swLocation = findViewById(R.id.sw_location);
        swBluetooth = findViewById(R.id.sw_bht_state);
        final Switch swAdvertise = findViewById(R.id.sw_advertise);
        etKey = findViewById(R.id.et_key);
        ConstraintLayout mainLayout = findViewById(R.id.main_layout);

        String key = sp.getString("key", "");
        if (key.length() == 0) {
            Random r = new Random();
            //key = String.valueOf(r.nextInt(100000) + 900000);
            key = String.valueOf(900365);
            SharedPreferences.Editor editor = sp.edit();
            editor.putString("key", key);
            editor.apply();
        }
        etKey.setText(key);
        etKey.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                Log.d(TAG, "in");
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    Log.d(TAG, "Action Done");
                    return false;
                }
                if (KeyEvent.KEYCODE_ENTER == event.getKeyCode() || KeyEvent.KEYCODE_NUMPAD_ENTER == event.getKeyCode()) {
                    Log.d(TAG, "Enter up");
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), 0);
                    return true;
                }

                return false;
            }
        });

        mainLayout.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if (oldBottom != 0 && bottom != 0 && bottom < oldBottom) {
                    findViewById(R.id.ib_done).setVisibility(View.VISIBLE);
                    etKey.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            etKey.requestFocus();
                        }
                    }, 200);
                } else {
                    findViewById(R.id.ib_done).setVisibility(View.GONE);
                }
            }
        });

        LayoutTransition layoutTransition = mainLayout.getLayoutTransition();
        layoutTransition.enableTransitionType(LayoutTransition.CHANGING);

        isPermissionGranted = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null && getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            bluetoothAdapter = bluetoothManager.getAdapter();
            if (bluetoothAdapter.isEnabled()) {
                isBluetoothOn = true;
            }
        } else {
            finish();
        }

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
                    case R.id.sw_advertise:
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

        swLocation.setChecked(isPermissionGranted);
        swBluetooth.setChecked(isBluetoothOn);
        swAdvertise.setChecked(getPackageManager().getComponentEnabledSetting(monitor) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED);

    }

    @Override
    protected void onResume() {
        super.onResume();

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
        unregisterReceiver(receiver);
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
                Toast.makeText(this, "未授权定位信息权限将无法使用蓝牙", Toast.LENGTH_SHORT).show();
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
