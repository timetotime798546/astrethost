package com.notescategoriesapp.app;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class NoteDetailActivity extends Activity {

    private EditText titleEditText;
    private EditText contentEditText;
    private EditText categoryEditText;
    private Button saveButton;
    private Button deleteButton;

    private NoteDatabaseHelper db;
    private long noteId = -1; // -1 indicates a new note

    public static final int RESULT_NOTE_DELETED = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_detail);

        titleEditText = (EditText) findViewById(R.id.edit_text_title);
        contentEditText = (EditText) findViewById(R.id.edit_text_content);
        categoryEditText = (EditText) findViewById(R.id.edit_text_category);
        saveButton = (Button) findViewById(R.id.button_save_note);
        deleteButton = (Button) findViewById(R.id.button_delete_note);

        db = new NoteDatabaseHelper(this);

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("note_id")) {
            noteId = intent.getLongExtra("note_id", -1);
            loadNoteData(noteId);
            deleteButton.setVisibility(View.VISIBLE); // Show delete button for existing notes
        } else {
            deleteButton.setVisibility(View.GONE); // Hide delete button for new notes
        }

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveNote();
            }
        });

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmDeleteNote();
            }
        });
    }

    private void loadNoteData(long id) {
        Note note = db.getNote(id);
        if (note != null) {
            titleEditText.setText(note.getTitle());
            contentEditText.setText(note.getContent());
            categoryEditText.setText(note.getCategory());
        }
    }

    private void saveNote() {
        String title = titleEditText.getText().toString().trim();
        String content = contentEditText.getText().toString().trim();
        String category = categoryEditText.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(this, "Title cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        if (category.isEmpty()) {
            category = "Uncategorized"; // Default category
        }

        Note note = new Note();
        note.setTitle(title);
        note.setContent(content);
        note.setCategory(category);

        if (noteId == -1) { // New note
            db.addNote(note);
        } else { // Existing note
            note.setId(noteId);
            db.updateNote(note);
        }

        setResult(RESULT_OK); // Indicate success to MainActivity
        finish(); // Close this activity
    }

    private void confirmDeleteNote() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Note");
        builder.setMessage("Are you sure you want to delete this note?");
        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                db.deleteNote(noteId);
                setResult(RESULT_NOTE_DELETED); // Custom result code for deletion
                finish();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }
}