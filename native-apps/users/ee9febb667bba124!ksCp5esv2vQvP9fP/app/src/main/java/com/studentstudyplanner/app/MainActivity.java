package com.studentstudyplanner.app;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private DatabaseHelper dbHelper;
    private String selectedColor = "#007AFF"; // Default color choice
    private String selectedDueDate = "";
    private int selectedFilter = 1; // 0 = All, 1 = Pending, 2 = Completed

    // Screen Navigation layouts
    private View layoutDashboard;
    private View layoutSubjects;
    private View layoutTasks;
    private View layoutStats;

    // Bottom Navigation items
    private LinearLayout tabDashboard;
    private LinearLayout tabSubjects;
    private LinearLayout tabTasks;
    private LinearLayout tabStats;

    // Dashboard UI items
    private ProgressBar dashboardProgressBar;
    private TextView dashboardProgressText;
    private LinearLayout dashboardTodayContainer;

    // Subject screen fields
    private EditText inputSubjectName;
    private LinearLayout subjectsListContainer;

    // Tasks Screen components
    private EditText inputTaskTitle;
    private EditText inputTaskDesc;
    private Spinner spinnerTaskSubject;
    private Spinner spinnerTaskPriority;
    private Button btnPickDate;
    private LinearLayout tasksListContainer;

    // Analytics widgets
    private TextView txtStatTotalSubj;
    private TextView txtStatTotalTasks;
    private TextView txtStatCompletedTasks;
    private LinearLayout statsSubjectsContainer;

    // Subject lists
    private List<Long> subjectIdsList = new ArrayList<>();
    private List<String> subjectNamesList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);

        // Request notification permissions for API 33+
        if (Build.VERSION.SDK_INT >= 33) {
            try {
                requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 101);
            } catch (Exception e) {
                // older runtime compatibility fallback
            }
        }

        initializeLayouts();
        setupNavigation();
        setupSubjectActions();
        setupTaskActions();
        
        // Default loaded view state
        selectTab(0);
    }

    private void initializeLayouts() {
        layoutDashboard = findViewById(R.id.layout_dashboard);
        layoutSubjects = findViewById(R.id.layout_subjects);
        layoutTasks = findViewById(R.id.layout_tasks);
        layoutStats = findViewById(R.id.layout_stats);

        tabDashboard = (LinearLayout) findViewById(R.id.tab_dashboard);
        tabSubjects = (LinearLayout) findViewById(R.id.tab_subjects);
        tabTasks = (LinearLayout) findViewById(R.id.tab_tasks);
        tabStats = (LinearLayout) findViewById(R.id.tab_stats);

        dashboardProgressBar = (ProgressBar) findViewById(R.id.dashboard_progress_bar);
        dashboardProgressText = (TextView) findViewById(R.id.dashboard_progress_text);
        dashboardTodayContainer = (LinearLayout) findViewById(R.id.dashboard_today_container);

        inputSubjectName = (EditText) findViewById(R.id.input_subject_name);
        subjectsListContainer = (LinearLayout) findViewById(R.id.subjects_list_container);

        inputTaskTitle = (EditText) findViewById(R.id.input_task_title);
        inputTaskDesc = (EditText) findViewById(R.id.input_task_desc);
        spinnerTaskSubject = (Spinner) findViewById(R.id.spinner_task_subject);
        spinnerTaskPriority = (Spinner) findViewById(R.id.spinner_task_priority);
        btnPickDate = (Button) findViewById(R.id.btn_pick_date);
        tasksListContainer = (LinearLayout) findViewById(R.id.tasks_list_container);

        txtStatTotalSubj = (TextView) findViewById(R.id.txt_stat_total_subj);
        txtStatTotalTasks = (TextView) findViewById(R.id.txt_stat_total_tasks);
        txtStatCompletedTasks = (TextView) findViewById(R.id.txt_stat_completed_tasks);
        statsSubjectsContainer = (LinearLayout) findViewById(R.id.stats_subjects_container);

        // Populate task priority dropdown items
        String[] priorities = {"High 🔴", "Medium 🟡", "Low 🟢"};
        ArrayAdapter<String> priorityAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, priorities);
        spinnerTaskPriority.setAdapter(priorityAdapter);
    }

    private void setupNavigation() {
        tabDashboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectTab(0);
            }
        });

        tabSubjects.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectTab(1);
            }
        });

        tabTasks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectTab(2);
            }
        });

        tabStats.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectTab(3);
            }
        });

        // Dashboard Quick action hooks
        findViewById(R.id.dash_btn_add_task).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectTab(2);
            }
        });

        findViewById(R.id.dash_btn_add_subj).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectTab(1);
            }
        });
    }

    private void selectTab(int index) {
        layoutDashboard.setVisibility(index == 0 ? View.VISIBLE : View.GONE);
        layoutSubjects.setVisibility(index == 1 ? View.VISIBLE : View.GONE);
        layoutTasks.setVisibility(index == 2 ? View.VISIBLE : View.GONE);
        layoutStats.setVisibility(index == 3 ? View.VISIBLE : View.GONE);

        // Toggle background highlight state style visually
        updateTabBackground(tabDashboard, index == 0);
        updateTabBackground(tabSubjects, index == 1);
        updateTabBackground(tabTasks, index == 2);
        updateTabBackground(tabStats, index == 3);

        if (index == 0) refreshDashboard();
        if (index == 1) refreshSubjects();
        if (index == 2) {
            loadSubjectSpinner();
            refreshTasks();
        }
        if (index == 3) refreshStats();
    }

    private void updateTabBackground(LinearLayout tabLayout, boolean isActive) {
        TextView label = (TextView) tabLayout.getChildAt(1);
        if (isActive) {
            tabLayout.setBackgroundColor(Color.parseColor("#F1F5F9"));
            label.setTextColor(Color.parseColor("#007AFF"));
        } else {
            tabLayout.setBackgroundColor(Color.TRANSPARENT);
            label.setTextColor(Color.parseColor("#64748B"));
        }
    }

    // --- SUBJECT ARCHITECTURES ---
    private void setupSubjectActions() {
        // Handle Color Pickers custom setup
        findViewById(R.id.color_pick_red).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { selectedColor = "#FF3B30"; resetColorPickerBorders(v); }
        });
        findViewById(R.id.color_pick_green).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { selectedColor = "#34C759"; resetColorPickerBorders(v); }
        });
        findViewById(R.id.color_pick_blue).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { selectedColor = "#007AFF"; resetColorPickerBorders(v); }
        });
        findViewById(R.id.color_pick_purple).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { selectedColor = "#AF52DE"; resetColorPickerBorders(v); }
        });
        findViewById(R.id.color_pick_orange).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { selectedColor = "#FF9500"; resetColorPickerBorders(v); }
        });

        findViewById(R.id.btn_save_subject).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = inputSubjectName.getText().toString().trim();
                if (name.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Enter a valid subject name", Toast.LENGTH_SHORT).show();
                    return;
                }
                long id = dbHelper.insertSubject(name, selectedColor);
                if (id != -1) {
                    Toast.makeText(MainActivity.this, "Subject added successfully!", Toast.LENGTH_SHORT).show();
                    inputSubjectName.setText("");
                    refreshSubjects();
                } else {
                    Toast.makeText(MainActivity.this, "Subject already exists!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void resetColorPickerBorders(View clicked) {
        // Visual click feedback helper
        Toast.makeText(this, "Color Selected!", Toast.LENGTH_SHORT).show();
    }

    private void refreshSubjects() {
        subjectsListContainer.removeAllViews();
        Cursor cursor = dbHelper.getAllSubjects();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                final long id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                final String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                String color = cursor.getString(cursor.getColumnIndexOrThrow("color"));

                LinearLayout item = new LinearLayout(this);
                item.setOrientation(LinearLayout.HORIZONTAL);
                item.setBackgroundResource(R.drawable.card_bg);
                item.setPadding(16, 16, 16, 16);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.setMargins(0, 0, 0, 12);
                item.setLayoutParams(lp);
                item.setGravity(Gravity.CENTER_VERTICAL);

                // Color Label Dot
                TextView dot = new TextView(this);
                dot.setText("● ");
                dot.setTextSize(20spToPx(10));
                dot.setTextColor(Color.parseColor(color));
                item.addView(dot);

                // Subject Title
                TextView title = new TextView(this);
                title.setText(name);
                title.setTextSize(16);
                title.setTextColor(Color.parseColor("#1E293B"));
                title.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                item.addView(title);

                // Delete Action button
                Button btnDel = new Button(this);
                btnDel.setText("🗑");
                btnDel.setTextSize(14);
                btnDel.setBackgroundColor(Color.TRANSPARENT);
                btnDel.setTextColor(Color.parseColor("#FF3B30"));
                btnDel.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                btnDel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dbHelper.deleteSubject(id);
                        Toast.makeText(MainActivity.this, "Deleted " + name, Toast.LENGTH_SHORT).show();
                        refreshSubjects();
                    }
                });
                item.addView(btnDel);

                subjectsListContainer.addView(item);
            }
            cursor.close();
        }
    }

    // --- TASKS INTERFACING STRUCTURES ---
    private void loadSubjectSpinner() {
        subjectIdsList.clear();
        subjectNamesList.clear();
        
        Cursor cursor = dbHelper.getAllSubjects();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                subjectIdsList.add(cursor.getLong(cursor.getColumnIndexOrThrow("id")));
                subjectNamesList.add(cursor.getString(cursor.getColumnIndexOrThrow("name")));
            }
            cursor.close();
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, subjectNamesList);
        spinnerTaskSubject.setAdapter(adapter);
    }

    private void setupTaskActions() {
        // Set default calendar due string
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        selectedDueDate = sdf.format(new Date());
        btnPickDate.setText(selectedDueDate);

        btnPickDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Calendar c = Calendar.getInstance();
                int year = c.get(Calendar.YEAR);
                int month = c.get(Calendar.MONTH);
                int day = c.get(Calendar.DAY_OF_MONTH);

                DatePickerDialog datePickerDialog = new DatePickerDialog(MainActivity.this,
                        new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                                Calendar resCal = Calendar.getInstance();
                                resCal.set(year, monthOfYear, dayOfMonth);
                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                                selectedDueDate = sdf.format(resCal.getTime());
                                btnPickDate.setText(selectedDueDate);
                            }
                        }, year, month, day);
                datePickerDialog.show();
            }
        });

        findViewById(R.id.btn_save_task).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String title = inputTaskTitle.getText().toString().trim();
                String desc = inputTaskDesc.getText().toString().trim();
                if (title.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please set task title", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (subjectIdsList.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Create a subject first before creating tasks!", Toast.LENGTH_SHORT).show();
                    return;
                }

                long subjectId = subjectIdsList.get(spinnerTaskSubject.getSelectedItemPosition());
                String priority = spinnerTaskPriority.getSelectedItem().toString();

                long taskId = dbHelper.insertTask(title, desc, selectedDueDate, subjectId, priority);
                if (taskId != -1) {
                    Toast.makeText(MainActivity.this, "Task Scheduled Successfully!", Toast.LENGTH_SHORT).show();
                    inputTaskTitle.setText("");
                    inputTaskDesc.setText("");
                    refreshTasks();
                }
            }
        });

        // Set Filters Button controls
        final Button filterAll = (Button) findViewById(R.id.btn_filter_all);
        final Button filterPending = (Button) findViewById(R.id.btn_filter_pending);
        final Button filterCompleted = (Button) findViewById(R.id.btn_filter_done);

        filterAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedFilter = 0;
                toggleFilterButtons(filterAll, filterPending, filterCompleted);
                refreshTasks();
            }
        });
        filterPending.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedFilter = 1;
                toggleFilterButtons(filterPending, filterAll, filterCompleted);
                refreshTasks();
            }
        });
        filterCompleted.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedFilter = 2;
                toggleFilterButtons(filterCompleted, filterAll, filterPending);
                refreshTasks();
            }
        });
    }

    private void toggleFilterButtons(Button active, Button b2, Button b3) {
        active.setBackgroundColor(Color.parseColor("#007AFF"));
        active.setTextColor(Color.WHITE);
        b2.setBackgroundColor(Color.parseColor("#E2E8F0"));
        b2.setTextColor(Color.parseColor("#1E293B"));
        b3.setBackgroundColor(Color.parseColor("#E2E8F0"));
        b3.setTextColor(Color.parseColor("#1E293B"));
    }

    private void refreshTasks() {
        tasksListContainer.removeAllViews();
        Cursor cursor = dbHelper.getAllTasks();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                final long id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                final String titleText = cursor.getString(cursor.getColumnIndexOrThrow("title"));
                String desc = cursor.getString(cursor.getColumnIndexOrThrow("description"));
                String dueDate = cursor.getString(cursor.getColumnIndexOrThrow("due_date"));
                int completed = cursor.getInt(cursor.getColumnIndexOrThrow("completed"));
                String priority = cursor.getString(cursor.getColumnIndexOrThrow("priority"));
                String subName = cursor.getString(cursor.getColumnIndexOrThrow("subj_name"));
                String subColor = cursor.getString(cursor.getColumnIndexOrThrow("subj_color"));

                if (selectedFilter == 1 && completed == 1) continue;
                if (selectedFilter == 2 && completed == 0) continue;

                LinearLayout taskCard = new LinearLayout(this);
                taskCard.setOrientation(LinearLayout.VERTICAL);
                taskCard.setBackgroundResource(R.drawable.card_bg);
                taskCard.setPadding(16, 16, 16, 16);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.setMargins(0, 0, 0, 12);
                taskCard.setLayoutParams(lp);

                LinearLayout rowHeader = new LinearLayout(this);
                rowHeader.setOrientation(LinearLayout.HORIZONTAL);
                rowHeader.setGravity(Gravity.CENTER_VERTICAL);

                // Completion check
                final CheckBox cb = new CheckBox(this);
                cb.setChecked(completed == 1);
                cb.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dbHelper.updateTaskCompletion(id, cb.isChecked());
                        refreshTasks();
                    }
                });
                rowHeader.addView(cb);

                // Title Layout
                TextView title = new TextView(this);
                title.setText(titleText);
                title.setTextSize(15);
                title.setTextColor(Color.parseColor("#1E293B"));
                title.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                if (completed == 1) {
                    title.setPaintFlags(title.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
                    title.setTextColor(Color.parseColor("#94A3B8"));
                }
                rowHeader.addView(title);

                // Reminder bell action button
                Button btnAlert = new Button(this);
                btnAlert.setText("⏰");
                btnAlert.setBackgroundColor(Color.TRANSPARENT);
                btnAlert.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        scheduleTaskReminder(titleText);
                    }
                });
                rowHeader.addView(btnAlert);

                // Delete task
                Button btnDel = new Button(this);
                btnDel.setText("✕");
                btnDel.setBackgroundColor(Color.TRANSPARENT);
                btnDel.setTextColor(Color.parseColor("#EF4444"));
                btnDel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dbHelper.deleteTask(id);
                        refreshTasks();
                    }
                });
                rowHeader.addView(btnDel);
                taskCard.addView(rowHeader);

                // Subtitle/Desc details row
                if (desc != null && !desc.isEmpty()) {
                    TextView descText = new TextView(this);
                    descText.setText(desc);
                    descText.setTextSize(13);
                    descText.setTextColor(Color.parseColor("#475569"));
                    descText.setPadding(32, 4, 0, 4);
                    taskCard.addView(descText);
                }

                // Tags Row container
                LinearLayout tagsRow = new LinearLayout(this);
                tagsRow.setOrientation(LinearLayout.HORIZONTAL);
                tagsRow.setPadding(32, 8, 0, 0);
                tagsRow.setGravity(Gravity.CENTER_VERTICAL);

                // Subject tag visual indicator
                if (subName != null) {
                    TextView tag = new TextView(this);
                    tag.setText(" " + subName + " ");
                    tag.setTextSize(11);
                    tag.setTextColor(Color.WHITE);
                    tag.setBackgroundColor(Color.parseColor(subColor != null ? subColor : "#64748B"));
                    tagsRow.addView(tag);
                }

                // Priority Tag indicator
                TextView pTag = new TextView(this);
                pTag.setText("   Priority: " + priority);
                pTag.setTextSize(11);
                pTag.setTextColor(Color.parseColor("#475569"));
                tagsRow.addView(pTag);

                // Due Date Tag indicator
                TextView dTag = new TextView(this);
                dTag.setText("   📅 Due: " + dueDate);
                dTag.setTextSize(11);
                dTag.setTextColor(Color.parseColor("#EF4444"));
                tagsRow.addView(dTag);

                taskCard.addView(tagsRow);
                tasksListContainer.addView(taskCard);
            }
            cursor.close();
        }
    }

    private void scheduleTaskReminder(final String taskTitle) {
        final Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        Calendar alarmTime = Calendar.getInstance();
                        alarmTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        alarmTime.set(Calendar.MINUTE, minute);
                        alarmTime.set(Calendar.SECOND, 0);

                        Intent intent = new Intent(MainActivity.this, AlarmReceiver.class);
                        intent.putExtra("task_title", taskTitle);
                        
                        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                                MainActivity.this,
                                (int) System.currentTimeMillis(),
                                intent,
                                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
                        );

                        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                        if (alarmManager != null) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmTime.getTimeInMillis(), pendingIntent);
                            } else {
                                alarmManager.setExact(AlarmManager.RTC_WAKEUP, alarmTime.getTimeInMillis(), pendingIntent);
                            }
                            Toast.makeText(MainActivity.this, "Study alarm reminder scheduled!", Toast.LENGTH_SHORT).show();
                        }
                    }
                }, hour, minute, true);
        timePickerDialog.show();
    }

    // --- PROGRESS STATS CALCULATOR METHODS ---
    private void refreshDashboard() {
        // Fetch general task variables counts
        Cursor cursor = dbHelper.getAllTasks();
        int total = 0;
        int completedCount = 0;
        dashboardTodayContainer.removeAllViews();

        if (cursor != null) {
            total = cursor.getCount();
            while (cursor.moveToNext()) {
                int completed = cursor.getInt(cursor.getColumnIndexOrThrow("completed"));
                if (completed == 1) {
                    completedCount++;
                } else {
                    // Render active elements on Dashboard overview
                    if (dashboardTodayContainer.getChildCount() < 3) {
                        TextView t = new TextView(this);
                        String sub = cursor.getString(cursor.getColumnIndexOrThrow("subj_name"));
                        String title = cursor.getString(cursor.getColumnIndexOrThrow("title"));
                        String date = cursor.getString(cursor.getColumnIndexOrThrow("due_date"));
                        t.setText("⚡ " + (sub != null ? "[" + sub + "] " : "") + title + " (Due: " + date + ")");
                        t.setPadding(0, 8, 0, 8);
                        t.setTextSize(13);
                        t.setTextColor(Color.parseColor("#334155"));
                        dashboardTodayContainer.addView(t);
                    }
                }
            }
            cursor.close();
        }

        if (dashboardTodayContainer.getChildCount() == 0) {
            TextView empty = new TextView(this);
            empty.setText("🎉 All clear! No pending tasks remaining.");
            empty.setTextColor(Color.parseColor("#475569"));
            empty.setPadding(0, 16, 0, 16);
            dashboardTodayContainer.addView(empty);
        }

        int percent = total > 0 ? (completedCount * 100) / total : 0;
        dashboardProgressBar.setProgress(percent);
        dashboardProgressText.setText(percent + "% of your study tasks completed (" + completedCount + "/" + total + ")");
    }

    private void refreshStats() {
        // General counts calculation metrics
        Cursor subCursor = dbHelper.getAllSubjects();
        int subCount = subCursor != null ? subCursor.getCount() : 0;
        if (subCursor != null) subCursor.close();

        Cursor taskCursor = dbHelper.getAllTasks();
        int totalTasks = 0;
        int completedTasks = 0;
        if (taskCursor != null) {
            totalTasks = taskCursor.getCount();
            while (taskCursor.moveToNext()) {
                if (taskCursor.getInt(taskCursor.getColumnIndexOrThrow("completed")) == 1) {
                    completedTasks++;
                }
            }
            taskCursor.close();
        }

        txtStatTotalSubj.setText(String.valueOf(subCount));
        txtStatTotalTasks.setText(String.valueOf(totalTasks));
        txtStatCompletedTasks.setText(String.valueOf(completedTasks));

        // Individual breakdown bars per registered subject
        statsSubjectsContainer.removeAllViews();
        Cursor subQuery = dbHelper.getAllSubjects();
        if (subQuery != null) {
            while (subQuery.moveToNext()) {
                long sId = subQuery.getLong(subQuery.getColumnIndexOrThrow("id"));
                String sName = subQuery.getString(subQuery.getColumnIndexOrThrow("name"));
                String sColor = subQuery.getString(subQuery.getColumnIndexOrThrow("color"));

                // calculate tasks stats for this specific subject
                int sTotal = 0;
                int sCompleted = 0;
                Cursor tc = dbHelper.getTasksBySubject(sId);
                if (tc != null) {
                    sTotal = tc.getCount();
                    while (tc.moveToNext()) {
                        if (tc.getInt(tc.getColumnIndexOrThrow("completed")) == 1) {
                            sCompleted++;
                        }
                    }
                    tc.close();
                }

                LinearLayout statBarCard = new LinearLayout(this);
                statBarCard.setOrientation(LinearLayout.VERTICAL);
                statBarCard.setBackgroundResource(R.drawable.card_bg);
                statBarCard.setPadding(16, 16, 16, 16);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.setMargins(0, 0, 0, 12);
                statBarCard.setLayoutParams(lp);

                TextView label = new TextView(this);
                label.setText(sName + " Progress (" + sCompleted + "/" + sTotal + ")");
                label.setTextSize(14);
                label.setTextColor(Color.parseColor("#1E293B"));
                statBarCard.addView(label);

                ProgressBar pb = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
                pb.setMax(sTotal > 0 ? sTotal : 1);
                pb.setProgress(sCompleted);
                pb.setPadding(0, 8, 0, 0);
                statBarCard.addView(pb);

                statsSubjectsContainer.addView(statBarCard);
            }
            subQuery.close();
        }
    }

    // Helper utility converting SP unit dimensions
    private int spToPx(float sp) {
        return (int) (sp * getResources().getDisplayMetrics().scaledDensity);
    }
}