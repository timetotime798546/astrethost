package com.simpleapp.app;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final String PREFS_NAME = "SimpleAppPrefs";
    private static final String KEY_COUNTER = "counter_val";
    private static final String KEY_NOTE = "note_val";

    private int counter = 0;
    private TextView tvCounter;
    private EditText etNote;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) { 
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvCounter = (TextView) findViewById(R.id.tvCounter);
        etNote = (EditText) findViewById(R.id.etNote);
        Button btnIncrement = (Button) findViewById(R.id.btnIncrement);
        Button btnDecrement = (Button) findViewById(R.id.btnDecrement);
        Button btnReset = (Button) findViewById(R.id.btnReset);
        Button btnSaveNote = (Button) findViewById(R.id.btnSaveNote);
        Button btnClearNote = (Button) findViewById(R.id.btnClearNote);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Load saved state
        counter = prefs.getInt(KEY_COUNTER, 0);
        String savedNote = prefs.getString(KEY_NOTE, "");

        updateCounterDisplay();
        etNote.setText(savedNote);

        btnIncrement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                counter++;
                updateCounterDisplay();
                saveCounter();
            }
        });

        btnDecrement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                counter--;
                updateCounterDisplay();
                saveCounter();
            }
        });

        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                counter = 0;
                updateCounterDisplay();
                saveCounter();
                Toast.makeText(MainActivity.this, "Counter reset", Toast.LENGTH_SHORT).show();
            }
        });

        btnSaveNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String noteText = etNote.getText().toString();
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(KEY_NOTE, noteText);
                editor.apply();
                Toast.makeText(MainActivity.this, "Note saved successfully!", Toast.LENGTH_SHORT).show();
            }
        });

        btnClearNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etNote.setText("");
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(KEY_NOTE, "");
                editor.apply();
                Toast.makeText(MainActivity.this, "Note cleared", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateCounterDisplay() {
        tvCounter.setText(String.valueOf(counter));
    }

    private void saveCounter() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_COUNTER, counter);
        editor.apply();
    }
}