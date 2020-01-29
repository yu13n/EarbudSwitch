package app.tuuure.earbudswitch;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.Context;

import java.lang.reflect.Method;
import java.util.List;

public class ProfileManager {
    public static final String TAG = "ProfileManager";
    private BluetoothA2dp bluetoothA2dp;
    private BluetoothHeadset bluetoothHeadset;
    private BluetoothAdapter bluetoothAdapter;
    private Context mContext;

    ProfileManager(Context context, BluetoothAdapter adapter, BluetoothProfile.ServiceListener proxyListener) {
        //初始化蓝牙服务
        bluetoothAdapter = adapter;
        mContext = context;
        adapter.getProfileProxy(mContext, proxyListener, BluetoothProfile.A2DP);
        adapter.getProfileProxy(mContext,proxyListener,BluetoothProfile.HEADSET);
    }

    void setAd2p(BluetoothA2dp bhtA2dp) {
        bluetoothA2dp = bhtA2dp;
    }

    void setHeadset(BluetoothHeadset bhtHeadset) {
        bluetoothHeadset = bhtHeadset;
    }

    boolean isProxyInited() {
        return bluetoothA2dp != null && bluetoothHeadset != null;
    }

    void a2dpConnect(BluetoothDevice device) {
//        setPriority(device, 100); //设置priority
        try {
            //通过反射获取BluetoothA2dp中connect方法（hide的），进行连接。
            Method connectMethod = BluetoothA2dp.class.getDeclaredMethod("connect", BluetoothDevice.class);
            connectMethod.invoke(bluetoothA2dp, device);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void a2dpDisconnect(BluetoothDevice device) {
        if (bluetoothA2dp.getConnectionState(device) != BluetoothProfile.STATE_CONNECTED) {
            return;
        }
//        setPriority(device, 0);
        try {
            //通过反射获取BluetoothA2dp中connect方法（hide的），断开连接。
            Method disconnectMethod = BluetoothA2dp.class.getDeclaredMethod("disconnect", BluetoothDevice.class);
            disconnectMethod.invoke(bluetoothA2dp, device);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

//    private void setPriority(BluetoothDevice device, int priority) {
//        if (bluetoothA2dp == null) return;
//        try {//通过反射获取BluetoothA2dp中setPriority方法（hide的），设置优先级
//            Method setPriorityMethod = BluetoothA2dp.class.getDeclaredMethod("setPriority", BluetoothDevice.class, int.class);
//            setPriorityMethod.invoke(bluetoothA2dp, device, priority);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    void headSetConnect(BluetoothDevice device) {
        try {
            Method disconnectMethod = BluetoothHeadset.class.getDeclaredMethod("connect", BluetoothDevice.class);
            disconnectMethod.invoke(bluetoothHeadset, device);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void headSetDisconnect(BluetoothDevice device) {
        if (bluetoothHeadset.getConnectionState(device) != BluetoothProfile.STATE_CONNECTED) {
            return;
        }
        try {
            Method disconnectMethod = BluetoothHeadset.class.getDeclaredMethod("disconnect", BluetoothDevice.class);
            disconnectMethod.invoke(bluetoothHeadset, device);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void destroy() {
        bluetoothAdapter.closeProfileProxy(BluetoothProfile.A2DP, bluetoothA2dp);
        bluetoothAdapter.closeProfileProxy(BluetoothProfile.HEADSET, bluetoothHeadset);
    }
}
