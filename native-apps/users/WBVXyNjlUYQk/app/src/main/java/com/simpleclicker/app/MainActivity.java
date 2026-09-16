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

    private TextView textCounter;
    private TextView textHighScore;
    private TextView textTotalClicks;

    private Button btnIncrement;
    private Button btnDecrement;
    private Button btnReset;

    private static final String PREFS_NAME = "SimpleClickerPrefs";
    private static final String KEY_HIGH_SCORE = "high_score";
    private static final String KEY_TOTAL_CLICKS = "total_clicks";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Access stored metrics
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        highScoreValue = prefs.getInt(KEY_HIGH_SCORE, 0);
        totalClicksValue = prefs.getInt(KEY_TOTAL_CLICKS, 0);

        // Bind layout structures
        textCounter = (TextView) findViewById(R.id.text_counter);
        textHighScore = (TextView) findViewById(R.id.text_high_score);
        textTotalClicks = (TextView) findViewById(R.id.text_total_clicks);

        btnIncrement = (Button) findViewById(R.id.btn_increment);
        btnDecrement = (Button) findViewById(R.id.btn_decrement);
        btnReset = (Button) findViewById(R.id.btn_reset);

        // Restore layout state configuration values on config alterations
        if (savedInstanceState != null) {
            counterValue = savedInstanceState.getInt("counterValue", 0);
        }

        renderMetrics();

        // Safe Java 8 traditional click listener actions
        btnIncrement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                counterValue++;
                totalClicksValue++;
                if (counterValue > highScoreValue) {
                    highScoreValue = counterValue;
                }
                commitLocalPreferences();
                renderMetrics();
            }
        });

        btnDecrement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                counterValue--;
                totalClicksValue++;
                commitLocalPreferences();
                renderMetrics();
            }
        });

        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                counterValue = 0;
                commitLocalPreferences();
                renderMetrics();
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("counterValue", counterValue);
    }

    private void renderMetrics() {
        textCounter.setText(String.valueOf(counterValue));
        textHighScore.setText("High Score: " + highScoreValue);
        textTotalClicks.setText("Total Session Clicks: " + totalClicksValue);
    }

    private void commitLocalPreferences() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_HIGH_SCORE, highScoreValue);
        editor.putInt(KEY_TOTAL_CLICKS, totalClicksValue);
        editor.apply();
    }
}