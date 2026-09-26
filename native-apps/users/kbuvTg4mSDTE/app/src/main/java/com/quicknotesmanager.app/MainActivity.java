package com.quicknotesmanager.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
    private NoteAdapter noteAdapter;
    private List<Note> notesList;

    // View declarations
    private LinearLayout layoutMainList;
    private LinearLayout layoutNoteEditor;

    private ListView notesListView;
    private TextView tvEmptyState;
    private EditText etSearch;
    private Spinner spinnerFilterCategory;

    // Editor field declarations
    private TextView tvEditorTitle;
    private EditText etNoteTitle;
    private EditText etNoteContent;
    private Spinner spinnerNoteCategory;
    private Button btnDeleteNote;
    private Button btnSaveNote;
    private Button btnEditorBack;

    private Button btnManageCategories;
    private Button btnNewNote;

    // State Variables
    private long selectedFilterCategoryId = -1; // -1 means "All Categories"
    private String currentSearchQuery = "";
    private long currentEditingNoteId = -1; // -1 means "New Note"

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);
        notesList = new ArrayList<>();

        initViews();
        setupListAndAdapters();
        setupListeners();
        
        populateCategorySpinners();
        loadNotes();
    }

    private void initViews() {
        layoutMainList = (LinearLayout) findViewById(R.id.layout_main_list);
        layoutNoteEditor = (LinearLayout) findViewById(R.id.layout_note_editor);

        notesListView = (ListView) findViewById(R.id.notes_list_view);
        tvEmptyState = (TextView) findViewById(R.id.tv_empty_state);
        etSearch = (EditText) findViewById(R.id.et_search);
        spinnerFilterCategory = (Spinner) findViewById(R.id.spinner_filter_category);

        tvEditorTitle = (TextView) findViewById(R.id.tv_editor_title);
        etNoteTitle = (EditText) findViewById(R.id.et_note_title);
        etNoteContent = (EditText) findViewById(R.id.et_note_content);
        spinnerNoteCategory = (Spinner) findViewById(R.id.spinner_note_category);
        btnDeleteNote = (Button) findViewById(R.id.btn_delete_note);
        btnSaveNote = (Button) findViewById(R.id.btn_save_note);
        btnEditorBack = (Button) findViewById(R.id.btn_editor_back);

        btnManageCategories = (Button) findViewById(R.id.btn_manage_categories);
        btnNewNote = (Button) findViewById(R.id.btn_new_note);
    }

    private void setupListAndAdapters() {
        noteAdapter = new NoteAdapter(this, notesList);
        notesListView.setAdapter(noteAdapter);
    }

    private void setupListeners() {
        // Search Filter text change listener
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString();
                loadNotes();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Category Filter selection listener
        spinnerFilterCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Category selectedCategory = (Category) parent.getItemAtPosition(position);
                if (selectedCategory != null) {
                    selectedFilterCategoryId = selectedCategory.getId();
                    loadNotes();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Add Note Button
        btnNewNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openEditorForNew();
            }
        });

        // Manage Categories Button
        btnManageCategories.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openCategoryManagerDialog();
            }
        });

        // Note Click Listener (to edit/delete)
        notesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Note selectedNote = (Note) parent.getItemAtPosition(position);
                if (selectedNote != null) {
                    openEditorForEdit(selectedNote);
                }
            }
        });

        // Save Note Button inside editor
        btnSaveNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveNote();
            }
        });

        // Delete Note Button inside editor
        btnDeleteNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteNote();
            }
        });

        // Editor Back Button
        btnEditorBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeEditor();
            }
        });
    }

    private void loadNotes() {
        notesList = dbHelper.searchNotes(currentSearchQuery, selectedFilterCategoryId);
        noteAdapter.updateData(notesList);

        if (notesList.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
        }
    }

    private void populateCategorySpinners() {
        List<Category> categories = dbHelper.getAllCategories();

        // 1. Filter Spinner
        List<Category> filterList = new ArrayList<>();
        filterList.add(new Category(-1, "All Categories"));
        filterList.add(new Category(0, "Uncategorized"));
        filterList.addAll(categories);

        ArrayAdapter<Category> filterAdapter = new ArrayAdapter<Category>(this,
                android.R.layout.simple_spinner_item, filterList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                view.setText(getItem(position).getName());
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getDropDownView(position, convertView, parent);
                view.setText(getItem(position).getName());
                return view;
            }
        };
        filterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilterCategory.setAdapter(filterAdapter);

        // Reset positions
        for (int i = 0; i < filterList.size(); i++) {
            if (filterList.get(i).getId() == selectedFilterCategoryId) {
                spinnerFilterCategory.setSelection(i);
                break;
            }
        }

        // 2. Editor Spinner
        List<Category> editorList = new ArrayList<>();
        editorList.add(new Category(0, "Uncategorized"));
        editorList.addAll(categories);

        ArrayAdapter<Category> editorAdapter = new ArrayAdapter<Category>(this,
                android.R.layout.simple_spinner_item, editorList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                view.setText(getItem(position).getName());
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getDropDownView(position, convertView, parent);
                view.setText(getItem(position).getName());
                return view;
            }
        };
        editorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerNoteCategory.setAdapter(editorAdapter);
    }

    private void openEditorForNew() {
        currentEditingNoteId = -1;
        tvEditorTitle.setText("New Note");
        etNoteTitle.setText("");
        etNoteContent.setText("");
        spinnerNoteCategory.setSelection(0);
        btnDeleteNote.setVisibility(View.GONE);

        layoutMainList.setVisibility(View.GONE);
        layoutNoteEditor.setVisibility(View.VISIBLE);
    }

    private void openEditorForEdit(Note note) {
        currentEditingNoteId = note.getId();
        tvEditorTitle.setText("Edit Note");
        etNoteTitle.setText(note.getTitle());
        etNoteContent.setText(note.getContent());
        btnDeleteNote.setVisibility(View.VISIBLE);

        ArrayAdapter<Category> adapter = (ArrayAdapter<Category>) spinnerNoteCategory.getAdapter();
        if (adapter != null) {
            for (int i = 0; i < adapter.getCount(); i++) {
                if (adapter.getItem(i).getId() == note.getCategoryId()) {
                    spinnerNoteCategory.setSelection(i);
                    break;
                }
            }
        }

        layoutMainList.setVisibility(View.GONE);
        layoutNoteEditor.setVisibility(View.VISIBLE);
    }

    private void closeEditor() {
        layoutNoteEditor.setVisibility(View.GONE);
        layoutMainList.setVisibility(View.VISIBLE);
        loadNotes();
    }

    private void saveNote() {
        String title = etNoteTitle.getText().toString().trim();
        String content = etNoteContent.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(this, "Please enter a note title", Toast.LENGTH_SHORT).show();
            return;
        }

        Category selectedCategory = (Category) spinnerNoteCategory.getSelectedItem();
        long categoryId = (selectedCategory != null) ? selectedCategory.getId() : 0;

        if (currentEditingNoteId == -1) {
            long id = dbHelper.insertNote(title, content, categoryId);
            if (id > -1) {
                Toast.makeText(this, "Note saved successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Error saving note", Toast.LENGTH_SHORT).show();
            }
        } else {
            int rows = dbHelper.updateNote(currentEditingNoteId, title, content, categoryId);
            if (rows > 0) {
                Toast.makeText(this, "Note updated successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Error updating note", Toast.LENGTH_SHORT).show();
            }
        }

        closeEditor();
    }

    private void deleteNote() {
        if (currentEditingNoteId != -1) {
            new AlertDialog.Builder(this)
                    .setTitle("Delete Note")
                    .setMessage("Are you sure you want to delete this note?")
                    .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dbHelper.deleteNote(currentEditingNoteId);
                            Toast.makeText(MainActivity.this, "Note deleted", Toast.LENGTH_SHORT).show();
                            closeEditor();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }
    }

    private void openCategoryManagerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_categories, null);
        builder.setView(dialogView);

        final EditText etNewCategory = (EditText) dialogView.findViewById(R.id.et_new_category_name);
        Button btnAdd = (Button) dialogView.findViewById(R.id.btn_add_category);
        final ListView lvCategories = (ListView) dialogView.findViewById(R.id.categories_list_view);

        final AlertDialog dialog = builder.create();

        final Runnable reloadDialogCategories = new Runnable() {
            @Override
            public void run() {
                final List<Category> categories = dbHelper.getAllCategories();
                ArrayAdapter<Category> adapter = new ArrayAdapter<Category>(MainActivity.this,
                        android.R.layout.simple_list_item_1, categories) {
                    @Override
                    public View getView(int position, View convertView, ViewGroup parent) {
                        TextView tv = (TextView) super.getView(position, convertView, parent);
                        tv.setText(getItem(position).getName());
                        tv.setPadding(24, 24, 24, 24);
                        tv.setTextSize(16);
                        return tv;
                    }
                };
                lvCategories.setAdapter(adapter);
            }
        };

        reloadDialogCategories.run();

        // Add Category
        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String catName = etNewCategory.getText().toString().trim();
                if (catName.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Category name cannot be empty", Toast.LENGTH_SHORT).show();
                    return;
                }
                long id = dbHelper.insertCategory(catName);
                if (id == -1) {
                    Toast.makeText(MainActivity.this, "Category already exists!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Category added", Toast.LENGTH_SHORT).show();
                    etNewCategory.setText("");
                    reloadDialogCategories.run();
                    populateCategorySpinners();
                    loadNotes();
                }
            }
        });

        // Delete Category
        lvCategories.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final Category category = (Category) parent.getItemAtPosition(position);
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Delete Category")
                        .setMessage("Are you sure you want to delete '" + category.getName() + "'? Notes in this category will become Uncategorized.")
                        .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface d, int which) {
                                dbHelper.deleteCategory(category.getId());
                                Toast.makeText(MainActivity.this, "Category deleted", Toast.LENGTH_SHORT).show();
                                reloadDialogCategories.run();
                                populateCategorySpinners();
                                loadNotes();
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });

        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "Close", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface d, int which) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }
}