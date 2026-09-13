package com.notesmaster.app;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "notes_db";
    private static final int DATABASE_VERSION = 1;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create Categories Table
        db.execSQL("CREATE TABLE categories (_id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT UNIQUE)");
        
        // Create Notes Table
        db.execSQL("CREATE TABLE notes (_id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT, content TEXT, category TEXT, timestamp TEXT)");

        // Populate Default Categories
        db.execSQL("INSERT INTO categories (name) VALUES ('General')");
        db.execSQL("INSERT INTO categories (name) VALUES ('Work')");
        db.execSQL("INSERT INTO categories (name) VALUES ('Personal')");
        db.execSQL("INSERT INTO categories (name) VALUES ('Study')");
        db.execSQL("INSERT INTO categories (name) VALUES ('Ideas')");

        // Populate Default Beautiful Notes
        db.execSQL("INSERT INTO notes (title, content, category, timestamp) VALUES ('Welcome to Notes Master', 'Organize your thoughts easily. Tap existing notes to edit details, filter categories from dropdown selection, search titles, or tap delete on the list row to remove them.', 'General', '2023-11-01 10:00 AM')");
        db.execSQL("INSERT INTO notes (title, content, category, timestamp) VALUES ('Project Specifications', 'Integrate SQLite storage mechanisms, construct fully parameterized statements, compile without AndroidX support, keep user interaction pure.', 'Work', '2023-11-01 10:15 AM')");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS notes");
        db.execSQL("DROP TABLE IF EXISTS categories");
        onCreate(db);
    }
}