package com.quicknotesmanager.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "quicknotes.db";
    private static final int DATABASE_VERSION = 1;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE categories (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT UNIQUE NOT NULL)");

        db.execSQL("CREATE TABLE notes (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "title TEXT, " +
                "content TEXT, " +
                "category_id INTEGER, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY(category_id) REFERENCES categories(id) ON DELETE SET NULL)");

        // Populate system default categories
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
    public long insertCategory(String name) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        try {
            return db.insert("categories", null, values);
        } catch (Exception e) {
            return -1;
        }
    }

    public void deleteCategory(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("categories", "id = ?", new String[]{String.valueOf(id)});
    }

    public List<Category> getAllCategories() {
        List<Category> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id, name FROM categories ORDER BY name ASC", null);
        if (cursor.moveToFirst()) {
            do {
                list.add(new Category(cursor.getLong(0), cursor.getString(1)));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    // Notes CRUD
    public long insertNote(String title, String content, long categoryId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("title", title);
        values.put("content", content);
        if (categoryId > 0) {
            values.put("category_id", categoryId);
        } else {
            values.putNull("category_id");
        }
        return db.insert("notes", null, values);
    }

    public int updateNote(long id, String title, String content, long categoryId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("title", title);
        values.put("content", content);
        if (categoryId > 0) {
            values.put("category_id", categoryId);
        } else {
            values.putNull("category_id");
        }
        return db.update("notes", values, "id = ?", new String[]{String.valueOf(id)});
    }

    public void deleteNote(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("notes", "id = ?", new String[]{String.valueOf(id)});
    }

    public List<Note> searchNotes(String query, long categoryId) {
        List<Note> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT n.id, n.title, n.content, n.category_id, c.name, n.created_at ");
        sql.append("FROM notes n ");
        sql.append("LEFT JOIN categories c ON n.category_id = c.id ");
        sql.append("WHERE 1=1 ");
        
        List<String> selectionArgs = new ArrayList<>();
        
        if (categoryId == 0) {
            sql.append("AND n.category_id IS NULL ");
        } else if (categoryId > 0) {
            sql.append("AND n.category_id = ? ");
            selectionArgs.add(String.valueOf(categoryId));
        }
        
        if (query != null && !query.trim().isEmpty()) {
            sql.append("AND (n.title LIKE ? OR n.content LIKE ?) ");
            selectionArgs.add("%" + query.trim() + "%");
            selectionArgs.add("%" + query.trim() + "%");
        }
        
        sql.append("ORDER BY n.created_at DESC");
        
        Cursor cursor = db.rawQuery(sql.toString(), selectionArgs.toArray(new String[0]));
        if (cursor.moveToFirst()) {
            do {
                list.add(new Note(
                        cursor.getLong(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.isNull(3) ? 0 : cursor.getLong(3),
                        cursor.isNull(4) ? "Uncategorized" : cursor.getString(4),
                        cursor.getString(5)
                ));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }
}