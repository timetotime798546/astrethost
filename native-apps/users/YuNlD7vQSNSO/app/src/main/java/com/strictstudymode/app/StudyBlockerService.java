package com.strictstudymode.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class StudyBlockerService extends Service {

    private static final int NOTIFICATION_ID = 4829;
    private static final String CHANNEL_ID = "StudyBlockerChannel";

    private Handler monitorHandler;
    private Runnable monitorRunnable;
    private SharedPreferences prefs;

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = getSharedPreferences("StrictStudyPrefs", Context.MODE_PRIVATE);

        createNotificationChannel();
        Notification notification = buildNotification("Strict Study Mode Active", "Guarding your focus session...");
        startForeground(NOTIFICATION_ID, notification);

        monitorHandler = new Handler();
        monitorRunnable = new Runnable() {
            @Override
            public void run() {
                evaluateAndBlockDistractions();
                // Check once every 1000 milliseconds for responsive background intercept
                monitorHandler.postDelayed(this, 1000);
            }
        };
        monitorHandler.post(monitorRunnable);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (monitorHandler != null && monitorRunnable != null) {
            monitorHandler.removeCallbacks(monitorRunnable);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void evaluateAndBlockDistractions() {
        boolean active = isStudyModeCurrentlyActive();
        if (!active) {
            // Keep service alive on standby to support scheduled focus hours automatically kicking in
            updateNotification("Strict Study Mode: Standby", "Monitoring offline schedules");
            return;
        }

        updateNotification("🔒 Focus Engine Active", "Unsanctioned apps are currently blocked.");

        // Check current foreground application package
        String foregroundPkg = getForegroundPackage();
        if (foregroundPkg == null || foregroundPkg.isEmpty()) {
            return;
        }

        // Exclude our own app from blocker checks so settings and countdown operate smoothly
        if (foregroundPkg.equals(getPackageName())) {
            return;
        }

        // Query blacklisted package roster
        String saved = prefs.getString("blocked_packages_list", "");
        if (saved.isEmpty()) {
            return;
        }

        boolean shouldBlock = false;
        String[] arr = saved.split(",");
        for (String s : arr) {
            if (!s.trim().isEmpty() && s.trim().equals(foregroundPkg)) {
                shouldBlock = true;
                break;
            }
        }

        if (shouldBlock) {
            // Increment intercepted statistic attempt counter locally
            long currentAttempts = prefs.getLong("stat_blocked_attempts", 0);
            prefs.edit().putLong("stat_blocked_attempts", currentAttempts + 1).apply();

            // Intercept app launch by starting blocking overlay
            Intent lockScreen = new Intent(this, BlockOverlayActivity.class);
            lockScreen.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            lockScreen.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            lockScreen.putExtra("blocked_package", foregroundPkg);
            startActivity(lockScreen);
        }
    }

    private boolean isStudyModeCurrentlyActive() {
        long end = prefs.getLong("manual_session_end", 0);
        boolean manualActive = System.currentTimeMillis() < end;

        // Check weekly timetables
        boolean schedActive = false;
        String list = prefs.getString("schedules_list", "");
        if (!list.isEmpty()) {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            int currentDay = cal.get(java.util.Calendar.DAY_OF_WEEK);
            int currentMinutes = cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE);

            String[] ids = list.split(",");
            for (String id : ids) {
                if (id.trim().isEmpty()) continue;
                String info = prefs.getString("schedule_info_" + id, "");
                if (info.isEmpty()) continue;

                String[] parts = info.split("\\|");
                if (parts.length >= 5) {
                    int schedDay = Integer.parseInt(parts[0]);
                    int sHour = Integer.parseInt(parts[1]);
                    int sMin = Integer.parseInt(parts[2]);
                    int eHour = Integer.parseInt(parts[3]);
                    int eMin = Integer.parseInt(parts[4]);

                    int startMinutes = sHour * 60 + sMin;
                    int endMinutes = eHour * 60 + eMin;

                    if (schedDay == currentDay && currentMinutes >= startMinutes && currentMinutes <= endMinutes) {
                        schedActive = true;
                        break;
                    }
                }
            }
        }
        return manualActive || schedActive;
    }

    private String getForegroundPackage() {
        String currentApp = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            UsageStatsManager usm = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
            long time = System.currentTimeMillis();
            List<UsageStats> appList = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000 * 30, time);
            if (appList != null && !appList.isEmpty()) {
                SortedMap<Long, UsageStats> mySortedMap = new TreeMap<>();
                for (UsageStats usageStats : appList) {
                    mySortedMap.put(usageStats.getLastTimeUsed(), usageStats);
                }
                if (!mySortedMap.isEmpty()) {
                    currentApp = mySortedMap.get(mySortedMap.lastKey()).getPackageName();
                }
            }
        }
        return currentApp;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Strict Study Monitoring Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private Notification buildNotification(String title, String text) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 
                0, 
                notificationIntent, 
                PendingIntent.FLAG_IMMUTABLE
        );

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }

        return builder
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .setContentIntent(pendingIntent)
                .build();
    }

    private void updateNotification(String title, String text) {
        Notification notification = buildNotification(title, text);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, notification);
        }
    }
}