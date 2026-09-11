package com.studyplannerpro.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MainActivity extends Activity {

    private DatabaseHelper dbHelper;

    // Bottom Navigation Elements
    private LinearLayout navDashboard, navSubjects, navTasks;
    private TextView tvNavDashboard, tvNavSubjects, tvNavTasks;

    // View Containers (Tabs)
    private ScrollView dashboardLayout;
    private LinearLayout subjectsLayout, tasksLayout;

    // --- Dashboard Tab UI ---
    private ProgressBar dashboardProgressBar;
    private TextView dashboardProgressText;
    private TextView statsSubjectsCount, statsTasksCount, statsCompletedCount, statsHoursCount;
    private LinearLayout upcomingTasksContainer;
    private TextView tvNoUpcoming;
    private Button btnQuickAddTask;

    // --- Subjects Tab UI ---
    private EditText etSubjectCode, etSubjectName;
    private Button btnAddSubject;
    private ListView subjectsListView;
    private TextView tvNoSubjects;

    // --- Tasks Tab UI ---
    private Button btnOpenAddTaskDialog;
    private Spinner spinnerFilterStatus, spinnerFilterSubject;
    private ListView tasksListView;
    private TextView tvNoTasks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);

        initViews();
        setupNavigation();
        setupSubjectsTab();
        setupTasksTab();

        // Load Default Tab (Dashboard)
        switchTab("dashboard");
    }

    private void initViews() {
        // Tabs navigation buttons
        navDashboard = findViewById(R.id.nav_dashboard);
        navSubjects = findViewById(R.id.nav_subjects);
        navTasks = findViewById(R.id.nav_tasks);

        tvNavDashboard = findViewById(R.id.tv_nav_dashboard);
        tvNavSubjects = findViewById(R.id.tv_nav_subjects);
        tvNavTasks = findViewById(R.id.tv_nav_tasks);

        // Frame view toggles
        dashboardLayout = findViewById(R.id.dashboard_layout);
        subjectsLayout = findViewById(R.id.subjects_layout);
        tasksLayout = findViewById(R.id.tasks_layout);

        // Dashboard Metrics Views
        dashboardProgressBar = findViewById(R.id.dashboard_progress_bar);
        dashboardProgressText = findViewById(R.id.dashboard_progress_text);
        statsSubjectsCount = findViewById(R.id.stats_subjects_count);
        statsTasksCount = findViewById(R.id.stats_tasks_count);
        statsCompletedCount = findViewById(R.id.stats_completed_count);
        statsHoursCount = findViewById(R.id.stats_hours_count);
        upcomingTasksContainer = findViewById(R.id.upcoming_tasks_container);
        tvNoUpcoming = findViewById(R.id.tv_no_upcoming);
        btnQuickAddTask = findViewById(R.id.btn_quick_add_task);

        // Subjects Tab Views
        etSubjectCode = findViewById(R.id.et_subject_code);
        etSubjectName = findViewById(R.id.et_subject_name);
        btnAddSubject = findViewById(R.id.btn_add_subject);
        subjectsListView = findViewById(R.id.subjects_listview);
        tvNoSubjects = findViewById(R.id.tv_no_subjects);

        // Tasks Tab Views
        btnOpenAddTaskDialog = findViewById(R.id.btn_open_add_task_dialog);
        spinnerFilterStatus = findViewById(R.id.spinner_filter_status);
        spinnerFilterSubject = findViewById(R.id.spinner_filter_subject);
        tasksListView = findViewById(R.id.tasks_listview);
        tvNoTasks = findViewById(R.id.tv_no_tasks);
    }

    private void setupNavigation() {
        navDashboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab("dashboard");
            }
        });

        navSubjects.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab("subjects");
            }
        });

        navTasks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab("tasks");
            }
        });

        btnQuickAddTask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab("tasks");
            }
        });
    }

    private void switchTab(String tabTag) {
        // Reset tab designs
        tvNavDashboard.setTextColor(0xFF777777);
        tvNavDashboard.setTypeface(null, android.graphics.Typeface.NORMAL);
        tvNavSubjects.setTextColor(0xFF777777);
        tvNavSubjects.setTypeface(null, android.graphics.Typeface.NORMAL);
        tvNavTasks.setTextColor(0xFF777777);
        tvNavTasks.setTypeface(null, android.graphics.Typeface.NORMAL);

        dashboardLayout.setVisibility(View.GONE);
        subjectsLayout.setVisibility(View.GONE);
        tasksLayout.setVisibility(View.GONE);

        if (tabTag.equals("dashboard")) {
            tvNavDashboard.setTextColor(0xFF3F51B5);
            tvNavDashboard.setTypeface(null, android.graphics.Typeface.BOLD);
            dashboardLayout.setVisibility(View.VISIBLE);
            refreshStats();
            refreshUpcomingTasks();
        } else if (tabTag.equals("subjects")) {
            tvNavSubjects.setTextColor(0xFF3F51B5);
            tvNavSubjects.setTypeface(null, android.graphics.Typeface.BOLD);
            subjectsLayout.setVisibility(View.VISIBLE);
            loadSubjects();
        } else if (tabTag.equals("tasks")) {
            tvNavTasks.setTextColor(0xFF3F51B5);
            tvNavTasks.setTypeface(null, android.graphics.Typeface.BOLD);
            tasksLayout.setVisibility(View.VISIBLE);
            refreshFilterSpinners();
            loadTasks();
        }
    }

    // --- STATISTICS AND DASHBOARD CALCULATIONS ---

    private void refreshStats() {
        int subjectsCount = dbHelper.getCount(DatabaseHelper.TABLE_SUBJECTS);
        int totalTasks = dbHelper.getCount(DatabaseHelper.TABLE_TASKS);
        int completedTasks = dbHelper.getCompletedTasksCount();
        int pendingTasks = totalTasks - completedTasks;
        int plannedHours = dbHelper.getTotalHoursPlanned();

        statsSubjectsCount.setText(String.valueOf(subjectsCount));
        statsTasksCount.setText(String.valueOf(pendingTasks));
        statsCompletedCount.setText(String.valueOf(completedTasks));
        statsHoursCount.setText(plannedHours + " hrs");

        int progress = 0;
        if (totalTasks > 0) {
            progress = (completedTasks * 100) / totalTasks;
        }

        dashboardProgressBar.setProgress(progress);
        dashboardProgressText.setText(progress + "%");
    }

    private void refreshUpcomingTasks() {
        upcomingTasksContainer.removeAllViews();
        List<Task> list = dbHelper.getUpcomingTasks(3);

        if (list.isEmpty()) {
            tvNoUpcoming.setVisibility(View.VISIBLE);
        } else {
            tvNoUpcoming.setVisibility(View.GONE);
            for (int i = 0; i < list.size(); i++) {
                final Task task = list.get(i);
                View view = getLayoutInflater().inflate(R.layout.task_item, upcomingTasksContainer, false);

                CheckBox cb = view.findViewById(R.id.task_checkbox);
                TextView tvTitle = view.findViewById(R.id.task_title);
                TextView tvSubject = view.findViewById(R.id.task_subject);
                TextView tvDueDate = view.findViewById(R.id.task_due_date);
                TextView tvPriority = view.findViewById(R.id.task_priority);
                TextView tvHours = view.findViewById(R.id.task_hours);
                Button btnDelete = view.findViewById(R.id.btn_delete_task);

                tvTitle.setText(task.getTitle());
                tvSubject.setText(task.getSubjectCode());
                tvDueDate.setText("Due: " + task.getDueDate());
                tvPriority.setText(task.getPriority().toUpperCase());
                tvHours.setText(task.getHours() + " hrs");

                if (task.getPriority().equalsIgnoreCase("High")) {
                    tvPriority.setTextColor(0xFFFF3D00);
                } else if (task.getPriority().equalsIgnoreCase("Medium")) {
                    tvPriority.setTextColor(0xFFFF9100);
                } else {
                    tvPriority.setTextColor(0xFF4CAF50);
                }

                cb.setOnCheckedChangeListener(null);
                cb.setChecked(task.getStatus() == 1);

                cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        dbHelper.updateTaskStatus(task.getId(), isChecked ? 1 : 0);
                        refreshStats();
                        refreshUpcomingTasks();
                    }
                });

                btnDelete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dbHelper.deleteTask(task.getId());
                        refreshStats();
                        refreshUpcomingTasks();
                    }
                });

                upcomingTasksContainer.addView(view);
            }
        }
    }

    // --- SUBJECTS TAB SETUP ---

    private void setupSubjectsTab() {
        btnAddSubject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String code = etSubjectCode.getText().toString().trim();
                String name = etSubjectName.getText().toString().trim();

                if (code.isEmpty() || name.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please fill in all subject fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                boolean success = dbHelper.addSubject(name, code);
                if (success) {
                    Toast.makeText(MainActivity.this, "Subject added successfully!", Toast.LENGTH_SHORT).show();
                    etSubjectCode.setText("");
                    etSubjectName.setText("");
                    loadSubjects();
                } else {
                    Toast.makeText(MainActivity.this, "Failed to save subject", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void loadSubjects() {
        List<Subject> subjects = dbHelper.getAllSubjects();
        if (subjects.isEmpty()) {
            tvNoSubjects.setVisibility(View.VISIBLE);
            subjectsListView.setVisibility(View.GONE);
        } else {
            tvNoSubjects.setVisibility(View.GONE);
            subjectsListView.setVisibility(View.VISIBLE);
            SubjectAdapter adapter = new SubjectAdapter(subjects);
            subjectsListView.setAdapter(adapter);
        }
    }

    // --- TASKS TAB SETUP ---

    private void setupTasksTab() {
        btnOpenAddTaskDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openAddTaskDialog();
            }
        });

        spinnerFilterStatus.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadTasks();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerFilterSubject.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadTasks();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void refreshFilterSpinners() {
        // Status filters dropdown list
        String[] statuses = {"All Statuses", "Pending", "Completed"};
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, statuses);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilterStatus.setAdapter(statusAdapter);

        // Subject filters list
        List<Subject> subjects = dbHelper.getAllSubjects();
        List<String> subjectNames = new ArrayList<>();
        subjectNames.add("All Subjects");
        for (Subject s : subjects) {
            subjectNames.add(s.getCode());
        }
        ArrayAdapter<String> subjectAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, subjectNames);
        subjectAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilterSubject.setAdapter(subjectAdapter);
    }

    private void loadTasks() {
        String selectedStatus = spinnerFilterStatus.getSelectedItem() != null ? spinnerFilterStatus.getSelectedItem().toString() : "All Statuses";
        if (selectedStatus.equals("All Statuses")) {
            selectedStatus = "All";
        }

        int selectedSubjectId = 0;
        int subjectSpinnerPos = spinnerFilterSubject.getSelectedItemPosition();
        if (subjectSpinnerPos > 0) {
            List<Subject> subjects = dbHelper.getAllSubjects();
            if (subjectSpinnerPos - 1 < subjects.size()) {
                selectedSubjectId = subjects.get(subjectSpinnerPos - 1).getId();
            }
        }

        List<Task> tasks = dbHelper.getAllTasks(selectedStatus, selectedSubjectId);
        if (tasks.isEmpty()) {
            tvNoTasks.setVisibility(View.VISIBLE);
            tasksListView.setVisibility(View.GONE);
        } else {
            tvNoTasks.setVisibility(View.GONE);
            tasksListView.setVisibility(View.VISIBLE);
            TaskAdapter adapter = new TaskAdapter(tasks);
            tasksListView.setAdapter(adapter);
        }
    }

    private void openAddTaskDialog() {
        final List<Subject> subjects = dbHelper.getAllSubjects();
        if (subjects.isEmpty()) {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("No Subjects Found")
                    .setMessage("Please add at least one subject first before scheduling academic tasks!")
                    .setPositiveButton("Go to Subjects", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switchTab("subjects");
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_task, null);
        builder.setView(dialogView);

        final EditText etTitle = dialogView.findViewById(R.id.dialog_et_title);
        final Spinner spinnerSubject = dialogView.findViewById(R.id.dialog_spinner_subject);
        final Button btnDatePicker = dialogView.findViewById(R.id.dialog_btn_datepicker);
        final Spinner spinnerPriority = dialogView.findViewById(R.id.dialog_spinner_priority);
        final EditText etHours = dialogView.findViewById(R.id.dialog_et_hours);

        // Load subjects in task selector
        ArrayAdapter<Subject> subAdapter = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_spinner_item, subjects);
        subAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSubject.setAdapter(subAdapter);

        // Load priority settings values
        final String[] priorities = {"High", "Medium", "Low"};
        ArrayAdapter<String> prioAdapter = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_spinner_item, priorities);
        prioAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPriority.setAdapter(prioAdapter);

        // Manage dynamic Date picker selections
        final Calendar calendar = Calendar.getInstance();
        final String[] chosenDate = {String.format("%04d-%02d-%02d", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH))};
        btnDatePicker.setText(chosenDate[0]);

        btnDatePicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int year = calendar.get(Calendar.YEAR);
                int month = calendar.get(Calendar.MONTH);
                int day = calendar.get(Calendar.DAY_OF_MONTH);

                DatePickerDialog datePickerDialog = new DatePickerDialog(MainActivity.this,
                        new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                                chosenDate[0] = String.format("%04d-%02d-%02d", year, (monthOfYear + 1), dayOfMonth);
                                btnDatePicker.setText(chosenDate[0]);
                            }
                        }, year, month, day);
                datePickerDialog.show();
            }
        });

        builder.setPositiveButton("Save Task", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String title = etTitle.getText().toString().trim();
                String hoursStr = etHours.getText().toString().trim();

                if (title.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Task title is required", Toast.LENGTH_SHORT).show();
                    return;
                }

                int selectedSubjectIndex = spinnerSubject.getSelectedItemPosition();
                if (selectedSubjectIndex < 0 || selectedSubjectIndex >= subjects.size()) {
                    return;
                }
                int subjectId = subjects.get(selectedSubjectIndex).getId();
                String priority = spinnerPriority.getSelectedItem() != null ? spinnerPriority.getSelectedItem().toString() : "Medium";
                int hours = 1;
                if (!hoursStr.isEmpty()) {
                    try {
                        hours = Integer.parseInt(hoursStr);
                    } catch (Exception ignored) {}
                }

                boolean success = dbHelper.addTask(subjectId, title, chosenDate[0], priority, hours);
                if (success) {
                    Toast.makeText(MainActivity.this, "Task added successfully!", Toast.LENGTH_SHORT).show();
                    loadTasks();
                    refreshStats();
                } else {
                    Toast.makeText(MainActivity.this, "Failed to schedule task", Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    // --- CUSTOM SUBJECT ADAPTER ---

    private class SubjectAdapter extends BaseAdapter {
        private List<Subject> list;

        public SubjectAdapter(List<Subject> list) {
            this.list = list;
        }

        @Override
        public int getCount() { return list.size(); }

        @Override
        public Object getItem(int position) { return list.get(position); }

        @Override
        public long getItemId(int position) { return list.get(position).getId(); }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.subject_item, parent, false);
            }
            final Subject subject = list.get(position);
            TextView tvCode = convertView.findViewById(R.id.subject_code);
            TextView tvName = convertView.findViewById(R.id.subject_name);
            Button btnDelete = convertView.findViewById(R.id.btn_delete_subject);

            tvCode.setText(subject.getCode());
            tvName.setText(subject.getName());

            btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Delete Subject?")
                            .setMessage("Are you sure? This will delete all tasks linked to this subject.")
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dbHelper.deleteSubject(subject.getId());
                                    loadSubjects();
                                    loadTasks();
                                    refreshStats();
                                }
                            })
                            .setNegativeButton("No", null)
                            .show();
                }
            });

            return convertView;
        }
    }

    // --- CUSTOM TASK ADAPTER ---

    private class TaskAdapter extends BaseAdapter {
        private List<Task> list;

        public TaskAdapter(List<Task> list) {
            this.list = list;
        }

        @Override
        public int getCount() { return list.size(); }

        @Override
        public Object getItem(int position) { return list.get(position); }

        @Override
        public long getItemId(int position) { return list.get(position).getId(); }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.task_item, parent, false);
            }
            final Task task = list.get(position);
            CheckBox cb = convertView.findViewById(R.id.task_checkbox);
            final TextView tvTitle = convertView.findViewById(R.id.task_title);
            TextView tvSubject = convertView.findViewById(R.id.task_subject);
            TextView tvDueDate = convertView.findViewById(R.id.task_due_date);
            TextView tvPriority = convertView.findViewById(R.id.task_priority);
            TextView tvHours = convertView.findViewById(R.id.task_hours);
            Button btnDelete = convertView.findViewById(R.id.btn_delete_task);

            tvTitle.setText(task.getTitle());
            tvSubject.setText(task.getSubjectCode());
            tvDueDate.setText("Due: " + task.getDueDate());
            tvPriority.setText(task.getPriority().toUpperCase());
            tvHours.setText(task.getHours() + " hrs");

            // Assign proper priority color properties
            if (task.getPriority().equalsIgnoreCase("High")) {
                tvPriority.setTextColor(0xFFFF3D00);
            } else if (task.getPriority().equalsIgnoreCase("Medium")) {
                tvPriority.setTextColor(0xFFFF9100);
            } else {
                tvPriority.setTextColor(0xFF4CAF50);
            }

            // Bind checkbox events securely
            cb.setOnCheckedChangeListener(null);
            cb.setChecked(task.getStatus() == 1);

            if (task.getStatus() == 1) {
                tvTitle.setPaintFlags(tvTitle.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                tvTitle.setPaintFlags(tvTitle.getPaintFlags() & (~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG));
            }

            cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    dbHelper.updateTaskStatus(task.getId(), isChecked ? 1 : 0);
                    refreshStats();
                    loadTasks();
                }
            });

            btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dbHelper.deleteTask(task.getId());
                    refreshStats();
                    loadTasks();
                }
            });

            return convertView;
        }
    }
}