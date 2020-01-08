package app.tuuure.earbudswitch;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class SetupActivity extends AppCompatActivity {
    private static final String TAG = "SetupActivity";
    private static final int REQUEST_PERMISSION_CODE = 14;
    private static final int REQUEST_BLUETOOTH_CODE = 15;

    private LinearLayout llLocation;
    private Switch swLocation;
    private LinearLayout llBluetooth;
    private Switch swBluetooth;
    private TextView tvBLE;

    private BluetoothManager bluetoothManager;
    BluetoothAdapter bluetoothAdapter;
    private Boolean isPermissionGranted = false;
    private Boolean isBluetoothOn = false;

    // 监听蓝牙关闭与自定义广播，用于关闭service
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) {
                Log.e(TAG, "Receiver action null");
                return;
            }
            if(BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)){
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                isBluetoothOn = state == BluetoothAdapter.STATE_ON;
                swBluetooth.setChecked(isBluetoothOn);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        llLocation = findViewById(R.id.ll_location);
        swLocation = findViewById(R.id.sw_location);
        llBluetooth = findViewById(R.id.ll_bluetooth);
        swBluetooth = findViewById(R.id.sw_bht_state);
        tvBLE = findViewById(R.id.tv_ble);

        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if(bluetoothManager != null){
            bluetoothAdapter = bluetoothManager.getAdapter();
        }

        CompoundButton.OnCheckedChangeListener swListener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                switch (buttonView.getId()) {
                    case R.id.sw_location:
                        buttonView.setClickable(!isChecked);
                        if(isChecked && !isPermissionGranted){
                            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSION_CODE);
                        }
                        break;
                    case R.id.sw_bht_state:
                        buttonView.setClickable(!isChecked);
                        if(isChecked && !isBluetoothOn) {
                            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                            startActivityForResult(intent, REQUEST_BLUETOOTH_CODE);
                        }
                        break;
                }
            }
        };
        swLocation.setOnCheckedChangeListener(swListener);
        swBluetooth.setOnCheckedChangeListener(swListener);

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bluetoothManager != null && getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            llLocation.setVisibility(View.VISIBLE);
            llBluetooth.setVisibility(View.VISIBLE);
            tvBLE.setVisibility(View.GONE);

            isPermissionGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
            swLocation.setChecked(isPermissionGranted);

            isBluetoothOn = bluetoothAdapter.isEnabled();
            swBluetooth.setChecked(isBluetoothOn);

            if (isPermissionGranted && isBluetoothOn) {
                setResult(Activity.RESULT_OK);
                finish();
            }
        } else {
            llLocation.setVisibility(View.GONE);
            llBluetooth.setVisibility(View.GONE);
            tvBLE.setVisibility(View.VISIBLE);
        }

        //注册监听蓝牙状态的receiver
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(receiver, intentFilter);
        Log.d(TAG, "Receiver registered");
    }

    @Override
    protected  void onPause(){
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
            }else{
                if (isBluetoothOn) {
                    setResult(Activity.RESULT_OK);
                    finish();
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_BLUETOOTH_CODE) {
            isBluetoothOn = resultCode == Activity.RESULT_OK;
            swBluetooth.setChecked(isBluetoothOn);
            if (isPermissionGranted && isBluetoothOn) {
                setResult(Activity.RESULT_OK);
                finish();
            }
        }
    }
}
