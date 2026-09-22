package com.simplecounter.app;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private int counter = 0;
    private int step = 1;
    private List<String> historyList = new ArrayList<String>();

    private TextView tvCounter;
    private TextView tvStatus;
    private TextView tvHistory;
    private Button btnIncrement;
    private Button btnDecrement;
    private Button btnReset;
    private Button btnStep;
    private EditText etNote;
    private Button btnSave;

    private static final String PREFS_NAME = "SimpleCounterPrefs";
    private static final String KEY_COUNTER = "counter_val";
    private static final String KEY_STEP = "step_val";
    private static final String KEY_NOTE = "note_val";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        tvCounter = (TextView) findViewById(R.id.tv_counter);
        tvStatus = (TextView) findViewById(R.id.tv_status);
        tvHistory = (TextView) findViewById(R.id.tv_history);
        btnIncrement = (Button) findViewById(R.id.btn_increment);
        btnDecrement = (Button) findViewById(R.id.btn_decrement);
        btnReset = (Button) findViewById(R.id.btn_reset);
        btnStep = (Button) findViewById(R.id.btn_step);
        etNote = (EditText) findViewById(R.id.et_note);
        btnSave = (Button) findViewById(R.id.btn_save);

        // Restore state
        loadPreferences();

        // Update UI displays
        updateCounterDisplay();
        updateStepDisplay();
        updateStatus("Session started.");
        addHistory("Session loaded");

        // Event Listeners (using anonymous inner classes for strict Java 8 compatibility)
        btnIncrement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                counter += step;
                updateCounterDisplay();
                updateStatus("Added " + step);
                addHistory("+" + step);
            }
        });

        btnDecrement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                counter -= step;
                updateCounterDisplay();
                updateStatus("Subtracted " + step);
                addHistory("-" + step);
            }
        });

        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                counter = 0;
                step = 1;
                updateCounterDisplay();
                updateStepDisplay();
                updateStatus("Counter reset");
                historyList.clear();
                addHistory("Reset");
            }
        });

        btnStep.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cycleStep();
                updateStepDisplay();
                updateStatus("Step changed to " + step);
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                savePreferences();
                Toast.makeText(MainActivity.this, "State & notes saved successfully!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void cycleStep() {
        if (step == 1) {
            step = 5;
        } else if (step == 5) {
            step = 10;
        } else if (step == 10) {
            step = 50;
        } else if (step == 50) {
            step = 100;
        } else {
            step = 1;
        }
    }

    private void updateCounterDisplay() {
        tvCounter.setText(String.valueOf(counter));
    }

    private void updateStepDisplay() {
        btnStep.setText("STEP: " + step);
    }

    private void updateStatus(String statusText) {
        tvStatus.setText(statusText);
    }

    private void addHistory(String event) {
        if (historyList.size() > 5) {
            historyList.remove(0);
        }
        historyList.add(event);
        
        StringBuilder builder = new StringBuilder();
        builder.append("Recent steps: ");
        for (int i = 0; i < historyList.size(); i++) {
            builder.append(historyList.get(i));
            if (i < historyList.size() - 1) {
                builder.append(" → ");
            }
        }
        tvHistory.setText(builder.toString());
    }

    private void savePreferences() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_COUNTER, counter);
        editor.putInt(KEY_STEP, step);
        editor.putString(KEY_NOTE, etNote.getText().toString());
        editor.apply();
    }

    private void loadPreferences() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        counter = prefs.getInt(KEY_COUNTER, 0);
        step = prefs.getInt(KEY_STEP, 1);
        String savedNote = prefs.getString(KEY_NOTE, "");
        etNote.setText(savedNote);
    }

    @Override
    protected void onPause() {
        super.onPause();
        savePreferences();
    }
}