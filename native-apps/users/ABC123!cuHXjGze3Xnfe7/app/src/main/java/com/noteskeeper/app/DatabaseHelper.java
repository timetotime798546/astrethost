package com.noteskeeper.app;

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
        db.execSQL("CREATE TABLE notes (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT, content TEXT, category TEXT, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        
        // Populate standard default categories
        db.execSQL("INSERT INTO categories (name) VALUES ('Personal')");
        db.execSQL("INSERT INTO categories (name) VALUES ('Work')");
        db.execSQL("INSERT INTO categories (name) VALUES ('Ideas')");
        db.execSQL("INSERT INTO categories (name) VALUES ('Todos')");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS notes");
        db.execSQL("DROP TABLE IF EXISTS categories");
        onCreate(db);
    }

    // Category DB APIs
    public long addCategory(String name) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        long id = -1;
        try {
            id = db.insert("categories", null, values);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.close();
        }
        return id;
    }

    public List<String> getAllCategories() {
        List<String> categories = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT name FROM categories ORDER BY name ASC", null);
        if (cursor.moveToFirst()) {
            do {
                categories.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return categories;
    }

    // Notes DB APIs
    public long addNote(String title, String content, String category) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("title", title);
        values.put("content", content);
        values.put("category", category);
        long id = db.insert("notes", null, values);
        db.close();
        return id;
    }

    public int updateNote(long id, String title, String content, String category) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("title", title);
        values.put("content", content);
        values.put("category", category);
        int rows = db.update("notes", values, "id = ?", new String[]{String.valueOf(id)});
        db.close();
        return rows;
    }

    public void deleteNote(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("notes", "id = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    public List<Note> getFilteredNotes(String categoryFilter, String searchQuery) {
        List<Note> notes = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        StringBuilder query = new StringBuilder("SELECT id, title, content, category, datetime(created_at, 'localtime') FROM notes WHERE 1=1");
        List<String> args = new ArrayList<>();

        if (categoryFilter != null && !categoryFilter.equals("All Categories")) {
            query.append(" AND category = ?");
            args.add(categoryFilter);
        }

        if (searchQuery != null && !searchQuery.trim().isEmpty()) {
            query.append(" AND (title LIKE ? OR content LIKE ?)");
            args.add("%" + searchQuery + "%");
            args.add("%" + searchQuery + "%");
        }

        query.append(" ORDER BY id DESC");

        String[] selectionArgs = args.toArray(new String[0]);
        Cursor cursor = db.rawQuery(query.toString(), selectionArgs);
        if (cursor.moveToFirst()) {
            do {
                notes.add(new Note(
                    cursor.getLong(0),
                    cursor.getString(1),
                    cursor.getString(2),
                    cursor.getString(3),
                    cursor.getString(4)
                ));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return notes;
    }
}