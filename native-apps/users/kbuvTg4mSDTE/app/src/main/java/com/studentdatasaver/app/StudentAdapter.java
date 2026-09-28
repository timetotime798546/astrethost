package com.studentdatasaver.app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class StudentAdapter extends ArrayAdapter<Student> {

    public StudentAdapter(Context context, List<Student> students) {
        super(context, 0, students);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        Student student = getItem(position);

        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_student, parent, false);
        }

        // Lookup view for data population
        TextView tvName = (TextView) convertView.findViewById(R.id.textViewStudentName);
        TextView tvStudentId = (TextView) convertView.findViewById(R.id.textViewStudentId);

        // Populate the data into the template view using the data object
        if (student != null) {
            tvName.setText(student.getName());
            tvStudentId.setText("ID: " + student.getId());
        }

        // Return the completed view to render on screen
        return convertView;
    }
}