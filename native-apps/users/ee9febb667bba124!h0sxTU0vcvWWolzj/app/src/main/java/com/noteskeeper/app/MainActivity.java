package com.noteskeeper.app;

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
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private DatabaseHelper dbHelper;
    private NotesAdapter adapter;
    private List<Note> currentNotesList;

    // Outer switcher containers
    private LinearLayout layoutMainList;
    private LinearLayout layoutEditNote;

    // Browser elements
    private EditText searchInput;
    private Spinner categoryFilterSpinner;
    private ListView notesListView;
    private Button btnAddNote;

    // Editor elements
    private TextView editorHeaderTitle;
    private Button btnDeleteNote;
    private EditText editTitleInput;
    private Spinner editCategorySpinner;
    private EditText editCategoryCustom;
    private EditText editContentInput;
    private Button btnCancelNote;
    private Button btnSaveNote;

    // Runtime variables
    private Note editingNote = null; 

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);
        currentNotesList = new ArrayList<Note>();

        initViews();
        setupListeners();
        loadNotesAndFilters();
    }

    private void initViews() {
        layoutMainList = (LinearLayout) findViewById(R.id.layout_main_list);
        layoutEditNote = (LinearLayout) findViewById(R.id.layout_edit_note);

        searchInput = (EditText) findViewById(R.id.search_input);
        categoryFilterSpinner = (Spinner) findViewById(R.id.category_filter_spinner);
        notesListView = (ListView) findViewById(R.id.notes_list_view);
        btnAddNote = (Button) findViewById(R.id.btn_add_note);

        editorHeaderTitle = (TextView) findViewById(R.id.editor_header_title);
        btnDeleteNote = (Button) findViewById(R.id.btn_delete_note);
        editTitleInput = (EditText) findViewById(R.id.edit_title_input);
        editCategorySpinner = (Spinner) findViewById(R.id.edit_category_spinner);
        editCategoryCustom = (EditText) findViewById(R.id.edit_category_custom);
        editContentInput = (EditText) findViewById(R.id.edit_content_input);
        btnCancelNote = (Button) findViewById(R.id.btn_cancel_note);
        btnSaveNote = (Button) findViewById(R.id.btn_save_note);

        adapter = new NotesAdapter();
        notesListView.setAdapter(adapter);
    }

    private void setupListeners() {
        // Query filter updates on text change
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterNotes();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Query filter updates on category spinner selections
        categoryFilterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filterNotes();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Handle item selection inside notes list view
        notesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Note selectedNote = currentNotesList.get(position);
                openEditor(selectedNote);
            }
        });

        // Setup triggers for creating new notes
        btnAddNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openEditor(null);
            }
        });

        // Trigger note persistence routine
        btnSaveNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveNote();
            }
        });

        // Dismiss editor sequence
        btnCancelNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeEditor();
            }
        });

        // Handle direct row removal
        btnDeleteNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmDeleteNote();
            }
        });
    }

    private void loadNotesAndFilters() {
        String query = searchInput.getText().toString();
        String selectedCategory = "All Categories";
        if (categoryFilterSpinner.getAdapter() != null && categoryFilterSpinner.getSelectedItem() != null) {
            selectedCategory = categoryFilterSpinner.getSelectedItem().toString();
        }

        currentNotesList = dbHelper.getAllNotes(query, selectedCategory);
        adapter.notifyDataSetChanged();

        List<String> categories = dbHelper.getUniqueCategories();
        ArrayAdapter<String> filterAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, categories);
        filterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        
        AdapterView.OnItemSelectedListener spinnerListener = categoryFilterSpinner.getOnItemSelectedListener();
        categoryFilterSpinner.setOnItemSelectedListener(null);
        
        int prevSelectedPos = 0;
        if (categoryFilterSpinner.getAdapter() != null && categoryFilterSpinner.getSelectedItem() != null) {
            String prevSelectedCat = categoryFilterSpinner.getSelectedItem().toString();
            prevSelectedPos = categories.indexOf(prevSelectedCat);
            if (prevSelectedPos < 0) prevSelectedPos = 0;
        }
        
        categoryFilterSpinner.setAdapter(filterAdapter);
        categoryFilterSpinner.setSelection(prevSelectedPos);
        categoryFilterSpinner.setOnItemSelectedListener(spinnerListener);
    }

    private void filterNotes() {
        String query = searchInput.getText().toString();
        String selectedCategory = "All Categories";
        if (categoryFilterSpinner.getSelectedItem() != null) {
            selectedCategory = categoryFilterSpinner.getSelectedItem().toString();
        }
        currentNotesList = dbHelper.getAllNotes(query, selectedCategory);
        adapter.notifyDataSetChanged();
    }

    private void openEditor(Note note) {
        editingNote = note;

        List<String> categories = new ArrayList<String>();
        categories.add("Personal");
        categories.add("Work");
        categories.add("Ideas");
        categories.add("Shopping");
        
        List<String> dbCategories = dbHelper.getUniqueCategories();
        for (String cat : dbCategories) {
            if (!cat.equals("All Categories") && !categories.contains(cat)) {
                categories.add(cat);
            }
        }

        ArrayAdapter<String> editorCategoryAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, categories);
        editorCategoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        editCategorySpinner.setAdapter(editorCategoryAdapter);

        if (note == null) {
            editorHeaderTitle.setText("Create Note");
            btnDeleteNote.setVisibility(View.GONE);
            editTitleInput.setText("");
            editContentInput.setText("");
            editCategoryCustom.setText("");
            editCategorySpinner.setSelection(0);
        } else {
            editorHeaderTitle.setText("Edit Note");
            btnDeleteNote.setVisibility(View.VISIBLE);
            editTitleInput.setText(note.getTitle());
            editContentInput.setText(note.getContent());
            editCategoryCustom.setText("");
            
            int catPos = categories.indexOf(note.getCategory());
            if (catPos >= 0) {
                editCategorySpinner.setSelection(catPos);
            } else {
                editCategoryCustom.setText(note.getCategory());
            }
        }

        layoutMainList.setVisibility(View.GONE);
        layoutEditNote.setVisibility(View.VISIBLE);
    }

    private void closeEditor() {
        layoutEditNote.setVisibility(View.GONE);
        layoutMainList.setVisibility(View.VISIBLE);
        editingNote = null;
        loadNotesAndFilters();
    }

    private void saveNote() {
        String title = editTitleInput.getText().toString().trim();
        String content = editContentInput.getText().toString().trim();
        
        if (title.isEmpty()) {
            Toast.makeText(this, "Please enter a note title", Toast.LENGTH_SHORT).show();
            return;
        }

        String category = "General";
        String customCat = editCategoryCustom.getText().toString().trim();
        if (!customCat.isEmpty()) {
            category = customCat;
        } else if (editCategorySpinner.getSelectedItem() != null) {
            category = editCategorySpinner.getSelectedItem().toString();
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        String currentDateTime = sdf.format(new Date());

        if (editingNote == null) {
            dbHelper.insertNote(title, content, category, currentDateTime);
            Toast.makeText(this, "Note saved successfully", Toast.LENGTH_SHORT).show();
        } else {
            dbHelper.updateNote(editingNote.getId(), title, content, category, currentDateTime);
            Toast.makeText(this, "Note updated successfully", Toast.LENGTH_SHORT).show();
        }

        closeEditor();
    }

    private void confirmDeleteNote() {
        if (editingNote == null) return;

        new AlertDialog.Builder(this)
                .setTitle("Delete Note")
                .setMessage("Are you sure you want to permanently delete this note?")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dbHelper.deleteNote(editingNote.getId());
                        Toast.makeText(MainActivity.this, "Note deleted successfully", Toast.LENGTH_SHORT).show();
                        closeEditor();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onBackPressed() {
        if (layoutEditNote.getVisibility() == View.VISIBLE) {
            closeEditor();
        } else {
            super.onBackPressed();
        }
    }

    private class NotesAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return currentNotesList.size();
        }

        @Override
        public Object getItem(int position) {
            return currentNotesList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return currentNotesList.get(position).getId();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(MainActivity.this).inflate(R.layout.note_item, parent, false);
            }

            Note note = currentNotesList.get(position);

            TextView titleTv = (TextView) convertView.findViewById(R.id.note_item_title);
            TextView categoryTv = (TextView) convertView.findViewById(R.id.note_item_category);
            TextView contentTv = (TextView) convertView.findViewById(R.id.note_item_content);
            TextView dateTv = (TextView) convertView.findViewById(R.id.note_item_date);

            titleTv.setText(note.getTitle());
            categoryTv.setText(note.getCategory());
            contentTv.setText(note.getContent());
            dateTv.setText(note.getTimestamp());

            return convertView;
        }
    }
}