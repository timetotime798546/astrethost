package com.studentstudyplanner.app;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private ListView subjectListView;
    private StudyPlannerDatabaseHelper db;
    private SubjectAdapter subjectAdapter;
    private List<Subject> currentSubjects;

    private static final int REQUEST_CODE_ADD_SUBJECT = 1;
    private static final int REQUEST_CODE_EDIT_SUBJECT = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        subjectListView = (ListView) findViewById(R.id.subjectListView);
        db = new StudyPlannerDatabaseHelper(this);

        currentSubjects = new ArrayList<Subject>();
        subjectAdapter = new SubjectAdapter(this, currentSubjects);
        subjectListView.setAdapter(subjectAdapter);

        subjectListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Subject selectedSubject = (Subject) parent.getItemAtPosition(position);
                Intent intent = new Intent(MainActivity.this, SubjectDetailActivity.class);
                intent.putExtra("subject_id", selectedSubject.getId());
                startActivityForResult(intent, REQUEST_CODE_EDIT_SUBJECT);
            }
        });

        loadSubjects();
        createNotificationChannel();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSubjects(); // Refresh the list when returning to MainActivity
    }

    private void loadSubjects() {
        currentSubjects.clear();
        currentSubjects.addAll(db.getAllSubjects());
        subjectAdapter.notifyDataSetChanged();
        if (currentSubjects.isEmpty()) {
            Toast.makeText(this, "No subjects found. Add a new subject!", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_add_subject) {
            showAddSubjectDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showAddSubjectDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add New Subject");

        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_subject, null);
        final EditText subjectNameEditText = (EditText) dialogView.findViewById(R.id.edit_text_subject_name);
        builder.setView(dialogView);

        builder.setPositiveButton("Add", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String subjectName = subjectNameEditText.getText().toString().trim();
                if (subjectName.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Subject name cannot be empty", Toast.LENGTH_SHORT).show();
                    return;
                }
                Subject newSubject = new Subject();
                newSubject.setName(subjectName);
                long id = db.addSubject(newSubject);
                if (id != -1) {
                    Toast.makeText(MainActivity.this, "Subject added!", Toast.LENGTH_SHORT).show();
                    loadSubjects();
                } else {
                    Toast.makeText(MainActivity.this, "Failed to add subject", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            loadSubjects(); // Reload subjects if an update or addition occurred in SubjectDetailActivity
            Toast.makeText(this, "Subject or Task updated!", Toast.LENGTH_SHORT).show();
        } else if (resultCode == SubjectDetailActivity.RESULT_SUBJECT_DELETED) {
            loadSubjects();
            Toast.makeText(this, "Subject deleted!", Toast.LENGTH_SHORT).show();
        }
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Study Planner Reminders";
            String description = "Channel for study planner task reminders";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel("student_study_planner_channel", name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}