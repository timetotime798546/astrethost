package com.studentplanner.app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.studentplanner.app.R;
import com.studentplanner.app.data.DatabaseHelper;
import com.studentplanner.app.models.Subject;

import java.util.ArrayList;

public class SubjectAdapter extends ArrayAdapter<Subject> {

    private final ArrayList<Subject> subjects;
    private final Context context;
    private final DatabaseHelper dbHelper;

    public SubjectAdapter(Context context, ArrayList<Subject> subjects, DatabaseHelper dbHelper) {
        super(context, 0, subjects);
        this.context = context;
        this.subjects = subjects;
        this.dbHelper = dbHelper;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_subject, parent, false);
        }

        Subject currentSubject = subjects.get(position);

        TextView subjectNameTextView = convertView.findViewById(R.id.subject_name_text_view);
        TextView subjectTaskCountTextView = convertView.findViewById(R.id.subject_task_count_text_view);

        subjectNameTextView.setText(currentSubject.getName());
        int taskCount = dbHelper.getTaskCountForSubject(currentSubject.getId());
        subjectTaskCountTextView.setText(taskCount + " tasks");

        return convertView;
    }
}