package com.notesapp.app;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends android.app.Activity {

    private static final String TAG = "MainActivity";
    private static final int REQUEST_CODE_EDIT_NOTE = 1;

    private ListView notesListView;
    private TextView emptyTextView;
    private EditText searchEditText;
    private NoteAdapter adapter;
    private ArrayList<Note> allNotes;
    private ArrayList<Note> filteredNotes;
    private BackendApi backendApi;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mainHandler = new Handler(Looper.getMainLooper());
        backendApi = new BackendApi(this);

        notesListView = (ListView) findViewById(R.id.notesListView);
        emptyTextView = (TextView) findViewById(R.id.emptyTextView);
        searchEditText = (EditText) findViewById(R.id.searchEditText);

        allNotes = new ArrayList<Note>();
        filteredNotes = new ArrayList<Note>();
        adapter = new NoteAdapter(this, filteredNotes);
        notesListView.setAdapter(adapter);

        // Check if user is logged in
        if (!backendApi.isLoggedIn()) {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish(); // Prevent going back to MainActivity without login
            return;
        }

        notesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Note selectedNote = (Note) parent.getItemAtPosition(position);
                Intent intent = new Intent(MainActivity.this, NoteEditActivity.class);
                intent.putExtra("note", selectedNote);
                startActivityForResult(intent, REQUEST_CODE_EDIT_NOTE);
            }
        });

        notesListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                final Note selectedNote = (Note) parent.getItemAtPosition(position);
                showDeleteConfirmationDialog(selectedNote);
                return true;
            }
        });

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterNotes(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        fetchNotes();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Re-fetch notes in case there were changes from NoteEditActivity
        fetchNotes();
    }

    private void fetchNotes() {
        backendApi.readRecords("notes", new BackendApi.BackendApiListCallback() {
            @Override
            public void onSuccess(final JSONArray response) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        allNotes.clear();
                        try {
                            for (int i = 0; i < response.length(); i++) {
                                JSONObject record = response.getJSONObject(i);
                                String id = record.getString("id");
                                JSONObject data = record.getJSONObject("data");
                                String title = data.optString("title", "");
                                String content = data.optString("content", "");
                                String category = data.optString("category", "");
                                allNotes.add(new Note(id, title, content, category));
                            }
                            filterNotes(searchEditText.getText().toString());
                            updateEmptyState();
                        } catch (JSONException e) {
                            Log.e(TAG, "JSON parsing error for notes: " + e.getMessage());
                            Toast.makeText(MainActivity.this, "Error parsing notes.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }

            @Override
            public void onError(final String error) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Log.e(TAG, "Failed to fetch notes: " + error);
                        Toast.makeText(MainActivity.this, "Failed to fetch notes: " + error, Toast.LENGTH_LONG).show();
                        updateEmptyState();
                    }
                });
            }
        });
    }

    private void filterNotes(String query) {
        filteredNotes.clear();
        if (query == null || query.isEmpty()) {
            filteredNotes.addAll(allNotes);
        } else {
            query = query.toLowerCase(Locale.getDefault());
            for (Note note : allNotes) {
                if (note.getTitle().toLowerCase(Locale.getDefault()).contains(query) ||
                    note.getContent().toLowerCase(Locale.getDefault()).contains(query) ||
                    note.getCategory().toLowerCase(Locale.getDefault()).contains(query)) {
                    filteredNotes.add(note);
                }
            }
        }
        adapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (filteredNotes.isEmpty()) {
            notesListView.setVisibility(View.GONE);
            emptyTextView.setVisibility(View.VISIBLE);
        } else {
            notesListView.setVisibility(View.VISIBLE);
            emptyTextView.setVisibility(View.GONE);
        }
    }

    private void showDeleteConfirmationDialog(final Note note) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Note")
                .setMessage("Are you sure you want to delete this note?")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteNote(note);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteNote(Note note) {
        backendApi.deleteRecord(note.getId(), new BackendApi.BackendApiCallback() {
            @Override
            public void onSuccess(final JSONObject response) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "Note deleted successfully!", Toast.LENGTH_SHORT).show();
                        fetchNotes(); // Refresh the list
                    }
                });
            }

            @Override
            public void onError(final String error) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Log.e(TAG, "Failed to delete note: " + error);
                        Toast.makeText(MainActivity.this, "Failed to delete note: " + error, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_add_note) {
            Intent intent = new Intent(MainActivity.this, NoteEditActivity.class);
            startActivityForResult(intent, REQUEST_CODE_EDIT_NOTE);
            return true;
        } else if (id == R.id.action_logout) {
            logoutUser();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void logoutUser() {
        backendApi.logout(new BackendApi.BackendApiCallback() {
            @Override
            public void onSuccess(final JSONObject response) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "Logged out successfully!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                        startActivity(intent);
                        finish();
                    }
                });
            }

            @Override
            public void onError(final String error) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Log.e(TAG, "Logout failed: " + error);
                        Toast.makeText(MainActivity.this, "Logout failed: " + error, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_EDIT_NOTE && resultCode == RESULT_OK) {
            fetchNotes(); // Refresh notes if a note was added, edited, or deleted
        }
    }
}