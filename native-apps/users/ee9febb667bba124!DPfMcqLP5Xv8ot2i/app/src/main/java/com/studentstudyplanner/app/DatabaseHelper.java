package com.studentstudyplanner.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "study_planner.db";
    private static final int DATABASE_VERSION = 1;

    // SUBJECTS TABLE
    public static final String TABLE_SUBJECTS = "subjects";
    public static final String COL_SUB_ID = "id";
    public static final String COL_SUB_NAME = "name";
    public static final String COL_SUB_COLOR = "color";

    // TASKS TABLE
    public static final String TABLE_TASKS = "tasks";
    public static final String COL_TASK_ID = "id";
    public static final String COL_TASK_SUBJECT_ID = "subject_id";
    public static final String COL_TASK_TITLE = "title";
    public static final String COL_TASK_DUE = "due_date";
    public static final String COL_TASK_HOURS = "hours";
    public static final String COL_TASK_COMPLETED = "completed"; // 0 for incomplete, 1 for completed

    // REMINDERS TABLE
    public static final String TABLE_REMINDERS = "reminders";
    public static final String COL_REM_ID = "id";
    public static final String COL_REM_TITLE = "title";
    public static final String COL_REM_TIME = "time_millis";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create Subjects Table
        db.execSQL("CREATE TABLE " + TABLE_SUBJECTS + " (" +
                COL_SUB_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_SUB_NAME + " TEXT, " +
                COL_SUB_COLOR + " TEXT)");

        // Create Tasks Table
        db.execSQL("CREATE TABLE " + TABLE_TASKS + " (" +
                COL_TASK_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_TASK_SUBJECT_ID + " INTEGER, " +
                COL_TASK_TITLE + " TEXT, " +
                COL_TASK_DUE + " TEXT, " +
                COL_TASK_HOURS + " REAL, " +
                COL_TASK_COMPLETED + " INTEGER DEFAULT 0)");

        // Create Reminders Table
        db.execSQL("CREATE TABLE " + TABLE_REMINDERS + " (" +
                COL_REM_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_REM_TITLE + " TEXT, " +
                COL_REM_TIME + " INTEGER)");

        // Insert initial fallback default subjects
        db.execSQL("INSERT INTO " + TABLE_SUBJECTS + " (" + COL_SUB_NAME + ", " + COL_SUB_COLOR + ") VALUES ('Mathematics', '#E57373')");
        db.execSQL("INSERT INTO " + TABLE_SUBJECTS + " (" + COL_SUB_NAME + ", " + COL_SUB_COLOR + ") VALUES ('Computer Science', '#64B5F6')");
        db.execSQL("INSERT INTO " + TABLE_SUBJECTS + " (" + COL_SUB_NAME + ", " + COL_SUB_COLOR + ") VALUES ('Physics', '#81C784')");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SUBJECTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TASKS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_REMINDERS);
        onCreate(db);
    }

    // SUBJECT DB ACTIONS
    public boolean addSubject(String name, String color) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_SUB_NAME, name);
        cv.put(COL_SUB_COLOR, color);
        long res = db.insert(TABLE_SUBJECTS, null, cv);
        return res != -1;
    }

    public Cursor getAllSubjects() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_SUBJECTS + " ORDER BY " + COL_SUB_NAME + " ASC", null);
    }

    public void deleteSubject(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_SUBJECTS, COL_SUB_ID + "=?", new String[]{String.valueOf(id)});
        // Cascade delete tasks corresponding to subject
        db.delete(TABLE_TASKS, COL_TASK_SUBJECT_ID + "=?", new String[]{String.valueOf(id)});
    }

    // TASK DB ACTIONS
    public boolean addTask(int subjectId, String title, String dueDate, double hours) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_TASK_SUBJECT_ID, subjectId);
        cv.put(COL_TASK_TITLE, title);
        cv.put(COL_TASK_DUE, dueDate);
        cv.put(COL_TASK_HOURS, hours);
        cv.put(COL_TASK_COMPLETED, 0);
        long res = db.insert(TABLE_TASKS, null, cv);
        return res != -1;
    }

    public Cursor getAllTasks() {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT t.*, s." + COL_SUB_NAME + ", s." + COL_SUB_COLOR + " FROM " + TABLE_TASKS + " t LEFT JOIN " +
                TABLE_SUBJECTS + " s ON t." + COL_TASK_SUBJECT_ID + " = s." + COL_SUB_ID;
        return db.rawQuery(query, null);
    }

    public Cursor getTasksBySubject(int subjectId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT t.*, s." + COL_SUB_NAME + ", s." + COL_SUB_COLOR + " FROM " + TABLE_TASKS + " t LEFT JOIN " +
                TABLE_SUBJECTS + " s ON t." + COL_TASK_SUBJECT_ID + " = s." + COL_SUB_ID + " WHERE t." + COL_TASK_SUBJECT_ID + " = ?";
        return db.rawQuery(query, new String[]{String.valueOf(subjectId)});
    }

    public void updateTaskCompletion(int id, boolean isCompleted) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_TASK_COMPLETED, isCompleted ? 1 : 0);
        db.update(TABLE_TASKS, cv, COL_TASK_ID + "=?", new String[]{String.valueOf(id)});
    }

    public void deleteTask(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_TASKS, COL_TASK_ID + "=?", new String[]{String.valueOf(id)});
    }

    // REMINDERS DB ACTIONS
    public boolean addReminder(String title, long timeMillis) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_REM_TITLE, title);
        cv.put(COL_REM_TIME, timeMillis);
        long res = db.insert(TABLE_REMINDERS, null, cv);
        return res != -1;
    }

    public Cursor getAllReminders() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_REMINDERS + " ORDER BY " + COL_REM_TIME + " ASC", null);
    }

    public void deleteReminder(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_REMINDERS, COL_REM_ID + "=?", new String[]{String.valueOf(id)});
    }
}