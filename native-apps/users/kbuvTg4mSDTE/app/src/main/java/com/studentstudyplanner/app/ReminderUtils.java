package com.studentstudyplanner.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ReminderUtils {

    public static void setReminder(Context context, int taskId, long reminderTimeMillis, String message) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, ReminderBroadcastReceiver.class);
        intent.setAction(ReminderBroadcastReceiver.ACTION_REMINDER);
        intent.putExtra(ReminderBroadcastReceiver.EXTRA_TASK_ID, (long) taskId);
        intent.putExtra(ReminderBroadcastReceiver.EXTRA_MESSAGE, message);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                taskId, // Unique request code for each task
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        if (alarmManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderTimeMillis, pendingIntent);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, reminderTimeMillis, pendingIntent);
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, reminderTimeMillis, pendingIntent);
            }
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
            //Toast.makeText(context, "Reminder set for " + sdf.format(new Date(reminderTimeMillis)), Toast.LENGTH_SHORT).show();
        }
    }

    public static void cancelReminder(Context context, int taskId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, ReminderBroadcastReceiver.class);
        intent.setAction(ReminderBroadcastReceiver.ACTION_REMINDER); // Action must match for cancellation
        // Ensure extras are the same for the PendingIntent to match for cancellation
        intent.putExtra(ReminderBroadcastReceiver.EXTRA_TASK_ID, (long) taskId);
        // Message extra is not strictly needed for cancellation if taskId is enough, but good practice to match
        intent.putExtra(ReminderBroadcastReceiver.EXTRA_MESSAGE, ""); // Dummy message if needed

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                taskId, // Unique request code must match
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
            //Toast.makeText(context, "Reminder cancelled.", Toast.LENGTH_SHORT).show();
        }
    }
}