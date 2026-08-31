package com.omnitoolspremium.app;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class NoteEditActivity extends Activity {

    private EditText editNoteBody;
    private SharedPreferences sharedPrefs;
    private String noteKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_edit);

        editNoteBody = (EditText) findViewById(R.id.edit_note_body);
        Button btnSaveNote = (Button) findViewById(R.id.btn_save_note);
        Button btnDeleteNote = (Button) findViewById(R.id.btn_delete_note);

        sharedPrefs = getSharedPreferences("PremiumNotesPrefs", Context.MODE_PRIVATE);
        noteKey = getIntent().getStringExtra("NOTE_KEY");

        if (noteKey != null &amp;&amp; !noteKey.startsWith("NEW_")) {
            String existingContent = sharedPrefs.getString(noteKey, "");
            editNoteBody.setText(existingContent);
        }

        btnSaveNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveNote();
            }
        });

        btnDeleteNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteNote();
            }
        });
    }

    private void saveNote() {
        String content = editNoteBody.getText().toString().trim();
        if (content.isEmpty()) {
            Toast.makeText(this, "Note cannot be blank", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences.Editor editor = sharedPrefs.edit();
        if (noteKey == null || noteKey.startsWith("NEW_")) {
            noteKey = "Note_" + System.currentTimeMillis();
        }
        editor.putString(noteKey, content);
        editor.apply();

        Toast.makeText(this, "Note successfully stored", Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }

    private void deleteNote() {
        if (noteKey != null &amp;&amp; !noteKey.startsWith("NEW_")) {
            SharedPreferences.Editor editor = sharedPrefs.edit();
            editor.remove(noteKey);
            editor.apply();
            Toast.makeText(this, "Note deleted", Toast.LENGTH_SHORT).show();
        }
        setResult(RESULT_OK);
        finish();
    }
}