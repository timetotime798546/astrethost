package com.simpleclicker.app;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends Activity {

    private int counterValue = 0;
    private int highScoreValue = 0;
    private int totalClicksValue = 0;
    private int stepSize = 1;
    private boolean hapticEnabled = true;

    private ThreeDCanvasView threeDCanvas;

    private static final String PREFS_NAME = "SimpleClicker3DPrefs";
    private static final String KEY_HIGH_SCORE = "high_score";
    private static final String KEY_TOTAL_CLICKS = "total_clicks";
    private static final String KEY_STEP_SIZE = "step_size";
    private static final String KEY_HAPTIC_ENABLED = "haptic_enabled";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Retrieve saved stats
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        highScoreValue = prefs.getInt(KEY_HIGH_SCORE, 0);
        totalClicksValue = prefs.getInt(KEY_TOTAL_CLICKS, 0);
        stepSize = prefs.getInt(KEY_STEP_SIZE, 1);
        hapticEnabled = prefs.getBoolean(KEY_HAPTIC_ENABLED, true);

        // Bind interactive 3D Canvas
        threeDCanvas = (ThreeDCanvasView) findViewById(R.id.three_d_canvas);

        // Synchronize startup configuration parameters with the 3D UI Renderer
        threeDCanvas.setCountValue(counterValue);
        threeDCanvas.setHighScoreValue(highScoreValue);
        threeDCanvas.setActiveStep(stepSize);
        threeDCanvas.setHapticEnabled(hapticEnabled);

        // Restore layout state configurations
        if (savedInstanceState != null) {
            counterValue = savedInstanceState.getInt("counterValue", 0);
            stepSize = savedInstanceState.getInt("stepSize", 1);
            hapticEnabled = savedInstanceState.getBoolean("hapticEnabled", true);
            threeDCanvas.setCountValue(counterValue);
            threeDCanvas.setActiveStep(stepSize);
            threeDCanvas.setHapticEnabled(hapticEnabled);
        }

        renderMetrics();

        // Connect high fidelity interactive triggers directly to 3D controls
        threeDCanvas.setOnUIActionListener(new ThreeDCanvasView.OnUIActionListener() {
            @Override
            public void onIncrement() {
                incrementCounter();
            }

            @Override
            public void onDecrement() {
                decrementCounter();
            }

            @Override
            public void onReset() {
                resetCounter();
            }

            @Override
            public void onStepSelected(int step) {
                stepSize = step;
                threeDCanvas.setActiveStep(step);
                commitLocalPreferences();
            }

            @Override
            public void onHapticToggled() {
                hapticEnabled = !hapticEnabled;
                threeDCanvas.setHapticEnabled(hapticEnabled);
                commitLocalPreferences();
            }
        });
    }

    private void incrementCounter() {
        counterValue += stepSize;
        totalClicksValue++;
        renderMetrics();
    }

    private void decrementCounter() {
        counterValue -= stepSize;
        totalClicksValue++;
        renderMetrics();
    }

    private void resetCounter() {
        counterValue = 0;
        renderMetrics();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("counterValue", counterValue);
        outState.putInt("stepSize", stepSize);
        outState.putBoolean("hapticEnabled", hapticEnabled);
    }

    private void renderMetrics() {
        boolean isNewHighScore = false;
        if (counterValue > highScoreValue) {
            highScoreValue = counterValue;
            isNewHighScore = true;
        }

        commitLocalPreferences();

        threeDCanvas.setCountValue(counterValue);
        threeDCanvas.setHighScoreValue(highScoreValue);

        // Change rotating gemstone theme colors dynamically based on achievement statuses
        if (isNewHighScore) {
            threeDCanvas.setThemeColor(0xFFEF4444); // Radiant Ruby
        } else if (counterValue == highScoreValue && highScoreValue > 0) {
            threeDCanvas.setThemeColor(0xFF3B82F6); // Sapphire Blue
        } else if (highScoreValue > 0 && counterValue >= highScoreValue - 10 && counterValue < highScoreValue) {
            threeDCanvas.setThemeColor(0xFFF59E0B); // Sparking Amber Gold
        } else {
            threeDCanvas.setThemeColor(0xFF10B981); // Emerald Green
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