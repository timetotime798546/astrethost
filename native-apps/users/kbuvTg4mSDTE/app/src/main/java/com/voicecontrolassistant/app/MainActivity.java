package com.voicecontrolassistant.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends Activity {

    private static final int REQUEST_MIC_PERMISSION = 200;
    
    private TextView tvServiceStatusBadge;
    private TextView tvServiceStatusDesc;
    private Button btnActionAccessibility;
    private LinearLayout llAppsContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvServiceStatusBadge = (TextView) findViewById(R.id.tv_service_status_badge);
        tvServiceStatusDesc = (TextView) findViewById(R.id.tv_service_status_desc);
        btnActionAccessibility = (Button) findViewById(R.id.btn_action_accessibility);
        llAppsContainer = (LinearLayout) findViewById(R.id.ll_detected_apps_container);

        btnActionAccessibility.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                startActivity(intent);
            }
        });

        // Request Audio Permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_MIC_PERMISSION);
            }
        }

        // List installed launcher apps
        listLauncherApps();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateServiceStatusUI();
    }

    private void updateServiceStatusUI() {
        boolean isServiceEnabled = isAccessibilityServiceEnabled();
        if (isServiceEnabled) {
            tvServiceStatusBadge.setText("ACTIVE");
            tvServiceStatusBadge.setTextColor(0xFF4CAF50); // Material Green
            tvServiceStatusDesc.setText("Voice Control Assistant is active! Use the floating microphone overlay anywhere on your screen.");
            btnActionAccessibility.setText("Configure Service Settings");
            btnActionAccessibility.setBackgroundColor(0xFF757575); // Dark grey
        } else {
            tvServiceStatusBadge.setText("INACTIVE / DISABLED");
            tvServiceStatusBadge.setTextColor(0xFFF44336); // Material Red
            tvServiceStatusDesc.setText("Accessibility permissions are required to click, scroll, and parse application screens. Please enable inside settings below.");
            btnActionAccessibility.setText("Enable Accessibility Service");
            btnActionAccessibility.setBackgroundColor(0xFF2196F3); // Blue
        }
    }

    private boolean isAccessibilityServiceEnabled() {
        String servicePath = getPackageName() + "/" + VoiceAccessibilityService.class.getName();
        int accessibilityEnabled = 0;
        try {
            accessibilityEnabled = Settings.Secure.getInt(
                    getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_ENABLED);
        } catch (Settings.SettingNotFoundException e) {
            // Ignored
        }

        if (accessibilityEnabled == 1) {
            String settingValue = Settings.Secure.getString(
                    getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (settingValue != null) {
                TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(':');
                splitter.setString(settingValue);
                while (splitter.hasNext()) {
                    String accessabilityService = splitter.next();
                    if (accessabilityService.equalsIgnoreCase(servicePath)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void listLauncherApps() {
        PackageManager pm = getPackageManager();
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> apps = pm.queryIntentActivities(mainIntent, 0);

        List<String> appNames = new ArrayList<>();
        for (ResolveInfo info : apps) {
            appNames.add(info.loadLabel(pm).toString());
        }
        Collections.sort(appNames);

        llAppsContainer.removeAllViews();
        for (String name : appNames) {
            TextView tv = new TextView(this);
            tv.setText("✓  " + name);
            tv.setTextColor(0xFF333333);
            tv.setTextSize(14);
            tv.setPadding(8, 6, 8, 6);
            llAppsContainer.addView(tv);
        }
    }
}