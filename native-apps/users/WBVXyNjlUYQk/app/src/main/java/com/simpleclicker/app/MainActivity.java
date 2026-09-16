package com.simpleclicker.app;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {

    private int counterValue = 0;
    private int highScoreValue = 0;
    private int totalClicksValue = 0;
    private int stepSize = 1;
    private boolean hapticEnabled = true;

    private TextView textCounter;
    private TextView textHighScore;
    private TextView textTotalClicks;
    private TextView textAchievement;
    private ThreeDObjectView threedView;

    private Button btnIncrement;
    private Button btnDecrement;
    private Button btnReset;

    private Button btnStep1;
    private Button btnStep5;
    private Button btnStep10;
    private Button btnHapticToggle;

    private static final String PREFS_NAME = "SimpleClickerPrefs";
    private static final String KEY_HIGH_SCORE = "high_score";
    private static final String KEY_TOTAL_CLICKS = "total_clicks";
    private static final String KEY_STEP_SIZE = "step_size";
    private static final String KEY_HAPTIC_ENABLED = "haptic_enabled";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Access stored metrics
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        highScoreValue = prefs.getInt(KEY_HIGH_SCORE, 0);
        totalClicksValue = prefs.getInt(KEY_TOTAL_CLICKS, 0);
        stepSize = prefs.getInt(KEY_STEP_SIZE, 1);
        hapticEnabled = prefs.getBoolean(KEY_HAPTIC_ENABLED, true);

        // Bind layout structures
        textCounter = (TextView) findViewById(R.id.text_counter);
        textHighScore = (TextView) findViewById(R.id.text_high_score);
        textTotalClicks = (TextView) findViewById(R.id.text_total_clicks);
        textAchievement = (TextView) findViewById(R.id.text_achievement);
        threedView = (ThreeDObjectView) findViewById(R.id.threed_view);

        btnIncrement = (Button) findViewById(R.id.btn_increment);
        btnDecrement = (Button) findViewById(R.id.btn_decrement);
        btnReset = (Button) findViewById(R.id.btn_reset);

        btnStep1 = (Button) findViewById(R.id.btn_step_1);
        btnStep5 = (Button) findViewById(R.id.btn_step_5);
        btnStep10 = (Button) findViewById(R.id.btn_step_10);
        btnHapticToggle = (Button) findViewById(R.id.btn_haptic_toggle);

        // Restore state on configuration changes (e.g. orientation swap)
        if (savedInstanceState != null) {
            counterValue = savedInstanceState.getInt("counterValue", 0);
            stepSize = savedInstanceState.getInt("stepSize", 1);
            hapticEnabled = savedInstanceState.getBoolean("hapticEnabled", true);
        }

        renderMetrics();
        updateStepSelectionUI();
        updateHapticToggleUI();

        // 3D Viewport Interaction callback mapping
        threedView.setOnThreeDClickListener(new ThreeDObjectView.OnThreeDClickListener() {
            @Override
            public void onThreeDClick() {
                counterValue += stepSize;
                totalClicksValue++;
                triggerHaptic(threedView);
                renderMetrics();
            }
        });

        // Setup standard button click actions
        btnIncrement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                counterValue += stepSize;
                totalClicksValue++;
                triggerHaptic(v);
                renderMetrics();
            }
        });

        btnDecrement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                counterValue -= stepSize;
                totalClicksValue++;
                triggerHaptic(v);
                renderMetrics();
            }
        });

        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                counterValue = 0;
                triggerHaptic(v);
                renderMetrics();
            }
        });

        btnStep1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stepSize = 1;
                triggerHaptic(v);
                commitLocalPreferences();
                updateStepSelectionUI();
            }
        });

        btnStep5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stepSize = 5;
                triggerHaptic(v);
                commitLocalPreferences();
                updateStepSelectionUI();
            }
        });

        btnStep10.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stepSize = 10;
                triggerHaptic(v);
                commitLocalPreferences();
                updateStepSelectionUI();
            }
        });

        btnHapticToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hapticEnabled = !hapticEnabled;
                triggerHaptic(v);
                commitLocalPreferences();
                updateHapticToggleUI();
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("counterValue", counterValue);
        outState.putInt("stepSize", stepSize);
        outState.putBoolean("hapticEnabled", hapticEnabled);
    }

    private void renderMetrics() {
        textCounter.setText(String.valueOf(counterValue));

        // Evaluate whether the current count sets a new genuine high score record
        boolean isNewHighScore = false;
        if (counterValue > highScoreValue) {
            highScoreValue = counterValue;
            isNewHighScore = true;
        }

        // Commit state changes to storage
        commitLocalPreferences();

        // Update 3D Canvas view with current status values
        if (threedView != null) {
            threedView.setCounterValue(counterValue);
            threedView.setHighScoreState(isNewHighScore, highScoreValue);
        }

        textHighScore.setText("High Score: " + highScoreValue);
        textTotalClicks.setText("Total Session Clicks: " + totalClicksValue);

        // Update user feedback text beautifully based on score targets
        if (isNewHighScore) {
            textAchievement.setText("🎉 NEW HIGH SCORE ACCUMULATED! 🎉");
            textAchievement.setTextColor(0xFF10B981); // beautiful mint green
        } else if (counterValue == highScoreValue && highScoreValue > 0) {
            textAchievement.setText("🏆 Match Point! You tied your high score!");
            textAchievement.setTextColor(0xFF2563EB); // modern royal blue
        } else if (highScoreValue > 0 && counterValue >= highScoreValue - 10 && counterValue < highScoreValue) {
            int diff = highScoreValue - counterValue;
            textAchievement.setText("🔥 Almost there! Just " + diff + " steps away!");
            textAchievement.setTextColor(0xFFF59E0B); // warm amber accent
        } else {
            textAchievement.setText("Swipe to Rotate • Tap to Counter");
            textAchievement.setTextColor(0xFF64748B); // neutral slate
        }
    }

    private void updateStepSelectionUI() {
        if (stepSize == 1) {
            btnStep1.setBackgroundResource(R.drawable.chip_selected);
            btnStep1.setTextColor(0xFFFFFFFF);
            btnStep5.setBackgroundResource(R.drawable.chip_unselected);
            btnStep5.setTextColor(0xFF475569);
            btnStep10.setBackgroundResource(R.drawable.chip_unselected);
            btnStep10.setTextColor(0xFF475569);
        } else if (stepSize == 5) {
            btnStep1.setBackgroundResource(R.drawable.chip_unselected);
            btnStep1.setTextColor(0xFF475569);
            btnStep5.setBackgroundResource(R.drawable.chip_selected);
            btnStep5.setTextColor(0xFFFFFFFF);
            btnStep10.setBackgroundResource(R.drawable.chip_unselected);
            btnStep10.setTextColor(0xFF475569);
        } else {
            btnStep1.setBackgroundResource(R.drawable.chip_unselected);
            btnStep1.setTextColor(0xFF475569);
            btnStep5.setBackgroundResource(R.drawable.chip_unselected);
            btnStep5.setTextColor(0xFF475569);
            btnStep10.setBackgroundResource(R.drawable.chip_selected);
            btnStep10.setTextColor(0xFFFFFFFF);
        }
    }

    private void updateHapticToggleUI() {
        if (hapticEnabled) {
            btnHapticToggle.setBackgroundResource(R.drawable.toggle_active);
            btnHapticToggle.setText("HAPTIC FEEDBACK: ON");
        } else {
            btnHapticToggle.setBackgroundResource(R.drawable.toggle_inactive);
            btnHapticToggle.setText("HAPTIC FEEDBACK: OFF");
        }
    }

    private void triggerHaptic(View v) {
        if (hapticEnabled) {
            v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
        }
    }

    private void commitLocalPreferences() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_HIGH_SCORE, highScoreValue);
        editor.putInt(KEY_TOTAL_CLICKS, totalClicksValue);
        editor.putInt(KEY_STEP_SIZE, stepSize);
        editor.putBoolean(KEY_HAPTIC_ENABLED, hapticEnabled);
        editor.apply();
    }
}