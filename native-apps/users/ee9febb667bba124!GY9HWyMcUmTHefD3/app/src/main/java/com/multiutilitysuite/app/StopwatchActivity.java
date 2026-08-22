package com.multiutilitysuite.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class StopwatchActivity extends Activity {
    private TextView timerView;
    private Button startButton, pauseButton, resetButton;
    private Handler customHandler = new Handler();
    private long startTime = 0L;
    private long timeInMilliseconds = 0L;
    private long timeSwapBuff = 0L;
    private long updatedTime = 0L;
    private boolean isRunning = false;

    private Runnable updateTimerThread = new Runnable() {
        public void run() {
            timeInMilliseconds = SystemClock.uptimeMillis() - startTime;
            updatedTime = timeSwapBuff + timeInMilliseconds;
            int secs = (int) (updatedTime / 1000);
            int mins = secs / 60;
            secs = secs % 60;
            int milliseconds = (int) (updatedTime % 1000);
            timerView.setText("" + mins + ":" 
                + String.format("%02d", secs) + ":"
                + String.format("%03d", milliseconds));
            customHandler.postDelayed(this, 10);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout parent = new LinearLayout(this);
        parent.setOrientation(LinearLayout.VERTICAL);
        parent.setBackgroundColor(Color.parseColor("#F5F5F5"));

        // Custom Header Bar
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setBackgroundColor(Color.parseColor("#3F51B5"));
        header.setPadding(32, 24, 32, 24);
        header.setGravity(Gravity.CENTER_VERTICAL);

        Button backButton = new Button(this);
        backButton.setText("< Back");
        backButton.setTextColor(Color.WHITE);
        backButton.setBackgroundColor(Color.TRANSPARENT);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        header.addView(backButton);

        TextView headerTitle = new TextView(this);
        headerTitle.setText("Digital Stopwatch");
        headerTitle.setTextColor(Color.WHITE);
        headerTitle.setTextSize(20);
        headerTitle.setPadding(32, 0, 0, 0);
        header.addView(headerTitle);
        parent.addView(header);

        // Time Indicator Panel
        timerView = new TextView(this);
        timerView.setText("0:00:000");
        timerView.setTextSize(54);
        timerView.setGravity(Gravity.CENTER);
        timerView.setTextColor(Color.parseColor("#212121"));
        LinearLayout.LayoutParams timerParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f);
        timerView.setLayoutParams(timerParams);
        parent.addView(timerView);

        // Control Buttons Layout
        LinearLayout controlLayout = new LinearLayout(this);
        controlLayout.setOrientation(LinearLayout.VERTICAL);
        controlLayout.setPadding(32, 32, 32, 64);

        startButton = new Button(this);
        startButton.setText("START");
        startButton.setBackgroundColor(Color.parseColor("#4CAF50"));
        startButton.setTextColor(Color.WHITE);
        startButton.setPadding(0, 32, 0, 32);
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        btnParams.setMargins(0, 16, 0, 16);
        startButton.setLayoutParams(btnParams);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isRunning) {
                    startTime = SystemClock.uptimeMillis();
                    customHandler.postDelayed(updateTimerThread, 0);
                    isRunning = true;
                }
            }
        });
        controlLayout.addView(startButton);

        pauseButton = new Button(this);
        pauseButton.setText("PAUSE");
        pauseButton.setBackgroundColor(Color.parseColor("#FF9800"));
        pauseButton.setTextColor(Color.WHITE);
        pauseButton.setPadding(0, 32, 0, 32);
        pauseButton.setLayoutParams(btnParams);
        pauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isRunning) {
                    timeSwapBuff += timeInMilliseconds;
                    customHandler.removeCallbacks(updateTimerThread);
                    isRunning = false;
                }
            }
        });
        controlLayout.addView(pauseButton);

        resetButton = new Button(this);
        resetButton.setText("RESET");
        resetButton.setBackgroundColor(Color.parseColor("#F44336"));
        resetButton.setTextColor(Color.WHITE);
        resetButton.setPadding(0, 32, 0, 32);
        resetButton.setLayoutParams(btnParams);
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startTime = 0L;
                timeInMilliseconds = 0L;
                timeSwapBuff = 0L;
                updatedTime = 0L;
                customHandler.removeCallbacks(updateTimerThread);
                timerView.setText("0:00:000");
                isRunning = false;
            }
        });
        controlLayout.addView(resetButton);

        parent.addView(controlLayout);
        setContentView(parent);
    }
}