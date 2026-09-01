package com.quicknotes.app;

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

    public static final String TABLE_NOTES = "notes";
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
        String createTable = "CREATE TABLE " + TABLE_NOTES + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_TITLE + " TEXT, " +
                COLUMN_CONTENT + " TEXT, " +
                COLUMN_CATEGORY + " TEXT, " +
                COLUMN_TIMESTAMP + " INTEGER" +
                ")";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOTES);
        onCreate(db);
    }

    public long insertNote(String title, String content, String category) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, title);
        values.put(COLUMN_CONTENT, content);
        values.put(COLUMN_CATEGORY, category);
        values.put(COLUMN_TIMESTAMP, System.currentTimeMillis());
        long id = db.insert(TABLE_NOTES, null, values);
        db.close();
        return id;
    }

    public int updateNote(long id, String title, String content, String category) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, title);
        values.put(COLUMN_CONTENT, content);
        values.put(COLUMN_CATEGORY, category);
        values.put(COLUMN_TIMESTAMP, System.currentTimeMillis());
        int rows = db.update(TABLE_NOTES, values, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
        return rows;
    }

    public void deleteNote(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NOTES, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    public List<Note> getAllNotes(String searchQuery, String categoryFilter) {
        List<Note> notes = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        StringBuilder queryBuilder = new StringBuilder("SELECT * FROM " + TABLE_NOTES);
        List<String> selectionArgs = new ArrayList<>();

        boolean hasSearch = searchQuery != null && !searchQuery.trim().isEmpty();
        boolean hasCategory = categoryFilter != null && !categoryFilter.equals("All");

        if (hasSearch || hasCategory) {
            queryBuilder.append(" WHERE ");
            if (hasSearch) {
                queryBuilder.append("(" + COLUMN_TITLE + " LIKE ? OR " + COLUMN_CONTENT + " LIKE ?)");
                selectionArgs.add("%" + searchQuery + "%");
                selectionArgs.add("%" + searchQuery + "%");
            }
            if (hasCategory) {
                if (hasSearch) {
                    queryBuilder.append(" AND ");
                }
                queryBuilder.append(COLUMN_CATEGORY + " = ?");
                selectionArgs.add(categoryFilter);
            }
        }

        queryBuilder.append(" ORDER BY " + COLUMN_TIMESTAMP + " DESC");

        Cursor cursor = db.rawQuery(queryBuilder.toString(), selectionArgs.toArray(new String[0]));
        if (cursor.moveToFirst()) {
            do {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID));
                String title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE));
                String content = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CONTENT));
                String category = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY));
                long timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP));
                notes.add(new Note(id, title, content, category, timestamp));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return notes;
    }
}