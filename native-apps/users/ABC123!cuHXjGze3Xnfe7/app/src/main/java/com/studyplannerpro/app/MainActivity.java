package com.studyplannerpro.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.Gravity;
import android.view.LayoutInflater;
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
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {

    private DatabaseHelper dbHelper;

    private View layoutDashboard;
    private View layoutSubjects;
    private View layoutTasks;
    private View layoutFocus;

    private Button btnTabDashboard;
    private Button btnTabSubjects;
    private Button btnTabTasks;
    private Button btnTabFocus;

    private TextView tvDashboardStudyTime;
    private TextView tvDashboardTasks;
    private ProgressBar pbOverallTasks;
    private TextView tvProgressPercent;
    private LinearLayout layoutDashboardBreakdown;

    private ListView listSubjects;
    private ListView listTasks;
    private Spinner spinnerFilterStatus;

    private Spinner spinnerFocusSubject;
    private TextView tvFocusTimerClock;
    private TextView tvFocusStatus;
    private Button btnTimerStart;
    private Button btnTimerReset;
    private Button btnPreset15, btnPreset25, btnPreset45, btnPreset60;

    private CountDownTimer focusTimer;
    private boolean timerRunning = false;
    private long timeLeftInMillis = 1500000; 
    private long selectedFocusDurationMinutes = 25;
    private long focusSubjectId = -1;

    public static class SubjectItem {
        long id;
        String name;
        int color;

        public SubjectItem(long id, String name, int color) {
            this.id = id;
            this.name = name;
            this.color = color;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public static class TaskItem {
        long id;
        long subjectId;
        String title;
        String desc;
        String dueDate;
        int status;
        String priority;
        String reminder;
        String subjectName;
        int subjectColor;

        public TaskItem(long id, long subjectId, String title, String desc, String dueDate, int status, String priority, String reminder, String subjectName, int subjectColor) {
            this.id = id;
            this.subjectId = subjectId;
            this.title = title;
            this.desc = desc;
            this.dueDate = dueDate;
            this.status = status;
            this.priority = priority;
            this.reminder = reminder;
            this.subjectName = subjectName;
            this.subjectColor = subjectColor;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        Cursor cursor = dbHelper.getAllSubjects();
        if (cursor != null) {
            if (cursor.getCount() == 0) {
                dbHelper.insertSubject("Mathematics", 0xFF3B82F6); 
                dbHelper.insertSubject("Science & Biology", 0xFF10B981); 
                dbHelper.insertSubject("World History", 0xFFEF4444); 
                dbHelper.insertSubject("Computer Science", 0xFF8B5CF6); 
            }
            cursor.close();
        }

        initViews();
        setupNavigation();
        setupFilters();
        setupFocusTimer();

        switchTab(0);
    }

    private void initViews() {
        layoutDashboard = findViewById(R.id.layout_dashboard);
        layoutSubjects = findViewById(R.id.layout_subjects);
        layoutTasks = findViewById(R.id.layout_tasks);
        layoutFocus = findViewById(R.id.layout_focus);

        btnTabDashboard = (Button) findViewById(R.id.btn_tab_dashboard);
        btnTabSubjects = (Button) findViewById(R.id.btn_tab_subjects);
        btnTabTasks = (Button) findViewById(R.id.btn_tab_tasks);
        btnTabFocus = (Button) findViewById(R.id.btn_tab_focus);

        tvDashboardStudyTime = (TextView) findViewById(R.id.tv_dashboard_study_time);
        tvDashboardTasks = (TextView) findViewById(R.id.tv_dashboard_tasks);
        pbOverallTasks = (ProgressBar) findViewById(R.id.pb_overall_tasks);
        tvProgressPercent = (TextView) findViewById(R.id.tv_progress_percent);
        layoutDashboardBreakdown = (LinearLayout) findViewById(R.id.layout_dashboard_breakdown);

        listSubjects = (ListView) findViewById(R.id.list_subjects);
        listTasks = (ListView) findViewById(R.id.list_tasks);
        spinnerFilterStatus = (Spinner) findViewById(R.id.spinner_filter_status);

        spinnerFocusSubject = (Spinner) findViewById(R.id.spinner_focus_subject);
        tvFocusTimerClock = (TextView) findViewById(R.id.tv_focus_timer_clock);
        tvFocusStatus = (TextView) findViewById(R.id.tv_focus_status);
        btnTimerStart = (Button) findViewById(R.id.btn_timer_start);
        btnTimerReset = (Button) findViewById(R.id.btn_timer_reset);
        
        btnPreset15 = (Button) findViewById(R.id.btn_preset_15);
        btnPreset25 = (Button) findViewById(R.id.btn_preset_25);
        btnPreset45 = (Button) findViewById(R.id.btn_preset_45);
        btnPreset60 = (Button) findViewById(R.id.btn_preset_60);

        findViewById(R.id.btn_add_subject_dialog).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddSubjectDialog();
            }
        });

        findViewById(R.id.btn_add_task_dialog).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddTaskDialog();
            }
        });
    }

    private void setupNavigation() {
        btnTabDashboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(0);
            }
        });
        btnTabSubjects.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(1);
            }
        });
        btnTabTasks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(2);
            }
        });
        btnTabFocus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(3);
            }
        });
    }

    private void switchTab(int tabIndex) {
        layoutDashboard.setVisibility(View.GONE);
        layoutSubjects.setVisibility(View.GONE);
        layoutTasks.setVisibility(View.GONE);
        layoutFocus.setVisibility(View.GONE);

        btnTabDashboard.setBackgroundColor(0xFFFFFFFF);
        btnTabSubjects.setBackgroundColor(0xFFFFFFFF);
        btnTabTasks.setBackgroundColor(0xFFFFFFFF);
        btnTabFocus.setBackgroundColor(0xFFFFFFFF);

        btnTabDashboard.setTextColor(0xFF64748B);
        btnTabSubjects.setTextColor(0xFF64748B);
        btnTabTasks.setTextColor(0xFF64748B);
        btnTabFocus.setTextColor(0xFF64748B);

        switch (tabIndex) {
            case 0:
                layoutDashboard.setVisibility(View.VISIBLE);
                btnTabDashboard.setBackgroundColor(0xFFEEF2FF);
                btnTabDashboard.setTextColor(0xFF4F46E5);
                loadDashboardData();
                break;
            case 1:
                layoutSubjects.setVisibility(View.VISIBLE);
                btnTabSubjects.setBackgroundColor(0xFFEEF2FF);
                btnTabSubjects.setTextColor(0xFF4F46E5);
                loadSubjectsData();
                break;
            case 2:
                layoutTasks.setVisibility(View.VISIBLE);
                btnTabTasks.setBackgroundColor(0xFFEEF2FF);
                btnTabTasks.setTextColor(0xFF4F46E5);
                loadTasksData();
                break;
            case 3:
                layoutFocus.setVisibility(View.VISIBLE);
                btnTabFocus.setBackgroundColor(0xFFEEF2FF);
                btnTabFocus.setTextColor(0xFF4F46E5);
                loadFocusData();
                break;
        }
    }

    private void setupFilters() {
        List<String> statuses = new ArrayList<>();
        statuses.add("All");
        statuses.add("Pending");
        statuses.add("Completed");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, statuses);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilterStatus.setAdapter(adapter);

        spinnerFilterStatus.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadTasksData();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void loadDashboardData() {
        int totalMin = dbHelper.getTotalStudyMinutes();
        tvDashboardStudyTime.setText(totalMin + " mins");

        int totalTasks = dbHelper.getTotalTasksCount();
        int doneTasks = dbHelper.getTaskCountByStatus(1);
        if (totalTasks > 0) {
            int percent = (doneTasks * 100) / totalTasks;
            tvDashboardTasks.setText(doneTasks + "/" + totalTasks + " (" + percent + "%)");
            pbOverallTasks.setProgress(percent);
            tvProgressPercent.setText(percent + "% Completed");
        } else {
            tvDashboardTasks.setText("0/0 (0%)");
            pbOverallTasks.setProgress(0);
            tvProgressPercent.setText("No Tasks Added yet");
        }

        layoutDashboardBreakdown.removeAllViews();
        List<Map<String, Object>> subjectBreakdowns = dbHelper.getSubjectBreakdown();
        for (int i = 0; i < subjectBreakdowns.size(); i++) {
            Map<String, Object> subMap = subjectBreakdowns.get(i);
            String name = (String) subMap.get("name");
            int color = (Integer) subMap.get("color");
            int minutes = (Integer) subMap.get("minutes");
            int done = (Integer) subMap.get("done_tasks");
            int total = (Integer) subMap.get("total_tasks");

            LinearLayout itemCard = new LinearLayout(this);
            itemCard.setOrientation(LinearLayout.VERTICAL);
            itemCard.setBackgroundResource(R.drawable.card_bg);
            
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 0, 16);
            itemCard.setLayoutParams(params);
            itemCard.setPadding(32, 32, 32, 32);

            LinearLayout headerRow = new LinearLayout(this);
            headerRow.setOrientation(LinearLayout.HORIZONTAL);
            headerRow.setGravity(Gravity.CENTER_VERTICAL);

            View colorDot = new View(this);
            LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(24, 24);
            dotParams.rightMargin = 16;
            colorDot.setLayoutParams(dotParams);
            colorDot.setBackgroundColor(color);

            TextView tvSubName = new TextView(this);
            tvSubName.setText(name);
            tvSubName.setTextSize(16);
            tvSubName.setTextColor(0xFF1E293B);
            tvSubName.setTypeface(null, Typeface.BOLD);

            headerRow.addView(colorDot);
            headerRow.addView(tvSubName);

            LinearLayout statsRow = new LinearLayout(this);
            statsRow.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams statsParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            statsParams.topMargin = 16;
            statsRow.setLayoutParams(statsParams);

            TextView tvTime = new TextView(this);
            tvTime.setText("Logged Study: " + minutes + " mins");
            tvTime.setTextColor(0xFF64748B);
            tvTime.setTextSize(13);
            
            LinearLayout.LayoutParams timeParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
            tvTime.setLayoutParams(timeParams);

            TextView tvTaskStats = new TextView(this);
            tvTaskStats.setText("Tasks: " + done + "/" + total);
            tvTaskStats.setTextColor(0xFF64748B);
            tvTaskStats.setTextSize(13);
            tvTaskStats.setGravity(Gravity.END);
            
            LinearLayout.LayoutParams taskParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
            tvTaskStats.setLayoutParams(taskParams);

            statsRow.addView(tvTime);
            statsRow.addView(tvTaskStats);

            itemCard.addView(headerRow);
            itemCard.addView(statsRow);

            layoutDashboardBreakdown.addView(itemCard);
        }
    }

    private void loadSubjectsData() {
        List<SubjectItem> subjects = new ArrayList<>();
        Cursor cursor = dbHelper.getAllSubjects();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                int color = cursor.getInt(cursor.getColumnIndexOrThrow("color"));
                subjects.add(new SubjectItem(id, name, color));
            }
            cursor.close();
        }

        SubjectAdapter adapter = new SubjectAdapter(subjects);
        listSubjects.setAdapter(adapter);
    }

    private void loadTasksData() {
        String filter = spinnerFilterStatus.getSelectedItem() != null ? spinnerFilterStatus.getSelectedItem().toString() : "All";
        List<TaskItem> tasks = new ArrayList<>();
        Cursor cursor = dbHelper.getTasksFiltered(filter);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                long subjectId = cursor.getLong(cursor.getColumnIndexOrThrow("subject_id"));
                String title = cursor.getString(cursor.getColumnIndexOrThrow("title"));
                String desc = cursor.getString(cursor.getColumnIndexOrThrow("description"));
                String due = cursor.getString(cursor.getColumnIndexOrThrow("due_date"));
                int status = cursor.getInt(cursor.getColumnIndexOrThrow("status"));
                String priority = cursor.getString(cursor.getColumnIndexOrThrow("priority"));
                String reminder = cursor.getString(cursor.getColumnIndexOrThrow("reminder_time"));
                String subName = cursor.getString(cursor.getColumnIndexOrThrow("subject_name"));
                int subColor = cursor.getInt(cursor.getColumnIndexOrThrow("subject_color"));

                tasks.add(new TaskItem(id, subjectId, title, desc, due, status, priority, reminder, subName, subColor));
            }
            cursor.close();
        }

        TaskAdapter adapter = new TaskAdapter(tasks);
        listTasks.setAdapter(adapter);
    }

    private void loadFocusData() {
        List<SubjectItem> subjects = new ArrayList<>();
        Cursor cursor = dbHelper.getAllSubjects();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                int color = cursor.getInt(cursor.getColumnIndexOrThrow("color"));
                subjects.add(new SubjectItem(id, name, color));
            }
            cursor.close();
        }

        ArrayAdapter<SubjectItem> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, subjects);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFocusSubject.setAdapter(adapter);

        if (!subjects.isEmpty()) {
            focusSubjectId = subjects.get(0).id;
        } else {
            focusSubjectId = -1;
        }

        spinnerFocusSubject.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                SubjectItem item = (SubjectItem) parent.getSelectedItem();
                focusSubjectId = item.id;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setupFocusTimer() {
        updateTimerText();

        btnPreset15.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setPresetTime(15);
            }
        });
        btnPreset25.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setPresetTime(25);
            }
        });
        btnPreset45.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setPresetTime(45);
            }
        });
        btnPreset60.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setPresetTime(60);
            }
        });

        btnTimerStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startFocusTimer();
            }
        });

        btnTimerReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetFocusTimer();
            }
        });
    }

    private void setPresetTime(int minutes) {
        if (timerRunning) {
            focusTimer.cancel();
            timerRunning = false;
            btnTimerStart.setText("START");
        }
        selectedFocusDurationMinutes = minutes;
        timeLeftInMillis = minutes * 60 * 1000;
        updateTimerText();
        tvFocusStatus.setText("Interval set: " + minutes + " Min");
    }

    private void startFocusTimer() {
        if (timerRunning) {
            focusTimer.cancel();
            timerRunning = false;
            btnTimerStart.setText("START");
            tvFocusStatus.setText("Focus Session Paused");
        } else {
            if (focusSubjectId == -1) {
                Toast.makeText(this, "Please create and select a subject first!", Toast.LENGTH_SHORT).show();
                return;
            }

            timerRunning = true;
            btnTimerStart.setText("PAUSE");
            tvFocusStatus.setText("Focusing Now...");

            focusTimer = new CountDownTimer(timeLeftInMillis, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    timeLeftInMillis = millisUntilFinished;
                    updateTimerText();
                }

                @Override
                public void onFinish() {
                    timerRunning = false;
                    btnTimerStart.setText("START");
                    tvFocusStatus.setText("Session Finished!");

                    int completedMinutes = (int) selectedFocusDurationMinutes;
                    if (completedMinutes <= 0) completedMinutes = 1;

                    String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                    dbHelper.insertStudySession(focusSubjectId, completedMinutes, currentDate);

                    try {
                        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                        Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
                        r.play();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    Toast.makeText(MainActivity.this, "Great Job! You focused for " + completedMinutes + " minutes!", Toast.LENGTH_LONG).show();

                    timeLeftInMillis = selectedFocusDurationMinutes * 60 * 1000;
                    updateTimerText();
                    loadDashboardData();
                }
            }.start();
        }
    }

    private void resetFocusTimer() {
        if (focusTimer != null) {
            focusTimer.cancel();
        }
        timerRunning = false;
        timeLeftInMillis = selectedFocusDurationMinutes * 60 * 1000;
        updateTimerText();
        btnTimerStart.setText("START");
        tvFocusStatus.setText("Session Reset");
    }

    private void updateTimerText() {
        int minutes = (int) (timeLeftInMillis / 1000) / 60;
        int seconds = (int) (timeLeftInMillis / 1000) % 60;
        String timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        tvFocusTimerClock.setText(timeFormatted);
    }

    private void showAddSubjectDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_subject, null);
        builder.setView(dialogView);

        final EditText etSubjectName = (EditText) dialogView.findViewById(R.id.et_subject_name);
        final RadioButton rbBlue = (RadioButton) dialogView.findViewById(R.id.rb_color_blue);
        final RadioButton rbRed = (RadioButton) dialogView.findViewById(R.id.rb_color_red);
        final RadioButton rbGreen = (RadioButton) dialogView.findViewById(R.id.rb_color_green);
        final RadioButton rbOrange = (RadioButton) dialogView.findViewById(R.id.rb_color_orange);
        final RadioButton rbPurple = (RadioButton) dialogView.findViewById(R.id.rb_color_purple);

        Button btnCancel = (Button) dialogView.findViewById(R.id.btn_cancel_subject);
        Button btnSave = (Button) dialogView.findViewById(R.id.btn_save_subject);

        final AlertDialog dialog = builder.create();

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = etSubjectName.getText().toString().trim();
                if (name.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please enter a subject name", Toast.LENGTH_SHORT).show();
                    return;
                }

                int color = 0xFF3B82F6; // default blue
                if (rbRed.isChecked()) color = 0xFFEF4444;
                else if (rbGreen.isChecked()) color = 0xFF10B981;
                else if (rbOrange.isChecked()) color = 0xFFF59E0B;
                else if (rbPurple.isChecked()) color = 0xFF8B5CF6;

                dbHelper.insertSubject(name, color);
                Toast.makeText(MainActivity.this, "Subject added successfully", Toast.LENGTH_SHORT).show();
                loadSubjectsData();
                loadDashboardData();
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void showAddTaskDialog() {
        List<SubjectItem> subjects = new ArrayList<>();
        Cursor cursor = dbHelper.getAllSubjects();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                int color = cursor.getInt(cursor.getColumnIndexOrThrow("color"));
                subjects.add(new SubjectItem(id, name, color));
            }
            cursor.close();
        }

        if (subjects.isEmpty()) {
            Toast.makeText(this, "Please create at least one subject first!", Toast.LENGTH_LONG).show();
            switchTab(1);
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_task, null);
        builder.setView(dialogView);

        final EditText etTaskTitle = (EditText) dialogView.findViewById(R.id.et_task_title);
        final EditText etTaskDesc = (EditText) dialogView.findViewById(R.id.et_task_desc);
        final Spinner spinnerSubject = (Spinner) dialogView.findViewById(R.id.spinner_task_subject);
        final Spinner spinnerPriority = (Spinner) dialogView.findViewById(R.id.spinner_task_priority);
        final Button btnSelectDueDate = (Button) dialogView.findViewById(R.id.btn_select_due_date);
        final CheckBox cbSetReminder = (CheckBox) dialogView.findViewById(R.id.cb_set_reminder);

        Button btnCancel = (Button) dialogView.findViewById(R.id.btn_cancel_task);
        Button btnSave = (Button) dialogView.findViewById(R.id.btn_save_task);

        ArrayAdapter<SubjectItem> subAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, subjects);
        subAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSubject.setAdapter(subAdapter);

        final List<String> priorities = new ArrayList<>();
        priorities.add("LOW");
        priorities.add("MEDIUM");
        priorities.add("HIGH");
        ArrayAdapter<String> prioAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, priorities);
        prioAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPriority.setAdapter(prioAdapter);

        final Calendar calendar = Calendar.getInstance();
        final String[] dueDateStr = {new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.getTime())};
        btnSelectDueDate.setText(dueDateStr[0]);

        btnSelectDueDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int year = calendar.get(Calendar.YEAR);
                int month = calendar.get(Calendar.MONTH);
                int day = calendar.get(Calendar.DAY_OF_MONTH);

                DatePickerDialog datePickerDialog = new DatePickerDialog(MainActivity.this,
                        new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                                dueDateStr[0] = year + "-" + String.format(Locale.getDefault(), "%02d", (monthOfYear + 1)) + "-" + String.format(Locale.getDefault(), "%02d", dayOfMonth);
                                btnSelectDueDate.setText(dueDateStr[0]);
                            }
                        }, year, month, day);
                datePickerDialog.show();
            }
        });

        final AlertDialog dialog = builder.create();

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String title = etTaskTitle.getText().toString().trim();
                String desc = etTaskDesc.getText().toString().trim();
                if (title.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please enter a task title", Toast.LENGTH_SHORT).show();
                    return;
                }

                SubjectItem selectedSubject = (SubjectItem) spinnerSubject.getSelectedItem();
                String priority = spinnerPriority.getSelectedItem().toString();

                dbHelper.insertTask(selectedSubject.id, title, desc, dueDateStr[0], priority, cbSetReminder.isChecked() ? "TEST_ALARM" : "NONE");

                if (cbSetReminder.isChecked()) {
                    scheduleTestAlarm(title);
                }

                Toast.makeText(MainActivity.this, "Task created successfully!", Toast.LENGTH_SHORT).show();
                loadTasksData();
                loadDashboardData();
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void scheduleTestAlarm(String taskTitle) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, ReminderReceiver.class);
        intent.putExtra("title", "Study Session Pending: " + taskTitle);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this, 
                (int) System.currentTimeMillis(), 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        long triggerTime = System.currentTimeMillis() + 10000; // 10 seconds from now
        if (alarmManager != null) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        }
        Toast.makeText(this, "Test alarm scheduled in 10 seconds!", Toast.LENGTH_LONG).show();
    }

    private class SubjectAdapter extends BaseAdapter {
        private List<SubjectItem> list;

        public SubjectAdapter(List<SubjectItem> list) {
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
            return list.get(position).id;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(MainActivity.this).inflate(R.layout.item_subject, parent, false);
            }

            final SubjectItem item = list.get(position);

            View colorView = convertView.findViewById(R.id.view_subject_color);
            TextView nameView = convertView.findViewById(R.id.tv_subject_name);
            Button deleteBtn = convertView.findViewById(R.id.btn_delete_subject);

            colorView.setBackgroundColor(item.color);
            nameView.setText(item.name);

            deleteBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Delete Subject")
                            .setMessage("Are you sure? All relative tasks and logged focus logs will be completely deleted.")
                            .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dbHelper.deleteSubject(item.id);
                                    loadSubjectsData();
                                    loadDashboardData();
                                }
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                }
            });

            return convertView;
        }
    }

    private class TaskAdapter extends BaseAdapter {
        private List<TaskItem> list;

        public TaskAdapter(List<TaskItem> list) {
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
            return list.get(position).id;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(MainActivity.this).inflate(R.layout.item_task, parent, false);
            }

            final TaskItem item = list.get(position);

            CheckBox cbStatus = (CheckBox) convertView.findViewById(R.id.cb_task_status);
            TextView tvTitle = (TextView) convertView.findViewById(R.id.tv_task_title);
            TextView tvDesc = (TextView) convertView.findViewById(R.id.tv_task_desc);
            View viewSubColor = convertView.findViewById(R.id.view_task_sub_color);
            TextView tvSubName = (TextView) convertView.findViewById(R.id.tv_task_subject);
            TextView tvPriority = (TextView) convertView.findViewById(R.id.tv_task_priority);
            TextView tvDue = (TextView) convertView.findViewById(R.id.tv_task_due);
            Button btnDelete = (Button) convertView.findViewById(R.id.btn_delete_task);

            cbStatus.setOnCheckedChangeListener(null);
            cbStatus.setChecked(item.status == 1);

            tvTitle.setText(item.title);
            if (item.status == 1) {
                tvTitle.setPaintFlags(tvTitle.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                tvTitle.setPaintFlags(tvTitle.getPaintFlags() & (~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG));
            }

            tvDesc.setText(item.desc != null ? item.desc : "");
            viewSubColor.setBackgroundColor(item.subjectColor);
            tvSubName.setText(item.subjectName);
            tvPriority.setText(item.priority);
            tvDue.setText("Due: " + item.dueDate);

            if ("HIGH".equals(item.priority)) {
                tvPriority.setTextColor(0xFFEF4444);
            } else if ("MEDIUM".equals(item.priority)) {
                tvPriority.setTextColor(0xFFF59E0B);
            } else {
                tvPriority.setTextColor(0xFF10B981);
            }

            cbStatus.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    dbHelper.updateTaskStatus(item.id, isChecked ? 1 : 0);
                    loadTasksData();
                    loadDashboardData();
                }
            });

            btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dbHelper.deleteTask(item.id);
                    loadTasksData();
                    loadDashboardData();
                }
            });

            return convertView;
        }
    }
}