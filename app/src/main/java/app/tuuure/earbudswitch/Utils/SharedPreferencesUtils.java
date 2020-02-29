package app.tuuure.earbudswitch.Utils;

import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;

import app.tuuure.earbudswitch.CusApplication;

import static android.content.Context.MODE_PRIVATE;

public class SharedPreferencesUtils {
    private SharedPreferences sp;
    private SharedPreferences.Editor editor;
    private static final String SP_NAME = "Earbudswitch";

    private SharedPreferencesUtils() {
        sp = CusApplication.getContext().getSharedPreferences(SP_NAME, MODE_PRIVATE);
        editor = sp.edit();
    }

    /**
     * 单例模式
     */
    private static SharedPreferencesUtils instance;//单例模式 双重检查锁定

    public static SharedPreferencesUtils getInstance() {
        if (instance == null) {
            synchronized (SharedPreferencesUtils.class) {
                if (instance == null) {
                    instance = new SharedPreferencesUtils();
                }
            }
        }
        return instance;
    }

    private static final String KEY_NAME = "key";

    public void putKey(String key) {
        editor.putString(KEY_NAME, key);
        editor.commit();
    }

    public String getKey() {
        return sp.getString(KEY_NAME, "");
    }

    private static final String AGGER_NAME = "aggressive";

    public void putAggre(boolean aggre) {
        editor.putBoolean(AGGER_NAME, aggre);
        editor.commit();
    }

    public boolean getAggre() {
        return sp.getBoolean(AGGER_NAME, true);
    }

    private static final String TWS_NAME = "twsp";

    public String getTWS() {
        return sp.getString(TWS_NAME, "");
    }

    public void putTWS(JSONArray jsonArray) {
        editor.putString(TWS_NAME, jsonArray.toString());
        editor.commit();
    }

    public void clearTWS() {
        editor.remove(TWS_NAME);
    }
}