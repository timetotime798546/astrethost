package com.studentstudyplanner.app;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TaskAdapter extends ArrayAdapter<Task> {

    private LayoutInflater inflater;

    public TaskAdapter(Context context, List<Task> tasks) {
        super(context, 0, tasks);
        inflater = LayoutInflater.from(context);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_task, parent, false);
            holder = new ViewHolder();
            holder.titleTextView = (TextView) convertView.findViewById(R.id.task_title);
            holder.descriptionSnippetTextView = (TextView) convertView.findViewById(R.id.task_description_snippet);
            holder.dueDateTextView = (TextView) convertView.findViewById(R.id.task_due_date);
            holder.priorityTextView = (TextView) convertView.findViewById(R.id.task_priority);
            holder.statusTextView = (TextView) convertView.findViewById(R.id.task_status);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Task currentTask = getItem(position);
        if (currentTask != null) {
            holder.titleTextView.setText(currentTask.getTitle());

            String description = currentTask.getDescription();
            if (description != null && description.length() > 100) {
                holder.descriptionSnippetTextView.setText(description.substring(0, 100) + "...");
            } else {
                holder.descriptionSnippetTextView.setText(description);
            }

            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            holder.dueDateTextView.setText("Due: " + sdf.format(new Date(currentTask.getDueDate())));

            holder.priorityTextView.setText(currentTask.getPriorityString() + " Priority");
            // Set priority color
            int priorityColor = Color.GRAY;
            switch (currentTask.getPriority()) {
                case 0: // Low
                    priorityColor = Color.parseColor("#4CAF50"); // Green
                    break;
                case 1: // Medium
                    priorityColor = Color.parseColor("#FFC107"); // Amber
                    break;
                case 2: // High
                    priorityColor = Color.parseColor("#F44336"); // Red
                    break;
            }
            holder.priorityTextView.setTextColor(priorityColor);

            holder.statusTextView.setText(currentTask.getStatusString());
            // Set status color
            if (currentTask.getStatus() == 1) { // Completed
                holder.statusTextView.setTextColor(Color.parseColor("#009688")); // Teal
            } else { // Pending
                holder.statusTextView.setTextColor(Color.parseColor("#FF9800")); // Orange
            }
        }

        return convertView;
    }

    private static class ViewHolder {
        TextView titleTextView;
        TextView descriptionSnippetTextView;
        TextView dueDateTextView;
        TextView priorityTextView;
        TextView statusTextView;
    }
}