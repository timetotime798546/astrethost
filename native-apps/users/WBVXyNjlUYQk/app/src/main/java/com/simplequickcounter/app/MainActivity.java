package com.simplequickcounter.app;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final String PREFS_NAME = "SimpleCounterPrefs";
    private static final String KEY_COUNT = "counter_value";
    private static final String KEY_NOTES = "quick_notes";
    private static final String KEY_THEME = "app_theme"; // 0: Light, 1: Warm, 2: Dark

    private int count = 0;
    private int activeTheme = 0;

    private TextView counterValueText;
    private EditText etCustomStep;
    private EditText etQuickNotes;
    private ScrollView scrollView;
    private LinearLayout mainContainer;
    private TextView appTitle;
    private TextView appSubtitle;

    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bind layout resources to UI components
        scrollView = (ScrollView) findViewById(R.id.main_scroll_view);
        mainContainer = (LinearLayout) findViewById(R.id.main_container);
        appTitle = (TextView) findViewById(R.id.app_title);
        appSubtitle = (TextView) findViewById(R.id.app_subtitle);
        counterValueText = (TextView) findViewById(R.id.counter_value);
        etCustomStep = (EditText) findViewById(R.id.et_custom_step);
        etQuickNotes = (EditText) findViewById(R.id.et_quick_notes);

        Button btnDecrement = (Button) findViewById(R.id.btn_decrement);
        Button btnReset = (Button) findViewById(R.id.btn_reset);
        Button btnIncrement = (Button) findViewById(R.id.btn_increment);
        Button btnCustomAdd = (Button) findViewById(R.id.btn_custom_add);
        
        Button btnThemeLight = (Button) findViewById(R.id.btn_theme_light);
        Button btnThemeWarm = (Button) findViewById(R.id.btn_theme_warm);
        Button btnThemeDark = (Button) findViewById(R.id.btn_theme_dark);

        // Access stored app settings configuration
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        count = sharedPreferences.getInt(KEY_COUNT, 0);
        activeTheme = sharedPreferences.getInt(KEY_THEME, 0);
        String savedNotes = sharedPreferences.getString(KEY_NOTES, "");

        // Feed initial saved attributes to system outputs
        updateCounterDisplay();
        etQuickNotes.setText(savedNotes);
        applyTheme(activeTheme);

        // Configure standard event observers through Java 8 compatible anonymous classes
        btnIncrement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                count++;
                updateCounterDisplay();
                saveState();
            }
        });

        btnDecrement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                count--;
                updateCounterDisplay();
                saveState();
            }
        });

        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                count = 0;
                updateCounterDisplay();
                saveState();
                Toast.makeText(MainActivity.this, "Counter Reset", Toast.LENGTH_SHORT).show();
            }
        });

        btnCustomAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String input = etCustomStep.getText().toString().trim();
                if (!input.isEmpty()) {
                    try {
                        int val = Integer.parseInt(input);
                        count += val;
                        updateCounterDisplay();
                        saveState();
                        etCustomStep.setText("");
                    } catch (NumberFormatException e) {
                        Toast.makeText(MainActivity.this, "Invalid number format", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Please write a value first", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Click handlers to control style variables dynamically
        btnThemeLight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyTheme(0);
            }
        });

        btnThemeWarm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyTheme(1);
            }
        });

        btnThemeDark.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyTheme(2);
            }
        });

        // Monitor real-time user keystrokes dynamically saving note entries
        etQuickNotes.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                saveNotesState(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void updateCounterDisplay() {
        counterValueText.setText(String.valueOf(count));
    }

    private void applyTheme(int themeCode) {
        activeTheme = themeCode;
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(KEY_THEME, activeTheme);
        editor.apply();

        if (themeCode == 0) {
            // Light Minimalist Theme
            scrollView.setBackgroundColor(Color.parseColor("#F5F5F5"));
            appTitle.setTextColor(Color.parseColor("#212121"));
            appSubtitle.setTextColor(Color.parseColor("#757575"));
        } else if (themeCode == 1) {
            // Warm Amber/Sepia Theme
            scrollView.setBackgroundColor(Color.parseColor("#FFF3E0"));
            appTitle.setTextColor(Color.parseColor("#5D4037"));
            appSubtitle.setTextColor(Color.parseColor("#8D6E63"));
        } else if (themeCode == 2) {
            // Deep Dark Theme
            scrollView.setBackgroundColor(Color.parseColor("#121212"));
            appTitle.setTextColor(Color.parseColor("#FFFFFF"));
            appSubtitle.setTextColor(Color.parseColor("#B0BEC5"));
        }
    }

    private void saveState() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(KEY_COUNT, count);
        editor.apply();
    }

    private void saveNotesState(String notes) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_NOTES, notes);
        editor.apply();
    }
}