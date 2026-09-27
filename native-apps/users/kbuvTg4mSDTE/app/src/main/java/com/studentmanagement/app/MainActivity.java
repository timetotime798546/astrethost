package com.studentmanagement.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private ListView listViewStudents;
    private Button buttonAddStudent;
    private Button buttonLogout;
    private BackendApi backendApi;
    private SharedPreferencesManager sharedPreferencesManager;
    private StudentAdapter studentAdapter;
    private List<Student> studentList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listViewStudents = (ListView) findViewById(R.id.listViewStudents);
        buttonAddStudent = (Button) findViewById(R.id.buttonAddStudent);
        buttonLogout = (Button) findViewById(R.id.buttonLogout);

        backendApi = new BackendApi(this); // Pass context
        sharedPreferencesManager = new SharedPreferencesManager(this);

        // Check if user is logged in
        if (sharedPreferencesManager.getAuthToken() == null) {
            redirectToLogin();
            return;
        }

        studentList = new ArrayList<Student>();
        studentAdapter = new StudentAdapter(this, studentList);
        listViewStudents.setAdapter(studentAdapter);

        buttonAddStudent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, StudentDetailActivity.class);
                startActivity(intent);
            }
        });

        buttonLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sharedPreferencesManager.clearAuthToken();
                redirectToLogin();
            }
        });

        listViewStudents.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Student selectedStudent = studentAdapter.getItem(position);
                Intent intent = new Intent(MainActivity.this, StudentDetailActivity.class);
                intent.putExtra("student", selectedStudent); // Pass the entire student object
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Ensure backendApi is initialized with context before fetching
        if (backendApi == null) {
            backendApi = new BackendApi(this);
        }
        fetchStudents(); // Refresh student list when activity resumes
    }

    private void redirectToLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private void fetchStudents() {
        String authToken = sharedPreferencesManager.getAuthToken();
        if (authToken == null) {
            redirectToLogin();
            return;
        }

        backendApi.getStudents(authToken, new BackendApi.ApiCallback<List<Student>>() {
            @Override
            public void onSuccess(final List<Student> students) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        studentList.clear();
                        studentList.addAll(students);
                        studentAdapter.notifyDataSetChanged();
                        if (students.isEmpty()) {
                            Toast.makeText(MainActivity.this, "No students found. Add one!", Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }

            @Override
            public void onError(final String error) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "Failed to load students: " + error, Toast.LENGTH_LONG).show();
                        // If error is related to auth, redirect to login
                        if (error.contains("401") || error.contains("Unauthorized") || error.toLowerCase().contains("token")) {
                            sharedPreferencesManager.clearAuthToken();
                            redirectToLogin();
                        }
                    }
                });
            }
        });
    }

    private static class StudentAdapter extends ArrayAdapter<Student> {
        private LayoutInflater inflater;

        public StudentAdapter(Context context, List<Student> students) {
            super(context, 0, students);
            inflater = LayoutInflater.from(context);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.item_student, parent, false);
            }

            Student currentStudent = getItem(position);

            TextView textViewName = (TextView) convertView.findViewById(R.id.textViewStudentName);
            TextView textViewDetails = (TextView) convertView.findViewById(R.id.textViewStudentDetails);

            if (currentStudent != null) {
                textViewName.setText(currentStudent.getName());
                textViewDetails.setText("Class: " + currentStudent.getClassName() + " | Roll No: " + currentStudent.getRollNumber() + " | Phone: " + currentStudent.getPhone());
            }

            return convertView;
        }
    }
}