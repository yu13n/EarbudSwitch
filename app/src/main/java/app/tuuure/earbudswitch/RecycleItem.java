package app.tuuure.earbudswitch;

import android.bluetooth.BluetoothDevice;

class RecycleItem {
    String budsName;
    String budsAddress;
    String serverAddress;
    boolean isConnected;

    RecycleItem(String budsName, String budsAddress, String serverAddress) {
        this.budsName = budsName;
        this.budsAddress = budsAddress;
        this.serverAddress = serverAddress;
        this.isConnected = false;
    }

    RecycleItem(BluetoothDevice device) {
        this.budsName = device.getName();
        this.budsAddress = device.getAddress();
        this.serverAddress = null;
        this.isConnected = false;
    }
}
