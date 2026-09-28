package com.studentmanager.app;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

public class StudentDetailActivity extends Activity {

    private static final String TAG = "StudentDetailActivity";

    private EditText nameEditText;
    private EditText rollNumberEditText;
    private EditText gradeEditText;
    private EditText emailEditText;
    private Button saveButton;
    private Button deleteButton;

    private BackendApi backendApi;
    private Student currentStudent; // Null if adding new, populated if editing existing

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_detail);

        backendApi = new BackendApi(this);

        nameEditText = findViewById(R.id.nameEditText);
        rollNumberEditText = findViewById(R.id.rollNumberEditText);
        gradeEditText = findViewById(R.id.gradeEditText);
        emailEditText = findViewById(R.id.emailEditText);
        saveButton = findViewById(R.id.saveButton);
        deleteButton = findViewById(R.id.deleteButton);

        // Check if we are editing an existing student
        if (getIntent().hasExtra("student")) {
            currentStudent = (Student) getIntent().getSerializableExtra("student");
            if (currentStudent != null) {
                nameEditText.setText(currentStudent.getName());
                rollNumberEditText.setText(currentStudent.getRollNumber());
                gradeEditText.setText(currentStudent.getGrade());
                emailEditText.setText(currentStudent.getEmail());
                deleteButton.setVisibility(View.VISIBLE); // Show delete button for existing students
                setTitle("Edit Student");
            }
        } else {
            deleteButton.setVisibility(View.GONE); // Hide delete button for new students
            setTitle("Add New Student");
        }

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveStudent();
            }
        });

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteStudent();
            }
        });
    }

    private void saveStudent() {
        String name = nameEditText.getText().toString().trim();
        String rollNumber = rollNumberEditText.getText().toString().trim();
        String grade = gradeEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();

        if (name.isEmpty() || rollNumber.isEmpty() || grade.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "All fields are required.", Toast.LENGTH_SHORT).show();
            return;
        }

        JSONObject studentData = new JSONObject();
        try {
            studentData.put("name", name);
            studentData.put("rollNumber", rollNumber);
            studentData.put("grade", grade);
            studentData.put("email", email);
        } catch (JSONException e) {
            Log.e(TAG, "Error creating student JSON: " + e.getMessage());
            Toast.makeText(this, "Error preparing data. Please try again.", Toast.LENGTH_LONG).show();
            return;
        }

        saveButton.setEnabled(false); // Disable button during API call

        if (currentStudent == null) {
            // Create new student
            backendApi.createRecord("students", studentData, new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(StudentDetailActivity.this, "Student added successfully!", Toast.LENGTH_SHORT).show();
                            finish(); // Go back to MainActivity
                        }
                    });
                }

                @Override
                public void onError(final String error) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.e(TAG, "Failed to add student: " + error);
                            Toast.makeText(StudentDetailActivity.this, "Failed to add student: " + error, Toast.LENGTH_LONG).show();
                            saveButton.setEnabled(true);
                        }
                    });
                }
            });
        } else {
            // Update existing student
            backendApi.updateRecord(currentStudent.getId(), studentData, new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(StudentDetailActivity.this, "Student updated successfully!", Toast.LENGTH_SHORT).show();
                            finish(); // Go back to MainActivity
                        }
                    });
                }

                @Override
                public void onError(final String error) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.e(TAG, "Failed to update student: " + error);
                            Toast.makeText(StudentDetailActivity.this, "Failed to update student: " + error, Toast.LENGTH_LONG).show();
                            saveButton.setEnabled(true);
                        }
                    });
                }
            });
        }
    }

    private void deleteStudent() {
        if (currentStudent == null || currentStudent.getId() == null) {
            Toast.makeText(this, "Cannot delete, no student selected.", Toast.LENGTH_SHORT).show();
            return;
        }

        deleteButton.setEnabled(false); // Disable button during API call

        backendApi.deleteRecord(currentStudent.getId(), new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(StudentDetailActivity.this, "Student deleted successfully!", Toast.LENGTH_SHORT).show();
                        finish(); // Go back to MainActivity
                    }
                });
            }

            @Override
            public void onError(final String error) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.e(TAG, "Failed to delete student: " + error);
                        Toast.makeText(StudentDetailActivity.this, "Failed to delete student: " + error, Toast.LENGTH_LONG).show();
                        deleteButton.setEnabled(true);
                    }
                });
            }
        });
    }
}