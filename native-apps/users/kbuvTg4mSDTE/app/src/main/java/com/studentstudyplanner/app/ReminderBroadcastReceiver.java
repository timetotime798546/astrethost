package com.studentstudyplanner.app;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.Toast;
import android.app.Notification;
import android.app.Notification.Builder;

import java.util.List;

public class ReminderBroadcastReceiver extends BroadcastReceiver {

    public static final String ACTION_REMINDER = "com.studentstudyplanner.app.ACTION_REMINDER";
    public static final String EXTRA_TASK_ID = "task_id";
    public static final String EXTRA_MESSAGE = "message";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null) {
            String action = intent.getAction();

            if (ACTION_REMINDER.equals(action)) {
                // This is our custom reminder action
                long taskId = intent.getLongExtra(EXTRA_TASK_ID, -1);
                String message = intent.getStringExtra(EXTRA_MESSAGE);

                if (taskId != -1 && message != null) {
                    showNotification(context, (int) taskId, message);

                    // Optionally, update the reminder's isActive status to false if it's a one-time reminder
                    // For persistent reminders, you might re-schedule it here for a future time
                    StudyPlannerDatabaseHelper db = new StudyPlannerDatabaseHelper(context);
                    Reminder reminder = db.getReminderByTaskId(taskId);
                    if (reminder != null) {
                        reminder.setIsActive(0); // Mark as inactive after triggering
                        db.updateReminder(reminder);
                        // No need to cancel alarm manager, it's a one-shot by default or will be re-scheduled
                    }
                    db.close();
                }
            } else if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
                // Re-schedule all active reminders after device reboot
                Toast.makeText(context, "Rescheduling Study Planner Reminders...", Toast.LENGTH_LONG).show();
                StudyPlannerDatabaseHelper db = new StudyPlannerDatabaseHelper(context);
                List<Reminder> activeReminders = db.getAllActiveReminders();
                for (Reminder reminder : activeReminders) {
                    // Only re-schedule if reminder time is in the future
                    if (reminder.getReminderTime() > System.currentTimeMillis()) {
                        ReminderUtils.setReminder(context, (int) reminder.getTaskId(), reminder.getReminderTime(), reminder.getMessage());
                    } else {
                        // If reminder time is in the past, mark it as inactive
                        reminder.setIsActive(0);
                        db.updateReminder(reminder);
                    }
                }
                db.close();
            }
        }
    }

    private void showNotification(Context context, int taskId, String message) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        Intent intent = new Intent(context, TaskDetailActivity.class);
        intent.putExtra("task_id", (long) taskId);
        // Ensure that clicking the notification opens the correct TaskDetailActivity
        // Use a unique request code for each task reminder pending intent
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                taskId, // Use taskId as request code for uniqueness
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        // Ensure the notification channel is created for Android O and above
        String channelId = "student_study_planner_channel"; // Must match channel ID in MainActivity
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = notificationManager.getNotificationChannel(channelId);
            if (channel == null) {
                // Channel might not have been created if app was never opened after install on O+
                // Re-create it here or ensure MainActivity creates it on app launch
                channel = new NotificationChannel(channelId, "Study Planner Reminders", NotificationManager.IMPORTANCE_HIGH);
                channel.setDescription("Channel for study planner task reminders");
                notificationManager.createNotificationChannel(channel);
            }
        }

        Builder builder = new Builder(context, channelId)
                .setSmallIcon(R.drawable.icon) // Use existing app icon
                .setContentTitle("Task Reminder!")
                .setContentText(message)
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        notificationManager.notify(taskId, builder.build()); // Use taskId as notification ID for uniqueness
    }
}