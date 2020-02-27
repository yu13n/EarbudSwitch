package app.tuuure.earbudswitch;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;


import com.microsoft.appcenter.analytics.Analytics;
import com.microsoft.appcenter.crashes.Crashes;
import com.microsoft.appcenter.utils.async.AppCenterConsumer;

import java.util.Random;

import static android.content.Context.MODE_PRIVATE;

public class SettingsFrag extends Fragment {
    private static final int REQUEST_PERMISSION_CODE = 5;

    private DialogActivity mContext;
    private Switch swLocation;

    private Switch swAggressive;
    private Switch swAnalytics;
    private Switch swCrashlytics;
    private EditText etKey;
    private SharedPreferences sp;

    private Boolean isPermissionGranted;


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mContext = (DialogActivity) context;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        swLocation = view.findViewById(R.id.sw_location);
        swAggressive = view.findViewById(R.id.sw_aggressive);
        swAnalytics = view.findViewById(R.id.sw_analytics);
        swCrashlytics = view.findViewById(R.id.sw_crashlytics);

        etKey = view.findViewById(R.id.et_key);

        view.findViewById(R.id.tv_aggressive).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(mContext, getString(R.string.aggressive_mode_toast), Toast.LENGTH_LONG).show();
            }
        });
        view.findViewById(R.id.tv_analytics).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(mContext, getString(R.string.firebase_analytics_toast), Toast.LENGTH_LONG).show();
            }
        });
        view.findViewById(R.id.tv_crashlytics).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(mContext, getString(R.string.firebase_crashlytics_toast), Toast.LENGTH_LONG).show();
            }
        });

        sp = mContext.getSharedPreferences(getString(R.string.app_title), MODE_PRIVATE);

        setSwitchOnCheckedChangeListener();
        setEditTextListener();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        String key = sp.getString("key", "");
        if (key.isEmpty()) {
            saveKey("");
        } else {
            etKey.setText(key);
        }

        isPermissionGranted = mContext.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        mContext.setSwitchAccent(new Switch[]{swLocation, swAggressive, swAnalytics, swCrashlytics});
        swLocation.setChecked(isPermissionGranted);
        swLocation.setClickable(!isPermissionGranted);

        swAggressive.setChecked(sp.getBoolean("aggressive", true));

        Analytics.isEnabled().thenAccept(new AppCenterConsumer<Boolean>() {

            @Override
            public void accept(Boolean enabled) {
                swAnalytics.setChecked(enabled);
            }
        });
        Crashes.isEnabled().thenAccept(new AppCenterConsumer<Boolean>() {

            @Override
            public void accept(Boolean enabled) {
                swCrashlytics.setChecked(enabled);
            }
        });

    }

    @Override
    public void onPause() {
        saveKey(etKey.getText().toString());
        super.onPause();
    }

    private void setSwitchOnCheckedChangeListener() {
        CompoundButton.OnCheckedChangeListener swListener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!buttonView.isPressed()) {
                    return;
                }
                switch (buttonView.getId()) {
                    case R.id.sw_location:
                        if (!isPermissionGranted) {
                            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSION_CODE);
                        }
                        break;
                    case R.id.sw_aggressive:
                        SharedPreferences.Editor editor = sp.edit();
                        editor.putBoolean("aggressive", isChecked);
                        editor.apply();
                        if (!isChecked)
                            Analytics.trackEvent("Aggressive_Mode_Off");
                        break;
                    case R.id.sw_analytics:
                        if (!isChecked)
                            Analytics.trackEvent("Analytics_Down");
                        Analytics.setEnabled(isChecked);
                        break;
                    case R.id.sw_crashlytics:
                        if (!isChecked)
                            Analytics.trackEvent("Crashlytics_Down");
                        Crashes.setEnabled(isChecked);
                        break;
                }
            }
        };
        swLocation.setOnCheckedChangeListener(swListener);
        swAggressive.setOnCheckedChangeListener(swListener);
        swAnalytics.setOnCheckedChangeListener(swListener);
        swCrashlytics.setOnCheckedChangeListener(swListener);
    }

    private boolean etKeyRequestFocus = true;

    private void setEditTextListener() {
        etKey.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    switch (event.getKeyCode()) {
                        case KeyEvent.KEYCODE_ENTER:
                        case KeyEvent.KEYCODE_NUMPAD_ENTER:
                            InputMethodManager imm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(mContext.getWindow().getDecorView().getWindowToken(), 0);
                            etKey.clearFocus();
                            saveKey(etKey.getText().toString());
                            return true;
                    }
                }
                return false;
            }
        });
        etKey.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    saveKey(etKey.getText().toString());
                    return false;
                }
                return false;
            }
        });
        etKey.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus && etKeyRequestFocus) {
                    etKeyRequestFocus = false;
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            etKey.requestFocus();
                            etKeyRequestFocus = true;
                        }
                    }, 200);
                }
            }
        });
    }

    private void saveKey(String key) {
        SharedPreferences.Editor editor = sp.edit();
        if (key.isEmpty()) {
            Random r = new Random();
            key = String.valueOf(r.nextInt(900000) + 100000);
        }
        editor.putString("key", key);
        editor.apply();
        etKey.setText(key);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_PERMISSION_CODE) {
            isPermissionGranted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            swLocation.setChecked(isPermissionGranted);
            if (!isPermissionGranted) {
                Toast.makeText(mContext, getText(R.string.request_permission), Toast.LENGTH_SHORT).show();
            }
        }
    }
}
