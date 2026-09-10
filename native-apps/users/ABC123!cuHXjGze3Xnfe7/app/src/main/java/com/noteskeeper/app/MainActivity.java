package com.noteskeeper.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private DatabaseHelper dbHelper;
    private ListView listNotes;
    private TextView txtEmptyState;
    private EditText etSearch;
    private Spinner spinnerFilterCategory;
    private Button btnAddCategory;
    private Button btnAddNote;

    private List<Note> notesList;
    private NoteAdapter noteAdapter;
    private List<String> categoryFilterList;
    private ArrayAdapter<String> spinnerFilterAdapter;

    private String currentCategoryFilter = "All Categories";
    private String currentSearchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);

        // Bind Views
        listNotes = findViewById(R.id.list_notes);
        txtEmptyState = findViewById(R.id.txt_empty_state);
        etSearch = findViewById(R.id.et_search);
        spinnerFilterCategory = findViewById(R.id.spinner_filter_category);
        btnAddCategory = findViewById(R.id.btn_add_category);
        btnAddNote = findViewById(R.id.btn_add_note);

        notesList = new ArrayList<>();
        categoryFilterList = new ArrayList<>();

        // Setup notes adapter with custom interface implementations
        noteAdapter = new NoteAdapter(this, notesList, new NoteAdapter.OnNoteActionListener() {
            @Override
            public void onEdit(Note note) {
                showAddEditNoteDialog(note);
            }

            @Override
            public void onDelete(Note note) {
                showDeleteConfirmDialog(note);
            }
        });
        listNotes.setAdapter(noteAdapter);

        // Setup dynamic filter Spinner adapter
        spinnerFilterAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categoryFilterList);
        spinnerFilterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilterCategory.setAdapter(spinnerFilterAdapter);

        // Set up Listeners
        setupListeners();

        // Load metadata and lists
        loadCategories();
        refreshNotesList();
    }

    private void setupListeners() {
        // Real-time keyword filter watcher
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString();
                refreshNotesList();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Drop-down item selection listener
        spinnerFilterCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentCategoryFilter = categoryFilterList.get(position);
                refreshNotesList();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Create new category form trigger
        btnAddCategory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddCategoryDialog();
            }
        });

        // Create note action trigger
        btnAddNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddEditNoteDialog(null);
            }
        });
    }

    private void loadCategories() {
        List<String> list = dbHelper.getAllCategories();
        categoryFilterList.clear();
        categoryFilterList.add("All Categories");
        categoryFilterList.addAll(list);
        spinnerFilterAdapter.notifyDataSetChanged();
    }

    private void refreshNotesList() {
        List<Note> filtered = dbHelper.getFilteredNotes(currentCategoryFilter, currentSearchQuery);
        notesList.clear();
        notesList.addAll(filtered);
        noteAdapter.notifyDataSetChanged();

        if (notesList.isEmpty()) {
            txtEmptyState.setVisibility(View.VISIBLE);
        } else {
            txtEmptyState.setVisibility(View.GONE);
        }
    }

    private void showAddCategoryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_category, null);
        builder.setView(dialogView);

        final EditText etCategoryName = dialogView.findViewById(R.id.dialog_category_name);
        Button btnCancel = dialogView.findViewById(R.id.dialog_category_cancel);
        Button btnSave = dialogView.findViewById(R.id.dialog_category_save);

        final AlertDialog dialog = builder.create();

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = etCategoryName.getText().toString().trim();
                if (name.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Category name cannot be empty", Toast.LENGTH_SHORT).show();
                    return;
                }

                long result = dbHelper.addCategory(name);
                if (result == -1) {
                    Toast.makeText(MainActivity.this, "Category already exists", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Category added successfully", Toast.LENGTH_SHORT).show();
                    loadCategories();
                    dialog.dismiss();
                }
            }
        });

        dialog.show();
    }

    private void showAddEditNoteDialog(final Note existingNote) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_note, null);
        builder.setView(dialogView);

        TextView dialogTitle = dialogView.findViewById(R.id.dialog_note_title);
        final EditText etTitle = dialogView.findViewById(R.id.dialog_et_title);
        final EditText etContent = dialogView.findViewById(R.id.dialog_et_content);
        final Spinner spinnerCategory = dialogView.findViewById(R.id.dialog_spinner_category);
        Button btnCancel = dialogView.findViewById(R.id.dialog_btn_cancel);
        Button btnSave = dialogView.findViewById(R.id.dialog_btn_save);

        // Dynamically fetch and pre-populate options in notes category dropdown
        final List<String> noteCategories = dbHelper.getAllCategories();
        if (noteCategories.isEmpty()) {
            noteCategories.add("General");
            dbHelper.addCategory("General");
        }
        
        ArrayAdapter<String> noteCategoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, noteCategories);
        noteCategoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(noteCategoryAdapter);

        final boolean isEditMode = (existingNote != null);

        if (isEditMode) {
            dialogTitle.setText("Edit Note");
            etTitle.setText(existingNote.title);
            etContent.setText(existingNote.content);
            int index = noteCategories.indexOf(existingNote.category);
            if (index >= 0) {
                spinnerCategory.setSelection(index);
            }
        } else {
            dialogTitle.setText("Create Note");
        }

        final AlertDialog dialog = builder.create();

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String title = etTitle.getText().toString().trim();
                String content = etContent.getText().toString().trim();
                String selectedCategory = "General";
                
                if (spinnerCategory.getSelectedItem() != null) {
                    selectedCategory = spinnerCategory.getSelectedItem().toString();
                }

                if (title.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Title is required", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (isEditMode) {
                    dbHelper.updateNote(existingNote.id, title, content, selectedCategory);
                    Toast.makeText(MainActivity.this, "Note updated", Toast.LENGTH_SHORT).show();
                } else {
                    dbHelper.addNote(title, content, selectedCategory);
                    Toast.makeText(MainActivity.this, "Note saved", Toast.LENGTH_SHORT).show();
                }

                refreshNotesList();
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void showDeleteConfirmDialog(final Note note) {
        new AlertDialog.Builder(this)
            .setTitle("Delete Note")
            .setMessage("Are you sure you want to delete this note?")
            .setPositiveButton("DELETE", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dbHelper.deleteNote(note.id);
                    Toast.makeText(MainActivity.this, "Note deleted", Toast.LENGTH_SHORT).show();
                    refreshNotesList();
                }
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }
}