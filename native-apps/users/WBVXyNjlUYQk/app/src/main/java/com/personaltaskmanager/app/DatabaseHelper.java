package com.personaltaskmanager.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "task_manager.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_TASKS = "tasks";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_DESCRIPTION = "description";
    public static final String COLUMN_PRIORITY = "priority";
    public static final String COLUMN_DUE_DATE = "due_date";
    public static final String COLUMN_COMPLETED = "completed";

    private static final String TABLE_CREATE =
            "CREATE TABLE " + TABLE_TASKS + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_TITLE + " TEXT NOT NULL, " +
                    COLUMN_DESCRIPTION + " TEXT, " +
                    COLUMN_PRIORITY + " TEXT, " +
                    COLUMN_DUE_DATE + " TEXT, " +
                    COLUMN_COMPLETED + " INTEGER DEFAULT 0" +
                    ");";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TASKS);
        onCreate(db);
    }

    public long insertTask(String title, String description, String priority, String dueDate) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, title);
        values.put(COLUMN_DESCRIPTION, description);
        values.put(COLUMN_PRIORITY, priority);
        values.put(COLUMN_DUE_DATE, dueDate);
        values.put(COLUMN_COMPLETED, 0);
        long id = db.insert(TABLE_TASKS, null, values);
        db.close();
        return id;
    }

    public int updateTask(long id, String title, String description, String priority, String dueDate, boolean completed) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, title);
        values.put(COLUMN_DESCRIPTION, description);
        values.put(COLUMN_PRIORITY, priority);
        values.put(COLUMN_DUE_DATE, dueDate);
        values.put(COLUMN_COMPLETED, completed ? 1 : 0);
        int rows = db.update(TABLE_TASKS, values, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
        return rows;
    }

    public int setTaskCompletion(long id, boolean completed) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_COMPLETED, completed ? 1 : 0);
        int rows = db.update(TABLE_TASKS, values, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
        return rows;
    }

    public int deleteTask(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rows = db.delete(TABLE_TASKS, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
        return rows;
    }

    public Task getTask(long id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_TASKS, null, COLUMN_ID + " = ?", new String[]{String.valueOf(id)}, null, null, null);
        Task task = null;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                task = new Task(
                        cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PRIORITY)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DUE_DATE)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_COMPLETED)) == 1
                );
            }
            cursor.close();
        }
        db.close();
        return task;
    }

    public List<Task> getFilteredTasks(String searchWord, String filterMode) {
        List<Task> tasks = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        StringBuilder selection = new StringBuilder();
        List<String> selectionArgs = new ArrayList<>();

        // Handle Search
        if (searchWord != null && !searchWord.trim().isEmpty()) {
            selection.append(COLUMN_TITLE).append(" LIKE ?");
            selectionArgs.add("%" + searchWord.trim() + "%");
        }

        // Handle Status Filter
        if (filterMode != null) {
            if (selection.length() > 0) {
                selection.append(" AND ");
            }
            
            if (filterMode.equals("Pending")) {
                selection.append(COLUMN_COMPLETED).append(" = 0");
            } else if (filterMode.equals("Completed")) {
                selection.append(COLUMN_COMPLETED).append(" = 1");
            } else if (filterMode.equals("High Priority")) {
                selection.append(COLUMN_PRIORITY).append(" = 'High'");
            } else {
                // "All" - no extra condition needed
                if (searchWord == null || searchWord.trim().isEmpty()) {
                    selection = new StringBuilder(); // Reset to empty
                } else {
                    selection.delete(selection.length() - 5, selection.length()); // Trim " AND "
                }
            }
        }

        String selectionStr = selection.length() > 0 ? selection.toString() : null;
        String[] selectionArgsArray = selectionArgs.isEmpty() ? null : selectionArgs.toArray(new String[0]);

        // Order by due date ascending
        Cursor cursor = db.query(TABLE_TASKS, null, selectionStr, selectionArgsArray, null, null, COLUMN_DUE_DATE + " ASC");
        
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    Task task = new Task(
                            cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PRIORITY)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DUE_DATE)),
                            cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_COMPLETED)) == 1
                    );
                    tasks.add(task);
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        db.close();
        return tasks;
    }

    public Stats getStats(String todayDateString) {
        SQLiteDatabase db = this.getReadableDatabase();
        Stats stats = new Stats();

        // Total
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_TASKS, null);
        if (cursor.moveToFirst()) stats.total = cursor.getInt(0);
        cursor.close();

        // Completed
        cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_TASKS + " WHERE " + COLUMN_COMPLETED + " = 1", null);
        if (cursor.moveToFirst()) stats.completed = cursor.getInt(0);
        cursor.close();

        // Pending
        cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_TASKS + " WHERE " + COLUMN_COMPLETED + " = 0", null);
        if (cursor.moveToFirst()) stats.pending = cursor.getInt(0);
        cursor.close();

        // Today
        cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_TASKS + " WHERE " + COLUMN_DUE_DATE + " = ?", new String[]{todayDateString});
        if (cursor.moveToFirst()) stats.today = cursor.getInt(0);
        cursor.close();

        db.close();
        return stats;
    }

    public static class Stats {
        public int total = 0;
        public int completed = 0;
        public int pending = 0;
        public int today = 0;
    }
}