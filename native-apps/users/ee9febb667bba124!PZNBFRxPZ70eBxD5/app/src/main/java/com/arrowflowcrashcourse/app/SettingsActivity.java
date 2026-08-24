package com.arrowflowcrashcourse.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.Toast;

public class SettingsActivity extends Activity {
    private ScoreManager scoreManager;
    private boolean darkTheme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        scoreManager = new ScoreManager(this);
        darkTheme = ThemeManager.isDarkMode(this);
        setTheme(darkTheme ? android.R.style.Theme_Material : android.R.style.Theme_Material_Light);
        setContentView(R.layout.activity_settings);

        final Switch themeSwitch = findViewById(R.id.switch_theme);
        themeSwitch.setChecked(darkTheme);
        themeSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean isChecked = themeSwitch.isChecked();
                ThemeManager.setDarkMode(SettingsActivity.this, isChecked);
                recreate();
            }
        });

        Button resetBtn = findViewById(R.id.btn_reset_scores);
        resetBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scoreManager.resetAll();
                Toast.makeText(SettingsActivity.this, "Scores & Levels Reset Completed!", Toast.LENGTH_SHORT).show();
            }
        });

        Button backBtn = findViewById(R.id.btn_back_settings);
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}