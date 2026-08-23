package com.omnicontrolsuite.app;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import java.util.Locale;

public class MainActivity extends Activity {

    private TextView txtActiveTasks;
    private TextView txtSavedNotes;
    private TextView txtSystemUptime;
    private DatabaseHelper dbHelper;

    private long systemUptimeSeconds = 0;
    private Handler timerHandler = new Handler();
    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            systemUptimeSeconds++;
            long hrs = systemUptimeSeconds / 3600;
            long mins = (systemUptimeSeconds % 3600) / 60;
            long secs = systemUptimeSeconds % 60;
            txtSystemUptime.setText(String.format(Locale.getDefault(),
                    "System Engine Status: Active and Running\nUptime Counter: %02d:%02d:%02d", hrs, mins, secs));
            timerHandler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);

        txtActiveTasks = (TextView) findViewById(R.id.txtActiveTasks);
        txtSavedNotes = (TextView) findViewById(R.id.txtSavedNotes);
        txtSystemUptime = (TextView) findViewById(R.id.txtSystemUptime);

        Button btnTasks = (Button) findViewById(R.id.btnTaskManager);
        Button btnNotes = (Button) findViewById(R.id.btnNotes);
        Button btnCalculator = (Button) findViewById(R.id.btnCalculator);
        Button btnAnalytics = (Button) findViewById(R.id.btnAnalytics);
        Button btnSettings = (Button) findViewById(R.id.btnSettings);

        btnTasks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, TaskActivity.class));
            }
        });

        btnNotes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, NotesActivity.class));
            }
        });

        btnCalculator.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, CalculatorActivity.class));
            }
        });

        btnAnalytics.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, AnalyticsActivity.class));
            }
        });

        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            }
        });

        timerHandler.post(timerRunnable);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateDashboardStats();
    }

    private void updateDashboardStats() {
        int taskCount = 0;
        int noteCount = 0;

        Cursor taskCursor = dbHelper.getAllTasks();
        if (taskCursor != null) {
            taskCount = taskCursor.getCount();
            taskCursor.close();
        }

        Cursor noteCursor = dbHelper.getAllNotes();
        if (noteCursor != null) {
            noteCount = noteCursor.getCount();
            noteCursor.close();
        }

        txtActiveTasks.setText(String.valueOf(taskCount));
        txtSavedNotes.setText(String.valueOf(noteCount));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        timerHandler.removeCallbacks(timerRunnable);
    }
}