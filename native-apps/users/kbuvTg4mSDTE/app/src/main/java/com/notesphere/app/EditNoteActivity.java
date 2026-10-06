package com.notesphere.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;
import java.util.UUID;

public class EditNoteActivity extends Activity {

    private EditText editTitle;
    private EditText editContent;
    private Spinner spinnerCategory;
    private Button btnDelete;
    private Button btnCancel;
    private Button btnSave;
    private TextView textScreenTitle;

    private String noteId = null;
    private List<Note> notes;
    private final String[] categories = {"General", "Personal", "Work", "Ideas", "Todo"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_note);

        editTitle = (EditText) findViewById(R.id.edit_note_title);
        editContent = (EditText) findViewById(R.id.edit_note_content);
        spinnerCategory = (Spinner) findViewById(R.id.spinner_category);
        btnDelete = (Button) findViewById(R.id.button_delete_note);
        btnCancel = (Button) findViewById(R.id.button_cancel_note);
        btnSave = (Button) findViewById(R.id.button_save_note);
        textScreenTitle = (TextView) findViewById(R.id.text_screen_title);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);

        notes = NoteStorage.loadNotes(this);

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("note_id")) {
            noteId = intent.getStringExtra("note_id");
            textScreenTitle.setText("Edit Note");
            btnDelete.setVisibility(View.VISIBLE);

            for (int i = 0; i < notes.size(); i++) {
                Note note = notes.get(i);
                if (note.getId().equals(noteId)) {
                    editTitle.setText(note.getTitle());
                    editContent.setText(note.getContent());
                    
                    for (int j = 0; j < categories.length; j++) {
                        if (categories[j].equalsIgnoreCase(note.getCategory())) {
                            spinnerCategory.setSelection(j);
                            break;
                        }
                    }
                    break;
                }
            }
        } else {
            textScreenTitle.setText("Create Note");
            btnDelete.setVisibility(View.GONE);
        }

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveNote();
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmDelete();
            }
        });
    }

    private void saveNote() {
        String title = editTitle.getText().toString().trim();
        String content = editContent.getText().toString().trim();
        String category = spinnerCategory.getSelectedItem().toString();

        if (title.isEmpty()) {
            Toast.makeText(this, "Please enter a note title", Toast.LENGTH_SHORT).show();
            return;
        }

        if (content.isEmpty()) {
            Toast.makeText(this, "Please write some note content", Toast.LENGTH_SHORT).show();
            return;
        }

        if (noteId == null) {
            String newId = UUID.randomUUID().toString();
            Note newNote = new Note(newId, title, content, category, System.currentTimeMillis());
            notes.add(0, newNote);
            Toast.makeText(this, "Note created successfully", Toast.LENGTH_SHORT).show();
        } else {
            for (int i = 0; i < notes.size(); i++) {
                Note note = notes.get(i);
                if (note.getId().equals(noteId)) {
                    note.setTitle(title);
                    note.setContent(content);
                    note.setCategory(category);
                    note.setTimestamp(System.currentTimeMillis());
                    break;
                }
            }
            Toast.makeText(this, "Note updated successfully", Toast.LENGTH_SHORT).show();
        }

        NoteStorage.saveNotes(this, notes);
        setResult(RESULT_OK);
        finish();
    }

    private void confirmDelete() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Note");
        builder.setMessage("Are you sure you want to delete this note?");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                deleteNote();
            }
        });
        builder.setNegativeButton("No", null);
        builder.show();
    }

    private void deleteNote() {
        if (noteId != null) {
            for (int i = 0; i < notes.size(); i++) {
                if (notes.get(i).getId().equals(noteId)) {
                    notes.remove(i);
                    break;
                }
            }
            NoteStorage.saveNotes(this, notes);
            Toast.makeText(this, "Note deleted", Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        }
    }
}