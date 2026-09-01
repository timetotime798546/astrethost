package com.notecraft.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class NotesDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "notecraft.db";
    private static final int DATABASE_VERSION = 1;

    // Categories Table
    public static final String TABLE_CATEGORIES = "categories";
    public static final String COL_CAT_ID = "id";
    public static final String COL_CAT_NAME = "name";

    // Notes Table
    public static final String TABLE_NOTES = "notes";
    public static final String COL_NOTE_ID = "id";
    public static final String COL_NOTE_TITLE = "title";
    public static final String COL_NOTE_CONTENT = "content";
    public static final String COL_NOTE_CAT_ID = "category_id";
    public static final String COL_NOTE_TIMESTAMP = "timestamp";

    public NotesDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createCategories = "CREATE TABLE " + TABLE_CATEGORIES + " (" +
                COL_CAT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_CAT_NAME + " TEXT UNIQUE)";

        String createNotes = "CREATE TABLE " + TABLE_NOTES + " (" +
                COL_NOTE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_NOTE_TITLE + " TEXT, " +
                COL_NOTE_CONTENT + " TEXT, " +
                COL_NOTE_CAT_ID + " INTEGER, " +
                COL_NOTE_TIMESTAMP + " TEXT, " +
                "FOREIGN KEY(" + COL_NOTE_CAT_ID + ") REFERENCES " + TABLE_CATEGORIES + "(" + COL_CAT_ID + ") ON DELETE SET NULL)";

        db.execSQL(createCategories);
        db.execSQL(createNotes);

        // Default categories
        db.execSQL("INSERT INTO " + TABLE_CATEGORIES + " (" + COL_CAT_NAME + ") VALUES ('Work')");
        db.execSQL("INSERT INTO " + TABLE_CATEGORIES + " (" + COL_CAT_NAME + ") VALUES ('Personal')");
        db.execSQL("INSERT INTO " + TABLE_CATEGORIES + " (" + COL_CAT_NAME + ") VALUES ('Ideas')");
        db.execSQL("INSERT INTO " + TABLE_CATEGORIES + " (" + COL_CAT_NAME + ") VALUES ('To-Do')");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOTES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CATEGORIES);
        onCreate(db);
    }

    // Category Operations
    public long insertCategory(String name) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_CAT_NAME, name);
        long result = db.insert(TABLE_CATEGORIES, null, values);
        db.close();
        return result;
    }

    public boolean deleteCategory(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rows = db.delete(TABLE_CATEGORIES, COL_CAT_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
        return rows > 0;
    }

    public List<Category> getAllCategories() {
        List<Category> categories = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_CATEGORIES + " ORDER BY " + COL_CAT_NAME + " ASC", null);
        if (cursor.moveToFirst()) {
            do {
                categories.add(new Category(
                        cursor.getLong(cursor.getColumnIndexOrThrow(COL_CAT_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_CAT_NAME))
                ));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return categories;
    }

    // Notes Operations
    public long insertNote(String title, String content, long categoryId, String timestamp) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_NOTE_TITLE, title);
        values.put(COL_NOTE_CONTENT, content);
        values.put(COL_NOTE_CAT_ID, categoryId);
        values.put(COL_NOTE_TIMESTAMP, timestamp);
        long id = db.insert(TABLE_NOTES, null, values);
        db.close();
        return id;
    }

    public int updateNote(long id, String title, String content, long categoryId, String timestamp) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_NOTE_TITLE, title);
        values.put(COL_NOTE_CONTENT, content);
        values.put(COL_NOTE_CAT_ID, categoryId);
        values.put(COL_NOTE_TIMESTAMP, timestamp);
        int rows = db.update(TABLE_NOTES, values, COL_NOTE_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
        return rows;
    }

    public boolean deleteNote(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rows = db.delete(TABLE_NOTES, COL_NOTE_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
        return rows > 0;
    }

    public List<Note> getFilteredNotes(String query, long categoryFilterId) {
        List<Note> notes = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        StringBuilder sql = new StringBuilder(
                "SELECT n." + COL_NOTE_ID + ", n." + COL_NOTE_TITLE + ", n." + COL_NOTE_CONTENT + ", n." + COL_NOTE_CAT_ID + ", c." + COL_CAT_NAME + ", n." + COL_NOTE_TIMESTAMP +
                        " FROM " + TABLE_NOTES + " n " +
                        " LEFT JOIN " + TABLE_CATEGORIES + " c ON n." + COL_NOTE_CAT_ID + " = c." + COL_CAT_ID + " WHERE 1=1"
        );

        List<String> selectionArgs = new ArrayList<>();

        if (categoryFilterId > 0) {
            sql.append(" AND n.").append(COL_NOTE_CAT_ID).append(" = ?");
            selectionArgs.add(String.valueOf(categoryFilterId));
        }

        if (query != null && !query.trim().isEmpty()) {
            sql.append(" AND (n.").append(COL_NOTE_TITLE).append(" LIKE ? OR n.").append(COL_NOTE_CONTENT).append(" LIKE ?)");
            selectionArgs.add("%" + query + "%");
            selectionArgs.add("%" + query + "%");
        }

        sql.append(" ORDER BY n.").append(COL_NOTE_ID).append(" DESC");

        Cursor cursor = db.rawQuery(sql.toString(), selectionArgs.toArray(new String[0]));
        if (cursor.moveToFirst()) {
            do {
                long noteId = cursor.getLong(cursor.getColumnIndexOrThrow(COL_NOTE_ID));
                String title = cursor.getString(cursor.getColumnIndexOrThrow(COL_NOTE_TITLE));
                String content = cursor.getString(cursor.getColumnIndexOrThrow(COL_NOTE_CONTENT));
                long catId = cursor.getLong(cursor.getColumnIndexOrThrow(COL_NOTE_CAT_ID));
                String catName = cursor.getString(cursor.getColumnIndexOrThrow(COL_CAT_NAME));
                String ts = cursor.getString(cursor.getColumnIndexOrThrow(COL_NOTE_TIMESTAMP));

                if (catName == null) {
                    catName = "Uncategorized";
                }

                notes.add(new Note(noteId, title, content, catId, catName, ts));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return notes;
    }
}
