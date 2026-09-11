package com.studyplannerpro.app;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.ContentValues;
import android.database.Cursor;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "study_planner.db";
    private static final int DATABASE_VERSION = 1;

    // Subject Table
    public static final String TABLE_SUBJECTS = "subjects";
    public static final String COL_SUB_ID = "id";
    public static final String COL_SUB_NAME = "name";
    public static final String COL_SUB_CODE = "code";

    // Task Table
    public static final String TABLE_TASKS = "tasks";
    public static final String COL_TASK_ID = "id";
    public static final String COL_TASK_SUB_ID = "subject_id";
    public static final String COL_TASK_TITLE = "title";
    public static final String COL_TASK_DUE_DATE = "due_date";
    public static final String COL_TASK_STATUS = "status"; // 0 for pending, 1 for completed
    public static final String COL_TASK_PRIORITY = "priority"; // High, Medium, Low
    public static final String COL_TASK_HOURS = "hours";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createSubjectsTable = "CREATE TABLE " + TABLE_SUBJECTS + "("
                + COL_SUB_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COL_SUB_NAME + " TEXT,"
                + COL_SUB_CODE + " TEXT" + ")";

        String createTasksTable = "CREATE TABLE " + TABLE_TASKS + "("
                + COL_TASK_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COL_TASK_SUB_ID + " INTEGER,"
                + COL_TASK_TITLE + " TEXT,"
                + COL_TASK_DUE_DATE + " TEXT,"
                + COL_TASK_STATUS + " INTEGER DEFAULT 0,"
                + COL_TASK_PRIORITY + " TEXT,"
                + COL_TASK_HOURS + " INTEGER,"
                + "FOREIGN KEY(" + COL_TASK_SUB_ID + ") REFERENCES " + TABLE_SUBJECTS + "(" + COL_SUB_ID + ") ON DELETE CASCADE" + ")";

        db.execSQL(createSubjectsTable);
        db.execSQL(createTasksTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TASKS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SUBJECTS);
        onCreate(db);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    // --- SUBJECT METHODS ---
    
    public boolean addSubject(String name, String code) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_SUB_NAME, name);
        values.put(COL_SUB_CODE, code);
        long result = db.insert(TABLE_SUBJECTS, null, values);
        return result != -1;
    }

    public List<Subject> getAllSubjects() {
        List<Subject> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_SUBJECTS + " ORDER BY " + COL_SUB_CODE + " ASC", null);
        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_SUB_ID));
                String name = cursor.getString(cursor.getColumnIndexOrThrow(COL_SUB_NAME));
                String code = cursor.getString(cursor.getColumnIndexOrThrow(COL_SUB_CODE));
                list.add(new Subject(id, name, code));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    public void deleteSubject(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_SUBJECTS, COL_SUB_ID + "=?", new String[]{String.valueOf(id)});
    }

    // --- TASK METHODS ---

    public boolean addTask(int subjectId, String title, String dueDate, String priority, int hours) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_TASK_SUB_ID, subjectId);
        values.put(COL_TASK_TITLE, title);
        values.put(COL_TASK_DUE_DATE, dueDate);
        values.put(COL_TASK_STATUS, 0); // Pending by default
        values.put(COL_TASK_PRIORITY, priority);
        values.put(COL_TASK_HOURS, hours);
        long result = db.insert(TABLE_TASKS, null, values);
        return result != -1;
    }

    public List<Task> getAllTasks(String statusFilter, int subjectFilterId) {
        List<Task> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        StringBuilder query = new StringBuilder();
        query.append("SELECT t.*, s.").append(COL_SUB_CODE);
        query.append(" FROM ").append(TABLE_TASKS).append(" t");
        query.append(" LEFT JOIN ").append(TABLE_SUBJECTS).append(" s");
        query.append(" ON t.").append(COL_TASK_SUB_ID).append(" = s.").append(COL_SUB_ID);
        
        List<String> selectionArgs = new ArrayList<>();
        boolean hasWhere = false;

        if (statusFilter != null && !statusFilter.equals("All")) {
            query.append(" WHERE t.").append(COL_TASK_STATUS).append(" = ?");
            selectionArgs.add(statusFilter.equals("Completed") ? "1" : "0");
            hasWhere = true;
        }

        if (subjectFilterId > 0) {
            if (hasWhere) {
                query.append(" AND t.").append(COL_TASK_SUB_ID).append(" = ?");
            } else {
                query.append(" WHERE t.").append(COL_TASK_SUB_ID).append(" = ?");
            }
            selectionArgs.add(String.valueOf(subjectFilterId));
        }

        query.append(" ORDER BY t.").append(COL_TASK_ID).append(" DESC");

        Cursor cursor = db.rawQuery(query.toString(), selectionArgs.toArray(new String[0]));
        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_TASK_ID));
                int subId = cursor.getInt(cursor.getColumnIndexOrThrow(COL_TASK_SUB_ID));
                String subCode = cursor.getString(cursor.getColumnIndexOrThrow(COL_SUB_CODE));
                if (subCode == null) subCode = "No Subject";
                String title = cursor.getString(cursor.getColumnIndexOrThrow(COL_TASK_TITLE));
                String dueDate = cursor.getString(cursor.getColumnIndexOrThrow(COL_TASK_DUE_DATE));
                int status = cursor.getInt(cursor.getColumnIndexOrThrow(COL_TASK_STATUS));
                String priority = cursor.getString(cursor.getColumnIndexOrThrow(COL_TASK_PRIORITY));
                int hours = cursor.getInt(cursor.getColumnIndexOrThrow(COL_TASK_HOURS));
                
                list.add(new Task(id, subId, subCode, title, dueDate, status, priority, hours));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    public List<Task> getUpcomingTasks(int limit) {
        List<Task> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        String query = "SELECT t.*, s." + COL_SUB_CODE +
                " FROM " + TABLE_TASKS + " t" +
                " LEFT JOIN " + TABLE_SUBJECTS + " s" +
                " ON t." + COL_TASK_SUB_ID + " = s." + COL_SUB_ID +
                " WHERE t." + COL_TASK_STATUS + " = 0" + // Only Pending
                " ORDER BY t." + COL_TASK_DUE_DATE + " ASC LIMIT " + limit;

        Cursor cursor = db.rawQuery(query, null);
        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_TASK_ID));
                int subId = cursor.getInt(cursor.getColumnIndexOrThrow(COL_TASK_SUB_ID));
                String subCode = cursor.getString(cursor.getColumnIndexOrThrow(COL_SUB_CODE));
                if (subCode == null) subCode = "N/A";
                String title = cursor.getString(cursor.getColumnIndexOrThrow(COL_TASK_TITLE));
                String dueDate = cursor.getString(cursor.getColumnIndexOrThrow(COL_TASK_DUE_DATE));
                int status = cursor.getInt(cursor.getColumnIndexOrThrow(COL_TASK_STATUS));
                String priority = cursor.getString(cursor.getColumnIndexOrThrow(COL_TASK_PRIORITY));
                int hours = cursor.getInt(cursor.getColumnIndexOrThrow(COL_TASK_HOURS));
                
                list.add(new Task(id, subId, subCode, title, dueDate, status, priority, hours));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    public void updateTaskStatus(int taskId, int isCompleted) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_TASK_STATUS, isCompleted);
        db.update(TABLE_TASKS, values, COL_TASK_ID + "=?", new String[]{String.valueOf(taskId)});
    }

    public void deleteTask(int taskId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_TASKS, COL_TASK_ID + "=?", new String[]{String.valueOf(taskId)});
    }

    // --- STATISTICS METHODS ---

    public int getCount(String table) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + table, null);
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

    public int getCompletedTasksCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_TASKS + " WHERE " + COL_TASK_STATUS + " = 1", null);
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

    public int getTotalHoursPlanned() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT SUM(" + COL_TASK_HOURS + ") FROM " + TABLE_TASKS, null);
        int sum = 0;
        if (cursor.moveToFirst()) {
            sum = cursor.getInt(0);
        }
        cursor.close();
        return sum;
    }
}