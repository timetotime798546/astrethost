package com.studentmanager.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private TextView tvTotalStudents;
    private TextView tvAvgAttendance;
    private TextView tvAvgMarks;
    private ListView lvStudents;
    private Button btnAddStudent;
    private Button btnRefresh;
    private Button btnLogout;
    private TextView tvNoData;

    private BackendApi backendApi;
    private List<Student> studentsList = new ArrayList<>();
    private StudentAdapter adapter;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        backendApi = new BackendApi(this);

        tvTotalStudents = (TextView) findViewById(R.id.tv_total_students);
        tvAvgAttendance = (TextView) findViewById(R.id.tv_avg_attendance);
        tvAvgMarks = (TextView) findViewById(R.id.tv_avg_marks);
        lvStudents = (ListView) findViewById(R.id.lv_students);
        btnAddStudent = (Button) findViewById(R.id.btn_add_student);
        btnRefresh = (Button) findViewById(R.id.btn_refresh);
        btnLogout = (Button) findViewById(R.id.btn_logout);
        tvNoData = (TextView) findViewById(R.id.tv_no_data);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading student data...");
        progressDialog.setCancelable(false);

        adapter = new StudentAdapter(this, studentsList);
        lvStudents.setAdapter(adapter);

        lvStudents.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final Student selectedStudent = studentsList.get(position);
                showStudentOptionsDialog(selectedStudent);
            }
        });

        btnAddStudent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, AddEditStudentActivity.class));
            }
        });

        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fetchStudentsData();
            }
        });

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleLogout();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchStudentsData();
    }

    private void fetchStudentsData() {
        progressDialog.show();
        backendApi.getStudents(new ApiCallback<List<Student>>() {
            @Override
            public void onSuccess(List<Student> students) {
                progressDialog.dismiss();
                studentsList.clear();
                studentsList.addAll(students);
                adapter.notifyDataSetChanged();
                
                if (studentsList.isEmpty()) {
                    tvNoData.setVisibility(View.VISIBLE);
                    lvStudents.setVisibility(View.GONE);
                } else {
                    tvNoData.setVisibility(View.GONE);
                    lvStudents.setVisibility(View.VISIBLE);
                }
                
                calculateAndDisplayStats();
            }

            @Override
            public void onError(String errorMsg) {
                progressDialog.dismiss();
                Toast.makeText(MainActivity.this, "Failed to load data: " + errorMsg, Toast.LENGTH_LONG).show();
            }
        });
    }

    // MANDATORY OFFLINE CALCULATION: Run inside Android native Java
    private void calculateAndDisplayStats() {
        int total = studentsList.size();
        tvTotalStudents.setText(String.valueOf(total));

        if (total == 0) {
            tvAvgAttendance.setText("0%");
            tvAvgMarks.setText("0%");
            return;
        }

        double sumAttendancePct = 0;
        double sumMarksPct = 0;

        for (int i = 0; i < studentsList.size(); i++) {
            Student s = studentsList.get(i);
            sumAttendancePct += s.getAttendancePercentage();
            sumMarksPct += s.getMarksPercentage();
        }

        double avgAttendance = sumAttendancePct / total;
        double avgMarks = sumMarksPct / total;

        tvAvgAttendance.setText(String.format("%.1f%%", avgAttendance));
        tvAvgMarks.setText(String.format("%.1f%%", avgMarks));
    }

    private void showStudentOptionsDialog(final Student student) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(student.getName());
        String[] options = {"Edit Student Details", "Delete Student Record"};
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    Intent intent = new Intent(MainActivity.this, AddEditStudentActivity.class);
                    intent.putExtra("student_id", student.getId());
                    intent.putExtra("name", student.getName());
                    intent.putExtra("roll_no", student.getRollNo());
                    intent.putExtra("grade", student.getGrade());
                    intent.putExtra("attendance_present", student.getAttendancePresent());
                    intent.putExtra("attendance_total", student.getAttendanceTotal());
                    intent.putExtra("marks_obtained", student.getMarksObtained());
                    intent.putExtra("marks_total", student.getMarksTotal());
                    startActivity(intent);
                } else if (which == 1) {
                    confirmDelete(student);
                }
            }
        });
        builder.show();
    }

    private void confirmDelete(final Student student) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirm Delete");
        builder.setMessage("Are you sure you want to delete the record for " + student.getName() + "?");
        builder.setPositiveButton("Yes, Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                progressDialog.setMessage("Deleting student...");
                progressDialog.show();
                backendApi.deleteStudent(student.getId(), new ApiCallback<String>() {
                    @Override
                    public void onSuccess(String result) {
                        progressDialog.dismiss();
                        Toast.makeText(MainActivity.this, "Student deleted successfully", Toast.LENGTH_SHORT).show();
                        fetchStudentsData();
                    }

                    @Override
                    public void onError(String errorMsg) {
                        progressDialog.dismiss();
                        Toast.makeText(MainActivity.this, "Delete failed: " + errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void handleLogout() {
        progressDialog.setMessage("Logging out...");
        progressDialog.show();
        backendApi.logout(new ApiCallback<String>() {
            @Override
            public void onSuccess(String result) {
                progressDialog.dismiss();
                Toast.makeText(MainActivity.this, "Logged out successfully", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
                finish();
            }

            @Override
            public void onError(String errorMsg) {
                progressDialog.dismiss();
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
                finish();
            }
        });
    }

    private static class StudentAdapter extends BaseAdapter {
        private final Context context;
        private final List<Student> list;

        public StudentAdapter(Context context, List<Student> list) {
            this.context = context;
            this.list = list;
        }

        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public Object getItem(int position) {
            return list.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.row_student, parent, false);
            }

            Student student = list.get(position);

            TextView rowName = (TextView) convertView.findViewById(R.id.row_name);
            TextView rowDetails = (TextView) convertView.findViewById(R.id.row_details);
            TextView rowAttendance = (TextView) convertView.findViewById(R.id.row_attendance);
            TextView rowMarks = (TextView) convertView.findViewById(R.id.row_marks);
            TextView rowGradeLetter = (TextView) convertView.findViewById(R.id.row_grade_letter);

            rowName.setText(student.getName());
            rowDetails.setText("Roll No: " + student.getRollNo() + "  •  Class: " + student.getGrade());
            
            rowAttendance.setText("Attendance: " + student.getAttendancePresent() + "/" + student.getAttendanceTotal() + 
                    " (" + String.format("%.1f%%", student.getAttendancePercentage()) + ")");
            
            rowMarks.setText("Marks: " + student.getMarksObtained() + "/" + student.getMarksTotal() + 
                    " (" + String.format("%.1f%%", student.getMarksPercentage()) + ")");
            
            rowGradeLetter.setText(student.getCalculatedGrade());

            return convertView;
        }
    }
}