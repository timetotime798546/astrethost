package com.voicecontrolassistant.app;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends Activity {

    private static final int REQUEST_MIC_PERMISSION = 200;
    private static final int REQUEST_OVERLAY_PERMISSION = 201;

    private TextView tvServiceStatusBadge;
    private TextView tvServiceStatusDesc;
    private TextView tvServiceStatusDot;
    private LinearLayout cardOverlayPermission;
    private Button btnActionAccessibility;
    private Button btnActionOverlay;
    private LinearLayout llAppsContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvServiceStatusBadge = (TextView) findViewById(R.id.tv_service_status_badge);
        tvServiceStatusDesc = (TextView) findViewById(R.id.tv_service_status_desc);
        tvServiceStatusDot = (TextView) findViewById(R.id.tv_service_status_dot);
        cardOverlayPermission = (LinearLayout) findViewById(R.id.card_overlay_permission);
        btnActionAccessibility = (Button) findViewById(R.id.btn_action_accessibility);
        btnActionOverlay = (Button) findViewById(R.id.btn_action_overlay);
        llAppsContainer = (LinearLayout) findViewById(R.id.ll_detected_apps_container);

        btnActionAccessibility.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                startActivity(intent);
            }
        });

        btnActionOverlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION);
                }
            }
        });

        // Request Microphone/Audio Recording Permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_MIC_PERMISSION);
            }
        }

        // List launchable applications
        listLauncherApps();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateServiceStatusUI();
    }

    private void updateServiceStatusUI() {
        boolean isServiceEnabled = isAccessibilityServiceEnabled();
        boolean isOverlayGranted = true;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            isOverlayGranted = Settings.canDrawOverlays(this);
        }

        // Control permissions display block
        if (isOverlayGranted) {
            cardOverlayPermission.setVisibility(View.GONE);
        } else {
            cardOverlayPermission.setVisibility(View.VISIBLE);
        }

        if (isServiceEnabled) {
            tvServiceStatusBadge.setText("ACTIVE");
            tvServiceStatusBadge.setTextColor(0xFF4CAF50); // Green
            tvServiceStatusDot.setBackgroundResource(R.drawable.circle_processing);
            tvServiceStatusDesc.setText("Voice Control Assistant is running! Click the glowing overlay orb anywhere on screen.");
            btnActionAccessibility.setText("Configure Service Settings");
            btnActionAccessibility.setBackgroundColor(0xFF78909C);
        } else {
            tvServiceStatusBadge.setText("INACTIVE / DISABLED");
            tvServiceStatusBadge.setTextColor(0xFFF44336); // Red
            tvServiceStatusDot.setBackgroundResource(R.drawable.circle_error);
            tvServiceStatusDesc.setText("Accessibility permissions are required to perform programmatic clicks, text typing, scrolls, and UI traversing.");
            btnActionAccessibility.setText("Enable Accessibility Service");
            btnActionAccessibility.setBackgroundColor(0xFF1A237E);
        }
    }

    private boolean isAccessibilityServiceEnabled() {
        AccessibilityManager am = (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (am != null) {
            List<AccessibilityServiceInfo> enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC);
            for (AccessibilityServiceInfo service : enabledServices) {
                if (service.getId().contains(getPackageName())) {
                    return true;
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
        // Limit display count on screen dashboard to keep layout lightweight
        int limit = Math.min(appNames.size(), 20);
        for (int i = 0; i < limit; i++) {
            TextView tv = new TextView(this);
            tv.setText("✓  " + appNames.get(i));
            tv.setTextColor(0xFF37474F);
            tv.setTextSize(13);
            tv.setPadding(6, 4, 6, 4);
            llAppsContainer.addView(tv);
        }
    }
}