package com.smartnotes.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class NotesDatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "notes.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_NAME = "notes";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_TITLE = "title";
    private static final String COLUMN_CONTENT = "content";
    private static final String COLUMN_CATEGORY = "category";
    private static final String COLUMN_DATE = "date";

    public NotesDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_NAME + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_TITLE + " TEXT, " +
                COLUMN_CONTENT + " TEXT, " +
                COLUMN_CATEGORY + " TEXT, " +
                COLUMN_DATE + " INTEGER" +
                ")";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public long insertNote(String title, String content, String category) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, title);
        values.put(COLUMN_CONTENT, content);
        values.put(COLUMN_CATEGORY, category);
        values.put(COLUMN_DATE, System.currentTimeMillis());
        long id = db.insert(TABLE_NAME, null, values);
        db.close();
        return id;
    }

    public int updateNote(long id, String title, String content, String category) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, title);
        values.put(COLUMN_CONTENT, content);
        values.put(COLUMN_CATEGORY, category);
        values.put(COLUMN_DATE, System.currentTimeMillis());
        int rows = db.update(TABLE_NAME, values, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
        return rows;
    }

    public void deleteNote(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NAME, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    public List<Note> getAllNotes(String searchQuery, String categoryFilter) {
        List<Note> notes = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        StringBuilder query = new StringBuilder("SELECT * FROM " + TABLE_NAME);
        List<String> args = new ArrayList<>();
        
        boolean hasQuery = searchQuery != null && !searchQuery.trim().isEmpty();
        boolean hasCategory = categoryFilter != null && !categoryFilter.equals("All");
        
        if (hasQuery || hasCategory) {
            query.append(" WHERE ");
            if (hasQuery) {
                query.append("(" + COLUMN_TITLE + " LIKE ? OR " + COLUMN_CONTENT + " LIKE ?)");
                args.add("%" + searchQuery + "%");
                args.add("%" + searchQuery + "%");
            }
            if (hasCategory) {
                if (hasQuery) {
                    query.append(" AND ");
                }
                query.append(COLUMN_CATEGORY + " = ?");
                args.add(categoryFilter);
            }
        }
        
        query.append(" ORDER BY " + COLUMN_DATE + " DESC");
        
        Cursor cursor = db.rawQuery(query.toString(), args.toArray(new String[0]));
        if (cursor.moveToFirst()) {
            do {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID));
                String title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE));
                String content = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CONTENT));
                String category = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY));
                long date = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_DATE));
                notes.add(new Note(id, title, content, category, date));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return notes;
    }
}