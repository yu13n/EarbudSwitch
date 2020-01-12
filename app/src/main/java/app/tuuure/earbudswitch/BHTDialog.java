package app.tuuure.earbudswitch;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


public class BHTDialog extends AppCompatActivity {
    final String TAG = "EBSDialog";
    RecyclerView rvDialog;
    DevicesAdapter rvAdapter;
    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_bht);
        mContext = this;

        Window window = getWindow();
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        rvDialog = findViewById(R.id.rv_dialog);
        LinearLayoutManager mLinearLayoutManager = new LinearLayoutManager(this);
        mLinearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        rvDialog.setLayoutManager(mLinearLayoutManager);
        rvDialog.setItemAnimator(new DefaultItemAnimator());

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null) {
            Toast.makeText(this, "设备不支持BLE", Toast.LENGTH_SHORT).show();
            finish();
        }
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        if (!bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.enable();
        }

        rvAdapter = new DevicesAdapter(bluetoothAdapter.getBondedDevices());
        rvAdapter.setOnItemClickListener(new DevicesAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                BluetoothDevice device = (BluetoothDevice) rvAdapter.devices.toArray()[position];
                Log.d(TAG, device.getName() + device.getAddress());
                //TextView tvDevice = view.findViewById(R.id.tv_device);

                Intent service = new Intent(mContext, EarbudsManager.class);
                service.putExtra("ops", EarbudsManager.ACTION_SCAN);
                service.putExtra(BluetoothDevice.EXTRA_DEVICE, device);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(service);
                } else {
                    startService(service);
                }
                finish();
            }
        });
        rvDialog.setAdapter(rvAdapter);

        Toolbar toolbar = findViewById(R.id.tb_dialog);
        //toolbar.inflateMenu(R.menu.menu_quicksetting);
        setSupportActionBar(toolbar);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_quicksetting, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_system_settings) {
            startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
            finish();
        }
        return super.onOptionsItemSelected(item);
    }
}
