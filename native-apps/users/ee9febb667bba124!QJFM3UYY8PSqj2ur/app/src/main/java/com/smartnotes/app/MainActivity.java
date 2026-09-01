package com.smartnotes.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
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
import android.widget.ViewFlipper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private ViewFlipper viewFlipper;
    private EditText etSearch;
    private LinearLayout categoryTabsContainer;
    private ListView lvNotes;
    private TextView tvEmpty;
    private Button btnCreateNote;

    // Editor Screen Views
    private EditText etNoteTitle;
    private EditText etNoteContent;
    private Spinner spinnerCategory;
    private Button btnSave;
    private Button btnBack;
    private Button btnDelete;
    private TextView tvEditorTitle;

    private DatabaseHelper dbHelper;
    private NotesAdapter notesAdapter;
    private List<Note> currentNotesList = new ArrayList<>();

    private static final String[] CATEGORIES = {"Personal", "Work", "Study", "Ideas", "Others"};
    private String selectedCategory = "All";
    private String searchKeyword = "";
    private Note editingNote = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);

        // Bind list screen views
        viewFlipper = (ViewFlipper) findViewById(R.id.viewFlipper);
        etSearch = (EditText) findViewById(R.id.etSearch);
        categoryTabsContainer = (LinearLayout) findViewById(R.id.categoryTabsContainer);
        lvNotes = (ListView) findViewById(R.id.lvNotes);
        tvEmpty = (TextView) findViewById(R.id.tvEmpty);
        btnCreateNote = (Button) findViewById(R.id.btnCreateNote);

        // Bind editor screen views
        etNoteTitle = (EditText) findViewById(R.id.etNoteTitle);
        etNoteContent = (EditText) findViewById(R.id.etNoteContent);
        spinnerCategory = (Spinner) findViewById(R.id.spinnerCategory);
        btnSave = (Button) findViewById(R.id.btnSave);
        btnBack = (Button) findViewById(R.id.btnBack);
        btnDelete = (Button) findViewById(R.id.btnDelete);
        tvEditorTitle = (TextView) findViewById(R.id.tvEditorTitle);

        // Set up spinner
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, CATEGORIES);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(spinnerAdapter);

        // Set up List adapter
        notesAdapter = new NotesAdapter();
        lvNotes.setAdapter(notesAdapter);

        // Bind events
        setupEventListeners();

        // Load layout details
        populateCategoryTabs();
        refreshNotesList();
    }

    private void setupEventListeners() {
        // Search handler
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchKeyword = s.toString().trim();
                refreshNotesList();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // FAB Note click
        btnCreateNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openEditor(null);
            }
        });

        // List item click
        lvNotes.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Note note = currentNotesList.get(position);
                openEditor(note);
            }
        });

        // Save note click
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveNote();
            }
        });

        // Back editor click
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeEditor();
            }
        });

        // Delete handler
        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmDelete();
            }
        });
    }

    private void populateCategoryTabs() {
        categoryTabsContainer.removeAllViews();

        // Add "All" Tab
        addTabButton("All");

        // Add specific categories
        for (String cat : CATEGORIES) {
            addTabButton(cat);
        }
    }

    private void addTabButton(final String category) {
        final Button btn = new Button(this);
        btn.setText(category);
        btn.setAllCaps(false);
        btn.setTextSize(13f);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(10, 0, 10, 0);
        btn.setLayoutParams(params);

        boolean isSelected = category.equalsIgnoreCase(selectedCategory);
        android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
        shape.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        shape.setCornerRadius(30f);

        if (isSelected) {
            shape.setColor(0xFF2196F3); // Blue Primary
            btn.setTextColor(0xFFFFFFFF);
        } else {
            shape.setColor(0xFFECEFF1); // Soft Grey
            btn.setTextColor(0xFF37474F);
        }
        btn.setBackground(shape);
        btn.setPadding(36, 12, 36, 12);

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedCategory = category;
                populateCategoryTabs();
                refreshNotesList();
            }
        });

        categoryTabsContainer.addView(btn);
    }

    private void refreshNotesList() {
        currentNotesList.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String selection = null;
        List<String> selectionArgs = new ArrayList<>();

        // Filter Category first
        if (!"All".equalsIgnoreCase(selectedCategory)) {
            selection = "category = ?";
            selectionArgs.add(selectedCategory);
        }

        // Dynamic keyword search
        if (!searchKeyword.isEmpty()) {
            if (selection == null) {
                selection = "(title LIKE ? OR content LIKE ?)";
            } else {
                selection += " AND (title LIKE ? OR content LIKE ?)";
            }
            selectionArgs.add("%" + searchKeyword + "%");
            selectionArgs.add("%" + searchKeyword + "%");
        }

        String[] argsArray = selectionArgs.toArray(new String[0]);
        Cursor cursor = db.query("notes", null, selection, argsArray, null, null, "timestamp DESC");

        if (cursor != null) {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                String title = cursor.getString(cursor.getColumnIndexOrThrow("title"));
                String content = cursor.getString(cursor.getColumnIndexOrThrow("content"));
                String category = cursor.getString(cursor.getColumnIndexOrThrow("category"));
                long timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp"));

                currentNotesList.add(new Note(id, title, content, category, timestamp));
            }
            cursor.close();
        }

        notesAdapter.notifyDataSetChanged();

        if (currentNotesList.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            lvNotes.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            lvNotes.setVisibility(View.VISIBLE);
        }
    }

    private void openEditor(Note note) {
        editingNote = note;
        if (note == null) {
            // Create mode
            tvEditorTitle.setText("Create Note");
            etNoteTitle.setText("");
            etNoteContent.setText("");
            spinnerCategory.setSelection(0);
            btnDelete.setVisibility(View.GONE);
        } else {
            // Edit mode
            tvEditorTitle.setText("Edit Note");
            etNoteTitle.setText(note.title);
            etNoteContent.setText(note.content);
            btnDelete.setVisibility(View.VISIBLE);

            // Select correct category
            int index = 0;
            for (int i = 0; i < CATEGORIES.length; i++) {
                if (CATEGORIES[i].equalsIgnoreCase(note.category)) {
                    index = i;
                    break;
                }
            }
            spinnerCategory.setSelection(index);
        }
        viewFlipper.setDisplayedChild(1);
    }

    private void closeEditor() {
        // Hide Keyboard
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
        viewFlipper.setDisplayedChild(0);
        refreshNotesList();
    }

    private void saveNote() {
        String title = etNoteTitle.getText().toString().trim();
        String content = etNoteContent.getText().toString().trim();
        String category = spinnerCategory.getSelectedItem().toString();

        if (title.isEmpty()) {
            Toast.makeText(this, "Please enter a title", Toast.LENGTH_SHORT).show();
            return;
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("title", title);
        values.put("content", content);
        values.put("category", category);
        values.put("timestamp", System.currentTimeMillis());

        if (editingNote == null) {
            // Insert note
            db.insert("notes", null, values);
            Toast.makeText(this, "Note saved successfully", Toast.LENGTH_SHORT).show();
        } else {
            // Update note
            db.update("notes", values, "id = ?", new String[]{String.valueOf(editingNote.id)});
            Toast.makeText(this, "Note updated successfully", Toast.LENGTH_SHORT).show();
        }

        closeEditor();
    }

    private void confirmDelete() {
        if (editingNote == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Note");
        builder.setMessage("Are you sure you want to delete this note?");
        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                db.delete("notes", "id = ?", new String[]{String.valueOf(editingNote.id)});
                Toast.makeText(MainActivity.this, "Note deleted", Toast.LENGTH_SHORT).show();
                closeEditor();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    // Note Model Class
    public static class Note {
        public long id;
        public String title;
        public String content;
        public String category;
        public long timestamp;

        public Note(long id, String title, String content, String category, long timestamp) {
            this.id = id;
            this.title = title;
            this.content = content;
            this.category = category;
            this.timestamp = timestamp;
        }
    }

    // SQLite Database Helper
    public static class DatabaseHelper extends SQLiteOpenHelper {
        private static final String DATABASE_NAME = "notes.db";
        private static final int DATABASE_VERSION = 1;

        public DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE notes (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "title TEXT," +
                    "content TEXT," +
                    "category TEXT," +
                    "timestamp INTEGER" +
                    ")");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS notes");
            onCreate(db);
        }
    }

    // Base Adapter for List rendering
    private class NotesAdapter extends BaseAdapter {
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault());

        @Override
        public int getCount() {
            return currentNotesList.size();
        }

        @Override
        public Object getItem(int position) {
            return currentNotesList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return currentNotesList.get(position).id;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(MainActivity.this);
                convertView = inflater.inflate(R.layout.note_item, parent, false);
            }

            TextView tvTitle = (TextView) convertView.findViewById(R.id.tvNoteTitle);
            TextView tvCategory = (TextView) convertView.findViewById(R.id.tvNoteCategory);
            TextView tvSnippet = (TextView) convertView.findViewById(R.id.tvNoteSnippet);
            TextView tvDate = (TextView) convertView.findViewById(R.id.tvNoteDate);

            Note note = currentNotesList.get(position);

            tvTitle.setText(note.title);
            tvCategory.setText(note.category);
            tvSnippet.setText(note.content.isEmpty() ? "(No additional text)" : note.content);

            Date date = new Date(note.timestamp);
            tvDate.setText(dateFormat.format(date));

            // Dynamic theme pill styling mapping
            int bgColor;
            int textColor;
            if ("Work".equalsIgnoreCase(note.category)) {
                bgColor = 0xFFE3F2FD;
                textColor = 0xFF1565C0;
            } else if ("Personal".equalsIgnoreCase(note.category)) {
                bgColor = 0xFFE8F5E9;
                textColor = 0xFF2E7D32;
            } else if ("Study".equalsIgnoreCase(note.category)) {
                bgColor = 0xFFF3E5F5;
                textColor = 0xFF6A1B9A;
            } else if ("Ideas".equalsIgnoreCase(note.category)) {
                bgColor = 0xFFFFF8E1;
                textColor = 0xFFF57F17;
            } else {
                bgColor = 0xFFECEFF1;
                textColor = 0xFF37474F;
            }

            android.graphics.drawable.GradientDrawable pill = new android.graphics.drawable.GradientDrawable();
            pill.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
            pill.setCornerRadius(24f);
            pill.setColor(bgColor);
            tvCategory.setBackground(pill);
            tvCategory.setTextColor(textColor);

            return convertView;
        }
    }
}