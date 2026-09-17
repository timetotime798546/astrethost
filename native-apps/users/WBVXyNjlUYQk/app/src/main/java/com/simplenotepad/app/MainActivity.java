package com.simplenotepad.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final String PREFS_NAME = "SimpleNotepadPrefs";
    private static final String KEY_NOTES = "notes_list";

    // Form inputs and triggers
    private EditText searchInput;
    private TextView editorLabel;
    private EditText noteTitleInput;
    private EditText noteInput;
    private Button btnSave;
    private Button btnCancelEdit;
    private Button btnClear;
    private ListView notesListView;

    // Structured storage state
    private ArrayList<Note> notesList;
    private ArrayList<Note> filteredList;
    private NotesAdapter adapter;

    // Active edit indicator state
    private String editingNoteId = null;

    // Structured Note Model representing updated object configurations
    public static class Note {
        public String id;
        public String title;
        public String content;
        public long timestamp;

        public Note(String id, String title, String content, long timestamp) {
            this.id = id;
            this.title = title;
            this.content = content;
            this.timestamp = timestamp;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Retrieve widgets
        searchInput = (EditText) findViewById(R.id.search_input);
        editorLabel = (TextView) findViewById(R.id.editor_label);
        noteTitleInput = (EditText) findViewById(R.id.note_title_input);
        noteInput = (EditText) findViewById(R.id.note_input);
        btnSave = (Button) findViewById(R.id.btn_save);
        btnCancelEdit = (Button) findViewById(R.id.btn_cancel_edit);
        btnClear = (Button) findViewById(R.id.btn_clear);
        notesListView = (ListView) findViewById(R.id.notes_listview);

        // Load notepad records dynamically supporting old formats
        notesList = loadNotes();
        filteredList = new ArrayList<Note>(notesList);

        // Custom list adapters binding notes
        adapter = new NotesAdapter();
        notesListView.setAdapter(adapter);

        // Search text watcher instantiation
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterNotes(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Trigger notes save/update events
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String title = noteTitleInput.getText().toString().trim();
                String content = noteInput.getText().toString().trim();

                if (content.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Note content cannot be empty!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (editingNoteId == null) {
                    // Create mode initialization
                    String newId = String.valueOf(System.currentTimeMillis());
                    Note note = new Note(newId, title, content, System.currentTimeMillis());
                    notesList.add(0, note); // Insert modern note at the top
                    Toast.makeText(MainActivity.this, "Note saved!", Toast.LENGTH_SHORT).show();
                } else {
                    // Edit mode modification updates
                    for (int i = 0; i < notesList.size(); i++) {
                        if (notesList.get(i).id.equals(editingNoteId)) {
                            Note updatedNote = new Note(editingNoteId, title, content, System.currentTimeMillis());
                            notesList.set(i, updatedNote);
                            break;
                        }
                    }
                    Toast.makeText(MainActivity.this, "Note updated!", Toast.LENGTH_SHORT).show();
                    cancelEditing();
                }

                saveNotes();
                filterNotes(searchInput.getText().toString());
            }
        });

        // Clear current active editor states
        btnCancelEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelEditing();
            }
        });

        // Delete confirmation dialogue handlers for purging the database
        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (notesList.isEmpty()) {
                    Toast.makeText(MainActivity.this, "No notes to clear!", Toast.LENGTH_SHORT).show();
                    return;
                }

                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Clear All Notes")
                        .setMessage("Are you sure you want to delete all notes? This action cannot be undone.")
                        .setPositiveButton("Delete All", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                notesList.clear();
                                cancelEditing();
                                saveNotes();
                                filterNotes(searchInput.getText().toString());
                                Toast.makeText(MainActivity.this, "All notes cleared!", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });

        // Bind single list item clicks to load active note targets into editing panels
        notesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Note note = filteredList.get(position);
                editingNoteId = note.id;
                noteTitleInput.setText(note.title);
                noteInput.setText(note.content);
                btnSave.setText("Update Note");
                editorLabel.setText("Editing Note");
            }
        });

        // Item deletion long click listeners
        notesListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                final Note selectedNote = filteredList.get(position);
                String displayTitle = selectedNote.title.trim().isEmpty() ? "Untitled Note" : selectedNote.title;
                
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Delete Note")
                        .setMessage("Do you want to delete '" + displayTitle + "'?")
                        .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // Locate exact target inside original source listing
                                for (int i = 0; i < notesList.size(); i++) {
                                    if (notesList.get(i).id.equals(selectedNote.id)) {
                                        notesList.remove(i);
                                        break;
                                    }
                                }

                                if (editingNoteId != null && editingNoteId.equals(selectedNote.id)) {
                                    cancelEditing();
                                }

                                saveNotes();
                                filterNotes(searchInput.getText().toString());
                                Toast.makeText(MainActivity.this, "Note deleted!", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                return true;
            }
        });
    }

    // Restore standard editor elements
    private void cancelEditing() {
        editingNoteId = null;
        noteTitleInput.setText("");
        noteInput.setText("");
        btnSave.setText("Save Note");
        editorLabel.setText("Create Note");
    }

    // Refresh display lists to match query criteria
    private void filterNotes(String query) {
        filteredList.clear();
        String queryLower = query.toLowerCase(Locale.getDefault()).trim();
        for (int i = 0; i < notesList.size(); i++) {
            Note note = notesList.get(i);
            if (queryLower.isEmpty() || 
                note.title.toLowerCase(Locale.getDefault()).contains(queryLower) || 
                note.content.toLowerCase(Locale.getDefault()).contains(queryLower)) {
                filteredList.add(note);
            }
        }
        adapter.notifyDataSetChanged();
    }

    // Robust deserializer with support for both legacy Strings and newer structures
    private ArrayList<Note> loadNotes() {
        ArrayList<Note> list = new ArrayList<Note>();
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String json = prefs.getString(KEY_NOTES, null);
        if (json != null) {
            try {
                JSONArray jsonArray = new JSONArray(json);
                for (int i = 0; i < jsonArray.length(); i++) {
                    Object item = jsonArray.get(i);
                    if (item instanceof JSONObject) {
                        JSONObject obj = (JSONObject) item;
                        String id = obj.optString("id", String.valueOf(System.currentTimeMillis() + i));
                        String title = obj.optString("title", "");
                        String content = obj.optString("content", "");
                        long timestamp = obj.optLong("timestamp", System.currentTimeMillis());
                        list.add(new Note(id, title, content, timestamp));
                    } else if (item instanceof String) {
                        // Support migrating legacy notepad inputs directly
                        String legacyContent = (String) item;
                        String legacyTitle = "";
                        if (legacyContent.length() > 20) {
                            legacyTitle = legacyContent.substring(0, 18) + "...";
                        } else {
                            legacyTitle = legacyContent;
                        }
                        list.add(new Note(
                            String.valueOf(System.currentTimeMillis() + i),
                            legacyTitle,
                            legacyContent,
                            System.currentTimeMillis()
                        ));
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return list;
    }

    // Serialize custom Note models to local settings
    private void saveNotes() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        JSONArray jsonArray = new JSONArray();
        for (int i = 0; i < notesList.size(); i++) {
            Note note = notesList.get(i);
            JSONObject obj = new JSONObject();
            try {
                obj.put("id", note.id);
                obj.put("title", note.title);
                obj.put("content", note.content);
                obj.put("timestamp", note.timestamp);
                jsonArray.put(obj);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        editor.putString(KEY_NOTES, jsonArray.toString());
        editor.apply();
    }

    // Nested custom list view BaseAdapter class compliant with Java 8
    private class NotesAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return filteredList.size();
        }

        @Override
        public Object getItem(int position) {
            return filteredList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public android.view.View getView(int position, android.view.View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.note_item, parent, false);
            }

            Note note = filteredList.get(position);

            TextView txtTitle = (TextView) convertView.findViewById(R.id.item_title);
            TextView txtContent = (TextView) convertView.findViewById(R.id.item_content);
            TextView txtDate = (TextView) convertView.findViewById(R.id.item_date);

            String displayTitle = note.title.trim().isEmpty() ? "Untitled Note" : note.title;
            txtTitle.setText(displayTitle);
            txtContent.setText(note.content);

            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
            txtDate.setText(sdf.format(new Date(note.timestamp)));

            return convertView;
        }
    }
}