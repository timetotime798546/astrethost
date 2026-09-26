package com.notescategorized.app;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextMenu;
import android.view.MenuItem;
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

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends android.app.Activity {

    private NoteStorage noteStorage;
    private ListView notesListView;
    private EditText searchEditText;
    private Spinner categorySpinner;
    private Button addNoteButton;
    private Button manageCategoriesButton;

    private List<Note> allNotes;
    private List<Note> filteredNotes;
    private ArrayAdapter<Note> notesAdapter;
    private ArrayAdapter<String> categoryAdapter;

    private String currentSearchQuery = "";
    private String currentCategoryFilter = "All";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        noteStorage = new NoteStorage(this);

        notesListView = (ListView) findViewById(R.id.notesListView);
        searchEditText = (EditText) findViewById(R.id.searchEditText);
        categorySpinner = (Spinner) findViewById(R.id.categorySpinner);
        addNoteButton = (Button) findViewById(R.id.addNoteButton);
        manageCategoriesButton = (Button) findViewById(R.id.manageCategoriesButton);

        registerForContextMenu(notesListView);

        addNoteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, NoteDetailActivity.class);
                startActivity(intent);
            }
        });

        manageCategoriesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, CategoryManageActivity.class);
                startActivity(intent);
            }
        });

        notesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Note selectedNote = filteredNotes.get(position);
                Intent intent = new Intent(MainActivity.this, NoteDetailActivity.class);
                intent.putExtra("note_id", selectedNote.getId());
                startActivity(intent);
            }
        });

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

        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentCategoryFilter = (String) parent.getItemAtPosition(position);
                applyFilters();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNotesAndCategories();
        applyFilters();
    }

    private void loadNotesAndCategories() {
        allNotes = noteStorage.loadNotes();
        List<String> categories = noteStorage.loadCategories();

        notesAdapter = new ArrayAdapter<Note>(this, R.layout.list_item_note, allNotes) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = getLayoutInflater().inflate(R.layout.list_item_note, parent, false);
                }
                TextView titleTextView = (TextView) convertView.findViewById(R.id.noteTitleTextView);
                TextView contentTextView = (TextView) convertView.findViewById(R.id.noteContentTextView);

                Note note = getItem(position);
                titleTextView.setText(note.getTitle());
                // Display first 100 characters of content or less if shorter
                String contentPreview = note.getContent();
                if (contentPreview.length() > 100) {
                    contentPreview = contentPreview.substring(0, 100) + "...";
                }
                contentTextView.setText(contentPreview);
                return convertView;
            }
        };
        notesListView.setAdapter(notesAdapter);

        categoryAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, categories);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(categoryAdapter);

        // Set spinner to currentCategoryFilter
        int spinnerPosition = categoryAdapter.getPosition(currentCategoryFilter);
        if (spinnerPosition >= 0) {
            categorySpinner.setSelection(spinnerPosition);
        } else {
            categorySpinner.setSelection(0); // Default to 'All'
            currentCategoryFilter = "All";
        }
    }

    private void applyFilters() {
        filteredNotes = new ArrayList<Note>();
        for (Note note : allNotes) {
            boolean matchesSearch = currentSearchQuery.isEmpty() ||
                                    note.getTitle().toLowerCase().contains(currentSearchQuery.toLowerCase()) ||
                                    note.getContent().toLowerCase().contains(currentSearchQuery.toLowerCase());

            boolean matchesCategory = currentCategoryFilter.equals("All") ||
                                      note.getCategory().equals(currentCategoryFilter);

            if (matchesSearch && matchesCategory) {
                filteredNotes.add(note);
            }
        }
        // Update adapter with filtered notes
        notesAdapter.clear();
        notesAdapter.addAll(filteredNotes);
        notesAdapter.notifyDataSetChanged();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        if (v.getId() == R.id.notesListView) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            menu.setHeaderTitle("Note Options");
            menu.add(0, 0, 0, "Edit");
            menu.add(0, 1, 1, "Delete");
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        final Note selectedNote = filteredNotes.get(info.position);

        if (item.getItemId() == 0) { // Edit
            Intent intent = new Intent(MainActivity.this, NoteDetailActivity.class);
            intent.putExtra("note_id", selectedNote.getId());
            startActivity(intent);
            return true;
        } else if (item.getItemId() == 1) { // Delete
            new AlertDialog.Builder(this)
                    .setTitle("Delete Note")
                    .setMessage("Are you sure you want to delete '" + selectedNote.getTitle() + "'?")
                    .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            noteStorage.deleteNote(selectedNote.getId());
                            Toast.makeText(MainActivity.this, "Note deleted", Toast.LENGTH_SHORT).show();
                            loadNotesAndCategories(); // Reload and re-filter notes
                            applyFilters();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return true;
        }
        return super.onContextItemSelected(item);
    }
}