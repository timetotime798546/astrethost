package com.studyplannerpro.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "study_planner.db";
    private static final int DATABASE_VERSION = 1;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE subjects (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL," +
                "color INTEGER NOT NULL)");

        db.execSQL("CREATE TABLE tasks (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "subject_id INTEGER NOT NULL," +
                "title TEXT NOT NULL," +
                "description TEXT," +
                "due_date TEXT," +
                "status INTEGER DEFAULT 0," + 
                "priority TEXT," +
                "reminder_time TEXT," +
                "FOREIGN KEY(subject_id) REFERENCES subjects(id) ON DELETE CASCADE)");

        db.execSQL("CREATE TABLE study_sessions (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "subject_id INTEGER NOT NULL," +
                "duration INTEGER NOT NULL," + 
                "session_date TEXT NOT NULL," +
                "FOREIGN KEY(subject_id) REFERENCES subjects(id) ON DELETE CASCADE)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS study_sessions");
        db.execSQL("DROP TABLE IF EXISTS tasks");
        db.execSQL("DROP TABLE IF EXISTS subjects");
        onCreate(db);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    public long insertSubject(String name, int color) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("color", color);
        return db.insert("subjects", null, values);
    }

    public Cursor getAllSubjects() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM subjects ORDER BY name ASC", null);
    }

    public void deleteSubject(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("subjects", "id = ?", new String[]{String.valueOf(id)});
    }

    public long insertTask(long subjectId, String title, String desc, String dueDate, String priority, String reminder) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("subject_id", subjectId);
        values.put("title", title);
        values.put("description", desc);
        values.put("due_date", dueDate);
        values.put("status", 0);
        values.put("priority", priority);
        values.put("reminder_time", reminder);
        return db.insert("tasks", null, values);
    }

    public Cursor getTasksFiltered(String filter) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT tasks.*, subjects.name AS subject_name, subjects.color AS subject_color " +
                "FROM tasks JOIN subjects ON tasks.subject_id = subjects.id ";
        if ("Pending".equals(filter)) {
            query += "WHERE tasks.status = 0 ";
        } else if ("Completed".equals(filter)) {
            query += "WHERE tasks.status = 1 ";
        }
        query += "ORDER BY tasks.id DESC";
        return db.rawQuery(query, null);
    }

    public void updateTaskStatus(long taskId, int status) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("status", status);
        db.update("tasks", values, "id = ?", new String[]{String.valueOf(taskId)});
    }

    public void deleteTask(long taskId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("tasks", "id = ?", new String[]{String.valueOf(taskId)});
    }

    public long insertStudySession(long subjectId, int duration, String date) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("subject_id", subjectId);
        values.put("duration", duration);
        values.put("session_date", date);
        return db.insert("study_sessions", null, values);
    }

    public int getTotalStudyMinutes() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT SUM(duration) FROM study_sessions", null);
        int total = 0;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                total = cursor.getInt(0);
            }
            cursor.close();
        }
        return total;
    }

    public int getTaskCountByStatus(int status) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM tasks WHERE status = " + status, null);
        int count = 0;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
            cursor.close();
        }
        return count;
    }

    public int getTotalTasksCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM tasks", null);
        int count = 0;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
            cursor.close();
        }
        return count;
    }

    public List<Map<String, Object>> getSubjectBreakdown() {
        List<Map<String, Object>> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        Cursor subCursor = db.rawQuery("SELECT * FROM subjects", null);
        if (subCursor != null) {
            while (subCursor.moveToNext()) {
                long subId = subCursor.getLong(subCursor.getColumnIndexOrThrow("id"));
                String name = subCursor.getString(subCursor.getColumnIndexOrThrow("name"));
                int color = subCursor.getInt(subCursor.getColumnIndexOrThrow("color"));

                int loggedMinutes = 0;
                Cursor timeCursor = db.rawQuery("SELECT SUM(duration) FROM study_sessions WHERE subject_id = " + subId, null);
                if (timeCursor != null) {
                    if (timeCursor.moveToFirst()) {
                        loggedMinutes = timeCursor.getInt(0);
                    }
                    timeCursor.close();
                }

                int doneTasks = 0;
                Cursor taskDoneCursor = db.rawQuery("SELECT COUNT(*) FROM tasks WHERE subject_id = " + subId + " AND status = 1", null);
                if (taskDoneCursor != null) {
                    if (taskDoneCursor.moveToFirst()) {
                        doneTasks = taskDoneCursor.getInt(0);
                    }
                    taskDoneCursor.close();
                }

                int totalTasks = 0;
                Cursor taskTotalCursor = db.rawQuery("SELECT COUNT(*) FROM tasks WHERE subject_id = " + subId, null);
                if (taskTotalCursor != null) {
                    if (taskTotalCursor.moveToFirst()) {
                        totalTasks = taskTotalCursor.getInt(0);
                    }
                    taskTotalCursor.close();
                }

                Map<String, Object> map = new HashMap<>();
                map.put("id", subId);
                map.put("name", name);
                map.put("color", color);
                map.put("minutes", loggedMinutes);
                map.put("done_tasks", doneTasks);
                map.put("total_tasks", totalTasks);
                list.add(map);
            }
            subCursor.close();
        }
        return list;
    }
}