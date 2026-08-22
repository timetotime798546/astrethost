package com.multitoolsuite.app;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class NotesActivity extends Activity {
    private EditText etNote;
    private static final String PREFS_NAME = "MultiToolNotes";
    private static final String NOTE_KEY = "saved_note";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes);

        etNote = (EditText) findViewById(R.id.et_note);
        Button btnSave = (Button) findViewById(R.id.btn_save);
        Button btnClear = (Button) findViewById(R.id.btn_clear);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String savedNote = prefs.getString(NOTE_KEY, "");
        etNote.setText(savedNote);

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String noteText = etNote.getText().toString();
                SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
                editor.putString(NOTE_KEY, noteText);
                editor.apply();
                Toast.makeText(NotesActivity.this, "Note saved successfully!", Toast.LENGTH_SHORT).show();
            }
        });

        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etNote.setText("");
                SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
                editor.putString(NOTE_KEY, "");
                editor.apply();
                Toast.makeText(NotesActivity.this, "Cleared note!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
