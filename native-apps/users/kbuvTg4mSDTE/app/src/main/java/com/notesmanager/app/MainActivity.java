package com.notesmanager.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
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
    private EditText etSearch;
    private Spinner spinnerFilter;
    private Button btnAddCategory;
    private Button btnAddNote;
    private ListView lvNotes;

    private List<Note> noteList;
    private NoteAdapter noteAdapter;
    private List<String> categories;
    private ArrayAdapter<String> filterSpinnerAdapter;

    private String currentCategoryFilter = "All";
    private String currentSearchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);
        noteList = new ArrayList<Note>();

        // Bind layouts
        etSearch = (EditText) findViewById(R.id.et_search);
        spinnerFilter = (Spinner) findViewById(R.id.spinner_filter);
        btnAddCategory = (Button) findViewById(R.id.btn_add_category);
        btnAddNote = (Button) findViewById(R.id.btn_add_note);
        lvNotes = (ListView) findViewById(R.id.lv_notes);

        setupFilters();
        setupNoteList();
        setupListeners();
        refreshNotes();
    }

    private void setupFilters() {
        categories = new ArrayList<String>();
        categories.add("All");
        categories.addAll(dbHelper.getAllCategories());

        filterSpinnerAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, categories);
        filterSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilter.setAdapter(filterSpinnerAdapter);
    }

    private void setupNoteList() {
        noteAdapter = new NoteAdapter(this, noteList);
        lvNotes.setAdapter(noteAdapter);
    }

    private void setupListeners() {
        // Search field text listener
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString();
                refreshNotes();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Spinner item selection listener
        spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentCategoryFilter = categories.get(position);
                refreshNotes();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Add category button click listener
        btnAddCategory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddCategoryDialog();
            }
        });

        // Add note button click listener
        btnAddNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddEditNoteDialog(null);
            }
        });
    }

    private void refreshNotes() {
        noteList.clear();
        noteList.addAll(dbHelper.getNotes(currentSearchQuery, currentCategoryFilter));
        noteAdapter.notifyDataSetChanged();
    }

    private void showAddCategoryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_category, null);
        builder.setView(dialogView);

        final EditText etCatName = (EditText) dialogView.findViewById(R.id.et_dialog_cat_name);

        builder.setPositiveButton("Add", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String catName = etCatName.getText().toString().trim();
                if (catName.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Category name cannot be empty", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (catName.equalsIgnoreCase("All")) {
                    Toast.makeText(MainActivity.this, "Reserved category name", Toast.LENGTH_SHORT).show();
                    return;
                }
                boolean success = dbHelper.addCategory(catName);
                if (success) {
                    Toast.makeText(MainActivity.this, "Category added", Toast.LENGTH_SHORT).show();
                    setupFilters(); // Refresh spinner options
                } else {
                    Toast.makeText(MainActivity.this, "Category already exists", Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.create().show();
    }

    private void showAddEditNoteDialog(final Note noteToEdit) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_note, null);
        builder.setView(dialogView);

        TextView dialogTitleView = (TextView) dialogView.findViewById(R.id.dialog_note_title);
        final EditText etTitle = (EditText) dialogView.findViewById(R.id.et_dialog_title);
        final EditText etContent = (EditText) dialogView.findViewById(R.id.et_dialog_content);
        final Spinner spinnerCategory = (Spinner) dialogView.findViewById(R.id.spinner_dialog_category);

        // Populate categories list for dialog spinner (exclude "All")
        final List<String> dialogCategories = dbHelper.getAllCategories();
        if (dialogCategories.isEmpty()) {
            dialogCategories.add("General");
            dbHelper.addCategory("General");
            setupFilters();
        }
        ArrayAdapter<String> dialogSpinAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, dialogCategories);
        dialogSpinAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(dialogSpinAdapter);

        final boolean isEdit = (noteToEdit != null);
        if (isEdit) {
            dialogTitleView.setText("Edit Note");
            etTitle.setText(noteToEdit.getTitle());
            etContent.setText(noteToEdit.getContent());
            int index = dialogCategories.indexOf(noteToEdit.getCategory());
            if (index >= 0) {
                spinnerCategory.setSelection(index);
            }
        } else {
            dialogTitleView.setText("Create Note");
        }

        builder.setPositiveButton(isEdit ? "Update" : "Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String title = etTitle.getText().toString().trim();
                String content = etContent.getText().toString().trim();
                if (title.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Title is required", Toast.LENGTH_SHORT).show();
                    return;
                }

                String category = "";
                if (spinnerCategory.getSelectedItem() != null) {
                    category = spinnerCategory.getSelectedItem().toString();
                }

                String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());

                if (isEdit) {
                    dbHelper.updateNote(noteToEdit.getId(), title, content, category, timestamp);
                    Toast.makeText(MainActivity.this, "Note updated", Toast.LENGTH_SHORT).show();
                } else {
                    dbHelper.addNote(title, content, category, timestamp);
                    Toast.makeText(MainActivity.this, "Note saved", Toast.LENGTH_SHORT).show();
                }

                refreshNotes();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.create().show();
    }

    private class NoteAdapter extends ArrayAdapter<Note> {
        private Context context;
        private List<Note> list;

        public NoteAdapter(Context context, List<Note> list) {
            super(context, R.layout.note_item, list);
            this.context = context;
            this.list = list;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(R.layout.note_item, null);
            }

            final Note note = list.get(position);

            TextView tvTitle = (TextView) view.findViewById(R.id.note_title);
            TextView tvCategory = (TextView) view.findViewById(R.id.note_category);
            TextView tvContent = (TextView) view.findViewById(R.id.note_content);
            TextView tvDate = (TextView) view.findViewById(R.id.note_date);
            Button btnEdit = (Button) view.findViewById(R.id.btn_note_edit);
            Button btnDelete = (Button) view.findViewById(R.id.btn_note_delete);

            tvTitle.setText(note.getTitle());
            tvCategory.setText(note.getCategory());
            tvContent.setText(note.getContent());
            tvDate.setText(note.getTimestamp());

            btnEdit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showAddEditNoteDialog(note);
                }
            });

            btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new AlertDialog.Builder(context)
                            .setTitle("Delete Note")
                            .setMessage("Are you sure you want to delete this note?")
                            .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dbHelper.deleteNote(note.getId());
                                    Toast.makeText(context, "Note deleted", Toast.LENGTH_SHORT).show();
                                    refreshNotes();
                                }
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                }
            });

            return view;
        }
    }
}