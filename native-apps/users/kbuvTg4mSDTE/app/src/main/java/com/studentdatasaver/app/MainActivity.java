package com.studentdatasaver.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";
    private ListView studentListView;
    private StudentAdapter studentAdapter;
    private List<Student> studentList;
    private BackendApi backendApi;
    private SharedPrefsManager prefsManager;
    private String appId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefsManager = new SharedPrefsManager(this);
        appId = prefsManager.getAppId();
        backendApi = new BackendApi(appId, prefsManager.getAuthToken());

        if (!prefsManager.isLoggedIn()) {
            // User is not logged in, redirect to LoginActivity
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        studentListView = (ListView) findViewById(R.id.studentListView);
        studentList = new ArrayList<Student>();
        studentAdapter = new StudentAdapter(this, studentList);
        studentListView.setAdapter(studentAdapter);

        findViewById(R.id.fab_add_student).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, AddEditStudentActivity.class);
                startActivityForResult(intent, 1);
            }
        });

        studentListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Student selectedStudent = studentList.get(position);
                Intent intent = new Intent(MainActivity.this, AddEditStudentActivity.class);
                intent.putExtra("student_id", selectedStudent.getId());
                intent.putExtra("student_name", selectedStudent.getName());
                intent.putExtra("student_backend_id", selectedStudent.getBackendId());
                startActivityForResult(intent, 2);
            }
        });

        studentListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                final Student selectedStudent = studentList.get(position);
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Delete Student")
                        .setMessage("Are you sure you want to delete " + selectedStudent.getName() + "?")
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                deleteStudent(selectedStudent.getBackendId());
                            }
                        })
                        .setNegativeButton(android.R.string.no, null)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
                return true;
            }
        });

        loadStudents();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (prefsManager.isLoggedIn()) {
            loadStudents();
        } else {
            // If somehow logged out while in other activity, redirect to login
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
    }

    private void loadStudents() {
        backendApi.setAuthToken(prefsManager.getAuthToken()); // Ensure token is current
        backendApi.readData("students", new BackendApi.BackendApiCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    Log.d(TAG, "Read data success: " + response);
                    Gson gson = new Gson();
                    Type responseType = new TypeToken<ApiResponse<List<Record>>>(){}.getType();
                    ApiResponse<List<Record>> apiResponse = gson.fromJson(response, responseType);

                    if (apiResponse != null && apiResponse.success) {
                        studentList.clear();
                        for (Record record : apiResponse.records) {
                            Student student = new Student(
                                    record.id,
                                    (String) record.data.get("name"),
                                    (String) record.data.get("student_id")
                            );
                            studentList.add(student);
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                studentAdapter.notifyDataSetChanged();
                            }
                        });
                    } else {
                        Log.e(TAG, "API Response indicates failure: " + response);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "Failed to load students: " + (apiResponse != null ? apiResponse.message : "Unknown error"), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing student list: " + e.getMessage(), e);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "Error parsing student data.", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onError(final String error) {
                Log.e(TAG, "Failed to load students: " + error);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "Error loading students: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void deleteStudent(String backendId) {
        backendApi.setAuthToken(prefsManager.getAuthToken()); // Ensure token is current
        backendApi.deleteData(backendId, new BackendApi.BackendApiCallback() {
            @Override
            public void onSuccess(String response) {
                Log.d(TAG, "Delete student success: " + response);
                try {
                    Gson gson = new Gson();
                    Type responseType = new TypeToken<ApiResponse<Object>>(){}.getType(); // Use Object for generic success response
                    ApiResponse<Object> apiResponse = gson.fromJson(response, responseType);

                    if (apiResponse != null && apiResponse.success) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "Student deleted successfully.", Toast.LENGTH_SHORT).show();
                                loadStudents(); // Reload list after deletion
                            }
                        });
                    } else {
                        Log.e(TAG, "API Response indicates failure during delete: " + response);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "Failed to delete student: " + (apiResponse != null ? apiResponse.message : "Unknown error"), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing delete student response: " + e.getMessage(), e);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "Error parsing delete response.", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onError(final String error) {
                Log.e(TAG, "Failed to delete student: " + error);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "Error deleting student: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            loadStudents(); // Refresh student list after add/edit
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            logoutUser();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void logoutUser() {
        backendApi.setAuthToken(prefsManager.getAuthToken()); // Ensure token is current
        backendApi.logout(new BackendApi.BackendApiCallback() {
            @Override
            public void onSuccess(String response) {
                Log.d(TAG, "Logout success: " + response);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        prefsManager.clearAuthToken();
                        Toast.makeText(MainActivity.this, "Logged out successfully.", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                        startActivity(intent);
                        finish();
                    }
                });
            }

            @Override
            public void onError(final String error) {
                Log.e(TAG, "Logout error: " + error);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // Even if logout API fails, clear token locally and redirect
                        prefsManager.clearAuthToken();
                        Toast.makeText(MainActivity.this, "Logout failed but cleared session. " + error, Toast.LENGTH_LONG).show();
                        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                        startActivity(intent);
                        finish();
                    }
                });
            }
        });
    }

    // Helper classes for GSON parsing
    private static class ApiResponse<T> {
        boolean success;
        String message;
        T records; // For GET /data
        Record record; // For POST /data
        String id; // For PUT/DELETE /data
    }

    private static class Record {
        String id; // backendId
        String app_id;
        String user_id;
        String collection;
        HashMap<String, Object> data;
        String created_at;
        String updated_at;
    }
}