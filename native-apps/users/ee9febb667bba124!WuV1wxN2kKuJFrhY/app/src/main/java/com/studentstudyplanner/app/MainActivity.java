package com.studentstudyplanner.app;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
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
    private SQLiteDatabase db;

    // Active Tab Navigation Elements
    private View viewDashboard, viewSubjects, viewTasks, viewReminders;
    private Button tabDashboard, tabSubjects, tabTasks, tabReminders;
    private TextView headerTitle, quickStatsText;

    // Subject Form Controls
    private EditText subjectInput;
    private Button addSubjectBtn;
    private ListView subjectsListView;

    // Task Form Controls
    private Spinner taskSubjectSpinner;
    private EditText taskTitleInput;
    private EditText taskDescInput;
    private TextView selectedDateLabel;
    private Button chooseDateBtn;
    private Button addTaskBtn;
    private Spinner filterSubjectSpinner;
    private ListView tasksListView;
    private String taskSelectedDate = "";

    // Reminder Form Controls
    private EditText reminderTitleInput;
    private TextView reminderTimeLabel;
    private Button chooseReminderTimeBtn;
    private Button addReminderBtn;
    private ListView remindersListView;
    private Calendar reminderCalendar;

    // Dashboard UI Panels
    private ProgressBar dashboardProgress;
    private TextView dashboardStatsDetail;
    private TextView completedTasksCount;
    private TextView pendingTasksCount;
    private TextView totalSubjectsCount;
    private TextView activeAlarmsCount;
    private LinearLayout subjectsProgressContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);
        db = dbHelper.getWritableDatabase();

        reminderCalendar = Calendar.getInstance();

        initViews();
        setupNavigation();
        setupSubjectOperations();
        setupTaskOperations();
        setupReminderOperations();

        // Load Default View
        switchToDashboard();
    }

    private void initViews() {
        headerTitle = (TextView) findViewById(R.id.headerTitle);
        quickStatsText = (TextView) findViewById(R.id.quickStatsText);

        viewDashboard = findViewById(R.id.viewDashboard);
        viewSubjects = findViewById(R.id.viewSubjects);
        viewTasks = findViewById(R.id.viewTasks);
        viewReminders = findViewById(R.id.viewReminders);

        tabDashboard = (Button) findViewById(R.id.tabDashboard);
        tabSubjects = (Button) findViewById(R.id.tabSubjects);
        tabTasks = (Button) findViewById(R.id.tabTasks);
        tabReminders = (Button) findViewById(R.id.tabReminders);

        // Subjects inputs
        subjectInput = (EditText) findViewById(R.id.subjectInput);
        addSubjectBtn = (Button) findViewById(R.id.addSubjectBtn);
        subjectsListView = (ListView) findViewById(R.id.subjectsListView);

        // Tasks inputs
        taskSubjectSpinner = (Spinner) findViewById(R.id.taskSubjectSpinner);
        taskTitleInput = (EditText) findViewById(R.id.taskTitleInput);
        taskDescInput = (EditText) findViewById(R.id.taskDescInput);
        selectedDateLabel = (TextView) findViewById(R.id.selectedDateLabel);
        chooseDateBtn = (Button) findViewById(R.id.chooseDateBtn);
        addTaskBtn = (Button) findViewById(R.id.addTaskBtn);
        filterSubjectSpinner = (Spinner) findViewById(R.id.filterSubjectSpinner);
        tasksListView = (ListView) findViewById(R.id.tasksListView);

        // Reminders inputs
        reminderTitleInput = (EditText) findViewById(R.id.reminderTitleInput);
        reminderTimeLabel = (TextView) findViewById(R.id.reminderTimeLabel);
        chooseReminderTimeBtn = (Button) findViewById(R.id.chooseReminderTimeBtn);
        addReminderBtn = (Button) findViewById(R.id.addReminderBtn);
        remindersListView = (ListView) findViewById(R.id.remindersListView);

        // Dashboard Panels
        dashboardProgress = (ProgressBar) findViewById(R.id.dashboardProgress);
        dashboardStatsDetail = (TextView) findViewById(R.id.dashboardStatsDetail);
        completedTasksCount = (TextView) findViewById(R.id.completedTasksCount);
        pendingTasksCount = (TextView) findViewById(R.id.pendingTasksCount);
        totalSubjectsCount = (TextView) findViewById(R.id.totalSubjectsCount);
        activeAlarmsCount = (TextView) findViewById(R.id.activeAlarmsCount);
        subjectsProgressContainer = (LinearLayout) findViewById(R.id.subjectsProgressContainer);
    }

    private void setupNavigation() {
        tabDashboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchToDashboard();
            }
        });

        tabSubjects.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchToTab(viewSubjects, tabSubjects, "Academic Subjects");
                loadSubjectsList();
            }
        });

        tabTasks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchToTab(viewTasks, tabTasks, "Planner Backlog");
                loadTasksSpinners();
                loadTasksList();
            }
        });

        tabReminders.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchToTab(viewReminders, tabReminders, "Review Alarms");
                loadRemindersList();
            }
        });
    }

    private void switchToDashboard() {
        switchToTab(viewDashboard, tabDashboard, "Dashboard");
        calculateAndUpdateStats();
    }

    private void switchToTab(View selectedView, Button selectedTab, String title) {
        // Reset view states
        viewDashboard.setVisibility(View.GONE);
        viewSubjects.setVisibility(View.GONE);
        viewTasks.setVisibility(View.GONE);
        viewReminders.setVisibility(View.GONE);

        selectedView.setVisibility(View.VISIBLE);
        headerTitle.setText(title);

        // Style standard buttons
        resetTabButtons();
        selectedTab.setTextColor(Color.parseColor("#2196F3"));
        selectedTab.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
    }

    private void resetTabButtons() {
        tabDashboard.setTextColor(Color.parseColor("#777777"));
        tabDashboard.setTypeface(android.graphics.Typeface.DEFAULT);
        tabSubjects.setTextColor(Color.parseColor("#777777"));
        tabSubjects.setTypeface(android.graphics.Typeface.DEFAULT);
        tabTasks.setTextColor(Color.parseColor("#777777"));
        tabTasks.setTypeface(android.graphics.Typeface.DEFAULT);
        tabReminders.setTextColor(Color.parseColor("#777777"));
        tabReminders.setTypeface(android.graphics.Typeface.DEFAULT);
    }

    // ==========================================
    // STATS & PROGRESS CALCULATION
    // ==========================================
    private void calculateAndUpdateStats() {
        int totalSub = 0;
        Cursor subCur = db.rawQuery("SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_SUBJECTS, null);
        if (subCur.moveToFirst()) {
            totalSub = subCur.getInt(0);
        }
        subCur.close();

        int totalTasks = 0;
        int compTasks = 0;
        Cursor taskCur = db.rawQuery("SELECT COUNT(*), SUM(CASE WHEN " + DatabaseHelper.COL_TASK_STATUS + "=1 THEN 1 ELSE 0 END) FROM " + DatabaseHelper.TABLE_TASKS, null);
        if (taskCur.moveToFirst()) {
            totalTasks = taskCur.getInt(0);
            compTasks = taskCur.getInt(1);
        }
        taskCur.close();

        int totalRems = 0;
        Cursor remCur = db.rawQuery("SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_REMINDERS, null);
        if (remCur.moveToFirst()) {
            totalRems = remCur.getInt(0);
        }
        remCur.close();

        // Math Calculations
        int completionPercent = 0;
        if (totalTasks > 0) {
            completionPercent = (compTasks * 100) / totalTasks;
        }

        dashboardProgress.setProgress(completionPercent);
        quickStatsText.setText(completionPercent + "% Done");

        if (totalTasks == 0) {
            dashboardStatsDetail.setText("No Planner Tasks Entered Yet. Go to tasks to configure.");
        } else {
            dashboardStatsDetail.setText("Completed " + compTasks + " out of " + totalTasks + " scheduled tasks (" + completionPercent + "% success rate).");
        }

        completedTasksCount.setText(String.valueOf(compTasks));
        pendingTasksCount.setText(String.valueOf(totalTasks - compTasks));
        totalSubjectsCount.setText(String.valueOf(totalSub));
        activeAlarmsCount.setText(String.valueOf(totalRems));

        // Draw individual progress indicators dynamically
        subjectsProgressContainer.removeAllViews();
        if (totalSub == 0) {
            TextView noSubText = new TextView(this);
            noSubText.setText("Create subjects to analyze individual metrics.");
            noSubText.setPadding(0, 10, 0, 10);
            noSubText.setTextColor(Color.GRAY);
            subjectsProgressContainer.addView(noSubText);
        } else {
            Cursor progressCursor = db.rawQuery(
                    "SELECT s." + DatabaseHelper.COL_SUB_NAME + ", " +
                            "COUNT(t." + DatabaseHelper.COL_TASK_ID + "), " +
                            "SUM(CASE WHEN t." + DatabaseHelper.COL_TASK_STATUS + "=1 THEN 1 ELSE 0 END) " +
                            "FROM " + DatabaseHelper.TABLE_SUBJECTS + " s " +
                            "LEFT JOIN " + DatabaseHelper.TABLE_TASKS + " t " +
                            "ON s." + DatabaseHelper.COL_SUB_ID + " = t." + DatabaseHelper.COL_TASK_SUB_ID + " " +
                            "GROUP BY s." + DatabaseHelper.COL_SUB_ID, null);

            while (progressCursor.moveToNext()) {
                String sName = progressCursor.getString(0);
                int subTotalTasks = progressCursor.getInt(1);
                int subCompTasks = progressCursor.getInt(2);

                int subProgress = 0;
                if (subTotalTasks > 0) {
                    subProgress = (subCompTasks * 100) / subTotalTasks;
                }

                // Append custom textual layout dynamically for simplicity and safety
                LinearLayout detailLayout = new LinearLayout(this);
                detailLayout.setOrientation(LinearLayout.VERTICAL);
                detailLayout.setPadding(0, 8, 0, 16);

                TextView label = new TextView(this);
                label.setText(sName + " (" + subProgress + "% completed - " + subCompTasks + "/" + subTotalTasks + " tasks)");
                label.setTextColor(Color.BLACK);
                label.setTextSize(14spToPx(14));
                label.setPadding(0, 0, 0, 4);

                ProgressBar itemBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
                itemBar.setMax(100);
                itemBar.setProgress(subProgress);

                detailLayout.addView(label);
                detailLayout.addView(itemBar);
                subjectsProgressContainer.addView(detailLayout);
            }
            progressCursor.close();
        }
    }

    private int sPtoPx(int sp) {
        return (int) (sp * getResources().getDisplayMetrics().scaledDensity);
    }
    private int spToPx(float sp) {
        return (int) (sp * getResources().getDisplayMetrics().scaledDensity);
    }

    // ==========================================
    // MODULE: SUBJECTS
    // ==========================================
    private void setupSubjectOperations() {
        addSubjectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String subName = subjectInput.getText().toString().trim();
                if (subName.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please insert a valid subject name", Toast.LENGTH_SHORT).show();
                    return;
                }

                ContentValues vals = new ContentValues();
                vals.put(DatabaseHelper.COL_SUB_NAME, subName);

                long result = db.insert(DatabaseHelper.TABLE_SUBJECTS, null, vals);
                if (result == -1) {
                    Toast.makeText(MainActivity.this, "Subject matches an existing one", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Subject added successfully", Toast.LENGTH_SHORT).show();
                    subjectInput.setText("");
                    loadSubjectsList();
                }
            }
        });
    }

    private void loadSubjectsList() {
        final ArrayList<Long> ids = new ArrayList<Long>();
        final ArrayList<String> names = new ArrayList<String>();

        Cursor cursor = db.rawQuery("SELECT * FROM " + DatabaseHelper.TABLE_SUBJECTS, null);
        while (cursor.moveToNext()) {
            ids.add(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SUB_ID)));
            names.add(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SUB_NAME)));
        }
        cursor.close();

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, names) {
            @Override
            public View getView(final int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = LayoutInflater.from(getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
                }
                TextView text = (TextView) convertView.findViewById(android.R.id.text1);
                text.setText(names.get(position));

                // Support custom click logic to delete on long press or structured alert Dialog
                convertView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle("Delete Subject")
                                .setMessage("Deleting subject '" + names.get(position) + "' will also remove all associated planner tasks. Do you want to proceed?")
                                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        db.delete(DatabaseHelper.TABLE_SUBJECTS, DatabaseHelper.COL_SUB_ID + "=?", new String[]{String.valueOf(ids.get(position))});
                                        loadSubjectsList();
                                    }
                                })
                                .setNegativeButton("Cancel", null)
                                .show();
                        return true;
                    }
                });

                return convertView;
            }
        };

        subjectsListView.setAdapter(adapter);
    }

    // ==========================================
    // MODULE: TASKS
    // ==========================================
    private void setupTaskOperations() {
        chooseDateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calendar c = Calendar.getInstance();
                DatePickerDialog datePicker = new DatePickerDialog(MainActivity.this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        Calendar chosen = Calendar.getInstance();
                        chosen.set(year, monthOfYear, dayOfMonth);
                        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                        taskSelectedDate = fmt.format(chosen.getTime());
                        selectedDateLabel.setText(taskSelectedDate);
                    }
                }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
                datePicker.show();
            }
        });

        addTaskBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (taskSubjectSpinner.getSelectedItem() == null) {
                    Toast.makeText(MainActivity.this, "Please select or create a study subject first", Toast.LENGTH_SHORT).show();
                    return;
                }

                String title = taskTitleInput.getText().toString().trim();
                String desc = taskDescInput.getText().toString().trim();

                if (title.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please complete the title field", Toast.LENGTH_SHORT).show();
                    return;
                }

                SpinnerItem selectedSub = (SpinnerItem) taskSubjectSpinner.getSelectedItem();
                long subId = selectedSub.id;

                ContentValues vals = new ContentValues();
                vals.put(DatabaseHelper.COL_TASK_SUB_ID, subId);
                vals.put(DatabaseHelper.COL_TASK_TITLE, title);
                vals.put(DatabaseHelper.COL_TASK_DESC, desc);
                vals.put(DatabaseHelper.COL_TASK_DUE, taskSelectedDate);
                vals.put(DatabaseHelper.COL_TASK_STATUS, 0);

                db.insert(DatabaseHelper.TABLE_TASKS, null, vals);
                Toast.makeText(MainActivity.this, "Planner task saved", Toast.LENGTH_SHORT).show();

                taskTitleInput.setText("");
                taskDescInput.setText("");
                taskSelectedDate = "";
                selectedDateLabel.setText("No date chosen");

                loadTasksList();
            }
        });

        filterSubjectSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadTasksList();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void loadTasksSpinners() {
        ArrayList<SpinnerItem> list = new ArrayList<SpinnerItem>();
        ArrayList<SpinnerItem> filterList = new ArrayList<SpinnerItem>();

        filterList.add(new SpinnerItem(-1, "All Subjects"));

        Cursor c = db.rawQuery("SELECT * FROM " + DatabaseHelper.TABLE_SUBJECTS, null);
        while (c.moveToNext()) {
            long id = c.getLong(c.getColumnIndexOrThrow(DatabaseHelper.COL_SUB_ID));
            String name = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_SUB_NAME));
            SpinnerItem item = new SpinnerItem(id, name);
            list.add(item);
            filterList.add(item);
        }
        c.close();

        ArrayAdapter<SpinnerItem> adapter = new ArrayAdapter<SpinnerItem>(this, android.R.layout.simple_spinner_item, list);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        taskSubjectSpinner.setAdapter(adapter);

        ArrayAdapter<SpinnerItem> filterAdapter = new ArrayAdapter<SpinnerItem>(this, android.R.layout.simple_spinner_item, filterList);
        filterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        filterSubjectSpinner.setAdapter(filterAdapter);
    }

    private void loadTasksList() {
        final ArrayList<TaskModel> taskList = new ArrayList<TaskModel>();
        SpinnerItem selectedFilterItem = (SpinnerItem) filterSubjectSpinner.getSelectedItem();

        String query = "SELECT t.*, s." + DatabaseHelper.COL_SUB_NAME + " FROM " + DatabaseHelper.TABLE_TASKS + " t " +
                "LEFT JOIN " + DatabaseHelper.TABLE_SUBJECTS + " s ON t." + DatabaseHelper.COL_TASK_SUB_ID + " = s." + DatabaseHelper.COL_SUB_ID;

        String[] selectionArgs = null;
        if (selectedFilterItem != null && selectedFilterItem.id != -1) {
            query += " WHERE t." + DatabaseHelper.COL_TASK_SUB_ID + " = ?";
            selectionArgs = new String[]{String.valueOf(selectedFilterItem.id)};
        }

        query += " ORDER BY t." + DatabaseHelper.COL_TASK_STATUS + " ASC, t." + DatabaseHelper.COL_TASK_ID + " DESC";

        Cursor c = db.rawQuery(query, selectionArgs);
        while (c.moveToNext()) {
            TaskModel m = new TaskModel();
            m.id = c.getLong(c.getColumnIndexOrThrow(DatabaseHelper.COL_TASK_ID));
            m.subjectId = c.getLong(c.getColumnIndexOrThrow(DatabaseHelper.COL_TASK_SUB_ID));
            m.title = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_TASK_TITLE));
            m.desc = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_TASK_DESC));
            m.due = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_TASK_DUE));
            m.isCompleted = c.getInt(c.getColumnIndexOrThrow(DatabaseHelper.COL_TASK_STATUS)) == 1;
            m.subjectName = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_SUB_NAME));
            taskList.add(m);
        }
        c.close();

        CustomTaskAdapter adapter = new CustomTaskAdapter(this, taskList);
        tasksListView.setAdapter(adapter);
    }

    // Custom Task Adaptor class to handle checkbox and delete hooks within strict Java 8 specs
    private class CustomTaskAdapter extends ArrayAdapter<TaskModel> {
        public CustomTaskAdapter(Context context, List<TaskModel> objects) {
            super(context, 0, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final TaskModel item = getItem(position);
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
            }

            TextView t1 = (TextView) convertView.findViewById(android.R.id.text1);
            TextView t2 = (TextView) convertView.findViewById(android.R.id.text2);

            String statusStr = item.isCompleted ? "[COMPLETED] " : "[PENDING] ";
            String subLabel = item.subjectName != null ? " | Subject: " + item.subjectName : "";
            String dueLabel = (item.due != null && !item.due.isEmpty()) ? " | Due: " + item.due : "";

            t1.setText(statusStr + item.title);
            t2.setText(item.desc + subLabel + dueLabel);

            if (item.isCompleted) {
                t1.setTextColor(Color.GRAY);
            } else {
                t1.setTextColor(Color.BLACK);
            }

            // Simple click listener to flip completed state, and long-click to delete task item
            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ContentValues vls = new ContentValues();
                    vls.put(DatabaseHelper.COL_TASK_STATUS, item.isCompleted ? 0 : 1);
                    db.update(DatabaseHelper.TABLE_TASKS, vls, DatabaseHelper.COL_TASK_ID + "=?", new String[]{String.valueOf(item.id)});
                    loadTasksList();
                }
            });

            convertView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    new AlertDialog.Builder(getContext())
                            .setTitle("Delete Task")
                            .setMessage("Do you want to clear this task record?")
                            .setPositiveButton("Remove", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    db.delete(DatabaseHelper.TABLE_TASKS, DatabaseHelper.COL_TASK_ID + "=?", new String[]{String.valueOf(item.id)});
                                    loadTasksList();
                                }
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                    return true;
                }
            });

            return convertView;
        }
    }

    // ==========================================
    // MODULE: REMINDERS / ALARMS
    // ==========================================
    private void setupReminderOperations() {
        chooseReminderTimeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Calendar cur = Calendar.getInstance();
                DatePickerDialog datePicker = new DatePickerDialog(MainActivity.this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        reminderCalendar.set(Calendar.YEAR, year);
                        reminderCalendar.set(Calendar.MONTH, month);
                        reminderCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                        TimePickerDialog timePicker = new TimePickerDialog(MainActivity.this, new TimePickerDialog.OnTimeSetListener() {
                            @Override
                            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                                reminderCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                reminderCalendar.set(Calendar.MINUTE, minute);
                                reminderCalendar.set(Calendar.SECOND, 0);

                                SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                                reminderTimeLabel.setText(fmt.format(reminderCalendar.getTime()));
                            }
                        }, cur.get(Calendar.HOUR_OF_DAY), cur.get(Calendar.MINUTE), true);
                        timePicker.show();
                    }
                }, cur.get(Calendar.YEAR), cur.get(Calendar.MONTH), cur.get(Calendar.DAY_OF_MONTH));
                datePicker.show();
            }
        });

        addReminderBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String title = reminderTitleInput.getText().toString().trim();
                if (title.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Enter reminder study topic", Toast.LENGTH_SHORT).show();
                    return;
                }

                long systemTime = System.currentTimeMillis();
                long triggerMillis = reminderCalendar.getTimeInMillis();

                if (triggerMillis <= systemTime) {
                    Toast.makeText(MainActivity.this, "Set a calendar timing in the future", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Write to local database table record
                ContentValues vals = new ContentValues();
                vals.put(DatabaseHelper.COL_REM_TITLE, title);
                vals.put(DatabaseHelper.COL_REM_TIME, triggerMillis);
                long rowId = db.insert(DatabaseHelper.TABLE_REMINDERS, null, vals);

                if (rowId != -1) {
                    scheduleAlarm(rowId, title, triggerMillis);
                    Toast.makeText(MainActivity.this, "Study alert scheduled!", Toast.LENGTH_SHORT).show();

                    reminderTitleInput.setText("");
                    reminderTimeLabel.setText("Date & Time: Not Set");
                    loadRemindersList();
                }
            }
        });
    }

    private void scheduleAlarm(long id, String title, long timeMillis) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, ReminderReceiver.class);
        intent.putExtra("title", title);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                (int) id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (alarmManager != null) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, timeMillis, pendingIntent);
        }
    }

    private void cancelAlarm(long id) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, ReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                (int) id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
    }

    private void loadRemindersList() {
        final ArrayList<ReminderModel> list = new ArrayList<ReminderModel>();
        Cursor c = db.rawQuery("SELECT * FROM " + DatabaseHelper.TABLE_REMINDERS + " ORDER BY " + DatabaseHelper.COL_REM_TIME + " ASC", null);

        while (c.moveToNext()) {
            ReminderModel m = new ReminderModel();
            m.id = c.getLong(c.getColumnIndexOrThrow(DatabaseHelper.COL_REM_ID));
            m.title = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_REM_TITLE));
            m.timeMillis = c.getLong(c.getColumnIndexOrThrow(DatabaseHelper.COL_REM_TIME));
            list.add(m);
        }
        c.close();

        ArrayAdapter<ReminderModel> adapter = new ArrayAdapter<ReminderModel>(this, android.R.layout.simple_list_item_2, list) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                final ReminderModel item = getItem(position);
                if (convertView == null) {
                    convertView = LayoutInflater.from(getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
                }

                TextView t1 = (TextView) convertView.findViewById(android.R.id.text1);
                TextView t2 = (TextView) convertView.findViewById(android.R.id.text2);

                t1.setText(item.title);

                SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                t2.setText("Time: " + fmt.format(new Date(item.timeMillis)));

                convertView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        new AlertDialog.Builder(getContext())
                                .setTitle("Clear Alert")
                                .setMessage("Do you wish to wipe this registered notification alarm?")
                                .setPositiveButton("Remove Alarm", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        cancelAlarm(item.id);
                                        db.delete(DatabaseHelper.TABLE_REMINDERS, DatabaseHelper.COL_REM_ID + "=?", new String[]{String.valueOf(item.id)});
                                        loadRemindersList();
                                    }
                                })
                                .setNegativeButton("Keep", null)
                                .show();
                        return true;
                    }
                });

                return convertView;
            }
        };

        remindersListView.setAdapter(adapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (db != null && db.isOpen()) {
            db.close();
        }
    }

    // ==========================================
    // STRUCTURAL MODEL REPRESENTATIONS
    // ==========================================
    private static class SpinnerItem {
        long id;
        String name;

        public SpinnerItem(long id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static class TaskModel {
        long id;
        long subjectId;
        String title;
        String desc;
        String due;
        boolean isCompleted;
        String subjectName;
    }

    private static class ReminderModel {
        long id;
        String title;
        long timeMillis;
    }
}