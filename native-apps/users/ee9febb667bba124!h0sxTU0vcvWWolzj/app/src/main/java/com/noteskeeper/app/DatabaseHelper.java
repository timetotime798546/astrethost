package com.noteskeeper.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "notes_keeper.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_NOTES = "notes";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_CONTENT = "content";
    public static final String COLUMN_CATEGORY = "category";
    public static final String COLUMN_TIMESTAMP = "timestamp";

    private static final String CREATE_TABLE_NOTES = "CREATE TABLE " + TABLE_NOTES + "("
            + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COLUMN_TITLE + " TEXT,"
            + COLUMN_CONTENT + " TEXT,"
            + COLUMN_CATEGORY + " TEXT,"
            + COLUMN_TIMESTAMP + " TEXT"
            + ")";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_NOTES);
        
        // Populate layout with dynamic sample categories
        insertSampleNote(db, "Welcome Note", "Welcome to Notes Keeper app! This is a simple, intuitive note-taking dashboard. Search tags or content above, and start organizing instantly.", "Personal", "2023-10-25 10:00");
        insertSampleNote(db, "Grocery Checklist", "Items to grab:\n- Farm-fresh Eggs\n- Whole wheat grain loaf\n- Avocado spread\n- Organic dark roast beans", "Shopping", "2023-10-26 14:30");
        insertSampleNote(db, "Work Meeting Agenda", "Agenda topics:\n- Finalize app UI templates\n- Coordinate release updates\n- Establish QA pipelines", "Work", "2023-10-27 09:15");
    }

    private void insertSampleNote(SQLiteDatabase db, String title, String content, String category, String timestamp) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, title);
        values.put(COLUMN_CONTENT, content);
        values.put(COLUMN_CATEGORY, category);
        values.put(COLUMN_TIMESTAMP, timestamp);
        db.insert(TABLE_NOTES, null, values);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOTES);
        onCreate(db);
    }

    public long insertNote(String title, String content, String category, String timestamp) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, title);
        values.put(COLUMN_CONTENT, content);
        values.put(COLUMN_CATEGORY, category);
        values.put(COLUMN_TIMESTAMP, timestamp);
        long id = db.insert(TABLE_NOTES, null, values);
        db.close();
        return id;
    }

    public int updateNote(long id, String title, String content, String category, String timestamp) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, title);
        values.put(COLUMN_CONTENT, content);
        values.put(COLUMN_CATEGORY, category);
        values.put(COLUMN_TIMESTAMP, timestamp);
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
        List<Note> notes = new ArrayList<Note>();
        SQLiteDatabase db = this.getReadableDatabase();

        String selectQuery = "SELECT * FROM " + TABLE_NOTES;
        List<String> selectionArgs = new ArrayList<String>();
        String whereClause = "";

        boolean hasSearch = searchQuery != null && !searchQuery.trim().isEmpty();
        boolean hasCategory = categoryFilter != null && !categoryFilter.equals("All Categories") && !categoryFilter.trim().isEmpty();

        if (hasSearch && hasCategory) {
            whereClause = " WHERE (" + COLUMN_TITLE + " LIKE ? OR " + COLUMN_CONTENT + " LIKE ?) AND " + COLUMN_CATEGORY + " = ?";
            selectionArgs.add("%" + searchQuery + "%");
            selectionArgs.add("%" + searchQuery + "%");
            selectionArgs.add(categoryFilter);
        } else if (hasSearch) {
            whereClause = " WHERE " + COLUMN_TITLE + " LIKE ? OR " + COLUMN_CONTENT + " LIKE ?";
            selectionArgs.add("%" + searchQuery + "%");
            selectionArgs.add("%" + searchQuery + "%");
        } else if (hasCategory) {
            whereClause = " WHERE " + COLUMN_CATEGORY + " = ?";
            selectionArgs.add(categoryFilter);
        }

        selectQuery += whereClause + " ORDER BY " + COLUMN_ID + " DESC";

        Cursor cursor = db.rawQuery(selectQuery, selectionArgs.toArray(new String[0]));

        if (cursor.moveToFirst()) {
            do {
                Note note = new Note();
                note.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)));
                note.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE)));
                note.setContent(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CONTENT)));
                note.setCategory(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY)));
                note.setTimestamp(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP)));
                notes.add(note);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return notes;
    }

    public List<String> getUniqueCategories() {
        List<String> categories = new ArrayList<String>();
        categories.add("All Categories");
        
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT DISTINCT " + COLUMN_CATEGORY + " FROM " + TABLE_NOTES + " ORDER BY " + COLUMN_CATEGORY + " ASC", null);
        
        if (cursor.moveToFirst()) {
            do {
                String cat = cursor.getString(0);
                if (cat != null && !cat.trim().isEmpty()) {
                    categories.add(cat);
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return categories;
    }
}