package com.lifeflowproductivity.app;

import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {

    // Databases
    private DatabaseHelper dbHelper;
    private SQLiteDatabase db;

    // Visibility Switcher Layouts
    private ScrollView layoutDashboard;
    private LinearLayout layoutTasks;
    private ScrollView layoutTimer;
    private LinearLayout layoutHabits;
    private LinearLayout layoutMore;

    // Tab Buttons
    private LinearLayout btnHome, btnTasks, btnTimer, btnHabits, btnMore;
    private TextView txtHome, txtTasks, txtTimer, txtHabits, txtMore;
    private TextView toolbarTitle;
    private TextView themeToggleBtn;

    // Dashboard Elements
    private TextView welcomeText, dateText, taskSummary, habitSummary, expenseSummary;

    // Tasks Elements
    private EditText taskInput;
    private Button addTaskBtn;
    private LinearLayout tasksContainer;

    // Pomodoro Elements
    private TextView timerDisplay;
    private Button modeWorkBtn, modeBreakBtn, startTimerBtn, resetTimerBtn;
    private CountDownTimer countDownTimer;
    private long totalTimerMillis = 1500000; // default 25 mins
    private long currentMillisLeft = 1500000;
    private boolean isTimerRunning = false;

    // Habits Elements
    private EditText habitInput;
    private Button addHabitBtn;
    private LinearLayout habitsContainer;

    // More View Sub Elements
    private Button subBtnExpenses, subBtnNotes;
    private LinearLayout subLayoutExpenses, subLayoutNotes;
    private EditText expenseTitleInput, expenseAmountInput;
    private Button addExpenseBtn;
    private LinearLayout expensesContainer;
    private EditText noteTitleInput, noteContentInput;
    private Button addNoteBtn;
    private LinearLayout notesContainer;

    // State
    private boolean isDarkMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) { 
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);
        db = dbHelper.getWritableDatabase();

        initViews();
        setupNavigation();
        setupTaskTracker();
        setupPomodoroTimer();
        setupHabitTracker();
        setupMoreSection();
        setupThemeToggle();

        // Default display states
        refreshDashboard();
    }

    private void initViews() {
        // Navigation Screens
        layoutDashboard = findViewById(R.id.layout_dashboard);
        layoutTasks = findViewById(R.id.layout_tasks);
        layoutTimer = findViewById(R.id.layout_timer);
        layoutHabits = findViewById(R.id.layout_habits);
        layoutMore = findViewById(R.id.layout_more);

        // Navigation Controls
        btnHome = findViewById(R.id.nav_btn_home);
        btnTasks = findViewById(R.id.nav_btn_tasks);
        btnTimer = findViewById(R.id.nav_btn_timer);
        btnHabits = findViewById(R.id.nav_btn_habits);
        btnMore = findViewById(R.id.nav_btn_more);

        txtHome = findViewById(R.id.nav_text_home);
        txtTasks = findViewById(R.id.nav_text_tasks);
        txtTimer = findViewById(R.id.nav_text_timer);
        txtHabits = findViewById(R.id.nav_text_habits);
        txtMore = findViewById(R.id.nav_text_more);

        toolbarTitle = findViewById(R.id.toolbar_title);
        themeToggleBtn = findViewById(R.id.theme_toggle_btn);

        // Dashboard
        welcomeText = findViewById(R.id.dashboard_welcome);
        dateText = findViewById(R.id.dashboard_date);
        taskSummary = findViewById(R.id.dashboard_task_summary);
        habitSummary = findViewById(R.id.dashboard_habit_summary);
        expenseSummary = findViewById(R.id.dashboard_expense_summary);

        // Tasks
        taskInput = findViewById(R.id.task_input);
        addTaskBtn = findViewById(R.id.add_task_btn);
        tasksContainer = findViewById(R.id.tasks_container);

        // Pomodoro
        timerDisplay = findViewById(R.id.timer_display);
        modeWorkBtn = findViewById(R.id.timer_mode_work);
        modeBreakBtn = findViewById(R.id.timer_mode_break);
        startTimerBtn = findViewById(R.id.timer_start_btn);
        resetTimerBtn = findViewById(R.id.timer_reset_btn);

        // Habits
        habitInput = findViewById(R.id.habit_input);
        addHabitBtn = findViewById(R.id.add_habit_btn);
        habitsContainer = findViewById(R.id.habits_container);

        // More / Finance / Notes
        subBtnExpenses = findViewById(R.id.sub_btn_expenses);
        subBtnNotes = findViewById(R.id.sub_btn_notes);
        subLayoutExpenses = findViewById(R.id.sub_layout_expenses);
        subLayoutNotes = findViewById(R.id.sub_layout_notes);

        expenseTitleInput = findViewById(R.id.expense_title_input);
        expenseAmountInput = findViewById(R.id.expense_amount_input);
        addExpenseBtn = findViewById(R.id.add_expense_btn);
        expensesContainer = findViewById(R.id.expenses_container);

        noteTitleInput = findViewById(R.id.note_title_input);
        noteContentInput = findViewById(R.id.note_content_input);
        addNoteBtn = findViewById(R.id.add_note_btn);
        notesContainer = findViewById(R.id.notes_container);
    }

    private void setupNavigation() {
        btnHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showScreen(1);
            }
        });
        btnTasks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showScreen(2);
            }
        });
        btnTimer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showScreen(3);
            }
        });
        btnHabits.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showScreen(4);
            }
        });
        btnMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showScreen(5);
            }
        });
    }

    private void showScreen(int screenIndex) {
        // Reset navigators look
        txtHome.setTextColor(0xFF757575);
        txtTasks.setTextColor(0xFF757575);
        txtTimer.setTextColor(0xFF757575);
        txtHabits.setTextColor(0xFF757575);
        txtMore.setTextColor(0xFF757575);

        layoutDashboard.setVisibility(View.GONE);
        layoutTasks.setVisibility(View.GONE);
        layoutTimer.setVisibility(View.GONE);
        layoutHabits.setVisibility(View.GONE);
        layoutMore.setVisibility(View.GONE);

        int primaryColor = isDarkMode ? 0xFFBB86FC : 0xFF6200EE;

        switch (screenIndex) {
            case 1:
                layoutDashboard.setVisibility(View.VISIBLE);
                txtHome.setTextColor(primaryColor);
                toolbarTitle.setText("Dashboard");
                refreshDashboard();
                break;
            case 2:
                layoutTasks.setVisibility(View.VISIBLE);
                txtTasks.setTextColor(primaryColor);
                toolbarTitle.setText("Daily Tasks");
                refreshTasks();
                break;
            case 3:
                layoutTimer.setVisibility(View.VISIBLE);
                txtTimer.setTextColor(primaryColor);
                toolbarTitle.setText("Study & Work Timer");
                break;
            case 4:
                layoutHabits.setVisibility(View.VISIBLE);
                txtHabits.setTextColor(primaryColor);
                toolbarTitle.setText("Habit Tracker");
                refreshHabits();
                break;
            case 5:
                layoutMore.setVisibility(View.VISIBLE);
                txtMore.setTextColor(primaryColor);
                toolbarTitle.setText("Expenses & Notes");
                refreshExpenses();
                refreshNotes();
                break;
        }
    }

    private void refreshDashboard() {
        // Setup current system time
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault());
        dateText.setText(sdf.format(new Date()));

        // Task Summary calculation
        Cursor taskCursor = db.rawQuery("SELECT COUNT(*), SUM(is_completed) FROM tasks", null);
        int totalTasks = 0;
        int completedTasks = 0;
        if (taskCursor.moveToFirst()) {
            totalTasks = taskCursor.getInt(0);
            completedTasks = taskCursor.getInt(1);
        }
        taskCursor.close();
        taskSummary.setText(completedTasks + " of " + totalTasks + " tasks completed");

        // Habit Best Streak calculation
        Cursor habitCursor = db.rawQuery("SELECT MAX(streak) FROM habits", null);
        int bestStreak = 0;
        if (habitCursor.moveToFirst()) {
            bestStreak = habitCursor.getInt(0);
        }
        habitCursor.close();
        habitSummary.setText("Best habit streak: " + bestStreak + " days");

        // Budget calculation
        Cursor expenseCursor = db.rawQuery("SELECT SUM(amount) FROM expenses", null);
        double totalExpenses = 0;
        if (expenseCursor.moveToFirst()) {
            totalExpenses = expenseCursor.getDouble(0);
        }
        expenseCursor.close();
        expenseSummary.setText("Total Expenses: $" + String.format(Locale.US, "%.2f", totalExpenses));

        applyThemeOnView(findViewById(R.id.root_layout), isDarkMode);
    }

    // ---------------- TASKS SECTION ----------------
    private void setupTaskTracker() {
        addTaskBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String title = taskInput.getText().toString().trim();
                if (title.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please type a task label", Toast.LENGTH_SHORT).show();
                    return;
                }
                ContentValues values = new ContentValues();
                values.put("title", title);
                values.put("is_completed", 0);
                db.insert("tasks", null, values);
                taskInput.setText("");
                refreshTasks();
            }
        });
    }

    private void refreshTasks() {
        tasksContainer.removeAllViews();
        Cursor cursor = db.rawQuery("SELECT * FROM tasks ORDER BY id DESC", null);
        if (cursor.moveToFirst()) {
            do {
                final int id = cursor.getInt(0);
                final String title = cursor.getString(1);
                final boolean isCompleted = cursor.getInt(2) == 1;

                LinearLayout row = new LinearLayout(MainActivity.this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8));
                row.setTag("card");

                LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                rowParams.setMargins(0, 0, 0, dpToPx(8));
                row.setLayoutParams(rowParams);

                final CheckBox cb = new CheckBox(MainActivity.this);
                cb.setChecked(isCompleted);
                cb.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        db.execSQL("UPDATE tasks SET is_completed = " + (cb.isChecked() ? 1 : 0) + " WHERE id = " + id);
                        refreshTasks();
                    }
                });
                row.addView(cb);

                TextView tv = new TextView(MainActivity.this);
                tv.setText(title);
                tv.setTextSize(16);
                tv.setPadding(dpToPx(8), 0, dpToPx(8), 0);
                if (isCompleted) {
                    tv.setPaintFlags(tv.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                    tv.setTextColor(0xFF9E9E9E);
                } else {
                    tv.setTextColor(isDarkMode ? Color.WHITE : Color.BLACK);
                }
                LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
                tv.setLayoutParams(textParams);
                row.addView(tv);

                Button delBtn = new Button(MainActivity.this);
                delBtn.setText("✕");
                delBtn.setTextColor(Color.RED);
                delBtn.setBackgroundColor(Color.TRANSPARENT);
                delBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        db.execSQL("DELETE FROM tasks WHERE id = " + id);
                        refreshTasks();
                    }
                });
                row.addView(delBtn);

                tasksContainer.addView(row);
            } while (cursor.moveToNext());
        }
        cursor.close();
        applyThemeOnView(tasksContainer, isDarkMode);
    }

    // ---------------- POMODORO TIMER SECTION ----------------
    private void setupPomodoroTimer() {
        modeWorkBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setTimerDuration(1500000); // 25 mins
                Toast.makeText(MainActivity.this, "Work interval: 25 mins selected", Toast.LENGTH_SHORT).show();
            }
        });

        modeBreakBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setTimerDuration(300000); // 5 mins
                Toast.makeText(MainActivity.this, "Break interval: 5 mins selected", Toast.LENGTH_SHORT).show();
            }
        });

        startTimerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isTimerRunning) {
                    pauseTimer();
                } else {
                    startTimer();
                }
            }
        });

        resetTimerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetTimer();
            }
        });
    }

    private void setTimerDuration(long millis) {
        pauseTimer();
        totalTimerMillis = millis;
        currentMillisLeft = millis;
        updateTimerDisplay();
    }

    private void startTimer() {
        isTimerRunning = true;
        startTimerBtn.setText("Pause");
        countDownTimer = new CountDownTimer(currentMillisLeft, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                currentMillisLeft = millisUntilFinished;
                updateTimerDisplay();
            }

            @Override
            public void onFinish() {
                isTimerRunning = false;
                startTimerBtn.setText("Start");
                timerDisplay.setText("00:00");
                triggerVibration();
                Toast.makeText(MainActivity.this, "Interval Finished!", Toast.LENGTH_LONG).show();
            }
        }.start();
    }

    private void pauseTimer() {
        if (isTimerRunning) {
            countDownTimer.cancel();
            isTimerRunning = false;
            startTimerBtn.setText("Resume");
        }
    }

    private void resetTimer() {
        pauseTimer();
        currentMillisLeft = totalTimerMillis;
        startTimerBtn.setText("Start");
        updateTimerDisplay();
    }

    private void updateTimerDisplay() {
        int minutes = (int) (currentMillisLeft / 1000) / 60;
        int seconds = (int) (currentMillisLeft / 1000) % 60;
        timerDisplay.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
    }

    private void triggerVibration() {
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (vibrator != null) {
            vibrator.vibrate(800); // obsolete in higher versions but fully compatible
        }
    }

    // ---------------- HABIT TRACKER SECTION ----------------
    private void setupHabitTracker() {
        addHabitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = habitInput.getText().toString().trim();
                if (name.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please type a habit to log", Toast.LENGTH_SHORT).show();
                    return;
                }
                ContentValues values = new ContentValues();
                values.put("name", name);
                values.put("streak", 0);
                values.put("last_done", "");
                db.insert("habits", null, values);
                habitInput.setText("");
                refreshHabits();
            }
        });
    }

    private String getTodayString() { 
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    private String getYesterdayString() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.getTime());
    }

    private void refreshHabits() {
        habitsContainer.removeAllViews();
        Cursor cursor = db.rawQuery("SELECT * FROM habits ORDER BY id DESC", null);
        if (cursor.moveToFirst()) {
            do {
                final int id = cursor.getInt(0);
                final String name = cursor.getString(1);
                final int streak = cursor.getInt(2);
                final String lastDone = cursor.getString(3);

                LinearLayout row = new LinearLayout(MainActivity.this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8));
                row.setTag("card");

                LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                rowParams.setMargins(0, 0, 0, dpToPx(8));
                row.setLayoutParams(rowParams);

                LinearLayout infoLayout = new LinearLayout(MainActivity.this);
                infoLayout.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
                infoLayout.setLayoutParams(infoParams);

                TextView titleTv = new TextView(MainActivity.this);
                titleTv.setText(name);
                titleTv.setTextSize(16);
                titleTv.setTextColor(isDarkMode ? Color.WHITE : Color.BLACK);
                titleTv.setTypeface(null, Typeface.BOLD);
                infoLayout.addView(titleTv);

                TextView streakTv = new TextView(MainActivity.this);
                streakTv.setText("🔥 " + streak + " day streak");
                streakTv.setTextSize(12);
                streakTv.setTag("sec_text");
                streakTv.setTextColor(0xFFFF9800);
                infoLayout.addView(streakTv);

                row.addView(infoLayout);

                final Button checkInBtn = new Button(MainActivity.this);
                final String today = getTodayString();
                if (today.equals(lastDone)) {
                    checkInBtn.setText("Done");
                    checkInBtn.setEnabled(false);
                } else {
                    checkInBtn.setText("Check In");
                    checkInBtn.setEnabled(true);
                }

                checkInBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String yesterday = getYesterdayString();
                        int newStreak = 1;
                        if (today.equals(lastDone)) {
                            return;
                        } else if (yesterday.equals(lastDone)) {
                            newStreak = streak + 1;
                        }
                        db.execSQL("UPDATE habits SET streak = " + newStreak + ", last_done = '" + today + "' WHERE id = " + id);
                        refreshHabits();
                    }
                });
                row.addView(checkInBtn);

                Button delBtn = new Button(MainActivity.this);
                delBtn.setText("✕");
                delBtn.setTextColor(Color.RED);
                delBtn.setBackgroundColor(Color.TRANSPARENT);
                delBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        db.execSQL("DELETE FROM habits WHERE id = " + id);
                        refreshHabits();
                    }
                });
                row.addView(delBtn);

                habitsContainer.addView(row);
            } while (cursor.moveToNext());
        }
        cursor.close();
        applyThemeOnView(habitsContainer, isDarkMode);
    }

    // ---------------- EXPENSES & NOTES SECTION ----------------
    private void setupMoreSection() {
        subBtnExpenses.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                subLayoutExpenses.setVisibility(View.VISIBLE);
                subLayoutNotes.setVisibility(View.GONE);
            }
        });

        subBtnNotes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                subLayoutExpenses.setVisibility(View.GONE);
                subLayoutNotes.setVisibility(View.VISIBLE);
            }
        });

        // EXPENSES Action
        addExpenseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String title = expenseTitleInput.getText().toString().trim();
                String amtStr = expenseAmountInput.getText().toString().trim();
                if (title.isEmpty() || amtStr.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Input title and amount parameters", Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    double amount = Double.parseDouble(amtStr);
                    ContentValues values = new ContentValues();
                    values.put("title", title);
                    values.put("amount", amount);
                    values.put("category", "General");
                    values.put("date", getTodayString());
                    db.insert("expenses", null, values);

                    expenseTitleInput.setText("");
                    expenseAmountInput.setText("");
                    refreshExpenses();
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Verify amount accuracy", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // NOTES Action
        addNoteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String title = noteTitleInput.getText().toString().trim();
                String body = noteContentInput.getText().toString().trim();
                if (title.isEmpty() || body.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Log proper headers and bodies", Toast.LENGTH_SHORT).show();
                    return;
                }
                ContentValues values = new ContentValues();
                values.put("title", title);
                values.put("content", body);
                db.insert("notes", null, values);

                noteTitleInput.setText("");
                noteContentInput.setText("");
                refreshNotes();
            }
        });
    }

    private void refreshExpenses() {
        expensesContainer.removeAllViews();
        Cursor cursor = db.rawQuery("SELECT * FROM expenses ORDER BY id DESC", null);
        if (cursor.moveToFirst()) {
            do {
                final int id = cursor.getInt(0);
                final String title = cursor.getString(1);
                final double amount = cursor.getDouble(2);

                LinearLayout row = new LinearLayout(MainActivity.this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8));
                row.setTag("card");

                LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                rowParams.setMargins(0, 0, 0, dpToPx(8));
                row.setLayoutParams(rowParams);

                TextView labelTv = new TextView(MainActivity.this);
                labelTv.setText(title);
                labelTv.setTextSize(16);
                labelTv.setTextColor(isDarkMode ? Color.WHITE : Color.BLACK);
                LinearLayout.LayoutParams lParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
                labelTv.setLayoutParams(lParams);
                row.addView(labelTv);

                TextView amtTv = new TextView(MainActivity.this);
                amtTv.setText("$" + String.format(Locale.US, "%.2f", amount));
                amtTv.setTextSize(16);
                amtTv.setTextColor(0xFFE53935);
                amtTv.setPadding(dpToPx(12), 0, dpToPx(12), 0);
                row.addView(amtTv);

                Button delBtn = new Button(MainActivity.this);
                delBtn.setText("✕");
                delBtn.setTextColor(Color.RED);
                delBtn.setBackgroundColor(Color.TRANSPARENT);
                delBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        db.execSQL("DELETE FROM expenses WHERE id = " + id);
                        refreshExpenses();
                    }
                });
                row.addView(delBtn);

                expensesContainer.addView(row);
            } while (cursor.moveToNext());
        }
        cursor.close();
        applyThemeOnView(expensesContainer, isDarkMode);
    }

    private void refreshNotes() {
        notesContainer.removeAllViews();
        Cursor cursor = db.rawQuery("SELECT * FROM notes ORDER BY id DESC", null);
        if (cursor.moveToFirst()) {
            do {
                final int id = cursor.getInt(0);
                final String title = cursor.getString(1);
                final String body = cursor.getString(2);

                LinearLayout row = new LinearLayout(MainActivity.this);
                row.setOrientation(LinearLayout.VERTICAL);
                row.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));
                row.setTag("card");

                LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                rowParams.setMargins(0, 0, 0, dpToPx(12));
                row.setLayoutParams(rowParams);

                RelativeLayout headerRow = new RelativeLayout(MainActivity.this);
                headerRow.setLayoutParams(new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT));

                TextView titleTv = new TextView(MainActivity.this);
                titleTv.setText(title);
                titleTv.setTextSize(16);
                titleTv.setTypeface(null, Typeface.BOLD);
                titleTv.setTextColor(isDarkMode ? Color.WHITE : Color.BLACK);
                headerRow.addView(titleTv);

                Button delBtn = new Button(MainActivity.this);
                delBtn.setText("✕");
                delBtn.setTextColor(Color.RED);
                delBtn.setBackgroundColor(Color.TRANSPARENT);
                RelativeLayout.LayoutParams btnParams = new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                btnParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                delBtn.setLayoutParams(btnParams);
                delBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        db.execSQL("DELETE FROM notes WHERE id = " + id);
                        refreshNotes();
                    }
                });
                headerRow.addView(delBtn);
                row.addView(headerRow);

                TextView bodyTv = new TextView(MainActivity.this);
                bodyTv.setText(body);
                bodyTv.setTextSize(14);
                bodyTv.setPadding(0, dpToPx(6), 0, 0);
                bodyTv.setTag("sec_text");
                bodyTv.setTextColor(isDarkMode ? 0xFFA0A0A0 : 0xFF757575);
                row.addView(bodyTv);

                notesContainer.addView(row);
            } while (cursor.moveToNext());
        }
        cursor.close();
        applyThemeOnView(notesContainer, isDarkMode);
    }

    // ---------------- INTERACTIVE THEME MANAGEMENT ----------------
    private void setupThemeToggle() {
        themeToggleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isDarkMode = !isDarkMode;
                if (isDarkMode) {
                    themeToggleBtn.setText("🌙 Dark Mode");
                } else {
                    themeToggleBtn.setText("☀️ Light Mode");
                }
                applyThemeOnView(findViewById(R.id.root_layout), isDarkMode);
            }
        });
    }

    private void applyThemeOnView(View view, boolean dark) {
        if (view == null) return;

        int bg = dark ? 0xFF121212 : 0xFFF5F5F5;
        int text = dark ? 0xFFFFFFFF : 0xFF212121;
        int cardBg = dark ? 0xFF1E1E1E : 0xFFFFFFFF;
        int secText = dark ? 0xFFA0A0A0 : 0xFF757575;
        int border = dark ? 0xFF333333 : 0xFFDDDDDD;

        if (view == findViewById(R.id.root_layout)) {
            view.setBackgroundColor(bg);
        }

        if (view.getTag() != null) {
            String tag = view.getTag().toString();
            if (tag.equals("card")) {
                GradientDrawable shape = new GradientDrawable();
                shape.setShape(GradientDrawable.RECTANGLE);
                shape.setCornerRadius(dpToPx(12));
                shape.setColor(cardBg);
                shape.setStroke(dpToPx(1), border);
                view.setBackground(shape);
            }
        }

        if (view instanceof TextView) {
            TextView tv = (TextView) view;
            if (tv == toolbarTitle || tv == themeToggleBtn) {
                // Keep fixed white style inside top navigation toolbar
            } else if (tv.getId() == R.id.nav_text_home || tv.getId() == R.id.nav_text_tasks || 
                       tv.getId() == R.id.nav_text_timer || tv.getId() == R.id.nav_text_habits || 
                       tv.getId() == R.id.nav_text_more) {
                // Let navigation controls styling handle this independently
            } else if (view.getTag() != null && view.getTag().toString().equals("sec_text")) {
                tv.setTextColor(secText);
            } else {
                tv.setTextColor(text);
            }
        }

        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                applyThemeOnView(vg.getChildAt(i), dark);
            }
        }

        // Static sub elements color updates
        findViewById(R.id.bottom_nav).setBackgroundColor(dark ? 0xFF1C1C1E : 0xFFFFFFFF);
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }
}