package com.categorynotesapp.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends Activity {

    private static final String PREFS_NAME = "CategoryNotesPrefs";
    private static final String KEY_NOTES = "notes";
    private static final String[] CATEGORIES = {"All", "Personal", "Work", "Ideas", "Shopping", "Other"};

    private ListView notesListView;
    private EditText searchEditText;
    private Button addNoteButton;
    private List<Note> allNotes;
    private ArrayAdapter<Note> notesAdapter;
    private String currentFilterCategory = "All";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        notesListView = (ListView) findViewById(R.id.notesListView);
        searchEditText = (EditText) findViewById(R.id.searchEditText);
        addNoteButton = (Button) findViewById(R.id.addNoteButton);

        allNotes = new ArrayList<Note>();
        notesAdapter = new ArrayAdapter<Note>(this, android.R.layout.simple_list_item_1, allNotes);
        notesListView.setAdapter(notesAdapter);

        loadNotes();
        applyFilterAndSearch();

        addNoteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddEditNoteDialog(null); // Pass null for adding new note
            }
        });

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilterAndSearch();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        registerForContextMenu(notesListView);
    }

    private void loadNotes() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> noteSet = prefs.getStringSet(KEY_NOTES, new HashSet<String>());
        allNotes.clear();
        for (String noteJson : noteSet) {
            Note note = Note.fromJsonString(noteJson);
            if (note != null) {
                allNotes.add(note);
            }
        }
        sortNotes();
    }

    private void saveNotes() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        Set<String> noteSet = new HashSet<String>();
        for (Note note : allNotes) {
            noteSet.add(note.toJsonString());
        }
        editor.putStringSet(KEY_NOTES, noteSet);
        editor.apply();
        applyFilterAndSearch();
    }

    private void sortNotes() {
        Collections.sort(allNotes, new Comparator<Note>() {
            @Override
            public int compare(Note n1, Note n2) {
                return n1.getTitle().compareToIgnoreCase(n2.getTitle());
            }
        });
    }

    private void applyFilterAndSearch() {
        List<Note> filteredAndSearchedNotes = new ArrayList<Note>();
        String query = searchEditText.getText().toString().toLowerCase();

        for (Note note : allNotes) {
            boolean matchesCategory = (currentFilterCategory.equals("All") || note.getCategory().equals(currentFilterCategory));
            boolean matchesSearch = (query.isEmpty() || note.getTitle().toLowerCase().contains(query) || note.getContent().toLowerCase().contains(query));

            if (matchesCategory && matchesSearch) {
                filteredAndSearchedNotes.add(note);
            }
        }
        notesAdapter.clear();
        notesAdapter.addAll(filteredAndSearchedNotes);
        notesAdapter.notifyDataSetChanged();
    }

    private void showAddEditNoteDialog(final Note existingNote) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_edit_note, null);
        builder.setView(dialogView);

        final EditText titleEditText = (EditText) dialogView.findViewById(R.id.dialogTitleEditText);
        final EditText contentEditText = (EditText) dialogView.findViewById(R.id.dialogContentEditText);
        final Spinner categorySpinner = (Spinner) dialogView.findViewById(R.id.dialogCategorySpinner);

        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, CATEGORIES);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(categoryAdapter);

        if (existingNote != null) {
            builder.setTitle("Edit Note");
            titleEditText.setText(existingNote.getTitle());
            contentEditText.setText(existingNote.getContent());
            for (int i = 0; i < CATEGORIES.length; i++) {
                if (CATEGORIES[i].equals(existingNote.getCategory())) {
                    categorySpinner.setSelection(i);
                    break;
                }
            }
        } else {
            builder.setTitle("Add New Note");
            categorySpinner.setSelection(0); // Default to "All" or first category
        }

        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String title = titleEditText.getText().toString().trim();
                String content = contentEditText.getText().toString().trim();
                String category = categorySpinner.getSelectedItem().toString();

                if (title.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Note title cannot be empty", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (existingNote != null) {
                    // Edit existing note
                    existingNote.setTitle(title);
                    existingNote.setContent(content);
                    existingNote.setCategory(category);
                } else {
                    // Add new note
                    Note newNote = new Note(UUID.randomUUID().toString(), title, content, category);
                    allNotes.add(newNote);
                }
                sortNotes();
                saveNotes();
                Toast.makeText(MainActivity.this, "Note saved!", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        if (v.getId() == R.id.notesListView) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            menu.setHeaderTitle(allNotes.get(info.position).getTitle());
            menu.add(Menu.NONE, 0, 0, "Edit Note");
            menu.add(Menu.NONE, 1, 1, "Delete Note");
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        final Note selectedNote = (Note) notesListView.getItemAtPosition(info.position);

        switch (item.getItemId()) {
            case 0: // Edit
                showAddEditNoteDialog(selectedNote);
                return true;
            case 1: // Delete
                new AlertDialog.Builder(this)
                        .setTitle("Delete Note")
                        .setMessage("Are you sure you want to delete '" + selectedNote.getTitle() + "'?")
                        .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                allNotes.remove(selectedNote);
                                saveNotes();
                                Toast.makeText(MainActivity.this, "Note deleted", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, Menu.FIRST, Menu.NONE, "Filter by Category");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == Menu.FIRST) {
            showCategoryFilterDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showCategoryFilterDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Category Filter");
        builder.setItems(CATEGORIES, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                currentFilterCategory = CATEGORIES[which];
                applyFilterAndSearch();
                Toast.makeText(MainActivity.this, "Filtered by: " + currentFilterCategory, Toast.LENGTH_SHORT).show();
            }
        });
        builder.show();
    }
}