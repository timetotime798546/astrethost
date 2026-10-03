package com.taskmanager.app;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import java.util.List;

public class TaskAdapter extends ArrayAdapter<Task> {

    public interface TaskActionListener {
        void onToggleStatus(Task task, boolean isChecked);
        void onDelete(Task task);
    }

    private final LayoutInflater inflater;
    private final TaskActionListener actionListener;

    public TaskAdapter(Context context, List<Task> tasks, TaskActionListener actionListener) {
        super(context, 0, tasks);
        this.inflater = LayoutInflater.from(context);
        this.actionListener = actionListener;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.task_item, parent, false);
            holder = new ViewHolder();
            holder.cbStatus = (CheckBox) convertView.findViewById(R.id.cbTaskStatus);
            holder.tvTitle = (TextView) convertView.findViewById(R.id.tvTaskTitle);
            holder.btnDelete = (Button) convertView.findViewById(R.id.btnDeleteTask);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        final Task task = getItem(position);
        if (task != null) {
            holder.tvTitle.setText(task.getTitle());
            
            // Remove listener before setting progress state to avoid firing recursive callbacks
            holder.cbStatus.setOnCheckedChangeListener(null);
            holder.cbStatus.setChecked(task.isCompleted());

            if (task.isCompleted()) {
                holder.tvTitle.setPaintFlags(holder.tvTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                holder.tvTitle.setTextColor(0xFF9CA3AF);
            } else {
                holder.tvTitle.setPaintFlags(holder.tvTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                holder.tvTitle.setTextColor(0xFF111827);
            }

            holder.cbStatus.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean checked = ((CheckBox) v).isChecked();
                    actionListener.onToggleStatus(task, checked);
                }
            });

            holder.btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    actionListener.onDelete(task);
                }
            });
        }

        return convertView;
    }

    private static class ViewHolder {
        CheckBox cbStatus;
        TextView tvTitle;
        Button btnDelete;
    }
}