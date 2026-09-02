package com.noteflow.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.text.Editable;
import android.text.TextWatcher;

public class MainActivity extends Activity {

    private DatabaseHelper dbHelper;
    private NoteAdapter adapter;
    private java.util.List<Note> currentNotesList;

    // Layout views
    private View listContainer;
    private View formContainer;

    // Main panel widgets
    private EditText searchInput;
    private Spinner filterSpinner;
    private ListView notesListView;
    private Button btnAddNote;

    // Form widgets
    private TextView formTitle;
    private EditText noteTitleInput;
    private Spinner noteCategorySpinner;
    private EditText noteContentInput;
    private Button btnSaveNote;
    private Button btnCancelNote;

    // State metrics
    private boolean isEditing = false;
    private long editingNoteId = -1;
    private String currentSearchQuery = "";
    private String currentCategoryFilter = "All Categories";

    private final String[] filterCategories = {"All Categories", "Work", "Personal", "Ideas", "Todo", "Others"};
    private final String[] formCategories = {"Work", "Personal", "Ideas", "Todo", "Others"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getResources().getIdentifier("activity_main", "layout", getPackageName()));

        dbHelper = new DatabaseHelper(this);
        currentNotesList = new java.util.ArrayList<Note>();

        initViews();
        setupSpinners();
        setupListeners();

        refreshNotesList();
    }

    private void initViews() {
        listContainer = findViewById(getResources().getIdentifier("list_container", "id", getPackageName()));
        formContainer = findViewById(getResources().getIdentifier("form_container", "id", getPackageName()));

        searchInput = (EditText) findViewById(getResources().getIdentifier("search_input", "id", getPackageName()));
        filterSpinner = (Spinner) findViewById(getResources().getIdentifier("filter_spinner", "id", getPackageName()));
        notesListView = (ListView) findViewById(getResources().getIdentifier("notes_listview", "id", getPackageName()));
        btnAddNote = (Button) findViewById(getResources().getIdentifier("btn_add_note", "id", getPackageName()));

        formTitle = (TextView) findViewById(getResources().getIdentifier("form_title", "id", getPackageName()));
        noteTitleInput = (EditText) findViewById(getResources().getIdentifier("note_title_input", "id", getPackageName()));
        noteCategorySpinner = (Spinner) findViewById(getResources().getIdentifier("note_category_spinner", "id", getPackageName()));
        noteContentInput = (EditText) findViewById(getResources().getIdentifier("note_content_input", "id", getPackageName()));
        btnSaveNote = (Button) findViewById(getResources().getIdentifier("btn_save_note", "id", getPackageName()));
        btnCancelNote = (Button) findViewById(getResources().getIdentifier("btn_cancel_note", "id", getPackageName()));
    }

    private void setupSpinners() {
        // Setup Filter Categories dropdown list
        ArrayAdapter<String> filterAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, filterCategories);
        filterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        filterSpinner.setAdapter(filterAdapter);

        // Setup Form Category picker spinner
        ArrayAdapter<String> formAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, formCategories);
        formAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        noteCategorySpinner.setAdapter(formAdapter);
    }

    private void setupListeners() {
        // Live text change listener for notes searching
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString();
                refreshNotesList();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Dropdown filter listener for filtering categories
        filterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentCategoryFilter = filterCategories[position];
                refreshNotesList();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Navigate to note creation screen
        btnAddNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openForm(false, null);
            }
        });

        // Exit editor layout view
        btnCancelNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeForm();
            }
        });

        // Save note configuration logic click binding
        btnSaveNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveNote();
            }
        });

        // List click item handler triggers edit options panel
        notesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Note clickedNote = currentNotesList.get(position);
                openForm(true, clickedNote);
            }
        });
    }

    private void openForm(boolean editMode, Note note) {
        isEditing = editMode;
        if (editMode && note != null) {
            editingNoteId = note.id;
            formTitle.setText("Edit Note");
            noteTitleInput.setText(note.title);
            noteContentInput.setText(note.content);

            for (int i = 0; i < formCategories.length; i++) {
                if (formCategories[i].equals(note.category)) {
                    noteCategorySpinner.setSelection(i);
                    break;
                }
            }
        } else {
            editingNoteId = -1;
            formTitle.setText("Create Note");
            noteTitleInput.setText("");
            noteContentInput.setText("");
            noteCategorySpinner.setSelection(0);
        }

        listContainer.setVisibility(View.GONE);
        formContainer.setVisibility(View.VISIBLE);
    }

    private void closeForm() {
        listContainer.setVisibility(View.VISIBLE);
        formContainer.setVisibility(View.GONE);
    }

    private void saveNote() {
        String title = noteTitleInput.getText().toString().trim();
        String content = noteContentInput.getText().toString().trim();
        String category = noteCategorySpinner.getSelectedItem().toString();

        if (title.isEmpty()) {
            Toast.makeText(this, "Please enter a title", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isEditing) {
            updateNote(editingNoteId, title, content, category);
            Toast.makeText(this, "Note updated!", Toast.LENGTH_SHORT).show();
        } else {
            insertNote(title, content, category);
            Toast.makeText(this, "Note saved!", Toast.LENGTH_SHORT).show();
        }

        closeForm();
        refreshNotesList();
    }

    private void refreshNotesList() {
        currentNotesList = getAllNotes(currentSearchQuery, currentCategoryFilter);
        if (adapter == null) {
            adapter = new NoteAdapter(this, currentNotesList);
            notesListView.setAdapter(adapter);
        } else {
            adapter.setNotes(currentNotesList);
        }
    }

    // SQLite data helpers
    private java.util.List<Note> getAllNotes(String searchQuery, String categoryFilter) {
        java.util.List<Note> list = new java.util.ArrayList<Note>();
        android.database.sqlite.SQLiteDatabase db = dbHelper.getReadableDatabase();

        StringBuilder selection = new StringBuilder();
        java.util.List<String> selectionArgs = new java.util.ArrayList<String>();

        if (categoryFilter != null && !categoryFilter.equals("All Categories")) {
            selection.append("category = ?");
            selectionArgs.add(categoryFilter);
        }

        if (searchQuery != null && searchQuery.trim().length() > 0) {
            if (selection.length() > 0) {
                selection.append(" AND ");
            }
            selection.append("(title LIKE ? OR content LIKE ?)");
            selectionArgs.add("%" + searchQuery + "%");
            selectionArgs.add("%" + searchQuery + "%");
        }

        String whereClause = selection.length() > 0 ? selection.toString() : null;
        String[] whereArgs = selectionArgs.isEmpty() ? null : selectionArgs.toArray(new String[0]);

        android.database.Cursor cursor = db.query(
                "notes",
                null,
                whereClause,
                whereArgs,
                null,
                null,
                "id DESC"
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                String title = cursor.getString(cursor.getColumnIndexOrThrow("title"));
                String content = cursor.getString(cursor.getColumnIndexOrThrow("content"));
                String category = cursor.getString(cursor.getColumnIndexOrThrow("category"));
                String timestamp = cursor.getString(cursor.getColumnIndexOrThrow("timestamp"));
                list.add(new Note(id, title, content, category, timestamp));
            }
            cursor.close();
        }
        return list;
    }

    private void insertNote(String title, String content, String category) {
        android.database.sqlite.SQLiteDatabase db = dbHelper.getWritableDatabase();
        android.content.ContentValues values = new android.content.ContentValues();
        values.put("title", title);
        values.put("content", content);
        values.put("category", category);

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd, yyyy hh:mm a", java.util.Locale.getDefault());
        String currentDateAndTime = sdf.format(new java.util.Date());
        values.put("timestamp", currentDateAndTime);

        db.insert("notes", null, values);
    }

    private void updateNote(long id, String title, String content, String category) {
        android.database.sqlite.SQLiteDatabase db = dbHelper.getWritableDatabase();
        android.content.ContentValues values = new android.content.ContentValues();
        values.put("title", title);
        values.put("content", content);
        values.put("category", category);

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd, yyyy hh:mm a", java.util.Locale.getDefault());
        String currentDateAndTime = sdf.format(new java.util.Date());
        values.put("timestamp", currentDateAndTime);

        db.update("notes", values, "id = ?", new String[]{String.valueOf(id)});
    }

    private void deleteNote(long id) {
        android.database.sqlite.SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("notes", "id = ?", new String[]{String.valueOf(id)});
    }

    // Note data model structures
    public static class Note {
        public long id;
        public String title;
        public String content;
        public String category;
        public String timestamp;

        public Note(long id, String title, String content, String category, String timestamp) {
            this.id = id;
            this.title = title;
            this.content = content;
            this.category = category;
            this.timestamp = timestamp;
        }
    }

    // SQLiteOpenHelper implementation helper
    public static class DatabaseHelper extends android.database.sqlite.SQLiteOpenHelper {
        private static final String DATABASE_NAME = "notes.db";
        private static final int DATABASE_VERSION = 1;
        private static final String TABLE_NAME = "notes";
        private static final String COLUMN_ID = "id";
        private static final String COLUMN_TITLE = "title";
        private static final String COLUMN_CONTENT = "content";
        private static final String COLUMN_CATEGORY = "category";
        private static final String COLUMN_TIMESTAMP = "timestamp";

        public DatabaseHelper(android.content.Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(android.database.sqlite.SQLiteDatabase db) {
            String createTable = "CREATE TABLE " + TABLE_NAME + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_TITLE + " TEXT, " +
                    COLUMN_CONTENT + " TEXT, " +
                    COLUMN_CATEGORY + " TEXT, " +
                    COLUMN_TIMESTAMP + " TEXT)";
            db.execSQL(createTable);
        }

        @Override
        public void onUpgrade(android.database.sqlite.SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
            onCreate(db);
        }
    }

    // ListView Custom Adapter
    public class NoteAdapter extends android.widget.BaseAdapter {
        private android.content.Context context;
        private java.util.List<Note> notes;

        public NoteAdapter(android.content.Context context, java.util.List<Note> notes) {
            this.context = context;
            this.notes = notes;
        }

        public void setNotes(java.util.List<Note> notes) {
            this.notes = notes;
            notifyDataSetChanged();
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
        public View getView(final int position, View convertView, android.view.ViewGroup parent) {
            if (convertView == null) {
                convertView = android.view.LayoutInflater.from(context).inflate(
                        context.getResources().getIdentifier("note_item", "layout", context.getPackageName()),
                        parent,
                        false
                );
            }

            final Note note = notes.get(position);

            TextView tvTitle = (TextView) convertView.findViewById(
                    context.getResources().getIdentifier("note_item_title", "id", context.getPackageName())
            );
            TextView tvCategory = (TextView) convertView.findViewById(
                    context.getResources().getIdentifier("note_item_category", "id", context.getPackageName())
            );
            TextView tvSnippet = (TextView) convertView.findViewById(
                    context.getResources().getIdentifier("note_item_snippet", "id", context.getPackageName())
            );
            TextView tvDate = (TextView) convertView.findViewById(
                    context.getResources().getIdentifier("note_item_date", "id", context.getPackageName())
            );
            View btnDeleteWrapper = convertView.findViewById(
                    context.getResources().getIdentifier("btn_delete_wrapper", "id", context.getPackageName())
            );

            tvTitle.setText(note.title);
            tvCategory.setText(note.category);
            tvSnippet.setText(note.content);
            tvDate.setText(note.timestamp);

            if ("Work".equals(note.category)) {
                tvCategory.setTextColor(android.graphics.Color.parseColor("#E65100"));
            } else if ("Personal".equals(note.category)) {
                tvCategory.setTextColor(android.graphics.Color.parseColor("#0D47A1"));
            } else if ("Ideas".equals(note.category)) {
                tvCategory.setTextColor(android.graphics.Color.parseColor("#1B5E20"));
            } else if ("Todo".equals(note.category)) {
                tvCategory.setTextColor(android.graphics.Color.parseColor("#4A148C"));
            } else {
                tvCategory.setTextColor(android.graphics.Color.parseColor("#37474F"));
            }

            btnDeleteWrapper.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new android.app.AlertDialog.Builder(context)
                            .setTitle("Delete Note")
                            .setMessage("Are you sure you want to delete this note?")
                            .setPositiveButton("Yes", new android.content.DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(android.content.DialogInterface dialog, int which) {
                                    deleteNote(note.id);
                                    refreshNotesList();
                                }
                            })
                            .setNegativeButton("No", null)
                            .show();
                }
            });

            return convertView;
        }
    }
}
