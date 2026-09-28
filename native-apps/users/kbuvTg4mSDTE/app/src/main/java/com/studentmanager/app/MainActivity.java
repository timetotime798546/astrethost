package com.studentmanager.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";
    private ListView studentListView;
    private Button addStudentButton;
    private BackendApi backendApi;
    private List<Student> studentList;
    private StudentAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        backendApi = new BackendApi(this);

        studentListView = findViewById(R.id.studentListView);
        addStudentButton = findViewById(R.id.addStudentButton);

        studentList = new ArrayList<>();
        adapter = new StudentAdapter(this, studentList);
        studentListView.setAdapter(adapter);

        addStudentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, StudentDetailActivity.class);
                startActivity(intent);
            }
        });

        studentListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Student selectedStudent = studentList.get(position);
                Intent intent = new Intent(MainActivity.this, StudentDetailActivity.class);
                intent.putExtra("student", selectedStudent); // Pass the entire student object
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchStudents(); // Refresh student list whenever activity resumes
    }

    private void fetchStudents() {
        backendApi.readRecords("students", new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            JSONArray recordsArray = response.getJSONArray("records");
                            studentList.clear();
                            for (int i = 0; i < recordsArray.length(); i++) {
                                JSONObject record = recordsArray.getJSONObject(i);
                                String id = record.getString("id");
                                JSONObject data = record.getJSONObject("data");
                                String name = data.optString("name", "");
                                String rollNumber = data.optString("rollNumber", "");
                                String grade = data.optString("grade", "");
                                String email = data.optString("email", "");
                                studentList.add(new Student(id, name, rollNumber, grade, email));
                            }
                            adapter.notifyDataSetChanged();
                            Toast.makeText(MainActivity.this, "Students loaded successfully.", Toast.LENGTH_SHORT).show();
                        } catch (JSONException e) {
                            Log.e(TAG, "Error parsing student records: " + e.getMessage());
                            Toast.makeText(MainActivity.this, "Error parsing student data.", Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }

            @Override
            public void onError(final String error) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.e(TAG, "Failed to fetch students: " + error);
                        Toast.makeText(MainActivity.this, "Failed to load students: " + error, Toast.LENGTH_LONG).show();
                        // If authentication error, redirect to login
                        if (error.toLowerCase().contains("authentication required") || error.toLowerCase().contains("invalid token")) {
                            logoutUser();
                        }
                    }
                });
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_logout) {
            logoutUser();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void logoutUser() {
        backendApi.logout(new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "Logged out successfully.", Toast.LENGTH_SHORT).show();
                        redirectToLogin();
                    }
                });
            }

            @Override
            public void onError(final String error) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.e(TAG, "Logout failed: " + error);
                        Toast.makeText(MainActivity.this, "Logout failed, but clearing session: " + error, Toast.LENGTH_LONG).show();
                        backendApi.clearAuthToken(); // Force clear token even if logout API fails
                        redirectToLogin();
                    }
                });
            }
        });
    }

    private void redirectToLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clear back stack
        startActivity(intent);
        finish();
    }
}