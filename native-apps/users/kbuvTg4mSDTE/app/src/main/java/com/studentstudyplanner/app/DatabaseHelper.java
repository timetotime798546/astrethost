package com.studentstudyplanner.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "student_study_planner_db.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_SUBJECTS = "subjects";
    public static final String COL_SUBJECT_ID = "id";
    public static final String COL_SUBJECT_NAME = "name";

    public static final String TABLE_TASKS = "tasks";
    public static final String COL_TASK_ID = "id";
    public static final String COL_TASK_SUBJECT_ID = "subject_id";
    public static final String COL_TASK_TITLE = "title";
    public static final String COL_TASK_DUE_DATE = "due_date";
    public static final String COL_TASK_PRIORITY = "priority";
    public static final String COL_TASK_COMPLETED = "completed";

    public static final String TABLE_REMINDERS = "reminders";
    public static final String COL_REMINDER_ID = "id";
    public static final String COL_REMINDER_TITLE = "title";
    public static final String COL_REMINDER_TIME = "time";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createSubjectsTable = "CREATE TABLE " + TABLE_SUBJECTS + " (" +
                COL_SUBJECT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_SUBJECT_NAME + " TEXT NOT NULL)";

        String createTasksTable = "CREATE TABLE " + TABLE_TASKS + " (" +
                COL_TASK_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_TASK_SUBJECT_ID + " INTEGER, " +
                COL_TASK_TITLE + " TEXT NOT NULL, " +
                COL_TASK_DUE_DATE + " TEXT, " +
                COL_TASK_PRIORITY + " TEXT, " +
                COL_TASK_COMPLETED + " INTEGER DEFAULT 0)";

        String createRemindersTable = "CREATE TABLE " + TABLE_REMINDERS + " (" +
                COL_REMINDER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_REMINDER_TITLE + " TEXT NOT NULL, " +
                COL_REMINDER_TIME + " TEXT NOT NULL)";

        db.execSQL(createSubjectsTable);
        db.execSQL(createTasksTable);
        db.execSQL(createRemindersTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SUBJECTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TASKS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_REMINDERS);
        onCreate(db);
    }

    public long insertSubject(String name) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_SUBJECT_NAME, name);
        return db.insert(TABLE_SUBJECTS, null, values);
    }

    public long insertTask(int subjectId, String title, String dueDate, String priority) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_TASK_SUBJECT_ID, subjectId);
        values.put(COL_TASK_TITLE, title);
        values.put(COL_TASK_DUE_DATE, dueDate);
        values.put(COL_TASK_PRIORITY, priority);
        values.put(COL_TASK_COMPLETED, 0);
        return db.insert(TABLE_TASKS, null, values);
    }

    public long insertReminder(String title, String time) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_REMINDER_TITLE, title);
        values.put(COL_REMINDER_TIME, time);
        return db.insert(TABLE_REMINDERS, null, values);
    }

    public Cursor getAllSubjects() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_SUBJECTS + " ORDER BY " + COL_SUBJECT_NAME + " ASC", null);
    }

    public Cursor getTasksBySubject(int subjectId) {
        SQLiteDatabase db = this.getReadableDatabase();
        if (subjectId == -1) {
            return db.rawQuery("SELECT * FROM " + TABLE_TASKS + " ORDER BY " + COL_TASK_ID + " DESC", null);
        } else {
            return db.rawQuery("SELECT * FROM " + TABLE_TASKS + " WHERE " + COL_TASK_SUBJECT_ID + " = ? ORDER BY " + COL_TASK_ID + " DESC", new String[]{String.valueOf(subjectId)});
        }
    }

    public Cursor getAllReminders() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_REMINDERS + " ORDER BY " + COL_REMINDER_ID + " DESC", null);
    }

    public String getSubjectNameById(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + COL_SUBJECT_NAME + " FROM " + TABLE_SUBJECTS + " WHERE " + COL_SUBJECT_ID + " = ?", new String[]{String.valueOf(id)});
        String name = "General";
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                name = cursor.getString(0);
            }
            cursor.close();
        }
        return name;
    }

    public int updateTaskStatus(int taskId, int completed) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_TASK_COMPLETED, completed);
        return db.update(TABLE_TASKS, values, COL_TASK_ID + " = ?", new String[]{String.valueOf(taskId)});
    }

    public void deleteSubject(int subjectId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_SUBJECTS, COL_SUBJECT_ID + " = ?", new String[]{String.valueOf(subjectId)});
        db.delete(TABLE_TASKS, COL_TASK_SUBJECT_ID + " = ?", new String[]{String.valueOf(subjectId)});
    }

    public void deleteTask(int taskId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_TASKS, COL_TASK_ID + " = ?", new String[]{String.valueOf(taskId)});
    }

    public void deleteReminder(int reminderId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_REMINDERS, COL_REMINDER_ID + " = ?", new String[]{String.valueOf(reminderId)});
    }

    public int getTotalTasksCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_TASKS, null);
        int count = 0;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
            cursor.close();
        }
        return count;
    }

    public int getCompletedTasksCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_TASKS + " WHERE " + COL_TASK_COMPLETED + " = 1", null);
        int count = 0;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
            cursor.close();
        }
        return count;
    }

    public int getTasksCountBySubject(int subjectId, boolean completedOnly) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query;
        if (completedOnly) {
            query = "SELECT COUNT(*) FROM " + TABLE_TASKS + " WHERE " + COL_TASK_SUBJECT_ID + " = ? AND " + COL_TASK_COMPLETED + " = 1";
        } else {
            query = "SELECT COUNT(*) FROM " + TABLE_TASKS + " WHERE " + COL_TASK_SUBJECT_ID + " = ?";
        }
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(subjectId)});
        int count = 0;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
            cursor.close();
        }
        return count;
    }
}