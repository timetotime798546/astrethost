package com.notekeep.app;

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
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.List;

public class MainActivity extends Activity {
    private NotesDbHelper dbHelper;
    private long currentEditingNoteId = -1;

    // Main layouts
    private LinearLayout layoutDashboard;
    private LinearLayout layoutEditor;

    // Dashboard UI controls
    private EditText etSearch;
    private Spinner spinnerFilterCategory;
    private Button btnManageCategories;
    private Button btnAddNote;
    private ListView listNotes;

    // Editor UI controls
    private EditText etNoteTitle;
    private Spinner spinnerNoteCategory;
    private EditText etNoteContent;
    private Button btnBack;
    private Button btnSave;
    private Button btnDeleteNote;

    @Override
    protected void onCreate(Bundle savedInstanceState) { 
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new NotesDbHelper(this);

        // Bind views
        layoutDashboard = (LinearLayout) findViewById(R.id.layout_dashboard);
        layoutEditor = (LinearLayout) findViewById(R.id.layout_editor);

        etSearch = (EditText) findViewById(R.id.et_search);
        spinnerFilterCategory = (Spinner) findViewById(R.id.spinner_filter_category);
        btnManageCategories = (Button) findViewById(R.id.btn_manage_categories);
        btnAddNote = (Button) findViewById(R.id.btn_add_note);
        listNotes = (ListView) findViewById(R.id.list_notes);

        etNoteTitle = (EditText) findViewById(R.id.et_note_title);
        spinnerNoteCategory = (Spinner) findViewById(R.id.spinner_note_category);
        etNoteContent = (EditText) findViewById(R.id.et_note_content);
        btnBack = (Button) findViewById(R.id.btn_back);
        btnSave = (Button) findViewById(R.id.btn_save);
        btnDeleteNote = (Button) findViewById(R.id.btn_delete_note);

        // Setup search inputs trigger
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

        // Refresh active categories list
        refreshSpinners();

        spinnerFilterCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                refreshNotesList();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Action bindings
        btnAddNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEditor(null);
            }
        });

        btnManageCategories.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showManageCategoriesDialog();
            }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDashboard();
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveNote();
            }
        });

        btnDeleteNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteNote();
            }
        });

        listNotes.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Note selectedNote = (Note) listNotes.getItemAtPosition(position);
                showEditor(selectedNote);
            }
        });

        showDashboard();
    }

    private void refreshSpinners() {
        List<Category> categoriesList = getCategories();

        // Filter categories list creation
        List<Category> filterCategories = new ArrayList<>();
        filterCategories.add(new Category(-1, "All Categories"));
        filterCategories.addAll(categoriesList);

        ArrayAdapter<Category> filterAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, filterCategories);
        filterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilterCategory.setAdapter(filterAdapter);

        // Editor categories list creation
        List<Category> editorCategories = new ArrayList<>();
        editorCategories.add(new Category(0, "Uncategorized"));
        editorCategories.addAll(categoriesList);

        ArrayAdapter<Category> editorAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, editorCategories);
        editorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerNoteCategory.setAdapter(editorAdapter);
    }

    private List<Category> getCategories() {
        List<Category> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id, name FROM categories ORDER BY name ASC", null);
        if (cursor.moveToFirst()) {
            do {
                list.add(new Category(cursor.getLong(0), cursor.getString(1)));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    private List<Note> getAllNotes(String searchQuery, long filterCategoryId) {
        List<Note> notes = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String selection = "";
        List<String> selectionArgs = new ArrayList<>();

        if (searchQuery != null && !searchQuery.trim().isEmpty()) {
            selection += "(notes.title LIKE ? OR notes.content LIKE ?)";
            selectionArgs.add("%" + searchQuery + "%");
            selectionArgs.add("%" + searchQuery + "%");
        }

        if (filterCategoryId > 0) {
            if (!selection.isEmpty()) {
                selection += " AND ";
            }
            selection += "notes.category_id = ?";
            selectionArgs.add(String.valueOf(filterCategoryId));
        }

        String query = "SELECT notes.id, notes.title, notes.content, notes.category_id, notes.timestamp, categories.name " +
                "FROM notes LEFT JOIN categories ON notes.category_id = categories.id";

        if (!selection.isEmpty()) {
            query += " WHERE " + selection;
        }
        query += " ORDER BY notes.id DESC";

        Cursor cursor = db.rawQuery(query, selectionArgs.toArray(new String[0]));
        if (cursor.moveToFirst()) {
            do {
                long id = cursor.getLong(0);
                String title = cursor.getString(1);
                String content = cursor.getString(2);
                long catId = cursor.getLong(3);
                String timestamp = cursor.getString(4);
                String catName = cursor.getString(5);

                notes.add(new Note(id, title, content, catId, catName, timestamp));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return notes;
    }

    private void refreshNotesList() {
        String query = etSearch.getText().toString();
        Category selectedCat = (Category) spinnerFilterCategory.getSelectedItem();
        long catId = -1;
        if (selectedCat != null) {
            catId = selectedCat.getId();
        }

        List<Note> notes = getAllNotes(query, catId);
        NoteListAdapter adapter = new NoteListAdapter(this, notes);
        listNotes.setAdapter(adapter);
    }

    private void showDashboard() {
        layoutDashboard.setVisibility(View.VISIBLE);
        layoutEditor.setVisibility(View.GONE);
        currentEditingNoteId = -1;
        refreshNotesList();
    }

    private void showEditor(Note note) {
        layoutDashboard.setVisibility(View.GONE);
        layoutEditor.setVisibility(View.VISIBLE);

        if (note == null) {
            currentEditingNoteId = -1;
            etNoteTitle.setText("");
            etNoteContent.setText("");
            btnDeleteNote.setVisibility(View.GONE);
            if (spinnerNoteCategory.getCount() > 0) {
                spinnerNoteCategory.setSelection(0);
            }
        } else {
            currentEditingNoteId = note.getId();
            etNoteTitle.setText(note.getTitle());
            etNoteContent.setText(note.getContent());
            btnDeleteNote.setVisibility(View.VISIBLE);

            long noteCatId = note.getCategoryId();
            int selectionIndex = 0;
            for (int i = 0; i < spinnerNoteCategory.getCount(); i++) {
                Category cat = (Category) spinnerNoteCategory.getItemAtPosition(i);
                if (cat.getId() == noteCatId) {
                    selectionIndex = i;
                    break;
                }
            }
            spinnerNoteCategory.setSelection(selectionIndex);
        }
    }

    private void saveNote() {
        String title = etNoteTitle.getText().toString().trim();
        String content = etNoteContent.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(this, "Please enter a title", Toast.LENGTH_SHORT).show();
            return;
        }

        Category selectedCategory = (Category) spinnerNoteCategory.getSelectedItem();
        long catId = 0;
        if (selectedCategory != null) {
            catId = selectedCategory.getId();
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("title", title);
        values.put("content", content);
        if (catId <= 0) {
            values.putNull("category_id");
        } else {
            values.put("category_id", catId);
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        String currentDate = sdf.format(new Date());
        values.put("timestamp", currentDate);

        if (currentEditingNoteId == -1) {
            db.insert("notes", null, values);
            Toast.makeText(this, "Note saved", Toast.LENGTH_SHORT).show();
        } else {
            db.update("notes", values, "id = ?", new String[]{String.valueOf(currentEditingNoteId)});
            Toast.makeText(this, "Note updated", Toast.LENGTH_SHORT).show();
        }

        showDashboard();
    }

    private void deleteNote() {
        if (currentEditingNoteId == -1) {
            showDashboard();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Delete Note")
                .setMessage("Are you sure you want to delete this note?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SQLiteDatabase db = dbHelper.getWritableDatabase();
                        db.delete("notes", "id = ?", new String[]{String.valueOf(currentEditingNoteId)});
                        Toast.makeText(MainActivity.this, "Note deleted", Toast.LENGTH_SHORT).show();
                        showDashboard();
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void showManageCategoriesDialog() {
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_manage_categories, null);

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        final EditText etNewCategory = (EditText) dialogView.findViewById(R.id.et_new_category);
        Button btnAddCategory = (Button) dialogView.findViewById(R.id.btn_add_category);
        final ListView listCategoriesManage = (ListView) dialogView.findViewById(R.id.list_categories_manage);
        Button btnClose = (Button) dialogView.findViewById(R.id.btn_close_categories);

        final List<Category> categoriesList = getCategories();
        final CategoryAdapter adapter = new CategoryAdapter(this, categoriesList);
        listCategoriesManage.setAdapter(adapter);

        btnAddCategory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String catName = etNewCategory.getText().toString().trim();
                if (catName.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Category name cannot be empty", Toast.LENGTH_SHORT).show();
                    return;
                }

                SQLiteDatabase db = dbHelper.getWritableDatabase();
                ContentValues values = new ContentValues();
                values.put("name", catName);
                long id = db.insertWithOnConflict("categories", null, values, SQLiteDatabase.CONFLICT_IGNORE);
                if (id == -1) {
                    Toast.makeText(MainActivity.this, "Category already exists", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Category added", Toast.LENGTH_SHORT).show();
                    etNewCategory.setText("");
                    categoriesList.clear();
                    categoriesList.addAll(getCategories());
                    adapter.notifyDataSetChanged();
                    refreshSpinners();
                }
            }
        });

        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private class CategoryAdapter extends BaseAdapter {
        private Context context;
        private List<Category> list;

        public CategoryAdapter(Context context, List<Category> list) {
            this.context = context;
            this.list = list;
        }

        @Override
        public int getCount() { return list.size(); }
        @Override
        public Object getItem(int position) { return list.get(position); }
        @Override
        public long getItemId(int position) { return list.get(position).getId(); }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.item_category_manage, parent, false);
            }

            TextView tvName = (TextView) convertView.findViewById(R.id.tv_category_name);
            Button btnDelete = (Button) convertView.findViewById(R.id.btn_delete_category);

            final Category cat = list.get(position);
            tvName.setText(cat.getName());

            btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Delete Category")
                            .setMessage("Are you sure you want to delete this category? Notes in this category will become uncategorized.")
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                                    db.delete("categories", "id = ?", new String[]{String.valueOf(cat.getId())});
                                    Toast.makeText(MainActivity.this, "Category deleted", Toast.LENGTH_SHORT).show();

                                    list.remove(position);
                                    notifyDataSetChanged();
                                    refreshSpinners();
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