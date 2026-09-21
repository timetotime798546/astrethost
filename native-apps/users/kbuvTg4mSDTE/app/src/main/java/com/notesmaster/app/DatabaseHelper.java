package com.notesmaster.app;

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

    public static final String TABLE_NAME = "notes";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_CONTENT = "content";
    public static final String COLUMN_CATEGORY = "category";
    public static final String COLUMN_TIMESTAMP = "timestamp";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTableQuery = "CREATE TABLE " + TABLE_NAME + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_TITLE + " TEXT, " +
                COLUMN_CONTENT + " TEXT, " +
                COLUMN_CATEGORY + " TEXT, " +
                COLUMN_TIMESTAMP + " TEXT)";
        db.execSQL(createTableQuery);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    // Insert Note
    public long insertNote(Note note) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, note.getTitle());
        values.put(COLUMN_CONTENT, note.getContent());
        values.put(COLUMN_CATEGORY, note.getCategory());
        values.put(COLUMN_TIMESTAMP, note.getTimestamp());

        long id = db.insert(TABLE_NAME, null, values);
        db.close();
        return id;
    }

    // Update Note
    public int updateNote(Note note) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, note.getTitle());
        values.put(COLUMN_CONTENT, note.getContent());
        values.put(COLUMN_CATEGORY, note.getCategory());
        values.put(COLUMN_TIMESTAMP, note.getTimestamp());

        int count = db.update(TABLE_NAME, values, COLUMN_ID + " = ?",
                new String[]{String.valueOf(note.getId())});
        db.close();
        return count;
    }

    // Delete Note
    public void deleteNote(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NAME, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    // Retrieve and filter notes
    public List<Note> getNotes(String category, String searchQuery) {
        List<Note> notes = new ArrayList<Note>();
        SQLiteDatabase db = this.getReadableDatabase();

        String selectQuery = "SELECT * FROM " + TABLE_NAME;
        List<String> selectionArgs = new ArrayList<String>();
        String whereClause = "";

        boolean hasCategory = category != null && !category.equalsIgnoreCase("All");
        boolean hasSearch = searchQuery != null && !searchQuery.trim().isEmpty();

        if (hasCategory && hasSearch) {
            whereClause = " WHERE " + COLUMN_CATEGORY + " = ? AND (" +
                    COLUMN_TITLE + " LIKE ? OR " + COLUMN_CONTENT + " LIKE ?)";
            selectionArgs.add(category);
            selectionArgs.add("%" + searchQuery + "%");
            selectionArgs.add("%" + searchQuery + "%");
        } else if (hasCategory) {
            whereClause = " WHERE " + COLUMN_CATEGORY + " = ?";
            selectionArgs.add(category);
        } else if (hasSearch) {
            whereClause = " WHERE " + COLUMN_TITLE + " LIKE ? OR " + COLUMN_CONTENT + " LIKE ?";
            selectionArgs.add("%" + searchQuery + "%");
            selectionArgs.add("%" + searchQuery + "%");
        }

        selectQuery = selectQuery + whereClause + " ORDER BY " + COLUMN_ID + " DESC";

        String[] args = selectionArgs.toArray(new String[0]);
        Cursor cursor = db.rawQuery(selectQuery, args);

        if (cursor.moveToFirst()) {
            do {
                int idIndex = cursor.getColumnIndex(COLUMN_ID);
                int titleIndex = cursor.getColumnIndex(COLUMN_TITLE);
                int contentIndex = cursor.getColumnIndex(COLUMN_CONTENT);
                int categoryIndex = cursor.getColumnIndex(COLUMN_CATEGORY);
                int timeIndex = cursor.getColumnIndex(COLUMN_TIMESTAMP);

                int id = idIndex != -1 ? cursor.getInt(idIndex) : 0;
                String title = titleIndex != -1 ? cursor.getString(titleIndex) : "";
                String content = contentIndex != -1 ? cursor.getString(contentIndex) : "";
                String categoryVal = categoryIndex != -1 ? cursor.getString(categoryIndex) : "Others";
                String timestamp = timeIndex != -1 ? cursor.getString(timeIndex) : "";

                notes.add(new Note(id, title, content, categoryVal, timestamp));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return notes;
    }
}