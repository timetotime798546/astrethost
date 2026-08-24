package com.arrowflowcrashcourse.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView; 

public class MainActivity extends Activity {
    private ScoreManager scoreManager;
    private boolean darkTheme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        scoreManager = new ScoreManager(this);
        applyTheme();
        setContentView(R.layout.activity_main);

        TextView highscoreText = findViewById(R.id.text_highscore);
        highscoreText.setText("Cosmic High Score: " + scoreManager.getHighScore());

        Button playButton = findViewById(R.id.btn_play);
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, LevelSelectionActivity.class);
                startActivity(intent);
            }
        });

        Button settingsButton = findViewById(R.id.btn_settings);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        });

        Button helpButton = findViewById(R.id.btn_help);
        helpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, HelpActivity.class);
                startActivity(intent);
            }
        });
    }

    private void applyTheme() {
        darkTheme = ThemeManager.isDarkMode(this);
        setTheme(darkTheme ? android.R.style.Theme_Material : android.R.style.Theme_Material_Light);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (darkTheme != ThemeManager.isDarkMode(this)) {
            recreate();
        } else {
            TextView highscoreText = findViewById(R.id.text_highscore);
            if (highscoreText != null) {
                highscoreText.setText("Cosmic High Score: " + scoreManager.getHighScore());
            }
        }
    }
}