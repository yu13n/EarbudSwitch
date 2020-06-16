package app.tuuure.earbudswitch.Utils;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONException;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;

import static java.lang.Class.forName;

public class TwsUtils {

    public interface Callback {
        void notice();
    }

    public static void isTWS(Context context, Set<BluetoothDevice> devices, final Callback callBack) {
        isTWS(context, getAddress(devices), callBack);
    }

    public static void isTWS(Context context, final String[] macAddress, final Callback callBack) {
        final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        BluetoothProfile.ServiceListener proxyListener = new BluetoothProfile.ServiceListener() {
            @Override
            public void onServiceDisconnected(int profile) {
            }

            @Override
            public void onServiceConnected(int profile, BluetoothProfile proxy) {
                if (profile != BluetoothProfile.A2DP) {
                    return;
                }
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    bluetoothAdapter.closeProfileProxy(profile, proxy);
                    return;
                }
                                /*
                                    public static final int SOURCE_CODEC_TYPE_SBC = 0;
                                    public static final int SOURCE_CODEC_TYPE_AAC = 1;
                                    public static final int SOURCE_CODEC_TYPE_APTX = 2;
                                    public static final int SOURCE_CODEC_TYPE_APTX_HD = 3;
                                    public static final int SOURCE_CODEC_TYPE_LDAC = 4;
                                    public static final int SOURCE_CODEC_TYPE_MAX = 5;

                                    public static final int CHANNEL_MODE_NONE = 0;
                                    public static final int CHANNEL_MODE_MONO = 0x1 << 0;
                                    public static final int CHANNEL_MODE_STEREO = 0x1 << 1;
                                 */

                for (String address : macAddress) {
                    BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
                    try {
                        //通过反射获取BluetoothA2dp中connect方法（hide的），进行连接。
                        Class<?> BluetoothCodecStatus = (Class<?>) forName("android.bluetooth.BluetoothCodecStatus");
                        Class<?> BluetoothCodecConfig = (Class<?>) forName("android.bluetooth.BluetoothCodecConfig");

                        Method method = BluetoothA2dp.class.getDeclaredMethod("getCodecStatus", BluetoothDevice.class);
                        Object codecStatus = method.invoke(proxy, device);
                        Method getCodecConfig = BluetoothCodecStatus.getDeclaredMethod("getCodecConfig");
                        Object codecConfig = getCodecConfig.invoke(codecStatus);
                        Method getCodecTypegetCodecType = BluetoothCodecConfig.getDeclaredMethod("getCodecType");
                        int codecType = (int) getCodecTypegetCodecType.invoke(codecConfig);
                        Method getChannelMode = BluetoothCodecConfig.getDeclaredMethod("getChannelMode");
                        int channelMode = (int) getChannelMode.invoke(codecConfig);

                        Field CHANNEL_MODE_MONO = BluetoothCodecConfig.getDeclaredField("CHANNEL_MODE_MONO");
                        if (channelMode != CHANNEL_MODE_MONO.getInt(codecConfig)) {
                            //双声道设备，排除
                            bluetoothAdapter.closeProfileProxy(profile, proxy);
                            return;
                        }
                        Field SOURCE_CODEC_TYPE_APTX_TWSP = null;
                        for (Field field : BluetoothCodecConfig.getDeclaredFields()) {
                            String name = field.getName();
                            if (name.contains("APTX") && name.contains("TWS")) {
                                SOURCE_CODEC_TYPE_APTX_TWSP = field;
                                break;
                            }
                        }
                        if (SOURCE_CODEC_TYPE_APTX_TWSP == null || (int) SOURCE_CODEC_TYPE_APTX_TWSP.get(codecConfig) != codecType) {
                            //不支持，或目标不为TWS设备
                            bluetoothAdapter.closeProfileProxy(profile, proxy);
                            return;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        bluetoothAdapter.closeProfileProxy(profile, proxy);
                        return;
                    }
                }
                bluetoothAdapter.closeProfileProxy(profile, proxy);
                //目标设备使用TWSP协议
                callBack.notice();
            }
        };
        bluetoothAdapter.getProfileProxy(context, proxyListener, BluetoothProfile.A2DP);
    }

    public static String getTWS(BluetoothDevice device) {
        return getTWS(device.getAddress());
    }

    public static String getTWS(String macAddress) {
        String raw = SharedPreferencesUtils.getInstance().getTWS();
        if (raw.contains(macAddress)) {
            JSONArray array;
            try {
                array = new JSONArray(raw);
                for (int i = 0; i < array.length(); i++) {
                    JSONArray item = array.getJSONArray(i);
                    if (item.toString().contains(macAddress)) {
                        for (int k = 0; k < item.length(); k++) {
                            String temp = item.getString(k);
                            if (!temp.equals(macAddress)) {
                                return temp;
                            }
                        }
                    }
                }
            } catch (JSONException ignored) {
            }
        }
        //目标设备不存在于缓存中，寻找名字类似的设备
        String origin = BluetoothAdapter.getDefaultAdapter()
                .getRemoteDevice(macAddress)
                .getName()
                .toLowerCase().replaceAll("[lr\\W]", "");
        for (BluetoothDevice device : BluetoothAdapter.getDefaultAdapter().getBondedDevices()) {
            if (device.getBluetoothClass().getMajorDeviceClass() != BluetoothClass.Device.Major.AUDIO_VIDEO
                    || device.getName() == null || device.getName().isEmpty())
                continue;
            if (!device.getAddress().equals(macAddress)) {
                if (origin.equals(device.getName().toLowerCase().replaceAll("[lr\\W]", "")))
                    return device.getAddress();
            }
        }
        return "";
    }

    public static boolean isTWSContain(BluetoothDevice device) {
        return isTWSContain(device.getAddress());
    }

    public static boolean isTWSContain(String macAddress) {
        return SharedPreferencesUtils.getInstance().getTWS().contains(macAddress);
    }

    private static String[] getAddress(Set<BluetoothDevice> devices) {
        ArrayList<String> array = new ArrayList<>(2);
        for (BluetoothDevice device : devices) {
            array.add(device.getAddress());
        }
        String[] result = new String[array.size()];
        array.toArray(result);
        return result;
    }

    public static void putTWS(Set<BluetoothDevice> devices) {
        putTWS(getAddress(devices));
    }

    public static void putTWS(String[] macAddress) {
        String raw = SharedPreferencesUtils.getInstance().getTWS();
        if (macAddress.length != 2) {
            return;
        }
        String macAddress1 = macAddress[0];
        String macAddress2 = macAddress[1];
        if (raw.contains(macAddress1) || raw.contains(macAddress2)) {
            return;
        }
        JSONArray array;
        if (raw.isEmpty()) {
            array = new JSONArray();
        } else {
            try {
                array = new JSONArray(raw);
            } catch (JSONException e) {
                array = new JSONArray();
            }
        }
        JSONArray item = new JSONArray();
        item.put(macAddress1);
        item.put(macAddress2);
        array.put(item);
        SharedPreferencesUtils.getInstance().putTWS(array);
    }
}
