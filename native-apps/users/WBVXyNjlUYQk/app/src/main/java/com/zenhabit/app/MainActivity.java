package com.zenhabit.app;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private View layoutHabitsTab;
    private View layoutTimerTab;
    private View layoutJournalTab;

    private Button navHabitsBtn;
    private Button navTimerBtn;
    private Button navJournalBtn;

    private List<Habit> habitsList = new ArrayList<>();
    private Spinner spinnerCategory;
    private EditText editHabitName;
    private Button btnAddHabit;

    private CustomTimerVisualizerView timerVisualizer;
    private TextView timerCountdownText;
    private Button btnTimerStart;
    private Button btnTimerReset;
    private Button btnPresetFocus;
    private Button btnPresetShort;
    private Button btnPresetLong;

    private CountDownTimer countDownTimer;
    private boolean isTimerActive = false;
    private long timeRemainingMillis = 1500000; // 25 mins

    private int selectedMoodScore = 3; 
    private TextView txtSelectedMood;
    private EditText editMoodNote;
    private Button btnSaveMood;

    private android.os.Handler animationHandler = new android.os.Handler();
    private Runnable animationRunnable = new Runnable() {
        @Override
        public void run() {
            if (isTimerActive) {
                timerVisualizer.updateAnimation();
                animationHandler.postDelayed(this, 50);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        layoutHabitsTab = findViewById(R.id.layout_habits_tab);
        layoutTimerTab = findViewById(R.id.layout_timer_tab);
        layoutJournalTab = findViewById(R.id.layout_journal_tab);

        navHabitsBtn = (Button) findViewById(R.id.nav_habits);
        navTimerBtn = (Button) findViewById(R.id.nav_timer);
        navJournalBtn = (Button) findViewById(R.id.nav_journal);

        spinnerCategory = (Spinner) findViewById(R.id.spinner_category);
        editHabitName = (EditText) findViewById(R.id.edit_habit_name);
        btnAddHabit = (Button) findViewById(R.id.btn_add_habit);

        timerVisualizer = (CustomTimerVisualizerView) findViewById(R.id.timer_visualizer);
        timerCountdownText = (TextView) findViewById(R.id.timer_countdown_text);
        btnTimerStart = (Button) findViewById(R.id.btn_timer_start);
        btnTimerReset = (Button) findViewById(R.id.btn_timer_reset);
        btnPresetFocus = (Button) findViewById(R.id.btn_preset_focus);
        btnPresetShort = (Button) findViewById(R.id.btn_preset_short);
        btnPresetLong = (Button) findViewById(R.id.btn_preset_long);

        txtSelectedMood = (TextView) findViewById(R.id.txt_selected_mood);
        editMoodNote = (EditText) findViewById(R.id.edit_mood_note);
        btnSaveMood = (Button) findViewById(R.id.btn_save_mood);

        navHabitsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(1);
            }
        });
        navTimerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(2);
            }
        });
        navJournalBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(3);
            }
        });

        setupCategorySpinner();
        loadHabits();
        refreshHabitsUI();

        btnAddHabit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = editHabitName.getText().toString().trim();
                if (name.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please write a habit definition", Toast.LENGTH_SHORT).show();
                    return;
                }
                String category = spinnerCategory.getSelectedItem().toString();
                String id = String.valueOf(System.currentTimeMillis());

                Habit h = new Habit(id, name, category, 21, 0, false);
                habitsList.add(h);
                saveHabits();
                refreshHabitsUI();

                editHabitName.setText("");
                Toast.makeText(MainActivity.this, "Mindful habit initialized", Toast.LENGTH_SHORT).show();
            }
        });

        setupTimerControls();
        setupMoodSelectors();
        loadMoodHistory();
    }

    private void switchTab(int index) {
        layoutHabitsTab.setVisibility(index == 1 ? View.VISIBLE : View.GONE);
        layoutTimerTab.setVisibility(index == 2 ? View.VISIBLE : View.GONE);
        layoutJournalTab.setVisibility(index == 3 ? View.VISIBLE : View.GONE);

        navHabitsBtn.setTextColor(index == 1 ? Color.parseColor("#00796B") : Color.parseColor("#757575"));
        navTimerBtn.setTextColor(index == 2 ? Color.parseColor("#00796B") : Color.parseColor("#757575"));
        navJournalBtn.setTextColor(index == 3 ? Color.parseColor("#00796B") : Color.parseColor("#757575"));
    }

    private void setupCategorySpinner() {
        String[] list = {"Health", "Mind", "Productivity", "Creativity"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, list);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);
    }

    private void loadHabits() {
        habitsList.clear();
        SharedPreferences p = getSharedPreferences("zen_prefs", MODE_PRIVATE);
        String idsStr = p.getString("habit_ids", "");
        if (!idsStr.isEmpty()) {
            String[] split = idsStr.split(",");
            for (String s : split) {
                String name = p.getString("habit_name_" + s, "");
                String category = p.getString("habit_cat_" + s, "General");
                int target = p.getInt("habit_target_" + s, 21);
                int done = p.getInt("habit_completed_" + s, 0);
                boolean today = p.getBoolean("habit_done_today_" + s, false);

                habitsList.add(new Habit(s, name, category, target, done, today));
            }
        } else {
            habitsList.add(new Habit("990", "Deep Breathing Session", "Mind", 21, 6, false));
            habitsList.add(new Habit("991", "Water Consumption Goal", "Health", 21, 3, true));
            saveHabits();
        }
    }

    private void saveHabits() {
        SharedPreferences p = getSharedPreferences("zen_prefs", MODE_PRIVATE);
        SharedPreferences.Editor e = p.edit();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < habitsList.size(); i++) {
            Habit h = habitsList.get(i);
            sb.append(h.getId());
            if (i < habitsList.size() - 1) {
                sb.append(",");
            }
            e.putString("habit_name_" + h.getId(), h.getName());
            e.putString("habit_cat_" + h.getId(), h.getCategory());
            e.putInt("habit_target_" + h.getId(), h.getTargetDays());
            e.putInt("habit_completed_" + h.getId(), h.getCompletedDays());
            e.putBoolean("habit_done_today_" + h.getId(), h.isCompletedToday());
        }
        e.putString("habit_ids", sb.toString());
        e.apply();
    }

    private void deleteHabit(String id) {
        SharedPreferences p = getSharedPreferences("zen_prefs", MODE_PRIVATE);
        SharedPreferences.Editor e = p.edit();
        e.remove("habit_name_" + id);
        e.remove("habit_cat_" + id);
        e.remove("habit_target_" + id);
        e.remove("habit_completed_" + id);
        e.remove("habit_done_today_" + id);
        e.apply();

        for (int i = 0; i < habitsList.size(); i++) {
            if (habitsList.get(i).getId().equals(id)) {
                habitsList.remove(i);
                break;
            }
        }
        saveHabits();
        refreshHabitsUI();
    }

    private void refreshHabitsUI() {
        LinearLayout container = (LinearLayout) findViewById(R.id.habits_list_container);
        container.removeAllViews();

        int size = habitsList.size();
        int doneCount = 0;

        for (int i = 0; i < size; i++) {
            final Habit item = habitsList.get(i);

            LinearLayout block = new LinearLayout(this);
            block.setOrientation(LinearLayout.HORIZONTAL);
            block.setPadding(32, 24, 32, 24);

            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            p.setMargins(0, 0, 0, 16);
            block.setLayoutParams(p);
            block.setBackground(getResources().getDrawable(R.drawable.card_background));
            block.setGravity(android.view.Gravity.CENTER_VERTICAL);

            LinearLayout details = new LinearLayout(this);
            details.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams detParam = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f
            );
            details.setLayoutParams(detParam);

            TextView title = new TextView(this);
            title.setText(item.getName());
            title.setTextSize(16);
            title.setTypeface(null, android.graphics.Typeface.BOLD);
            title.setTextColor(Color.parseColor("#212121"));

            TextView meta = new TextView(this);
            meta.setText(item.getCategory() + " • Streak: " + item.getCompletedDays() + " days");
            meta.setTextSize(12);
            meta.setTextColor(Color.parseColor("#757575"));

            details.addView(title);
            details.addView(meta);

            CheckBox doneCheck = new CheckBox(this);
            doneCheck.setChecked(item.isCompletedToday());
            doneCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    item.setCompletedToday(isChecked);
                    saveHabits();
                    refreshHabitsUI();
                }
            });

            if (item.isCompletedToday()) {
                doneCount++;
            }

            Button del = new Button(this, null, android.R.attr.buttonStyleSmall);
            del.setText("✕");
            del.setTextColor(Color.RED);
            del.setBackgroundColor(Color.TRANSPARENT);
            del.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    deleteHabit(item.getId());
                }
            });

            block.addView(details);
            block.addView(doneCheck);
            block.addView(del);

            container.addView(block);
        }

        ProgressBar bar = (ProgressBar) findViewById(R.id.habits_progress_bar);
        TextView txt = (TextView) findViewById(R.id.habits_progress_text);
        if (size > 0) {
            int ratio = (doneCount * 100) / size;
            bar.setProgress(ratio);
            txt.setText(ratio + "% habits completed today (" + doneCount + "/" + size + ")");
        } else {
            bar.setProgress(0);
            txt.setText("No habits. Click above to add some goals!");
        }
    }

    private void setupTimerControls() {
        btnTimerStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isTimerActive) {
                    pauseTimer();
                } else {
                    startTimer();
                }
            }
        });

        btnTimerReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetTimer();
            }
        });

        btnPresetFocus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setTimerDuration(1500000);
            }
        });

        btnPresetShort.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setTimerDuration(60000);
            }
        });

        btnPresetLong.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setTimerDuration(600000);
            }
        });
    }

    private void setTimerDuration(long val) {
        pauseTimer();
        timeRemainingMillis = val;
        updateTimerText();
    }

    private void startTimer() {
        isTimerActive = true;
        btnTimerStart.setText("Pause");
        timerVisualizer.setTimerRunning(true);

        countDownTimer = new CountDownTimer(timeRemainingMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeRemainingMillis = millisUntilFinished;
                updateTimerText();
            }

            @Override
            public void onFinish() {
                isTimerActive = false;
                timerVisualizer.setTimerRunning(false);
                btnTimerStart.setText("Start Focus");
                Toast.makeText(MainActivity.this, "Calm sound finished! Focus interval complete.", Toast.LENGTH_LONG).show();
                resetTimer();
            }
        };
        countDownTimer.start();
        animationHandler.post(animationRunnable);
    }

    private void pauseTimer() {
        isTimerActive = false;
        btnTimerStart.setText("Resume");
        timerVisualizer.setTimerRunning(false);
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    private void resetTimer() {
        pauseTimer();
        timeRemainingMillis = 1500000;
        updateTimerText();
        btnTimerStart.setText("Start Focus");
    }

    private void updateTimerText() {
        int m = (int) (timeRemainingMillis / 1000) / 60;
        int s = (int) (timeRemainingMillis / 1000) % 60;
        String val = String.format("%02d:%02d", m, s);
        timerCountdownText.setText(val);
        timerVisualizer.setTimerText(val);
    }

    private void setupMoodSelectors() {
        findViewById(R.id.btn_mood_1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { selectMood(5, "Peaceful"); }
        });
        findViewById(R.id.btn_mood_2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { selectMood(4, "Joyful"); }
        });
        findViewById(R.id.btn_mood_3).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { selectMood(3, "Calm"); }
        });
        findViewById(R.id.btn_mood_4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { selectMood(2, "Anxious"); }
        });
        findViewById(R.id.btn_mood_5).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { selectMood(1, "Tired"); }
        });

        btnSaveMood.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveMoodLog(selectedMoodScore);
                editMoodNote.setText("");
                Toast.makeText(MainActivity.this, "Daily journal entry completed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void selectMood(int value, String label) {
        selectedMoodScore = value;
        txtSelectedMood.setText("Selected Mood: " + label + " (" + value + ")");
    }

    private void loadMoodHistory() {
        SharedPreferences p = getSharedPreferences("zen_prefs", MODE_PRIVATE);
        String history = p.getString("mood_history", "3,4,4,5,3");
        String[] split = history.split(",");
        List<Integer> list = new ArrayList<>();
        for (String s : split) {
            if (!s.trim().isEmpty()) {
                try {
                    list.add(Integer.parseInt(s));
                } catch (NumberFormatException exc) {
                    // ignore format errors
                }
            }
        }
        CustomChartView view = (CustomChartView) findViewById(R.id.custom_chart_view);
        view.setData(list);
    }

    private void saveMoodLog(int value) {
        SharedPreferences p = getSharedPreferences("zen_prefs", MODE_PRIVATE);
        String history = p.getString("mood_history", "3,4,4,5,3");
        if (!history.isEmpty()) {
            history += "," + value;
        } else {
            history = String.valueOf(value);
        }
        String[] split = history.split(",");
        if (split.length > 7) {
            StringBuilder sb = new StringBuilder();
            for (int i = split.length - 7; i < split.length; i++) {
                sb.append(split[i]);
                if (i < split.length - 1) {
                    sb.append(",");
                }
            }
            history = sb.toString();
        }
        p.edit().putString("mood_history", history).apply();
        loadMoodHistory();
    }
}