package com.quicknotes.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class NotesDbHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "quick_notes.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_NOTES = "notes";
    private static final String KEY_ID = "id";
    private static final String KEY_TITLE = "title";
    private static final String KEY_CONTENT = "content";
    private static final String KEY_CATEGORY = "category";
    private static final String KEY_TIMESTAMP = "timestamp";

    public NotesDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE_NOTES = "CREATE TABLE " + TABLE_NOTES + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_TITLE + " TEXT,"
                + KEY_CONTENT + " TEXT,"
                + KEY_CATEGORY + " TEXT,"
                + KEY_TIMESTAMP + " INTEGER" + ")";
        db.execSQL(CREATE_TABLE_NOTES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOTES);
        onCreate(db);
    }

    // Insert new note
    public long insertNote(String title, String content, String category) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_TITLE, title);
        values.put(KEY_CONTENT, content);
        values.put(KEY_CATEGORY, category);
        values.put(KEY_TIMESTAMP, System.currentTimeMillis());
        long id = db.insert(TABLE_NOTES, null, values);
        db.close();
        return id;
    }

    // Update note
    public int updateNote(long id, String title, String content, String category) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_TITLE, title);
        values.put(KEY_CONTENT, content);
        values.put(KEY_CATEGORY, category);
        values.put(KEY_TIMESTAMP, System.currentTimeMillis());
        int rows = db.update(TABLE_NOTES, values, KEY_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
        return rows;
    }

    // Delete note
    public void deleteNote(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NOTES, KEY_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    // Get all notes ordered by timestamp desc
    public List<Note> getAllNotes() {
        List<Note> notes = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_NOTES + " ORDER BY " + KEY_TIMESTAMP + " DESC";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                Note note = new Note(
                        cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(KEY_TITLE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(KEY_CONTENT)),
                        cursor.getString(cursor.getColumnIndexOrThrow(KEY_CATEGORY)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(KEY_TIMESTAMP))
                );
                notes.add(note);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return notes;
    }

    // Get all unique categories dynamically stored in DB plus standard ones
    public List<String> getCategories() {
        List<String> categories = new ArrayList<>();
        categories.add("All");
        categories.add("Personal");
        categories.add("Work");
        categories.add("Ideas");
        categories.add("Todo");

        String query = "SELECT DISTINCT " + KEY_CATEGORY + " FROM " + TABLE_NOTES;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                String cat = cursor.getString(0);
                if (cat != null && !cat.trim().isEmpty() && !categories.contains(cat)) {
                    categories.add(cat);
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return categories;
    }
}