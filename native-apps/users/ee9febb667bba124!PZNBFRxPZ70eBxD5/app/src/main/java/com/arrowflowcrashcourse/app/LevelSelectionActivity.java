package com.arrowflowcrashcourse.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;

public class LevelSelectionActivity extends Activity {
    private ScoreManager scoreManager;
    private boolean darkTheme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        scoreManager = new ScoreManager(this);
        applyTheme();
        setContentView(R.layout.activity_level_selection);

        GridLayout gridLayout = findViewById(R.id.level_grid);
        int unlocked = scoreManager.getUnlockedLevel();

        int count = 10;
        for (int i = 1; i <= count; i++) {
            final int levelNum = i;
            Button btn = new Button(this);
            btn.setText("LEVEL " + levelNum);
            btn.setTextSize(18f);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(16, 16, 16, 16);
            btn.setLayoutParams(params);

            if (levelNum <= unlocked) {
                btn.setEnabled(true);
                btn.setBackgroundColor(ThemeManager.getPrimaryAccent(darkTheme));
                btn.setTextColor(Color.WHITE);
                btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(LevelSelectionActivity.this, GameActivity.class);
                        intent.putExtra("LEVEL_NUM", levelNum);
                        startActivity(intent);
                        finish();
                    }
                });
            } else {
                btn.setEnabled(false);
                btn.setBackgroundColor(darkTheme ? Color.parseColor("#1B2336") : Color.parseColor("#E2E8F0"));
                btn.setTextColor(darkTheme ? Color.parseColor("#475569") : Color.parseColor("#94A3B8"));
                btn.setText("Locked \uD83D\uDD12");
            }
            gridLayout.addView(btn);
        }
    }

    private void applyTheme() {
        darkTheme = ThemeManager.isDarkMode(this);
        setTheme(darkTheme ? android.R.style.Theme_Material : android.R.style.Theme_Material_Light);
    }
}