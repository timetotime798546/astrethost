package com.classicnotes.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "classic_notes.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_NOTES = "notes";
    public static final String TABLE_CATEGORIES = "categories";

    public static final String KEY_NOTE_ID = "id";
    public static final String KEY_NOTE_TITLE = "title";
    public static final String KEY_NOTE_CONTENT = "content";
    public static final String KEY_NOTE_CATEGORY_ID = "category_id";
    public static final String KEY_NOTE_DATE = "date_created";

    public static final String KEY_CAT_ID = "id";
    public static final String KEY_CAT_NAME = "name";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_CATEGORIES_TABLE = "CREATE TABLE " + TABLE_CATEGORIES + "("
                + KEY_CAT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_CAT_NAME + " TEXT UNIQUE" + ")";
        
        String CREATE_NOTES_TABLE = "CREATE TABLE " + TABLE_NOTES + "("
                + KEY_NOTE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_NOTE_TITLE + " TEXT,"
                + KEY_NOTE_CONTENT + " TEXT,"
                + KEY_NOTE_CATEGORY_ID + " INTEGER,"
                + KEY_NOTE_DATE + " TEXT,"
                + "FOREIGN KEY(" + KEY_NOTE_CATEGORY_ID + ") REFERENCES " + TABLE_CATEGORIES + "(" + KEY_CAT_ID + ") ON DELETE SET NULL" + ")";

        db.execSQL(CREATE_CATEGORIES_TABLE);
        db.execSQL(CREATE_NOTES_TABLE);

        db.execSQL("INSERT INTO " + TABLE_CATEGORIES + " (" + KEY_CAT_NAME + ") VALUES ('Personal')");
        db.execSQL("INSERT INTO " + TABLE_CATEGORIES + " (" + KEY_CAT_NAME + ") VALUES ('Work')");
        db.execSQL("INSERT INTO " + TABLE_CATEGORIES + " (" + KEY_CAT_NAME + ") VALUES ('Ideas')");
        db.execSQL("INSERT INTO " + TABLE_CATEGORIES + " (" + KEY_CAT_NAME + ") VALUES ('Todo')");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOTES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CATEGORIES);
        onCreate(db);
    }

    public List<Category> getAllCategories() {
        List<Category> categories = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_CATEGORIES + " ORDER BY " + KEY_CAT_NAME + " ASC", null);
        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_CAT_ID));
                String name = cursor.getString(cursor.getColumnIndexOrThrow(KEY_CAT_NAME));
                categories.add(new Category(id, name));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return categories;
    }

    public long addCategory(String name) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_CAT_NAME, name);
        return db.insertWithOnConflict(TABLE_CATEGORIES, null, values, SQLiteDatabase.CONFLICT_IGNORE);
    }

    public void deleteCategory(int categoryId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.putNull(KEY_NOTE_CATEGORY_ID);
        db.update(TABLE_NOTES, values, KEY_NOTE_CATEGORY_ID + " = ?", new String[]{String.valueOf(categoryId)});
        db.delete(TABLE_CATEGORIES, KEY_CAT_ID + " = ?", new String[]{String.valueOf(categoryId)});
    }

    public long addNote(String title, String content, int categoryId, String date) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_NOTE_TITLE, title);
        values.put(KEY_NOTE_CONTENT, content);
        if (categoryId > 0) {
            values.put(KEY_NOTE_CATEGORY_ID, categoryId);
        } else {
            values.putNull(KEY_NOTE_CATEGORY_ID);
        }
        values.put(KEY_NOTE_DATE, date);
        return db.insert(TABLE_NOTES, null, values);
    }

    public int updateNote(int id, String title, String content, int categoryId, String date) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_NOTE_TITLE, title);
        values.put(KEY_NOTE_CONTENT, content);
        if (categoryId > 0) {
            values.put(KEY_NOTE_CATEGORY_ID, categoryId);
        } else {
            values.putNull(KEY_NOTE_CATEGORY_ID);
        }
        values.put(KEY_NOTE_DATE, date);
        return db.update(TABLE_NOTES, values, KEY_NOTE_ID + " = ?", new String[]{String.valueOf(id)});
    }

    public void deleteNote(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NOTES, KEY_NOTE_ID + " = ?", new String[]{String.valueOf(id)});
    }

    public List<Note> getFilteredNotes(String searchQuery, int categoryId) {
        List<Note> notes = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        StringBuilder query = new StringBuilder();
        query.append("SELECT n.*, c.").append(KEY_CAT_NAME).append(" FROM ").append(TABLE_NOTES).append(" n ");
        query.append("LEFT JOIN ").append(TABLE_CATEGORIES).append(" c ON n.").append(KEY_NOTE_CATEGORY_ID).append(" = c.").append(KEY_CAT_ID);
        
        List<String> selectionArgs = new ArrayList<>();
        boolean hasWhere = false;
        
        if (categoryId == -2) {
            query.append(" WHERE n.").append(KEY_NOTE_CATEGORY_ID).append(" IS NULL");
            hasWhere = true;
        } else if (categoryId > 0) {
            query.append(" WHERE n.").append(KEY_NOTE_CATEGORY_ID).append(" = ?");
            selectionArgs.add(String.valueOf(categoryId));
            hasWhere = true;
        }
        
        if (searchQuery != null && !searchQuery.trim().isEmpty()) {
            if (hasWhere) {
                query.append(" AND ");
            } else {
                query.append(" WHERE ");
            }
            query.append("(n.").append(KEY_NOTE_TITLE).append(" LIKE ? OR n.").append(KEY_NOTE_CONTENT).append(" LIKE ?)");
            selectionArgs.add("%" + searchQuery.trim() + "%");
            selectionArgs.add("%" + searchQuery.trim() + "%");
        }
        
        query.append(" ORDER BY n.").append(KEY_NOTE_ID).append(" DESC");
        
        Cursor cursor = db.rawQuery(query.toString(), selectionArgs.toArray(new String[0]));
        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_NOTE_ID));
                String title = cursor.getString(cursor.getColumnIndexOrThrow(KEY_NOTE_TITLE));
                String content = cursor.getString(cursor.getColumnIndexOrThrow(KEY_NOTE_CONTENT));
                int catId = cursor.isNull(cursor.getColumnIndexOrThrow(KEY_NOTE_CATEGORY_ID)) ? -1 : cursor.getInt(cursor.getColumnIndexOrThrow(KEY_NOTE_CATEGORY_ID));
                String catName = cursor.isNull(cursor.getColumnIndexOrThrow(KEY_CAT_NAME)) ? "Uncategorized" : cursor.getString(cursor.getColumnIndexOrThrow(KEY_CAT_NAME));
                String date = cursor.getString(cursor.getColumnIndexOrThrow(KEY_NOTE_DATE));
                
                notes.add(new Note(id, title, content, catId, catName, date));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return notes;
    }
}