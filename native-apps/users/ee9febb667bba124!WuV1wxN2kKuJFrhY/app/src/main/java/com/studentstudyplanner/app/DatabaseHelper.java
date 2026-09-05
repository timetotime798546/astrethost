package com.studentstudyplanner.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "study_planner.db";
    private static final int DATABASE_VERSION = 1;

    // Subjects Table
    public static final String TABLE_SUBJECTS = "subjects";
    public static final String COL_SUB_ID = "id";
    public static final String COL_SUB_NAME = "name";

    // Tasks Table
    public static final String TABLE_TASKS = "tasks";
    public static final String COL_TASK_ID = "id";
    public static final String COL_TASK_SUB_ID = "subject_id";
    public static final String COL_TASK_TITLE = "title";
    public static final String COL_TASK_DESC = "description";
    public static final String COL_TASK_DUE = "due_date";
    public static final String COL_TASK_STATUS = "is_completed"; // 0 for active, 1 for completed

    // Reminders Table
    public static final String TABLE_REMINDERS = "reminders";
    public static final String COL_REM_ID = "id";
    public static final String COL_REM_TITLE = "title";
    public static final String COL_REM_TIME = "time_millis";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createSubjects = "CREATE TABLE " + TABLE_SUBJECTS + " (" +
                COL_SUB_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_SUB_NAME + " TEXT UNIQUE NOT NULL)";

        String createTasks = "CREATE TABLE " + TABLE_TASKS + " (" +
                COL_TASK_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_TASK_SUB_ID + " INTEGER, " +
                COL_TASK_TITLE + " TEXT NOT NULL, " +
                COL_TASK_DESC + " TEXT, " +
                COL_TASK_DUE + " TEXT, " +
                COL_TASK_STATUS + " INTEGER DEFAULT 0, " +
                "FOREIGN KEY(" + COL_TASK_SUB_ID + ") REFERENCES " + TABLE_SUBJECTS + "(" + COL_SUB_ID + ") ON DELETE CASCADE)";

        String createReminders = "CREATE TABLE " + TABLE_REMINDERS + " (" +
                COL_REM_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_REM_TITLE + " TEXT NOT NULL, " +
                COL_REM_TIME + " INTEGER NOT NULL)";

        db.execSQL(createSubjects);
        db.execSQL(createTasks);
        db.execSQL(createReminders);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TASKS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SUBJECTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_REMINDERS);
        onCreate(db);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }
}