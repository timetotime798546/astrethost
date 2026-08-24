package com.arrowflowcrashcourse.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class HelpActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        boolean darkTheme = ThemeManager.isDarkMode(this);
        setTheme(darkTheme ? android.R.style.Theme_Material : android.R.style.Theme_Material_Light);
        setContentView(R.layout.activity_help);

        Button backBtn = findViewById(R.id.btn_back_help);
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}