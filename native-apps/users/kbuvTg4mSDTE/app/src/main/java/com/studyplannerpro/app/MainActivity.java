package com.studyplannerpro.app;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MainActivity extends Activity {

    private static final String PREFS_NAME = "StudyPlannerPrefs";
    private static final String KEY_SUBJECTS = "subjects";
    private static final String KEY_TASKS = "tasks";

    // Header & switch actions
    private TextView tvHeaderTitle;
    private Button btnActionAdd;

    // View Switching layouts
    private View layoutDashboard;
    private View layoutSubjects;
    private View layoutTasks;

    // Tabs
    private LinearLayout tabDashboard;
    private LinearLayout tabSubjects;
    private LinearLayout tabTasks;
    private TextView tvTabDashboard;
    private TextView tvTabSubjects;
    private TextView tvTabTasks;

    // Dashboard Items
    private TextView tvOverallPercentage;
    private TextView tvOverallFraction;
    private ProgressBar pbOverallProgress;
    private TextView tvStatSubjects;
    private TextView tvStatPending;

    // Subjects UI Items
    private LinearLayout containerSubjects;
    private TextView tvEmptySubjects;

    // Tasks UI Items
    private LinearLayout containerTasks;
    private TextView tvEmptyTasks;
    private Spinner spinnerTaskFilter;

    // Internal Memory storage sets
    private List<Subject> subjectsList = new ArrayList<>();
    private List<Task> tasksList = new ArrayList<>();

    // Variables used during addition modal workflows
    private int selectedSubjectColor = Color.parseColor("#1E88E5");
    private Calendar calendarReminderTime = Calendar.getInstance();

    // Data Structures
    static class Subject {
        String name;
        int color;

        Subject(String name, int color) {
            this.name = name;
            this.color = color;
        }

        JSONObject toJSONObject() throws JSONException {
            JSONObject obj = new JSONObject();
            obj.put("name", name);
            obj.put("color", color);
            return obj;
        }

        static Subject fromJSONObject(JSONObject obj) throws JSONException {
            return new Subject(obj.getString("name"), obj.getInt("color"));
        }
    }

    static class Task {
        long id;
        String title;
        String subjectName;
        String dueDate;
        boolean completed;
        boolean reminderEnabled;
        long reminderTimestamp;

        Task(long id, String title, String subjectName, String dueDate, boolean completed, boolean reminderEnabled, long reminderTimestamp) {
            this.id = id;
            this.title = title;
            this.subjectName = subjectName;
            this.dueDate = dueDate;
            this.completed = completed;
            this.reminderEnabled = reminderEnabled;
            this.reminderTimestamp = reminderTimestamp;
        }

        JSONObject toJSONObject() throws JSONException {
            JSONObject obj = new JSONObject();
            obj.put("id", id);
            obj.put("title", title);
            obj.put("subjectName", subjectName);
            obj.put("dueDate", dueDate);
            obj.put("completed", completed);
            obj.put("reminderEnabled", reminderEnabled);
            obj.put("reminderTimestamp", reminderTimestamp);
            return obj;
        }

        static Task fromJSONObject(JSONObject obj) throws JSONException {
            return new Task(
                    obj.getLong("id"),
                    obj.getString("title"),
                    obj.getString("subjectName"),
                    obj.getString("dueDate"),
                    obj.getBoolean("completed"),
                    obj.optBoolean("reminderEnabled", false),
                    obj.optLong("reminderTimestamp", 0)
            );
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bind main interface controllers
        tvHeaderTitle = (TextView) findViewById(R.id.tv_header_title);
        btnActionAdd = (Button) findViewById(R.id.btn_action_add);

        layoutDashboard = findViewById(R.id.layout_dashboard);
        layoutSubjects = findViewById(R.id.layout_subjects);
        layoutTasks = findViewById(R.id.layout_tasks);

        tabDashboard = (LinearLayout) findViewById(R.id.tab_dashboard);
        tabSubjects = (LinearLayout) findViewById(R.id.tab_subjects);
        tabTasks = (LinearLayout) findViewById(R.id.tab_tasks);

        tvTabDashboard = (TextView) findViewById(R.id.tv_tab_dashboard);
        tvTabSubjects = (TextView) findViewById(R.id.tv_tab_subjects);
        tvTabTasks = (TextView) findViewById(R.id.tv_tab_tasks);

        // Bind Analytics widgets
        tvOverallPercentage = (TextView) findViewById(R.id.tv_overall_percentage);
        tvOverallFraction = (TextView) findViewById(R.id.tv_overall_fraction);
        pbOverallProgress = (ProgressBar) findViewById(R.id.pb_overall_progress);
        tvStatSubjects = (TextView) findViewById(R.id.tv_stat_subjects);
        tvStatPending = (TextView) findViewById(R.id.tv_stat_pending);

        // Bind Lists widgets
        containerSubjects = (LinearLayout) findViewById(R.id.container_subjects);
        tvEmptySubjects = (TextView) findViewById(R.id.tv_empty_subjects);

        containerTasks = (LinearLayout) findViewById(R.id.container_tasks);
        tvEmptyTasks = (TextView) findViewById(R.id.tv_empty_tasks);
        spinnerTaskFilter = (Spinner) findViewById(R.id.spinner_task_filter);

        // Load Persistent Memory
        loadSavedData();

        // Switch to default dashboard tab
        switchActiveTab(0);

        // Setup switch action tab operations
        tabDashboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchActiveTab(0);
            }
        });

        tabSubjects.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchActiveTab(1);
            }
        });

        tabTasks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchActiveTab(2);
            }
        });

        // Main upper dynamic add action operation
        btnActionAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (layoutSubjects.getVisibility() == View.VISIBLE) {
                    showAddSubjectDialog();
                } else {
                    showAddTaskDialog();
                }
            }
        });

        // Filter operations setups
        final String[] filters = {"All Tasks", "Active Tasks", "Completed Tasks"};
        ArrayAdapter<String> filterAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, filters);
        spinnerTaskFilter.setAdapter(filterAdapter);
        spinnerTaskFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                renderTasks();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Ask for exact alarm permission on Android 13+ inside main
        requestNotificationPermission();

        // Populate initial UI displays
        refreshDashboardAnalytics();
        renderSubjects();
        renderTasks();
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 101);
            }
        }
    }

    private void switchActiveTab(int index) {
        // Clear background highlights
        tabDashboard.setBackgroundColor(Color.TRANSPARENT);
        tabSubjects.setBackgroundColor(Color.TRANSPARENT);
        tabTasks.setBackgroundColor(Color.TRANSPARENT);

        tvTabDashboard.setTextColor(Color.parseColor("#757575"));
        tvTabSubjects.setTextColor(Color.parseColor("#757575"));
        tvTabTasks.setTextColor(Color.parseColor("#757575"));

        tvTabDashboard.setTypeface(null, android.graphics.Typeface.NORMAL);
        tvTabSubjects.setTypeface(null, android.graphics.Typeface.NORMAL);
        tvTabTasks.setTypeface(null, android.graphics.Typeface.NORMAL);

        layoutDashboard.setVisibility(View.GONE);
        layoutSubjects.setVisibility(View.GONE);
        layoutTasks.setVisibility(View.GONE);

        // Highlight selected state
        if (index == 0) {
            layoutDashboard.setVisibility(View.VISIBLE);
            tabDashboard.setBackgroundResource(R.drawable.tab_selected_bg);
            tvTabDashboard.setTextColor(Color.parseColor("#1976D2"));
            tvTabDashboard.setTypeface(null, android.graphics.Typeface.BOLD);
            tvHeaderTitle.setText("Dashboard");
            btnActionAdd.setVisibility(View.GONE);
            refreshDashboardAnalytics();
        } else if (index == 1) {
            layoutSubjects.setVisibility(View.VISIBLE);
            tabSubjects.setBackgroundResource(R.drawable.tab_selected_bg);
            tvTabSubjects.setTextColor(Color.parseColor("#1976D2"));
            tvTabSubjects.setTypeface(null, android.graphics.Typeface.BOLD);
            tvHeaderTitle.setText("Subjects");
            btnActionAdd.setVisibility(View.VISIBLE);
            btnActionAdd.setText("+ SUBJECT");
            renderSubjects();
        } else if (index == 2) {
            layoutTasks.setVisibility(View.VISIBLE);
            tabTasks.setBackgroundResource(R.drawable.tab_selected_bg);
            tvTabTasks.setTextColor(Color.parseColor("#1976D2"));
            tvTabTasks.setTypeface(null, android.graphics.Typeface.BOLD);
            tvHeaderTitle.setText("Planner Tasks");
            btnActionAdd.setVisibility(View.VISIBLE);
            btnActionAdd.setText("+ TASK");
            renderTasks();
        }
    }

    // Refresh metrics shown in the dashboard insights
    private void refreshDashboardAnalytics() {
        int subjectsCount = subjectsList.size();
        int totalTasks = tasksList.size();
        int completedTasksCount = 0;

        for (int i = 0; i < tasksList.size(); i++) {
            if (tasksList.get(i).completed) {
                completedTasksCount++;
            }
        }

        int pendingTasksCount = totalTasks - completedTasksCount;
        int percentage = totalTasks == 0 ? 0 : (completedTasksCount * 100) / totalTasks;

        tvOverallPercentage.setText(percentage + "%");
        tvOverallFraction.setText(" (" + completedTasksCount + " of " + totalTasks + " tasks done)");
        pbOverallProgress.setProgress(percentage);

        tvStatSubjects.setText(String.valueOf(subjectsCount));
        tvStatPending.setText(String.valueOf(pendingTasksCount));
    }

    // Load elements from SharedPreferences
    private void loadSavedData() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        // Load Subjects
        subjectsList.clear();
        String subJson = prefs.getString(KEY_SUBJECTS, null);
        if (subJson != null) {
            try {
                JSONArray arr = new JSONArray(subJson);
                for (int i = 0; i < arr.length(); i++) {
                    subjectsList.add(Subject.fromJSONObject(arr.getJSONObject(i)));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            // Default Subjects to pre-populate if empty
            subjectsList.add(new Subject("Mathematics", Color.parseColor("#1E88E5")));
            subjectsList.add(new Subject("Science", Color.parseColor("#43A047")));
            subjectsList.add(new Subject("History", Color.parseColor("#FB8C00")));
            saveSubjects();
        }

        // Load Tasks
        tasksList.clear();
        String taskJson = prefs.getString(KEY_TASKS, null);
        if (taskJson != null) {
            try {
                JSONArray arr = new JSONArray(taskJson);
                for (int i = 0; i < arr.length(); i++) {
                    tasksList.add(Task.fromJSONObject(arr.getJSONObject(i)));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void saveSubjects() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        JSONArray arr = new JSONArray();
        try {
            for (int i = 0; i < subjectsList.size(); i++) {
                arr.put(subjectsList.get(i).toJSONObject());
            }
            editor.putString(KEY_SUBJECTS, arr.toString());
            editor.apply();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void saveTasks() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        JSONArray arr = new JSONArray();
        try {
            for (int i = 0; i < tasksList.size(); i++) {
                arr.put(tasksList.get(i).toJSONObject());
            }
            editor.putString(KEY_TASKS, arr.toString());
            editor.apply();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // Dynamic generation of subject items rows
    private void renderSubjects() {
        containerSubjects.removeAllViews();

        if (subjectsList.isEmpty()) {
            tvEmptySubjects.setVisibility(View.VISIBLE);
            return;
        }
        tvEmptySubjects.setVisibility(View.GONE);

        LayoutInflater inflater = LayoutInflater.from(this);

        for (int i = 0; i < subjectsList.size(); i++) {
            final Subject subject = subjectsList.get(i);
            View itemView = inflater.inflate(R.layout.item_subject, containerSubjects, false);

            View vColor = itemView.findViewById(R.id.v_subject_color);
            TextView tvName = (TextView) itemView.findViewById(R.id.tv_subject_name);
            TextView tvDelete = (TextView) itemView.findViewById(R.id.tv_delete_subject);
            TextView tvTasksCount = (TextView) itemView.findViewById(R.id.tv_subject_stat_tasks);
            TextView tvProgress = (TextView) itemView.findViewById(R.id.tv_subject_progress);

            // Shape the color marker
            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.OVAL);
            shape.setColor(subject.color);
            vColor.setBackground(shape);

            tvName.setText(subject.name);

            // Calculate metrics for this specific subject
            int total = 0;
            int completed = 0;
            for (int k = 0; k < tasksList.size(); k++) {
                Task t = tasksList.get(k);
                if (t.subjectName.equals(subject.name)) {
                    total++;
                    if (t.completed) {
                        completed++;
                    }
                }
            }

            int pending = total - completed;
            int pct = total == 0 ? 0 : (completed * 100) / total;

            tvTasksCount.setText(pending + " pending tasks");
            tvProgress.setText(pct + "% complete");

            tvDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    confirmAndRemoveSubject(subject);
                }
            });

            containerSubjects.addView(itemView);
        }
    }

    private void confirmAndRemoveSubject(final Subject subject) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Remove Subject");
        builder.setMessage("Are you sure you want to remove '" + subject.name + "'? This will also remove any tasks assigned to it.");
        builder.setPositiveButton("Remove", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Delete linked tasks
                List<Task> toDelete = new ArrayList<>();
                for (int i = 0; i < tasksList.size(); i++) {
                    Task t = tasksList.get(i);
                    if (t.subjectName.equals(subject.name)) {
                        toDelete.add(t);
                    }
                }
                for (int i = 0; i < toDelete.size(); i++) {
                    cancelAlarmReminder(toDelete.get(i));
                    tasksList.remove(toDelete.get(i));
                }

                subjectsList.remove(subject);
                saveSubjects();
                saveTasks();

                renderSubjects();
                renderTasks();
                refreshDashboardAnalytics();
                Toast.makeText(MainActivity.this, "Subject removed", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    // Dynamic generation of tasks list items
    private void renderTasks() {
        containerTasks.removeAllViews();

        int selectedFilterIndex = spinnerTaskFilter.getSelectedItemPosition();

        List<Task> filteredList = new ArrayList<>();
        for (int i = 0; i < tasksList.size(); i++) {
            Task task = tasksList.get(i);
            if (selectedFilterIndex == 1 && task.completed) {
                continue; // Show only Active
            }
            if (selectedFilterIndex == 2 && !task.completed) {
                continue; // Show only Completed
            }
            filteredList.add(task);
        }

        if (filteredList.isEmpty()) {
            tvEmptyTasks.setVisibility(View.VISIBLE);
            return;
        }
        tvEmptyTasks.setVisibility(View.GONE);

        LayoutInflater inflater = LayoutInflater.from(this);

        for (int i = 0; i < filteredList.size(); i++) {
            final Task task = filteredList.get(i);
            View itemView = inflater.inflate(R.layout.item_task, containerTasks, false);

            CheckBox cbComplete = (CheckBox) itemView.findViewById(R.id.cb_task_complete);
            TextView tvTitle = (TextView) itemView.findViewById(R.id.tv_task_title);
            TextView tvSubjectTag = (TextView) itemView.findViewById(R.id.tv_task_subject_tag);
            TextView tvDue = (TextView) itemView.findViewById(R.id.tv_task_due);
            TextView tvReminderInfo = (TextView) itemView.findViewById(R.id.tv_task_reminder_info);
            TextView tvDelete = (TextView) itemView.findViewById(R.id.tv_delete_task);

            tvTitle.setText(task.title);
            tvSubjectTag.setText(task.subjectName);

            // Assign subject color tag background
            int tagColor = Color.parseColor("#757575");
            for (int k = 0; k < subjectsList.size(); k++) {
                if (subjectsList.get(k).name.equals(task.subjectName)) {
                    tagColor = subjectsList.get(k).color;
                    break;
                }
            }

            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.RECTANGLE);
            shape.setCornerRadius(6);
            shape.setColor(tagColor);
            tvSubjectTag.setBackground(shape);

            tvDue.setText("Due: " + task.dueDate);

            if (task.reminderEnabled && task.reminderTimestamp > System.currentTimeMillis()) {
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(task.reminderTimestamp);
                
                int hour = cal.get(Calendar.HOUR);
                int minute = cal.get(Calendar.MINUTE);
                String ampm = cal.get(Calendar.AM_PM) == Calendar.AM ? "AM" : "PM";
                if (hour == 0) hour = 12;

                String minStr = minute < 10 ? "0" + minute : String.valueOf(minute);
                tvReminderInfo.setText("⏰ Reminder: " + hour + ":" + minStr + " " + ampm);
                tvReminderInfo.setVisibility(View.VISIBLE);
            } else {
                tvReminderInfo.setVisibility(View.GONE);
            }

            cbComplete.setChecked(task.completed);
            if (task.completed) {
                tvTitle.setPaintFlags(tvTitle.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
                tvTitle.setTextColor(Color.parseColor("#9E9E9E"));
            } else {
                tvTitle.setPaintFlags(tvTitle.getPaintFlags() & (~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG));
                tvTitle.setTextColor(Color.parseColor("#212121"));
            }

            // Lock list elements during modification click tasks
            final CheckBox finalCb = cbComplete;
            cbComplete.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    task.completed = isChecked;
                    saveTasks();
                    
                    // Modify alert status on completion
                    if (isChecked) {
                        cancelAlarmReminder(task);
                    } else {
                        if (task.reminderEnabled && task.reminderTimestamp > System.currentTimeMillis()) {
                            scheduleAlarmReminder(task);
                        }
                    }

                    // Render list and views
                    renderTasks();
                    refreshDashboardAnalytics();
                }
            });

            tvDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    cancelAlarmReminder(task);
                    tasksList.remove(task);
                    saveTasks();
                    renderTasks();
                    refreshDashboardAnalytics();
                    Toast.makeText(MainActivity.this, "Task removed", Toast.LENGTH_SHORT).show();
                }
            });

            containerTasks.addView(itemView);
        }
    }

    // Modal view window dialog for adding a new academic subject item
    private void showAddSubjectDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_subject, null);
        builder.setView(dialogView);

        final EditText etName = (EditText) dialogView.findViewById(R.id.et_subject_name);
        
        final View opt1 = dialogView.findViewById(R.id.color_opt1);
        final View opt2 = dialogView.findViewById(R.id.color_opt2);
        final View opt3 = dialogView.findViewById(R.id.color_opt3);
        final View opt4 = dialogView.findViewById(R.id.color_opt4);
        final View opt5 = dialogView.findViewById(R.id.color_opt5);
        final View opt6 = dialogView.findViewById(R.id.color_opt6);

        // Pre-select default color opt2 (blue)
        selectedSubjectColor = Color.parseColor("#1E88E5");
        highlightColorSelection(opt1, opt2, opt3, opt4, opt5, opt6);

        opt1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedSubjectColor = Color.parseColor("#E53935");
                highlightColorSelection(opt1, opt2, opt3, opt4, opt5, opt6);
            }
        });
        opt2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedSubjectColor = Color.parseColor("#1E88E5");
                highlightColorSelection(opt1, opt2, opt3, opt4, opt5, opt6);
            }
        });
        opt3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedSubjectColor = Color.parseColor("#43A047");
                highlightColorSelection(opt1, opt2, opt3, opt4, opt5, opt6);
            }
        });
        opt4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedSubjectColor = Color.parseColor("#8E24AA");
                highlightColorSelection(opt1, opt2, opt3, opt4, opt5, opt6);
            }
        });
        opt5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedSubjectColor = Color.parseColor("#FDD835");
                highlightColorSelection(opt1, opt2, opt3, opt4, opt5, opt6);
            }
        });
        opt6.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedSubjectColor = Color.parseColor("#FB8C00");
                highlightColorSelection(opt1, opt2, opt3, opt4, opt5, opt6);
            }
        });

        builder.setPositiveButton("Create", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String name = etName.getText().toString().trim();
                if (name.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please enter a valid name", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Check duplicate subject name
                for (int i = 0; i < subjectsList.size(); i++) {
                    if (subjectsList.get(i).name.equalsIgnoreCase(name)) {
                        Toast.makeText(MainActivity.this, "This subject already exists", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                Subject newSubject = new Subject(name, selectedSubjectColor);
                subjectsList.add(newSubject);
                saveSubjects();
                renderSubjects();
                Toast.makeText(MainActivity.this, "Subject '" + name + "' created!", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void highlightColorSelection(View o1, View o2, View o3, View o4, View o5, View o6) {
        // Redraw rounded shape selections
        o1.setAlpha(selectedSubjectColor == Color.parseColor("#E53935") ? 1.0f : 0.4f);
        o2.setAlpha(selectedSubjectColor == Color.parseColor("#1E88E5") ? 1.0f : 0.4f);
        o3.setAlpha(selectedSubjectColor == Color.parseColor("#43A047") ? 1.0f : 0.4f);
        o4.setAlpha(selectedSubjectColor == Color.parseColor("#8E24AA") ? 1.0f : 0.4f);
        o5.setAlpha(selectedSubjectColor == Color.parseColor("#FDD835") ? 1.0f : 0.4f);
        o6.setAlpha(selectedSubjectColor == Color.parseColor("#FB8C00") ? 1.0f : 0.4f);
    }

    // Dialog creation workflow for building task item objects
    private void showAddTaskDialog() {
        if (subjectsList.isEmpty()) {
            Toast.makeText(this, "Please create at least one Subject first!", Toast.LENGTH_LONG).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_task, null);
        builder.setView(dialogView);

        final Spinner spinnerSubject = (Spinner) dialogView.findViewById(R.id.spinner_task_subject);
        final EditText etTitle = (EditText) dialogView.findViewById(R.id.et_task_title);
        final EditText etDue = (EditText) dialogView.findViewById(R.id.et_task_due);
        final CheckBox cbReminder = (CheckBox) dialogView.findViewById(R.id.cb_reminder_enabled);
        final LinearLayout layoutReminderTime = (LinearLayout) dialogView.findViewById(R.id.layout_reminder_time);
        final EditText etTime = (EditText) dialogView.findViewById(R.id.et_task_time);

        // Populate dynamic dialog spinner subjects choices
        List<String> subNames = new ArrayList<>();
        for (int i = 0; i < subjectsList.size(); i++) {
            subNames.add(subjectsList.get(i).name);
        }
        ArrayAdapter<String> subAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, subNames);
        subAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSubject.setAdapter(subAdapter);

        // Date selection popup modal triggers
        etDue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Calendar c = Calendar.getInstance();
                int year = c.get(Calendar.YEAR);
                int month = c.get(Calendar.MONTH);
                int day = c.get(Calendar.DAY_OF_MONTH);

                DatePickerDialog dpd = new DatePickerDialog(MainActivity.this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int yr, int mth, int dy) {
                        calendarReminderTime.set(Calendar.YEAR, yr);
                        calendarReminderTime.set(Calendar.MONTH, mth);
                        calendarReminderTime.set(Calendar.DAY_OF_MONTH, dy);

                        String[] monthNames = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
                        etDue.setText(dy + " " + monthNames[mth]);
                    }
                }, year, month, day);
                dpd.show();
            }
        });

        // Interactive toggle display layout details corresponding with options check selectors
        cbReminder.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                layoutReminderTime.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            }
        });

        // Time selector events trigger picker dialog logic
        etTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Calendar c = Calendar.getInstance();
                int hour = c.get(Calendar.HOUR_OF_DAY);
                int minute = c.get(Calendar.MINUTE);

                TimePickerDialog tpd = new TimePickerDialog(MainActivity.this, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hr, int min) {
                        calendarReminderTime.set(Calendar.HOUR_OF_DAY, hr);
                        calendarReminderTime.set(Calendar.MINUTE, min);
                        calendarReminderTime.set(Calendar.SECOND, 0);

                        int displayHour = hr > 12 ? hr - 12 : hr;
                        if (displayHour == 0) displayHour = 12;
                        String displayMin = min < 10 ? "0" + min : String.valueOf(min);
                        String ampm = hr >= 12 ? "PM" : "AM";

                        etTime.setText(displayHour + ":" + displayMin + " " + ampm);
                    }
                }, hour, minute, false);
                tpd.show();
            }
        });

        builder.setPositiveButton("Create Task", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String title = etTitle.getText().toString().trim();
                String due = etDue.getText().toString().trim();
                String selSubName = spinnerSubject.getSelectedItem().toString();

                if (title.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please insert a valid Title", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (due.isEmpty()) {
                    due = "Today";
                }

                boolean alarmAlert = cbReminder.isChecked();
                long timestamp = 0;

                if (alarmAlert) {
                    if (etTime.getText().toString().isEmpty()) {
                        Toast.makeText(MainActivity.this, "Please select an Alert Time", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    timestamp = calendarReminderTime.getTimeInMillis();
                    if (timestamp < System.currentTimeMillis()) {
                        Toast.makeText(MainActivity.this, "Warning: Time set in past! Reminder trigger is scheduled for tomorrow.", Toast.LENGTH_LONG).show();
                        calendarReminderTime.add(Calendar.DAY_OF_YEAR, 1);
                        timestamp = calendarReminderTime.getTimeInMillis();
                    }
                }

                long uniqueId = System.currentTimeMillis();
                Task newTask = new Task(uniqueId, title, selSubName, due, false, alarmAlert, timestamp);
                tasksList.add(newTask);
                saveTasks();

                if (alarmAlert) {
                    scheduleAlarmReminder(newTask);
                }

                renderTasks();
                refreshDashboardAnalytics();
                Toast.makeText(MainActivity.this, "Task created!", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    // Schedule active AlarmManager intents pointing directly at broadcast receiver class
    private void scheduleAlarmReminder(Task task) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent alarmIntent = new Intent(this, ReminderReceiver.class);
        alarmIntent.putExtra("TASK_TITLE", "Upcoming Study Task: " + task.title + " (" + task.subjectName + ")");
        
        PendingIntent pendingIntent;
        int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE : 
                PendingIntent.FLAG_UPDATE_CURRENT;

        pendingIntent = PendingIntent.getBroadcast(this, (int) task.id, alarmIntent, flags);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, task.reminderTimestamp, pendingIntent);
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, task.reminderTimestamp, pendingIntent);
        }
    }

    private void cancelAlarmReminder(Task task) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent alarmIntent = new Intent(this, ReminderReceiver.class);
        
        PendingIntent pendingIntent;
        int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? 
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE : 
                PendingIntent.FLAG_NO_CREATE;

        pendingIntent = PendingIntent.getBroadcast(this, (int) task.id, alarmIntent, flags);

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }
    }
}