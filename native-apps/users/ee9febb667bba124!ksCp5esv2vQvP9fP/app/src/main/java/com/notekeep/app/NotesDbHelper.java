package com.notekeep.app;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class NotesDbHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "notekeep.db";
    private static final int DATABASE_VERSION = 1;

    public NotesDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE categories (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT UNIQUE NOT NULL)");

        db.execSQL("CREATE TABLE notes (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "title TEXT NOT NULL, " +
                "content TEXT NOT NULL, " +
                "category_id INTEGER, " +
                "timestamp TEXT NOT NULL, " +
                "FOREIGN KEY(category_id) REFERENCES categories(id) ON DELETE SET NULL)");

        // Insert some default categories to start with
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
}