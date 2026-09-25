package com.studentplanner.app.adapters;

import android.content.Context;
import android.graphics.Color;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.studentplanner.app.R;
import com.studentplanner.app.models.Task;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class TaskAdapter extends ArrayAdapter<Task> {

    private final ArrayList<Task> tasks;
    private final Context context;

    public TaskAdapter(Context context, ArrayList<Task> tasks) {
        super(context, 0, tasks);
        this.context = context;
        this.tasks = tasks;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_task, parent, false);
        }

        Task currentTask = tasks.get(position);

        TextView taskTitleTextView = convertView.findViewById(R.id.task_title_text_view);
        TextView taskDueDateTextView = convertView.findViewById(R.id.task_due_date_text_view);
        TextView taskStatusTextView = convertView.findViewById(R.id.task_status_text_view);

        taskTitleTextView.setText(currentTask.getTitle());

        // Format due date and time
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(currentTask.getDueDateMillis());
        String dueDate = DateFormat.format("MMM dd, yyyy", calendar).toString();
        String dueTime = DateFormat.format("hh:mm a", calendar).toString();
        taskDueDateTextView.setText("Due: " + dueDate + " at " + dueTime);

        // Set status and color
        if (currentTask.isCompleted()) {
            taskStatusTextView.setText(R.string.mark_as_complete);
            taskStatusTextView.setTextColor(context.getResources().getColor(R.color.task_completed));
        } else {
            taskStatusTextView.setText(R.string.mark_as_incomplete);
            taskStatusTextView.setTextColor(context.getResources().getColor(R.color.task_pending));
        }

        return convertView;
    }
}