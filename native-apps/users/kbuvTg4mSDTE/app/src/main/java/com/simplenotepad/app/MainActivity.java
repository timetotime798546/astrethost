package com.simplenotepad.app;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final String PREFS_NAME = "SimpleNotePadPrefs";
    private static final String KEY_NOTE_TEXT = "saved_note";
    private static final String KEY_COUNTER_VAL = "counter_value";

    private EditText noteEditText;
    private Button btnSave;
    private Button btnClear;
    private TextView counterText;
    private Button btnTap;
    private Button btnResetCounter;

    private int count = 0;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Bind Views
        noteEditText = (EditText) findViewById(R.id.note_edit_text);
        btnSave = (Button) findViewById(R.id.btn_save);
        btnClear = (Button) findViewById(R.id.btn_clear);
        counterText = (TextView) findViewById(R.id.counter_text);
        btnTap = (Button) findViewById(R.id.btn_tap);
        btnResetCounter = (Button) findViewById(R.id.btn_reset_counter);

        // Load Persistent Data
        String savedNote = sharedPreferences.getString(KEY_NOTE_TEXT, "");
        noteEditText.setText(savedNote);

        count = sharedPreferences.getInt(KEY_COUNTER_VAL, 0);
        counterText.setText(String.valueOf(count));

        // Note save listener
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String input = noteEditText.getText().toString();
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(KEY_NOTE_TEXT, input);
                editor.apply();
                Toast.makeText(MainActivity.this, "Note saved successfully!", Toast.LENGTH_SHORT).show();
            }
        });

        // Note clear listener
        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                noteEditText.setText("");
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(KEY_NOTE_TEXT, "");
                editor.apply();
                Toast.makeText(MainActivity.this, "Note cleared!", Toast.LENGTH_SHORT).show();
            }
        });

        // Tap increment listener
        btnTap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                count++;
                counterText.setText(String.valueOf(count));
                saveCounterValue();
            }
        });

        // Reset counter listener
        btnResetCounter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                count = 0;
                counterText.setText(String.valueOf(count));
                saveCounterValue();
                Toast.makeText(MainActivity.this, "Counter reset!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveCounterValue() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(KEY_COUNTER_VAL, count);
        editor.apply();
    }
}