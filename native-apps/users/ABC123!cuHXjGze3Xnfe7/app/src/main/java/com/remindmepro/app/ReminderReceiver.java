package com.remindmepro.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

public class ReminderReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "reminder_channel_pro";
    private static final String CHANNEL_NAME = "RemindMe Pro Alerts";

    @Override
    public void onReceive(Context context, Intent intent) {
        String title = intent.getStringExtra("title");
        String description = intent.getStringExtra("desc");
        long id = intent.getLongExtra("id", 0);

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("RemindMe Pro alerts notifications channel");
            nm.createNotificationChannel(channel);
        }

        Intent mainIntent = new Intent(context, MainActivity.class);
        mainIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        
        int pendingIntentFlag = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingIntentFlag |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pi = PendingIntent.getActivity(context, (int) id, mainIntent, pendingIntentFlag);

        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(context, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(context);
        }

        int iconResId = context.getResources().getIdentifier("icon", "drawable", context.getPackageName());
        if (iconResId == 0) {
            iconResId = android.R.drawable.ic_lock_idle_alarm;
        }

        builder.setContentTitle(title)
                .setContentText(description)
                .setSmallIcon(iconResId)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setSound(alarmSound);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setCategory(Notification.CATEGORY_ALARM)
                   .setPriority(Notification.PRIORITY_HIGH);
        }

        nm.notify((int) id, builder.build());

        // Update persistence since reminder is triggered/expired
        ReminderDatabaseHelper dbHelper = new ReminderDatabaseHelper(context);
        dbHelper.updateReminderActive(id, false);
        
        // Broadcast local refresh request to MainActivity
        Intent refreshIntent = new Intent("com.remindmepro.app.REFRESH_DATA");
        context.sendBroadcast(refreshIntent);
    }
}