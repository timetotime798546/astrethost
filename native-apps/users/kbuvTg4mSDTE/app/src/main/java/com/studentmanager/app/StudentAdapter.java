package com.studentmanager.app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class StudentAdapter extends ArrayAdapter<Student> {

    private Context context;
    private List<Student> studentList;

    public StudentAdapter(Context context, List<Student> studentList) {
        super(context, 0, studentList);
        this.context = context;
        this.studentList = studentList;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.list_item_student, parent, false);
        }

        TextView studentNameTextView = convertView.findViewById(R.id.studentNameTextView);
        TextView studentRollNumberTextView = convertView.findViewById(R.id.studentRollNumberTextView);
        TextView studentGradeTextView = convertView.findViewById(R.id.studentGradeTextView);
        TextView studentEmailTextView = convertView.findViewById(R.id.studentEmailTextView);

        Student currentStudent = studentList.get(position);

        if (currentStudent != null) {
            studentNameTextView.setText(currentStudent.getName());
            studentRollNumberTextView.setText("Roll Number: " + currentStudent.getRollNumber());
            studentGradeTextView.setText("Grade: " + currentStudent.getGrade());
            studentEmailTextView.setText("Email: " + currentStudent.getEmail());
        }

        return convertView;
    }
}