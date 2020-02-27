package app.tuuure.earbudswitch;

import android.bluetooth.BluetoothDevice;

class RecycleItem {
    String budsName;
    String budsAddress;
    String serverAddress;
    boolean isConnected;
    long lastSeen;

    RecycleItem(String budsName, String budsAddress, String serverAddress, long timeStamp) {
        this.budsName = budsName;
        this.budsAddress = budsAddress;
        this.serverAddress = serverAddress;
        this.isConnected = false;
        this.lastSeen = timeStamp;
    }

    RecycleItem(BluetoothDevice device) {
        this.budsName = device.getName();
        this.budsAddress = device.getAddress();
        this.serverAddress = null;
        this.isConnected = false;
        this.lastSeen = 0;
    }

    void setUnavailable(){
        this.serverAddress = null;
        this.lastSeen = 0;
    }
}
