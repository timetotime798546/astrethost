package com.generatedappname.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class NoteDetailActivity extends Activity {
    private EditText titleEdit;
    private EditText contentEdit;
    private Button saveButton;
    private NoteDbHelper dbHelper;
    private long noteId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_detail);

        titleEdit = findViewById(R.id.note_title);
        contentEdit = findViewById(R.id.note_content);
        saveButton = findViewById(R.id.save_note_button);
        dbHelper = new NoteDbHelper(this);

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("note_id")) {
            noteId = intent.getLongExtra("note_id", -1);
            if (noteId != -1) {
                Note note = dbHelper.getNoteById(noteId);
                if (note != null) {
                    titleEdit.setText(note.getTitle());
                    contentEdit.setText(note.getContent());
                }
            }
        }

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String title = titleEdit.getText().toString().trim();
                String content = contentEdit.getText().toString().trim();
                if (title.isEmpty()) {
                    title = "Untitled";
                }
                Note note = new Note();
                note.setTitle(title);
                note.setContent(content);
                if (noteId == -1) {
                    dbHelper.insertNote(note);
                } else {
                    note.setId(noteId);
                    dbHelper.updateNote(note);
                }
                finish();
            }
        });
    }
}
