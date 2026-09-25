package com.studentplanner.app.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.Toast;

import com.studentplanner.app.data.DatabaseHelper;
import com.studentplanner.app.models.Task;

import java.util.ArrayList;

public class ReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }

        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            // Reschedule all incomplete tasks after reboot
            DatabaseHelper dbHelper = new DatabaseHelper(context);
            ArrayList<Task> incompleteTasks = dbHelper.getAllIncompleteTasks();
            for (int i = 0; i < incompleteTasks.size(); i++) {
                Task task = incompleteTasks.get(i);
                if (task.getDueDateMillis() > System.currentTimeMillis()) { // Only reschedule future tasks
                    NotificationHelper.scheduleReminder(context, task);
                }
            }
            Toast.makeText(context, "Study Planner reminders re-scheduled.", Toast.LENGTH_LONG).show();
        } else if ("com.studentplanner.app.ACTION_REMINDER".equals(intent.getAction())) {
            int taskId = intent.getIntExtra("task_id", -1);
            if (taskId != -1) {
                DatabaseHelper dbHelper = new DatabaseHelper(context);
                Task task = dbHelper.getTask(taskId);
                if (task != null && !task.isCompleted()) { // Only show reminder if task is not completed
                    String subjectName = dbHelper.getSubject(task.getSubjectId()).getName();
                    NotificationHelper.showNotification(context, task, subjectName);
                }
            }
        }
    }
}