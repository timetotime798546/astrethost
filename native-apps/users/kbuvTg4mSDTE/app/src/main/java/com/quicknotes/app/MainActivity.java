package com.quicknotes.app;

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

public class MainActivity extends Activity implements NoteAdapter.OnNoteActionListener {

    private NotesDbHelper dbHelper;
    private List<Note> allNotesList;
    private List<Note> filteredNotesList;
    
    private ListView notesListView;
    private NoteAdapter adapter;
    private TextView emptyStateText;
    
    private EditText searchEditText;
    private Spinner categoryFilterSpinner;
    
    private String currentSearchQuery = "";
    private String currentCategoryFilter = "All";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new NotesDbHelper(this);
        allNotesList = new ArrayList<>();
        filteredNotesList = new ArrayList<>();

        notesListView = findViewById(R.id.notes_list_view);
        emptyStateText = findViewById(R.id.empty_state_text);
        searchEditText = findViewById(R.id.search_edit_text);
        categoryFilterSpinner = findViewById(R.id.category_filter_spinner);
        Button btnAddNote = findViewById(R.id.btn_add_note);

        // Setup dynamic Note List Adapter
        adapter = new NoteAdapter(this, filteredNotesList, this);
        notesListView.setAdapter(adapter);

        // Setup Floating Action/Add button
        btnAddNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showNoteDialog(null);
            }
        });

        // Setup search watcher
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString();
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Setup Category Filter Selector Spinner
        setupCategorySpinner();

        // Refresh/load data initially
        refreshData();
    }

    private void setupCategorySpinner() {
        final List<String> categories = dbHelper.getCategories();
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                categories
        );
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categoryFilterSpinner.setAdapter(spinnerAdapter);

        categoryFilterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentCategoryFilter = categories.get(position);
                applyFilters();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                currentCategoryFilter = "All";
                applyFilters();
            }
        });
    }

    private void refreshData() {
        allNotesList.clear();
        allNotesList.addAll(dbHelper.getAllNotes());
        applyFilters();
    }

    private void applyFilters() {
        filteredNotesList.clear();
        String query = currentSearchQuery.toLowerCase().trim();

        for (int i = 0; i < allNotesList.size(); i++) {
            Note note = allNotesList.get(i);
            
            // 1. Check Category
            boolean categoryMatches = currentCategoryFilter.equalsIgnoreCase("All") 
                    || note.getCategory().equalsIgnoreCase(currentCategoryFilter);

            // 2. Check Search query
            boolean queryMatches = query.isEmpty()
                    || note.getTitle().toLowerCase().contains(query)
                    || note.getContent().toLowerCase().contains(query);

            if (categoryMatches && queryMatches) {
                filteredNotesList.add(note);
            }
        }

        adapter.notifyDataSetChanged();

        if (filteredNotesList.isEmpty()) {
            emptyStateText.setVisibility(View.VISIBLE);
        } else {
            emptyStateText.setVisibility(View.GONE);
        }
    }

    private void showNoteDialog(final Note noteToEdit) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_note, null);
        builder.setView(dialogView);

        final AlertDialog dialog = builder.create();

        TextView titleView = dialogView.findViewById(R.id.dialog_title);
        final EditText titleInput = dialogView.findViewById(R.id.input_title);
        final EditText contentInput = dialogView.findViewById(R.id.input_content);
        final Spinner categorySpinner = dialogView.findViewById(R.id.input_category_spinner);
        final EditText customCategoryInput = dialogView.findViewById(R.id.input_custom_category);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        Button btnSave = dialogView.findViewById(R.id.btn_save);

        // Prepopulate default category values in the input dialog
        final List<String> dialogCategories = new ArrayList<>();
        dialogCategories.add("Personal");
        dialogCategories.add("Work");
        dialogCategories.add("Ideas");
        dialogCategories.add("Todo");
        dialogCategories.add("Custom...");

        ArrayAdapter<String> dialogCategoryAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                dialogCategories
        );
        dialogCategoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(dialogCategoryAdapter);

        // Listen for "Custom..." choice to show helper text input
        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (dialogCategories.get(position).equals("Custom...")) {
                    customCategoryInput.setVisibility(View.VISIBLE);
                } else {
                    customCategoryInput.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        final boolean isEditMode = (noteToEdit != null);
        if (isEditMode) {
            titleView.setText("Edit Note");
            titleInput.setText(noteToEdit.getTitle());
            contentInput.setText(noteToEdit.getContent());

            // Pre-select category spinner or configure custom category input if not standard
            String currentCat = noteToEdit.getCategory();
            int index = dialogCategories.indexOf(currentCat);
            if (index >= 0 && index < dialogCategories.size() - 1) {
                categorySpinner.setSelection(index);
                customCategoryInput.setVisibility(View.GONE);
            } else {
                // If it is custom, select the last item ("Custom...") and populate field
                categorySpinner.setSelection(dialogCategories.size() - 1);
                customCategoryInput.setVisibility(View.VISIBLE);
                customCategoryInput.setText(currentCat);
            }
        } else {
            titleView.setText("Add Note");
        }

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String title = titleInput.getText().toString().trim();
                String content = contentInput.getText().toString().trim();
                String selectedCategory = categorySpinner.getSelectedItem().toString();

                if (selectedCategory.equals("Custom...")) {
                    selectedCategory = customCategoryInput.getText().toString().trim();
                    if (selectedCategory.isEmpty()) {
                        selectedCategory = "General";
                    }
                }

                if (title.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Title cannot be empty", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (isEditMode) {
                    dbHelper.updateNote(noteToEdit.getId(), title, content, selectedCategory);
                    Toast.makeText(MainActivity.this, "Note updated", Toast.LENGTH_SHORT).show();
                } else {
                    dbHelper.insertNote(title, content, selectedCategory);
                    Toast.makeText(MainActivity.this, "Note saved", Toast.LENGTH_SHORT).show();
                }

                // Refresh application states, category list & search filters
                setupCategorySpinner();
                refreshData();
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    // Implementing NoteAdapter.OnNoteActionListener Interface methods
    @Override
    public void onEdit(Note note) {
        showNoteDialog(note);
    }

    @Override
    public void onDelete(final Note note) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Note");
        builder.setMessage("Are you sure you want to delete \"" + note.getTitle() + "\"?");
        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dbHelper.deleteNote(note.getId());
                Toast.makeText(MainActivity.this, "Note deleted", Toast.LENGTH_SHORT).show();
                setupCategorySpinner();
                refreshData();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
}