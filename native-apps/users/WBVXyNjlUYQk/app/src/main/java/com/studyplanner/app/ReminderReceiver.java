package com.studyplanner.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class ReminderReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String reminderTitle = intent.getStringExtra("reminder_title");
        if (reminderTitle == null || reminderTitle.trim().isEmpty()) {
            reminderTitle = "Scheduled Study Focus Session!";
        }

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        String channelId = "study_planner_reminders_channel";
        Notification.Builder builder;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = nm.getNotificationChannel(channelId);
            if (channel == null) {
                channel = new NotificationChannel(
                        channelId,
                        "Study Alarms Channel",
                        NotificationManager.IMPORTANCE_HIGH
                );
                channel.setDescription("Delivers real-time student planner notifications and exam schedule triggers.");
                channel.enableVibration(true);
                nm.createNotificationChannel(channel);
            }
            builder = new Notification.Builder(context, channelId);
        } else {
            builder = new Notification.Builder(context);
        }

        builder.setContentTitle("Time to Study!")
                .setContentText(reminderTitle)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setAutoCancel(true)
                .setPriority(Notification.PRIORITY_HIGH)
                .setDefaults(Notification.DEFAULT_ALL);

        nm.notify((int) System.currentTimeMillis(), builder.build());
    }
}