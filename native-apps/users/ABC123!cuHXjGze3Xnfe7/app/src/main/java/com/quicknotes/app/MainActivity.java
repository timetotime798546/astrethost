package com.quicknotes.app;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final String PREFS_NAME = "QuickNotesPrefs";
    private static final String KEY_SAVED_NOTE = "saved_note";

    private EditText noteInput;
    private Button btnSave;
    private TextView savedNoteText;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // UI Initialization
        noteInput = (EditText) findViewById(R.id.noteInput);
        btnSave = (Button) findViewById(R.id.btnSave);
        savedNoteText = (TextView) findViewById(R.id.savedNoteText);

        // SharedPreferences Setup
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Restore saved note if available
        loadSavedNote();

        // Save note on click (Java 8 compatibility compatible with anonymous class)
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveNote();
            }
        });
    }

    private void saveNote() {
        String note = noteInput.getText().toString().trim();

        if (TextUtils.isEmpty(note)) {
            Toast.makeText(MainActivity.this, R.string.error_empty, Toast.LENGTH_SHORT).show();
            return;
        }

        // Save data offline
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_SAVED_NOTE, note);
        editor.apply();

        // Update UI
        savedNoteText.setText(note);

        // Clear input layout fields
        noteInput.setText("");

        // Feedback notification
        Toast.makeText(MainActivity.this, R.string.success_saved, Toast.LENGTH_SHORT).show();
    }

    private void loadSavedNote() {
        String savedNote = sharedPreferences.getString(KEY_SAVED_NOTE, "");
        if (!TextUtils.isEmpty(savedNote)) {
            savedNoteText.setText(savedNote);
        } else {
            savedNoteText.setText(getString(R.string.no_saved_note));
        }
    }
}