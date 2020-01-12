package app.tuuure.earbudswitch;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;

import java.lang.reflect.Method;
import java.util.List;

public class A2dpManager {
    public static final String TAG = "A2dpManager";
    private BluetoothA2dp bluetoothA2dp;
    private BluetoothAdapter bluetoothAdapter;

    A2dpManager(Context context, BluetoothAdapter adapter, BluetoothProfile.ServiceListener a2dpListener) {
        //初始化蓝牙服务
        bluetoothAdapter = adapter;
        adapter.getProfileProxy(context, a2dpListener, BluetoothProfile.A2DP);
    }

    void setAd2p(BluetoothA2dp bhtA2dp) {
        bluetoothA2dp = bhtA2dp;
    }

    void connect(BluetoothDevice device) {
        setPriority(device, 100); //设置priority
        try {
            //通过反射获取BluetoothA2dp中connect方法（hide的），进行连接。
            Method connectMethod = BluetoothA2dp.class.getDeclaredMethod("connect", BluetoothDevice.class);
            connectMethod.invoke(bluetoothA2dp, device);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void disconnect(BluetoothDevice device) {
        setPriority(device, 0);
        try {
            //通过反射获取BluetoothA2dp中connect方法（hide的），断开连接。
            Method disconnectMethod = BluetoothA2dp.class.getDeclaredMethod("disconnect", BluetoothDevice.class);
            disconnectMethod.invoke(bluetoothA2dp, device);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setPriority(BluetoothDevice device, int priority) {
        if (bluetoothA2dp == null) return;
        try {//通过反射获取BluetoothA2dp中setPriority方法（hide的），设置优先级
            Method setPriorityMethod = BluetoothA2dp.class.getDeclaredMethod("setPriority", BluetoothDevice.class, int.class);
            setPriorityMethod.invoke(bluetoothA2dp, device, priority);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void destroy() {
        bluetoothAdapter.closeProfileProxy(BluetoothProfile.A2DP, bluetoothA2dp);
    }

    List<BluetoothDevice> getConnectedDevices() {
        return bluetoothA2dp.getConnectedDevices();
    }

    int getConnectionState(BluetoothDevice device) {
        return bluetoothA2dp.getConnectionState(device);
    }
}
