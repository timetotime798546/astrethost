package com.notesphere.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
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
import java.util.Locale;

public class MainActivity extends Activity {

    // Database
    private NoteDatabaseHelper dbHelper;
    private SQLiteDatabase database;

    // Core Views
    private LinearLayout layoutDashboard;
    private LinearLayout layoutEditor;
    private ListView lvNotes;
    private TextView tvEmptyState;
    private EditText etSearch;
    private Spinner spinnerFilterCategory;
    private Button btnAddNote;

    // Editor Views
    private TextView tvEditorTitle;
    private EditText etNoteTitle;
    private EditText etNoteContent;
    private Spinner spinnerNoteCategory;
    private Button btnSaveNote;
    private Button btnCancelNote;

    // Categories array
    private final String[] categories = {"All Categories", "General", "Personal", "Work", "Ideas", "Urgent"};
    private final String[] editorCategories = {"General", "Personal", "Work", "Ideas", "Urgent"};

    // State Variables
    private boolean isEditing = false;
    private long editingNoteId = -1;
    private NotesAdapter notesAdapter;
    private ArrayList<Note> notesList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize DB
        dbHelper = new NoteDatabaseHelper(this);
        database = dbHelper.getWritableDatabase();
        dbHelper.populateDefaultNotes();

        // Initialize Core Component Views
        layoutDashboard = (LinearLayout) findViewById(R.id.layout_dashboard);
        layoutEditor = (LinearLayout) findViewById(R.id.layout_editor);
        lvNotes = (ListView) findViewById(R.id.lv_notes);
        tvEmptyState = (TextView) findViewById(R.id.tv_empty_state);
        etSearch = (EditText) findViewById(R.id.et_search);
        spinnerFilterCategory = (Spinner) findViewById(R.id.spinner_filter_category);
        btnAddNote = (Button) findViewById(R.id.btn_add_note);

        // Initialize Editor Components
        tvEditorTitle = (TextView) findViewById(R.id.tv_editor_title);
        etNoteTitle = (EditText) findViewById(R.id.et_note_title);
        etNoteContent = (EditText) findViewById(R.id.et_note_content);
        spinnerNoteCategory = (Spinner) findViewById(R.id.spinner_note_category);
        btnSaveNote = (Button) findViewById(R.id.btn_save_note);
        btnCancelNote = (Button) findViewById(R.id.btn_cancel_note);

        // Setup Spinner Adapters
        ArrayAdapter<String> filterAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, categories);
        filterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilterCategory.setAdapter(filterAdapter);

        ArrayAdapter<String> editorCategoryAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, editorCategories);
        editorCategoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerNoteCategory.setAdapter(editorCategoryAdapter);

        // Notes Collection
        notesList = new ArrayList<Note>();
        notesAdapter = new NotesAdapter(this, notesList);
        lvNotes.setAdapter(notesAdapter);

        // Attach View Listeners
        initListeners();

        // Load initial records
        refreshNotesList();
    }

    private void initListeners() {
        // Search bar changes
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                refreshNotesList();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Filter Category changes
        spinnerFilterCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                refreshNotesList();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Add Note Action Button
        btnAddNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openEditor(false, -1, "", "", "General");
            }
        });

        // Editor Save Button
        btnSaveNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveNote();
            }
        });

        // Editor Cancel Button
        btnCancelNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeEditor();
            }
        });

        // ListView note item click (Opens Editor in edit mode)
        lvNotes.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Note note = notesList.get(position);
                openEditor(true, note.id, note.title, note.content, note.category);
            }
        });
    }

    private void refreshNotesList() {
        notesList.clear();

        String searchQuery = etSearch.getText().toString().trim();
        String categoryFilter = spinnerFilterCategory.getSelectedItem().toString();

        StringBuilder query = new StringBuilder("SELECT * FROM " + NoteDatabaseHelper.TABLE_NAME);
        ArrayList<String> argsList = new ArrayList<String>();

        boolean hasFilter = !categoryFilter.equals("All Categories");
        boolean hasSearch = searchQuery.length() > 0;

        if (hasFilter || hasSearch) {
            query.append(" WHERE ");
            if (hasFilter) {
                query.append(NoteDatabaseHelper.COLUMN_CATEGORY).append(" = ? ");
                argsList.add(categoryFilter);
            }
            if (hasSearch) {
                if (hasFilter) {
                    query.append(" AND ");
                }
                query.append("(").append(NoteDatabaseHelper.COLUMN_TITLE).append(" LIKE ? OR ")
                     .append(NoteDatabaseHelper.COLUMN_CONTENT).append(" LIKE ?) ");
                argsList.add("%" + searchQuery + "%");
                argsList.add("%" + searchQuery + "%");
            }
        }

        query.append(" ORDER BY " + NoteDatabaseHelper.COLUMN_TIMESTAMP + " DESC");

        String[] selectionArgs = argsList.toArray(new String[0]);
        Cursor cursor = database.rawQuery(query.toString(), selectionArgs);

        if (cursor != null) {
            int idIndex = cursor.getColumnIndex(NoteDatabaseHelper.COLUMN_ID);
            int titleIndex = cursor.getColumnIndex(NoteDatabaseHelper.COLUMN_TITLE);
            int contentIndex = cursor.getColumnIndex(NoteDatabaseHelper.COLUMN_CONTENT);
            int categoryIndex = cursor.getColumnIndex(NoteDatabaseHelper.COLUMN_CATEGORY);
            int timeIndex = cursor.getColumnIndex(NoteDatabaseHelper.COLUMN_TIMESTAMP);

            while (cursor.moveToNext()) {
                long id = cursor.getLong(idIndex);
                String title = cursor.getString(titleIndex);
                String content = cursor.getString(contentIndex);
                String category = cursor.getString(categoryIndex);
                String timestamp = cursor.getString(timeIndex);

                notesList.add(new Note(id, title, content, category, timestamp));
            }
            cursor.close();
        }

        notesAdapter.notifyDataSetChanged();

        if (notesList.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            lvNotes.setVisibility(View.GONE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            lvNotes.setVisibility(View.VISIBLE);
        }
    }

    private void openEditor(boolean editMode, long noteId, String title, String content, String category) {
        isEditing = editMode;
        editingNoteId = noteId;

        if (editMode) {
            tvEditorTitle.setText("Edit Note");
            etNoteTitle.setText(title);
            etNoteContent.setText(content);

            // Select correct Spinner index
            for (int i = 0; i < editorCategories.length; i++) {
                if (editorCategories[i].equals(category)) {
                    spinnerNoteCategory.setSelection(i);
                    break;
                }
            }
        } else {
            tvEditorTitle.setText("New Note");
            etNoteTitle.setText("");
            etNoteContent.setText("");
            spinnerNoteCategory.setSelection(0); // General
        }

        layoutDashboard.setVisibility(View.GONE);
        btnAddNote.setVisibility(View.GONE);
        layoutEditor.setVisibility(View.VISIBLE);
    }

    private void closeEditor() {
        layoutEditor.setVisibility(View.GONE);
        layoutDashboard.setVisibility(View.VISIBLE);
        btnAddNote.setVisibility(View.VISIBLE);
        refreshNotesList();
    }

    private void saveNote() {
        String title = etNoteTitle.getText().toString().trim();
        String content = etNoteContent.getText().toString().trim();
        String category = spinnerNoteCategory.getSelectedItem().toString();

        if (title.isEmpty()) {
            Toast.makeText(this, "Title cannot be empty!", Toast.LENGTH_SHORT).show();
            return;
        }

        ContentValues values = new ContentValues();
        values.put(NoteDatabaseHelper.COLUMN_TITLE, title);
        values.put(NoteDatabaseHelper.COLUMN_CONTENT, content);
        values.put(NoteDatabaseHelper.COLUMN_CATEGORY, category);

        // Update timestamp manually to keep edited item at top
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        values.put(NoteDatabaseHelper.COLUMN_TIMESTAMP, dateFormat.format(new Date()));

        if (isEditing) {
            database.update(NoteDatabaseHelper.TABLE_NAME, values, NoteDatabaseHelper.COLUMN_ID + " = ?", new String[]{String.valueOf(editingNoteId)});
            Toast.makeText(this, "Note updated successfully", Toast.LENGTH_SHORT).show();
        } else {
            database.insert(NoteDatabaseHelper.TABLE_NAME, null, values);
            Toast.makeText(this, "Note created successfully", Toast.LENGTH_SHORT).show();
        }

        closeEditor();
    }

    private void deleteNote(final Note note) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirm Delete");
        builder.setMessage("Are you sure you want to delete '" + note.title + "'?");
        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                database.delete(NoteDatabaseHelper.TABLE_NAME, NoteDatabaseHelper.COLUMN_ID + " = ?", new String[]{String.valueOf(note.id)});
                Toast.makeText(MainActivity.this, "Note deleted", Toast.LENGTH_SHORT).show();
                refreshNotesList();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    // Dynamic Entity Object
    private static class Note {
        long id;
        String title;
        String content;
        String category;
        String timestamp;

        Note(long id, String title, String content, String category, String timestamp) {
            this.id = id;
            this.title = title;
            this.content = content;
            this.category = category;
            this.timestamp = timestamp;
        }
    }

    // Custom UI Adapter for Listing Notes
    private class NotesAdapter extends ArrayAdapter<Note> {

        NotesAdapter(Context context, ArrayList<Note> notes) {
            super(context, 0, notes);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.note_item, parent, false);
            }

            final Note note = getItem(position);

            TextView tvTitle = (TextView) convertView.findViewById(R.id.tv_item_title);
            TextView tvCategory = (TextView) convertView.findViewById(R.id.tv_item_category);
            TextView tvSnippet = (TextView) convertView.findViewById(R.id.tv_item_snippet);
            TextView tvTime = (TextView) convertView.findViewById(R.id.tv_item_time);
            Button btnDelete = (Button) convertView.findViewById(R.id.btn_item_delete);

            if (note != null) {
                tvTitle.setText(note.title);
                tvCategory.setText(note.category);
                
                // Show dynamic category indicator styling colors
                if ("Urgent".equalsIgnoreCase(note.category)) {
                    tvCategory.setTextColor(0xFFFF0000); // Red
                } else if ("Work".equalsIgnoreCase(note.category)) {
                    tvCategory.setTextColor(0xFF3F51B5); // Blue
                } else if ("Personal".equalsIgnoreCase(note.category)) {
                    tvCategory.setTextColor(0xFF4CAF50); // Green
                } else if ("Ideas".equalsIgnoreCase(note.category)) {
                    tvCategory.setTextColor(0xFFFF9800); // Orange
                } else {
                    tvCategory.setTextColor(0xFF757575); // Gray
                }

                String snippet = note.content;
                if (snippet == null || snippet.trim().isEmpty()) {
                    snippet = "(No content inside)";
                }
                tvSnippet.setText(snippet);

                // Format friendly visual date time
                String formattedTime = note.timestamp;
                try {
                    SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                    Date date = dbFormat.parse(note.timestamp);
                    if (date != null) {
                        SimpleDateFormat friendlyFormat = new SimpleDateFormat("MMM d, yyyy - h:mm a", Locale.getDefault());
                        formattedTime = friendlyFormat.format(date);
                    }
                } catch (Exception ignored) {}

                tvTime.setText(formattedTime);

                // Inline single tap deletion
                btnDelete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        deleteNote(note);
                    }
                });
            }

            return convertView;
        }
    }

    @Override
    protected void onDestroy() {
        if (database != null) {
            database.close();
        }
        if (dbHelper != null) {
            dbHelper.close();
        }
        super.onDestroy();
    }
}