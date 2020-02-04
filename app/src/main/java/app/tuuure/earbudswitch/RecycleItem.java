package app.tuuure.earbudswitch;

import android.bluetooth.BluetoothDevice;

import java.util.Set;

class RecycleItem {
    String budsName;
    String budsAddress;
    String serverAddress;

    RecycleItem(String budsName, String budsAddress, String serverAddress) {
        this.budsName = budsName;
        this.budsAddress = budsAddress;
        this.serverAddress = serverAddress;
    }

    RecycleItem(BluetoothDevice device) {
        this.budsName = device.getName();
        this.budsAddress = device.getAddress();
        this.serverAddress = null;
    }
}
