package com.simplenotesandcounter.app;

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

    private static final String PREFS_NAME = "SimpleAppPrefs";
    private static final String KEY_COUNT = "counter_val";
    private static final String KEY_NOTE = "note_text";

    private int counter = 0;
    private TextView countText;
    private EditText noteEdit;
    private TextView savedNoteText;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize preferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        counter = sharedPreferences.getInt(KEY_COUNT, 0);
        String savedNote = sharedPreferences.getString(KEY_NOTE, "");

        // Initialize Views
        countText = (TextView) findViewById(R.id.countText);
        noteEdit = (EditText) findViewById(R.id.noteEdit);
        savedNoteText = (TextView) findViewById(R.id.savedNoteText);

        Button btnMinus = (Button) findViewById(R.id.btnMinus);
        Button btnReset = (Button) findViewById(R.id.btnReset);
        Button btnPlus = (Button) findViewById(R.id.btnPlus);
        Button btnSaveNote = (Button) findViewById(R.id.btnSaveNote);
        Button btnClearNote = (Button) findViewById(R.id.btnClearNote);

        // Set initial state
        updateCounterUI();
        updateNoteUI(savedNote);

        // Counter Action Listeners (Java 8 Anonymous Classes)
        btnPlus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                counter++;
                saveCounter();
                updateCounterUI();
            }
        });

        btnMinus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                counter--;
                saveCounter();
                updateCounterUI();
            }
        });

        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                counter = 0;
                saveCounter();
                updateCounterUI();
            }
        });

        // Note Action Listeners
        btnSaveNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String note = noteEdit.getText().toString().trim();
                if (!note.isEmpty()) {
                    saveNote(note);
                    updateNoteUI(note);
                    noteEdit.setText("");
                    Toast.makeText(MainActivity.this, "Note Saved!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Please type something first.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnClearNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveNote("");
                updateNoteUI("");
                Toast.makeText(MainActivity.this, "Note Cleared!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateCounterUI() {
        countText.setText(String.valueOf(counter));
    }

    private void updateNoteUI(String note) {
        if (note == null || note.isEmpty()) {
            savedNoteText.setText("No saved note yet.");
        } else {
            savedNoteText.setText(note);
        }
    }

    private void saveCounter() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(KEY_COUNT, counter);
        editor.apply();
    }

    private void saveNote(String note) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_NOTE, note);
        editor.apply();
    }
}