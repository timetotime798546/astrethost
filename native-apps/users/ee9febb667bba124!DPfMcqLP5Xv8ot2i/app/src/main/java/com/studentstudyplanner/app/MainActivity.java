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
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
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
import android.widget.ImageView;
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

        // Assign Navigation Click events with tactile feedback sound
        tabDashboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playClickSound();
                switchTab(0);
            }
        });
        tabSubjects.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playClickSound();
                switchTab(1);
            }
        });
        tabTasks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playClickSound();
                switchTab(2);
            }
        });
        tabReminders.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playClickSound();
                switchTab(3);
            }
        });

        // Register Action triggers
        buttonQuickAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playClickSound();
                showQuickAddTaskDialog();
            }
        });
        btnAddSubject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playClickSound();
                showAddSubjectDialog();
            }
        });
        btnAddTask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playClickSound();
                showAddTaskDialog();
            }
        });
        btnAddReminder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playClickSound();
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

        // Show Leader Dialog and play Startup chime on launch
        showWelcomeLeaderSplash();

        // Request runtime notifications permission safely on Android 13+ (API 33+)
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 101);
        }
    }

    /**
     * Synthesizes audio frequencies on-the-fly inside a background thread.
     * Offers high fidelity UI sound effects with 100% device independence and no binary assets.
     */
    private void playSynthTone(final int[] freqs, final int[] durations) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int sampleRate = 8000;
                    for (int step = 0; step < freqs.length; step++) {
                        int freq = freqs[step];
                        int durationMs = durations[step];
                        int numSamples = durationMs * sampleRate / 1000;
                        double[] sample = new double[numSamples];
                        byte[] generatedSnd = new byte[2 * numSamples];
                        for (int i = 0; i < numSamples; ++i) {
                            sample[i] = Math.sin(2 * Math.PI * i / ((double) sampleRate / freq));
                        }
                        int idx = 0;
                        for (final double dVal : sample) {
                            final short val = (short) ((dVal * 32767));
                            generatedSnd[idx++] = (byte) (val & 0x00ff);
                            generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
                        }
                        AudioTrack audioTrack = new AudioTrack(
                                AudioManager.STREAM_MUSIC,
                                sampleRate, AudioFormat.CHANNEL_OUT_MONO,
                                AudioFormat.ENCODING_PCM_16BIT, generatedSnd.length,
                                AudioTrack.MODE_STATIC);
                        audioTrack.write(generatedSnd, 0, generatedSnd.length);
                        audioTrack.play();
                        Thread.sleep(durationMs + 15);
                        audioTrack.release();
                    }
                } catch (Exception e) {
                    // Fail-safe to avoid crash under thread state scenarios
                }
            }
        }).start();
    }

    private void playClickSound() {
        playSynthTone(new int[]{700}, new int[]{65});
    }

    private void playSuccessSound() {
        playSynthTone(new int[]{587, 880}, new int[]{110, 220});
    }

    private void playWarningSound() {
        playSynthTone(new int[]{380, 240}, new int[]{100, 160});
    }

    private void playWelcomeFanfare() {
        // Welcome Leader melody (Chords of C Major Arpeggio: C5 -> E5 -> G5 -> C6)
        playSynthTone(new int[]{523, 659, 784, 1046}, new int[]{140, 140, 140, 320});
    }

    /**
     * Builds and displays the "Leader" Welcome Splash screen overlay when opening the app.
     */
    private void showWelcomeLeaderSplash() {
        playWelcomeFanfare();

        AlertDialog.Builder splashBuilder = new AlertDialog.Builder(this);
        
        // Construct dynamic 3D visual container programmatically
        LinearLayout rootContainer = new LinearLayout(this);
        rootContainer.setOrientation(LinearLayout.VERTICAL);
        rootContainer.setPadding(28, 28, 28, 28);
        rootContainer.setBackgroundColor(Color.parseColor("#ECEFF1"));

        // Elegant Card view inside
        LinearLayout mainCard = new LinearLayout(this);
        mainCard.setOrientation(LinearLayout.VERTICAL);
        mainCard.setBackgroundResource(R.drawable.card_bg);
        mainCard.setPadding(24, 24, 24, 24);
        
        // App badge
        TextView iconText = new TextView(this);
        iconText.setText("🏆");
        iconText.setTextSize(48);
        iconText.setGravity(android.view.Gravity.CENTER);
        iconText.setPadding(0, 0, 0, 12);
        mainCard.addView(iconText);

        TextView titleText = new TextView(this);
        titleText.setText("Welcome Back, Study Leader!");
        titleText.setTextSize(18);
        titleText.setTextColor(Color.parseColor("#1E88E5"));
        titleText.setGravity(android.view.Gravity.CENTER);
        titleText.setTypeface(null, android.graphics.Typeface.BOLD);
        titleText.setPadding(0, 0, 0, 8);
        mainCard.addView(titleText);

        TextView subtitleText = new TextView(this);
        subtitleText.setText("Your goals are structured and organized in premium 3D. Let's make today productive!");
        subtitleText.setTextSize(13);
        subtitleText.setTextColor(Color.parseColor("#546E7A"));
        subtitleText.setGravity(android.view.Gravity.CENTER);
        subtitleText.setPadding(0, 0, 0, 20);
        mainCard.addView(subtitleText);

        // Tactile Dismiss Button
        Button closeBtn = new Button(this);
        closeBtn.setText("Enter Dashboard");
        closeBtn.setBackgroundResource(R.drawable.button_bg);
        closeBtn.setTextColor(Color.WHITE);
        closeBtn.setTypeface(null, android.graphics.Typeface.BOLD);
        closeBtn.setTransformationMethod(null);
        
        mainCard.addView(closeBtn);
        rootContainer.addView(mainCard);

        splashBuilder.setView(rootContainer);
        final AlertDialog splashDialog = splashBuilder.create();
        splashDialog.show();

        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playClickSound();
                splashDialog.dismiss();
            }
        });
    }

    private void switchTab(int index) {
        // Clear visibility state
        panelDashboard.setVisibility(View.GONE);
        panelSubjects.setVisibility(View.GONE);
        panelTasks.setVisibility(View.GONE);
        panelReminders.setVisibility(View.GONE);

        // Reset colors to inactive
        tabTextDashboard.setTextColor(Color.parseColor("#78909C"));
        tabTextDashboard.setTextSize(12);
        tabTextSubjects.setTextColor(Color.parseColor("#78909C"));
        tabTextSubjects.setTextSize(12);
        tabTextTasks.setTextColor(Color.parseColor("#78909C"));
        tabTextTasks.setTextSize(12);
        tabTextReminders.setTextColor(Color.parseColor("#78909C"));
        tabTextReminders.setTextSize(12);

        switch (index) {
            case 0:
                panelDashboard.setVisibility(View.VISIBLE);
                tabTextDashboard.setTextColor(Color.parseColor("#1E88E5"));
                tabTextDashboard.setTextSize(13);
                refreshDashboard();
                break;
            case 1:
                panelSubjects.setVisibility(View.VISIBLE);
                tabTextSubjects.setTextColor(Color.parseColor("#1E88E5"));
                tabTextSubjects.setTextSize(13);
                refreshSubjectList();
                break;
            case 2:
                panelTasks.setVisibility(View.VISIBLE);
                tabTextTasks.setTextColor(Color.parseColor("#1E88E5"));
                tabTextTasks.setTextSize(13);
                refreshTaskList();
                break;
            case 3:
                panelReminders.setVisibility(View.VISIBLE);
                tabTextReminders.setTextColor(Color.parseColor("#1E88E5"));
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

        // Update counts
        statTotalTasks.setText(String.valueOf(totalTasks));
        statTotalHours.setText(String.format(Locale.getDefault(), "%.1fh", totalStudyHours));
        statSubjectsCount.setText(String.valueOf(subjectCount));

        // Update overall calculations
        int percentage = 0;
        if (totalTasks > 0) {
            percentage = (completedTasks * 100) / totalTasks;
        }

        textProgressPercentage.setText(percentage + "% Complete");
        progressOverall.setProgress(percentage);

        // Subject breakdown layout injection with 3D design cards
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

                    // Generate a beautiful programmatic 3D card layout
                    LinearLayout row = new LinearLayout(this);
                    row.setOrientation(LinearLayout.VERTICAL);
                    row.setBackgroundResource(R.drawable.card_bg);
                    row.setPadding(16, 16, 16, 20); // Extra bottom padding for 3D extrusion effect
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    lp.setMargins(0, 4, 0, 14);
                    row.setLayoutParams(lp);

                    LinearLayout rowHeader = new LinearLayout(this);
                    rowHeader.setOrientation(LinearLayout.HORIZONTAL);
                    rowHeader.setGravity(android.view.Gravity.CENTER_VERTICAL);

                    // Badge indicator
                    View badge = new View(this);
                    LinearLayout.LayoutParams badgeLp = new LinearLayout.LayoutParams(16, 16);
                    badgeLp.setMargins(0, 0, 12, 0);
                    badge.setLayoutParams(badgeLp);
                    GradientDrawable bgShape = new GradientDrawable();
                    bgShape.setShape(GradientDrawable.OVAL);
                    bgShape.setColor(Color.parseColor(subColor));
                    badge.setBackground(bgShape);

                    TextView textName = new TextView(this);
                    textName.setText(subName);
                    textName.setTextSize(14);
                    textName.setTextColor(Color.parseColor("#37474F"));
                    textName.setTypeface(null, android.graphics.Typeface.BOLD);
                    textName.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

                    TextView textStatus = new TextView(this);
                    textStatus.setText(sCompleted + "/" + sTotal + " Done");
                    textStatus.setTextSize(12);
                    textStatus.setTextColor(Color.parseColor("#546E7A"));

                    rowHeader.addView(badge);
                    rowHeader.addView(textName);
                    rowHeader.addView(textStatus);

                    ProgressBar subProgress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
                    subProgress.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 10));
                    subProgress.setMax(100);
                    subProgress.setProgress(sPercent);
                    subProgress.setPadding(0, 12, 0, 0);

                    row.addView(rowHeader);
                    row.addView(subProgress);

                    layoutSubjectProgressList.addView(row);
                }
            }
            cursor.close();
        }

        // Reminders presentation layout
        layoutDashboardReminders.removeAllViews();
        Cursor remCursor = dbHelper.getAllReminders();
        if (remCursor != null && remCursor.getCount() > 0) {
            textNoRemindersDashboard.setVisibility(View.GONE);
            int titleIdx = remCursor.getColumnIndex(DatabaseHelper.COL_REM_TITLE);
            int timeIdx = remCursor.getColumnIndex(DatabaseHelper.COL_REM_TIME);

            int count = 0;
            while (remCursor.moveToNext() && count < 3) {
                if (titleIdx != -1 && timeIdx != -1) {
                    String titleStr = remCursor.getString(titleIdx);
                    long alertTime = remCursor.getLong(timeIdx);

                    TextView item = new TextView(this);
                    SimpleDateFormat format = new SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault());
                    String formattedDate = format.format(new Date(alertTime));

                    item.setText("⏰ " + titleStr + " (" + formattedDate + ")");
                    item.setPadding(0, 6, 0, 6);
                    item.setTextColor(Color.parseColor("#37474F"));
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

    private void showAddSubjectDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create New Subject");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(30, 24, 30, 24);

        final EditText inputName = new EditText(this);
        inputName.setHint("Subject Title (e.g. History)");
        layout.addView(inputName);

        TextView textSelectColor = new TextView(this);
        textSelectColor.setText("Select Color Badge Theme:");
        textSelectColor.setPadding(0, 20, 0, 10);
        textSelectColor.setTypeface(null, android.graphics.Typeface.BOLD);
        layout.addView(textSelectColor);

        final LinearLayout colorsLayout = new LinearLayout(this);
        colorsLayout.setOrientation(LinearLayout.HORIZONTAL);
        colorsLayout.setGravity(android.view.Gravity.CENTER);

        final String[] hexColors = {"#F44336", "#E91E63", "#9C27B0", "#3F51B5", "#2196F3", "#009688", "#4CAF50", "#FFEB3B", "#FF9800"};
        final ArrayList<View> colorViews = new ArrayList<View>();
        selectedColorHex = hexColors[4]; // Default to blue

        for (int i = 0; i < hexColors.length; i++) {
            final String hex = hexColors[i];
            final View colorBox = new View(this);
            LinearLayout.LayoutParams boxLp = new LinearLayout.LayoutParams(42, 42);
            boxLp.setMargins(8, 8, 8, 8);
            colorBox.setLayoutParams(boxLp);

            final GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.OVAL);
            shape.setColor(Color.parseColor(hex));
            if (hex.equals(selectedColorHex)) {
                shape.setStroke(4, Color.parseColor("#37474F"));
            } else {
                shape.setStroke(1, Color.parseColor("#B0BEC5"));
            }
            colorBox.setBackground(shape);

            colorBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    playClickSound();
                    selectedColorHex = hex;
                    for (int k = 0; k < hexColors.length; k++) {
                        GradientDrawable s = (GradientDrawable) colorViews.get(k).getBackground();
                        if (hexColors[k].equals(selectedColorHex)) {
                            s.setStroke(4, Color.parseColor("#37474F"));
                        } else {
                            s.setStroke(1, Color.parseColor("#B0BEC5"));
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
                    playWarningSound();
                    Toast.makeText(MainActivity.this, "Please insert a valid title", Toast.LENGTH_SHORT).show();
                    return;
                }
                boolean done = dbHelper.addSubject(subName, selectedColorHex);
                if (done) {
                    playSuccessSound();
                    Toast.makeText(MainActivity.this, "Subject registered!", Toast.LENGTH_SHORT).show();
                    populateSpinnerFilters();
                    refreshAllData();
                } else {
                    playWarningSound();
                    Toast.makeText(MainActivity.this, "Error inserting subject", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                playClickSound();
            }
        });
        builder.show();
    }

    private void showAddTaskDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create Planner Task");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(30, 24, 30, 24);

        final EditText inputTitle = new EditText(this);
        inputTitle.setHint("Study Task Label");
        layout.addView(inputTitle);

        TextView selectSubjectText = new TextView(this);
        selectSubjectText.setText("Select Subject Category:");
        selectSubjectText.setPadding(0, 16, 0, 6);
        selectSubjectText.setTypeface(null, android.graphics.Typeface.BOLD);
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
            playWarningSound();
            Toast.makeText(this, "Please create at least one subject first!", Toast.LENGTH_LONG).show();
            return;
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, subjectNamesList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        subjectSpinner.setAdapter(adapter);
        layout.addView(subjectSpinner);

        final EditText inputHours = new EditText(this);
        inputHours.setHint("Estimated study hours (e.g. 2.5)");
        inputHours.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        layout.addView(inputHours);

        final Button btnDatePicker = new Button(this);
        btnDatePicker.setText("Select Due Date");
        btnDatePicker.setBackgroundResource(R.drawable.button_bg);
        btnDatePicker.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        btnLp.setMargins(0, 18, 0, 0);
        btnDatePicker.setLayoutParams(btnLp);

        final Calendar calendar = Calendar.getInstance();
        final SimpleDateFormat dateOnlyFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        btnDatePicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playClickSound();
                new DatePickerDialog(MainActivity.this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        playClickSound();
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
                    playWarningSound();
                    Toast.makeText(MainActivity.this, "Check entered parameters", Toast.LENGTH_SHORT).show();
                    return;
                }

                double hoursVal = 1.0;
                try {
                    hoursVal = Double.parseDouble(hoursText);
                } catch (NumberFormatException e) {
                    // Fallback
                }

                int subId = subjectIdsList.get(selectedPosition);
                String formattedDueDate = dateOnlyFormat.format(calendar.getTime());

                boolean success = dbHelper.addTask(subId, taskTitle, formattedDueDate, hoursVal);
                if (success) {
                    playSuccessSound();
                    Toast.makeText(MainActivity.this, "Study task assigned!", Toast.LENGTH_SHORT).show();
                    refreshAllData();
                } else {
                    playWarningSound();
                    Toast.makeText(MainActivity.this, "Failed saving task", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                playClickSound();
            }
        });
        builder.show();
    }

    private void showQuickAddTaskDialog() {
        switchTab(2);
        showAddTaskDialog();
    }

    private void showAddReminderDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Set Dynamic Study Alert");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(30, 24, 30, 24);

        final EditText inputTitle = new EditText(this);
        inputTitle.setHint("Alarm Alert Message (e.g., Study Chemistry)");
        layout.addView(inputTitle);

        final Button dateBtn = new Button(this);
        dateBtn.setText("Choose Date");
        dateBtn.setBackgroundResource(R.drawable.button_bg);
        dateBtn.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams lpDate = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpDate.setMargins(0, 20, 0, 10);
        dateBtn.setLayoutParams(lpDate);

        final Button timeBtn = new Button(this);
        timeBtn.setText("Choose Time");
        timeBtn.setBackgroundResource(R.drawable.button_bg);
        timeBtn.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams lpTime = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpTime.setMargins(0, 6, 0, 20);
        timeBtn.setLayoutParams(lpTime);

        final Calendar scheduleCalendar = Calendar.getInstance();

        dateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playClickSound();
                new DatePickerDialog(MainActivity.this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        playClickSound();
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
                playClickSound();
                new TimePickerDialog(MainActivity.this, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        playClickSound();
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
                    playWarningSound();
                    Toast.makeText(MainActivity.this, "Reminder alert is empty!", Toast.LENGTH_SHORT).show();
                    return;
                }

                long alertTime = scheduleCalendar.getTimeInMillis();
                if (alertTime < System.currentTimeMillis()) {
                    playWarningSound();
                    Toast.makeText(MainActivity.this, "Cannot schedule in the past!", Toast.LENGTH_SHORT).show();
                    return;
                }

                boolean done = dbHelper.addReminder(title, alertTime);
                if (done) {
                    playSuccessSound();
                    setSystemAlarm(title, alertTime);
                    Toast.makeText(MainActivity.this, "Alert programmed successfully!", Toast.LENGTH_SHORT).show();
                    refreshAllData();
                } else {
                    playWarningSound();
                    Toast.makeText(MainActivity.this, "Database storage failure", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("Close", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                playClickSound();
            }
        });
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
                // Instantiates simple container card with 3D backing
                LinearLayout cardWrapper = new LinearLayout(context);
                cardWrapper.setOrientation(LinearLayout.VERTICAL);
                cardWrapper.setBackgroundResource(R.drawable.card_bg);
                cardWrapper.setPadding(20, 20, 20, 24);

                TextView text1 = new TextView(context);
                text1.setId(android.R.id.text1);
                text1.setTextSize(16);

                TextView text2 = new TextView(context);
                text2.setId(android.R.id.text2);
                text2.setTextSize(12);
                text2.setPadding(0, 6, 0, 0);

                cardWrapper.addView(text1);
                cardWrapper.addView(text2);
                convertView = cardWrapper;
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
                title.setTypeface(null, android.graphics.Typeface.BOLD);

                sub.setText("Hold physical long-press to remove Subject and associated Tasks");
                sub.setTextColor(Color.parseColor("#78909C"));

                convertView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        playWarningSound();
                        new AlertDialog.Builder(context)
                                .setTitle("Delete Subject")
                                .setMessage("Are you sure you want to delete '" + subName + "'? All associated tasks will be removed.")
                                .setPositiveButton("Yes, Delete", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        playSuccessSound();
                                        dbHelper.deleteSubject(subId);
                                        refreshAllData();
                                    }
                                })
                                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        playClickSound();
                                    }
                                })
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
                // Beautiful Programmatic 3D Card wrapper for list items
                LinearLayout itemContainer = new LinearLayout(context);
                itemContainer.setOrientation(LinearLayout.VERTICAL);
                itemContainer.setBackgroundResource(R.drawable.card_bg);
                itemContainer.setPadding(16, 16, 16, 20);

                LinearLayout horizontalLayout = new LinearLayout(context);
                horizontalLayout.setOrientation(LinearLayout.HORIZONTAL);
                horizontalLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);

                CheckBox chk = new CheckBox(context);
                chk.setTag("chk");

                LinearLayout info = new LinearLayout(context);
                info.setOrientation(LinearLayout.VERTICAL);
                info.setPadding(16, 0, 16, 0);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
                info.setLayoutParams(lp);

                TextView tTitle = new TextView(context);
                tTitle.setTextSize(15);
                tTitle.setTag("title");

                TextView tSub = new TextView(context);
                tSub.setTextSize(11);
                tSub.setTag("subtitle");

                info.addView(tTitle);
                info.addView(tSub);

                Button del = new Button(context);
                del.setText("✕");
                del.setTextSize(14);
                del.setTextColor(Color.RED);
                del.setBackgroundColor(Color.TRANSPARENT);
                del.setTag("delete");
                del.setLayoutParams(new LinearLayout.LayoutParams(64, LinearLayout.LayoutParams.WRAP_CONTENT));

                horizontalLayout.addView(chk);
                horizontalLayout.addView(info);
                horizontalLayout.addView(del);

                itemContainer.addView(horizontalLayout);
                convertView = itemContainer;
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
                        playSuccessSound();
                        dbHelper.updateTaskCompletion(taskId, isChecked);
                        refreshAllData();
                    }
                });

                deleteBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        playWarningSound();
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
                LinearLayout cardWrapper = new LinearLayout(context);
                cardWrapper.setOrientation(LinearLayout.VERTICAL);
                cardWrapper.setBackgroundResource(R.drawable.card_bg);
                cardWrapper.setPadding(20, 20, 20, 24);

                TextView text1 = new TextView(context);
                text1.setId(android.R.id.text1);
                text1.setTextSize(16);

                TextView text2 = new TextView(context);
                text2.setId(android.R.id.text2);
                text2.setTextSize(12);
                text2.setPadding(0, 6, 0, 0);

                cardWrapper.addView(text1);
                cardWrapper.addView(text2);
                convertView = cardWrapper;
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
                title.setTypeface(null, android.graphics.Typeface.BOLD);
                title.setTextColor(Color.parseColor("#37474F"));

                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd 'at' hh:mm a", Locale.getDefault());
                sub.setText("Scheduled: " + format.format(new Date(timeMillis)) + "\n(Hold long-press to cancel alert)");
                sub.setTextColor(Color.parseColor("#78909C"));

                convertView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        playWarningSound();
                        new AlertDialog.Builder(context)
                                .setTitle("Cancel Alert")
                                .setMessage("Do you want to cancel and delete the alert reminder '" + titleStr + "'?")
                                .setPositiveButton("Remove Alert", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        playSuccessSound();
                                        cancelSystemAlarm(titleStr, timeMillis);
                                        dbHelper.deleteReminder(remId);
                                        refreshAllData();
                                        Toast.makeText(context, "Alert removed!", Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .setNegativeButton("Keep", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        playClickSound();
                                    }
                                })
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