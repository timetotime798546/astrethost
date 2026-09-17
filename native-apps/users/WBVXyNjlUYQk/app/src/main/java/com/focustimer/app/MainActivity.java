package com.focustimer.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {

    private SharedPreferences prefs;

    // Timer logic properties
    private long timeTotalInMillis = 25 * 60 * 1000L;
    private long timeRemainingInMillis = 25 * 60 * 1000L;
    private boolean isTimerRunning = false;
    private boolean isTimerPaused = false;
    private CountDownTimer countDownTimer;

    // Timer UI components
    private TextView tvCountdown;
    private TextView tvTimerStatus;
    private CircularProgressView circularProgress;

    private Button btnPreset5, btnPreset15, btnPreset25, btnPreset45, btnPreset60;
    private Button btnStart, btnPause, btnResume, btnReset;

    // View panels
    private View layoutTimerContainer;
    private View layoutHistoryContainer;
    private View layoutSettingsContainer;

    // Bottom Navigation Components
    private LinearLayout tabTimer, tabHistory, tabSettings;
    private TextView tvTabTimer, tvTabHistory, tvTabSettings;

    // History UI metrics
    private TextView tvStatsCount, tvStatsTime;
    private LinearLayout historyLogsContainer;
    private TextView tvEmptyHistory;

    // Settings elements
    private Switch switchSound;
    private Spinner spinnerDefaultTime;
    private Button btnSettingsClearData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("FocusTimerPrefs", Context.MODE_PRIVATE);

        initViews();
        setupNavigation();
        setupPresets();
        setupTimerControls();
        setupSettings();
        loadInitialState(savedInstanceState);
        
        // Dynamic Startup animation flow
        layoutTimerContainer.setAlpha(0f);
        layoutTimerContainer.setTranslationY(45f);
        layoutTimerContainer.animate().alpha(1f).translationY(0f).setDuration(700).start();
    }

    private void initViews() {
        tvCountdown = (TextView) findViewById(R.id.tv_countdown);
        tvTimerStatus = (TextView) findViewById(R.id.tv_timer_status);
        circularProgress = (CircularProgressView) findViewById(R.id.circular_progress);

        btnPreset5 = (Button) findViewById(R.id.btn_preset_5);
        btnPreset15 = (Button) findViewById(R.id.btn_preset_15);
        btnPreset25 = (Button) findViewById(R.id.btn_preset_25);
        btnPreset45 = (Button) findViewById(R.id.btn_preset_45);
        btnPreset60 = (Button) findViewById(R.id.btn_preset_60);

        btnStart = (Button) findViewById(R.id.btn_start);
        btnPause = (Button) findViewById(R.id.btn_pause);
        btnResume = (Button) findViewById(R.id.btn_resume);
        btnReset = (Button) findViewById(R.id.btn_reset);

        layoutTimerContainer = findViewById(R.id.layout_timer_container);
        layoutHistoryContainer = findViewById(R.id.layout_history_container);
        layoutSettingsContainer = findViewById(R.id.layout_settings_container);

        tabTimer = (LinearLayout) findViewById(R.id.tab_timer);
        tabHistory = (LinearLayout) findViewById(R.id.tab_history);
        tabSettings = (LinearLayout) findViewById(R.id.tab_settings);

        tvTabTimer = (TextView) findViewById(R.id.tv_tab_timer);
        tvTabHistory = (TextView) findViewById(R.id.tv_tab_history);
        tvTabSettings = (TextView) findViewById(R.id.tv_tab_settings);

        tvStatsCount = (TextView) findViewById(R.id.tv_stats_count);
        tvStatsTime = (TextView) findViewById(R.id.tv_stats_time);
        historyLogsContainer = (LinearLayout) findViewById(R.id.history_logs_container);
        tvEmptyHistory = (TextView) findViewById(R.id.tv_empty_history);
        findViewById(R.id.btn_clear_history).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showClearHistoryConfirmDialog();
            }
        });

        switchSound = (Switch) findViewById(R.id.switch_sound);
        spinnerDefaultTime = (Spinner) findViewById(R.id.spinner_default_time);
        btnSettingsClearData = (Button) findViewById(R.id.btn_settings_clear_data);
    }

    private void setupNavigation() {
        tabTimer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectTab(0);
            }
        });

        tabHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectTab(1);
            }
        });

        tabSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectTab(2);
            }
        });

        selectTab(0); // Default home panel
    }

    private void selectTab(int index) {
        tabTimer.setSelected(index == 0);
        tabHistory.setSelected(index == 1);
        tabSettings.setSelected(index == 2);

        tvTabTimer.setSelected(index == 0);
        tvTabHistory.setSelected(index == 1);
        tvTabSettings.setSelected(index == 2);

        layoutTimerContainer.setVisibility(index == 0 ? View.VISIBLE : View.GONE);
        layoutHistoryContainer.setVisibility(index == 1 ? View.VISIBLE : View.GONE);
        layoutSettingsContainer.setVisibility(index == 2 ? View.VISIBLE : View.GONE);

        if (index == 1) {
            updateHistoryLogs();
        }
    }

    private void setupPresets() {
        View.OnClickListener presetListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int minutes = 25;
                if (v.getId() == R.id.btn_preset_5) minutes = 5;
                else if (v.getId() == R.id.btn_preset_15) minutes = 15;
                else if (v.getId() == R.id.btn_preset_25) minutes = 25;
                else if (v.getId() == R.id.btn_preset_45) minutes = 45;
                else if (v.getId() == R.id.btn_preset_60) minutes = 60;

                selectPreset(minutes);
            }
        };

        btnPreset5.setOnClickListener(presetListener);
        btnPreset15.setOnClickListener(presetListener);
        btnPreset25.setOnClickListener(presetListener);
        btnPreset45.setOnClickListener(presetListener);
        btnPreset60.setOnClickListener(presetListener);

        applyPressAnimation(btnPreset5);
        applyPressAnimation(btnPreset15);
        applyPressAnimation(btnPreset25);
        applyPressAnimation(btnPreset45);
        applyPressAnimation(btnPreset60);
    }

    private void selectPreset(int minutes) {
        if (isTimerRunning) {
            if (countDownTimer != null) {
                countDownTimer.cancel();
            }
            isTimerRunning = false;
            isTimerPaused = false;
        }

        timeTotalInMillis = minutes * 60L * 1000L;
        timeRemainingInMillis = timeTotalInMillis;

        updatePresetButtonSelection(minutes);
        updateTimerUI();
        updateButtonStates();
    }

    private void updatePresetButtonSelection(int currentMinutes) {
        btnPreset5.setSelected(currentMinutes == 5);
        btnPreset15.setSelected(currentMinutes == 15);
        btnPreset25.setSelected(currentMinutes == 25);
        btnPreset45.setSelected(currentMinutes == 45);
        btnPreset60.setSelected(currentMinutes == 60);
    }

    private void setupTimerControls() {
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startTimer(timeRemainingInMillis);
            }
        });

        btnPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pauseTimer();
            }
        });

        btnResume.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startTimer(timeRemainingInMillis);
            }
        });

        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetTimer();
            }
        });

        applyPressAnimation(btnStart);
        applyPressAnimation(btnPause);
        applyPressAnimation(btnResume);
        applyPressAnimation(btnReset);
    }

    private void startTimer(long durationInMillis) {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        // Timer ticked every 50ms to offer extremely smooth rendering of circle canvas
        countDownTimer = new CountDownTimer(durationInMillis, 50) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeRemainingInMillis = millisUntilFinished;
                updateTimerUI();
            }

            @Override
            public void onFinish() {
                timeRemainingInMillis = 0;
                updateTimerUI();
                onTimerFinished();
            }
        };

        countDownTimer.start();
        isTimerRunning = true;
        isTimerPaused = false;
        tvTimerStatus.setText("FOCUS ACTIVE");
        updateButtonStates();
    }

    private void pauseTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        isTimerRunning = false;
        isTimerPaused = true;
        tvTimerStatus.setText("PAUSED");
        updateButtonStates();
    }

    private void resetTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        isTimerRunning = false;
        isTimerPaused = false;
        
        int defaultIndex = prefs.getInt("default_duration_index", 2);
        int defaultMinutes = getMinutesFromIndex(defaultIndex);
        timeTotalInMillis = defaultMinutes * 60L * 1000L;
        timeRemainingInMillis = timeTotalInMillis;
        
        tvTimerStatus.setText("STAY FOCUSED");
        updatePresetButtonSelection(defaultMinutes);
        updateTimerUI();
        updateButtonStates();
    }

    private void updateTimerUI() {
        int minutes = (int) (timeRemainingInMillis / 1000) / 60;
        int seconds = (int) (timeRemainingInMillis / 1000) % 60;
        tvCountdown.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));

        circularProgress.setMaxProgress(timeTotalInMillis);
        circularProgress.setProgress(timeRemainingInMillis);
    }

    private void updateButtonStates() {
        if (!isTimerRunning && !isTimerPaused) {
            btnStart.setVisibility(View.VISIBLE);
            btnPause.setVisibility(View.GONE);
            btnResume.setVisibility(View.GONE);
        } else if (isTimerRunning) {
            btnStart.setVisibility(View.GONE);
            btnPause.setVisibility(View.VISIBLE);
            btnResume.setVisibility(View.GONE);
        } else {
            btnStart.setVisibility(View.GONE);
            btnPause.setVisibility(View.GONE);
            btnResume.setVisibility(View.VISIBLE);
        }
    }

    private void onTimerFinished() {
        isTimerRunning = false;
        isTimerPaused = false;
        updateButtonStates();
        tvTimerStatus.setText("STAY FOCUSED");

        int durationMinutes = (int) (timeTotalInMillis / (60 * 1000));
        saveCompletedSession(durationMinutes);

        boolean soundOn = prefs.getBoolean("sound_enabled", true);
        if (soundOn) {
            playNotificationSound();
        }

        showCompletionDialog(durationMinutes);
    }

    private void playNotificationSound() {
        try {
            android.net.Uri notification = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION);
            android.media.Ringtone r = android.media.RingtoneManager.getRingtone(getApplicationContext(), notification);
            r.play();
        } catch (Exception e) {
            try {
                android.media.ToneGenerator tg = new android.media.ToneGenerator(android.media.AudioManager.STREAM_NOTIFICATION, 100);
                tg.startTone(android.media.ToneGenerator.TONE_PROP_BEEP2, 1100);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void showCompletionDialog(int minutes) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_complete, null);
        builder.setView(dialogView);

        final AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        if (dialog.getWindow() != null) {
            dialog.getWindow().getAttributes().windowAnimations = android.R.style.Animation_Dialog;
        }

        TextView tvDialogMessage = (TextView) dialogView.findViewById(R.id.tv_dialog_message);
        tvDialogMessage.setText("Excellent work! You focused for " + minutes + " minutes. Take a well-deserved short break!");

        TextView tvDialogIcon = (TextView) dialogView.findViewById(R.id.tv_dialog_icon);
        android.view.animation.ScaleAnimation pulse = new android.view.animation.ScaleAnimation(
                1f, 1.14f, 1f, 1.14f,
                android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f,
                android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f
        );
        pulse.setDuration(550);
        pulse.setRepeatCount(android.view.animation.Animation.INFINITE);
        pulse.setRepeatMode(android.view.animation.Animation.REVERSE);
        tvDialogIcon.startAnimation(pulse);

        Button btnDismiss = (Button) dialogView.findViewById(R.id.btn_dialog_dismiss);
        btnDismiss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                resetTimer();
            }
        });

        dialog.show();
    }

    private void saveCompletedSession(int minutes) {
        String history = prefs.getString("history_data", "");
        long timestamp = System.currentTimeMillis();
        String item = timestamp + "|" + minutes;
        
        if (history.isEmpty()) {
            history = item;
        } else {
            history = history + ";" + item;
        }
        
        prefs.edit().putString("history_data", history).apply();
    }

    private void updateHistoryLogs() {
        historyLogsContainer.removeAllViews();

        String history = prefs.getString("history_data", "");
        if (history.isEmpty()) {
            tvStatsCount.setText("0");
            tvStatsTime.setText("0");
            tvEmptyHistory.setVisibility(View.VISIBLE);
            return;
        }

        String[] items = history.split(";");
        int todaySessions = 0;
        int todayMinutes = 0;

        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        long startOfToday = today.getTimeInMillis();

        for (int i = items.length - 1; i >= 0; i--) {
            String item = items[i];
            if (item.trim().isEmpty()) continue;

            String[] parts = item.split("\\|");
            if (parts.length < 2) continue;

            try {
                long timestamp = Long.parseLong(parts[0]);
                int duration = Integer.parseInt(parts[1]);

                if (timestamp >= startOfToday) {
                    todaySessions++;
                    todayMinutes += duration;

                    LinearLayout row = new LinearLayout(this);
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    row.setLayoutParams(new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    ));
                    row.setPadding(0, 24, 0, 24);

                    TextView tvLeft = new TextView(this);
                    tvLeft.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                    tvLeft.setText("⚡ Focus Session - " + duration + " min");
                    tvLeft.setTextColor(0xFF212121);
                    tvLeft.setTextSize(14.5f);

                    TextView tvRight = new TextView(this);
                    tvRight.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                    SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                    tvRight.setText(sdf.format(new Date(timestamp)));
                    tvRight.setTextColor(0xFF757575);
                    tvRight.setTextSize(12.5f);

                    row.addView(tvLeft);
                    row.addView(tvRight);

                    View divider = new View(this);
                    divider.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 2));
                    divider.setBackgroundColor(0xFFEEEEEE);

                    historyLogsContainer.addView(row);
                    historyLogsContainer.addView(divider);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        tvStatsCount.setText(String.valueOf(todaySessions));
        tvStatsTime.setText(String.valueOf(todayMinutes));

        if (todaySessions == 0) {
            tvEmptyHistory.setVisibility(View.VISIBLE);
        } else {
            tvEmptyHistory.setVisibility(View.GONE);
        }
    }

    private void showClearHistoryConfirmDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Clear Focus History")
                .setMessage("Are you sure you want to permanently clear all focus logs?")
                .setPositiveButton("Clear All", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        clearHistoryData();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void clearHistoryData() {
        prefs.edit().putString("history_data", "").apply();
        updateHistoryLogs();
    }

    private void setupSettings() {
        boolean soundEnabled = prefs.getBoolean("sound_enabled", true);
        switchSound.setChecked(soundEnabled);
        switchSound.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.edit().putBoolean("sound_enabled", isChecked).apply();
            }
        });

        String[] timeOptions = {"5 Minutes", "15 Minutes", "25 Minutes", "45 Minutes", "60 Minutes"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, timeOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDefaultTime.setAdapter(adapter);

        int defaultIndex = prefs.getInt("default_duration_index", 2); 
        spinnerDefaultTime.setSelection(defaultIndex);

        spinnerDefaultTime.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                prefs.edit().putInt("default_duration_index", position).apply();
                if (!isTimerRunning && !isTimerPaused) {
                    resetTimer();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnSettingsClearData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showClearHistoryConfirmDialog();
            }
        });
        applyPressAnimation(btnSettingsClearData);
    }

    private int getMinutesFromIndex(int index) {
        switch (index) {
            case 0: return 5;
            case 1: return 15;
            case 2: return 25;
            case 3: return 45;
            case 4: return 60;
            default: return 25;
        }
    }

    private void loadInitialState(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            timeTotalInMillis = savedInstanceState.getLong("timeTotalInMillis", 25 * 60 * 1000L);
            timeRemainingInMillis = savedInstanceState.getLong("timeRemainingInMillis", 25 * 60 * 1000L);
            isTimerRunning = savedInstanceState.getBoolean("isTimerRunning", false);
            isTimerPaused = savedInstanceState.getBoolean("isTimerPaused", false);

            if (isTimerRunning) {
                startTimer(timeRemainingInMillis);
            } else {
                updateTimerUI();
                updateButtonStates();
            }
        } else {
            resetTimer();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong("timeTotalInMillis", timeTotalInMillis);
        outState.putLong("timeRemainingInMillis", timeRemainingInMillis);
        outState.putBoolean("isTimerRunning", isTimerRunning);
        outState.putBoolean("isTimerPaused", isTimerPaused);

        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    public static void applyPressAnimation(final View view) {
        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, android.view.MotionEvent event) {
                switch (event.getAction()) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(80).start();
                        break;
                    case android.view.MotionEvent.ACTION_UP:
                    case android.view.MotionEvent.ACTION_CANCEL:
                        v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(80).start();
                        break;
                }
                return false;
            }
        });
    }
}