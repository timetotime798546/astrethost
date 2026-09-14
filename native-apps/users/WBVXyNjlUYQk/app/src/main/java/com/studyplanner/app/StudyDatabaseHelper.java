package com.studyplanner.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudyDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "study_planner.db";
    private static final int DATABASE_VERSION = 1;

    public StudyDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Table 1: Subjects
        db.execSQL("CREATE TABLE subjects (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT, " +
                "color TEXT, " +
                "hours INTEGER)");

        // Table 2: Tasks
        db.execSQL("CREATE TABLE tasks (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "subject_id INTEGER, " +
                "title TEXT, " +
                "due_date TEXT, " +
                "priority TEXT, " +
                "is_completed INTEGER)");

        // Table 3: Reminders
        db.execSQL("CREATE TABLE reminders (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "title TEXT, " +
                "time_stamp LONG, " +
                "is_active INTEGER)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS subjects");
        db.execSQL("DROP TABLE IF EXISTS tasks");
        db.execSQL("DROP TABLE IF EXISTS reminders");
        onCreate(db);
    }

    // SUBJECT DATA MUTATIONS
    public long insertSubject(String name, String color, int hours) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("color", color);
        values.put("hours", hours);
        return db.insert("subjects", null, values);
    }

    public Cursor getAllSubjects() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM subjects ORDER BY id DESC", null);
    }

    public void deleteSubject(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("subjects", "id = ?", new String[]{String.valueOf(id)});
        // Cascade delete tasks linked to subject
        db.delete("tasks", "subject_id = ?", new String[]{String.valueOf(id)});
    }

    // TASK DATA MUTATIONS
    public long insertTask(int subjectId, String title, String dueDate, String priority) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("subject_id", subjectId);
        values.put("title", title);
        values.put("due_date", dueDate);
        values.put("priority", priority);
        values.put("is_completed", 0);
        return db.insert("tasks", null, values);
    }

    public Cursor getAllTasks() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT tasks.*, subjects.name AS subj_name, subjects.color AS subj_color " +
                "FROM tasks LEFT JOIN subjects ON tasks.subject_id = subjects.id " +
                "ORDER BY tasks.is_completed ASC, tasks.id DESC", null);
    }

    public Cursor getTasksFiltered(String priorityFilter) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT tasks.*, subjects.name AS subj_name, subjects.color AS subj_color " +
                "FROM tasks LEFT JOIN subjects ON tasks.subject_id = subjects.id " +
                "WHERE tasks.priority = ? " +
                "ORDER BY tasks.is_completed ASC, tasks.id DESC", new String[]{priorityFilter});
    }

    public void updateTaskStatus(int id, int isCompleted) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("is_completed", isCompleted);
        db.update("tasks", values, "id = ?", new String[]{String.valueOf(id)});
    }

    public void deleteTask(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("tasks", "id = ?", new String[]{String.valueOf(id)});
    }

    // REMINDERS DATA MUTATIONS
    public long insertReminder(String title, long timeStamp) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("title", title);
        values.put("time_stamp", timeStamp);
        values.put("is_active", 1);
        return db.insert("reminders", null, values);
    }

    public Cursor getAllReminders() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM reminders ORDER BY time_stamp ASC", null);
    }

    public void deleteReminder(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("reminders", "id = ?", new String[]{String.valueOf(id)});
    }
}