package com.studentplanner.app.notifications;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.text.format.DateFormat;

import com.studentplanner.app.R;
import com.studentplanner.app.models.Task;

import java.util.Calendar;
import java.util.Locale;

public class NotificationHelper {

    private static final String CHANNEL_ID = "study_planner_channel";
    private static final String CHANNEL_NAME = "Task Reminders";
    private static final String CHANNEL_DESCRIPTION = "Notifications for upcoming study tasks";

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription(CHANNEL_DESCRIPTION);
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    public static void scheduleReminder(Context context, Task task) {
        if (task.isCompleted()) { // Do not schedule reminders for completed tasks
            cancelReminder(context, task.getId());
            return;
        }

        createNotificationChannel(context); // Ensure channel exists

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.setAction("com.studentplanner.app.ACTION_REMINDER");
        intent.putExtra("task_id", task.getId());

        // Use the task ID as the request code for unique PendingIntent
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                task.getId(), // Unique request code per task
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE // Use FLAG_IMMUTABLE
        );

        if (alarmManager != null) {
            // Schedule the alarm for the task's due date
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, task.getDueDateMillis(), pendingIntent);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, task.getDueDateMillis(), pendingIntent);
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, task.getDueDateMillis(), pendingIntent);
            }
        }
    }

    public static void cancelReminder(Context context, int taskId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.setAction("com.studentplanner.app.ACTION_REMINDER");
        intent.putExtra("task_id", taskId); // Important to match the original intent extras for cancellation

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                taskId, // Must match the original request code
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
    }

    public static void showNotification(Context context, Task task, String subjectName) {
        createNotificationChannel(context); // Ensure channel exists

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Intent to open MainActivity when notification is clicked
        Intent resultIntent = new Intent(context, com.studentplanner.app.MainActivity.class);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(
                context,
                0,
                resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(task.getDueDateMillis());
        String dueDate = DateFormat.format("MMM dd, yyyy", calendar).toString();
        String dueTime = DateFormat.format("hh:mm a", calendar).toString();

        String contentTitle = String.format(Locale.getDefault(), context.getString(R.string.task_reminder_title), task.getTitle());
        String contentText = String.format(Locale.getDefault(), context.getString(R.string.task_reminder_content), dueDate, dueTime, subjectName);

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(context, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(context);
        }

        builder.setContentTitle(contentTitle)
                .setContentText(contentText)
                .setSmallIcon(R.drawable.icon)
                .setContentIntent(resultPendingIntent)
                .setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_ALL)
                .setPriority(Notification.PRIORITY_HIGH);

        if (notificationManager != null) {
            notificationManager.notify(task.getId(), builder.build()); // Use task ID as notification ID
        }
    }
}