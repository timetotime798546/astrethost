package com.smartnotes.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "smart_notes_db.db";
    private static final int DATABASE_VERSION = 1;

    // Table Notes
    public static final String TABLE_NOTES = "notes";
    public static final String COL_NOTE_ID = "id";
    public static final String COL_NOTE_TITLE = "title";
    public static final String COL_NOTE_CONTENT = "content";
    public static final String COL_NOTE_CATEGORY_ID = "category_id";
    public static final String COL_NOTE_TIMESTAMP = "timestamp";

    // Table Categories
    public static final String TABLE_CATEGORIES = "categories";
    public static final String COL_CAT_ID = "id";
    public static final String COL_CAT_NAME = "name";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create Categories Table First
        String createCategoriesTable = "CREATE TABLE " + TABLE_CATEGORIES + " (" +
                COL_CAT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_CAT_NAME + " TEXT UNIQUE NOT NULL)";

        // Create Notes Table
        String createNotesTable = "CREATE TABLE " + TABLE_NOTES + " (" +
                COL_NOTE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_NOTE_TITLE + " TEXT NOT NULL, " +
                COL_NOTE_CONTENT + " TEXT, " +
                COL_NOTE_CATEGORY_ID + " INTEGER, " +
                COL_NOTE_TIMESTAMP + " DATETIME DEFAULT CURRENT_TIMESTAMP)";

        db.execSQL(createCategoriesTable);
        db.execSQL(createNotesTable);

        // Seed default categories
        db.execSQL("INSERT INTO " + TABLE_CATEGORIES + " (" + COL_CAT_NAME + ") VALUES ('Uncategorized')");
        db.execSQL("INSERT INTO " + TABLE_CATEGORIES + " (" + COL_CAT_NAME + ") VALUES ('Personal')");
        db.execSQL("INSERT INTO " + TABLE_CATEGORIES + " (" + COL_CAT_NAME + ") VALUES ('Work')");
        db.execSQL("INSERT INTO " + TABLE_CATEGORIES + " (" + COL_CAT_NAME + ") VALUES ('Ideas')");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOTES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CATEGORIES);
        onCreate(db);
    }

    // --- Category Functions ---
    public long insertCategory(String name) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_CAT_NAME, name);
        long id = db.insert(TABLE_CATEGORIES, null, values);
        db.close();
        return id;
    }

    public Cursor getAllCategories() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_CATEGORIES + " ORDER BY " + COL_CAT_NAME + " ASC", null);
    }

    public String getCategoryName(int catId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + COL_CAT_NAME + " FROM " + TABLE_CATEGORIES + " WHERE " + COL_CAT_ID + " = ?", new String[]{String.valueOf(catId)});
        String name = "Uncategorized";
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                name = cursor.getString(0);
            }
            cursor.close();
        }
        return name;
    }

    public void deleteCategory(int catId) {
        SQLiteDatabase db = this.getWritableDatabase();
        // Update any notes attached to this category to Uncategorized (ID 1 fallback)
        ContentValues values = new ContentValues();
        values.put(COL_NOTE_CATEGORY_ID, 1);
        db.update(TABLE_NOTES, values, COL_NOTE_CATEGORY_ID + " = ?", new String[]{String.valueOf(catId)});
        
        // Delete category
        db.delete(TABLE_CATEGORIES, COL_CAT_ID + " = ?", new String[]{String.valueOf(catId)});
        db.close();
    }

    // --- Notes Functions ---
    public long insertNote(String title, String content, int categoryId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_NOTE_TITLE, title);
        values.put(COL_NOTE_CONTENT, content);
        values.put(COL_NOTE_CATEGORY_ID, categoryId);
        long id = db.insert(TABLE_NOTES, null, values);
        db.close();
        return id;
    }

    public int updateNote(int id, String title, String content, int categoryId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_NOTE_TITLE, title);
        values.put(COL_NOTE_CONTENT, content);
        values.put(COL_NOTE_CATEGORY_ID, categoryId);
        int rows = db.update(TABLE_NOTES, values, COL_NOTE_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
        return rows;
    }

    public void deleteNote(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NOTES, COL_NOTE_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    public Cursor getNotes(int filterCategoryId, String searchQuery) {
        SQLiteDatabase db = this.getReadableDatabase();
        StringBuilder query = new StringBuilder("SELECT * FROM " + TABLE_NOTES + " WHERE 1=1");
        
        if (filterCategoryId != -1) {
            query.append(" AND ").append(COL_NOTE_CATEGORY_ID).append(" = ").append(filterCategoryId);
        }

        if (searchQuery != null && !searchQuery.trim().isEmpty()) {
            query.append(" AND (")
                 .append(COL_NOTE_TITLE).append(" LIKE '%").append(searchQuery).append("%' OR ")
                 .append(COL_NOTE_CONTENT).append(" LIKE '%").append(searchQuery).append("%')");
        }

        query.append(" ORDER BY ").append(COL_NOTE_ID).append(" DESC");
        return db.rawQuery(query.toString(), null);
    }
}