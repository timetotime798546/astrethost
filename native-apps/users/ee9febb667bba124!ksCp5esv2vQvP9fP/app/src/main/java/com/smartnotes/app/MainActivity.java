package com.smartnotes.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private DatabaseHelper dbHelper;
    private NoteAdapter adapter;
    private List<Note> notesList;

    private LinearLayout layoutMainView;
    private LinearLayout layoutEditView;

    private Button btnAddNewNote;
    private EditText etSearchQuery;
    private Spinner spinnerFilterCategory;
    private ListView listViewNotes;
    private TextView tvEmptyState;

    private TextView tvEditorTitle;
    private Spinner spinnerEditCategory;
    private EditText etNoteTitle;
    private EditText etNoteContent;
    private Button btnCancelEdit;
    private Button btnSaveNote;

    private boolean isEditingMode = false;
    private long currentEditingNoteId = -1; 

    private final String[] filterCategories = {"All", "Personal", "Work", "Ideas", "Important"};
    private final String[] editCategories = {"Personal", "Work", "Ideas", "Important"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);
        notesList = new ArrayList<>();

        initializeViews();
        setupSpinners();
        setupListeners();
        
        loadAndFilterNotes();
    }

    private void initializeViews() {
        layoutMainView = (LinearLayout) findViewById(R.id.layoutMainView);
        layoutEditView = (LinearLayout) findViewById(R.id.layoutEditView);

        btnAddNewNote = (Button) findViewById(R.id.btnAddNewNote);
        etSearchQuery = (EditText) findViewById(R.id.etSearchQuery);
        spinnerFilterCategory = (Spinner) findViewById(R.id.spinnerFilterCategory);
        listViewNotes = (ListView) findViewById(R.id.listViewNotes);
        tvEmptyState = (TextView) findViewById(R.id.tvEmptyState);

        tvEditorTitle = (TextView) findViewById(R.id.tvEditorTitle);
        spinnerEditCategory = (Spinner) findViewById(R.id.spinnerEditCategory);
        etNoteTitle = (EditText) findViewById(R.id.etNoteTitle);
        etNoteContent = (EditText) findViewById(R.id.etNoteContent);
        btnCancelEdit = (Button) findViewById(R.id.btnCancelEdit);
        btnSaveNote = (Button) findViewById(R.id.btnSaveNote);
    }

    private void setupSpinners() {
        ArrayAdapter<String> filterAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, filterCategories);
        filterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilterCategory.setAdapter(filterAdapter);

        ArrayAdapter<String> editAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, editCategories);
        editAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerEditCategory.setAdapter(editAdapter);
    }

    private void setupListeners() {
        adapter = new NoteAdapter(this, notesList, new NoteAdapter.OnNoteActionListener() {
            @Override
            public void onEdit(Note note) {
                openEditor(note);
            }

            @Override
            public void onDelete(final Note note) {
                confirmDeleteNote(note);
            }
        });
        listViewNotes.setAdapter(adapter);

        btnAddNewNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openEditor(null);
            }
        });

        btnCancelEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeEditor();
            }
        });

        btnSaveNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveNote();
            }
        });

        etSearchQuery.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                loadAndFilterNotes();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        spinnerFilterCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadAndFilterNotes();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadAndFilterNotes() {
        String searchQuery = etSearchQuery.getText().toString();
        String selectedCategory = spinnerFilterCategory.getSelectedItem().toString();

        List<Note> filtered = dbHelper.getFilteredNotes(searchQuery, selectedCategory);
        notesList.clear();
        notesList.addAll(filtered);
        adapter.updateList(notesList);

        if (notesList.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            listViewNotes.setVisibility(View.GONE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            listViewNotes.setVisibility(View.VISIBLE);
        }
    }

    private void openEditor(Note note) {
        isEditingMode = true;
        layoutMainView.setVisibility(View.GONE);
        layoutEditView.setVisibility(View.VISIBLE);

        if (note == null) {
            tvEditorTitle.setText("Create Note");
            currentEditingNoteId = -1;
            etNoteTitle.setText("");
            etNoteContent.setText("");
            spinnerEditCategory.setSelection(0);
        } else {
            tvEditorTitle.setText("Edit Note");
            currentEditingNoteId = note.id;
            etNoteTitle.setText(note.title);
            etNoteContent.setText(note.content);

            int index = 0;
            for (int i = 0; i < editCategories.length; i++) {
                if (editCategories[i].equals(note.category)) {
                    index = i;
                    break;
                }
            }
            spinnerEditCategory.setSelection(index);
        }
        etNoteTitle.requestFocus();
    }

    private void closeEditor() {
        isEditingMode = false;
        layoutMainView.setVisibility(View.VISIBLE);
        layoutEditView.setVisibility(View.GONE);

        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null && getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }

        loadAndFilterNotes();
    }

    private void saveNote() {
        String title = etNoteTitle.getText().toString().trim();
        String content = etNoteContent.getText().toString().trim();
        String category = spinnerEditCategory.getSelectedItem().toString();

        if (title.isEmpty()) {
            Toast.makeText(this, "Title cannot be empty!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentEditingNoteId == -1) {
            long id = dbHelper.insertNote(title, content, category);
            if (id != -1) {
                Toast.makeText(this, "Note saved!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to save note", Toast.LENGTH_SHORT).show();
            }
        } else {
            int rowsAffected = dbHelper.updateNote(currentEditingNoteId, title, content, category);
            if (rowsAffected > 0) {
                Toast.makeText(this, "Note updated!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to update note", Toast.LENGTH_SHORT).show();
            }
        }

        closeEditor();
    }

    private void confirmDeleteNote(final Note note) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Note");
        builder.setMessage("Are you sure you want to delete '" + note.title + "'?");
        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dbHelper.deleteNote(note.id);
                Toast.makeText(MainActivity.this, "Note deleted", Toast.LENGTH_SHORT).show();
                loadAndFilterNotes();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    @Override
    public void onBackPressed() {
        if (isEditingMode) {
            closeEditor();
        } else {
            super.onBackPressed();
        }
    }
}