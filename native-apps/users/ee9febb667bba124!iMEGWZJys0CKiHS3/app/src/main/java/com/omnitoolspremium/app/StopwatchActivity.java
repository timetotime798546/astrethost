package com.omnitoolspremium.app;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class StopwatchActivity extends Activity {

    private TextView txtTimeCounter;
    private TextView txtLaps;
    private Handler handler;
    
    private long startTime = 0L;
    private long timeInMilliseconds = 0L;
    private long timeSwapBuff = 0L;
    private long updatedTime = 0L;
    private boolean isRunning = false;
    private int lapCount = 1;

    private Runnable updateTimerRunnable = new Runnable() {
        @Override
        public void run() {
            timeInMilliseconds = SystemClock.uptimeMillis() - startTime;
            updatedTime = timeSwapBuff + timeInMilliseconds;

            int secs = (int) (updatedTime / 1000);
            int mins = secs / 60;
            secs = secs % 60;
            int milliseconds = (int) (updatedTime % 1000) / 10;

            txtTimeCounter.setText(String.format("%02d:%02d.%02d", mins, secs, milliseconds));
            if (isRunning) {
                handler.postDelayed(this, 20);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stopwatch);

        txtTimeCounter = (TextView) findViewById(R.id.txt_time_counter);
        txtLaps = (TextView) findViewById(R.id.txt_stopwatch_laps);
        Button btnStart = (Button) findViewById(R.id.btn_stopwatch_start);
        Button btnPause = (Button) findViewById(R.id.btn_stopwatch_pause);
        Button btnReset = (Button) findViewById(R.id.btn_stopwatch_reset);

        handler = new Handler();

        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isRunning) {
                    startTime = SystemClock.uptimeMillis();
                    isRunning = true;
                    handler.postDelayed(updateTimerRunnable, 0);
                }
            }
        });

        btnPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isRunning) {
                    timeSwapBuff += timeInMilliseconds;
                    isRunning = false;
                    handler.removeCallbacks(updateTimerRunnable);
                    
                    // Append lap record on pauses
                    String currentLap = "Lap " + lapCount + ": " + txtTimeCounter.getText().toString() + "\n";
                    txtLaps.append(currentLap);
                    lapCount++;
                }
            }
        });

        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isRunning = false;
                handler.removeCallbacks(updateTimerRunnable);
                startTime = 0L;
                timeInMilliseconds = 0L;
                timeSwapBuff = 0L;
                updatedTime = 0L;
                lapCount = 1;
                txtTimeCounter.setText("00:00.00");
                txtLaps.setText("");
            }
        });
    }
}