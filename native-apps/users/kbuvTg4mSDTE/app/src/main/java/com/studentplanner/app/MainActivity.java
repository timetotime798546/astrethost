package com.studentplanner.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.studentplanner.app.activities.AddSubjectActivity;
import com.studentplanner.app.activities.TaskListActivity;
import com.studentplanner.app.adapters.SubjectAdapter;
import com.studentplanner.app.data.DatabaseHelper;
import com.studentplanner.app.models.Subject;

import java.util.ArrayList;

public class MainActivity extends Activity {

    private ListView subjectListView;
    private Button addSubjectButton;
    private DatabaseHelper dbHelper;
    private SubjectAdapter subjectAdapter;
    private ArrayList<Subject> subjects;

    private static final int REQUEST_ADD_SUBJECT = 1;
    private static final int REQUEST_EDIT_SUBJECT = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);

        subjectListView = findViewById(R.id.subject_list_view);
        addSubjectButton = findViewById(R.id.add_subject_button);

        loadSubjects();

        addSubjectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, AddSubjectActivity.class);
                startActivityForResult(intent, REQUEST_ADD_SUBJECT);
            }
        });

        subjectListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Subject selectedSubject = (Subject) parent.getItemAtPosition(position);
                Intent intent = new Intent(MainActivity.this, TaskListActivity.class);
                intent.putExtra("subject_id", selectedSubject.getId());
                intent.putExtra("subject_name", selectedSubject.getName());
                startActivity(intent);
            }
        });

        subjectListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Subject selectedSubject = (Subject) parent.getItemAtPosition(position);
                Intent intent = new Intent(MainActivity.this, AddSubjectActivity.class);
                intent.putExtra("subject_id", selectedSubject.getId());
                intent.putExtra("subject_name", selectedSubject.getName());
                startActivityForResult(intent, REQUEST_EDIT_SUBJECT);
                return true;
            }
        });
    }

    private void loadSubjects() {
        subjects = dbHelper.getAllSubjects();
        subjectAdapter = new SubjectAdapter(this, subjects, dbHelper);
        subjectListView.setAdapter(subjectAdapter);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            loadSubjects(); // Refresh the list
            if (requestCode == REQUEST_ADD_SUBJECT) {
                Toast.makeText(this, "Subject added!", Toast.LENGTH_SHORT).show();
            } else if (requestCode == REQUEST_EDIT_SUBJECT) {
                Toast.makeText(this, "Subject updated!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSubjects(); // Ensure subjects are refreshed if tasks were added/removed
    }
}