package com.virtualmousetouchpad.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final int REQUEST_OVERLAY_PERMISSION = 1234;

    private TextView tvOverlayStatus;
    private TextView tvAccessibilityStatus;
    private Button btnGrantOverlay;
    private Button btnGrantAccessibility;
    private SeekBar sbSensitivity;
    private SeekBar sbTouchpadSize;
    private TextView tvSensitivityVal;
    private TextView tvTouchpadSizeVal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvOverlayStatus = (TextView) findViewById(R.id.tv_overlay_status);
        tvAccessibilityStatus = (TextView) findViewById(R.id.tv_accessibility_status);
        btnGrantOverlay = (Button) findViewById(R.id.btn_grant_overlay);
        btnGrantAccessibility = (Button) findViewById(R.id.btn_grant_accessibility);
        sbSensitivity = (SeekBar) findViewById(R.id.sb_sensitivity);
        sbTouchpadSize = (SeekBar) findViewById(R.id.sb_touchpad_size);
        tvSensitivityVal = (TextView) findViewById(R.id.tv_sensitivity_val);
        tvTouchpadSizeVal = (TextView) findViewById(R.id.tv_touchpad_size_val);

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        // Load Settings
        float savedSensitivity = prefs.getFloat("pref_sensitivity", 1.5f);
        int savedSize = prefs.getInt("pref_touchpad_size", 220);

        // Sensitivity range: 0.5 to 4.5. Seekbar max is 40. progress = (val - 0.5) * 10
        int sensitivityProgress = Math.round((savedSensitivity - 0.5f) * 10f);
        sbSensitivity.setProgress(sensitivityProgress);
        tvSensitivityVal.setText(String.format("%.1fx", savedSensitivity));

        // Touchpad size range: 150 to 350. Seekbar max is 200. progress = val - 150
        int sizeProgress = savedSize - 150;
        sbTouchpadSize.setProgress(sizeProgress);
        tvTouchpadSizeVal.setText(savedSize + " dp");

        // Action Listeners
        btnGrantOverlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION);
                } else {
                    Toast.makeText(MainActivity.this, "Overlay permission granted automatically on this device.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnGrantAccessibility.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                startActivity(intent);
                Toast.makeText(MainActivity.this, "Find 'Virtual Mouse Touchpad' and turn it ON", Toast.LENGTH_LONG).show();
            }
        });

        sbSensitivity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float val = 0.5f + (progress / 10f);
                tvSensitivityVal.setText(String.format("%.1fx", val));
                prefs.edit().putFloat("pref_sensitivity", val).apply();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        sbTouchpadSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int val = 150 + progress;
                tvTouchpadSizeVal.setText(val + " dp");
                prefs.edit().putInt("pref_touchpad_size", val).apply();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkPermissions();
    }

    private void checkPermissions() {
        boolean hasOverlay = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            hasOverlay = Settings.canDrawOverlays(this);
        }

        if (hasOverlay) {
            tvOverlayStatus.setText("Status: GRANTED");
            tvOverlayStatus.setTextColor(0xFF2E7D32); // Green
            btnGrantOverlay.setEnabled(false);
            btnGrantOverlay.setText("Permission Granted");
        } else {
            tvOverlayStatus.setText("Status: DENIED");
            tvOverlayStatus.setTextColor(0xFFC62828); // Red
            btnGrantOverlay.setEnabled(true);
            btnGrantOverlay.setText("Grant Overlay Permission");
        }

        boolean hasAccessibility = isAccessibilityServiceEnabled();
        if (hasAccessibility) {
            tvAccessibilityStatus.setText("Status: ACTIVE");
            tvAccessibilityStatus.setTextColor(0xFF2E7D32); // Green
            btnGrantAccessibility.setText("Service Active");
        } else { 
            tvAccessibilityStatus.setText("Status: INACTIVE");
            tvAccessibilityStatus.setTextColor(0xFFC62828); // Red
            btnGrantAccessibility.setText("Enable Accessibility Service");
        }
    }

    private boolean isAccessibilityServiceEnabled() {
        int accessibilityEnabled = 0;
        final String service = getPackageName() + "/" + MouseService.class.getName();
        try {
            accessibilityEnabled = Settings.Secure.getInt(
                    getContentResolver(),
                    android.provider.Settings.Secure.ACCESSIBILITY_ENABLED);
        } catch (Settings.SettingNotFoundException e) {
            // Error handling
        }
        TextUtils.SimpleStringSplitter mStringColonSplitter = new TextUtils.SimpleStringSplitter(':');

        if (accessibilityEnabled == 1) {
            String settingValue = Settings.Secure.getString(
                    getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (settingValue != null) {
                mStringColonSplitter.setString(settingValue);
                while (mStringColonSplitter.hasNext()) {
                    String accessibilityService = mStringColonSplitter.next();
                    if (accessibilityService.equalsIgnoreCase(service)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}