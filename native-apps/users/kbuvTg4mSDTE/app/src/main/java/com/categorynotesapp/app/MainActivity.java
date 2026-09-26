package com.categorynotesapp.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
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
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends Activity {

    private static final String PREFS_NAME = "CategoryNotesPrefs";
    private static final String KEY_NOTES = "notes";
    private static final String[] CATEGORIES = {"All", "Personal", "Work", "Ideas", "Shopping", "Other"};

    private ListView notesListView;
    private EditText searchEditText;
    private Button addNoteButton;
    private ProgressBar loadingProgressBar;
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
        loadingProgressBar = (ProgressBar) findViewById(R.id.loadingProgressBar);

        allNotes = new ArrayList<Note>();
        notesAdapter = new ArrayAdapter<Note>(this, android.R.layout.simple_list_item_1, allNotes);
        notesListView.setAdapter(notesAdapter);

        // Load notes asynchronously with a loader
        new LoadNotesTask().execute();

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

    // AsyncTask for loading notes in the background
    private class LoadNotesTask extends AsyncTask<Void, Void, List<Note>> {
        @Override
        protected void onPreExecute() {
            notesListView.setVisibility(View.GONE);
            loadingProgressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected List<Note> doInBackground(Void... voids) {
            // Simulate a delay for loader visibility, remove in production if not needed
            try {
                Thread.sleep(1000); // 1 second delay
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            Set<String> noteSet = prefs.getStringSet(KEY_NOTES, new HashSet<String>());
            List<Note> loadedNotes = new ArrayList<Note>();
            for (String noteJson : noteSet) {
                Note note = Note.fromJsonString(noteJson);
                if (note != null) {
                    loadedNotes.add(note);
                }
            }
            // Sort loaded notes before returning
            Collections.sort(loadedNotes, new Comparator<Note>() {
                @Override
                public int compare(Note n1, Note n2) {
                    return n1.getTitle().compareToIgnoreCase(n2.getTitle());
                }
            });
            return loadedNotes;
        }

        @Override
        protected void onPostExecute(List<Note> result) {
            allNotes.clear();
            allNotes.addAll(result);
            applyFilterAndSearch(); // This will populate the adapter and notify it
            loadingProgressBar.setVisibility(View.GONE);
            notesListView.setVisibility(View.VISIBLE);
        }
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
        // This method is now called within LoadNotesTask.doInBackground and before saving
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
                sortNotes(); // Ensure notes are sorted after adding/editing
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
[2026-09-26 04:58:45] [INFO] Gemini 2.5 completed directly. Calling process-from-log.php
{
    "user_id": "kbuvTg4mSDTE",
    "interaction_id": "v1_ChdSSW0zYW9PdUZJNk4zYm9QNlpTZy1RcxIXUkltM2FvT3VGSTZOM2JvUDZaU2ctUXM",
    "serial": "6"
}
[2026-09-26 04:58:46] [INFO] process-from-log.php called
{
    "user_id": "kbuvTg4mSDTE",
    "http_code": 200,
    "error": ""
}
[2026-09-26 04:58:46] [INFO] process-from-log.php completed
{
    "success": true,
    "http_code": 200,
    "response": "Found Interaction ID: v1_ChdSSW0zYW9PdUZJNk4zYm9QNlpTZy1RcxIXUkltM2FvT3VGSTZOM2JvUDZaU2ctUXM
Found Serial: 6
Found User ID: kbuvTg4mSDTE
Found App Name: Category Notes
Generated Project Name: category-notes
[v1_ChdSSW0zYW9PdUZJNk4zYm9QNlpTZy1RcxIXUkltM2FvT3VGSTZOM2JvUDZaU2ctUXM] Successfully saved to Database.
[v1_ChdSSW0zYW9PdUZJNk4zYm9QNlpTZy1RcxIXUkltM2FvT3VGSTZOM2JvUDZaU2ctUXM] Files successfully saved to disk.
{"success":true,"timestamp":"2026-09-26 04:58:46","interaction_id":"v1_ChdSSW0zYW9PdUZJNk4zYm9QNlpTZy1RcxIXUkltM2FvT3VGSTZOM2JvUDZaU2ctUXM","chat_id":"chat_new_87c0109d_1790406209","result":{"status":"completed","updated":true,"files_saved":true,"saved_file_count":10,"build_triggered":true,"api_serial":"6","project_name":"category-notes","build_http_code":0}}",
    "error": ""
}
[2026-09-26 04:58:46] [SUCCESS] Interaction ID received
{
    "interaction_id": "v1_ChdSSW0zYW9PdUZJNk4zYm9QNlpTZy1RcxIXUkltM2FvT3VGSTZOM2JvUDZaU2ctUXM",
    "serial": "6",
    "status": "completed"
}
[2026-09-26 04:58:46] [WARNING] Database connection lost. Reconnecting...
[2026-09-26 04:58:46] [START] Connecting to MySQL
[2026-09-26 04:58:46] [SUCCESS] MySQL connection successful
[2026-09-26 04:58:46] [START] Saving Gemini key usage
{
    "serial": "6",
    "minute": 1,
    "hour": 2,
    "day": 9
}
[2026-09-26 04:58:46] [SUCCESS] Gemini key usage saved
{
    "serial": "6"
}
[2026-09-26 04:58:46] [SUCCESS] Gemini key usage incremented after successful create
{
    "serial": "6",
    "usage": {
        "minute": 1,
        "hour": 2,
        "day": 9,
        "minuteReset": 1790413147806,
        "hourReset": 1790416573976,
        "dayReset": 1790428441010
    }
}
[2026-09-26 04:58:46] [SUCCESS] Returning interaction ID to frontend
{
    "interaction_id": "v1_ChdSSW0zYW9PdUZJNk4zYm9QNlpTZy1RcxIXUkltM2FvT3VGSTZOM2JvUDZaU2ctUXM",
    "status": "in_progress",
    "api_serial": "6"
}
[2026-09-26 04:58:46] [SUCCESS] Sending JSON response
{
    "http_status": 200,
    "response": {
        "success": true,
        "interaction_id": "v1_ChdSSW0zYW9PdUZJNk4zYm9QNlpTZy1RcxIXUkltM2FvT3VGSTZOM2JvUDZaU2ctUXM",
        "status": "in_progress",
        "api_serial": "6",
        "limits": {
            "minute": {
                "used": 1,
                "limit": 15,
                "remaining": 14
            },
            "hour": {
                "used": 2,
                "limit": 200,
                "remaining": 198
            },
            "day": {
                "used": 9,
                "limit": 500,
                "remaining": 491
            },
            "device": {
                "used": 12,
                "limit": 50,
                "remaining": 38
            }
        }
    }
}
[2026-09-26 04:58:46] [SUCCESS] jsonResponse() completed. Exiting worker.