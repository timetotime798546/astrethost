package com.categorizednotes.app;

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

import java.util.ArrayList;
import java.util.List;

public class NoteEditorActivity extends Activity {

    private DatabaseHelper dbHelper;
    private long editingNoteId = -1; // -1 represents a new note flow

    private TextView editorHeaderTitle;
    private EditText editorTitle;
    private EditText editorContent;
    private Spinner editorCategorySpinner;
    private Button btnSave;
    private Button btnDelete;

    private List<String> categoriesList;
    private ArrayAdapter<String> categoryAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_editor);

        dbHelper = new DatabaseHelper(this);

        editorHeaderTitle = (TextView) findViewById(R.id.editor_header_title);
        editorTitle = (EditText) findViewById(R.id.editor_title);
        editorContent = (EditText) findViewById(R.id.editor_content);
        editorCategorySpinner = (Spinner) findViewById(R.id.editor_category_spinner);
        btnSave = (Button) findViewById(R.id.btn_editor_save);
        btnDelete = (Button) findViewById(R.id.btn_editor_delete);

        // Load Categories into spinner list
        categoriesList = new ArrayList<>();
        loadCategories();

        categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categoriesList);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        editorCategorySpinner.setAdapter(categoryAdapter);

        // Handle parameters from intent
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("NOTE_ID")) {
            editingNoteId = intent.getLongExtra("NOTE_ID", -1);
        }

        if (editingNoteId != -1) {
            // Edit Flow layout adjustments
            editorHeaderTitle.setText("Edit Note");
            btnDelete.setVisibility(View.VISIBLE);
            populateNoteData();
        } else {
            // Creation flow options configurations
            editorHeaderTitle.setText("Compose Note");
            btnDelete.setVisibility(View.GONE);
        }

        // Save note configuration action (Standard Java 8 Listener syntax)
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveNoteData();
            }
        });

        // Delete note action click listner (Standard Java 8 syntax)
        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmDeleteAction();
            }
        });
    }

    private void loadCategories() {
        categoriesList.clear();
        List<Category> allCats = dbHelper.getAllCategories();
        for (int i = 0; i < allCats.size(); i++) {
            categoriesList.add(allCats.get(i).getName());
        }
        // If categories table is somehow fully cleaned up, add a fallback standard
        if (categoriesList.isEmpty()) {
            categoriesList.add("General");
        }
    }

    private void populateNoteData() {
        Note note = dbHelper.getNoteById(editingNoteId);
        if (note != null) {
            editorTitle.setText(note.getTitle());
            editorContent.setText(note.getContent());

            // Select matching category inside layout selector
            int index = categoriesList.indexOf(note.getCategory());
            if (index >= 0) {
                editorCategorySpinner.setSelection(index);
            }
        } else {
            Toast.makeText(this, "Error fetching target note object", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void saveNoteData() {
        String titleStr = editorTitle.getText().toString().trim();
        String contentStr = editorContent.getText().toString().trim();
        String selectedCategory = (String) editorCategorySpinner.getSelectedItem();

        if (titleStr.isEmpty()) {
            Toast.makeText(this, "Title cannot be blank", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedCategory == null || selectedCategory.isEmpty()) {
            selectedCategory = "General";
        }

        boolean success;
        if (editingNoteId == -1) {
            // Execute Database insert operations
            success = dbHelper.insertNote(titleStr, contentStr, selectedCategory);
        } else {
            // Execute database update operations
            success = dbHelper.updateNote(editingNoteId, titleStr, contentStr, selectedCategory);
        }

        if (success) {
            Toast.makeText(this, "Note saved successfully!", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Error! Action failed during saving database task", Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmDeleteAction() {
        AlertDialog.Builder deleteDialog = new AlertDialog.Builder(this);
        deleteDialog.setTitle("Delete Note");
        deleteDialog.setMessage("Are you sure you want to delete this note?");

        deleteDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dbHelper.deleteNote(editingNoteId);
                Toast.makeText(NoteEditorActivity.this, "Note Deleted", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        deleteDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        deleteDialog.show();
    }
}