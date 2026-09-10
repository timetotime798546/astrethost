package com.notesmaster.app;

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
    private EditText searchBox;
    private Spinner filterCategorySpinner;
    private Button btnAddNote;
    private TextView emptyText;

    private List<Note> allNotesList = new ArrayList<Note>();
    private List<Note> filteredNotesList = new ArrayList<Note>();
    private NotesAdapter adapter;

    // Filter spinner list (includes "All Categories")
    private final String[] filterCategories = {"All Categories", "Personal", "Work", "Ideas", "Todo", "Others"};
    // Note edit category spinner selection options
    private final String[] noteCategories = {"Personal", "Work", "Ideas", "Todo", "Others"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);

        notesListView = (ListView) findViewById(R.id.notes_list);
        searchBox = (EditText) findViewById(R.id.search_box);
        filterCategorySpinner = (Spinner) findViewById(R.id.filter_category_spinner);
        btnAddNote = (Button) findViewById(R.id.btn_add_note);
        emptyText = (TextView) findViewById(R.id.empty_text);

        // Configure Category Search Spinner Dropdown
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, filterCategories);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        filterCategorySpinner.setAdapter(spinnerAdapter);

        // Bind Base Custom Adapter
        adapter = new NotesAdapter(this, filteredNotesList);
        notesListView.setAdapter(adapter);

        // Action Trigger for Launching New Note Dialog form
        btnAddNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showNoteDialog(null);
            }
        });

        // Search edit listener mapping
        searchBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterNotes();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Spinner list selection filtering triggering
        filterCategorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filterNotes();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        loadNotesFromDatabase();
    }

    private void loadNotesFromDatabase() {
        allNotesList.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_NAME, null, null, null, null, null, DatabaseHelper.COLUMN_ID + " DESC");

        if (cursor != null) {
            while (cursor.moveToNext()) {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID));
                String title = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TITLE));
                String content = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CONTENT));
                String category = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CATEGORY));
                String timestamp = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TIMESTAMP));

                allNotesList.add(new Note(id, title, content, category, timestamp));
            }
            cursor.close();
        }
        filterNotes();
    }

    private void filterNotes() {
        filteredNotesList.clear();
        String searchQuery = searchBox.getText().toString().toLowerCase().trim();
        String selectedCategory = filterCategorySpinner.getSelectedItem().toString();

        for (int i = 0; i < allNotesList.size(); i++) {
            Note note = allNotesList.get(i);
            boolean matchesSearch = note.getTitle().toLowerCase().contains(searchQuery)
                    || note.getContent().toLowerCase().contains(searchQuery);

            boolean matchesCategory = selectedCategory.equals("All Categories")
                    || note.getCategory().equalsIgnoreCase(selectedCategory);

            if (matchesSearch && matchesCategory) {
                filteredNotesList.add(note);
            }
        }

        adapter.notifyDataSetChanged();

        if (filteredNotesList.isEmpty()) {
            emptyText.setVisibility(View.VISIBLE);
        } else {
            emptyText.setVisibility(View.GONE);
        }
    }

    private void showNoteDialog(final Note noteToEdit) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_note, null);
        builder.setView(dialogView);

        final AlertDialog dialog = builder.create();

        TextView dialogTitle = (TextView) dialogView.findViewById(R.id.dialog_title);
        final EditText editTitle = (EditText) dialogView.findViewById(R.id.edit_title);
        final Spinner editCategorySpinner = (Spinner) dialogView.findViewById(R.id.edit_category_spinner);
        final EditText editContent = (EditText) dialogView.findViewById(R.id.edit_content);
        Button btnCancel = (Button) dialogView.findViewById(R.id.btn_dialog_cancel);
        Button btnSave = (Button) dialogView.findViewById(R.id.btn_dialog_save);

        // Populate Category Spinner in Dialog
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, noteCategories);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        editCategorySpinner.setAdapter(categoryAdapter);

        if (noteToEdit != null) {
            dialogTitle.setText("Edit Note");
            editTitle.setText(noteToEdit.getTitle());
            editContent.setText(noteToEdit.getContent());

            for (int i = 0; i < noteCategories.length; i++) {
                if (noteCategories[i].equalsIgnoreCase(noteToEdit.getCategory())) {
                    editCategorySpinner.setSelection(i);
                    break;
                }
            }
        } else {
            dialogTitle.setText("Create Note");
        }

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String title = editTitle.getText().toString().trim();
                String content = editContent.getText().toString().trim();
                String category = editCategorySpinner.getSelectedItem().toString();

                if (title.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please enter a title", Toast.LENGTH_SHORT).show();
                    return;
                }

                String timestamp = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault()).format(new Date());

                SQLiteDatabase db = dbHelper.getWritableDatabase();
                ContentValues values = new ContentValues();
                values.put(DatabaseHelper.COLUMN_TITLE, title);
                values.put(DatabaseHelper.COLUMN_CONTENT, content);
                values.put(DatabaseHelper.COLUMN_CATEGORY, category);
                values.put(DatabaseHelper.COLUMN_TIMESTAMP, timestamp);

                if (noteToEdit != null) {
                    db.update(DatabaseHelper.TABLE_NAME, values, DatabaseHelper.COLUMN_ID + "=?",
                            new String[]{String.valueOf(noteToEdit.getId())});
                    Toast.makeText(MainActivity.this, "Note updated", Toast.LENGTH_SHORT).show();
                } else {
                    db.insert(DatabaseHelper.TABLE_NAME, null, values);
                    Toast.makeText(MainActivity.this, "Note added successfully", Toast.LENGTH_SHORT).show();
                }

                loadNotesFromDatabase();
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void deleteNote(final Note note) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Note");
        builder.setMessage("Are you sure you want to delete this note?");
        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                db.delete(DatabaseHelper.TABLE_NAME, DatabaseHelper.COLUMN_ID + "=?",
                        new String[]{String.valueOf(note.getId())});
                Toast.makeText(MainActivity.this, "Note deleted", Toast.LENGTH_SHORT).show();
                loadNotesFromDatabase();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private class NotesAdapter extends BaseAdapter {
        private Context context;
        private List<Note> notes;

        public NotesAdapter(Context context, List<Note> notes) {
            this.context = context;
            this.notes = notes;
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
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.list_item_note, parent, false);
            }

            final Note note = notes.get(position);

            TextView txtTitle = (TextView) convertView.findViewById(R.id.note_title);
            TextView txtCategory = (TextView) convertView.findViewById(R.id.note_category);
            TextView txtExcerpt = (TextView) convertView.findViewById(R.id.note_excerpt);
            TextView txtDate = (TextView) convertView.findViewById(R.id.note_date);
            Button btnEdit = (Button) convertView.findViewById(R.id.btn_item_edit);
            Button btnDelete = (Button) convertView.findViewById(R.id.btn_item_delete);

            txtTitle.setText(note.getTitle());
            txtCategory.setText(note.getCategory());
            txtExcerpt.setText(note.getContent().isEmpty() ? "(No details)" : note.getContent());
            txtDate.setText(note.getTimestamp());

            btnEdit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showNoteDialog(note);
                }
            });

            btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    deleteNote(note);
                }
            });

            return convertView;
        }
    }
}