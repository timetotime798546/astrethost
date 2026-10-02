package com.studentmanager.app;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class AddEditStudentActivity extends Activity {

    private TextView tvFormTitle;
    private EditText etName;
    private EditText etRollNo;
    private EditText etGrade;
    private EditText etAttendancePresent;
    private EditText etAttendanceTotal;
    private EditText etMarksObtained;
    private EditText etMarksTotal;

    private TextView tvLiveAttendancePct;
    private TextView tvLiveMarksPct;
    private TextView tvLiveGrade;

    private Button btnSave;
    private Button btnCancel;

    private String studentId = null;
    private BackendApi backendApi;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit);

        backendApi = new BackendApi(this);

        tvFormTitle = (TextView) findViewById(R.id.tv_form_title);
        etName = (EditText) findViewById(R.id.et_name);
        etRollNo = (EditText) findViewById(R.id.et_roll_no);
        etGrade = (EditText) findViewById(R.id.et_grade);
        etAttendancePresent = (EditText) findViewById(R.id.et_attendance_present);
        etAttendanceTotal = (EditText) findViewById(R.id.et_attendance_total);
        etMarksObtained = (EditText) findViewById(R.id.et_marks_obtained);
        etMarksTotal = (EditText) findViewById(R.id.et_marks_total);

        tvLiveAttendancePct = (TextView) findViewById(R.id.tv_live_attendance_pct);
        tvLiveMarksPct = (TextView) findViewById(R.id.tv_live_marks_pct);
        tvLiveGrade = (TextView) findViewById(R.id.tv_live_grade);

        btnSave = (Button) findViewById(R.id.btn_save);
        btnCancel = (Button) findViewById(R.id.btn_cancel);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Saving student record...");
        progressDialog.setCancelable(false);

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("student_id")) {
            studentId = intent.getStringExtra("student_id");
            tvFormTitle.setText("Edit Student Record");
            etName.setText(intent.getStringExtra("name"));
            etRollNo.setText(intent.getStringExtra("roll_no"));
            etGrade.setText(intent.getStringExtra("grade"));
            etAttendancePresent.setText(String.valueOf(intent.getIntExtra("attendance_present", 0)));
            etAttendanceTotal.setText(String.valueOf(intent.getIntExtra("attendance_total", 0)));
            etMarksObtained.setText(String.valueOf(intent.getIntExtra("marks_obtained", 0)));
            etMarksTotal.setText(String.valueOf(intent.getIntExtra("marks_total", 0)));
            updateLiveCalculations();
        } else {
            tvFormTitle.setText("Add New Student");
        }

        // Live calculation logic tracking user updates immediately (MANDATORY LOCAL CALCULATION)
        TextWatcher liveCalcWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateLiveCalculations();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        etAttendancePresent.addTextChangedListener(liveCalcWatcher);
        etAttendanceTotal.addTextChangedListener(liveCalcWatcher);
        etMarksObtained.addTextChangedListener(liveCalcWatcher);
        etMarksTotal.addTextChangedListener(liveCalcWatcher);

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveStudent();
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void updateLiveCalculations() {
        int present = getIntFromEditText(etAttendancePresent);
        int totalAtt = getIntFromEditText(etAttendanceTotal);
        int obtained = getIntFromEditText(etMarksObtained);
        int totalMarks = getIntFromEditText(etMarksTotal);

        double attendancePct = 0;
        if (totalAtt > 0) {
            attendancePct = ((double) present / totalAtt) * 100.0;
        }
        tvLiveAttendancePct.setText(String.format("Attendance: %.1f%%", attendancePct));

        double marksPct = 0;
        if (totalMarks > 0) {
            marksPct = ((double) obtained / totalMarks) * 100.0;
        }
        tvLiveMarksPct.setText(String.format("Academic Score: %.1f%%", marksPct));

        String grade = "F";
        if (totalMarks > 0) {
            if (marksPct >= 90) grade = "A+";
            else if (marksPct >= 80) grade = "A";
            else if (marksPct >= 70) grade = "B";
            else if (marksPct >= 60) grade = "C";
            else if (marksPct >= 50) grade = "D";
        } else {
            grade = "N/A";
        }
        tvLiveGrade.setText("Projected Grade: " + grade);
    }

    private int getIntFromEditText(EditText editText) {
        String val = editText.getText().toString().trim();
        if (val.isEmpty()) return 0;
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void saveStudent() {
        String name = etName.getText().toString().trim();
        String rollNo = etRollNo.getText().toString().trim();
        String grade = etGrade.getText().toString().trim();
        String presentStr = etAttendancePresent.getText().toString().trim();
        String totalAttStr = etAttendanceTotal.getText().toString().trim();
        String obtainedStr = etMarksObtained.getText().toString().trim();
        String totalMarksStr = etMarksTotal.getText().toString().trim();

        if (name.isEmpty() || rollNo.isEmpty() || grade.isEmpty() || 
            presentStr.isEmpty() || totalAttStr.isEmpty() || obtainedStr.isEmpty() || totalMarksStr.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        int present = Integer.parseInt(presentStr);
        int totalAtt = Integer.parseInt(totalAttStr);
        int obtained = Integer.parseInt(obtainedStr);
        int totalMarks = Integer.parseInt(totalMarksStr);

        if (present > totalAtt) {
            Toast.makeText(this, "Present days cannot exceed total days", Toast.LENGTH_SHORT).show();
            return;
        }

        if (obtained > totalMarks) {
            Toast.makeText(this, "Obtained marks cannot exceed total marks", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.show();

        Student student = new Student(studentId, name, rollNo, grade, present, totalAtt, obtained, totalMarks);

        if (studentId == null) {
            backendApi.createStudent(student, new ApiCallback<Student>() {
                @Override
                public void onSuccess(Student result) {
                    progressDialog.dismiss();
                    Toast.makeText(AddEditStudentActivity.this, "Student created successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onError(String errorMsg) {
                    progressDialog.dismiss();
                    Toast.makeText(AddEditStudentActivity.this, "Save failed: " + errorMsg, Toast.LENGTH_LONG).show();
                }
            });
        } else {
            backendApi.updateStudent(student, new ApiCallback<String>() {
                @Override
                public void onSuccess(String result) {
                    progressDialog.dismiss();
                    Toast.makeText(AddEditStudentActivity.this, "Student updated successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onError(String errorMsg) {
                    progressDialog.dismiss();
                    Toast.makeText(AddEditStudentActivity.this, "Update failed: " + errorMsg, Toast.LENGTH_LONG).show();
                }
            });
        }
    }
}