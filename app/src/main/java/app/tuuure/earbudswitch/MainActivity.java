package app.tuuure.earbudswitch;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import java.util.LinkedHashSet;

public class MainActivity extends AppCompatActivity {

    final String TAG = "Earbudswitch";
    final int REQUEST_CODE = 11;

    RecyclerView rv_devices;
    DevicesAdapter rvAdapter;

    BleClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        client = new BleClient(this);

        rv_devices = findViewById(R.id.rv_devicesfound);
        LinearLayoutManager mLinearLayoutManager = new LinearLayoutManager(this);
        mLinearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        rv_devices.setLayoutManager(mLinearLayoutManager);
        rv_devices.setItemAnimator(new DefaultItemAnimator());

        rvAdapter = new DevicesAdapter(new LinkedHashSet<BluetoothDevice>());
        rvAdapter.setOnItemClickListener(new DevicesAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                BluetoothDevice device = (BluetoothDevice)rvAdapter.devices.toArray()[position];
                Log.d(TAG,device.getName() + device.getAddress());
            }
        });
        rv_devices.setAdapter(rvAdapter);

        if(client.isSetupNeeded){
            Intent intent = new Intent(getApplicationContext(), SetupActivity.class);
            startActivityForResult(intent, REQUEST_CODE);
        }else{
            startDiscoverys();
        }
    }

    @Override
    protected void onResume(){
        super.onResume();
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mReceiver, filter);
    }

    @Override
    protected void onPause(){
        unregisterReceiver(mReceiver);
        super.onPause();
    }
    private void startDiscoverys(){
        client.bluetoothAdapter.cancelDiscovery();
        if(client.bluetoothAdapter.startDiscovery()){
            Log.d("Test","succeed");

        }else{
            Log.d("Test","fail");
        }
    }

    @Override
    protected void onDestroy() {
        client.bluetoothAdapter.cancelDiscovery();

        super.onDestroy();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode,resultCode,data);

        if (requestCode == REQUEST_CODE) {
            switch (resultCode) {
                case Activity.RESULT_OK:
                    client.initBluetooth();
                    client.isSetupNeeded = false;
                    startDiscoverys();
                    break;
                case Activity.RESULT_CANCELED:
                    finish();
                    break;
            }
        }
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action){
                case BluetoothDevice.ACTION_FOUND:
                    Log.i("Android Bluetooth Scanner", "Found bluetooth device.");
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if(device.getName() !=null){
                        rvAdapter.addDevice(device);
                    }
                    Log.i("EBS",device.getName() + device.getAddress());
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                    Log.i("Android Bluetooth Scanner", "Bluetooth device discovering started.");
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                    Log.i("Android Bluetooth Scanner", "Bluetooth device discovering finished.");
                    break;
            }
        }
    };

}
