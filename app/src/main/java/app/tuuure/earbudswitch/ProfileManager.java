package app.tuuure.earbudswitch;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;


import java.lang.reflect.Method;

class ProfileManager {
    static final String TAG = "ProfileManager";
    private static final String CONNECT = "connect";
    private static final String DISCONNECT = "disconnect";

    private static void manage(Context mContext, final String action, final BluetoothDevice device) {
        final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothProfile.ServiceListener proxyListener = new BluetoothProfile.ServiceListener() {
            @Override
            public void onServiceDisconnected(int profile) {
            }

            @Override
            public void onServiceConnected(int profile, BluetoothProfile proxy) {
                Class<?> proxyClass;

                switch (profile) {
                    case BluetoothProfile.HEADSET:
                        proxyClass = BluetoothHeadset.class;
                        break;
                    case BluetoothProfile.A2DP:
                        proxyClass = BluetoothA2dp.class;
                        break;
                    default:
                        return;
                }
                try {
                    //通过反射获取BluetoothA2dp中connect方法（hide的），进行连接。
                    Method connectMethod = proxyClass.getDeclaredMethod(action, BluetoothDevice.class);
                    if (action.equals(DISCONNECT)) {
                        if (device == null) {
                            for (BluetoothDevice e : proxy.getConnectedDevices()) {
                                connectMethod.invoke(proxy, e);
                            }
                        } else {
                            if (proxy.getConnectedDevices().contains(device)) {
                                connectMethod.invoke(proxy, device);
                            }
                        }
                    } else {
                        connectMethod.invoke(proxy, device);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                bluetoothAdapter.closeProfileProxy(profile, proxy);
            }
        };
        bluetoothAdapter.getProfileProxy(mContext, proxyListener, BluetoothProfile.A2DP);
        bluetoothAdapter.getProfileProxy(mContext, proxyListener, BluetoothProfile.HEADSET);
    }

    static void disconnect(Context mContext, BluetoothDevice device) {
        manage(mContext, DISCONNECT, device);
    }

    static void connect(Context mContext, BluetoothDevice device) {
        manage(mContext, CONNECT, device);
    }

    static int getConnectionState() {
        try {
            Method method = BluetoothAdapter.class.getDeclaredMethod("getConnectionState");
            return (int) method.invoke(BluetoothAdapter.getDefaultAdapter());
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
        return -1;
    }
}
