package com.studentstudyplanner.app;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.app.Notification;

public class AlarmReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "study_planner_alerts";
    private static final String CHANNEL_NAME = "Study Reminder Notifications";

    @Override
    public void onReceive(Context context, Intent intent) {
        String title = intent.getStringExtra("title");
        if (title == null || title.isEmpty()) {
            title = "Time to study!";
        }

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        // Setup notification channel on Android Oreo or above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("System alarms configured in Student Planner App");
            channel.enableVibration(true);
            nm.createNotificationChannel(channel);
        }

        // Build native notification using pure non-androidx API
        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(context, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(context);
        }

        builder.setContentTitle("Planner study alert!")
               .setContentText(title)
               .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
               .setAutoCancel(true)
               .setDefaults(Notification.DEFAULT_ALL);

        nm.notify((int) System.currentTimeMillis(), builder.build());
    }
}