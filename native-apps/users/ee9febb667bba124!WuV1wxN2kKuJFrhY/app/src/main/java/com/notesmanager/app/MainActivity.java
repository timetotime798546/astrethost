package com.notesmanager.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
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
import android.widget.LinearLayout;
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

    // Available categories (The first is special "All" category filter)
    private static final String[] CATEGORIES = {"Personal", "Work", "Ideas", "Todo", "Uncategorized"};
    private static final String FILTER_ALL = "All";

    // DB Constants
    private static final String DATABASE_NAME = "notes_manager.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_NOTES = "notes";
    private static final String COL_ID = "id";
    private static final String COL_TITLE = "title";
    private static final String COL_CONTENT = "content";
    private static final String COL_CATEGORY = "category";
    private static final String COL_TIMESTAMP = "timestamp";

    private NotesDatabaseHelper dbHelper;
    private NoteAdapter noteAdapter;
    private List<Note> notesList;

    // View References
    private ListView notesListView;
    private TextView emptyView;
    private EditText searchBar;
    private LinearLayout filterContainer;
    private RelativeLayout editorOverlay;
    
    // Editor UI Elements
    private TextView editorTitleLabel;
    private EditText editNoteTitle;
    private EditText editNoteContent;
    private Spinner categorySpinner;
    private Button btnSaveNote;
    private Button btnCancelEditor;
    private Button btnAddNote;

    // Active States
    private String selectedFilterCategory = FILTER_ALL;
    private String searchQueryText = "";
    private Note editingNote = null; // null represents dynamic layout state "Add Mode"

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutId("activity_main"));

        // Initialize SQLite helper
        dbHelper = new NotesDatabaseHelper(this);
        notesList = new ArrayList<>();

        // Bind layouts
        notesListView = (ListView) findViewById(getViewId("notes_list_view"));
        emptyView = (TextView) findViewById(getViewId("empty_view"));
        searchBar = (EditText) findViewById(getViewId("search_bar"));
        filterContainer = (LinearLayout) findViewById(getViewId("categories_filter_container"));
        editorOverlay = (RelativeLayout) findViewById(getViewId("editor_overlay"));
        
        editorTitleLabel = (TextView) findViewById(getViewId("editor_title"));
        editNoteTitle = (EditText) findViewById(getViewId("edit_note_title"));
        editNoteContent = (EditText) findViewById(getViewId("edit_note_content"));
        categorySpinner = (Spinner) findViewById(getViewId("category_spinner"));
        btnSaveNote = (Button) findViewById(getViewId("btn_save_note"));
        btnCancelEditor = (Button) findViewById(getViewId("btn_cancel_editor"));
        btnAddNote = (Button) findViewById(getViewId("btn_add_note"));

        // Set up search bar logic (using text watcher to preserve Java 8 compatibility)
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQueryText = s.toString();
                refreshNotesList();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Set up Category Spinner in Overlay Editor
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, CATEGORIES);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(spinnerAdapter);

        // Bind event listeners
        btnAddNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openNoteEditor(null);
            }
        });

        btnCancelEditor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeNoteEditor();
            }
        });

        btnSaveNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveActiveNote();
            }
        });

        // Setup lists & views
        noteAdapter = new NoteAdapter(this, notesList);
        notesListView.setAdapter(noteAdapter);

        setupCategoryFilters();
        refreshNotesList();
    }

    private void setupCategoryFilters() {
        filterContainer.removeAllViews();

        // 1. All Button
        final Button btnAll = createCategoryFilterButton(FILTER_ALL);
        filterContainer.addView(btnAll);

        // 2. Specific Categories Buttons
        for (int i = 0; i < CATEGORIES.length; i++) {
            final String category = CATEGORIES[i];
            Button btn = createCategoryFilterButton(category);
            filterContainer.addView(btn);
        }
    }

    private Button createCategoryFilterButton(final String categoryName) {
        final Button btn = new Button(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(4, 4, 4, 4);
        btn.setLayoutParams(params);
        btn.setText(categoryName);
        btn.setTextSize(12spToPx(11));
        btn.setPadding(16, 0, 16, 0);

        // Styling state: active/inactive state
        styleFilterButton(btn, categoryName.equals(selectedFilterCategory));

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedFilterCategory = categoryName;
                setupCategoryFilters(); // re-render filters styling
                refreshNotesList();
            }
        });

        return btn;
    }

    private int spToPx(int sp) {
        return (int) (sp * getResources().getDisplayMetrics().scaledDensity);
    }

    private float spToPx(float sp) {
        return sp * getResources().getDisplayMetrics().scaledDensity;
    }

    private void styleFilterButton(Button btn, boolean isActive) {
        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(30);
        
        if (isActive) {
            shape.setColor(getColorByTag("primary"));
            btn.setTextColor(Color.WHITE);
        } else {
            shape.setColor(Color.WHITE);
            shape.setStroke(2, Color.LTGRAY);
            btn.setTextColor(Color.DKGRAY);
        }
        btn.setBackground(shape);
    }

    private int getColorByTag(String tag) {
        int resId = getResources().getIdentifier(tag, "color", getPackageName());
        if (resId != 0) {
            return getResources().getColor(resId);
        }
        return Color.GRAY;
    }

    private void refreshNotesList() {
        notesList.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String selection = null;
        List<String> selectionArgs = new ArrayList<>();

        // Filtering options
        if (!selectedFilterCategory.equals(FILTER_ALL)) {
            selection = COL_CATEGORY + " = ?";
            selectionArgs.add(selectedFilterCategory);
        }

        if (searchQueryText != null && searchQueryText.trim().length() > 0) {
            String searchPattern = "%" + searchQueryText.trim() + "%";
            if (selection == null) {
                selection = "(" + COL_TITLE + " LIKE ? OR " + COL_CONTENT + " LIKE ?)";
            } else {
                selection += " AND (" + COL_TITLE + " LIKE ? OR " + COL_CONTENT + " LIKE ?)";
            }
            selectionArgs.add(searchPattern);
            selectionArgs.add(searchPattern);
        }

        String[] selectionArgsArray = null;
        if (!selectionArgs.isEmpty()) {
            selectionArgsArray = selectionArgs.toArray(new String[0]);
        }

        Cursor cursor = db.query(
                TABLE_NOTES,
                null,
                selection,
                selectionArgsArray,
                null,
                null,
                COL_TIMESTAMP + " DESC"
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                Note note = new Note();
                note.id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID));
                note.title = cursor.getString(cursor.getColumnIndexOrThrow(COL_TITLE));
                note.content = cursor.getString(cursor.getColumnIndexOrThrow(COL_CONTENT));
                note.category = cursor.getString(cursor.getColumnIndexOrThrow(COL_CATEGORY));
                note.timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COL_TIMESTAMP));
                notesList.add(note);
            }
            cursor.close();
        }

        noteAdapter.notifyDataSetChanged();

        // Manage visibility of Empty State view
        if (notesList.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            notesListView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            notesListView.setVisibility(View.VISIBLE);
        }
    }

    private void openNoteEditor(Note note) {
        editingNote = note;
        if (note == null) {
            // New Note Mode
            editorTitleLabel.setText("Create New Note");
            editNoteTitle.setText("");
            editNoteContent.setText("");
            categorySpinner.setSelection(0);
        } else {
            // Edit Note Mode
            editorTitleLabel.setText("Edit Note");
            editNoteTitle.setText(note.title);
            editNoteContent.setText(note.content);

            // Locate corresponding spinner index
            int selectionIndex = 0;
            for (int i = 0; i < CATEGORIES.length; i++) {
                if (CATEGORIES[i].equalsIgnoreCase(note.category)) {
                    selectionIndex = i;
                    break;
                }
            }
            categorySpinner.setSelection(selectionIndex);
        }
        editorOverlay.setVisibility(View.VISIBLE);
    }

    private void closeNoteEditor() {
        editorOverlay.setVisibility(View.GONE);
        editingNote = null;
    }

    private void saveActiveNote() {
        String title = editNoteTitle.getText().toString().trim();
        String content = editNoteContent.getText().toString().trim();
        String category = categorySpinner.getSelectedItem().toString();

        if (title.isEmpty()) {
            Toast.makeText(this, "Please enter a title", Toast.LENGTH_SHORT).show();
            return;
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_TITLE, title);
        values.put(COL_CONTENT, content);
        values.put(COL_CATEGORY, category);
        values.put(COL_TIMESTAMP, System.currentTimeMillis());

        if (editingNote == null) {
            // Add new database note record
            db.insert(TABLE_NOTES, null, values);
            Toast.makeText(this, "Note saved successfully!", Toast.LENGTH_SHORT).show();
        } else {
            // Update existing entry
            db.update(TABLE_NOTES, values, COL_ID + " = ?", new String[]{String.valueOf(editingNote.id)});
            Toast.makeText(this, "Note updated successfully!", Toast.LENGTH_SHORT).show();
        }

        closeNoteEditor();
        refreshNotesList();
    }

    private void confirmDeleteNote(final Note note) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirm Deletion");
        builder.setMessage("Are you sure you want to permanently delete '" + note.title + "'?");
        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                db.delete(TABLE_NOTES, COL_ID + " = ?", new String[]{String.valueOf(note.id)});
                Toast.makeText(MainActivity.this, "Note deleted", Toast.LENGTH_SHORT).show();
                refreshNotesList();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    // Helper utilities to retrieve resource identifiers on the fly
    private int getLayoutId(String name) {
        return getResources().getIdentifier(name, "layout", getPackageName());
    }

    private int getViewId(String name) {
        return getResources().getIdentifier(name, "id", getPackageName());
    }

    private int getColorId(String name) {
        return getResources().getIdentifier(name, "color", getPackageName());
    }

    // SQLite Database Handler
    private static class NotesDatabaseHelper extends SQLiteOpenHelper {

        public NotesDatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            String createTableQuery = "CREATE TABLE " + TABLE_NOTES + " (" +
                    COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COL_TITLE + " TEXT NOT NULL, " +
                    COL_CONTENT + " TEXT, " +
                    COL_CATEGORY + " TEXT, " +
                    COL_TIMESTAMP + " INTEGER" +
                    ")";
            db.execSQL(createTableQuery);

            // Populating visual sample notes for layout demo
            insertDemoNote(db, "Welcome to Notes Manager", "Create, categorize, edit or delete notes freely.", "Personal");
            insertDemoNote(db, "Weekly Agenda", "1. Sync with client development team\n2. Review mockups and finalize layouts\n3. Deploy version patches", "Work");
            insertDemoNote(db, "Invention Idea", "An Android App that helps write notes cleanly using purely customized UI themes.", "Ideas");
        }

        private void insertDemoNote(SQLiteDatabase db, String title, String content, String category) {
            ContentValues cv = new ContentValues();
            cv.put(COL_TITLE, title);
            cv.put(COL_CONTENT, content);
            cv.put(COL_CATEGORY, category);
            cv.put(COL_TIMESTAMP, System.currentTimeMillis());
            db.insert(TABLE_NOTES, null, cv);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOTES);
            onCreate(db);
        }
    }

    // Custom Note object class
    private static class Note {
        long id;
        String title;
        String content;
        String category;
        long timestamp;
    }

    // List Adapter handling display layout inflation and customization
    private class NoteAdapter extends BaseAdapter {

        private List<Note> notes;
        private LayoutInflater inflater;

        public NoteAdapter(Context context, List<Note> notes) {
            this.notes = notes;
            this.inflater = LayoutInflater.from(context);
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
            return notes.get(position).id;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = inflater.inflate(getLayoutId("note_item"), parent, false);
            }

            final Note note = notes.get(position);

            TextView noteTitle = (TextView) convertView.findViewById(getViewId("note_item_title"));
            TextView noteCategory = (TextView) convertView.findViewById(getViewId("note_item_category"));
            TextView noteContent = (TextView) convertView.findViewById(getViewId("note_item_content"));
            TextView noteDate = (TextView) convertView.findViewById(getViewId("note_item_date"));
            View noteTagContainer = convertView.findViewById(getViewId("note_tag_container"));

            Button btnEdit = (Button) convertView.findViewById(getViewId("btn_item_edit"));
            Button btnDelete = (Button) convertView.findViewById(getViewId("btn_item_delete"));

            noteTitle.setText(note.title);
            noteContent.setText(note.content);
            noteCategory.setText(note.category.toUpperCase());

            // Convert and style dates nicely
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd hh:mm a", Locale.getDefault());
            String formattedDate = sdf.format(new Date(note.timestamp));
            noteDate.setText(formattedDate);

            // Style category badge visually based on predefined tag colors
            GradientDrawable badgeShape = new GradientDrawable();
            badgeShape.setCornerRadius(8);
            String categoryLower = note.category.toLowerCase();
            int colorRes = getColorId("cat_" + categoryLower);
            if (colorRes == 0) {
                colorRes = getColorId("cat_uncategorized");
            }
            badgeShape.setColor(getResources().getColor(colorRes));
            noteTagContainer.setBackground(badgeShape);

            // Setup button click bindings dynamically (Java 8 anonymous interfaces)
            btnEdit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openNoteEditor(note);
                }
            });

            btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    confirmDeleteNote(note);
                }
            });

            return convertView;
        }
    }
}