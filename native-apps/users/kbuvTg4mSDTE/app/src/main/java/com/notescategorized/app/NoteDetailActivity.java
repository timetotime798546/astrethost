package com.notescategorized.app;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.List;
import java.util.UUID;

public class NoteDetailActivity extends android.app.Activity {

    private EditText titleEditText;
    private EditText contentEditText;
    private Spinner categorySpinner;
    private Button saveButton;
    private Button deleteButton;

    private NoteStorage noteStorage;
    private String currentNoteId = null; // null for new note, ID for existing

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_detail);

        noteStorage = new NoteStorage(this);

        titleEditText = (EditText) findViewById(R.id.detailTitleEditText);
        contentEditText = (EditText) findViewById(R.id.detailContentEditText);
        categorySpinner = (Spinner) findViewById(R.id.detailCategorySpinner);
        saveButton = (Button) findViewById(R.id.saveNoteButton);
        deleteButton = (Button) findViewById(R.id.deleteNoteButton);

        List<String> categories = noteStorage.loadCategories();
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, filterCategoriesForSpinner(categories));
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(categoryAdapter);

        if (getIntent().hasExtra("note_id")) {
            currentNoteId = getIntent().getStringExtra("note_id");
            loadNoteData(currentNoteId);
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

    private List<String> filterCategoriesForSpinner(List<String> allCategories) {
        List<String> filtered = new java.util.ArrayList<String>();
        for (String cat : allCategories) {
            if (!"All".equals(cat)) { // Don't show "All" in the category selection spinner
                filtered.add(cat);
            }
        }
        if (!filtered.contains("Uncategorized")) {
            filtered.add("Uncategorized"); // Ensure uncategorized is always an option
        }
        return filtered;
    }

    private void loadNoteData(String noteId) {
        Note note = noteStorage.getNoteById(noteId);
        if (note != null) {
            titleEditText.setText(note.getTitle());
            contentEditText.setText(note.getContent());
            ArrayAdapter<String> adapter = (ArrayAdapter<String>) categorySpinner.getAdapter();
            int spinnerPosition = adapter.getPosition(note.getCategory());
            if (spinnerPosition >= 0) {
                categorySpinner.setSelection(spinnerPosition);
            } else {
                // If category not found (e.g., deleted), default to Uncategorized
                categorySpinner.setSelection(adapter.getPosition("Uncategorized"));
            }
        } else {
            Toast.makeText(this, "Note not found.", Toast.LENGTH_SHORT).show();
            finish(); // Close activity if note not found
        }
    }

    private void saveNote() {
        String title = titleEditText.getText().toString().trim();
        String content = contentEditText.getText().toString().trim();
        String category = (String) categorySpinner.getSelectedItem();

        if (title.isEmpty()) {
            Toast.makeText(this, "Title cannot be empty.", Toast.LENGTH_SHORT).show();
            return;
        }

        Note note;
        if (currentNoteId == null) {
            // New note
            note = new Note(UUID.randomUUID().toString(), title, content, category);
        } else {
            // Existing note
            note = new Note(currentNoteId, title, content, category);
        }
        noteStorage.addOrUpdateNote(note);
        Toast.makeText(this, "Note saved!", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void confirmDeleteNote() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Note")
                .setMessage("Are you sure you want to delete this note?")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        noteStorage.deleteNote(currentNoteId);
                        Toast.makeText(NoteDetailActivity.this, "Note deleted", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}