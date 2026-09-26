package com.notesmanager.app;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.ContentValues;
import android.database.Cursor;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "notes_db";
    private static final int DATABASE_VERSION = 1;

    // Table Names
    private static final String TABLE_NOTES = "notes";
    private static final String TABLE_CATEGORIES = "categories";

    // Notes Columns
    private static final String KEY_NOTE_ID = "id";
    private static final String KEY_NOTE_TITLE = "title";
    private static final String KEY_NOTE_CONTENT = "content";
    private static final String KEY_NOTE_CATEGORY = "category";
    private static final String KEY_NOTE_TIMESTAMP = "timestamp";

    // Categories Columns
    private static final String KEY_CAT_ID = "id";
    private static final String KEY_CAT_NAME = "name";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_CATEGORIES_TABLE = "CREATE TABLE " + TABLE_CATEGORIES + "("
                + KEY_CAT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_CAT_NAME + " TEXT UNIQUE" + ")";

        String CREATE_NOTES_TABLE = "CREATE TABLE " + TABLE_NOTES + "("
                + KEY_NOTE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_NOTE_TITLE + " TEXT,"
                + KEY_NOTE_CONTENT + " TEXT,"
                + KEY_NOTE_CATEGORY + " TEXT,"
                + KEY_NOTE_TIMESTAMP + " TEXT" + ")";

        db.execSQL(CREATE_CATEGORIES_TABLE);
        db.execSQL(CREATE_NOTES_TABLE);

        // Prepopulate standard categories
        db.execSQL("INSERT INTO " + TABLE_CATEGORIES + "(" + KEY_CAT_NAME + ") VALUES ('Work')");
        db.execSQL("INSERT INTO " + TABLE_CATEGORIES + "(" + KEY_CAT_NAME + ") VALUES ('Personal')");
        db.execSQL("INSERT INTO " + TABLE_CATEGORIES + "(" + KEY_CAT_NAME + ") VALUES ('Ideas')");
        db.execSQL("INSERT INTO " + TABLE_CATEGORIES + "(" + KEY_CAT_NAME + ") VALUES ('Todo')");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOTES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CATEGORIES);
        onCreate(db);
    }

    // Add Category
    public boolean addCategory(String name) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_CAT_NAME, name);
        long id = db.insert(TABLE_CATEGORIES, null, values);
        return id != -1;
    }

    // Get All Categories
    public List<String> getAllCategories() {
        List<String> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_CATEGORIES, null);
        if (cursor.moveToFirst()) {
            do {
                int index = cursor.getColumnIndex(KEY_CAT_NAME);
                if (index >= 0) {
                    list.add(cursor.getString(index));
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    // Add Note
    public void addNote(String title, String content, String category, String timestamp) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_NOTE_TITLE, title);
        values.put(KEY_NOTE_CONTENT, content);
        values.put(KEY_NOTE_CATEGORY, category);
        values.put(KEY_NOTE_TIMESTAMP, timestamp);
        db.insert(TABLE_NOTES, null, values);
    }

    // Update Note
    public void updateNote(int id, String title, String content, String category, String timestamp) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_NOTE_TITLE, title);
        values.put(KEY_NOTE_CONTENT, content);
        values.put(KEY_NOTE_CATEGORY, category);
        values.put(KEY_NOTE_TIMESTAMP, timestamp);
        db.update(TABLE_NOTES, values, KEY_NOTE_ID + " = ?", new String[]{String.valueOf(id)});
    }

    // Delete Note
    public void deleteNote(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NOTES, KEY_NOTE_ID + " = ?", new String[]{String.valueOf(id)});
    }

    // Get All Notes or Filtered
    public List<Note> getNotes(String searchQuery, String categoryFilter) {
        List<Note> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        StringBuilder query = new StringBuilder("SELECT * FROM " + TABLE_NOTES + " WHERE 1=1");
        List<String> args = new ArrayList<>();

        if (categoryFilter != null && !categoryFilter.isEmpty() && !categoryFilter.equals("All")) {
            query.append(" AND ").append(KEY_NOTE_CATEGORY).append(" = ?");
            args.add(categoryFilter);
        }

        if (searchQuery != null && !searchQuery.isEmpty()) {
            query.append(" AND (").append(KEY_NOTE_TITLE).append(" LIKE ? OR ").append(KEY_NOTE_CONTENT).append(" LIKE ?)");
            args.add("%" + searchQuery + "%");
            args.add("%" + searchQuery + "%");
        }

        query.append(" ORDER BY ").append(KEY_NOTE_ID).append(" DESC");

        Cursor cursor = db.rawQuery(query.toString(), args.toArray(new String[0]));
        if (cursor.moveToFirst()) {
            do {
                int idIdx = cursor.getColumnIndex(KEY_NOTE_ID);
                int titleIdx = cursor.getColumnIndex(KEY_NOTE_TITLE);
                int contentIdx = cursor.getColumnIndex(KEY_NOTE_CONTENT);
                int catIdx = cursor.getColumnIndex(KEY_NOTE_CATEGORY);
                int timeIdx = cursor.getColumnIndex(KEY_NOTE_TIMESTAMP);

                int id = idIdx >= 0 ? cursor.getInt(idIdx) : 0;
                String title = titleIdx >= 0 ? cursor.getString(titleIdx) : "";
                String content = contentIdx >= 0 ? cursor.getString(contentIdx) : "";
                String category = catIdx >= 0 ? cursor.getString(catIdx) : "";
                String timestamp = timeIdx >= 0 ? cursor.getString(timeIdx) : "";

                list.add(new Note(id, title, content, category, timestamp));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }
}