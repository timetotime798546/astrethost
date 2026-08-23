package com.taskflow.app;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.TextView;
import java.util.List;

public class TaskAdapter extends BaseAdapter {

    private Context context;
    private List<Task> taskList;
    private OnTaskStatusChangeListener listener;

    public interface OnTaskStatusChangeListener {
        void onTaskStatusChanged(Task task, boolean isCompleted);
        void onTaskDeleted(Task task);
    }

    public TaskAdapter(Context context, List<Task> taskList, OnTaskStatusChangeListener listener) {
        this.context = context;
        this.taskList = taskList;
        this.listener = listener;
    }

    public void updateData(List<Task> newTaskList) {
        this.taskList = newTaskList;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return taskList.size();
    }

    @Override
    public Object getItem(int position) {
        return taskList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return taskList.get(position).getId();
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.task_item, parent, false);
        }

        final Task task = taskList.get(position);

        View viewPriorityIndicator = convertView.findViewById(R.id.viewPriorityIndicator);
        CheckBox checkboxComplete = convertView.findViewById(R.id.checkboxComplete);
        final TextView textTaskTitle = convertView.findViewById(R.id.textTaskTitle);
        TextView textTaskDesc = convertView.findViewById(R.id.textTaskDesc);
        TextView textTaskDueDate = convertView.findViewById(R.id.textTaskDueDate);
        ImageButton btnDeleteTask = convertView.findViewById(R.id.btnDeleteTask);

        if ("HIGH".equals(task.getPriority())) {
            viewPriorityIndicator.setBackgroundColor(Color.parseColor("#FF1744"));
        } else if ("MEDIUM".equals(task.getPriority())) {
            viewPriorityIndicator.setBackgroundColor(Color.parseColor("#FFC107"));
        } else {
            viewPriorityIndicator.setBackgroundColor(Color.parseColor("#4CAF50"));
        }

        textTaskTitle.setText(task.getTitle());
        if (task.getDescription() == null || task.getDescription().trim().isEmpty()) {
            textTaskDesc.setVisibility(View.GONE);
        } else {
            textTaskDesc.setVisibility(View.VISIBLE);
            textTaskDesc.setText(task.getDescription());
        }

        if (task.getDueDate() == null || task.getDueDate().trim().isEmpty()) {
            textTaskDueDate.setVisibility(View.GONE);
        } else {
            textTaskDueDate.setVisibility(View.VISIBLE);
            textTaskDueDate.setText("Due: " + task.getDueDate());
        }

        checkboxComplete.setOnCheckedChangeListener(null);
        checkboxComplete.setChecked(task.getCompleted() == 1);

        if (task.getCompleted() == 1) {
            textTaskTitle.setPaintFlags(textTaskTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            textTaskTitle.setTextColor(Color.parseColor("#888888"));
        } else {
            textTaskTitle.setPaintFlags(textTaskTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            textTaskTitle.setTextColor(Color.parseColor("#212121"));
        }

        checkboxComplete.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (listener != null) {
                    listener.onTaskStatusChanged(task, isChecked);
                } 
            }
        });

        btnDeleteTask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onTaskDeleted(task);
                }
            }
        });

        return convertView;
    }
}