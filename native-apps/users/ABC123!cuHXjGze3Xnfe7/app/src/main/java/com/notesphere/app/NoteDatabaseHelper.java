package com.notesphere.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class NoteDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "notesphere.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_NAME = "notes";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_CONTENT = "content";
    public static final String COLUMN_CATEGORY = "category";
    public static final String COLUMN_TIMESTAMP = "timestamp";

    public NoteDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " ("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_TITLE + " TEXT, "
                + COLUMN_CONTENT + " TEXT, "
                + COLUMN_CATEGORY + " TEXT, "
                + COLUMN_TIMESTAMP + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                + ")";
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    // Insert simple initial starter notes
    public void populateDefaultNotes() {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_NAME, null);
        if (cursor != null) {
            cursor.moveToFirst();
            int count = cursor.getInt(0);
            cursor.close();
            if (count == 0) {
                insertStarterNote(db, "Welcome to NoteSphere!", "NoteSphere makes organizing ideas easy. Try search or adding different categories!", "General");
                insertStarterNote(db, "Shopping list", "1. Milk\n2. Oats\n3. Fresh strawberries\n4. Premium dark roast coffee beans", "Personal");
                insertStarterNote(db, "Business Idea", "Build a high-performance notes app utilizing native widgets with absolute zero overhead.", "Work");
            }
        }
    }

    private void insertStarterNote(SQLiteDatabase db, String title, String content, String category) {
        ContentValues values = new ContentValues();
        values.values.put(COLUMN_TITLE, title);
        values.values.put(COLUMN_CONTENT, content);
        values.values.put(COLUMN_CATEGORY, category);
        db.insert(TABLE_NAME, null, values);
    }
}