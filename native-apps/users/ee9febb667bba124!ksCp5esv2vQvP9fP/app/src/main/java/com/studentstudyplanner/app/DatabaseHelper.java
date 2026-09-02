package com.studentstudyplanner.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "study_planner.db";
    private static final int DATABASE_VERSION = 1;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE subjects (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT UNIQUE, color TEXT)");
        db.execSQL("CREATE TABLE tasks (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT, description TEXT, due_date TEXT, subject_id INTEGER, completed INTEGER DEFAULT 0, priority TEXT)");

        // Default subjects to start
        db.execSQL("INSERT INTO subjects (name, color) VALUES ('Mathematics', '#FF3B30')");
        db.execSQL("INSERT INTO subjects (name, color) VALUES ('Science', '#34C759')");
        db.execSQL("INSERT INTO subjects (name, color) VALUES ('History', '#007AFF')");
        db.execSQL("INSERT INTO subjects (name, color) VALUES ('Literature', '#AF52DE')");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS tasks");
        db.execSQL("DROP TABLE IF EXISTS subjects");
        onCreate(db);
    }

    public long insertSubject(String name, String color) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("color", color);
        long id = db.insert("subjects", null, values);
        db.close();
        return id;
    }

    public Cursor getAllSubjects() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM subjects", null);
    }

    public void deleteSubject(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("subjects", "id = ?", new String[]{String.valueOf(id)});
        db.delete("tasks", "subject_id = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    public long insertTask(String title, String desc, String dueDate, long subjectId, String priority) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("title", title);
        values.put("description", desc);
        values.put("due_date", dueDate);
        values.put("subject_id", subjectId);
        values.put("completed", 0);
        values.put("priority", priority);
        long id = db.insert("tasks", null, values);
        db.close();
        return id;
    }

    public Cursor getAllTasks() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT tasks.*, subjects.name as subj_name, subjects.color as subj_color FROM tasks LEFT JOIN subjects ON tasks.subject_id = subjects.id ORDER BY tasks.id DESC", null);
    }

    public Cursor getTasksBySubject(long subjectId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT tasks.*, subjects.name as subj_name, subjects.color as subj_color FROM tasks LEFT JOIN subjects ON tasks.subject_id = subjects.id WHERE tasks.subject_id = ? ORDER BY tasks.id DESC", new String[]{String.valueOf(subjectId)});
    }

    public void updateTaskCompletion(long id, boolean completed) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("completed", completed ? 1 : 0);
        db.update("tasks", values, "id = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    public void deleteTask(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("tasks", "id = ?", new String[]{String.valueOf(id)});
        db.close();
    }
}