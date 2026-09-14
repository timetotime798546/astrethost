package com.smartnotes.app;

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
import android.widget.BaseAdapter;
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
    private ListView notesListView;
    private TextView emptyText;
    private EditText searchEditText;
    private Spinner categoryFilterSpinner;
    private Button btnAddNote;
    private Button btnAddCategory;

    private List<Note> currentNotesList = new ArrayList<>();
    private NoteAdapter noteAdapter;
    
    private String selectedFilterCategory = "All";
    private String currentSearchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);

        notesListView = (ListView) findViewById(R.id.notes_list_view);
        emptyText = (TextView) findViewById(R.id.empty_text);
        searchEditText = (EditText) findViewById(R.id.search_edit_text);
        categoryFilterSpinner = (Spinner) findViewById(R.id.category_filter_spinner);
        btnAddNote = (Button) findViewById(R.id.btn_add_note);
        btnAddCategory = (Button) findViewById(R.id.btn_add_category);

        noteAdapter = new NoteAdapter(this, currentNotesList);
        notesListView.setAdapter(noteAdapter);

        // Load Filters and Notes
        setupFilters();
        refreshNotesList();

        // Search text watcher
        searchEditText.addTextChangedListener(new TextWatcher() {
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

        // Setup listeners
        btnAddNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showNoteDialog(null);
            }
        });

        btnAddCategory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCategoryDialog();
            }
        });
    }

    private void setupFilters() {
        final List<String> categories = dbHelper.getCategories();
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categoryFilterSpinner.setAdapter(spinnerAdapter);

        categoryFilterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedFilterCategory = categories.get(position);
                refreshNotesList();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void refreshNotesList() {
        currentNotesList.clear();
        List<Note> loaded = dbHelper.getAllNotes(selectedFilterCategory, currentSearchQuery);
        currentNotesList.addAll(loaded);
        noteAdapter.notifyDataSetChanged();

        if (currentNotesList.isEmpty()) {
            emptyText.setVisibility(View.VISIBLE);
        } else {
            emptyText.setVisibility(View.GONE);
        }
    }

    private void showCategoryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_category, null);
        builder.setView(dialogView);

        final EditText editCategoryName = (EditText) dialogView.findViewById(R.id.edit_category_name);

        builder.setPositiveButton("Create", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String name = editCategoryName.getText().toString().trim();
                if (!name.isEmpty()) {
                    dbHelper.addCategory(name);
                    setupFilters();
                    Toast.makeText(MainActivity.this, "Category created", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.create().show();
    }

    private void showNoteDialog(final Note noteToEdit) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_note, null);
        builder.setView(dialogView);

        TextView dialogTitle = (TextView) dialogView.findViewById(R.id.dialog_title);
        final EditText editNoteTitle = (EditText) dialogView.findViewById(R.id.edit_note_title);
        final EditText editNoteContent = (EditText) dialogView.findViewById(R.id.edit_note_content);
        final Spinner dialogCategorySpinner = (Spinner) dialogView.findViewById(R.id.dialog_note_category_spinner);

        // Populate Category Spinner inside dialog, excluding 'All'
        List<String> rawCategories = dbHelper.getCategories();
        final List<String> dialogCategories = new ArrayList<>();
        for (String cat : rawCategories) {
            if (!cat.equals("All")) {
                dialogCategories.add(cat);
            }
        }
        if (dialogCategories.isEmpty()) {
            dialogCategories.add("General");
        }

        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, dialogCategories);
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dialogCategorySpinner.setAdapter(catAdapter);

        if (noteToEdit != null) {
            dialogTitle.setText("Edit Note");
            editNoteTitle.setText(noteToEdit.getTitle());
            editNoteContent.setText(noteToEdit.getContent());
            int index = dialogCategories.indexOf(noteToEdit.getCategory());
            if (index >= 0) {
                dialogCategorySpinner.setSelection(index);
            }
        } else {
            dialogTitle.setText("Add Note");
        }

        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String title = editNoteTitle.getText().toString().trim();
                String content = editNoteContent.getText().toString().trim();
                String category = dialogCategorySpinner.getSelectedItem().toString();

                if (title.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Title is required", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (noteToEdit != null) {
                    dbHelper.updateNote(noteToEdit.getId(), title, content, category, System.currentTimeMillis());
                    Toast.makeText(MainActivity.this, "Note updated", Toast.LENGTH_SHORT).show();
                } else {
                    dbHelper.insertNote(title, content, category, System.currentTimeMillis());
                    Toast.makeText(MainActivity.this, "Note saved", Toast.LENGTH_SHORT).show();
                }
                refreshNotesList();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.create().show();
    }

    private class NoteAdapter extends BaseAdapter {
        private Context context;
        private List<Note> notes;
        private SimpleDateFormat sdf;

        public NoteAdapter(Context context, List<Note> notes) {
            this.context = context;
            this.notes = notes;
            this.sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
        }

        @Override
        public int getCount() {
            return notes.size();
        }

        @Override
        public Object getItem(int position) {
            return notes.get(position);
        }

        @Override
        public long getItemId(int position) {
            return notes.get(position).getId();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.note_list_item, parent, false);
            }

            final Note note = notes.get(position);

            TextView noteTitle = (TextView) convertView.findViewById(R.id.note_title);
            TextView noteContent = (TextView) convertView.findViewById(R.id.note_content);
            TextView noteCategory = (TextView) convertView.findViewById(R.id.note_category);
            TextView noteDate = (TextView) convertView.findViewById(R.id.note_date);
            Button btnEdit = (Button) convertView.findViewById(R.id.btn_edit_note);
            Button btnDelete = (Button) convertView.findViewById(R.id.btn_delete_note);

            noteTitle.setText(note.getTitle());
            noteContent.setText(note.getContent());
            noteCategory.setText(note.getCategory());
            noteDate.setText(sdf.format(new Date(note.getTimestamp())));

            btnEdit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showNoteDialog(note);
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
                                refreshNotesList();
                                Toast.makeText(context, "Note deleted", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                }
            });

            return convertView;
        }
    }
}