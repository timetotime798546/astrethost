package com.studentstudyplanner.app;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {

    private DatabaseHelper dbHelper;

    // View Panels
    private ScrollView panelDashboard;
    private LinearLayout panelSubjects;
    private LinearLayout panelTasks;
    private LinearLayout panelReminders;

    // Tab items
    private LinearLayout tabDashboard, tabSubjects, tabTasks, tabReminders;
    private TextView tabTextDashboard, tabTextSubjects, tabTextTasks, tabTextReminders;

    // ListViews and Adapters
    private ListView listViewSubjects;
    private ListView listViewTasks;
    private ListView listViewReminders;

    private SubjectAdapter subjectAdapter;
    private TaskAdapter taskAdapter;
    private ReminderAdapter reminderAdapter;

    // Dynamic Header Quick Add Buttons
    private Button buttonQuickAction;
    private Button btnAddSubject, btnAddTask, btnAddReminder;
    private Spinner spinnerTaskFilter;

    // Dashboard Statistics UI Elements
    private TextView textProgressPercentage;
    private ProgressBar progressOverall;
    private TextView statTotalTasks, statTotalHours, statSubjectsCount;
    private LinearLayout layoutSubjectProgressList;
    private LinearLayout layoutDashboardReminders;
    private TextView textNoRemindersDashboard;

    // Quick selections
    private String selectedColorHex = "#2196F3"; // Default initial color pick

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);

        // Map View panels
        panelDashboard = (ScrollView) findViewById(R.id.panelDashboard);
        panelSubjects = (LinearLayout) findViewById(R.id.panelSubjects);
        panelTasks = (LinearLayout) findViewById(R.id.panelTasks);
        panelReminders = (LinearLayout) findViewById(R.id.panelReminders);

        // Map Tabs
        tabDashboard = (LinearLayout) findViewById(R.id.tabDashboard);
        tabSubjects = (LinearLayout) findViewById(R.id.tabSubjects);
        tabTasks = (LinearLayout) findViewById(R.id.tabTasks);
        tabReminders = (LinearLayout) findViewById(R.id.tabReminders);

        tabTextDashboard = (TextView) findViewById(R.id.tabTextDashboard);
        tabTextSubjects = (TextView) findViewById(R.id.tabTextSubjects);
        tabTextTasks = (TextView) findViewById(R.id.tabTextTasks);
        tabTextReminders = (TextView) findViewById(R.id.tabTextReminders);

        // Map ListViews
        listViewSubjects = (ListView) findViewById(R.id.listViewSubjects);
        listViewTasks = (ListView) findViewById(R.id.listViewTasks);
        listViewReminders = (ListView) findViewById(R.id.listViewReminders);

        // Map Action Controls
        buttonQuickAction = (Button) findViewById(R.id.buttonQuickAction);
        btnAddSubject = (Button) findViewById(R.id.btnAddSubject);
        btnAddTask = (Button) findViewById(R.id.btnAddTask);
        btnAddReminder = (Button) findViewById(R.id.btnAddReminder);
        spinnerTaskFilter = (Spinner) findViewById(R.id.spinnerTaskFilter);

        // Map Dashboard stats UI
        textProgressPercentage = (TextView) findViewById(R.id.textProgressPercentage);
        progressOverall = (ProgressBar) findViewById(R.id.progressOverall);
        statTotalTasks = (TextView) findViewById(R.id.statTotalTasks);
        statTotalHours = (TextView) findViewById(R.id.statTotalHours);
        statSubjectsCount = (TextView) findViewById(R.id.statSubjectsCount);
        layoutSubjectProgressList = (LinearLayout) findViewById(R.id.layoutSubjectProgressList);
        layoutDashboardReminders = (LinearLayout) findViewById(R.id.layoutDashboardReminders);
        textNoRemindersDashboard = (TextView) findViewById(R.id.textNoRemindersDashboard);

        // Assign Navigation Click events (Traditional Java 8 inner classes compatibility)
        tabDashboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(0);
            }
        });
        tabSubjects.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(1);
            }
        });
        tabTasks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(2);
            }
        });
        tabReminders.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(3);
            }
        });

        // Register Action triggers
        buttonQuickAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showQuickAddTaskDialog();
            }
        });
        btnAddSubject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddSubjectDialog();
            }
        });
        btnAddTask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddTaskDialog();
            }
        });
        btnAddReminder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddReminderDialog();
            }
        });

        // Initialize Lists & Adapters
        refreshAllData();

        // Task filter listener
        spinnerTaskFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                refreshTaskList();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // Request runtime notifications permission safely on Android 13+ (API 33+)
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 101);
        }
    }

    private void switchTab(int index) {
        // Clear visibility state
        panelDashboard.setVisibility(View.GONE);
        panelSubjects.setVisibility(View.GONE);
        panelTasks.setVisibility(View.GONE);
        panelReminders.setVisibility(View.GONE);

        // Reset colors to inactive
        tabTextDashboard.setTextColor(Color.parseColor("#757575"));
        tabTextDashboard.setTextSize(12);
        tabTextSubjects.setTextColor(Color.parseColor("#757575"));
        tabTextSubjects.setTextSize(12);
        tabTextTasks.setTextColor(Color.parseColor("#757575"));
        tabTextTasks.setTextSize(12);
        tabTextReminders.setTextColor(Color.parseColor("#757575"));
        tabTextReminders.setTextSize(12);

        switch (index) {
            case 0:
                panelDashboard.setVisibility(View.VISIBLE);
                tabTextDashboard.setTextColor(Color.parseColor("#2196F3"));
                tabTextDashboard.setTextSize(13);
                refreshDashboard();
                break;
            case 1:
                panelSubjects.setVisibility(View.VISIBLE);
                tabTextSubjects.setTextColor(Color.parseColor("#2196F3"));
                tabTextSubjects.setTextSize(13);
                refreshSubjectList();
                break;
            case 2:
                panelTasks.setVisibility(View.VISIBLE);
                tabTextTasks.setTextColor(Color.parseColor("#2196F3"));
                tabTextTasks.setTextSize(13);
                refreshTaskList();
                break;
            case 3:
                panelReminders.setVisibility(View.VISIBLE);
                tabTextReminders.setTextColor(Color.parseColor("#2196F3"));
                tabTextReminders.setTextSize(13);
                refreshReminderList();
                break;
        }
    }

    private void refreshAllData() {
        populateSpinnerFilters();
        refreshDashboard();
        refreshSubjectList();
        refreshTaskList();
        refreshReminderList();
    }

    private void populateSpinnerFilters() {
        ArrayList<String> spinnerOptions = new ArrayList<String>();
        spinnerOptions.add("All Subjects");

        Cursor cursor = dbHelper.getAllSubjects();
        if (cursor != null) {
            int nameIndex = cursor.getColumnIndex(DatabaseHelper.COL_SUB_NAME);
            while (cursor.moveToNext()) {
                if (nameIndex != -1) {
                    spinnerOptions.add(cursor.getString(nameIndex));
                }
            }
            cursor.close();
        }

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, spinnerOptions);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTaskFilter.setAdapter(spinnerAdapter);
    }

    // Refresh dashboard visuals dynamically without relying on outside custom visual views
    private void refreshDashboard() {
        Cursor tasksCursor = dbHelper.getAllTasks();
        int totalTasks = 0;
        int completedTasks = 0;
        double totalStudyHours = 0.0;

        if (tasksCursor != null) {
            totalTasks = tasksCursor.getCount();
            int completedIdx = tasksCursor.getColumnIndex(DatabaseHelper.COL_TASK_COMPLETED);
            int hoursIdx = tasksCursor.getColumnIndex(DatabaseHelper.COL_TASK_HOURS);

            while (tasksCursor.moveToNext()) {
                if (completedIdx != -1 && tasksCursor.getInt(completedIdx) == 1) {
                    completedTasks++;
                }
                if (hoursIdx != -1) {
                    totalStudyHours += tasksCursor.getDouble(hoursIdx);
                }
            }
            tasksCursor.close();
        }

        // Subject count
        Cursor subCursor = dbHelper.getAllSubjects();
        int subjectCount = (subCursor != null) ? subCursor.getCount() : 0;
        if (subCursor != null) subCursor.close();

        // Update basic counts on Dashboard
        statTotalTasks.setText(String.valueOf(totalTasks));
        statTotalHours.setText(String.format(Locale.getDefault(), "%.1fh", totalStudyHours));
        statSubjectsCount.setText(String.valueOf(subjectCount));

        // Update overall circular statistics calculations
        int percentage = 0;
        if (totalTasks > 0) {
            percentage = (completedTasks * 100) / totalTasks;
        }

        textProgressPercentage.setText(percentage + "% Complete");
        progressOverall.setProgress(percentage);

        // Subject breakdown layouts injection
        layoutSubjectProgressList.removeAllViews();
        Cursor cursor = dbHelper.getAllSubjects();
        if (cursor != null) {
            int idIdx = cursor.getColumnIndex(DatabaseHelper.COL_SUB_ID);
            int nameIdx = cursor.getColumnIndex(DatabaseHelper.COL_SUB_NAME);
            int colorIdx = cursor.getColumnIndex(DatabaseHelper.COL_SUB_COLOR);

            while (cursor.moveToNext()) {
                if (idIdx != -1 && nameIdx != -1 && colorIdx != -1) {
                    final int subId = cursor.getInt(idIdx);
                    final String subName = cursor.getString(nameIdx);
                    final String subColor = cursor.getString(colorIdx);

                    // Compute specific statistics for this particular subject
                    Cursor subTaskCursor = dbHelper.getTasksBySubject(subId);
                    int sTotal = 0;
                    int sCompleted = 0;
                    if (subTaskCursor != null) {
                        sTotal = subTaskCursor.getCount();
                        int complIdx = subTaskCursor.getColumnIndex(DatabaseHelper.COL_TASK_COMPLETED);
                        while (subTaskCursor.moveToNext()) {
                            if (complIdx != -1 && subTaskCursor.getInt(complIdx) == 1) {
                                sCompleted++;
                            }
                        }
                        subTaskCursor.close();
                    }

                    int sPercent = (sTotal > 0) ? (sCompleted * 100) / sTotal : 0;

                    // Generate a nice visual row item programmatically for flexibility
                    LinearLayout row = new LinearLayout(this);
                    row.setOrientation(LinearLayout.VERTICAL);
                    row.setBackgroundResource(R.drawable.card_bg);
                    row.setPadding(12, 12, 12, 12);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    lp.setMargins(0, 4, 0, 8);
                    row.setLayoutParams(lp);

                    // Row Inner layout
                    LinearLayout rowHeader = new LinearLayout(this);
                    rowHeader.setOrientation(LinearLayout.HORIZONTAL);
                    rowHeader.setGravity(android.view.Gravity.CENTER_VERTICAL);

                    // Subject Badge indicator
                    View badge = new View(this);
                    LinearLayout.LayoutParams badgeLp = new LinearLayout.LayoutParams(14, 14);
                    badgeLp.setMargins(0, 0, 10, 0);
                    badge.setLayoutParams(badgeLp);
                    GradientDrawable bgShape = new GradientDrawable();
                    bgShape.setShape(GradientDrawable.OVAL);
                    bgShape.setColor(Color.parseColor(subColor));
                    badge.setBackground(bgShape);

                    TextView textName = new TextView(this);
                    textName.setText(subName);
                    textName.setTextSize(14);
                    textName.setTextColor(Color.parseColor("#333333"));
                    textName.setTextStyle(android.graphics.Typeface.BOLD);
                    textName.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

                    TextView textStatus = new TextView(this);
                    textStatus.setText(sCompleted + "/" + sTotal + " Done");
                    textStatus.setTextSize(12);
                    textStatus.setTextColor(Color.parseColor("#666666"));

                    rowHeader.addView(badge);
                    rowHeader.addView(textName);
                    rowHeader.addView(textStatus);

                    ProgressBar subProgress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
                    subProgress.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 8));
                    subProgress.setMax(100);
                    subProgress.setProgress(sPercent);
                    subProgress.setPadding(0, 8, 0, 0);

                    row.addView(rowHeader);
                    row.addView(subProgress);

                    layoutSubjectProgressList.addView(row);
                }
            }
            cursor.close();
        }

        // Inject upcoming planner reminders alerts
        layoutDashboardReminders.removeAllViews();
        Cursor remCursor = dbHelper.getAllReminders();
        if (remCursor != null && remCursor.getCount() > 0) {
            textNoRemindersDashboard.setVisibility(View.GONE);
            int titleIdx = remCursor.getColumnIndex(DatabaseHelper.COL_REM_TITLE);
            int timeIdx = remCursor.getColumnIndex(DatabaseHelper.COL_REM_TIME);

            int count = 0;
            while (remCursor.moveToNext() && count < 3) { // Show top 3 max on dashboard
                if (titleIdx != -1 && timeIdx != -1) {
                    String titleStr = remCursor.getString(titleIdx);
                    long alertTime = remCursor.getLong(timeIdx);

                    TextView item = new TextView(this);
                    SimpleDateFormat format = new SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault());
                    String formattedDate = format.format(new Date(alertTime));

                    item.setText("• " + titleStr + " (" + formattedDate + ")");
                    item.setPadding(0, 4, 0, 4);
                    item.setTextColor(Color.parseColor("#444444"));
                    item.setTextSize(13);

                    layoutDashboardReminders.addView(item);
                    count++;
                }
            }
            remCursor.close();
        } else {
            layoutDashboardReminders.addView(textNoRemindersDashboard);
            textNoRemindersDashboard.setVisibility(View.VISIBLE);
        }
    }

    private void refreshSubjectList() {
        Cursor cursor = dbHelper.getAllSubjects();
        if (subjectAdapter == null) {
            subjectAdapter = new SubjectAdapter(this, cursor);
            listViewSubjects.setAdapter(subjectAdapter);
        } else {
            subjectAdapter.changeCursor(cursor);
        }
    }

    private void refreshTaskList() {
        String filterSubject = spinnerTaskFilter.getSelectedItem() != null ? spinnerTaskFilter.getSelectedItem().toString() : "All Subjects";
        Cursor cursor;

        if (filterSubject.equals("All Subjects")) {
            cursor = dbHelper.getAllTasks();
        } else {
            // Locate Subject id by name
            int subjectId = -1;
            Cursor subCursor = dbHelper.getAllSubjects();
            if (subCursor != null) {
                int idIdx = subCursor.getColumnIndex(DatabaseHelper.COL_SUB_ID);
                int nameIdx = subCursor.getColumnIndex(DatabaseHelper.COL_SUB_NAME);
                while (subCursor.moveToNext()) {
                    if (nameIdx != -1 && subCursor.getString(nameIdx).equals(filterSubject)) {
                        subjectId = subCursor.getInt(idIdx);
                        break;
                    }
                }
                subCursor.close();
            }
            cursor = dbHelper.getTasksBySubject(subjectId);
        }

        if (taskAdapter == null) {
            taskAdapter = new TaskAdapter(this, cursor);
            listViewTasks.setAdapter(taskAdapter);
        } else {
            taskAdapter.changeCursor(cursor);
        }
    }

    private void refreshReminderList() {
        Cursor cursor = dbHelper.getAllReminders();
        if (reminderAdapter == null) {
            reminderAdapter = new ReminderAdapter(this, cursor);
            listViewReminders.setAdapter(reminderAdapter);
        } else {
            reminderAdapter.changeCursor(cursor);
        }
    }

    // Dynamic Dialog Creation for adding subjects
    private void showAddSubjectDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create New Subject");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(20, 20, 20, 20);

        final EditText inputName = new EditText(this);
        inputName.setHint("Subject Title (e.g. History)");
        layout.addView(inputName);

        // Color Palette Selector Title
        TextView textSelectColor = new TextView(this);
        textSelectColor.setText("Select Color Badge Theme:");
        textSelectColor.setPadding(0, 16, 0, 8);
        layout.addView(textSelectColor);

        // Simple color blocks layout
        final LinearLayout colorsLayout = new LinearLayout(this);
        colorsLayout.setOrientation(LinearLayout.HORIZONTAL);
        colorsLayout.setGravity(android.view.Gravity.CENTER);

        final String[] hexColors = {"#F44336", "#E91E63", "#9C27B0", "#3F51B5", "#2196F3", "#009688", "#4CAF50", "#FFEB3B", "#FF9800"};
        final ArrayList<View> colorViews = new ArrayList<View>();
        selectedColorHex = hexColors[4]; // Reset to default blue

        for (int i = 0; i < hexColors.length; i++) {
            final String hex = hexColors[i];
            final View colorBox = new View(this);
            LinearLayout.LayoutParams boxLp = new LinearLayout.LayoutParams(36, 36);
            boxLp.setMargins(6, 6, 6, 6);
            colorBox.setLayoutParams(boxLp);

            final GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.OVAL);
            shape.setColor(Color.parseColor(hex));
            // Add initial selection border
            if (hex.equals(selectedColorHex)) {
                shape.setStroke(4, Color.parseColor("#000000"));
            } else {
                shape.setStroke(1, Color.parseColor("#BBBBBB"));
            }
            colorBox.setBackground(shape);

            colorBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectedColorHex = hex;
                    // Redraw borders for all color view items
                    for (int k = 0; k < hexColors.length; k++) {
                        GradientDrawable s = (GradientDrawable) colorViews.get(k).getBackground();
                        if (hexColors[k].equals(selectedColorHex)) {
                            s.setStroke(4, Color.parseColor("#000000"));
                        } else {
                            s.setStroke(1, Color.parseColor("#BBBBBB"));
                        }
                    }
                }
            });
            colorViews.add(colorBox);
            colorsLayout.addView(colorBox);
        }
        layout.addView(colorsLayout);

        builder.setView(layout);
        builder.setPositiveButton("Create", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String subName = inputName.getText().toString().trim();
                if (subName.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please insert a valid title", Toast.LENGTH_SHORT).show();
                    return;
                }
                boolean done = dbHelper.addSubject(subName, selectedColorHex);
                if (done) {
                    Toast.makeText(MainActivity.this, "Subject registered!", Toast.LENGTH_SHORT).show();
                    populateSpinnerFilters();
                    refreshAllData();
                } else {
                    Toast.makeText(MainActivity.this, "Error inserting subject", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    // Dynamic dialog creation for adding study tasks
    private void showAddTaskDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create Planner Task");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(20, 20, 20, 20);

        // Task Title
        final EditText inputTitle = new EditText(this);
        inputTitle.setHint("Study Task Label");
        layout.addView(inputTitle);

        // Subject Selector
        TextView selectSubjectText = new TextView(this);
        selectSubjectText.setText("Select Subject Category:");
        selectSubjectText.setPadding(0, 10, 0, 4);
        layout.addView(selectSubjectText);

        final Spinner subjectSpinner = new Spinner(this);
        final ArrayList<Integer> subjectIdsList = new ArrayList<Integer>();
        ArrayList<String> subjectNamesList = new ArrayList<String>();

        Cursor cursor = dbHelper.getAllSubjects();
        if (cursor != null) {
            int idIdx = cursor.getColumnIndex(DatabaseHelper.COL_SUB_ID);
            int nameIdx = cursor.getColumnIndex(DatabaseHelper.COL_SUB_NAME);
            while (cursor.moveToNext()) {
                if (idIdx != -1 && nameIdx != -1) {
                    subjectIdsList.add(cursor.getInt(idIdx));
                    subjectNamesList.add(cursor.getString(nameIdx));
                }
            }
            cursor.close();
        }

        if (subjectNamesList.isEmpty()) {
            Toast.makeText(this, "Please create at least one subject first!", Toast.LENGTH_LONG).show();
            return;
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, subjectNamesList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        subjectSpinner.setAdapter(adapter);
        layout.addView(subjectSpinner);

        // Estimated hours
        final EditText inputHours = new EditText(this);
        inputHours.setHint("Estimated study hours (e.g. 2.5)");
        inputHours.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        layout.addView(inputHours);

        // Due date picker button
        final Button btnDatePicker = new Button(this);
        btnDatePicker.setText("Select Due Date");
        btnDatePicker.setBackgroundResource(R.drawable.button_bg);
        btnDatePicker.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        btnLp.setMargins(0, 14, 0, 0);
        btnDatePicker.setLayoutParams(btnLp);

        final Calendar calendar = Calendar.getInstance();
        final SimpleDateFormat dateOnlyFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        btnDatePicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new DatePickerDialog(MainActivity.this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        calendar.set(Calendar.YEAR, year);
                        calendar.set(Calendar.MONTH, month);
                        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        btnDatePicker.setText("Due: " + dateOnlyFormat.format(calendar.getTime()));
                    }
                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
            }
        });
        layout.addView(btnDatePicker);

        builder.setView(layout);
        builder.setPositiveButton("Create Task", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String taskTitle = inputTitle.getText().toString().trim();
                String hoursText = inputHours.getText().toString().trim();
                int selectedPosition = subjectSpinner.getSelectedItemPosition();

                if (taskTitle.isEmpty() || selectedPosition < 0) {
                    Toast.makeText(MainActivity.this, "Check entered parameters", Toast.LENGTH_SHORT).show();
                    return;
                }

                double hoursVal = 1.0;
                try {
                    hoursVal = Double.parseDouble(hoursText);
                } catch (NumberFormatException e) {
                    // fall back
                }

                int subId = subjectIdsList.get(selectedPosition);
                String formattedDueDate = dateOnlyFormat.format(calendar.getTime());

                boolean success = dbHelper.addTask(subId, taskTitle, formattedDueDate, hoursVal);
                if (success) {
                    Toast.makeText(MainActivity.this, "Study task assigned!", Toast.LENGTH_SHORT).show();
                    refreshAllData();
                } else {
                    Toast.makeText(MainActivity.this, "Failed saving task", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showQuickAddTaskDialog() {
        // Toggle view tab first to ensure smooth layout UX
        switchTab(2);
        showAddTaskDialog();
    }

    // Dynamic reminder configuration dialog connected to AlarmManager alerts
    private void showAddReminderDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Set Dynamic Study Alert");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(24, 24, 24, 24);

        final EditText inputTitle = new EditText(this);
        inputTitle.setHint("Alarm Alert Message (e.g., Study Chemistry)");
        layout.addView(inputTitle);

        final Button dateBtn = new Button(this);
        dateBtn.setText("Choose Date");
        dateBtn.setBackgroundResource(R.drawable.button_bg);
        dateBtn.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams lpDate = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpDate.setMargins(0, 16, 0, 8);
        dateBtn.setLayoutParams(lpDate);

        final Button timeBtn = new Button(this);
        timeBtn.setText("Choose Time");
        timeBtn.setBackgroundResource(R.drawable.button_bg);
        timeBtn.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams lpTime = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpTime.setMargins(0, 4, 0, 16);
        timeBtn.setLayoutParams(lpTime);

        final Calendar scheduleCalendar = Calendar.getInstance();
        final SimpleDateFormat fullFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

        dateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new DatePickerDialog(MainActivity.this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        scheduleCalendar.set(Calendar.YEAR, year);
                        scheduleCalendar.set(Calendar.MONTH, month);
                        scheduleCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        dateBtn.setText("Date: " + new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(scheduleCalendar.getTime()));
                    }
                }, scheduleCalendar.get(Calendar.YEAR), scheduleCalendar.get(Calendar.MONTH), scheduleCalendar.get(Calendar.DAY_OF_MONTH)).show();
            }
        });

        timeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new TimePickerDialog(MainActivity.this, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        scheduleCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        scheduleCalendar.set(Calendar.MINUTE, minute);
                        scheduleCalendar.set(Calendar.SECOND, 0);
                        timeBtn.setText("Time: " + String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute));
                    }
                }, scheduleCalendar.get(Calendar.HOUR_OF_DAY), scheduleCalendar.get(Calendar.MINUTE), true).show();
            }
        });

        layout.addView(dateBtn);
        layout.addView(timeBtn);

        builder.setView(layout);
        builder.setPositiveButton("Schedule Alarm", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String title = inputTitle.getText().toString().trim();
                if (title.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Reminder alert is empty!", Toast.LENGTH_SHORT).show();
                    return;
                }

                long alertTime = scheduleCalendar.getTimeInMillis();
                if (alertTime < System.currentTimeMillis()) {
                    Toast.makeText(MainActivity.this, "Cannot schedule in the past!", Toast.LENGTH_SHORT).show();
                    return;
                }

                boolean done = dbHelper.addReminder(title, alertTime);
                if (done) {
                    setSystemAlarm(title, alertTime);
                    Toast.makeText(MainActivity.this, "Alert programmed successfully!", Toast.LENGTH_SHORT).show();
                    refreshAllData();
                } else {
                    Toast.makeText(MainActivity.this, "Database storage failure", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("Close", null);
        builder.show();
    }

    private void setSystemAlarm(String title, long timeMillis) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            Intent intent = new Intent(this, AlarmReceiver.class);
            intent.putExtra("title", title);
            int code = (int) timeMillis;
            PendingIntent pendingIntent = PendingIntent.getBroadcast(this, code, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            alarmManager.set(AlarmManager.RTC_WAKEUP, timeMillis, pendingIntent);
        }
    }

    private void cancelSystemAlarm(String title, long timeMillis) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            Intent intent = new Intent(this, AlarmReceiver.class);
            intent.putExtra("title", title);
            int code = (int) timeMillis;
            PendingIntent pendingIntent = PendingIntent.getBroadcast(this, code, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            alarmManager.cancel(pendingIntent);
        }
    }

    // BASE NATIVE CURSOR ADAPTERS IMPLEMENTED COMPATIBLY

    private class SubjectAdapter extends BaseAdapter {
        private Context context;
        private Cursor cursor;

        public SubjectAdapter(Context context, Cursor cursor) {
            this.context = context;
            this.cursor = cursor;
        }

        public void changeCursor(Cursor newCursor) {
            if (this.cursor != null) {
                this.cursor.close();
            }
            this.cursor = newCursor;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return cursor != null ? cursor.getCount() : 0;
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_2, parent, false);
            }

            cursor.moveToPosition(position);

            int idIdx = cursor.getColumnIndex(DatabaseHelper.COL_SUB_ID);
            int nameIdx = cursor.getColumnIndex(DatabaseHelper.COL_SUB_NAME);
            int colorIdx = cursor.getColumnIndex(DatabaseHelper.COL_SUB_COLOR);

            if (idIdx != -1 && nameIdx != -1 && colorIdx != -1) {
                final int subId = cursor.getInt(idIdx);
                final String subName = cursor.getString(nameIdx);
                final String subColor = cursor.getString(colorIdx);

                TextView title = (TextView) convertView.findViewById(android.R.id.text1);
                TextView sub = (TextView) convertView.findViewById(android.R.id.text2);

                title.setText(subName);
                title.setTextColor(Color.parseColor(subColor));
                title.setTextStyle(android.graphics.Typeface.BOLD);

                sub.setText("Long-press to delete this Subject entirely");
                sub.setTextColor(Color.GRAY);

                convertView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        new AlertDialog.Builder(context)
                                .setTitle("Delete Subject")
                                .setMessage("Are you sure you want to delete '" + subName + "'? All associated tasks will be removed.")
                                .setPositiveButton("Yes, Delete", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dbHelper.deleteSubject(subId);
                                        refreshAllData();
                                    }
                                })
                                .setNegativeButton("No", null)
                                .show();
                        return true;
                    }
                });
            }

            return convertView;
        }
    }

    private class TaskAdapter extends BaseAdapter {
        private Context context;
        private Cursor cursor;

        public TaskAdapter(Context context, Cursor cursor) {
            this.context = context;
            this.cursor = cursor;
        }

        public void changeCursor(Cursor newCursor) {
            if (this.cursor != null) {
                this.cursor.close();
            }
            this.cursor = newCursor;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return cursor != null ? cursor.getCount() : 0;
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                // Dynamically inflate pure item container without separate files for bulletproof compiles
                LinearLayout taskRow = new LinearLayout(context);
                taskRow.setOrientation(LinearLayout.HORIZONTAL);
                taskRow.setPadding(16, 16, 16, 16);
                taskRow.setGravity(android.view.Gravity.CENTER_VERTICAL);

                CheckBox chk = new CheckBox(context);
                chk.setId(View.generateViewId());
                chk.setTag("chk");

                LinearLayout info = new LinearLayout(context);
                info.setOrientation(LinearLayout.VERTICAL);
                info.setPadding(16, 0, 16, 0);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
                info.setLayoutParams(lp);

                TextView tTitle = new TextView(context);
                tTitle.setTextSize(15);
                tTitle.setId(View.generateViewId());
                tTitle.setTag("title");

                TextView tSub = new TextView(context);
                tSub.setTextSize(11);
                tSub.setId(View.generateViewId());
                tSub.setTag("subtitle");

                info.addView(tTitle);
                info.addView(tSub);

                Button del = new Button(context);
                del.setText("X");
                del.setTextColor(Color.RED);
                del.setBackgroundColor(Color.TRANSPARENT);
                del.setTag("delete");
                del.setLayoutParams(new LinearLayout.LayoutParams(60, LinearLayout.LayoutParams.WRAP_CONTENT));

                taskRow.addView(chk);
                taskRow.addView(info);
                taskRow.addView(del);

                convertView = taskRow;
            }

            cursor.moveToPosition(position);

            int idIdx = cursor.getColumnIndex(DatabaseHelper.COL_TASK_ID);
            int titleIdx = cursor.getColumnIndex(DatabaseHelper.COL_TASK_TITLE);
            int completedIdx = cursor.getColumnIndex(DatabaseHelper.COL_TASK_COMPLETED);
            int subNameIdx = cursor.getColumnIndex(DatabaseHelper.COL_SUB_NAME);
            int subColorIdx = cursor.getColumnIndex(DatabaseHelper.COL_SUB_COLOR);
            int dueIdx = cursor.getColumnIndex(DatabaseHelper.COL_TASK_DUE);
            int hoursIdx = cursor.getColumnIndex(DatabaseHelper.COL_TASK_HOURS);

            if (idIdx != -1 && titleIdx != -1 && completedIdx != -1) {
                final int taskId = cursor.getInt(idIdx);
                final String taskTitle = cursor.getString(titleIdx);
                final int completed = cursor.getInt(completedIdx);
                final String subjectName = (subNameIdx != -1 && cursor.getString(subNameIdx) != null) ? cursor.getString(subNameIdx) : "General";
                final String colorStr = (subColorIdx != -1 && cursor.getString(subColorIdx) != null) ? cursor.getString(subColorIdx) : "#757575";
                final String dueDateStr = (dueIdx != -1) ? cursor.getString(dueIdx) : "";
                final double hoursEst = (hoursIdx != -1) ? cursor.getDouble(hoursIdx) : 1.0;

                CheckBox chk = (CheckBox) convertView.findViewWithTag("chk");
                TextView title = (TextView) convertView.findViewWithTag("title");
                TextView subtitle = (TextView) convertView.findViewWithTag("subtitle");
                Button deleteBtn = (Button) convertView.findViewWithTag("delete");

                title.setText(taskTitle);
                subtitle.setText(subjectName + " • " + hoursEst + " Hrs • Due: " + dueDateStr);
                subtitle.setTextColor(Color.parseColor(colorStr));

                // Unregister listener first to avoid recursion triggers
                chk.setOnCheckedChangeListener(null);
                chk.setChecked(completed == 1);

                if (completed == 1) {
                    title.setPaintFlags(title.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
                    title.setTextColor(Color.GRAY);
                } else {
                    title.setPaintFlags(title.getPaintFlags() & (~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG));
                    title.setTextColor(Color.BLACK);
                }

                chk.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(android.widget.CompoundButton buttonView, boolean isChecked) {
                        dbHelper.updateTaskCompletion(taskId, isChecked);
                        refreshAllData();
                    }
                });

                deleteBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dbHelper.deleteTask(taskId);
                        refreshAllData();
                    }
                });
            }

            return convertView;
        }
    }

    private class ReminderAdapter extends BaseAdapter {
        private Context context;
        private Cursor cursor;

        public ReminderAdapter(Context context, Cursor cursor) {
            this.context = context;
            this.cursor = cursor;
        }

        public void changeCursor(Cursor newCursor) {
            if (this.cursor != null) {
                this.cursor.close();
            }
            this.cursor = newCursor;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return cursor != null ? cursor.getCount() : 0;
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_2, parent, false);
            }

            cursor.moveToPosition(position);

            int idIdx = cursor.getColumnIndex(DatabaseHelper.COL_REM_ID);
            int titleIdx = cursor.getColumnIndex(DatabaseHelper.COL_REM_TITLE);
            int timeIdx = cursor.getColumnIndex(DatabaseHelper.COL_REM_TIME);

            if (idIdx != -1 && titleIdx != -1 && timeIdx != -1) {
                final int remId = cursor.getInt(idIdx);
                final String titleStr = cursor.getString(titleIdx);
                final long timeMillis = cursor.getLong(timeIdx);

                TextView title = (TextView) convertView.findViewById(android.R.id.text1);
                TextView sub = (TextView) convertView.findViewById(android.R.id.text2);

                title.setText(titleStr);
                title.setTextStyle(android.graphics.Typeface.BOLD);

                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd 'at' hh:mm a", Locale.getDefault());
                sub.setText("Scheduled: " + format.format(new Date(timeMillis)) + "\n(Long-press to cancel alert)");

                convertView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        new AlertDialog.Builder(context)
                                .setTitle("Cancel Alert")
                                .setMessage("Do you want to cancel and delete the alert reminder '" + titleStr + "'?")
                                .setPositiveButton("Remove Alert", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        cancelSystemAlarm(titleStr, timeMillis);
                                        dbHelper.deleteReminder(remId);
                                        refreshAllData();
                                        Toast.makeText(context, "Alert removed!", Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .setNegativeButton("Keep", null)
                                .show();
                        return true;
                    }
                });
            }

            return convertView;
        }
    }

    @Override
    protected void onDestroy() {
        if (dbHelper != null) {
            dbHelper.close();
        }
        super.onDestroy();
    }
}