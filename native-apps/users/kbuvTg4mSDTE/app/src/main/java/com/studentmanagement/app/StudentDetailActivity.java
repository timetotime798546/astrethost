package com.studentmanagement.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class StudentDetailActivity extends Activity {

    private EditText editTextName;
    private EditText editTextPhone;
    private EditText editTextClass;
    private EditText editTextRollNumber;
    private Button buttonSave;
    private Button buttonDelete;
    private TextView textViewTitle;

    private Student currentStudent; // Null for new student, populated for existing
    private BackendApi backendApi;
    private SharedPreferencesManager sharedPreferencesManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_detail);

        editTextName = (EditText) findViewById(R.id.editTextName);
        editTextPhone = (EditText) findViewById(R.id.editTextPhone);
        editTextClass = (EditText) findViewById(R.id.editTextClass);
        editTextRollNumber = (EditText) findViewById(R.id.editTextRollNumber);
        buttonSave = (Button) findViewById(R.id.buttonSave);
        buttonDelete = (Button) findViewById(R.id.buttonDelete);
        textViewTitle = (TextView) findViewById(R.id.textViewTitle);

        backendApi = new BackendApi(this); // Pass context
        sharedPreferencesManager = new SharedPreferencesManager(this);

        // Check if editing an existing student or creating a new one
        if (getIntent().hasExtra("student")) {
            currentStudent = (Student) getIntent().getSerializableExtra("student");
            if (currentStudent != null) {
                textViewTitle.setText("Edit Student");
                populateFields(currentStudent);
                buttonDelete.setVisibility(View.VISIBLE); // Show delete button for existing students
            }
        } else {
            textViewTitle.setText("Add New Student");
            buttonDelete.setVisibility(View.GONE); // Hide delete button for new students
        }

        buttonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveStudent();
            }
        });

        buttonDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteStudent();
            }
        });
    }

    private void populateFields(Student student) {
        editTextName.setText(student.getName());
        editTextPhone.setText(student.getPhone());
        editTextClass.setText(student.getClassName());
        editTextRollNumber.setText(String.valueOf(student.getRollNumber()));
    }

    private void saveStudent() {
        String name = editTextName.getText().toString().trim();
        String phone = editTextPhone.getText().toString().trim();
        String className = editTextClass.getText().toString().trim();
        String rollNumberStr = editTextRollNumber.getText().toString().trim();

        if (name.isEmpty() || phone.isEmpty() || className.isEmpty() || rollNumberStr.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        int rollNumber;
        try {
            rollNumber = Integer.parseInt(rollNumberStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Roll Number must be a valid number", Toast.LENGTH_SHORT).show();
            return;
        }

        String authToken = sharedPreferencesManager.getAuthToken();
        if (authToken == null) {
            Toast.makeText(this, "Authentication required. Please log in again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        buttonSave.setEnabled(false); // Disable button during API call

        if (currentStudent == null) {
            // Create new student
            Student newStudent = new Student(name, phone, className, rollNumber);
            backendApi.createStudent(authToken, newStudent, new BackendApi.ApiCallback<Student>() {
                @Override
                public void onSuccess(final Student result) {
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
                            Toast.makeText(StudentDetailActivity.this, "Failed to add student: " + error, Toast.LENGTH_LONG).show();
                            buttonSave.setEnabled(true);
                        }
                    });
                }
            });
        } else {
            // Update existing student
            currentStudent.setName(name);
            currentStudent.setPhone(phone);
            currentStudent.setClassName(className);
            currentStudent.setRollNumber(rollNumber);

            backendApi.updateStudent(authToken, currentStudent, new BackendApi.ApiCallback<Student>() {
                @Override
                public void onSuccess(final Student result) {
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
                            Toast.makeText(StudentDetailActivity.this, "Failed to update student: " + error, Toast.LENGTH_LONG).show();
                            buttonSave.setEnabled(true);
                        }
                    });
                }
            });
        }
    }

    private void deleteStudent() {
        if (currentStudent == null || currentStudent.getId() == null) {
            Toast.makeText(this, "Cannot delete a non-existent student.", Toast.LENGTH_SHORT).show();
            return;
        }

        String authToken = sharedPreferencesManager.getAuthToken();
        if (authToken == null) {
            Toast.makeText(this, "Authentication required. Please log in again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        buttonDelete.setEnabled(false); // Disable button during API call

        backendApi.deleteStudent(authToken, currentStudent.getId(), new BackendApi.ApiCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
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
                        Toast.makeText(StudentDetailActivity.this, "Failed to delete student: " + error, Toast.LENGTH_LONG).show();
                        buttonDelete.setEnabled(true);
                    }
                });
            }
        });
    }
}