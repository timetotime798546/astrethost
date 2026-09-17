package com.studentassistant.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    // Tab buttons
    private Button btnTabTasks, btnTabGPA, btnTabTimer, btnTabNotes;
    // Tab layouts
    private LinearLayout layoutTasks, layoutGPA, layoutTimer, layoutNotes;

    // --- Task Manager fields ---
    private EditText etTaskName;
    private Spinner spinnerPriority;
    private Button btnAddTask;
    private LinearLayout containerTasks;
    
    private static class Task {
        String name;
        String priority;

        Task(String name, String priority) {
            this.name = name;
            this.priority = priority;
        }
    }
    private List<Task> taskList = new ArrayList<>();

    // --- GPA Calculator fields ---
    private EditText etCourseName;
    private Spinner spinnerGrade;
    private EditText etCredits;
    private Button btnAddCourse, btnClearGPA;
    private LinearLayout containerCourses;
    private TextView tvGPAScore, tvGPASummary;

    private static class Course {
        String name;
        String grade;
        double credits;

        Course(String name, String grade, double credits) {
            this.name = name;
            this.grade = grade;
            this.credits = credits;
        }
    }
    private List<Course> courseList = new ArrayList<>();

    // --- Study Timer fields ---
    private TextView tvTimer, tvTimerState;
    private Button btnTimerStart, btnTimerReset;
    private Button btnModeStudy, btnModeShort, btnModeLong;
    private CountDownTimer timer;
    private boolean isTimerRunning = false;
    private long timeLeftInMillis = 1500000; // Default: 25 mins
    private String currentTimerState = "STUDY"; // STUDY, SHORT, LONG

    // --- Notes fields ---
    private EditText etNotes;
    private Button btnClearNotes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bind UI navigation
        btnTabTasks = findViewById(R.id.btn_tab_tasks);
        btnTabGPA = findViewById(R.id.btn_tab_gpa);
        btnTabTimer = findViewById(R.id.btn_tab_timer);
        btnTabNotes = findViewById(R.id.btn_tab_notes);

        layoutTasks = findViewById(R.id.layout_tasks);
        layoutGPA = findViewById(R.id.layout_gpa);
        layoutTimer = findViewById(R.id.layout_timer);
        layoutNotes = findViewById(R.id.layout_notes);

        // Navigation listeners
        btnTabTasks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab("TASKS");
            }
        });
        btnTabGPA.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab("GPA");
            }
        });
        btnTabTimer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab("TIMER");
            }
        });
        btnTabNotes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab("NOTES");
            }
        });

        // Initialize Tab view states
        switchTab("TASKS");

        // --- Task setup ---
        etTaskName = findViewById(R.id.et_task_name);
        spinnerPriority = findViewById(R.id.spinner_priority);
        btnAddTask = findViewById(R.id.btn_add_task);
        containerTasks = findViewById(R.id.container_tasks);

        String[] priorities = {"High Priority", "Medium Priority", "Low Priority"};
        ArrayAdapter<String> priorityAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, priorities);
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPriority.setAdapter(priorityAdapter);

        btnAddTask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = etTaskName.getText().toString().trim();
                if (name.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please enter a task description", Toast.LENGTH_SHORT).show();
                    return;
                }
                String priority = spinnerPriority.getSelectedItem().toString();
                taskList.add(new Task(name, priority));
                saveTasks();
                updateTasksUI();
                etTaskName.setText("");
            }
        });

        loadTasks();
        updateTasksUI();

        // --- GPA setup ---
        etCourseName = findViewById(R.id.et_course_name);
        spinnerGrade = findViewById(R.id.spinner_grade);
        etCredits = findViewById(R.id.et_credits);
        btnAddCourse = findViewById(R.id.btn_add_course);
        btnClearGPA = findViewById(R.id.btn_clear_gpa);
        containerCourses = findViewById(R.id.container_courses);
        tvGPAScore = findViewById(R.id.tv_gpa_score);
        tvGPASummary = findViewById(R.id.tv_gpa_summary);

        String[] grades = {"A (4.0)", "B (3.0)", "C (2.0)", "D (1.0)", "F (0.0)"};
        ArrayAdapter<String> gradeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, grades);
        gradeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGrade.setAdapter(gradeAdapter);

        btnAddCourse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = etCourseName.getText().toString().trim();
                String creditsStr = etCredits.getText().toString().trim();

                if (name.isEmpty() || creditsStr.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Fill in all course details", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    double credits = Double.parseDouble(creditsStr);
                    if (credits <= 0) {
                        Toast.makeText(MainActivity.this, "Credits must be greater than 0", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String grade = spinnerGrade.getSelectedItem().toString();
                    courseList.add(new Course(name, grade, credits));
                    saveCourses();
                    updateGPAUI();
                    etCourseName.setText("");
                    etCredits.setText("");
                } catch (NumberFormatException e) {
                    Toast.makeText(MainActivity.this, "Enter a valid credit number", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnClearGPA.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                courseList.clear();
                saveCourses();
                updateGPAUI();
                Toast.makeText(MainActivity.this, "Course list cleared", Toast.LENGTH_SHORT).show();
            }
        });

        loadCourses();
        updateGPAUI();

        // --- Timer Setup ---
        tvTimer = findViewById(R.id.tv_timer);
        tvTimerState = findViewById(R.id.tv_timer_state);
        btnTimerStart = findViewById(R.id.btn_timer_start);
        btnTimerReset = findViewById(R.id.btn_timer_reset);
        btnModeStudy = findViewById(R.id.btn_mode_study);
        btnModeShort = findViewById(R.id.btn_mode_short);
        btnModeLong = findViewById(R.id.btn_mode_long);

        btnTimerStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isTimerRunning) {
                    pauseTimer();
                } else {
                    startTimer();
                }
            }
        });

        btnTimerReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetTimer();
            }
        });

        btnModeStudy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setTimerMode("STUDY", 1500000, "#FF5722");
            }
        });

        btnModeShort.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setTimerMode("SHORT", 300000, "#4CAF50");
            }
        });

        btnModeLong.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setTimerMode("LONG", 900000, "#2196F3");
            }
        });

        updateTimerText();

        // --- Notes Setup ---
        etNotes = findViewById(R.id.et_notes);
        btnClearNotes = findViewById(R.id.btn_clear_notes);

        String savedNote = getSharedPreferences("StudentPref", MODE_PRIVATE).getString("notes", "");
        etNotes.setText(savedNote);

        etNotes.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                getSharedPreferences("StudentPref", MODE_PRIVATE).edit().putString("notes", s.toString()).apply();
            }
        });

        btnClearNotes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Clear Note?");
                builder.setMessage("This will delete your study notes completely. Proceed?");
                builder.setPositiveButton("Clear", new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface dialog, int which) {
                        etNotes.setText("");
                        getSharedPreferences("StudentPref", MODE_PRIVATE).edit().putString("notes", "").apply();
                    }
                });
                builder.setNegativeButton("Cancel", null);
                builder.create().show();
            }
        });
    }

    private void switchTab(String tabName) {
        layoutTasks.setVisibility(View.GONE);
        layoutGPA.setVisibility(View.GONE);
        layoutTimer.setVisibility(View.GONE);
        layoutNotes.setVisibility(View.GONE);

        btnTabTasks.setBackgroundColor(Color.parseColor("#303F9F"));
        btnTabGPA.setBackgroundColor(Color.parseColor("#303F9F"));
        btnTabTimer.setBackgroundColor(Color.parseColor("#303F9F"));
        btnTabNotes.setBackgroundColor(Color.parseColor("#303F9F"));

        if (tabName.equals("TASKS")) {
            layoutTasks.setVisibility(View.VISIBLE);
            btnTabTasks.setBackgroundColor(Color.parseColor("#3F51B5"));
        } else if (tabName.equals("GPA")) {
            layoutGPA.setVisibility(View.VISIBLE);
            btnTabGPA.setBackgroundColor(Color.parseColor("#3F51B5"));
        } else if (tabName.equals("TIMER")) {
            layoutTimer.setVisibility(View.VISIBLE);
            btnTabTimer.setBackgroundColor(Color.parseColor("#3F51B5"));
        } else if (tabName.equals("NOTES")) {
            layoutNotes.setVisibility(View.VISIBLE);
            btnTabNotes.setBackgroundColor(Color.parseColor("#3F51B5"));
        }
    }

    // --- Task functions ---
    private void saveTasks() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < taskList.size(); i++) {
            Task task = taskList.get(i);
            sb.append(task.name.replace("|", "").replace(";", "")).append("|")
              .append(task.priority);
            if (i < taskList.size() - 1) {
                sb.append(";");
            }
        }
        getSharedPreferences("StudentPref", MODE_PRIVATE).edit().putString("tasks", sb.toString()).apply();
    }

    private void loadTasks() {
        taskList.clear();
        String tasksStr = getSharedPreferences("StudentPref", MODE_PRIVATE).getString("tasks", "");
        if (!tasksStr.isEmpty()) {
            String[] items = tasksStr.split(";");
            for (String item : items) {
                String[] parts = item.split("\\|");
                if (parts.length == 2) {
                    taskList.add(new Task(parts[0], parts[1]));
                }
            }
        }
    }

    private void updateTasksUI() {
        containerTasks.removeAllViews();
        for (int i = 0; i < taskList.size(); i++) {
            final int index = i;
            final Task task = taskList.get(index);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(16, 16, 16, 16);
            
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            layoutParams.setMargins(0, 4, 0, 8);
            row.setLayoutParams(layoutParams);
            row.setBackgroundColor(0xFFFFFFFF);

            CheckBox cb = new CheckBox(this);
            cb.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    taskList.remove(index);
                    saveTasks();
                    Toast.makeText(MainActivity.this, "Task completed!", Toast.LENGTH_SHORT).show();
                    updateTasksUI();
                }
            });
            row.addView(cb);

            LinearLayout textWrapper = new LinearLayout(this);
            textWrapper.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams wrapParams = new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1.0f
            );
            wrapParams.gravity = Gravity.CENTER_VERTICAL;
            textWrapper.setLayoutParams(wrapParams);

            TextView tvName = new TextView(this);
            tvName.setText(task.name);
            tvName.setTextSize(15);
            tvName.setTextColor(0xFF212121);
            textWrapper.addView(tvName);

            TextView tvPriority = new TextView(this);
            tvPriority.setText(task.priority);
            tvPriority.setTextSize(11);
            if (task.priority.contains("High")) {
                tvPriority.setTextColor(Color.parseColor("#F44336"));
            } else if (task.priority.contains("Medium")) {
                tvPriority.setTextColor(Color.parseColor("#FF9800"));
            } else {
                tvPriority.setTextColor(Color.parseColor("#4CAF50"));
            }
            textWrapper.addView(tvPriority);

            row.addView(textWrapper);

            Button btnDelete = new Button(this);
            btnDelete.setText("✕");
            btnDelete.setTextSize(14);
            btnDelete.setTextColor(Color.parseColor("#9E9E9E"));
            btnDelete.setBackgroundColor(Color.TRANSPARENT);
            LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                    80,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            btnDelete.setLayoutParams(btnParams);
            btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    taskList.remove(index);
                    saveTasks();
                    updateTasksUI();
                }
            });
            row.addView(btnDelete);

            containerTasks.addView(row);
        }
    }

    // --- GPA functions ---
    private void saveCourses() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < courseList.size(); i++) {
            Course course = courseList.get(i);
            sb.append(course.name.replace("|", "").replace(";", "")).append("|")
              .append(course.grade).append("|")
              .append(course.credits);
            if (i < courseList.size() - 1) {
                sb.append(";");
            }
        }
        getSharedPreferences("StudentPref", MODE_PRIVATE).edit().putString("courses", sb.toString()).apply();
    }

    private void loadCourses() {
        courseList.clear();
        String coursesStr = getSharedPreferences("StudentPref", MODE_PRIVATE).getString("courses", "");
        if (!coursesStr.isEmpty()) {
            String[] items = coursesStr.split(";");
            for (String item : items) {
                String[] parts = item.split("\\|");
                if (parts.length == 3) {
                    try {
                        courseList.add(new Course(parts[0], parts[1], Double.parseDouble(parts[2])));
                    } catch (NumberFormatException e) {
                        // ignore corrupt records
                    }
                }
            }
        }
    }

    private void updateGPAUI() {
        containerCourses.removeAllViews();
        double totalPoints = 0;
        double totalCredits = 0;

        for (int i = 0; i < courseList.size(); i++) {
            final int index = i;
            final Course course = courseList.get(index);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(16, 16, 16, 16);
            
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            layoutParams.setMargins(0, 4, 0, 8);
            row.setLayoutParams(layoutParams);
            row.setBackgroundColor(0xFFFFFFFF);

            LinearLayout textWrapper = new LinearLayout(this);
            textWrapper.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams wrapParams = new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1.0f
            );
            wrapParams.gravity = Gravity.CENTER_VERTICAL;
            textWrapper.setLayoutParams(wrapParams);

            TextView tvName = new TextView(this);
            tvName.setText(course.name);
            tvName.setTextSize(15);
            tvName.setTextColor(0xFF212121);
            textWrapper.addView(tvName);

            TextView tvDetails = new TextView(this);
            tvDetails.setText("Grade: " + course.grade + " | Credits: " + course.credits);
            tvDetails.setTextSize(12);
            tvDetails.setTextColor(0xFF757575);
            textWrapper.addView(tvDetails);

            row.addView(textWrapper);

            double gradePoints = getGradePoints(course.grade);
            totalPoints += (gradePoints * course.credits);
            totalCredits += course.credits;

            Button btnDelete = new Button(this);
            btnDelete.setText("✕");
            btnDelete.setTextSize(14);
            btnDelete.setTextColor(Color.parseColor("#9E9E9E"));
            btnDelete.setBackgroundColor(Color.TRANSPARENT);
            LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                    80,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            btnDelete.setLayoutParams(btnParams);
            btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    courseList.remove(index);
                    saveCourses();
                    updateGPAUI();
                }
            });
            row.addView(btnDelete);

            containerCourses.addView(row);
        }

        double gpa = 0.0;
        if (totalCredits > 0) {
            gpa = totalPoints / totalCredits;
            tvGPASummary.setText("Total Credits: " + totalCredits + " | Total Points: " + String.format("%.2f", totalPoints));
        } else {
            tvGPASummary.setText("Add courses below to calculate GPA");
        }

        tvGPAScore.setText(String.format("%.2f", gpa));
        if (gpa >= 3.5) {
            tvGPAScore.setTextColor(Color.parseColor("#4CAF50"));
        } else if (gpa >= 2.5) {
            tvGPAScore.setTextColor(Color.parseColor("#FF9800"));
        } else if (gpa > 0) {
            tvGPAScore.setTextColor(Color.parseColor("#F44336"));
        } else {
            tvGPAScore.setTextColor(Color.parseColor("#757575"));
        }
    }

    private double getGradePoints(String grade) {
        if (grade.contains("A")) return 4.0;
        if (grade.contains("B")) return 3.0;
        if (grade.contains("C")) return 2.0;
        if (grade.contains("D")) return 1.0;
        return 0.0;
    }

    // --- Timer functions ---
    private void setTimerMode(String mode, long duration, String themeColorHex) {
        if (timer != null) {
            timer.cancel();
        }
        isTimerRunning = false;
        btnTimerStart.setText("START");
        currentTimerState = mode;
        timeLeftInMillis = duration;

        if (mode.equals("STUDY")) {
            tvTimerState.setText("STUDYING TIME");
        } else if (mode.equals("SHORT")) {
            tvTimerState.setText("SHORT BREAK");
        } else {
            tvTimerState.setText("LONG BREAK");
        }

        int themeColor = Color.parseColor(themeColorHex);
        tvTimer.setTextColor(themeColor);
        btnTimerStart.setBackgroundColor(themeColor);

        // Update button tabs selections visually
        btnModeStudy.setBackgroundColor(mode.equals("STUDY") ? Color.parseColor("#FF5722") : Color.parseColor("#9E9E9E"));
        btnModeShort.setBackgroundColor(mode.equals("SHORT") ? Color.parseColor("#4CAF50") : Color.parseColor("#9E9E9E"));
        btnModeLong.setBackgroundColor(mode.equals("LONG") ? Color.parseColor("#2196F3") : Color.parseColor("#9E9E9E"));

        updateTimerText();
    }

    private void startTimer() {
        timer = new CountDownTimer(timeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                updateTimerText();
            }

            @Override
            public void onFinish() {
                isTimerRunning = false;
                btnTimerStart.setText("START");
                Toast.makeText(MainActivity.this, "Session finished! Great job!", Toast.LENGTH_LONG).show();
                showTimerFinishedDialog();
            }
        }.start();

        isTimerRunning = true;
        btnTimerStart.setText("PAUSE");
    }

    private void pauseTimer() {
        if (timer != null) {
            timer.cancel();
        }
        isTimerRunning = false;
        btnTimerStart.setText("RESUME");
    }

    private void resetTimer() {
        if (timer != null) {
            timer.cancel();
        }
        isTimerRunning = false;
        btnTimerStart.setText("START");

        if (currentTimerState.equals("STUDY")) {
            timeLeftInMillis = 1500000;
        } else if (currentTimerState.equals("SHORT")) {
            timeLeftInMillis = 300000;
        } else {
            timeLeftInMillis = 900000;
        }
        updateTimerText();
    }

    private void updateTimerText() {
        int minutes = (int) (timeLeftInMillis / 1000) / 60;
        int seconds = (int) (timeLeftInMillis / 1000) % 60;
        tvTimer.setText(String.format("%02d:%02d", minutes, seconds));
    }

    private void showTimerFinishedDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Time is Up!");
        builder.setMessage("Awesome study effort! Take a quick rest or jump back into learning.");
        builder.setPositiveButton("Awesome!", null);
        builder.create().show();
    }
}