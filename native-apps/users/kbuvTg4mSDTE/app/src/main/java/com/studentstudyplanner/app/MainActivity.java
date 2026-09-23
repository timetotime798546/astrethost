package com.studentstudyplanner.app;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends Activity {

    private DatabaseHelper dbHelper;

    // View Switching Navigation Elements
    private View layoutSubjects, layoutTasks, layoutTimer, layoutProgress;
    private Button navBtnSubjects, navBtnTasks, navBtnTimer, navBtnProgress;

    // Subject Management Controls
    private EditText etSubjectName;
    private Button btnAddSubject;
    private LinearLayout containerSubjects;

    // Task Allocation Controls
    private EditText etTaskTitle;
    private Spinner spinnerTaskSubject;
    private Button btnPickDate;
    private TextView tvSelectedDate;
    private Spinner spinnerTaskPriority;
    private Button btnAddTask;
    private Spinner spinnerFilterSubject;
    private LinearLayout containerTasks;

    private String selectedTaskDate = "No date set";
    private ArrayList<Integer> taskSubjectIds = new ArrayList<>();
    private ArrayList<Integer> filterSubjectIds = new ArrayList<>();

    // Pomodoro Timer Controls
    private TextView tvTimerClock;
    private Button btnTimerStart, btnTimerPause, btnTimerReset;
    private TextView tvTimerStatus;
    private CountDownTimer countDownTimer;
    private long timeLeftInMillis = 1500000; // Default: 25 minutes
    private boolean timerRunning = false;

    // Quick Reminders Controls
    private EditText etReminderTitle;
    private Button btnPickReminderTime;
    private Button btnAddReminder;
    private LinearLayout containerReminders;
    private String selectedReminderTime = "Set Time";

    // Progress Reporting Indicators
    private TextView tvStatTotalTasks, tvStatCompletedTasks;
    private ProgressBar pbOverallProgress;
    private TextView tvOverallPercentage;
    private LinearLayout containerSubjectProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);

        // Map layout references
        layoutSubjects = findViewById(R.id.layout_subjects);
        layoutTasks = findViewById(R.id.layout_tasks);
        layoutTimer = findViewById(R.id.layout_timer);
        layoutProgress = findViewById(R.id.layout_progress);

        navBtnSubjects = findViewById(R.id.nav_btn_subjects);
        navBtnTasks = findViewById(R.id.nav_btn_tasks);
        navBtnTimer = findViewById(R.id.nav_btn_timer);
        navBtnProgress = findViewById(R.id.nav_btn_progress);

        // Subject Management Form Hooks
        etSubjectName = findViewById(R.id.et_subject_name);
        btnAddSubject = findViewById(R.id.btn_add_subject);
        containerSubjects = findViewById(R.id.container_subjects);

        // Task Creation Elements
        etTaskTitle = findViewById(R.id.et_task_title);
        spinnerTaskSubject = findViewById(R.id.spinner_task_subject);
        btnPickDate = findViewById(R.id.btn_pick_date);
        tvSelectedDate = findViewById(R.id.tv_selected_date);
        spinnerTaskPriority = findViewById(R.id.spinner_task_priority);
        btnAddTask = findViewById(R.id.btn_add_task);
        spinnerFilterSubject = findViewById(R.id.spinner_filter_subject);
        containerTasks = findViewById(R.id.container_tasks);

        // Timer Elements
        tvTimerClock = findViewById(R.id.tv_timer_clock);
        btnTimerStart = findViewById(R.id.btn_timer_start);
        btnTimerPause = findViewById(R.id.btn_timer_pause);
        btnTimerReset = findViewById(R.id.btn_timer_reset);
        tvTimerStatus = findViewById(R.id.tv_timer_status);

        // Reminder Elements
        etReminderTitle = findViewById(R.id.et_reminder_title);
        btnPickReminderTime = findViewById(R.id.btn_pick_reminder_time);
        btnAddReminder = findViewById(R.id.btn_add_reminder);
        containerReminders = findViewById(R.id.container_reminders);

        // Analytics Progress Elements
        tvStatTotalTasks = findViewById(R.id.tv_stat_total_tasks);
        tvStatCompletedTasks = findViewById(R.id.tv_stat_completed_tasks);
        pbOverallProgress = findViewById(R.id.pb_overall_progress);
        tvOverallPercentage = findViewById(R.id.tv_overall_percentage);
        containerSubjectProgress = findViewById(R.id.container_subject_progress);

        // Setup dynamic Switcher Callbacks
        navBtnSubjects.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLayout(layoutSubjects, navBtnSubjects);
            }
        });

        navBtnTasks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLayout(layoutTasks, navBtnTasks);
            }
        });

        navBtnTimer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLayout(layoutTimer, navBtnTimer);
            }
        });

        navBtnProgress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLayout(layoutProgress, navBtnProgress);
                refreshProgress();
            }
        });

        // Initialize SQLite Default Subjects securely
        Cursor testCursor = dbHelper.getAllSubjects();
        if (testCursor == null || testCursor.getCount() == 0) {
            dbHelper.insertSubject("Mathematics");
            dbHelper.insertSubject("Computer Science");
            dbHelper.insertSubject("Physics");
            dbHelper.insertSubject("History");
        }
        if (testCursor != null) {
            testCursor.close();
        }

        // Add Subject Click Trigger
        btnAddSubject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String subName = etSubjectName.getText().toString().trim();
                if (!subName.isEmpty()) {
                    dbHelper.insertSubject(subName);
                    etSubjectName.setText("");
                    Toast.makeText(MainActivity.this, "Subject Added!", Toast.LENGTH_SHORT).show();
                    refreshSubjectsList();
                    refreshTasksSubjectSpinners();
                    refreshProgress();
                } else {
                    Toast.makeText(MainActivity.this, "Subject name cannot be empty!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Setup Task Priority Selection Options
        ArrayList<String> priorityOptions = new ArrayList<>();
        priorityOptions.add("Low");
        priorityOptions.add("Medium");
        priorityOptions.add("High");
        ArrayAdapter<String> priorityAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, priorityOptions);
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTaskPriority.setAdapter(priorityAdapter);

        // Pick Date Picker Action
        btnPickDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calendar c = Calendar.getInstance();
                int yr = c.get(Calendar.YEAR);
                int mth = c.get(Calendar.MONTH);
                int dy = c.get(Calendar.DAY_OF_MONTH);

                DatePickerDialog dp = new DatePickerDialog(MainActivity.this,
                        new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                                selectedTaskDate = year + "-" + (month + 1) + "-" + dayOfMonth;
                                tvSelectedDate.setText(selectedTaskDate);
                            }
                        }, yr, mth, dy);
                dp.show();
            }
        });

        // Submit New Task Event
        btnAddTask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String titleStr = etTaskTitle.getText().toString().trim();
                if (titleStr.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please enter a task title!", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (spinnerTaskSubject.getSelectedItem() == null) {
                    Toast.makeText(MainActivity.this, "Please register a subject first!", Toast.LENGTH_SHORT).show();
                    return;
                }

                int selSubjectIdx = spinnerTaskSubject.getSelectedItemPosition();
                if (selSubjectIdx >= 0 && selSubjectIdx < taskSubjectIds.size()) {
                    int subjectId = taskSubjectIds.get(selSubjectIdx);
                    String priorityStr = spinnerTaskPriority.getSelectedItem().toString();

                    dbHelper.insertTask(subjectId, titleStr, selectedTaskDate, priorityStr);
                    etTaskTitle.setText("");
                    selectedTaskDate = "No date set";
                    tvSelectedDate.setText(selectedTaskDate);

                    Toast.makeText(MainActivity.this, "Study Task Assigned!", Toast.LENGTH_SHORT).show();
                    refreshTasksList();
                    refreshProgress();
                }
            }
        });

        // Pick Reminder Time Picker Action
        btnPickReminderTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calendar c = Calendar.getInstance();
                int hr = c.get(Calendar.HOUR_OF_DAY);
                int mn = c.get(Calendar.MINUTE);

                TimePickerDialog tp = new TimePickerDialog(MainActivity.this,
                        new TimePickerDialog.OnTimeSetListener() {
                            @Override
                            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                                selectedReminderTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
                                btnPickReminderTime.setText(selectedReminderTime);
                            }
                        }, hr, mn, true);
                tp.show();
            }
        });

        // Add Quick Reminder Event
        btnAddReminder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String title = etReminderTitle.getText().toString().trim();
                if (title.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Enter a title for the reminder!", Toast.LENGTH_SHORT).show();
                    return;
                }
                if ("Set Time".equals(selectedReminderTime)) {
                    Toast.makeText(MainActivity.this, "Please specify a reminder alarm time!", Toast.LENGTH_SHORT).show();
                    return;
                }

                dbHelper.insertReminder(title, selectedReminderTime);
                etReminderTitle.setText("");
                selectedReminderTime = "Set Time";
                btnPickReminderTime.setText("Time");

                Toast.makeText(MainActivity.this, "Study Reminder Set Successfully!", Toast.LENGTH_SHORT).show();
                refreshRemindersList();
            }
        });

        // Pomodoro Buttons Clicks
        btnTimerStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!timerRunning) {
                    startStudyTimer();
                }
            }
        });

        btnTimerPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (timerRunning) {
                    pauseStudyTimer();
                }
            }
        });

        btnTimerReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetStudyTimer();
            }
        });

        // Setup Filter Spinner Callback Trigger safely
        spinnerFilterSubject.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                refreshTasksList();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Primary initialization loading
        refreshSubjectsList();
        refreshTasksSubjectSpinners();
        refreshTasksList();
        refreshRemindersList();
        refreshProgress();
    }

    private void showLayout(View targetLayout, Button targetButton) {
        layoutSubjects.setVisibility(View.GONE);
        layoutTasks.setVisibility(View.GONE);
        layoutTimer.setVisibility(View.GONE);
        layoutProgress.setVisibility(View.GONE);

        navBtnSubjects.setTextColor(Color.parseColor("#757575"));
        navBtnTasks.setTextColor(Color.parseColor("#757575"));
        navBtnTimer.setTextColor(Color.parseColor("#757575"));
        navBtnProgress.setTextColor(Color.parseColor("#757575"));

        targetLayout.setVisibility(View.VISIBLE);
        targetButton.setTextColor(Color.parseColor("#2196F3"));
    }

    private void refreshSubjectsList() {
        containerSubjects.removeAllViews();
        Cursor cursor = dbHelper.getAllSubjects();
        if (cursor != null) {
            int idIndex = cursor.getColumnIndex(DatabaseHelper.COL_SUBJECT_ID);
            int nameIndex = cursor.getColumnIndex(DatabaseHelper.COL_SUBJECT_NAME);

            while (cursor.moveToNext()) {
                if (idIndex != -1 && nameIndex != -1) {
                    final int id = cursor.getInt(idIndex);
                    final String name = cursor.getString(nameIndex);

                    LinearLayout row = new LinearLayout(this);
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    row.setPadding(16, 16, 16, 16);
                    row.setBackgroundColor(Color.WHITE);
                    row.setGravity(Gravity.CENTER_VERTICAL);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    lp.setMargins(0, 4, 0, 8);
                    row.setLayoutParams(lp);

                    TextView tvName = new TextView(this);
                    tvName.setText(name);
                    tvName.setTextSize(16);
                    tvName.setTextColor(Color.BLACK);
                    tvName.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

                    Button btnDelete = new Button(this);
                    btnDelete.setText("Delete");
                    btnDelete.setTextSize(12);
                    btnDelete.setBackgroundColor(Color.parseColor("#F44336"));
                    btnDelete.setTextColor(Color.WHITE);
                    LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    btnLp.setMargins(8, 0, 0, 0);
                    btnDelete.setLayoutParams(btnLp);

                    btnDelete.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dbHelper.deleteSubject(id);
                            refreshSubjectsList();
                            refreshTasksSubjectSpinners();
                            refreshTasksList();
                            refreshProgress();
                        }
                    });

                    row.addView(tvName);
                    row.addView(btnDelete);
                    containerSubjects.addView(row);
                }
            }
            cursor.close();
        }
    }

    private void refreshTasksSubjectSpinners() {
        ArrayList<String> subjectNames = new ArrayList<>();
        taskSubjectIds.clear();

        Cursor cursor = dbHelper.getAllSubjects();
        if (cursor != null) {
            int idIndex = cursor.getColumnIndex(DatabaseHelper.COL_SUBJECT_ID);
            int nameIndex = cursor.getColumnIndex(DatabaseHelper.COL_SUBJECT_NAME);

            while (cursor.moveToNext()) {
                if (idIndex != -1 && nameIndex != -1) {
                    int id = cursor.getInt(idIndex);
                    String name = cursor.getString(nameIndex);
                    subjectNames.add(name);
                    taskSubjectIds.add(id);
                }
            }
            cursor.close();
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, subjectNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTaskSubject.setAdapter(adapter);

        // Set Filter List securely
        ArrayList<String> filterNames = new ArrayList<>();
        filterNames.add("All Subjects");
        filterSubjectIds.clear();
        for (int i = 0; i < subjectNames.size(); i++) {
            filterNames.add(subjectNames.get(i));
            if (i < taskSubjectIds.size()) {
                filterSubjectIds.add(taskSubjectIds.get(i));
            }
        }

        ArrayAdapter<String> filterAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, filterNames);
        filterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilterSubject.setAdapter(filterAdapter);
    }

    private void refreshTasksList() {
        containerTasks.removeAllViews();
        int selectedFilterSubjectId = -1;
        int filterPos = spinnerFilterSubject.getSelectedItemPosition();
        if (filterPos > 0) {
            int filterIdx = filterPos - 1;
            if (filterIdx >= 0 && filterIdx < filterSubjectIds.size()) {
                selectedFilterSubjectId = filterSubjectIds.get(filterIdx);
            }
        }

        Cursor cursor = dbHelper.getTasksBySubject(selectedFilterSubjectId);
        if (cursor != null) {
            int idIndex = cursor.getColumnIndex(DatabaseHelper.COL_TASK_ID);
            int subIdIndex = cursor.getColumnIndex(DatabaseHelper.COL_TASK_SUBJECT_ID);
            int titleIndex = cursor.getColumnIndex(DatabaseHelper.COL_TASK_TITLE);
            int dueDateIndex = cursor.getColumnIndex(DatabaseHelper.COL_TASK_DUE_DATE);
            int priorityIndex = cursor.getColumnIndex(DatabaseHelper.COL_TASK_PRIORITY);
            int completedIndex = cursor.getColumnIndex(DatabaseHelper.COL_TASK_COMPLETED);

            while (cursor.moveToNext()) {
                final int id = idIndex != -1 ? cursor.getInt(idIndex) : 0;
                final int subId = subIdIndex != -1 ? cursor.getInt(subIdIndex) : 0;
                final String title = titleIndex != -1 ? cursor.getString(titleIndex) : "";
                final String dueDate = dueDateIndex != -1 ? cursor.getString(dueDateIndex) : "";
                final String priority = priorityIndex != -1 ? cursor.getString(priorityIndex) : "";
                final int completed = completedIndex != -1 ? cursor.getInt(completedIndex) : 0;

                String subjectName = dbHelper.getSubjectNameById(subId);

                LinearLayout card = new LinearLayout(this);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setPadding(16, 16, 16, 16);
                card.setBackgroundColor(Color.WHITE);
                LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                cardLp.setMargins(0, 4, 0, 12);
                card.setLayoutParams(cardLp);

                LinearLayout titleLine = new LinearLayout(this);
                titleLine.setOrientation(LinearLayout.HORIZONTAL);
                titleLine.setGravity(Gravity.CENTER_VERTICAL);

                final CheckBox cbCompleted = new CheckBox(this);
                cbCompleted.setChecked(completed == 1);
                cbCompleted.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dbHelper.updateTaskStatus(id, cbCompleted.isChecked() ? 1 : 0);
                        refreshTasksList();
                        refreshProgress();
                    }
                });

                TextView tvTitle = new TextView(this);
                tvTitle.setText(title);
                tvTitle.setTextSize(16);
                tvTitle.setTextColor(Color.BLACK);
                tvTitle.setTypeface(null, Typeface.BOLD);
                if (completed == 1) {
                    tvTitle.setPaintFlags(tvTitle.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
                    tvTitle.setTextColor(Color.GRAY);
                }
                tvTitle.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

                Button btnDelete = new Button(this);
                btnDelete.setText("X");
                btnDelete.setTextSize(10);
                btnDelete.setBackgroundColor(Color.parseColor("#757575"));
                btnDelete.setTextColor(Color.WHITE);
                LinearLayout.LayoutParams delBtnLp = new LinearLayout.LayoutParams(48, 48);
                delBtnLp.setMargins(8, 0, 0, 0);
                btnDelete.setLayoutParams(delBtnLp);
                btnDelete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dbHelper.deleteTask(id);
                        refreshTasksList();
                        refreshProgress();
                    }
                });

                titleLine.addView(cbCompleted);
                titleLine.addView(tvTitle);
                titleLine.addView(btnDelete);

                LinearLayout detailsLine = new LinearLayout(this);
                detailsLine.setOrientation(LinearLayout.HORIZONTAL);
                detailsLine.setPadding(36, 8, 0, 0);

                TextView tvDetails = new TextView(this);
                tvDetails.setText(subjectName + " | Due: " + dueDate);
                tvDetails.setTextSize(12);
                tvDetails.setTextColor(Color.parseColor("#616161"));
                tvDetails.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

                TextView tvPriority = new TextView(this);
                tvPriority.setText(priority);
                tvPriority.setTextSize(12);
                tvPriority.setTypeface(null, Typeface.BOLD);
                if ("High".equals(priority)) {
                    tvPriority.setTextColor(Color.RED);
                } else if ("Medium".equals(priority)) {
                    tvPriority.setTextColor(Color.parseColor("#FF9800"));
                } else {
                    tvPriority.setTextColor(Color.GREEN);
                }

                detailsLine.addView(tvDetails);
                detailsLine.addView(tvPriority);

                card.addView(titleLine);
                card.addView(detailsLine);
                containerTasks.addView(card);
            }
            cursor.close();
        }
    }

    private void refreshRemindersList() {
        containerReminders.removeAllViews();
        Cursor cursor = dbHelper.getAllReminders();
        if (cursor != null) {
            int idIndex = cursor.getColumnIndex(DatabaseHelper.COL_REMINDER_ID);
            int titleIndex = cursor.getColumnIndex(DatabaseHelper.COL_REMINDER_TITLE);
            int timeIndex = cursor.getColumnIndex(DatabaseHelper.COL_REMINDER_TIME);

            while (cursor.moveToNext()) {
                if (idIndex != -1 && titleIndex != -1 && timeIndex != -1) {
                    final int id = cursor.getInt(idIndex);
                    final String title = cursor.getString(titleIndex);
                    final String time = cursor.getString(timeIndex);

                    LinearLayout row = new LinearLayout(this);
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    row.setPadding(16, 12, 16, 12);
                    row.setBackgroundColor(Color.WHITE);
                    row.setGravity(Gravity.CENTER_VERTICAL);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    lp.setMargins(0, 4, 0, 8);
                    row.setLayoutParams(lp);

                    TextView tvInfo = new TextView(this);
                    tvInfo.setText(title + " at " + time);
                    tvInfo.setTextSize(14);
                    tvInfo.setTextColor(Color.BLACK);
                    tvInfo.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

                    Button btnDelete = new Button(this);
                    btnDelete.setText("Cancel");
                    btnDelete.setTextSize(10);
                    btnDelete.setBackgroundColor(Color.parseColor("#E91E63"));
                    btnDelete.setTextColor(Color.WHITE);
                    btnDelete.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dbHelper.deleteReminder(id);
                            refreshRemindersList();
                        }
                    });

                    row.addView(tvInfo);
                    row.addView(btnDelete);
                    containerReminders.addView(row);
                }
            }
            cursor.close();
        }
    }

    private void refreshProgress() {
        int total = dbHelper.getTotalTasksCount();
        int completed = dbHelper.getCompletedTasksCount();

        tvStatTotalTasks.setText(String.valueOf(total));
        tvStatCompletedTasks.setText(String.valueOf(completed));

        int percentage = 0;
        if (total > 0) {
            percentage = (int) (((double) completed / total) * 100);
        }
        pbOverallProgress.setProgress(percentage);
        tvOverallPercentage.setText(percentage + "% Completed");

        // Per-Subject Progress Analysis List
        containerSubjectProgress.removeAllViews();
        Cursor cursor = dbHelper.getAllSubjects();
        if (cursor != null) {
            int idIndex = cursor.getColumnIndex(DatabaseHelper.COL_SUBJECT_ID);
            int nameIndex = cursor.getColumnIndex(DatabaseHelper.COL_SUBJECT_NAME);

            while (cursor.moveToNext()) {
                if (idIndex != -1 && nameIndex != -1) {
                    int subId = cursor.getInt(idIndex);
                    String name = cursor.getString(nameIndex);

                    int subTotal = dbHelper.getTasksCountBySubject(subId, false);
                    int subCompleted = dbHelper.getTasksCountBySubject(subId, true);

                    int subPercentage = 0;
                    if (subTotal > 0) {
                        subPercentage = (int) (((double) subCompleted / subTotal) * 100);
                    }

                    LinearLayout subjectRow = new LinearLayout(this);
                    subjectRow.setOrientation(LinearLayout.VERTICAL);
                    subjectRow.setPadding(16, 16, 16, 16);
                    subjectRow.setBackgroundColor(Color.WHITE);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    lp.setMargins(0, 4, 0, 8);
                    subjectRow.setLayoutParams(lp);

                    LinearLayout infoLine = new LinearLayout(this);
                    infoLine.setOrientation(LinearLayout.HORIZONTAL);

                    TextView tvName = new TextView(this);
                    tvName.setText(name);
                    tvName.setTypeface(null, Typeface.BOLD);
                    tvName.setTextColor(Color.BLACK);
                    tvName.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

                    TextView tvStats = new TextView(this);
                    tvStats.setText(subCompleted + " / " + subTotal + " (" + subPercentage + "%)");
                    tvStats.setTextColor(Color.parseColor("#757575"));

                    infoLine.addView(tvName);
                    infoLine.addView(tvStats);

                    ProgressBar pb = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
                    pb.setMax(100);
                    pb.setProgress(subPercentage);
                    LinearLayout.LayoutParams pbLp = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, 16);
                    pbLp.setMargins(0, 8, 0, 0);
                    pb.setLayoutParams(pbLp);

                    subjectRow.addView(infoLine);
                    subjectRow.addView(pb);

                    containerSubjectProgress.addView(subjectRow);
                }
            }
            cursor.close();
        }
    }

    private void startStudyTimer() {
        countDownTimer = new CountDownTimer(timeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                updateTimerText();
            }

            @Override
            public void onFinish() {
                timerRunning = false;
                btnTimerStart.setText("Start");
                tvTimerStatus.setText("Time is up! Great study session. Take a break.");
                try {
                    Ringtone r = RingtoneManager.getRingtone(
                            getApplicationContext(),
                            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    );
                    if (r != null) {
                        r.play();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();

        timerRunning = true;
        btnTimerStart.setText("Resume");
        tvTimerStatus.setText("Concentration Mode Active!");
    }

    private void pauseStudyTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        timerRunning = false;
        tvTimerStatus.setText("Session Paused.");
    }

    private void resetStudyTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        timeLeftInMillis = 1500000; // Reset to standard 25 mins
        updateTimerText();
        timerRunning = false;
        btnTimerStart.setText("Start");
        tvTimerStatus.setText("Prepare to Study!");
    }

    private void updateTimerText() {
        int minutes = (int) (timeLeftInMillis / 1000) / 60;
        int seconds = (int) (timeLeftInMillis / 1000) % 60;
        String format = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        tvTimerClock.setText(format);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        dbHelper.close();
    }
}