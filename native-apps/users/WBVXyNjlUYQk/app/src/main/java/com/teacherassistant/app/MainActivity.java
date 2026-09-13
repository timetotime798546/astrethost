package com.teacherassistant.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {

    // Db and Data Lists
    private DbHelper dbHelper;
    private List<Models.Student> studentList = new ArrayList<>();
    private List<Models.Assignment> assignmentList = new ArrayList<>();
    private List<Models.Lesson> lessonList = new ArrayList<>();

    // Navigation and Tab States
    private int currentTab = 0; // 0 = Students, 1 = Attendance, 2 = Grades, 3 = Planner
    private String selectedDate; // Attendance date target

    // UI elements references
    private TextView headerTitle;
    private Button btnActionAdd;

    // View Containers
    private RelativeLayout containerStudents;
    private RelativeLayout containerAttendance;
    private RelativeLayout containerGrades;
    private RelativeLayout containerLessons;

    // Bottom Tab views
    private LinearLayout tabStudents, tabAttendance, tabGrades, tabLessons;
    private TextView txtTabStudents, txtTabAttendance, txtTabGrades, txtTabLessons;

    // Students UI
    private TextView tvStudentStats;
    private ListView lvStudents;
    private StudentAdapter studentAdapter;

    // Attendance UI
    private TextView tvAttendanceDate;
    private Button btnChangeDate, btnSaveAttendance;
    private ListView lvAttendance;
    private AttendanceAdapter attendanceAdapter;

    // Grades UI
    private Spinner spinnerAssignments;
    private Button btnNewAssignment, btnSaveGrades;
    private TextView tvAssignmentDetails;
    private ListView lvGrades;
    private GradeAdapter gradeAdapter;
    private Models.Assignment activeAssignment = null;

    // Planner UI
    private ListView lvLessons;
    private LessonAdapter lessonAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize SQLite helper and default variables
        dbHelper = new DbHelper(this);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        selectedDate = sdf.format(new Date());

        initViews();
        setupNavigation();
        loadTabContent();
    }

    private void initViews() {
        headerTitle = (TextView) findViewById(R.id.header_title);
        btnActionAdd = (Button) findViewById(R.id.btn_action_add);

        // Sub-container views mapping
        containerStudents = (RelativeLayout) findViewById(R.id.container_students);
        containerAttendance = (RelativeLayout) findViewById(R.id.container_attendance);
        containerGrades = (RelativeLayout) findViewById(R.id.container_grades);
        containerLessons = (RelativeLayout) findViewById(R.id.container_lessons);

        // Bottom Navigation mappings
        tabStudents = (LinearLayout) findViewById(R.id.tab_students);
        tabAttendance = (LinearLayout) findViewById(R.id.tab_attendance);
        tabGrades = (LinearLayout) findViewById(R.id.tab_grades);
        tabLessons = (LinearLayout) findViewById(R.id.tab_lessons);

        txtTabStudents = (TextView) findViewById(R.id.text_tab_students);
        txtTabAttendance = (TextView) findViewById(R.id.text_tab_attendance);
        txtTabGrades = (TextView) findViewById(R.id.text_tab_grades);
        txtTabLessons = (TextView) findViewById(R.id.text_tab_lessons);

        // Students list hooks
        tvStudentStats = (TextView) findViewById(R.id.tv_student_stats);
        lvStudents = (ListView) findViewById(R.id.lv_students);
        studentAdapter = new StudentAdapter();
        lvStudents.setAdapter(studentAdapter);

        // Attendance hooks
        tvAttendanceDate = (TextView) findViewById(R.id.tv_attendance_date);
        btnChangeDate = (Button) findViewById(R.id.btn_change_date);
        btnSaveAttendance = (Button) findViewById(R.id.btn_save_attendance);
        lvAttendance = (ListView) findViewById(R.id.lv_attendance);
        attendanceAdapter = new AttendanceAdapter();
        lvAttendance.setAdapter(attendanceAdapter);

        tvAttendanceDate.setText("Date: " + selectedDate);

        // Gradebook hooks
        spinnerAssignments = (Spinner) findViewById(R.id.spinner_assignments);
        btnNewAssignment = (Button) findViewById(R.id.btn_new_assignment);
        btnSaveGrades = (Button) findViewById(R.id.btn_save_grades);
        tvAssignmentDetails = (TextView) findViewById(R.id.tv_assignment_details);
        lvGrades = (ListView) findViewById(R.id.lv_grades);
        gradeAdapter = new GradeAdapter();
        lvGrades.setAdapter(gradeAdapter);

        // Planner hooks
        lvLessons = (ListView) findViewById(R.id.lv_lessons);
        lessonAdapter = new LessonAdapter();
        lvLessons.setAdapter(lessonAdapter);

        // Context Action Button config
        btnActionAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleGenericAddAction();
            }
        });

        // Setup Date picker click logic
        btnChangeDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePicker();
            }
        });

        // Save Attendance click logic
        btnSaveAttendance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveAllAttendanceInList();
            }
        });

        // Add Assignment dialog launcher
        btnNewAssignment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddAssignmentDialog();
            }
        });

        // Save grades list changes to SQLite
        btnSaveGrades.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveAllGradesInList();
            }
        });

        // Spinner item select implementation
        spinnerAssignments.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                activeAssignment = assignmentList.get(position);
                tvAssignmentDetails.setText("Max score capability: " + activeAssignment.maxPoints + " pts");
                loadGradesForActiveAssignment();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                activeAssignment = null;
                tvAssignmentDetails.setText("Max score: -");
            }
        });
    }

    private void showDatePicker() {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        Calendar res = Calendar.getInstance();
                        res.set(Calendar.YEAR, year);
                        res.set(Calendar.MONTH, monthOfYear);
                        res.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                        selectedDate = sdf.format(res.getTime());
                        tvAttendanceDate.setText("Date: " + selectedDate);
                        loadAttendanceForSelectedDate();
                    }
                }, year, month, day);
        datePickerDialog.show();
    }

    private void setupNavigation() {
        View.OnClickListener clickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int id = v.getId();
                if (id == R.id.tab_students) {
                    currentTab = 0;
                } else if (id == R.id.tab_attendance) {
                    currentTab = 1;
                } else if (id == R.id.tab_grades) {
                    currentTab = 2;
                } else if (id == R.id.tab_lessons) {
                    currentTab = 3;
                }
                updateTabStyle();
                loadTabContent();
            }
        };

        tabStudents.setOnClickListener(clickListener);
        tabAttendance.setOnClickListener(clickListener);
        tabGrades.setOnClickListener(clickListener);
        tabLessons.setOnClickListener(clickListener);
    }

    private void updateTabStyle() {
        // Clear styles
        tabStudents.setBackgroundColor(Color.TRANSPARENT);
        tabAttendance.setBackgroundColor(Color.TRANSPARENT);
        tabGrades.setBackgroundColor(Color.TRANSPARENT);
        tabLessons.setBackgroundColor(Color.TRANSPARENT);

        txtTabStudents.setTextColor(Color.parseColor("#7F8C8D"));
        txtTabAttendance.setTextColor(Color.parseColor("#7F8C8D"));
        txtTabGrades.setTextColor(Color.parseColor("#7F8C8D"));
        txtTabLessons.setTextColor(Color.parseColor("#7F8C8D"));

        // Highlight selected
        if (currentTab == 0) {
            tabStudents.setBackgroundColor(Color.parseColor("#EAECEE"));
            txtTabStudents.setTextColor(Color.parseColor("#2C3E50"));
            txtTabStudents.setTypeface(null, android.graphics.Typeface.BOLD);
        } else if (currentTab == 1) {
            tabAttendance.setBackgroundColor(Color.parseColor("#EAECEE"));
            txtTabAttendance.setTextColor(Color.parseColor("#2C3E50"));
            txtTabAttendance.setTypeface(null, android.graphics.Typeface.BOLD);
        } else if (currentTab == 2) {
            tabGrades.setBackgroundColor(Color.parseColor("#EAECEE"));
            txtTabGrades.setTextColor(Color.parseColor("#2C3E50"));
            txtTabGrades.setTypeface(null, android.graphics.Typeface.BOLD);
        } else if (currentTab == 3) {
            tabLessons.setBackgroundColor(Color.parseColor("#EAECEE"));
            txtTabLessons.setTextColor(Color.parseColor("#2C3E50"));
            txtTabLessons.setTypeface(null, android.graphics.Typeface.BOLD);
        }
    }

    private void loadTabContent() {
        // Toggle view containers
        containerStudents.setVisibility(currentTab == 0 ? View.VISIBLE : View.GONE);
        containerAttendance.setVisibility(currentTab == 1 ? View.VISIBLE : View.GONE);
        containerGrades.setVisibility(currentTab == 2 ? View.VISIBLE : View.GONE);
        containerLessons.setVisibility(currentTab == 3 ? View.VISIBLE : View.GONE);

        // Update header & buttons contextual details
        if (currentTab == 0) {
            headerTitle.setText("Class Students Directory");
            btnActionAdd.setVisibility(View.VISIBLE);
            btnActionAdd.setText("+ Student");
            loadStudentDirectory();
        } else if (currentTab == 1) {
            headerTitle.setText("Roll Call Attendance");
            btnActionAdd.setVisibility(View.GONE);
            loadAttendanceForSelectedDate();
        } else if (currentTab == 2) {
            headerTitle.setText("Assignment Grades");
            btnActionAdd.setVisibility(View.GONE);
            loadAssignmentsHeader();
        } else if (currentTab == 3) {
            headerTitle.setText("Teacher Lesson Planner");
            btnActionAdd.setVisibility(View.VISIBLE);
            btnActionAdd.setText("+ Plan");
            loadLessonsPlanner();
        }
    }

    // Dynamic Context Add Action Button Helper
    private void handleGenericAddAction() {
        if (currentTab == 0) {
            showAddStudentDialog();
        } else if (currentTab == 3) {
            showAddLessonDialog();
        }
    }

    // --- STUDENT LOGIC METHODS ---
    private void loadStudentDirectory() {
        studentList = dbHelper.getAllStudents();
        tvStudentStats.setText("Total Student Records registered: " + studentList.size());
        studentAdapter.notifyDataSetChanged();
    }

    private void showAddStudentDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add New Student");

        final EditText input = new EditText(this);
        input.setHint("Enter student's full name");
        input.setPadding(32, 32, 32, 32);
        builder.setView(input);

        builder.setPositiveButton("Register", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String name = input.getText().toString().trim();
                if (!name.isEmpty()) {
                    dbHelper.insertStudent(name);
                    Toast.makeText(MainActivity.this, "Student registered successfully", Toast.LENGTH_SHORT).show();
                    loadStudentDirectory();
                } else {
                    Toast.makeText(MainActivity.this, "Name field cannot be empty", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    // Custom List Adapters for high compatibility (using BaseAdapter)
    private class StudentAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return studentList.size();
        }

        @Override
        public Object getItem(int position) {
            return studentList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return studentList.get(position).id;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(MainActivity.this).inflate(R.layout.item_student, parent, false);
            }
            final Models.Student student = studentList.get(position);

            TextView tvId = (TextView) convertView.findViewById(R.id.tv_student_id_indicator);
            TextView tvName = (TextView) convertView.findViewById(R.id.tv_student_name);
            TextView tvSummary = (TextView) convertView.findViewById(R.id.tv_student_perf_summary);
            Button btnDelete = (Button) convertView.findViewById(R.id.btn_delete_student);

            tvId.setText(String.valueOf(position + 1));
            tvName.setText(student.name);

            String gradeStr = (student.avgGrade < 0) ? "N/A" : String.format(Locale.US, "%.1f%%", student.avgGrade);
            tvSummary.setText("Attendance Rate: " + student.attendancePercentage + "% | Average grade: " + gradeStr);

            btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Delete Student")
                            .setMessage("Are you sure you want to delete " + student.name + " and all their attendance/grade histories?")
                            .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dbHelper.deleteStudent(student.id);
                                    loadStudentDirectory();
                                }
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                }
            });

            return convertView;
        }
    }

    // --- ATTENDANCE LOGIC METHODS ---
    private List<AttendanceRowHolder> attendanceViewList = new ArrayList<>();

    private static class AttendanceRowHolder {
        long studentId;
        String studentName;
        String status; // PRESENT or ABSENT
    }

    private void loadAttendanceForSelectedDate() {
        studentList = dbHelper.getAllStudents();
        attendanceViewList.clear();

        for (int i = 0; i < studentList.size(); i++) {
            Models.Student student = studentList.get(i);
            AttendanceRowHolder row = new AttendanceRowHolder();
            row.studentId = student.id;
            row.studentName = student.name;
            row.status = dbHelper.getAttendanceStatus(student.id, selectedDate);
            attendanceViewList.add(row);
        }

        attendanceAdapter.notifyDataSetChanged();
    }

    private void saveAllAttendanceInList() {
        if (attendanceViewList.isEmpty()) {
            Toast.makeText(this, "No students enrolled to log attendance", Toast.LENGTH_SHORT).show();
            return;
        }

        for (int i = 0; i < attendanceViewList.size(); i++) {
            AttendanceRowHolder row = attendanceViewList.get(i);
            dbHelper.saveAttendance(row.studentId, selectedDate, row.status);
        }
        Toast.makeText(this, "Attendance saved for date: " + selectedDate, Toast.LENGTH_SHORT).show();
    }

    private class AttendanceAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return attendanceViewList.size();
        }

        @Override
        public Object getItem(int position) {
            return attendanceViewList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return attendanceViewList.get(position).studentId;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(MainActivity.this).inflate(R.layout.item_attendance, parent, false);
            }
            final AttendanceRowHolder row = attendanceViewList.get(position);

            TextView tvName = (TextView) convertView.findViewById(R.id.tv_att_student_name);
            RadioGroup rgStatus = (RadioGroup) convertView.findViewById(R.id.rg_attendance_status);
            RadioButton rbPresent = (RadioButton) convertView.findViewById(R.id.rb_present);
            RadioButton rbAbsent = (RadioButton) convertView.findViewById(R.id.rb_absent);

            tvName.setText(row.studentName);

            // Remove listener before manual toggling to prevent double triggering
            rgStatus.setOnCheckedChangeListener(null);

            if ("PRESENT".equalsIgnoreCase(row.status)) {
                rbPresent.setChecked(true);
            } else {
                rbAbsent.setChecked(true);
            }

            rgStatus.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup group, int checkedId) {
                    if (checkedId == R.id.rb_present) {
                        row.status = "PRESENT";
                    } else if (checkedId == R.id.rb_absent) {
                        row.status = "ABSENT";
                    }
                }
            });

            return convertView;
        }
    }

    // --- GRADES MODULE LOGIC ---
    private List<GradeRowHolder> gradesViewList = new ArrayList<>();

    private static class GradeRowHolder {
        long studentId;
        String studentName;
        double score; // -1 represents empty input
    }

    private void loadAssignmentsHeader() {
        assignmentList = dbHelper.getAllAssignments();
        
        if (assignmentList.isEmpty()) {
            spinnerAssignments.setAdapter(null);
            tvAssignmentDetails.setText("No assignments created yet.");
            gradesViewList.clear();
            gradeAdapter.notifyDataSetChanged();
            activeAssignment = null;
            return;
        }

        ArrayAdapter<Models.Assignment> spinnerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, assignmentList);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAssignments.setAdapter(spinnerAdapter);

        // This triggers the onItemSelected trigger on spinner to fill items list
    }

    private void loadGradesForActiveAssignment() {
        if (activeAssignment == null) return;

        studentList = dbHelper.getAllStudents();
        gradesViewList.clear();

        for (int i = 0; i < studentList.size(); i++) {
            Models.Student s = studentList.get(i);
            GradeRowHolder row = new GradeRowHolder();
            row.studentId = s.id;
            row.studentName = s.name;
            row.score = dbHelper.getGradeScore(s.id, activeAssignment.id);
            gradesViewList.add(row);
        }

        gradeAdapter.notifyDataSetChanged();
    }

    private void showAddAssignmentDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create New Assignment");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 16, 32, 16);

        final EditText etTitle = new EditText(this);
        etTitle.setHint("e.g. Midterm Test, Homework 1");
        etTitle.setSingleLine(true);
        layout.addView(etTitle);

        final EditText etMax = new EditText(this);
        etMax.setHint("Max possible score (e.g. 100)");
        etMax.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        layout.addView(etMax);

        builder.setView(layout);

        builder.setPositiveButton("Create", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String title = etTitle.getText().toString().trim();
                String maxStr = etMax.getText().toString().trim();

                if (title.isEmpty() || maxStr.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Both fields are mandatory", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    double maxPoints = Double.parseDouble(maxStr);
                    dbHelper.insertAssignment(title, maxPoints);
                    Toast.makeText(MainActivity.this, "Assignment created successfully", Toast.LENGTH_SHORT).show();
                    loadAssignmentsHeader();
                } catch (NumberFormatException e) {
                    Toast.makeText(MainActivity.this, "Invalid max score value", Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void saveAllGradesInList() {
        if (activeAssignment == null) {
            Toast.makeText(this, "Please create and select an assignment first", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate values first before committing
        for (int i = 0; i < gradesViewList.size(); i++) {
            GradeRowHolder row = gradesViewList.get(i);
            if (row.score > activeAssignment.maxPoints) {
                Toast.makeText(this, "Error: " + row.studentName + " score exceeds max limit of " + activeAssignment.maxPoints, Toast.LENGTH_LONG).show();
                return;
            }
        }

        for (int i = 0; i < gradesViewList.size(); i++) {
            GradeRowHolder row = gradesViewList.get(i);
            dbHelper.saveGrade(row.studentId, activeAssignment.id, row.score);
        }

        Toast.makeText(this, "Successfully compiled and saved all grades to DB", Toast.LENGTH_SHORT).show();
    }

    private class GradeAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return gradesViewList.size();
        }

        @Override
        public Object getItem(int position) {
            return gradesViewList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return gradesViewList.get(position).studentId;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(MainActivity.this).inflate(R.layout.item_grade, parent, false);
            }
            final GradeRowHolder row = gradesViewList.get(position);

            TextView tvName = (TextView) convertView.findViewById(R.id.tv_grade_student_name);
            final EditText etScore = (EditText) convertView.findViewById(R.id.et_grade_score);
            TextView tvMax = (TextView) convertView.findViewById(R.id.tv_grade_max);

            tvName.setText(row.studentName);
            if (activeAssignment != null) {
                tvMax.setText("/ " + activeAssignment.maxPoints);
            } else {
                tvMax.setText("/ -");
            }

            // Remove watcher or change focus strategies to save inputs dynamically without complex TextWatchers
            if (row.score < 0) {
                etScore.setText("");
            } else {
                etScore.setText(String.valueOf(row.score));
            }

            etScore.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (!hasFocus) {
                        String val = etScore.getText().toString().trim();
                        if (val.isEmpty()) {
                            row.score = -1;
                        } else {
                            try {
                                row.score = Double.parseDouble(val);
                            } catch (NumberFormatException e) {
                                row.score = -1;
                            }
                        }
                    }
                }
            });

            return convertView;
        }
    }

    // --- LESSON PLANNER MODULE LOGIC ---
    private void loadLessonsPlanner() {
        lessonList = dbHelper.getAllLessons();
        lessonAdapter.notifyDataSetChanged();
    }

    private void showAddLessonDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("New Lesson Plan Outline");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 16, 32, 16);

        final EditText etTitle = new EditText(this);
        etTitle.setHint("Lesson Topic / Title");
        etTitle.setSingleLine(true);
        layout.addView(etTitle);

        final EditText etNotes = new EditText(this);
        etNotes.setHint("Goals, materials, checklists, reminders...");
        etNotes.setMinLines(3);
        layout.addView(etNotes);

        builder.setView(layout);

        builder.setPositiveButton("Save Outline", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String title = etTitle.getText().toString().trim();
                String notes = etNotes.getText().toString().trim();

                if (title.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Title must be set", Toast.LENGTH_SHORT).show();
                    return;
                }

                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                String curDate = sdf.format(new Date());

                dbHelper.insertLesson(title, curDate, notes, "DRAFT");
                Toast.makeText(MainActivity.this, "Lesson registered", Toast.LENGTH_SHORT).show();
                loadLessonsPlanner();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private class LessonAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return lessonList.size();
        }

        @Override
        public Object getItem(int position) {
            return lessonList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return lessonList.get(position).id;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(MainActivity.this).inflate(R.layout.item_lesson, parent, false);
            }
            final Models.Lesson lesson = lessonList.get(position);

            TextView tvTitle = (TextView) convertView.findViewById(R.id.tv_lesson_title);
            TextView tvDate = (TextView) convertView.findViewById(R.id.tv_lesson_date);
            TextView tvNotes = (TextView) convertView.findViewById(R.id.tv_lesson_notes);
            TextView tvStatus = (TextView) convertView.findViewById(R.id.tv_lesson_status);
            Button btnToggle = (Button) convertView.findViewById(R.id.btn_toggle_lesson);
            Button btnDelete = (Button) convertView.findViewById(R.id.btn_delete_lesson);

            tvTitle.setText(lesson.title);
            tvDate.setText("Scheduled: " + lesson.date);
            tvNotes.setText(lesson.notes);
            tvStatus.setText("Status: " + lesson.status);

            if ("COMPLETED".equalsIgnoreCase(lesson.status)) {
                tvStatus.setBackgroundColor(Color.parseColor("#27AE60"));
                btnToggle.setText("Mark Draft");
            } else {
                tvStatus.setBackgroundColor(Color.parseColor("#F39C12"));
                btnToggle.setText("Mark Complete");
            }

            btnToggle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String nextStatus = "COMPLETED".equalsIgnoreCase(lesson.status) ? "DRAFT" : "COMPLETED";
                    dbHelper.updateLessonStatus(lesson.id, nextStatus);
                    loadLessonsPlanner();
                }
            });

            btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dbHelper.deleteLesson(lesson.id);
                    loadLessonsPlanner();
                }
            });

            return convertView;
        }
    }
}