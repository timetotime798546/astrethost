package com.omnicontrolsuite.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "omnicontrol.db";
    private static final int DATABASE_VERSION = 1;

    // Tasks table
    public static final String TABLE_TASKS = "tasks";
    public static final String COL_TASK_ID = "id";
    public static final String COL_TASK_TITLE = "title";
    public static final String COL_TASK_STATUS = "status";

    // Notes table
    public static final String TABLE_NOTES = "notes";
    public static final String COL_NOTE_ID = "id";
    public static final String COL_NOTE_TITLE = "title";
    public static final String COL_NOTE_CONTENT = "content";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTasks = "CREATE TABLE " + TABLE_TASKS + " (" +
                COL_TASK_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_TASK_TITLE + " TEXT, " +
                COL_TASK_STATUS + " INTEGER DEFAULT 0)";
        
        String createNotes = "CREATE TABLE " + TABLE_NOTES + " (" +
                COL_NOTE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_NOTE_TITLE + " TEXT, " +
                COL_NOTE_CONTENT + " TEXT)";

        db.execSQL(createTasks);
        db.execSQL(createNotes);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TASKS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOTES);
        onCreate(db);
    }

    // Task Operations
    public long insertTask(String title) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_TASK_TITLE, title);
        values.put(COL_TASK_STATUS, 0);
        long result = db.insert(TABLE_TASKS, null, values);
        db.close();
        return result;
    }

    public Cursor getAllTasks() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_TASKS + " ORDER BY " + COL_TASK_ID + " DESC", null);
    }

    public void updateTaskStatus(int id, int status) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_TASK_STATUS, status);
        db.update(TABLE_TASKS, values, COL_TASK_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
    }

    public void deleteTask(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_TASKS, COL_TASK_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
    }

    public void purgeTasks() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_TASKS, null, null);
        db.close();
    }

    // Notes Operations
    public long insertNote(String title, String content) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_NOTE_TITLE, title);
        values.put(COL_NOTE_CONTENT, content);
        long result = db.insert(TABLE_NOTES, null, values);
        db.close();
        return result;
    }

    public Cursor getAllNotes() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_NOTES + " ORDER BY " + COL_NOTE_ID + " DESC", null);
    }

    public void updateNote(int id, String title, String content) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_NOTE_TITLE, title);
        values.put(COL_NOTE_CONTENT, content);
        db.update(TABLE_NOTES, values, COL_NOTE_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
    }

    public void deleteNote(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NOTES, COL_NOTE_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
    }

    public void purgeNotes() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NOTES, null, null);
        db.close();
    }
}