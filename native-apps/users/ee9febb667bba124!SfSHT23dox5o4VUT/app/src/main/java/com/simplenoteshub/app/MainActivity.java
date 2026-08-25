package com.simplenoteshub.app;

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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import com.simplenoteshub.app.R;

public class MainActivity extends Activity {

    public static class Note {
        public long id;
        public String title;
        public String content;
        public String category;
        public long createdAt;

        public Note(long id, String title, String content, String category, long createdAt) {
            this.id = id;
            this.title = title;
            this.content = content;
            this.category = category;
            this.createdAt = createdAt;
        }
    }

    public static class DatabaseHelper extends SQLiteOpenHelper {
        private static final String DATABASE_NAME = "notes_hub.db";
        private static final int DATABASE_VERSION = 1;

        public DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE notes (" +
                    "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "title TEXT," +
                    "content TEXT," +
                    "category TEXT," +
                    "created_at LONG" +
                    ");");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS notes");
            onCreate(db);
        }
    }

    private static class ViewHolder {
        TextView tvTitle;
        TextView tvTag;
        TextView tvSnippet;
        TextView tvDate;
    }

    private class NotesAdapter extends BaseAdapter {
        private List<Note> mNotes;

        public NotesAdapter(List<Note> notes) {
            this.mNotes = notes;
        }

        public void setNotes(List<Note> notes) {
            this.mNotes = notes;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mNotes.size();
        }

        @Override
        public Object getItem(int position) {
            return mNotes.get(position);
        }

        @Override
        public long getItemId(int position) {
            return mNotes.get(position).id;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                LinearLayout layout = new LinearLayout(MainActivity.this);
                layout.setOrientation(LinearLayout.VERTICAL);
                layout.setPadding(24, 24, 24, 24);

                LinearLayout topRow = new LinearLayout(MainActivity.this);
                topRow.setOrientation(LinearLayout.HORIZONTAL);

                TextView tvTitle = new TextView(MainActivity.this);
                tvTitle.setTextSize(16);
                tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
                tvTitle.setTextColor(0xFF212121);
                LinearLayout.LayoutParams lpTitle = new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
                topRow.addView(tvTitle, lpTitle);

                TextView tvTag = new TextView(MainActivity.this);
                tvTag.setTextSize(10);
                tvTag.setPadding(12, 6, 12, 6);
                tvTag.setTextColor(0xFFFFFFFF);
                LinearLayout.LayoutParams lpTag = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                lpTag.setMargins(12, 0, 0, 0);
                topRow.addView(tvTag, lpTag);

                layout.addView(topRow);

                TextView tvSnippet = new TextView(MainActivity.this);
                tvSnippet.setTextSize(14);
                tvSnippet.setTextColor(0xFF757575);
                tvSnippet.setSingleLine(true);
                tvSnippet.setPadding(0, 12, 0, 4);
                layout.addView(tvSnippet);

                TextView tvDate = new TextView(MainActivity.this);
                tvDate.setTextSize(11);
                tvDate.setTextColor(0xFF9E9E9E);
                layout.addView(tvDate);

                holder = new ViewHolder();
                holder.tvTitle = tvTitle;
                holder.tvTag = tvTag;
                holder.tvSnippet = tvSnippet;
                holder.tvDate = tvDate;

                convertView = layout;
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            Note note = mNotes.get(position);
            holder.tvTitle.setText(note.title);
            holder.tvTag.setText(note.category);

            if ("Work".equals(note.category)) {
                holder.tvTag.setBackgroundColor(0xFFE91E63);
            } else if ("Personal".equals(note.category)) {
                holder.tvTag.setBackgroundColor(0xFF4CAF50);
            } else if ("Ideas".equals(note.category)) {
                holder.tvTag.setBackgroundColor(0xFFFF9800);
            } else if ("Shopping".equals(note.category)) {
                holder.tvTag.setBackgroundColor(0xFF9C27B0);
            } else {
                holder.tvTag.setBackgroundColor(0xFF607D8B);
            }

            String snippet = note.content;
            if (snippet.length() > 60) {
                snippet = snippet.substring(0, 57) + "...";
            }
            holder.tvSnippet.setText(snippet);

            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
            holder.tvDate.setText(sdf.format(new Date(note.createdAt)));

            return convertView;
        }
    }

    private LinearLayout mLayoutList;
    private LinearLayout mLayoutEdit;

    private EditText mEtSearch;
    private Spinner mSpinnerFilterCategory;
    private ListView mLvNotes;
    private TextView mTvEmpty;
    private Button mBtnAddNote;

    private TextView mTvEditorTitle;
    private Button mBtnDelete;
    private Button mBtnSave;
    private Button mBtnCancel;
    private Spinner mSpinnerEditCategory;
    private EditText mEtTitle;
    private EditText mEtContent;

    private DatabaseHelper mDbHelper;
    private List<Note> mAllNotes = new ArrayList<Note>();
    private List<Note> mFilteredNotes = new ArrayList<Note>();
    private NotesAdapter mAdapter;

    private Note mCurrentEditingNote = null;

    private final String[] mFilterCategories = new String[]{"All Categories", "Work", "Personal", "Ideas", "Shopping", "Others"};
    private final String[] mEditCategories = new String[]{"Work", "Personal", "Ideas", "Shopping", "Others"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDbHelper = new DatabaseHelper(this);

        mLayoutList = (LinearLayout) findViewById(R.id.layout_list);
        mLayoutEdit = (LinearLayout) findViewById(R.id.layout_edit);

        mEtSearch = (EditText) findViewById(R.id.et_search);
        mSpinnerFilterCategory = (Spinner) findViewById(R.id.spinner_filter_category);
        mLvNotes = (ListView) findViewById(R.id.lv_notes);
        mTvEmpty = (TextView) findViewById(R.id.tv_empty);
        mBtnAddNote = (Button) findViewById(R.id.btn_add_note);

        mTvEditorTitle = (TextView) findViewById(R.id.tv_editor_title);
        mBtnDelete = (Button) findViewById(R.id.btn_delete);
        mBtnSave = (Button) findViewById(R.id.btn_save);
        mBtnCancel = (Button) findViewById(R.id.btn_cancel);
        mSpinnerEditCategory = (Spinner) findViewById(R.id.spinner_edit_category);
        mEtTitle = (EditText) findViewById(R.id.et_title);
        mEtContent = (EditText) findViewById(R.id.et_content);

        ArrayAdapter<String> filterAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, mFilterCategories);
        filterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerFilterCategory.setAdapter(filterAdapter);

        ArrayAdapter<String> editAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, mEditCategories);
        editAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerEditCategory.setAdapter(editAdapter);

        mAdapter = new NotesAdapter(mFilteredNotes);
        mLvNotes.setAdapter(mAdapter);

        mBtnAddNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openEditor(null);
            }
        });

        mBtnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeEditor();
            }
        });

        mBtnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveNote();
            }
        });

        mBtnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmDelete();
            }
        });

        mLvNotes.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                openEditor(mFilteredNotes.get(position));
            }
        });

        mEtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterNotes();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        mSpinnerFilterCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filterNotes();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        loadNotes();
        filterNotes();
    }

    private void loadNotes() {
        mAllNotes.clear();
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query("notes", null, null, null, null, null, "created_at DESC");
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    long id = cursor.getLong(cursor.getColumnIndexOrThrow("_id"));
                    String title = cursor.getString(cursor.getColumnIndexOrThrow("title"));
                    String content = cursor.getString(cursor.getColumnIndexOrThrow("content"));
                    String category = cursor.getString(cursor.getColumnIndexOrThrow("category"));
                    long createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at"));
                    mAllNotes.add(new Note(id, title, content, category, createdAt));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    private void filterNotes() {
        mFilteredNotes.clear();
        String query = mEtSearch.getText().toString().toLowerCase(Locale.getDefault()).trim();
        String selectedCategory = mSpinnerFilterCategory.getSelectedItem().toString();

        for (int i = 0; i < mAllNotes.size(); i++) {
            Note note = mAllNotes.get(i);
            boolean categoryMatches = "All Categories".equals(selectedCategory) || note.category.equals(selectedCategory);
            boolean queryMatches = note.title.toLowerCase(Locale.getDefault()).contains(query) ||
                    note.content.toLowerCase(Locale.getDefault()).contains(query);

            if (categoryMatches && queryMatches) {
                mFilteredNotes.add(note);
            }
        }

        mAdapter.setNotes(mFilteredNotes);

        if (mFilteredNotes.isEmpty()) {
            mTvEmpty.setVisibility(View.VISIBLE);
        } else {
            mTvEmpty.setVisibility(View.GONE);
        }
    }

    private void openEditor(Note note) {
        mCurrentEditingNote = note;
        if (note == null) {
            mEtTitle.setText("");
            mEtContent.setText("");
            mSpinnerEditCategory.setSelection(0);
            mBtnDelete.setVisibility(View.GONE);
            mTvEditorTitle.setText("Create Note");
        } else {
            mEtTitle.setText(note.title);
            mEtContent.setText(note.content);
            mBtnDelete.setVisibility(View.VISIBLE);
            mTvEditorTitle.setText("Edit Note");

            for (int i = 0; i < mEditCategories.length; i++) {
                if (mEditCategories[i].equals(note.category)) {
                    mSpinnerEditCategory.setSelection(i);
                    break;
                }
            }
        }
        mLayoutList.setVisibility(View.GONE);
        mLayoutEdit.setVisibility(View.VISIBLE);
    }

    private void closeEditor() {
        hideKeyboard();
        mLayoutEdit.setVisibility(View.GONE);
        mLayoutList.setVisibility(View.VISIBLE);
        mCurrentEditingNote = null;
    }

    private void saveNote() {
        String title = mEtTitle.getText().toString().trim();
        String content = mEtContent.getText().toString().trim();
        String category = mSpinnerEditCategory.getSelectedItem().toString();

        if (title.isEmpty()) {
            Toast.makeText(this, "Title cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("title", title);
        values.put("content", content);
        values.put("category", category);

        if (mCurrentEditingNote == null) {
            values.put("created_at", System.currentTimeMillis());
            db.insert("notes", null, values);
            Toast.makeText(this, "Note created successfully", Toast.LENGTH_SHORT).show();
        } else {
            db.update("notes", values, "_id = ?", new String[]{String.valueOf(mCurrentEditingNote.id)});
            Toast.makeText(this, "Note updated successfully", Toast.LENGTH_SHORT).show();
        }

        loadNotes();
        filterNotes();
        closeEditor();
    }

    private void confirmDelete() {
        if (mCurrentEditingNote == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Note");
        builder.setMessage("Are you sure you want to delete this note?");
        builder.setPositiveButton("Yes, Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                deleteNote();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void deleteNote() {
        if (mCurrentEditingNote == null) return;

        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        db.delete("notes", "_id = ?", new String[]{String.valueOf(mCurrentEditingNote.id)});
        Toast.makeText(this, "Note deleted", Toast.LENGTH_SHORT).show();

        loadNotes();
        filterNotes();
        closeEditor();
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            View view = getCurrentFocus();
            if (view != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (mLayoutEdit.getVisibility() == View.VISIBLE) {
            closeEditor();
        } else {
            super.onBackPressed();
        }
    }
}