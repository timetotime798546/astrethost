package com.smartnotes.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "notes.db";
    private static final int DATABASE_VERSION = 1;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE categories (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT UNIQUE)");
        db.execSQL("CREATE TABLE notes (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT, content TEXT, category TEXT, timestamp INTEGER)");
        
        // Insert default categories
        db.execSQL("INSERT INTO categories (name) VALUES ('All')");
        db.execSQL("INSERT INTO categories (name) VALUES ('Personal')");
        db.execSQL("INSERT INTO categories (name) VALUES ('Work')");
        db.execSQL("INSERT INTO categories (name) VALUES ('Ideas')");
        db.execSQL("INSERT INTO categories (name) VALUES ('Todo')");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS notes");
        db.execSQL("DROP TABLE IF EXISTS categories");
        onCreate(db);
    }

    // Category CRUD
    public void addCategory(String name) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        db.insertWithOnConflict("categories", null, values, SQLiteDatabase.CONFLICT_IGNORE);
    }

    public List<String> getCategories() {
        List<String> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT name FROM categories ORDER BY id ASC", null);
        if (cursor.moveToFirst()) {
            do {
                list.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    // Note CRUD
    public long insertNote(String title, String content, String category, long timestamp) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("title", title);
        values.put("content", content);
        values.put("category", category);
        values.put("timestamp", timestamp);
        return db.insert("notes", null, values);
    }

    public int updateNote(long id, String title, String content, String category, long timestamp) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("title", title);
        values.put("content", content);
        values.put("category", category);
        values.put("timestamp", timestamp);
        return db.update("notes", values, "id = ?", new String[]{String.valueOf(id)});
    }

    public void deleteNote(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("notes", "id = ?", new String[]{String.valueOf(id)});
    }

    public List<Note> getAllNotes(String categoryFilter, String searchQuery) {
        List<Note> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        StringBuilder query = new StringBuilder("SELECT * FROM notes WHERE 1=1");
        List<String> args = new ArrayList<>();

        if (categoryFilter != null && !categoryFilter.equals("All") && !categoryFilter.isEmpty()) {
            query.append(" AND category = ?");
            args.add(categoryFilter);
        }

        if (searchQuery != null && !searchQuery.isEmpty()) {
            query.append(" AND (title LIKE ? OR content LIKE ?)");
            args.add("%" + searchQuery + "%");
            args.add("%" + searchQuery + "%");
        }

        query.append(" ORDER BY timestamp DESC");

        Cursor cursor = db.rawQuery(query.toString(), args.toArray(new String[0]));
        if (cursor.moveToFirst()) {
            do {
                Note note = new Note();
                note.setId(cursor.getLong(cursor.getColumnIndexOrThrow("id")));
                note.setTitle(cursor.getString(cursor.getColumnIndexOrThrow("title")));
                note.setContent(cursor.getString(cursor.getColumnIndexOrThrow("content")));
                note.setCategory(cursor.getString(cursor.getColumnIndexOrThrow("category")));
                note.setTimestamp(cursor.getLong(cursor.getColumnIndexOrThrow("timestamp")));
                list.add(note);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }
}