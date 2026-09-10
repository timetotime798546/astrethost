package com.notesapp.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "notes_db";
    private static final int DATABASE_VERSION = 1;

    // Categories Table
    public static final String TABLE_CATEGORIES = "categories";
    public static final String COL_CAT_ID = "id";
    public static final String COL_CAT_NAME = "name";

    // Notes Table
    public static final String TABLE_NOTES = "notes";
    public static final String COL_NOTE_ID = "id";
    public static final String COL_NOTE_TITLE = "title";
    public static final String COL_NOTE_CONTENT = "content";
    public static final String COL_NOTE_CAT_ID = "category_id";
    public static final String COL_NOTE_TIMESTAMP = "timestamp";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_CATEGORIES + " (" +
                COL_CAT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_CAT_NAME + " TEXT UNIQUE)");

        db.execSQL("CREATE TABLE " + TABLE_NOTES + " (" +
                COL_NOTE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_NOTE_TITLE + " TEXT, " +
                COL_NOTE_CONTENT + " TEXT, " +
                COL_NOTE_CAT_ID + " INTEGER, " +
                COL_NOTE_TIMESTAMP + " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY(" + COL_NOTE_CAT_ID + ") REFERENCES " + TABLE_CATEGORIES + "(" + COL_CAT_ID + ") ON DELETE SET NULL)");

        // Insert some default categories
        db.execSQL("INSERT INTO " + TABLE_CATEGORIES + " (" + COL_CAT_NAME + ") VALUES ('General')");
        db.execSQL("INSERT INTO " + TABLE_CATEGORIES + " (" + COL_CAT_NAME + ") VALUES ('Work')");
        db.execSQL("INSERT INTO " + TABLE_CATEGORIES + " (" + COL_CAT_NAME + ") VALUES ('Personal')");
        db.execSQL("INSERT INTO " + TABLE_CATEGORIES + " (" + COL_CAT_NAME + ") VALUES ('Ideas')");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOTES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CATEGORIES);
        onCreate(db);
    }

    public List<Category> getAllCategories() {
        List<Category> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_CATEGORIES + " ORDER BY " + COL_CAT_NAME + " ASC", null);
        if (cursor.moveToFirst()) {
            do {
                list.add(new Category(
                    cursor.getLong(cursor.getColumnIndexOrThrow(COL_CAT_ID)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_CAT_NAME))
                ));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    public long addCategory(String name) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_CAT_NAME, name);
        try {
            return db.insertOrThrow(TABLE_CATEGORIES, null, values);
        } catch (Exception e) {
            return -1; // Duplicate or error
        }
    }

    public void deleteCategory(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_CATEGORIES, COL_CAT_ID + " = ?", new String[]{String.valueOf(id)});
    }

    public List<Note> getAllNotes(String searchQuery, long categoryIdFilter) {
        List<Note> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        StringBuilder query = new StringBuilder();
        query.append("SELECT n.*, c.").append(COL_CAT_NAME).append(" AS cat_name ");
        query.append("FROM ").append(TABLE_NOTES).append(" n ");
        query.append("LEFT JOIN ").append(TABLE_CATEGORIES).append(" c ");
        query.append("ON n.").append(COL_NOTE_CAT_ID).append(" = c.").append(COL_CAT_ID).append(" ");
        query.append("WHERE 1=1 ");

        List<String> args = new ArrayList<>();
        if (categoryIdFilter > 0) {
            query.append("AND n.").append(COL_NOTE_CAT_ID).append(" = ? ");
            args.add(String.valueOf(categoryIdFilter));
        }

        if (searchQuery != null && !searchQuery.trim().isEmpty()) {
            query.append("AND (n.").append(COL_NOTE_TITLE).append(" LIKE ? OR n.").append(COL_NOTE_CONTENT).append(" LIKE ?) ");
            args.add("%" + searchQuery.trim() + "%");
            args.add("%" + searchQuery.trim() + "%");
        }

        query.append("ORDER BY n.").append(COL_NOTE_TIMESTAMP).append(" DESC");

        Cursor cursor = db.rawQuery(query.toString(), args.toArray(new String[0]));
        if (cursor.moveToFirst()) {
            do {
                list.add(new Note(
                    cursor.getLong(cursor.getColumnIndexOrThrow(COL_NOTE_ID)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_NOTE_TITLE)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_NOTE_CONTENT)),
                    cursor.getLong(cursor.getColumnIndexOrThrow(COL_NOTE_CAT_ID)),
                    cursor.getString(cursor.getColumnIndexOrThrow("cat_name")),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_NOTE_TIMESTAMP))
                ));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    public long addNote(String title, String content, long categoryId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_NOTE_TITLE, title);
        values.put(COL_NOTE_CONTENT, content);
        if (categoryId > 0) {
            values.put(COL_NOTE_CAT_ID, categoryId);
        } else {
            values.putNull(COL_NOTE_CAT_ID);
        }
        return db.insert(TABLE_NOTES, null, values);
    }

    public int updateNote(long id, String title, String content, long categoryId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_NOTE_TITLE, title);
        values.put(COL_NOTE_CONTENT, content);
        if (categoryId > 0) {
            values.put(COL_NOTE_CAT_ID, categoryId);
        } else {
            values.putNull(COL_NOTE_CAT_ID);
        }
        // Force update timestamp to now for modified notes
        db.execSQL("UPDATE " + TABLE_NOTES + " SET " + COL_NOTE_TIMESTAMP + " = datetime('now','localtime') WHERE " + COL_NOTE_ID + " = " + id);
        return db.update(TABLE_NOTES, values, COL_NOTE_ID + " = ?", new String[]{String.valueOf(id)});
    }

    public void deleteNote(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NOTES, COL_NOTE_ID + " = ?", new String[]{String.valueOf(id)});
    }

    // Static structures representing objects
    public static class Category {
        public long id;
        public String name;

        public Category(long id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public static class Note {
        public long id;
        public String title;
        public String content;
        public long categoryId;
        public String categoryName;
        public String timestamp;

        public Note(long id, String title, String content, long categoryId, String categoryName, String timestamp) {
            this.id = id;
            this.title = title;
            this.content = content;
            this.categoryId = categoryId;
            this.categoryName = categoryName;
            this.timestamp = timestamp;
        }
    }
}