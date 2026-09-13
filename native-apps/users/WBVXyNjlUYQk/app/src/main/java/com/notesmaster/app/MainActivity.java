package com.notesmaster.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
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
    private EditText searchBox;
    private Spinner filterSpinner;
    private ListView notesListView;
    private TextView emptyView;
    private Button btnAddNote;
    private Button btnAddCategory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);

        searchBox = (EditText) findViewById(R.id.search_box);
        filterSpinner = (Spinner) findViewById(R.id.filter_spinner);
        notesListView = (ListView) findViewById(R.id.notes_list);
        emptyView = (TextView) findViewById(R.id.empty_view);
        btnAddNote = (Button) findViewById(R.id.btn_add_note);
        btnAddCategory = (Button) findViewById(R.id.btn_add_category);

        // Populate dynamic category selector
        refreshCategoryFilterSpinner();

        // Register action triggers
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

        notesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Note selectedNote = (Note) parent.getItemAtPosition(position);
                showNoteDialog(selectedNote);
            }
        });

        filterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                refreshNotesList();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        searchBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                refreshNotesList();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Initialize display
        refreshNotesList();
    }

    private void refreshCategoryFilterSpinner() {
        List<String> categories = new ArrayList<String>();
        categories.add("All Categories");

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT name FROM categories ORDER BY name ASC", null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String name = cursor.getString(0);
                if (!name.equalsIgnoreCase("All Categories")) {
                    categories.add(name);
                }
            }
            cursor.close();
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        filterSpinner.setAdapter(adapter);
    }

    private List<String> getFormCategories() {
        List<String> list = new ArrayList<String>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT name FROM categories ORDER BY name ASC", null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                list.add(cursor.getString(0));
            }
            cursor.close();
        }
        if (list.isEmpty()) {
            list.add("General");
        }
        return list;
    }

    private void refreshNotesList() {
        String selectedCategory = "All Categories";
        if (filterSpinner.getSelectedItem() != null) {
            selectedCategory = filterSpinner.getSelectedItem().toString();
        }
        String searchQuery = searchBox.getText().toString();

        List<Note> notes = searchNotes(selectedCategory, searchQuery);

        if (notes.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            notesListView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            notesListView.setVisibility(View.VISIBLE);
        }

        NoteAdapter adapter = new NoteAdapter(this, notes, new NoteAdapter.OnNoteDeleteListener() {
            @Override
            public void onDelete(final Note note) {
                new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Delete Note")
                    .setMessage("Are you sure you want to delete this note?")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            SQLiteDatabase db = dbHelper.getWritableDatabase();
                            db.delete("notes", "_id = ?", new String[]{String.valueOf(note.getId())});
                            Toast.makeText(MainActivity.this, "Note deleted successfully", Toast.LENGTH_SHORT).show();
                            refreshNotesList();
                        }
                    })
                    .setNegativeButton("No", null)
                    .show();
            }
        });
        notesListView.setAdapter(adapter);
    }

    private List<Note> searchNotes(String category, String query) {
        List<Note> list = new ArrayList<Note>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        StringBuilder sql = new StringBuilder("SELECT * FROM notes WHERE 1=1");
        List<String> args = new ArrayList<String>();

        if (category != null && !category.equals("All Categories") && !category.isEmpty()) {
            sql.append(" AND category = ?");
            args.add(category);
        }

        if (query != null && !query.trim().isEmpty()) {
            sql.append(" AND (title LIKE ? OR content LIKE ?)");
            args.add("%" + query + "%");
            args.add("%" + query + "%");
        }

        sql.append(" ORDER BY _id DESC");

        String[] selectionArgs = new String[args.size()];
        selectionArgs = args.toArray(selectionArgs);

        Cursor cursor = db.rawQuery(sql.toString(), selectionArgs);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow("_id"));
                String title = cursor.getString(cursor.getColumnIndexOrThrow("title"));
                String content = cursor.getString(cursor.getColumnIndexOrThrow("content"));
                String cat = cursor.getString(cursor.getColumnIndexOrThrow("category"));
                String ts = cursor.getString(cursor.getColumnIndexOrThrow("timestamp"));
                list.add(new Note(id, title, content, cat, ts));
            }
            cursor.close();
        }
        return list;
    }

    private void showNoteDialog(final Note noteToEdit) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_note, null);
        builder.setView(dialogView);

        final EditText titleEdit = (EditText) dialogView.findViewById(R.id.dialog_note_title);
        final EditText contentEdit = (EditText) dialogView.findViewById(R.id.dialog_note_content);
        final Spinner categorySpinner = (Spinner) dialogView.findViewById(R.id.dialog_note_category);

        List<String> categories = getFormCategories();
        ArrayAdapter<String> catAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, categories);
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(catAdapter);

        if (noteToEdit != null) {
            titleEdit.setText(noteToEdit.getTitle());
            contentEdit.setText(noteToEdit.getContent());
            int idx = categories.indexOf(noteToEdit.getCategory());
            if (idx >= 0) {
                categorySpinner.setSelection(idx);
            }
            builder.setTitle("Edit Note");
        } else {
            builder.setTitle("New Note");
        }

        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String title = titleEdit.getText().toString().trim();
                String content = contentEdit.getText().toString().trim();
                String category = "General";
                if (categorySpinner.getSelectedItem() != null) {
                    category = categorySpinner.getSelectedItem().toString();
                }

                if (title.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Title cannot be empty", Toast.LENGTH_SHORT).show();
                    return;
                }

                SQLiteDatabase db = dbHelper.getWritableDatabase();
                ContentValues values = new ContentValues();
                values.put("title", title);
                values.put("content", content);
                values.put("category", category);
                values.put("timestamp", getFormattedCurrentTime());

                if (noteToEdit != null) {
                    db.update("notes", values, "_id = ?", new String[]{String.valueOf(noteToEdit.getId())});
                    Toast.makeText(MainActivity.this, "Note updated", Toast.LENGTH_SHORT).show();
                } else {
                    db.insert("notes", null, values);
                    Toast.makeText(MainActivity.this, "Note added", Toast.LENGTH_SHORT).show();
                }

                refreshNotesList();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.create().show();
    }

    private void showCategoryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_category, null);
        builder.setView(dialogView);
        builder.setTitle("Create Custom Category");

        final EditText categoryEdit = (EditText) dialogView.findViewById(R.id.dialog_category_name);

        builder.setPositiveButton("Add", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String name = categoryEdit.getText().toString().trim();
                if (name.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Category name cannot be empty", Toast.LENGTH_SHORT).show();
                    return;
                }

                SQLiteDatabase db = dbHelper.getWritableDatabase();
                ContentValues values = new ContentValues();
                values.put("name", name);

                long result = db.insertWithOnConflict("categories", null, values, SQLiteDatabase.CONFLICT_IGNORE);
                if (result == -1) {
                    Toast.makeText(MainActivity.this, "Category already exists", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Category '" + name + "' added", Toast.LENGTH_SHORT).show();
                    refreshCategoryFilterSpinner();
                }
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.create().show();
    }

    private String getFormattedCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault());
        return sdf.format(new Date());
    }
}