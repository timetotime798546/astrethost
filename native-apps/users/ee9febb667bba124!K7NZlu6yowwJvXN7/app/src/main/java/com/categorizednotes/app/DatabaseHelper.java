package com.categorizednotes.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "notes_manager.db";
    private static final int DATABASE_VERSION = 1;

    // Table names
    public static final String TABLE_NOTES = "notes";
    public static final String TABLE_CATEGORIES = "categories";

    // Notes columns
    public static final String COLUMN_NOTE_ID = "id";
    public static final String COLUMN_NOTE_TITLE = "title";
    public static final String COLUMN_NOTE_CONTENT = "content";
    public static final String COLUMN_NOTE_CATEGORY = "category_name";
    public static final String COLUMN_NOTE_TIMESTAMP = "updated_at";

    // Categories columns
    public static final String COLUMN_CAT_ID = "id";
    public static final String COLUMN_CAT_NAME = "name";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create notes table
        String createNotesTable = "CREATE TABLE " + TABLE_NOTES + " (" +
                COLUMN_NOTE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_NOTE_TITLE + " TEXT, " +
                COLUMN_NOTE_CONTENT + " TEXT, " +
                COLUMN_NOTE_CATEGORY + " TEXT, " +
                COLUMN_NOTE_TIMESTAMP + " TEXT)";
        db.execSQL(createNotesTable);

        // Create categories table
        String createCategoriesTable = "CREATE TABLE " + TABLE_CATEGORIES + " (" +
                COLUMN_CAT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_CAT_NAME + " TEXT UNIQUE)";
        db.execSQL(createCategoriesTable);

        // Prepopulate standard starter categories
        String[] standardCategories = {"Personal", "Work", "Ideas", "Lists", "Reminders"};
        for (int i = 0; i < standardCategories.length; i++) {
            ContentValues values = new ContentValues();
            values.put(COLUMN_CAT_NAME, standardCategories[i]);
            db.insert(TABLE_CATEGORIES, null, values);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOTES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CATEGORIES);
        onCreate(db);
    }

    // Insert new Category
    public boolean insertCategory(String categoryName) {
        if (categoryName == null || categoryName.trim().isEmpty()) {
            return false;
        }
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_CAT_NAME, categoryName.trim());
        long result = db.insert(TABLE_CATEGORIES, null, values);
        return result != -1;
    }

    // Get all categories in List framework
    public List<Category> getAllCategories() {
        List<Category> categoriesList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_CATEGORIES + " ORDER BY " + COLUMN_CAT_NAME + " ASC", null);

        if (cursor.moveToFirst()) {
            do {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CAT_ID));
                String name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CAT_NAME));
                categoriesList.add(new Category(id, name));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return categoriesList;
    }

    // Insert Note Item
    public boolean insertNote(String title, String content, String category) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NOTE_TITLE, title);
        values.put(COLUMN_NOTE_CONTENT, content);
        values.put(COLUMN_NOTE_CATEGORY, category);
        values.put(COLUMN_NOTE_TIMESTAMP, getCurrentFormattedDate());

        long result = db.insert(TABLE_NOTES, null, values);
        return result != -1;
    }

    // Update existing Note Item
    public boolean updateNote(long id, String title, String content, String category) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NOTE_TITLE, title);
        values.put(COLUMN_NOTE_CONTENT, content);
        values.put(COLUMN_NOTE_CATEGORY, category);
        values.put(COLUMN_NOTE_TIMESTAMP, getCurrentFormattedDate());

        int affectedRows = db.update(TABLE_NOTES, values, COLUMN_NOTE_ID + " = ?", new String[]{String.valueOf(id)});
        return affectedRows > 0;
    }

    // Fetch Note details by ID
    public Note getNoteById(long id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NOTES, null, COLUMN_NOTE_ID + " = ?", new String[]{String.valueOf(id)}, null, null, null);
        Note note = null;
        if (cursor != null && cursor.moveToFirst()) {
            String title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOTE_TITLE));
            String content = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOTE_CONTENT));
            String category = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOTE_CATEGORY));
            String date = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOTE_TIMESTAMP));
            note = new Note(id, title, content, category, date);
            cursor.close();
        }
        return note;
    }

    // Fetch query notes with searching and categorization
    public List<Note> searchAndFilterNotes(String query, String categoryFilter) {
        List<Note> notes = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        StringBuilder queryBuilder = new StringBuilder("SELECT * FROM " + TABLE_NOTES + " WHERE 1=1");
        List<String> selectionArgsList = new ArrayList<>();

        if (categoryFilter != null && !categoryFilter.equals("All Categories")) {
            queryBuilder.append(" AND " + COLUMN_NOTE_CATEGORY + " = ?");
            selectionArgsList.add(categoryFilter);
        }

        if (query != null && !query.trim().isEmpty()) {
            queryBuilder.append(" AND (" + COLUMN_NOTE_TITLE + " LIKE ? OR " + COLUMN_NOTE_CONTENT + " LIKE ?)");
            String wrapQuery = "%" + query.trim() + "%";
            selectionArgsList.add(wrapQuery);
            selectionArgsList.add(wrapQuery);
        }

        queryBuilder.append(" ORDER BY " + COLUMN_NOTE_ID + " DESC");

        String[] args = selectionArgsList.toArray(new String[0]);
        Cursor cursor = db.rawQuery(queryBuilder.toString(), args);

        if (cursor.moveToFirst()) {
            do {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_NOTE_ID));
                String title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOTE_TITLE));
                String content = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOTE_CONTENT));
                String category = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOTE_CATEGORY));
                String date = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOTE_TIMESTAMP));
                notes.add(new Note(id, title, content, category, date));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return notes;
    }

    // Delete Note
    public void deleteNote(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NOTES, COLUMN_NOTE_ID + " = ?", new String[]{String.valueOf(id)});
    }

    // Utility timestamp retrieval
    private String getCurrentFormattedDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault());
        return sdf.format(new Date());
    }
}