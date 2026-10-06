package com.strictstudymode.app;

import android.app.Activity;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.util.Set;

public class MainActivity extends Activity {

    private TextView txtStatusState;
    private TextView txtCountdown;
    private TextView txtStrictAlert;
    private LinearLayout permissionCard;
    private Button btnGrantPermission;

    private LinearLayout layoutSessionCreator;
    private EditText edtMinutes;
    private Switch switchStrict;
    private Button btnStartSession;

    private LinearLayout layoutActiveControls;
    private Button btnStopSession;
    private TextView txtStrictDisabledWarning;

    private LinearLayout btnManageApps;
    private LinearLayout btnManageSchedules;
    private TextView txtBlockedCount;
    private TextView txtSchedulesCount;

    private TextView txtStatMinutes;
    private TextView txtStatAttempts;

    private Handler handler;
    private Runnable updateRunnable;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("StrictStudyPrefs", Context.MODE_PRIVATE);

        // Bind UI Elements
        txtStatusState = (TextView) findViewById(R.id.txtStatusState);
        txtCountdown = (TextView) findViewById(R.id.txtCountdown);
        txtStrictAlert = (TextView) findViewById(R.id.txtStrictAlert);
        permissionCard = (LinearLayout) findViewById(R.id.permissionCard);
        btnGrantPermission = (Button) findViewById(R.id.btnGrantPermission);

        layoutSessionCreator = (LinearLayout) findViewById(R.id.layoutSessionCreator);
        edtMinutes = (EditText) findViewById(R.id.edtMinutes);
        switchStrict = (Switch) findViewById(R.id.switchStrict);
        btnStartSession = (Button) findViewById(R.id.btnStartSession);

        layoutActiveControls = (LinearLayout) findViewById(R.id.layoutActiveControls);
        btnStopSession = (Button) findViewById(R.id.btnStopSession);
        txtStrictDisabledWarning = (TextView) findViewById(R.id.txtStrictDisabledWarning);

        btnManageApps = (LinearLayout) findViewById(R.id.btnManageApps);
        btnManageSchedules = (LinearLayout) findViewById(R.id.btnManageSchedules);
        txtBlockedCount = (TextView) findViewById(R.id.txtBlockedCount);
        txtSchedulesCount = (TextView) findViewById(R.id.txtSchedulesCount);

        txtStatMinutes = (TextView) findViewById(R.id.txtStatMinutes);
        txtStatAttempts = (TextView) findViewById(R.id.txtStatAttempts);

        // Listeners
        btnGrantPermission.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                startActivity(intent);
            }
        });

        btnStartSession.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startStudySession();
            }
        });

        btnStopSession.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopStudySession();
            }
        });

        btnManageApps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean active = isStudyModeCurrentlyActive();
                boolean strict = prefs.getBoolean("strict_mode", false);
                if (active && strict) {
                    Toast.makeText(MainActivity.this, "Strict study mode is active. Cannot edit block list!", Toast.LENGTH_SHORT).show();
                } else {
                    startActivity(new Intent(MainActivity.this, BlockedAppsActivity.class));
                }
            }
        });

        btnManageSchedules.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean active = isStudyModeCurrentlyActive();
                boolean strict = prefs.getBoolean("strict_mode", false);
                if (active && strict) {
                    Toast.makeText(MainActivity.this, "Strict study mode is active. Cannot edit weekly schedules!", Toast.LENGTH_SHORT).show();
                } else {
                    startActivity(new Intent(MainActivity.this, ScheduleActivity.class));
                }
            }
        });

        // Initialize background loop
        handler = new Handler();
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                updateUIState();
                handler.postDelayed(this, 1000);
            }
        };

        // Start Blocker background service to monitor continuously
        startBlockerService();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkUsageAccessPermission();
        updateUIState();
        handler.post(updateRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(updateRunnable);
    }

    private void checkUsageAccessPermission() {
        boolean granted = false;
        try {
            AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
            int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), getPackageName());
            granted = (mode == AppOpsManager.MODE_ALLOWED);
        } catch (Exception e) {
            granted = false;
        }

        if (granted) {
            permissionCard.setVisibility(View.GONE);
        } else {
            permissionCard.setVisibility(View.VISIBLE);
        }
    }

    private void startStudySession() {
        String minsStr = edtMinutes.getText().toString().trim();
        if (minsStr.isEmpty()) {
            Toast.makeText(this, "Please enter custom duration!", Toast.LENGTH_SHORT).show();
            return;
        }

        int minutes = Integer.parseInt(minsStr);
        if (minutes <= 0) {
            Toast.makeText(this, "Duration must be greater than 0 minutes!", Toast.LENGTH_SHORT).show();
            return;
        }

        long endTimeMillis = System.currentTimeMillis() + ((long) minutes * 60 * 1000);
        boolean strict = switchStrict.isChecked();

        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong("manual_session_end", endTimeMillis);
        editor.putBoolean("strict_mode", strict);
        editor.putLong("session_total_minutes_registered", minutes);
        editor.putBoolean("session_counted_stat", false);
        editor.apply();

        Toast.makeText(this, "Strict Study Session activated for " + minutes + " minutes!", Toast.LENGTH_LONG).show();

        startBlockerService();
        updateUIState();
    }

    private void stopStudySession() {
        boolean strict = prefs.getBoolean("strict_mode", false);
        if (strict && isManualSessionRunning()) {
            Toast.makeText(this, "STRICT MODE IS ACTIVE! You cannot stop this session early.", Toast.LENGTH_LONG).show();
            return;
        }

        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong("manual_session_end", 0);
        editor.putBoolean("strict_mode", false);
        editor.apply();

        Toast.makeText(this, "Study Session stopped.", Toast.LENGTH_SHORT).show();
        updateUIState();
    }

    private boolean isManualSessionRunning() {
        long end = prefs.getLong("manual_session_end", 0);
        return System.currentTimeMillis() < end;
    }

    private boolean isScheduledSessionRunning() {
        // Evaluate schedules
        String list = prefs.getString("schedules_list", "");
        if (list.isEmpty()) return false;

        java.util.Calendar cal = java.util.Calendar.getInstance();
        int currentDay = cal.get(java.util.Calendar.DAY_OF_WEEK); // Sunday = 1, Mon = 2
        int currentMinutes = cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE);

        String[] ids = list.split(",");
        for (String id : ids) {
            if (id.trim().isEmpty()) continue;
            String info = prefs.getString("schedule_info_" + id, "");
            if (info.isEmpty()) continue;

            String[] parts = info.split("\\|");
            if (parts.length >= 5) {
                int schedDay = Integer.parseInt(parts[0]);
                int startHour = Integer.parseInt(parts[1]);
                int startMin = Integer.parseInt(parts[2]);
                int endHour = Integer.parseInt(parts[3]);
                int endMin = Integer.parseInt(parts[4]);

                int startMinutes = startHour * 60 + startMin;
                int endMinutes = endHour * 60 + endMin;

                if (schedDay == currentDay) {
                    if (currentMinutes >= startMinutes && currentMinutes <= endMinutes) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isStudyModeCurrentlyActive() {
        return isManualSessionRunning() || isScheduledSessionRunning();
    }

    private void updateUIState() {
        boolean active = isStudyModeCurrentlyActive();
        boolean isStrict = prefs.getBoolean("strict_mode", false);

        if (active) {
            txtStatusState.setText("ACTIVE");
            txtStatusState.setTextColor(0xFF27AE60); // green
            txtCountdown.setVisibility(View.VISIBLE);

            if (isManualSessionRunning()) {
                long remainingMillis = prefs.getLong("manual_session_end", 0) - System.currentTimeMillis();
                if (remainingMillis < 0) {
                    remainingMillis = 0;
                }
                long hours = (remainingMillis / (1000 * 60 * 60)) % 24;
                long minutes = (remainingMillis / (1000 * 60)) % 60;
                long seconds = (remainingMillis / 1000) % 60;
                txtCountdown.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));

                if (isStrict) {
                    txtStrictAlert.setVisibility(View.VISIBLE);
                    txtStrictAlert.setText("🔒 STRICT LOCK ENGAGED: Back/Settings Guarded!");
                    layoutActiveControls.setVisibility(View.VISIBLE);
                    btnStopSession.setEnabled(false);
                    btnStopSession.setText("CANNOT CANCEL - STRICT ACTIVE");
                    txtStrictDisabledWarning.setVisibility(View.VISIBLE);
                    layoutSessionCreator.setVisibility(View.GONE);
                } else {
                    txtStrictAlert.setVisibility(View.GONE);
                    layoutActiveControls.setVisibility(View.VISIBLE);
                    btnStopSession.setEnabled(true);
                    btnStopSession.setText("STOP SESSION NOW");
                    txtStrictDisabledWarning.setVisibility(View.GONE);
                    layoutSessionCreator.setVisibility(View.GONE);
                }
            } else {
                // Active due to recurring weekly schedule!
                txtCountdown.setText("SCHEDULE LOCK");
                txtCountdown.setTextSize(26);
                txtStrictAlert.setVisibility(View.VISIBLE);
                txtStrictAlert.setText("🔒 Scheduled focus session is currently running.");
                layoutActiveControls.setVisibility(View.VISIBLE);
                btnStopSession.setEnabled(false);
                btnStopSession.setText("SCHEDULE CONTROLLED BLOCK");
                txtStrictDisabledWarning.setText("To stop, you must delete the timetable slot in Schedule Settings when unlocked.");
                txtStrictDisabledWarning.setVisibility(View.VISIBLE);
                layoutSessionCreator.setVisibility(View.GONE);
            }
        } else {
            // Check if manual session has ended naturally to increment stats
            long lastEnd = prefs.getLong("manual_session_end", 0);
            if (lastEnd > 0 && System.currentTimeMillis() >= lastEnd) {
                boolean counted = prefs.getBoolean("session_counted_stat", false);
                if (!counted) {
                    long addedMinutes = prefs.getLong("session_total_minutes_registered", 0);
                    long totalMins = prefs.getLong("stat_total_minutes", 0);
                    prefs.edit()
                         .putLong("stat_total_minutes", totalMins + addedMinutes)
                         .putBoolean("session_counted_stat", true)
                         .putLong("manual_session_end", 0)
                         .apply();
                }
            }

            txtStatusState.setText("INACTIVE");
            txtStatusState.setTextColor(0xFF7F8C8D); // muted gray
            txtCountdown.setVisibility(View.GONE);
            txtStrictAlert.setVisibility(View.GONE);
            layoutActiveControls.setVisibility(View.GONE);
            layoutSessionCreator.setVisibility(View.VISIBLE);
        }

        // Update Stats fields
        long mins = prefs.getLong("stat_total_minutes", 0);
        long attempts = prefs.getLong("stat_blocked_attempts", 0);
        txtStatMinutes.setText(String.valueOf(mins));
        txtStatAttempts.setText(String.valueOf(attempts));

        // Apps & schedule counts
        Set<String> blockedApps = BlockedAppsActivity.getBlockedPackages(this);
        txtBlockedCount.setText(blockedApps.size() + " apps listed");

        String list = prefs.getString("schedules_list", "");
        int schedCount = 0;
        if (!list.isEmpty()) {
            schedCount = list.split(",").length;
        }
        txtSchedulesCount.setText(schedCount + " active timetables");
    }

    private void startBlockerService() {
        Intent serviceIntent = new Intent(this, StudyBlockerService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }
}