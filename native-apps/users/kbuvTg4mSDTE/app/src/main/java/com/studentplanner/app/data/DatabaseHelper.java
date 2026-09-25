package com.studentplanner.app.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.studentplanner.app.models.Subject;
import com.studentplanner.app.models.Task;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "study_planner_db";

    // Subjects table
    private static final String TABLE_SUBJECTS = "subjects";
    private static final String KEY_SUBJECT_ID = "id";
    private static final String KEY_SUBJECT_NAME = "name";

    // Tasks table
    private static final String TABLE_TASKS = "tasks";
    private static final String KEY_TASK_ID = "id";
    private static final String KEY_TASK_SUBJECT_ID = "subject_id";
    private static final String KEY_TASK_TITLE = "title";
    private static final String KEY_TASK_DESCRIPTION = "description";
    private static final String KEY_TASK_DUE_DATE = "due_date"; // Stored as Unix timestamp (milliseconds)
    private static final String KEY_TASK_IS_COMPLETED = "is_completed"; // 0 for false, 1 for true

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_SUBJECTS_TABLE = "CREATE TABLE " + TABLE_SUBJECTS + "("
                + KEY_SUBJECT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_SUBJECT_NAME + " TEXT NOT NULL UNIQUE" + ")";
        db.execSQL(CREATE_SUBJECTS_TABLE);

        String CREATE_TASKS_TABLE = "CREATE TABLE " + TABLE_TASKS + "("
                + KEY_TASK_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_TASK_SUBJECT_ID + " INTEGER NOT NULL,"
                + KEY_TASK_TITLE + " TEXT NOT NULL,"
                + KEY_TASK_DESCRIPTION + " TEXT,"
                + KEY_TASK_DUE_DATE + " INTEGER,"
                + KEY_TASK_IS_COMPLETED + " INTEGER DEFAULT 0,"
                + "FOREIGN KEY(" + KEY_TASK_SUBJECT_ID + ") REFERENCES " + TABLE_SUBJECTS + "(" + KEY_SUBJECT_ID + ") ON DELETE CASCADE" + ")";
        db.execSQL(CREATE_TASKS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TASKS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SUBJECTS);
        onCreate(db);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        if (!db.isReadOnly()) {
            // Enable foreign key constraints
            db.execSQL("PRAGMA foreign_keys = ON;");
        }
    }

    // --- Subject operations ---

    public long addSubject(Subject subject) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_SUBJECT_NAME, subject.getName());
        long id = db.insert(TABLE_SUBJECTS, null, values);
        db.close();
        return id;
    }

    public Subject getSubject(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_SUBJECTS, new String[]{KEY_SUBJECT_ID,
                        KEY_SUBJECT_NAME}, KEY_SUBJECT_ID + "=?",
                new String[]{String.valueOf(id)}, null, null, null, null);
        if (cursor != null)
            cursor.moveToFirst();

        Subject subject = new Subject(Integer.parseInt(cursor.getString(0)),
                cursor.getString(1));
        cursor.close();
        db.close();
        return subject;
    }

    public ArrayList<Subject> getAllSubjects() {
        ArrayList<Subject> subjectList = new ArrayList<Subject>();
        String selectQuery = "SELECT * FROM " + TABLE_SUBJECTS + " ORDER BY " + KEY_SUBJECT_NAME + " ASC";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                Subject subject = new Subject();
                subject.setId(Integer.parseInt(cursor.getString(0)));
                subject.setName(cursor.getString(1));
                subjectList.add(subject);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return subjectList;
    }

    public int updateSubject(Subject subject) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_SUBJECT_NAME, subject.getName());
        int rowsAffected = db.update(TABLE_SUBJECTS, values, KEY_SUBJECT_ID + " = ?",
                new String[]{String.valueOf(subject.getId())});
        db.close();
        return rowsAffected;
    }

    public void deleteSubject(Subject subject) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_SUBJECTS, KEY_SUBJECT_ID + " = ?",
                new String[]{String.valueOf(subject.getId())});
        db.close();
    }

    public int getSubjectCount() {
        String countQuery = "SELECT * FROM " + TABLE_SUBJECTS;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        int count = cursor.getCount();
        cursor.close();
        db.close();
        return count;
    }

    public int getTaskCountForSubject(int subjectId) {
        String countQuery = "SELECT COUNT(*) FROM " + TABLE_TASKS + " WHERE " + KEY_TASK_SUBJECT_ID + " = " + subjectId;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        db.close();
        return count;
    }

    // --- Task operations ---

    public long addTask(Task task) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_TASK_SUBJECT_ID, task.getSubjectId());
        values.put(KEY_TASK_TITLE, task.getTitle());
        values.put(KEY_TASK_DESCRIPTION, task.getDescription());
        values.put(KEY_TASK_DUE_DATE, task.getDueDateMillis());
        values.put(KEY_TASK_IS_COMPLETED, task.isCompleted() ? 1 : 0);
        long id = db.insert(TABLE_TASKS, null, values);
        db.close();
        return id;
    }

    public Task getTask(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_TASKS, new String[]{KEY_TASK_ID,
                        KEY_TASK_SUBJECT_ID, KEY_TASK_TITLE, KEY_TASK_DESCRIPTION,
                        KEY_TASK_DUE_DATE, KEY_TASK_IS_COMPLETED}, KEY_TASK_ID + "=?",
                new String[]{String.valueOf(id)}, null, null, null, null);
        if (cursor != null)
            cursor.moveToFirst();

        Task task = new Task(
                Integer.parseInt(cursor.getString(0)),
                Integer.parseInt(cursor.getString(1)),
                cursor.getString(2),
                cursor.getString(3),
                Long.parseLong(cursor.getString(4)),
                Integer.parseInt(cursor.getString(5))
        );
        cursor.close();
        db.close();
        return task;
    }

    public ArrayList<Task> getTasksBySubject(int subjectId) {
        ArrayList<Task> taskList = new ArrayList<Task>();
        String selectQuery = "SELECT * FROM " + TABLE_TASKS + " WHERE " + KEY_TASK_SUBJECT_ID + " = " + subjectId +
                             " ORDER BY " + KEY_TASK_IS_COMPLETED + " ASC, " + KEY_TASK_DUE_DATE + " ASC";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                Task task = new Task();
                task.setId(Integer.parseInt(cursor.getString(0)));
                task.setSubjectId(Integer.parseInt(cursor.getString(1)));
                task.setTitle(cursor.getString(2));
                task.setDescription(cursor.getString(3));
                task.setDueDateMillis(Long.parseLong(cursor.getString(4)));
                task.setCompleted(Integer.parseInt(cursor.getString(5)) == 1);
                taskList.add(task);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return taskList;
    }

    public ArrayList<Task> getAllIncompleteTasks() {
        ArrayList<Task> taskList = new ArrayList<Task>();
        String selectQuery = "SELECT * FROM " + TABLE_TASKS + " WHERE " + KEY_TASK_IS_COMPLETED + " = 0" +
                             " ORDER BY " + KEY_TASK_DUE_DATE + " ASC";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                Task task = new Task();
                task.setId(Integer.parseInt(cursor.getString(0)));
                task.setSubjectId(Integer.parseInt(cursor.getString(1)));
                task.setTitle(cursor.getString(2));
                task.setDescription(cursor.getString(3));
                task.setDueDateMillis(Long.parseLong(cursor.getString(4)));
                task.setCompleted(Integer.parseInt(cursor.getString(5)) == 1);
                taskList.add(task);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return taskList;
    }

    public int updateTask(Task task) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_TASK_SUBJECT_ID, task.getSubjectId());
        values.put(KEY_TASK_TITLE, task.getTitle());
        values.put(KEY_TASK_DESCRIPTION, task.getDescription());
        values.put(KEY_TASK_DUE_DATE, task.getDueDateMillis());
        values.put(KEY_TASK_IS_COMPLETED, task.isCompleted() ? 1 : 0);
        int rowsAffected = db.update(TABLE_TASKS, values, KEY_TASK_ID + " = ?",
                new String[]{String.valueOf(task.getId())});
        db.close();
        return rowsAffected;
    }

    public void deleteTask(Task task) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_TASKS, KEY_TASK_ID + " = ?",
                new String[]{String.valueOf(task.getId())});
        db.close();
    }
}