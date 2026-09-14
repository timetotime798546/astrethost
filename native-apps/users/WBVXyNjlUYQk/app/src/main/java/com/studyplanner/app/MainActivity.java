package com.studyplanner.app;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
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
import java.util.Random;

public class MainActivity extends Activity {

    // Database helper instance
    private StudyDatabaseHelper dbHelper;

    // View panels representing each section tab
    private ScrollView panelDashboard;
    private ScrollView panelSubjects;
    private ScrollView panelTasks;
    private ScrollView panelReminders;

    // Custom tab text indicators acting as tab selectors
    private TextView tabDashboardBtn;
    private TextView tabSubjectsBtn;
    private TextView tabTasksBtn;
    private TextView tabRemindersBtn;

    // Dynamic lists inside panels
    private LinearLayout subjectsContainer;
    private LinearLayout tasksContainer;
    private LinearLayout remindersContainer;
    private LinearLayout dashboardTasksList;

    // Stat metric elements
    private ProgressBar dashboardProgressBar;
    private TextView dashboardProgressText;
    private TextView statSubjectsCount;
    private TextView statTasksPending;
    private TextView statRemindersActive;
    private TextView focusQuoteText;

    // Filters and setup options
    private Spinner spinnerTaskFilter;
    private Button btnAddTask;
    private Button btnAddSubject;
    private Button btnAddReminder;
    private Button dashQuickTaskBtn;

    // String collections for spinner and dynamic styles
    private final String[] PRIORITY_OPTIONS = {"High", "Medium", "Low"};
    private final String[] COLOR_OPTIONS = {"Blue", "Green", "Orange", "Purple", "Red"};
    private final String[] COLOR_HEX_VALUES = {"#2196F3", "#4CAF50", "#FF9800", "#9C27B0", "#E91E63"};

    // Active quotes selection
    private final String[] STUDENT_MOTIVATIONAL_QUOTES = {
            "Determine your goals. Systematic study is the highest leverage tool.",
            "You do not have to be excellent to begin, but you must begin to be excellent.",
            "Focus entirely on the action directly ahead. Micro accomplishments generate macro results.",
            "Your intellectual curiosity shapes your absolute limits. Exceed them daily.",
            "Consistent daily focus beats sporadic cram sessions with total ease."
    };

    // Helper calendars for custom picker callbacks
    private Calendar taskCalendarHelper = Calendar.getInstance();
    private Calendar reminderCalendarHelper = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Database Handler
        dbHelper = new StudyDatabaseHelper(this);

        // Initialize View Panel Elements
        panelDashboard = findViewById(R.id.panel_dashboard);
        panelSubjects = findViewById(R.id.panel_subjects);
        panelTasks = findViewById(R.id.panel_tasks);
        panelReminders = findViewById(R.id.panel_reminders);

        // Initialize Navigation Switch Triggers
        tabDashboardBtn = findViewById(R.id.tab_dashboard);
        tabSubjectsBtn = findViewById(R.id.tab_subjects);
        tabTasksBtn = findViewById(R.id.tab_tasks);
        tabRemindersBtn = findViewById(R.id.tab_reminders);

        // Initialize Dynamic Layout Container Holders
        subjectsContainer = findViewById(R.id.subjects_container);
        tasksContainer = findViewById(R.id.tasks_container);
        remindersContainer = findViewById(R.id.reminders_container);
        dashboardTasksList = findViewById(R.id.dashboard_tasks_list);

        // Initialize Metric Views
        dashboardProgressBar = findViewById(R.id.dashboard_progress_bar);
        dashboardProgressText = findViewById(R.id.dashboard_progress_text);
        statSubjectsCount = findViewById(R.id.stat_subjects_count);
        statTasksPending = findViewById(R.id.stat_tasks_pending);
        statRemindersActive = findViewById(R.id.stat_reminders_active);
        focusQuoteText = findViewById(R.id.focus_quote_text);

        // Filter and buttons
        spinnerTaskFilter = findViewById(R.id.spinner_task_filter);
        btnAddTask = findViewById(R.id.btn_add_task);
        btnAddSubject = findViewById(R.id.btn_add_subject);
        btnAddReminder = findViewById(R.id.btn_add_reminder);
        dashQuickTaskBtn = findViewById(R.id.dash_quick_task_btn);

        // Setup filter dropdown
        String[] filters = {"All", "High", "Medium", "Low"};
        ArrayAdapter<String> filterAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, filters);
        spinnerTaskFilter.setAdapter(filterAdapter);

        // Initialize interactive events
        setupNavListeners();
        setupClickEvents();
        checkPermissions();

        // Load initial values
        refreshAllPanels();
        randomizeFocusQuote();
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 202);
            }
        }
    }

    private void randomizeFocusQuote() {
        int index = new Random().nextInt(STUDENT_MOTIVATIONAL_QUOTES.length);
        focusQuoteText.setText(STUDENT_MOTIVATIONAL_QUOTES[index]);
    }

    private void setupNavListeners() {
        tabDashboardBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPanel(panelDashboard, tabDashboardBtn);
            }
        });

        tabSubjectsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPanel(panelSubjects, tabSubjectsBtn);
            }
        });

        tabTasksBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPanel(panelTasks, tabTasksBtn);
            }
        });

        tabRemindersBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPanel(panelReminders, tabRemindersBtn);
            }
        });
    }

    private void showPanel(ScrollView activePanel, TextView activeTab) {
        // Hide all screens
        panelDashboard.setVisibility(View.GONE);
        panelSubjects.setVisibility(View.GONE);
        panelTasks.setVisibility(View.GONE);
        panelReminders.setVisibility(View.GONE);

        // Unselect tab colors
        tabDashboardBtn.setBackgroundResource(R.drawable.tab_unselected);
        tabDashboardBtn.setTextColor(getResources().getColor(R.color.text_dark));
        tabSubjectsBtn.setBackgroundResource(R.drawable.tab_unselected);
        tabSubjectsBtn.setTextColor(getResources().getColor(R.color.text_dark));
        tabTasksBtn.setBackgroundResource(R.drawable.tab_unselected);
        tabTasksBtn.setTextColor(getResources().getColor(R.color.text_dark));
        tabRemindersBtn.setBackgroundResource(R.drawable.tab_unselected);
        tabRemindersBtn.setTextColor(getResources().getColor(R.color.text_dark));

        // Show targets
        activePanel.setVisibility(View.VISIBLE);
        activeTab.setBackgroundResource(R.drawable.tab_selected);
        activeTab.setTextColor(getResources().getColor(R.color.white));

        // Force reload state
        refreshAllPanels();
    }

    private void setupClickEvents() {
        btnAddSubject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                displayAddSubjectDialog();
            }
        });

        btnAddTask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                displayAddTaskDialog();
            }
        });

        dashQuickTaskBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                displayAddTaskDialog();
            }
        });

        btnAddReminder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                displayAddReminderDialog();
            }
        });

        spinnerTaskFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                renderTasks();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void refreshAllPanels() {
        renderDashboard();
        renderSubjects();
        renderTasks();
        renderReminders();
    }

    // RENDER: DASHBOARD VIEW STATS & PRIORITIES
    private void renderDashboard() {
        Cursor subCursor = dbHelper.getAllSubjects();
        int subjectsCount = subCursor.getCount();
        statSubjectsCount.setText(String.valueOf(subjectsCount));
        subCursor.close();

        Cursor taskCursor = dbHelper.getAllTasks();
        int totalTasks = taskCursor.getCount();
        int completedTasks = 0;
        int pendingTasks = 0;

        dashboardTasksList.removeAllViews();

        if (totalTasks > 0) {
            while (taskCursor.moveToNext()) {
                int isCompleted = taskCursor.getInt(taskCursor.getColumnIndexOrThrow("is_completed"));
                if (isCompleted == 1) {
                    completedTasks++;
                } else {
                    pendingTasks++;
                    
                    // Render up to 4 pending high priorities on dashboard panel
                    if (dashboardTasksList.getChildCount() < 4) {
                        final int id = taskCursor.getInt(taskCursor.getColumnIndexOrThrow("id"));
                        String title = taskCursor.getString(taskCursor.getColumnIndexOrThrow("title"));
                        String priority = taskCursor.getString(taskCursor.getColumnIndexOrThrow("priority"));
                        String date = taskCursor.getString(taskCursor.getColumnIndexOrThrow("due_date"));
                        String subjName = taskCursor.getString(taskCursor.getColumnIndexOrThrow("subj_name"));
                        String colorStr = taskCursor.getString(taskCursor.getColumnIndexOrThrow("subj_color"));

                        LinearLayout row = createDashboardTaskRow(id, title, priority, date, subjName, colorStr);
                        dashboardTasksList.addView(row);
                    }
                }
            }
        }
        taskCursor.close();

        statTasksPending.setText(String.valueOf(pendingTasks));

        // Calculate custom percentage progress
        int progressPercent = 0;
        if (totalTasks > 0) {
            progressPercent = (completedTasks * 100) / totalTasks;
        }
        dashboardProgressBar.setProgress(progressPercent);
        dashboardProgressText.setText(progressPercent + "% Tasks Completed (" + completedTasks + "/" + totalTasks + ")");

        Cursor reminderCursor = dbHelper.getAllReminders();
        statRemindersActive.setText(String.valueOf(reminderCursor.getCount()));
        reminderCursor.close();
    }

    // RENDER: SUBJECTS PANEL
    private void renderSubjects() {
        subjectsContainer.removeAllViews();
        Cursor cursor = dbHelper.getAllSubjects();

        if (cursor.getCount() == 0) {
            TextView emptyText = new TextView(this);
            emptyText.setText("No active subjects yet. Add one to link study tasks.");
            emptyText.setTextColor(getResources().getColor(R.color.text_muted));
            emptyText.setGravity(Gravity.CENTER);
            emptyText.setPadding(0, 40, 0, 40);
            subjectsContainer.addView(emptyText);
        } else {
            while (cursor.moveToNext()) {
                final int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                final String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                final String color = cursor.getString(cursor.getColumnIndexOrThrow("color"));
                final int hours = cursor.getInt(cursor.getColumnIndexOrThrow("hours"));

                LinearLayout card = new LinearLayout(this);
                card.setOrientation(LinearLayout.HORIZONTAL);
                card.setBackgroundResource(R.drawable.card_background);
                card.setPadding(16, 16, 16, 16);
                
                LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                cardParams.setMargins(0, 0, 0, 12);
                card.setLayoutParams(cardParams);
                card.setGravity(Gravity.CENTER_VERTICAL);

                // Subject Color Circle Pill indicator
                View colorPill = new View(this);
                LinearLayout.LayoutParams pillParams = new LinearLayout.LayoutParams(16, 60);
                pillParams.setMargins(0, 0, 16, 0);
                colorPill.setLayoutParams(pillParams);
                colorPill.setBackgroundColor(Color.parseColor(resolveHex(color)));
                card.addView(colorPill);

                // Information details
                LinearLayout infoCol = new LinearLayout(this);
                infoCol.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
                infoCol.setLayoutParams(infoParams);

                TextView nameView = new TextView(this);
                nameView.setText(name);
                nameView.setTextSize(16sp);
                nameView.setTextColor(getResources().getColor(R.color.text_dark));
                nameView.textStyle = android.graphics.Typeface.BOLD;
                infoCol.addView(nameView);

                TextView targetView = new TextView(this);
                targetView.setText("Goal: " + hours + " Hours study time per week.");
                targetView.setTextColor(getResources().getColor(R.color.text_muted));
                targetView.setTextSize(13sp);
                infoCol.addView(targetView);

                card.addView(infoCol);

                // Delete Action Button
                TextView deleteBtn = new TextView(this);
                deleteBtn.setText("Delete");
                deleteBtn.setTextColor(Color.parseColor("#E53935"));
                deleteBtn.setPadding(16, 12, 16, 12);
                deleteBtn.setTextSize(13sp);
                deleteBtn.setClickable(true);
                deleteBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle("Delete Subject?")
                                .setMessage("This action will also permanently delete all linked tasks.")
                                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dbHelper.deleteSubject(id);
                                        refreshAllPanels();
                                    }
                                })
                                .setNegativeButton("Cancel", null)
                                .show();
                    }
                });
                card.addView(deleteBtn);

                subjectsContainer.addView(card);
            }
        }
        cursor.close();
    }

    // RENDER: TASKS PANEL
    private void renderTasks() {
        tasksContainer.removeAllViews();
        String selectedFilter = spinnerTaskFilter.getSelectedItem() != null ? spinnerTaskFilter.getSelectedItem().toString() : "All";
        Cursor cursor;

        if (selectedFilter.equals("All")) {
            cursor = dbHelper.getAllTasks();
        } else {
            cursor = dbHelper.getTasksFiltered(selectedFilter);
        }

        if (cursor.getCount() == 0) {
            TextView emptyText = new TextView(this);
            emptyText.setText("No matching tasks. Touch Add Task to plan your work!");
            emptyText.setTextColor(getResources().getColor(R.color.text_muted));
            emptyText.setGravity(Gravity.CENTER);
            emptyText.setPadding(0, 40, 0, 40);
            tasksContainer.addView(emptyText);
        } else {
            while (cursor.moveToNext()) {
                final int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                final String title = cursor.getString(cursor.getColumnIndexOrThrow("title"));
                final String priority = cursor.getString(cursor.getColumnIndexOrThrow("priority"));
                final String date = cursor.getString(cursor.getColumnIndexOrThrow("due_date"));
                final int isCompleted = cursor.getInt(cursor.getColumnIndexOrThrow("is_completed"));
                final String subjName = cursor.getString(cursor.getColumnIndexOrThrow("subj_name"));
                final String colorStr = cursor.getString(cursor.getColumnIndexOrThrow("subj_color"));

                LinearLayout card = new LinearLayout(this);
                card.setOrientation(LinearLayout.HORIZONTAL);
                card.setBackgroundResource(R.drawable.card_background);
                card.setPadding(12, 12, 12, 12);

                LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                cardParams.setMargins(0, 0, 0, 10);
                card.setLayoutParams(cardParams);
                card.setGravity(Gravity.CENTER_VERTICAL);

                // Completion Checkbox
                CheckBox check = new CheckBox(this);
                check.setChecked(isCompleted == 1);
                check.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dbHelper.updateTaskStatus(id, check.isChecked() ? 1 : 0);
                        refreshAllPanels();
                    }
                });
                card.addView(check);

                // Task Information Col
                LinearLayout taskInfoCol = new LinearLayout(this);
                taskInfoCol.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
                taskInfoCol.setLayoutParams(infoParams);

                TextView titleText = new TextView(this);
                titleText.setText(title);
                titleText.setTextSize(15sp);
                titleText.setTextColor(getResources().getColor(R.color.text_dark));
                if (isCompleted == 1) {
                    titleText.setPaintFlags(titleText.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
                    titleText.setTextColor(getResources().getColor(R.color.text_muted));
                }
                taskInfoCol.addView(titleText);

                // Meta row: Subj name with priority badge
                LinearLayout metaRow = new LinearLayout(this);
                metaRow.setOrientation(LinearLayout.HORIZONTAL);
                metaRow.setPadding(0, 4, 0, 0);

                if (subjName != null) {
                    TextView tag = new TextView(this);
                    tag.setText(subjName);
                    tag.setTextSize(11sp);
                    tag.setTextColor(Color.WHITE);
                    tag.setBackgroundColor(Color.parseColor(resolveHex(colorStr)));
                    tag.setPadding(8, 2, 8, 2);
                    metaRow.addView(tag);
                }

                TextView priorityBadge = new TextView(this);
                priorityBadge.setText(" " + priority + " priority");
                priorityBadge.setTextSize(11sp);
                priorityBadge.setTextColor(resolvePriorityColor(priority));
                priorityBadge.setPadding(8, 2, 8, 2);
                metaRow.addView(priorityBadge);

                TextView dateTag = new TextView(this);
                dateTag.setText(" | Due: " + date);
                dateTag.setTextSize(11sp);
                dateTag.setTextColor(getResources().getColor(R.color.text_muted));
                metaRow.addView(dateTag);

                taskInfoCol.addView(metaRow);
                card.addView(taskInfoCol);

                // Delete Button
                TextView deleteText = new TextView(this);
                deleteText.setText("❌");
                deleteText.setPadding(12, 12, 12, 12);
                deleteText.setClickable(true);
                deleteText.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dbHelper.deleteTask(id);
                        refreshAllPanels();
                    }
                });
                card.addView(deleteText);

                tasksContainer.addView(card);
            }
        }
        cursor.close();
    }

    // RENDER: REMINDERS PANEL
    private void renderReminders() {
        remindersContainer.removeAllViews();
        Cursor cursor = dbHelper.getAllReminders();

        if (cursor.getCount() == 0) {
            TextView emptyText = new TextView(this);
            emptyText.setText("No scheduled alarms. Plan key revision sessions.");
            emptyText.setTextColor(getResources().getColor(R.color.text_muted));
            emptyText.setGravity(Gravity.CENTER);
            emptyText.setPadding(0, 40, 0, 40);
            remindersContainer.addView(emptyText);
        } else {
            while (cursor.moveToNext()) {
                final int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                final String title = cursor.getString(cursor.getColumnIndexOrThrow("title"));
                final long timeStamp = cursor.getLong(cursor.getColumnIndexOrThrow("time_stamp"));

                LinearLayout card = new LinearLayout(this);
                card.setOrientation(LinearLayout.HORIZONTAL);
                card.setBackgroundResource(R.drawable.card_background);
                card.setPadding(14, 14, 14, 14);

                LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                cardParams.setMargins(0, 0, 0, 10);
                card.setLayoutParams(cardParams);
                card.setGravity(Gravity.CENTER_VERTICAL);

                // Timer Visual pill icon
                TextView alarmIcon = new TextView(this);
                alarmIcon.setText("⏰");
                alarmIcon.setTextSize(20sp);
                alarmIcon.setPadding(0, 0, 12, 0);
                card.addView(alarmIcon);

                // Title + Date details
                LinearLayout infoCol = new LinearLayout(this);
                infoCol.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
                infoCol.setLayoutParams(infoParams);

                TextView titleText = new TextView(this);
                titleText.setText(title);
                titleText.setTextSize(15sp);
                titleText.setTextColor(getResources().getColor(R.color.text_dark));
                infoCol.addView(titleText);

                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy @ hh:mm a", Locale.getDefault());
                TextView timeText = new TextView(this);
                timeText.setText("Alarm scheduled: " + sdf.format(new Date(timeStamp)));
                timeText.setTextSize(12sp);
                timeText.setTextColor(getResources().getColor(R.color.text_muted));
                infoCol.addView(timeText);

                card.addView(infoCol);

                // Cancel Alarm action
                TextView cancelBtn = new TextView(this);
                cancelBtn.setText("Cancel");
                cancelBtn.setTextColor(Color.parseColor("#D32F2F"));
                cancelBtn.setPadding(12, 12, 12, 12);
                cancelBtn.setClickable(true);
                cancelBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        cancelSystemAlarm(id, title);
                        dbHelper.deleteReminder(id);
                        refreshAllPanels();
                        Toast.makeText(MainActivity.this, "Reminder alert deactivated", Toast.LENGTH_SHORT).show();
                    }
                });
                card.addView(cancelBtn);

                remindersContainer.addView(card);
            }
        }
        cursor.close();
    }

    // Helper constructor to generate a Priority Task Item dynamically for the Dashboard
    private LinearLayout createDashboardTaskRow(final int id, String title, String priority, String date, String subjName, String colorStr) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setBackgroundResource(R.drawable.card_background);
        card.setPadding(12, 12, 12, 12);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, 8);
        card.setLayoutParams(cardParams);
        card.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout textLayout = new LinearLayout(this);
        textLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        textLayout.setLayoutParams(textParams);

        TextView titleText = new TextView(this);
        titleText.setText(title);
        titleText.setTextSize(14sp);
        titleText.setTextColor(getResources().getColor(R.color.text_dark));
        textLayout.addView(titleText);

        LinearLayout tagLayout = new LinearLayout(this);
        tagLayout.setOrientation(LinearLayout.HORIZONTAL);
        tagLayout.setPadding(0, 4, 0, 0);

        if (subjName != null) {
            TextView tag = new TextView(this);
            tag.setText(subjName);
            tag.setTextSize(10sp);
            tag.setTextColor(Color.WHITE);
            tag.setBackgroundColor(Color.parseColor(resolveHex(colorStr)));
            tag.setPadding(6, 1, 6, 1);
            tagLayout.addView(tag);
        }

        TextView alertText = new TextView(this);
        alertText.setText(" " + priority + " | Due: " + date);
        alertText.setTextSize(11sp);
        alertText.setTextColor(resolvePriorityColor(priority));
        tagLayout.addView(alertText);

        textLayout.addView(tagLayout);
        card.addView(textLayout);

        // Simple action to complete directly from dashboard
        Button doneBtn = new Button(this);
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(110, 64);
        doneBtn.setLayoutParams(btnParams);
        doneBtn.setText("✔");
        doneBtn.setBackgroundResource(R.drawable.button_background);
        doneBtn.setTextColor(Color.WHITE);
        doneBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dbHelper.updateTaskStatus(id, 1);
                refreshAllPanels();
                Toast.makeText(MainActivity.this, "Completed study task!", Toast.LENGTH_SHORT).show();
            }
        });
        card.addView(doneBtn);

        return card;
    }

    // COLOR RESOLVER UTILS
    private String resolveHex(String input) {
        if (input == null) return "#2196F3";
        for (int i = 0; i < COLOR_OPTIONS.length; i++) {
            if (COLOR_OPTIONS[i].equalsIgnoreCase(input)) {
                return COLOR_HEX_VALUES[i];
            }
        }
        return "#2196F3";
    }

    private int resolvePriorityColor(String priority) {
        if ("High".equalsIgnoreCase(priority)) {
            return getResources().getColor(R.color.priority_high);
        } else if ("Medium".equalsIgnoreCase(priority)) {
            return getResources().getColor(R.color.priority_medium);
        } else {
            return getResources().getColor(R.color.priority_low);
        }
    }

    // DIALOGS & CAPTURING USER INPUTS
    private void displayAddSubjectDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create New Subject");

        View view = getLayoutInflater().inflate(R.layout.dialog_subject, null);
        final EditText nameInput = view.findViewById(R.id.edit_subject_name);
        final EditText hoursInput = view.findViewById(R.id.edit_subject_hours);
        final Spinner colorSpinner = view.findViewById(R.id.spinner_subject_color);

        // Fill color picker spinner
        ArrayAdapter<String> colorAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, COLOR_OPTIONS);
        colorSpinner.setAdapter(colorAdapter);

        builder.setView(view);
        builder.setPositiveButton("Create", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String name = nameInput.getText().toString().trim();
                String hoursStr = hoursInput.getText().toString().trim();
                String selectedColor = colorSpinner.getSelectedItem().toString();

                if (name.isEmpty() || hoursStr.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Input values are empty!", Toast.LENGTH_SHORT).show();
                    return;
                }

                int hours = Integer.parseInt(hoursStr);
                dbHelper.insertSubject(name, selectedColor, hours);
                refreshAllPanels();
                Toast.makeText(MainActivity.this, "Subject added successfully", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void displayAddTaskDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Study Task");

        View view = getLayoutInflater().inflate(R.layout.dialog_task, null);
        final EditText titleInput = view.findViewById(R.id.edit_task_title);
        final Spinner subjectSpinner = view.findViewById(R.id.spinner_task_subject);
        final Spinner prioritySpinner = view.findViewById(R.id.spinner_task_priority);
        final Button pickDateBtn = view.findViewById(R.id.btn_pick_task_date);
        final TextView selectedDateText = view.findViewById(R.id.text_selected_task_date);

        // Populate dynamic priorities
        ArrayAdapter<String> prioAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, PRIORITY_OPTIONS);
        prioritySpinner.setAdapter(prioAdapter);

        // Fetch subjects for select linker dropdown
        Cursor cursor = dbHelper.getAllSubjects();
        final ArrayList<Integer> subjectIds = new ArrayList<>();
        ArrayList<String> subjectNames = new ArrayList<>();

        while (cursor.moveToNext()) {
            subjectIds.add(cursor.getInt(cursor.getColumnIndexOrThrow("id")));
            subjectNames.add(cursor.getString(cursor.getColumnIndexOrThrow("name")));
        }
        cursor.close();

        if (subjectNames.isEmpty()) {
            Toast.makeText(this, "Please create at least one study subject first!", Toast.LENGTH_LONG).show();
            showPanel(panelSubjects, tabSubjectsBtn);
            return;
        }

        ArrayAdapter<String> subjAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, subjectNames);
        subjectSpinner.setAdapter(subjAdapter);

        // Date selection event
        pickDateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatePickerDialog datePicker = new DatePickerDialog(MainActivity.this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        taskCalendarHelper.set(Calendar.YEAR, year);
                        taskCalendarHelper.set(Calendar.MONTH, month);
                        taskCalendarHelper.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                        selectedDateText.setText(sdf.format(taskCalendarHelper.getTime()));
                    }
                }, taskCalendarHelper.get(Calendar.YEAR), taskCalendarHelper.get(Calendar.MONTH), taskCalendarHelper.get(Calendar.DAY_OF_MONTH));
                datePicker.show();
            }
        });

        builder.setView(view);
        builder.setPositiveButton("Plan Task", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String title = titleInput.getText().toString().trim();
                String dateStr = selectedDateText.getText().toString();
                String priorityStr = prioritySpinner.getSelectedItem().toString();

                if (title.isEmpty() || dateStr.equals("No date selected")) {
                    Toast.makeText(MainActivity.this, "Fill in all required values", Toast.LENGTH_SHORT).show();
                    return;
                }

                int chosenSubIndex = subjectSpinner.getSelectedItemPosition();
                int linkedSubjectId = subjectIds.get(chosenSubIndex);

                dbHelper.insertTask(linkedSubjectId, title, dateStr, priorityStr);
                refreshAllPanels();
                Toast.makeText(MainActivity.this, "Planned study target task", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void displayAddReminderDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Set Study Session Alarm");

        View view = getLayoutInflater().inflate(R.layout.dialog_reminder, null);
        final EditText titleInput = view.findViewById(R.id.edit_reminder_title);
        final Button dateBtn = view.findViewById(R.id.btn_reminder_date);
        final Button timeBtn = view.findViewById(R.id.btn_reminder_time);
        final TextView dateText = view.findViewById(R.id.text_reminder_date);
        final TextView timeText = view.findViewById(R.id.text_reminder_time);

        dateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new DatePickerDialog(MainActivity.this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        reminderCalendarHelper.set(Calendar.YEAR, year);
                        reminderCalendarHelper.set(Calendar.MONTH, month);
                        reminderCalendarHelper.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                        dateText.setText(sdf.format(reminderCalendarHelper.getTime()));
                    }
                }, reminderCalendarHelper.get(Calendar.YEAR), reminderCalendarHelper.get(Calendar.MONTH), reminderCalendarHelper.get(Calendar.DAY_OF_MONTH)).show();
            }
        });

        timeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new TimePickerDialog(MainActivity.this, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        reminderCalendarHelper.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        reminderCalendarHelper.set(Calendar.MINUTE, minute);
                        reminderCalendarHelper.set(Calendar.SECOND, 0);

                        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                        timeText.setText(sdf.format(reminderCalendarHelper.getTime()));
                    }
                }, reminderCalendarHelper.get(Calendar.HOUR_OF_DAY), reminderCalendarHelper.get(Calendar.MINUTE), false).show();
            }
        });

        builder.setView(view);
        builder.setPositiveButton("Schedule", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String title = titleInput.getText().toString().trim();
                String dStr = dateText.getText().toString();
                String tStr = timeText.getText().toString();

                if (title.isEmpty() || dStr.equals("Set target Date") || tStr.equals("Set target Time")) {
                    Toast.makeText(MainActivity.this, "Fill in valid timing and note title!", Toast.LENGTH_SHORT).show();
                    return;
                }

                long triggerMillis = reminderCalendarHelper.getTimeInMillis();
                if (triggerMillis < System.currentTimeMillis()) {
                    Toast.makeText(MainActivity.this, "Warning: Time set is in the past!", Toast.LENGTH_SHORT).show();
                }

                long id = dbHelper.insertReminder(title, triggerMillis);
                scheduleSystemAlarm((int) id, title, triggerMillis);

                refreshAllPanels();
                Toast.makeText(MainActivity.this, "Scheduled Academic Reminder Set", Toast.LENGTH_LONG).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    // REAL-TIME SYSTEM ALARM TRIGGERS
    private void scheduleSystemAlarm(int alarmId, String message, long triggerTime) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(this, ReminderReceiver.class);
        intent.putExtra("reminder_title", message);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                alarmId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        }
    }

    private void cancelSystemAlarm(int alarmId, String message) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(this, ReminderReceiver.class);
        intent.putExtra("reminder_title", message);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                alarmId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        alarmManager.cancel(pendingIntent);
    }
}