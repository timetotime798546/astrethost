package com.voicecontrolassistant.app;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
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

    // AI Settings variables
    private EditText etApiKey;
    private Spinner spModel;
    private EditText etCustomModel;
    private TextView tvCustomModelLabel;
    private Button btnSaveSettings;

    // Manual Command variables
    private EditText etTestCommand;
    private Button btnRunTestCommand;

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

        // Bind AI settings UI
        etApiKey = (EditText) findViewById(R.id.et_api_key);
        spModel = (Spinner) findViewById(R.id.sp_model);
        etCustomModel = (EditText) findViewById(R.id.et_custom_model);
        tvCustomModelLabel = (TextView) findViewById(R.id.tv_custom_model_label);
        btnSaveSettings = (Button) findViewById(R.id.btn_save_settings);

        // Bind Manual Command UI
        etTestCommand = (EditText) findViewById(R.id.et_test_command);
        btnRunTestCommand = (Button) findViewById(R.id.btn_run_test_command);

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

        // Initialize Gemini Model Spinner
        final String[] models = {"Gemini 2.5 Flash", "Gemini 2.5 Pro", "Gemini 1.5 Flash", "Gemini 1.5 Pro", "Custom Model"};
        ArrayAdapter<String> modelAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, models);
        modelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spModel.setAdapter(modelAdapter);

        // Load Saved Settings
        final SharedPreferences prefs = getSharedPreferences("VoiceControlPrefs", Context.MODE_PRIVATE);
        etApiKey.setText(prefs.getString("gemini_api_key", ""));
        String savedModel = prefs.getString("gemini_model", "Gemini 2.5 Flash");
        etCustomModel.setText(prefs.getString("gemini_custom_model", ""));

        int selectedPos = 0;
        for (int i = 0; i < models.length; i++) {
            if (models[i].equals(savedModel)) {
                selectedPos = i;
                break;
            }
        }
        spModel.setSelection(selectedPos);
        updateCustomModelVisibility(selectedPos);

        spModel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateCustomModelVisibility(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnSaveSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String apiKey = etApiKey.getText().toString().trim();
                String selectedModel = spModel.getSelectedItem().toString();
                String customModel = etCustomModel.getText().toString().trim();

                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("gemini_api_key", apiKey);
                editor.putString("gemini_model", selectedModel);
                editor.putString("gemini_custom_model", customModel);
                editor.apply();

                Toast.makeText(MainActivity.this, "AI Settings saved locally!", Toast.LENGTH_SHORT).show();
            }
        });

        // Setup Manual Test Command Trigger
        btnRunTestCommand.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String cmdText = etTestCommand.getText().toString().trim();
                if (cmdText.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please enter a test command first", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!isAccessibilityServiceEnabled()) {
                    Toast.makeText(MainActivity.this, "Please enable Accessibility Service first!", Toast.LENGTH_LONG).show();
                    return;
                }

                // Send dynamic intent execution to the running service
                Intent serviceIntent = new Intent(MainActivity.this, VoiceAccessibilityService.class);
                serviceIntent.putExtra("COMMAND_TEXT", cmdText);
                startService(serviceIntent);
                Toast.makeText(MainActivity.this, "Sending command to AI Interpreter...", Toast.LENGTH_SHORT).show();
            }
        });

        // List launchable applications
        listLauncherApps();
    }

    private void updateCustomModelVisibility(int position) {
        if (position == 4) { // "Custom Model" selected
            etCustomModel.setVisibility(View.VISIBLE);
            tvCustomModelLabel.setVisibility(View.VISIBLE);
        } else {
            etCustomModel.setVisibility(View.GONE);
            tvCustomModelLabel.setVisibility(View.GONE);
        }
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