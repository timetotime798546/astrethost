package com.notewise.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "notewise.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_CATEGORIES = "categories";
    private static final String TABLE_NOTES = "notes";

    private static final String KEY_ID = "id";
    private static final String KEY_CAT_NAME = "name";

    private static final String KEY_NOTE_TITLE = "title";
    private static final String KEY_NOTE_CONTENT = "content";
    private static final String KEY_NOTE_CAT_ID = "category_id";
    private static final String KEY_NOTE_COLOR = "color_index";
    private static final String KEY_NOTE_TIMESTAMP = "timestamp";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createCategoriesTable = "CREATE TABLE " + TABLE_CATEGORIES + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_CAT_NAME + " TEXT UNIQUE" + ")";
        db.execSQL(createCategoriesTable);

        String createNotesTable = "CREATE TABLE " + TABLE_NOTES + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_NOTE_TITLE + " TEXT,"
                + KEY_NOTE_CONTENT + " TEXT,"
                + KEY_NOTE_CAT_ID + " INTEGER,"
                + KEY_NOTE_COLOR + " INTEGER DEFAULT 0,"
                + KEY_NOTE_TIMESTAMP + " INTEGER" + ")";
        db.execSQL(createNotesTable);

        // Seed default category items
        db.execSQL("INSERT INTO " + TABLE_CATEGORIES + " (" + KEY_CAT_NAME + ") VALUES ('Work')");
        db.execSQL("INSERT INTO " + TABLE_CATEGORIES + " (" + KEY_CAT_NAME + ") VALUES ('Personal')");
        db.execSQL("INSERT INTO " + TABLE_CATEGORIES + " (" + KEY_CAT_NAME + ") VALUES ('Ideas')");
        db.execSQL("INSERT INTO " + TABLE_CATEGORIES + " (" + KEY_CAT_NAME + ") VALUES ('Todo')");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOTES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CATEGORIES);
        onCreate(db);
    }

    public long insertCategory(String name) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_CAT_NAME, name);
        try {
            return db.insert(TABLE_CATEGORIES, null, values);
        } catch (Exception e) {
            return -1;
        }
    }

    public List<Category> getAllCategories() {
        List<Category> categories = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_CATEGORIES + " ORDER BY " + KEY_CAT_NAME + " ASC", null);

        if (cursor.moveToFirst()) {
            do {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ID));
                String name = cursor.getString(cursor.getColumnIndexOrThrow(KEY_CAT_NAME));
                categories.add(new Category(id, name));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return categories;
    }

    public void deleteCategory(long categoryId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_CATEGORIES, KEY_ID + " = ?", new String[]{String.valueOf(categoryId)});
        
        ContentValues values = new ContentValues();
        values.put(KEY_NOTE_CAT_ID, 0);
        db.update(TABLE_NOTES, values, KEY_NOTE_CAT_ID + " = ?", new String[]{String.valueOf(categoryId)});
    }

    public long insertNote(String title, String content, long categoryId, int colorIndex) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_NOTE_TITLE, title);
        values.put(KEY_NOTE_CONTENT, content);
        values.put(KEY_NOTE_CAT_ID, categoryId);
        values.put(KEY_NOTE_COLOR, colorIndex);
        values.put(KEY_NOTE_TIMESTAMP, System.currentTimeMillis());
        return db.insert(TABLE_NOTES, null, values);
    }

    public int updateNote(long id, String title, String content, long categoryId, int colorIndex) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_NOTE_TITLE, title);
        values.put(KEY_NOTE_CONTENT, content);
        values.put(KEY_NOTE_CAT_ID, categoryId);
        values.put(KEY_NOTE_COLOR, colorIndex);
        values.put(KEY_NOTE_TIMESTAMP, System.currentTimeMillis());
        return db.update(TABLE_NOTES, values, KEY_ID + " = ?", new String[]{String.valueOf(id)});
    }

    public void deleteNote(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NOTES, KEY_ID + " = ?", new String[]{String.valueOf(id)});
    }

    public List<Note> getNotes(String searchQuery, long filterCategoryId) {
        List<Note> notes = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT n.*, c.name AS cat_name FROM ").append(TABLE_NOTES).append(" n ");
        queryBuilder.append("LEFT JOIN ").append(TABLE_CATEGORIES).append(" c ON n.category_id = c.id ");

        List<String> selectionArgs = new ArrayList<>();
        boolean whereAdded = false;

        if (filterCategoryId > 0) {
            queryBuilder.append("WHERE n.category_id = ? ");
            selectionArgs.add(String.valueOf(filterCategoryId));
            whereAdded = true;
        } else if (filterCategoryId == 0) {
            queryBuilder.append("WHERE (n.category_id = 0 OR n.category_id IS NULL) ");
            whereAdded = true;
        }

        if (searchQuery != null && !searchQuery.trim().isEmpty()) {
            if (whereAdded) {
                queryBuilder.append("AND ");
            } else {
                queryBuilder.append("WHERE ");
                whereAdded = true;
            }
            queryBuilder.append("(n.title LIKE ? OR n.content LIKE ?) ");
            selectionArgs.add("%" + searchQuery + "%");
            selectionArgs.add("%" + searchQuery + "%");
        }

        queryBuilder.append("ORDER BY n.timestamp DESC");

        String[] argsArray = selectionArgs.toArray(new String[0]);
        Cursor cursor = db.rawQuery(queryBuilder.toString(), argsArray);

        if (cursor.moveToFirst()) {
            do {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ID));
                String title = cursor.getString(cursor.getColumnIndexOrThrow(KEY_NOTE_TITLE));
                String content = cursor.getString(cursor.getColumnIndexOrThrow(KEY_NOTE_CONTENT));
                long catId = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_NOTE_CAT_ID));
                String catName = cursor.getString(cursor.getColumnIndexOrThrow("cat_name"));
                if (catName == null) {
                    catName = "Uncategorized";
                }
                int colorIndex = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_NOTE_COLOR));
                long timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_NOTE_TIMESTAMP));

                notes.add(new Note(id, title, content, catId, catName, colorIndex, timestamp));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return notes;
    }
}