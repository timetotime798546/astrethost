package com.chargingvoicealert.app;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private TextView tvBatteryPercentage;
    private TextView tvChargingStatus;
    private TextView tvPowerSource;
    private TextView tvMonitoringStatus;

    private Button btnStart;
    private Button btnStop;
    private Button btnBatteryOptimization;

    private Switch switchVoiceAlerts;
    private Switch switchPercentAlerts;
    private Switch switchAlertConnected;
    private Switch switchAlertDisconnected;
    private Switch switchAlertPercent;
    private Switch switchAlertFull;

    private SharedPreferences prefs;

    private final BroadcastReceiver updateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) return;
            String action = intent.getAction();

            if ("com.chargingvoicealert.app.BATTERY_UPDATE".equals(action)) {
                int level = intent.getIntExtra("level", -1);
                boolean isCharging = intent.getBooleanExtra("isCharging", false);
                int plugged = intent.getIntExtra("plugged", -1);
                updateBatteryUI(level, isCharging, plugged);
            } else if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);

                float batteryPct = level * 100 / (float) scale;
                boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING 
                        || status == BatteryManager.BATTERY_STATUS_FULL;
                updateBatteryUI((int) batteryPct, isCharging, plugged);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("ChargingVoicePrefs", MODE_PRIVATE);

        // View Binding
        tvBatteryPercentage = (TextView) findViewById(R.id.tv_battery_percentage);
        tvChargingStatus = (TextView) findViewById(R.id.tv_charging_status);
        tvPowerSource = (TextView) findViewById(R.id.tv_power_source);
        tvMonitoringStatus = (TextView) findViewById(R.id.tv_monitoring_status);

        btnStart = (Button) findViewById(R.id.btn_start);
        btnStop = (Button) findViewById(R.id.btn_stop);
        btnBatteryOptimization = (Button) findViewById(R.id.btn_battery_optimization);

        switchVoiceAlerts = (Switch) findViewById(R.id.switch_voice_alerts);
        switchPercentAlerts = (Switch) findViewById(R.id.switch_percent_alerts);
        switchAlertConnected = (Switch) findViewById(R.id.switch_alert_connected);
        switchAlertDisconnected = (Switch) findViewById(R.id.switch_alert_disconnected);
        switchAlertPercent = (Switch) findViewById(R.id.switch_alert_percent);
        switchAlertFull = (Switch) findViewById(R.id.switch_alert_full);

        // Initialize Preference Switch Values
        switchVoiceAlerts.setChecked(prefs.getBoolean("pref_voice_alerts_enabled", true));
        switchPercentAlerts.setChecked(prefs.getBoolean("pref_percent_alerts_enabled", true));
        switchAlertConnected.setChecked(prefs.getBoolean("pref_alert_connected_enabled", true));
        switchAlertDisconnected.setChecked(prefs.getBoolean("pref_alert_disconnected_enabled", true));
        switchAlertPercent.setChecked(prefs.getBoolean("pref_alert_percent_enabled", true));
        switchAlertFull.setChecked(prefs.getBoolean("pref_alert_full_enabled", true));

        // Switch Listeners
        CompoundButton.OnCheckedChangeListener changeListener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences.Editor editor = prefs.edit();
                int id = buttonView.getId();
                if (id == R.id.switch_voice_alerts) {
                    editor.putBoolean("pref_voice_alerts_enabled", isChecked);
                } else if (id == R.id.switch_percent_alerts) {
                    editor.putBoolean("pref_percent_alerts_enabled", isChecked);
                } else if (id == R.id.switch_alert_connected) {
                    editor.putBoolean("pref_alert_connected_enabled", isChecked);
                } else if (id == R.id.switch_alert_disconnected) {
                    editor.putBoolean("pref_alert_disconnected_enabled", isChecked);
                } else if (id == R.id.switch_alert_percent) {
                    editor.putBoolean("pref_alert_percent_enabled", isChecked);
                } else if (id == R.id.switch_alert_full) {
                    editor.putBoolean("pref_alert_full_enabled", isChecked);
                }
                editor.apply();
            }
        };

        switchVoiceAlerts.setOnCheckedChangeListener(changeListener);
        switchPercentAlerts.setOnCheckedChangeListener(changeListener);
        switchAlertConnected.setOnCheckedChangeListener(changeListener);
        switchAlertDisconnected.setOnCheckedChangeListener(changeListener);
        switchAlertPercent.setOnCheckedChangeListener(changeListener);
        switchAlertFull.setOnCheckedChangeListener(changeListener);

        // Start button handling
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startMonitoringService();
            }
        });

        // Stop button handling
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopMonitoringService();
            }
        });

        // Battery optimization settings redirections
        btnBatteryOptimization.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openBatterySettings();
            }
        });

        // Request notification permission dynamically for target Android 13+ SDK compatibility
        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 101);
            }
        }
    }

    private void startMonitoringService() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("pref_service_enabled", true);
        editor.apply();

        Intent intent = new Intent(this, ChargingMonitorService.class);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }
            Toast.makeText(this, "Monitoring Voice Alerts Started", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to start background monitor service.", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
        updateMonitoringStatusUI();
    }

    private void stopMonitoringService() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("pref_service_enabled", false);
        editor.apply();

        Intent intent = new Intent(this, ChargingMonitorService.class);
        stopService(intent);
        Toast.makeText(this, "Monitoring Voice Alerts Stopped", Toast.LENGTH_SHORT).show();
        updateMonitoringStatusUI();
    }

    private void updateBatteryUI(int level, boolean isCharging, int plugged) {
        if (level >= 0) {
            tvBatteryPercentage.setText(level + "%");
        } else {
            tvBatteryPercentage.setText("--%");
        }

        if (isCharging) {
            tvChargingStatus.setText("Status: Charging");
            tvChargingStatus.setTextColor(0xFF4CAF50);
        } else {
            tvChargingStatus.setText("Status: Not Charging");
            tvChargingStatus.setTextColor(0xFFE53935);
        }

        String sourceText = "Power Source: Battery";
        if (plugged == BatteryManager.BATTERY_PLUGGED_AC) {
            sourceText = "Power Source: AC Wall Adapter";
        } else if (plugged == BatteryManager.BATTERY_PLUGGED_USB) {
            sourceText = "Power Source: USB Port";
        } else if (plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS) {
            sourceText = "Power Source: Wireless Charging";
        }
        tvPowerSource.setText(sourceText);
    }

    private boolean isServiceRunning(Class<?> serviceClass) {
        android.app.ActivityManager manager = (android.app.ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (manager != null) {
            for (android.app.ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (serviceClass.getName().equals(service.service.getClassName())) {
                    return true;
                }
            }
        }
        return false;
    }

    private void updateMonitoringStatusUI() {
        boolean isRunning = isServiceRunning(ChargingMonitorService.class);
        if (isRunning) {
            tvMonitoringStatus.setText("Monitoring: Active");
            tvMonitoringStatus.setTextColor(0xFF4CAF50);
            btnStart.setEnabled(false);
            btnStop.setEnabled(true);
        } else {
            tvMonitoringStatus.setText("Monitoring: Inactive");
            tvMonitoringStatus.setTextColor(0xFFE53935);
            btnStart.setEnabled(true);
            btnStop.setEnabled(false);
        }
    }

    private void openBatterySettings() {
        try {
            Intent intent = new Intent();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                intent.setAction(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
            } else {
                intent.setAction(android.provider.Settings.ACTION_SETTINGS);
            }
            startActivity(intent);
        } catch (Exception e) {
            try {
                Intent intent = new Intent(android.provider.Settings.ACTION_SETTINGS);
                startActivity(intent);
            } catch (Exception ex) {
                Toast.makeText(this, "Could not open System Settings.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateMonitoringStatusUI();

        // Register receivers for active UI updates
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.chargingvoicealert.app.BATTERY_UPDATE");
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(updateReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            unregisterReceiver(updateReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}