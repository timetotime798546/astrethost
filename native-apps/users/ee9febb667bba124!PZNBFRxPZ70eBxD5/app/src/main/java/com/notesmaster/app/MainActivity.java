package com.notesmaster.app;

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
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private DatabaseHelper dbHelper;
    private List<DatabaseHelper.Note> allNotesList;
    private List<DatabaseHelper.Note> filteredNotesList;
    private NotesAdapter notesAdapter;

    private EditText searchEditText;
    private Spinner categoryFilterSpinner;
    private ListView notesListView;
    private TextView emptyStateTextView;
    private Button addNoteButton;

    private String[] filterCategories = {"All Categories", "Personal", "Work", "Ideas", "Important", "Others"};
    private String[] dialogCategories = {"Personal", "Work", "Ideas", "Important", "Others"};

    private String currentSearchQuery = "";
    private String currentFilterCategory = "All Categories";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);
        allNotesList = new ArrayList<DatabaseHelper.Note>();
        filteredNotesList = new ArrayList<DatabaseHelper.Note>();

        searchEditText = (EditText) findViewById(R.id.searchEditText);
        categoryFilterSpinner = (Spinner) findViewById(R.id.categoryFilterSpinner);
        notesListView = (ListView) findViewById(R.id.notesListView);
        emptyStateTextView = (TextView) findViewById(R.id.emptyStateTextView);
        addNoteButton = (Button) findViewById(R.id.addNoteButton);

        // Setup Filter Spinner
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, filterCategories);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categoryFilterSpinner.setAdapter(spinnerAdapter);

        // Setup ListView Adapter
        notesAdapter = new NotesAdapter();
        notesListView.setAdapter(notesAdapter);

        // Load data
        loadNotesFromDatabase();

        // Set listeners using standard anonymous implementations
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().trim();
                applyFilter();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        categoryFilterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentFilterCategory = filterCategories[position];
                applyFilter();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        notesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                DatabaseHelper.Note clickedNote = filteredNotesList.get(position);
                showNoteOptionsDialog(clickedNote);
            }
        });

        addNoteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddEditNoteDialog(null);
            }
        });
    }

    private void loadNotesFromDatabase() {
        allNotesList.clear();
        allNotesList.addAll(dbHelper.getAllNotes());
        applyFilter();
    }

    private void applyFilter() {
        filteredNotesList.clear();
        for (int i = 0; i < allNotesList.size(); i++) {
            DatabaseHelper.Note note = allNotesList.get(i);
            boolean matchesCategory = currentFilterCategory.equals("All Categories") || note.category.equalsIgnoreCase(currentFilterCategory);
            boolean matchesSearch = note.title.toLowerCase().contains(currentSearchQuery.toLowerCase()) || 
                                    note.content.toLowerCase().contains(currentSearchQuery.toLowerCase());

            if (matchesCategory && matchesSearch) {
                filteredNotesList.add(note);
            }
        }
        notesAdapter.notifyDataSetChanged();

        if (filteredNotesList.isEmpty()) {
            emptyStateTextView.setVisibility(View.VISIBLE);
        } else {
            emptyStateTextView.setVisibility(View.GONE);
        }
    }

    private void showNoteOptionsDialog(final DatabaseHelper.Note note) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(note.title);
        String[] options = {"Edit Note", "Delete Note"};
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    showAddEditNoteDialog(note);
                } else if (which == 1) {
                    showDeleteConfirmDialog(note);
                }
            }
        });
        builder.show();
    }

    private void showDeleteConfirmDialog(final DatabaseHelper.Note note) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Note");
        builder.setMessage("Are you sure you want to delete this note?");
        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dbHelper.deleteNote(note.id);
                Toast.makeText(MainActivity.this, "Note deleted", Toast.LENGTH_SHORT).show();
                loadNotesFromDatabase();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showAddEditNoteDialog(final DatabaseHelper.Note existingNote) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        
        android.widget.LinearLayout container = new android.widget.LinearLayout(this);
        container.setOrientation(android.widget.LinearLayout.VERTICAL);
        container.setPadding(36, 24, 36, 24);

        final EditText titleInput = new EditText(this);
        titleInput.setHint("Title");
        titleInput.setSingleLine(true);
        titleInput.setTextSize(16);
        container.addView(titleInput);

        final EditText contentInput = new EditText(this);
        contentInput.setHint("Write something...");
        contentInput.setLines(4);
        contentInput.setGravity(android.view.Gravity.TOP);
        contentInput.setTextSize(14);
        android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 20, 0, 20);
        contentInput.setLayoutParams(params);
        container.addView(contentInput);

        TextView categoryLabel = new TextView(this);
        categoryLabel.setText("Category:");
        categoryLabel.setTextSize(12);
        categoryLabel.setPadding(0, 10, 0, 4);
        container.addView(categoryLabel);

        final Spinner categorySelectSpinner = new Spinner(this);
        ArrayAdapter<String> dialogSpinnerAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, dialogCategories);
        dialogSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySelectSpinner.setAdapter(dialogSpinnerAdapter);
        container.addView(categorySelectSpinner);

        if (existingNote != null) {
            builder.setTitle("Edit Note");
            titleInput.setText(existingNote.title);
            contentInput.setText(existingNote.content);
            for (int i = 0; i < dialogCategories.length; i++) {
                if (dialogCategories[i].equalsIgnoreCase(existingNote.category)) {
                    categorySelectSpinner.setSelection(i);
                    break;
                }
            } 
        } else {
            builder.setTitle("Add Note");
        }

        builder.setView(container);

        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String title = titleInput.getText().toString().trim();
                String content = contentInput.getText().toString().trim();
                String category = categorySelectSpinner.getSelectedItem().toString();

                if (title.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Title cannot be empty", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (existingNote != null) {
                    dbHelper.updateNote(existingNote.id, title, content, category);
                    Toast.makeText(MainActivity.this, "Note updated", Toast.LENGTH_SHORT).show();
                } else {
                    dbHelper.insertNote(title, content, category);
                    Toast.makeText(MainActivity.this, "Note saved", Toast.LENGTH_SHORT).show();
                }
                loadNotesFromDatabase();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private class NotesAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return filteredNotesList.size();
        }

        @Override
        public Object getItem(int position) {
            return filteredNotesList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(MainActivity.this);
                convertView = inflater.inflate(R.layout.note_item, parent, false);
            }

            TextView titleText = (TextView) convertView.findViewById(R.id.noteTitle);
            TextView contentText = (TextView) convertView.findViewById(R.id.noteContent);
            TextView categoryText = (TextView) convertView.findViewById(R.id.noteCategory);
            TextView dateText = (TextView) convertView.findViewById(R.id.noteDate);

            DatabaseHelper.Note note = filteredNotesList.get(position);

            titleText.setText(note.title);
            contentText.setText(note.content);
            categoryText.setText(note.category);
            dateText.setText(note.timestamp);

            return convertView;
        }
    }
}
