package app.tuuure.earbudswitch;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;

import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


public class BHTDialog extends AppCompatActivity {
    final String TAG = "EBSDialog";
    RecyclerView rvDialog;
    DevicesAdapter rvAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_bht);

        Window window = getWindow();
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        rvDialog = findViewById(R.id.rv_dialog);
        LinearLayoutManager mLinearLayoutManager = new LinearLayoutManager(this);
        mLinearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        rvDialog.setLayoutManager(mLinearLayoutManager);
        rvDialog.setItemAnimator(new DefaultItemAnimator());
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        rvAdapter = new DevicesAdapter(bluetoothAdapter.getBondedDevices());
        rvAdapter.setOnItemClickListener(new DevicesAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                BluetoothDevice device = (BluetoothDevice)rvAdapter.devices.toArray()[position];
                Log.d(TAG,device.getName() + device.getAddress());
            }
        });
        rvDialog.setAdapter(rvAdapter);


        Toolbar toolbar = findViewById(R.id.tb_dialog);
        toolbar.inflateMenu(R.menu.menu_quicksetting);
    }
}
