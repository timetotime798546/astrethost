package com.cloudstudentmanager.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONObject;

public class AddEditActivity extends Activity {
    private EditText etName, etRoll, etCourse, etPhone, etEmail;
    private Button btnSave, btnDelete;
    private BackendApi api;
    private String studentId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit);

        etName = findViewById(R.id.etName);
        etRoll = findViewById(R.id.etRoll);
        etCourse = findViewById(R.id.etCourse);
        etPhone = findViewById(R.id.etPhone);
        etEmail = findViewById(R.id.etEmail);
        btnSave = findViewById(R.id.btnSave);
        btnDelete = findViewById(R.id.btnDelete);
        TextView tvTitle = findViewById(R.id.tvTitle);

        api = new BackendApi(this);
        api.setToken(new SessionManager(this).getToken());

        String jsonStr = getIntent().getStringExtra("student_json");
        if (jsonStr != null) {
            try {
                JSONObject obj = new JSONObject(jsonStr);
                studentId = obj.getString("id");
                JSONObject data = obj.getJSONObject("data");
                etName.setText(data.getString("name"));
                etRoll.setText(data.getString("roll_no"));
                etCourse.setText(data.getString("course"));
                etPhone.setText(data.getString("phone"));
                etEmail.setText(data.getString("email"));
                
                tvTitle.setText("Edit Student");
                btnDelete.setVisibility(View.VISIBLE);
            } catch (Exception e) {}
        }

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveStudent();
            }
        });

        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteStudent();
            }
        });
    }

    private void saveStudent() {
        String name = etName.getText().toString().trim();
        String roll = etRoll.getText().toString().trim();
        String course = etCourse.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String email = etEmail.getText().toString().trim();

        if (name.isEmpty() || roll.isEmpty()) {
            Toast.makeText(this, "Name and Roll No required", Toast.LENGTH_SHORT).show();
            return;
        }

        BackendApi.ApiCallback callback = new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(AddEditActivity.this, "Saved Successfully", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
            }

            @Override
            public void onError(final String message) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(AddEditActivity.this, "Error: " + message, Toast.LENGTH_LONG).show();
                    }
                });
            }
        };

        if (studentId == null) {
            api.createStudent(name, roll, course, phone, email, callback);
        } else {
            api.updateStudent(studentId, name, roll, course, phone, email, callback);
        }
    }

    private void deleteStudent() {
        api.deleteStudent(studentId, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(AddEditActivity.this, "Deleted", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
            }

            @Override
            public void onError(final String message) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(AddEditActivity.this, "Error: " + message, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }
}