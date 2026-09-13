package com.simplecounter.app;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    private static final String PREFS_NAME = "SimpleCounterPrefs";
    private static final String KEY_COUNT = "counter_value";
    private static final String KEY_COLOR = "theme_color";

    private int counter = 0;
    private int selectedColor = 0xFF009688; // Default Teal

    private TextView txtCount;
    private LinearLayout counterCard;
    private Button btnPlus;
    private Vibrator vibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI components
        txtCount = (TextView) findViewById(R.id.txt_count);
        counterCard = (LinearLayout) findViewById(R.id.counter_card);
        btnPlus = (Button) findViewById(R.id.btn_plus);
        Button btnMinus = (Button) findViewById(R.id.btn_minus);
        Button btnReset = (Button) findViewById(R.id.btn_reset);

        Button btnThemeTeal = (Button) findViewById(R.id.btn_theme_teal);
        Button btnThemeBlue = (Button) findViewById(R.id.btn_theme_blue);
        Button btnThemeOrange = (Button) findViewById(R.id.btn_theme_orange);
        Button btnThemePurple = (Button) findViewById(R.id.btn_theme_purple);

        // Get Vibrator service
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        // Load cached counter value and theme color
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        counter = prefs.getInt(KEY_COUNT, 0);
        selectedColor = prefs.getInt(KEY_COLOR, 0xFF009688);

        // Apply saved configurations
        updateCounterDisplay();
        applyThemeColor(selectedColor);

        // Setup increment click action
        btnPlus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                counter++;
                triggerVibration(40);
                updateCounterDisplay();
                saveCounterState();
            }
        });

        // Setup decrement click action
        btnMinus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (counter > 0) {
                    counter--;
                    triggerVibration(40);
                    updateCounterDisplay();
                    saveCounterState();
                } else {
                    triggerVibration(120); // Long vibrate to indicate limit reached
                }
            }
        });

        // Setup reset action
        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                counter = 0;
                triggerVibration(80);
                updateCounterDisplay();
                saveCounterState();
            }
        });

        // Theme selection clicks
        btnThemeTeal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateTheme(0xFF009688);
            }
        });

        btnThemeBlue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateTheme(0xFF2196F3);
            }
        });

        btnThemeOrange.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateTheme(0xFFFF9800);
            }
        });

        btnThemePurple.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateTheme(0xFF9C27B0);
            }
        });
    }

    private void updateCounterDisplay() {
        txtCount.setText(String.valueOf(counter));
    }

    private void saveCounterState() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_COUNT, counter);
        editor.apply();
    }

    private void updateTheme(int colorCode) {
        selectedColor = colorCode;
        applyThemeColor(selectedColor);
        triggerVibration(30);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_COLOR, selectedColor);
        editor.apply();
    }

    private void applyThemeColor(int colorCode) {
        // Dynamic styling configuration
        btnPlus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(colorCode));

        // Create a programmatic border card with a thick custom-colored stroke
        GradientDrawable cardBackground = new GradientDrawable();
        cardBackground.setShape(GradientDrawable.RECTANGLE);
        cardBackground.setColor(Color.WHITE);
        cardBackground.setCornerRadius(16);
        cardBackground.setStroke(6, colorCode);
        counterCard.setBackground(cardBackground);
    }

    private void triggerVibration(long ms) {
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(ms);
        }
    }
}