package com.smartnotes.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.HashMap;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "smart_notes.db";
    private static final int DATABASE_VERSION = 1;

    // Table names
    public static final String TABLE_NOTES = "notes";
    public static final String TABLE_CATEGORIES = "categories";

    // Notes table columns
    public static final String COLUMN_NOTE_ID = "id";
    public static final String COLUMN_NOTE_TITLE = "title";
    public static final String COLUMN_NOTE_CONTENT = "content";
    public static final String COLUMN_NOTE_CATEGORY_ID = "category_id";
    public static final String COLUMN_NOTE_TIMESTAMP = "timestamp";

    // Categories table columns
    public static final String COLUMN_CAT_ID = "id";
    public static final String COLUMN_CAT_NAME = "name";

    // Database Creation SQL commands
    private static final String CREATE_TABLE_CATEGORIES = "CREATE TABLE " + TABLE_CATEGORIES + "("
            + COLUMN_CAT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COLUMN_CAT_NAME + " TEXT UNIQUE NOT NULL" + ");";

    private static final String CREATE_TABLE_NOTES = "CREATE TABLE " + TABLE_NOTES + "("
            + COLUMN_NOTE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COLUMN_NOTE_TITLE + " TEXT NOT NULL, "
            + COLUMN_NOTE_CONTENT + " TEXT, "
            + COLUMN_NOTE_CATEGORY_ID + " INTEGER, "
            + COLUMN_NOTE_TIMESTAMP + " DATETIME DEFAULT CURRENT_TIMESTAMP, "
            + "FOREIGN KEY(" + COLUMN_NOTE_CATEGORY_ID + ") REFERENCES " + TABLE_CATEGORIES + "(" + COLUMN_CAT_ID + ")" + ");";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_CATEGORIES);
        db.execSQL(CREATE_TABLE_NOTES);

        // Prepopulate default category entries
        String[] defaults = {"General", "Personal", "Work", "Ideas"};
        for (int i = 0; i < defaults.length; i++) {
            ContentValues values = new ContentValues();
            values.put(COLUMN_CAT_NAME, defaults[i]);
            db.insert(TABLE_CATEGORIES, null, values);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOTES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CATEGORIES);
        onCreate(db);
    }

    // --- CATEGORY CRUD OPERATIONS ---

    public long insertCategory(String name) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_CAT_NAME, name);
        long id = db.insert(TABLE_CATEGORIES, null, values);
        db.close();
        return id;
    }

    public ArrayList<HashMap<String, String>> getAllCategories() {
        ArrayList<HashMap<String, String>> list = new ArrayList<HashMap<String, String>>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_CATEGORIES + " ORDER BY " + COLUMN_CAT_NAME + " ASC", null);

        if (cursor.moveToFirst()) {
            do {
                HashMap<String, String> map = new HashMap<String, String>();
                map.put("id", String.valueOf(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_CAT_ID))));
                map.put("name", cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CAT_NAME)));
                list.add(map);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }

    // --- NOTES CRUD OPERATIONS ---

    public long insertNote(String title, String content, long categoryId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NOTE_TITLE, title);
        values.put(COLUMN_NOTE_CONTENT, content);
        values.put(COLUMN_NOTE_CATEGORY_ID, categoryId);
        // Let SQL automatically populate the timestamp
        long id = db.insert(TABLE_NOTES, null, values);
        db.close();
        return id;
    }

    public int updateNote(long id, String title, String content, long categoryId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NOTE_TITLE, title);
        values.put(COLUMN_NOTE_CONTENT, content);
        values.put(COLUMN_NOTE_CATEGORY_ID, categoryId);
        // Refresh timestamp to modification date
        values.put(COLUMN_NOTE_TIMESTAMP, java.text.DateFormat.getDateTimeInstance().format(new java.util.Date()));
        
        int affectedRows = db.update(TABLE_NOTES, values, COLUMN_NOTE_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
        return affectedRows;
    }

    public void deleteNote(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NOTES, COLUMN_NOTE_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    public ArrayList<HashMap<String, String>> getFilteredNotes(String queryText, long filterCategoryId) {
        ArrayList<HashMap<String, String>> list = new ArrayList<HashMap<String, String>>();
        SQLiteDatabase db = this.getReadableDatabase();

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT n.*, c.").append(COLUMN_CAT_NAME).append(" AS category_name ");
        sql.append("FROM ").append(TABLE_NOTES).append(" n ");
        sql.append("LEFT JOIN ").append(TABLE_CATEGORIES).append(" c ");
        sql.append("ON n.").append(COLUMN_NOTE_CATEGORY_ID).append(" = c.").append(COLUMN_CAT_ID);

        ArrayList<String> selectionArgs = new ArrayList<String>();
        boolean hasFilter = false;

        if (filterCategoryId > 0) {
            sql.append(" WHERE n.").append(COLUMN_NOTE_CATEGORY_ID).append(" = ?");
            selectionArgs.add(String.valueOf(filterCategoryId));
            hasFilter = true;
        }

        if (queryText != null && !queryText.trim().isEmpty()) {
            if (hasFilter) {
                sql.append(" AND ");
            } else {
                sql.append(" WHERE ");
            }
            sql.append("(n.").append(COLUMN_NOTE_TITLE).append(" LIKE ? OR n.").append(COLUMN_NOTE_CONTENT).append(" LIKE ?)");
            selectionArgs.add("%" + queryText.trim() + "%");
            selectionArgs.add("%" + queryText.trim() + "%");
        }

        sql.append(" ORDER BY n.").append(COLUMN_NOTE_ID).append(" DESC");

        String[] args = new String[selectionArgs.size()];
        args = selectionArgs.toArray(args);

        Cursor cursor = db.rawQuery(sql.toString(), args);

        if (cursor.moveToFirst()) {
            do {
                HashMap<String, String> map = new HashMap<String, String>();
                map.put("id", String.valueOf(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_NOTE_ID))));
                map.put("title", cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOTE_TITLE)));
                map.put("content", cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOTE_CONTENT)));
                map.put("category_id", String.valueOf(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_NOTE_CATEGORY_ID))));
                map.put("category_name", cursor.getString(cursor.getColumnIndexOrThrow("category_name")));
                map.put("timestamp", cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOTE_TIMESTAMP)));
                list.add(map);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }
}