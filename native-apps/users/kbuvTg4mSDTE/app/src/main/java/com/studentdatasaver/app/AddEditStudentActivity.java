package com.studentdatasaver.app;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class AddEditStudentActivity extends Activity {

    private static final String TAG = "AddEditStudentActivity";
    private EditText editTextName, editTextStudentId;
    private Button buttonSave;
    private boolean isEditing = false;
    private String studentBackendId; // The ID from the backend
    private String studentId; // The user-defined student_id
    private BackendApi backendApi;
    private SharedPrefsManager prefsManager;
    private String appId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_student);

        prefsManager = new SharedPrefsManager(this);
        appId = prefsManager.getAppId();
        backendApi = new BackendApi(appId, prefsManager.getAuthToken());

        editTextName = (EditText) findViewById(R.id.editTextStudentName);
        editTextStudentId = (EditText) findViewById(R.id.editTextStudentIdentifier);
        buttonSave = (Button) findViewById(R.id.buttonSaveStudent);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            isEditing = true;
            studentBackendId = extras.getString("student_backend_id");
            String name = extras.getString("student_name");
            studentId = extras.getString("student_id");

            editTextName.setText(name);
            editTextStudentId.setText(studentId);
            buttonSave.setText("Update Student");
            setTitle("Edit Student");
        } else {
            buttonSave.setText("Add Student");
            setTitle("Add New Student");
        }

        buttonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveStudent();
            }
        });
    }

    private void saveStudent() {
        String name = editTextName.getText().toString().trim();
        String studentIdentifier = editTextStudentId.getText().toString().trim();

        if (name.isEmpty() || studentIdentifier.isEmpty()) {
            Toast.makeText(this, "Please enter student name and ID.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> studentData = new HashMap<String, Object>();
        studentData.put("name", name);
        studentData.put("student_id", studentIdentifier);

        backendApi.setAuthToken(prefsManager.getAuthToken()); // Ensure token is current

        if (isEditing) {
            backendApi.updateData(studentBackendId, studentData, "students", new BackendApi.BackendApiCallback() {
                @Override
                public void onSuccess(String response) {
                    try {
                        Log.d(TAG, "Update student success: " + response);
                        Gson gson = new Gson();
                        Type responseType = new TypeToken<ApiResponse<Object>>(){}.getType(); // Use Object for generic success response
                        ApiResponse<Object> apiResponse = gson.fromJson(response, responseType);

                        if (apiResponse != null && apiResponse.success) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(AddEditStudentActivity.this, "Student updated successfully!", Toast.LENGTH_SHORT).show();
                                    setResult(RESULT_OK);
                                    finish();
                                }
                            });
                        } else {
                            Log.e(TAG, "API Response indicates failure during update: " + response);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(AddEditStudentActivity.this, "Failed to update student: " + (apiResponse != null ? apiResponse.message : "Unknown error"), Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing update student response: " + e.getMessage(), e);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(AddEditStudentActivity.this, "Error parsing update response.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }

                @Override
                public void onError(final String error) {
                    Log.e(TAG, "Update student error: " + error);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(AddEditStudentActivity.this, "Error updating student: " + error, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
        } else {
            backendApi.createData("students", studentData, new BackendApi.BackendApiCallback() {
                @Override
                public void onSuccess(String response) {
                    try {
                        Log.d(TAG, "Create student success: " + response);
                        Gson gson = new Gson();
                        Type responseType = new TypeToken<ApiResponse<Record>>(){}.getType();
                        ApiResponse<Record> apiResponse = gson.fromJson(response, responseType);

                        if (apiResponse != null && apiResponse.success && apiResponse.record != null) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(AddEditStudentActivity.this, "Student added successfully!", Toast.LENGTH_SHORT).show();
                                    setResult(RESULT_OK);
                                    finish();
                                }
                            });
                        } else {
                            Log.e(TAG, "API Response indicates failure during create: " + response);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(AddEditStudentActivity.this, "Failed to add student: " + (apiResponse != null ? apiResponse.message : "Unknown error"), Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing create student response: " + e.getMessage(), e);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(AddEditStudentActivity.this, "Error parsing create response.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }

                @Override
                public void onError(final String error) {
                    Log.e(TAG, "Create student error: " + error);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(AddEditStudentActivity.this, "Error adding student: " + error, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
        }
    }

    // Helper classes for GSON parsing
    private static class ApiResponse<T> {
        boolean success;
        String message;
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