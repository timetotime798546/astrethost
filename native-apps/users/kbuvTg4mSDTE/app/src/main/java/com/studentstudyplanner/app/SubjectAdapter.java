package com.studentstudyplanner.app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class SubjectAdapter extends ArrayAdapter<Subject> {

    private LayoutInflater inflater;
    private StudyPlannerDatabaseHelper db;

    public SubjectAdapter(Context context, List<Subject> subjects) {
        super(context, 0, subjects);
        inflater = LayoutInflater.from(context);
        db = new StudyPlannerDatabaseHelper(context);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_subject, parent, false);
            holder = new ViewHolder();
            holder.subjectNameTextView = (TextView) convertView.findViewById(R.id.subject_name);
            holder.subjectProgressTextView = (TextView) convertView.findViewById(R.id.subject_progress_text);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Subject currentSubject = getItem(position);
        if (currentSubject != null) {
            holder.subjectNameTextView.setText(currentSubject.getName());

            int totalTasks = db.getTotalTasksCountForSubject(currentSubject.getId());
            int completedTasks = db.getCompletedTasksCountForSubject(currentSubject.getId());
            holder.subjectProgressTextView.setText(completedTasks + "/" + totalTasks + " tasks completed");
        }

        return convertView;
    }

    private static class ViewHolder {
        TextView subjectNameTextView;
        TextView subjectProgressTextView;
    }
}