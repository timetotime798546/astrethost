package com.studyplannerpro.app;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Vibrator;
import android.util.Log;

public class ReminderReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "STUDY_PLANNER_ALERTS";

    @Override
    public void onReceive(Context context, Intent intent) {
        String taskTitle = intent.getStringExtra("TASK_TITLE");
        if (taskTitle == null || taskTitle.isEmpty()) {
            taskTitle = "You have an upcoming study event scheduled!";
        }

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        // Setup notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Study Planner Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Delivers scheduled study alarms");
            nm.createNotificationChannel(channel);
        }

        Intent mainIntent = new Intent(context, MainActivity.class);
        mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        
        PendingIntent pendingIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingIntent = PendingIntent.getActivity(context, 0, mainIntent, PendingIntent.FLAG_IMMUTABLE);
        } else {
            pendingIntent = PendingIntent.getActivity(context, 0, mainIntent, 0);
        }

        android.app.Notification notification;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notification = new android.app.Notification.Builder(context, CHANNEL_ID)
                    .setContentTitle("Study Planner Pro Alert")
                    .setContentText(taskTitle)
                    .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                    .build();
        } else {
            notification = new android.app.Notification.Builder(context)
                    .setContentTitle("Study Planner Pro Alert")
                    .setContentText(taskTitle)
                    .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                    .getNotification();
        }

        nm.notify((int) System.currentTimeMillis(), notification);

        // Buzz device
        try {
            Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null) {
                vibrator.vibrate(800);
            }
        } catch (Exception e) {
            Log.e("StudyPlanner", "Vibration failed", e);
        }
    }
}