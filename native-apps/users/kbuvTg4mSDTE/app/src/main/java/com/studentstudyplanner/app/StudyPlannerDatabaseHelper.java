package com.studentstudyplanner.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class StudyPlannerDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "study_planner_db";
    private static final int DATABASE_VERSION = 1;

    // Subjects Table
    public static final String TABLE_SUBJECTS = "subjects";
    public static final String COLUMN_SUBJECT_ID = "_id";
    public static final String COLUMN_SUBJECT_NAME = "name";

    // Tasks Table
    public static final String TABLE_TASKS = "tasks";
    public static final String COLUMN_TASK_ID = "_id";
    public static final String COLUMN_TASK_SUBJECT_ID = "subject_id";
    public static final String COLUMN_TASK_TITLE = "title";
    public static final String COLUMN_TASK_DESCRIPTION = "description";
    public static final String COLUMN_TASK_DUE_DATE = "due_date";
    public static final String COLUMN_TASK_PRIORITY = "priority";
    public static final String COLUMN_TASK_STATUS = "status"; // 0=Pending, 1=Completed

    // Reminders Table
    public static final String TABLE_REMINDERS = "reminders";
    public static final String COLUMN_REMINDER_ID = "_id";
    public static final String COLUMN_REMINDER_TASK_ID = "task_id";
    public static final String COLUMN_REMINDER_MESSAGE = "message";
    public static final String COLUMN_REMINDER_TIME = "reminder_time"; // Timestamp
    public static final String COLUMN_REMINDER_IS_ACTIVE = "is_active"; // 0=false, 1=true


    // Create Subjects Table
    private static final String CREATE_TABLE_SUBJECTS =
            "CREATE TABLE " + TABLE_SUBJECTS + "(" +
                    COLUMN_SUBJECT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    COLUMN_SUBJECT_NAME + " TEXT UNIQUE" + // Subject names should be unique
                    ")";

    // Create Tasks Table
    private static final String CREATE_TABLE_TASKS =
            "CREATE TABLE " + TABLE_TASKS + "(" +
                    COLUMN_TASK_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    COLUMN_TASK_SUBJECT_ID + " INTEGER," +
                    COLUMN_TASK_TITLE + " TEXT," +
                    COLUMN_TASK_DESCRIPTION + " TEXT," +
                    COLUMN_TASK_DUE_DATE + " INTEGER," +
                    COLUMN_TASK_PRIORITY + " INTEGER," +
                    COLUMN_TASK_STATUS + " INTEGER," +
                    "FOREIGN KEY(" + COLUMN_TASK_SUBJECT_ID + ") REFERENCES " +
                    TABLE_SUBJECTS + "(" + COLUMN_SUBJECT_ID + ") ON DELETE CASCADE" +
                    ")";

    // Create Reminders Table
    private static final String CREATE_TABLE_REMINDERS =
            "CREATE TABLE " + TABLE_REMINDERS + "(" +
                    COLUMN_REMINDER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    COLUMN_REMINDER_TASK_ID + " INTEGER," +
                    COLUMN_REMINDER_MESSAGE + " TEXT," +
                    COLUMN_REMINDER_TIME + " INTEGER," +
                    COLUMN_REMINDER_IS_ACTIVE + " INTEGER," +
                    "FOREIGN KEY(" + COLUMN_REMINDER_TASK_ID + ") REFERENCES " +
                    TABLE_TASKS + "(" + COLUMN_TASK_ID + ") ON DELETE CASCADE" +
                    ")";


    public StudyPlannerDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_SUBJECTS);
        db.execSQL(CREATE_TABLE_TASKS);
        db.execSQL(CREATE_TABLE_REMINDERS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_REMINDERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TASKS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SUBJECTS);
        onCreate(db);
    }

    // --- Subject Operations ---
    public long addSubject(Subject subject) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_SUBJECT_NAME, subject.getName());
        long id = db.insert(TABLE_SUBJECTS, null, values);
        db.close();
        return id;
    }

    public Subject getSubject(long id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_SUBJECTS,
                new String[]{COLUMN_SUBJECT_ID, COLUMN_SUBJECT_NAME},
                COLUMN_SUBJECT_ID + "=?",
                new String[]{String.valueOf(id)}, null, null, null, null);

        Subject subject = null;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                subject = new Subject(
                        cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_SUBJECT_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SUBJECT_NAME))
                );
            }
            cursor.close();
        }
        db.close();
        return subject;
    }

    public List<Subject> getAllSubjects() {
        List<Subject> subjects = new ArrayList<Subject>();
        String selectQuery = "SELECT * FROM " + TABLE_SUBJECTS + " ORDER BY " + COLUMN_SUBJECT_NAME + " ASC";
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                Subject subject = new Subject(
                        cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_SUBJECT_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SUBJECT_NAME))
                );
                subjects.add(subject);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return subjects;
    }

    public int updateSubject(Subject subject) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_SUBJECT_NAME, subject.getName());
        int rowsAffected = db.update(TABLE_SUBJECTS, values, COLUMN_SUBJECT_ID + " = ?",
                new String[]{String.valueOf(subject.getId())});
        db.close();
        return rowsAffected;
    }

    public void deleteSubject(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        // Deleting a subject should cascade delete its tasks and reminders due to FOREIGN KEY ON DELETE CASCADE
        db.delete(TABLE_SUBJECTS, COLUMN_SUBJECT_ID + " = ?",
                new String[]{String.valueOf(id)});
        db.close();
    }

    // --- Task Operations ---
    public long addTask(Task task) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TASK_SUBJECT_ID, task.getSubjectId());
        values.put(COLUMN_TASK_TITLE, task.getTitle());
        values.put(COLUMN_TASK_DESCRIPTION, task.getDescription());
        values.put(COLUMN_TASK_DUE_DATE, task.getDueDate());
        values.put(COLUMN_TASK_PRIORITY, task.getPriority());
        values.put(COLUMN_TASK_STATUS, task.getStatus());
        long id = db.insert(TABLE_TASKS, null, values);
        db.close();
        return id;
    }

    public Task getTask(long id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_TASKS,
                new String[]{COLUMN_TASK_ID, COLUMN_TASK_SUBJECT_ID, COLUMN_TASK_TITLE,
                        COLUMN_TASK_DESCRIPTION, COLUMN_TASK_DUE_DATE, COLUMN_TASK_PRIORITY, COLUMN_TASK_STATUS},
                COLUMN_TASK_ID + "=?",
                new String[]{String.valueOf(id)}, null, null, null, null);

        Task task = null;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                task = new Task(
                        cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TASK_ID)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TASK_SUBJECT_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TASK_TITLE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TASK_DESCRIPTION)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TASK_DUE_DATE)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TASK_PRIORITY)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TASK_STATUS))
                );
            }
            cursor.close();
        }
        db.close();
        return task;
    }

    public List<Task> getTasksBySubject(long subjectId) {
        List<Task> tasks = new ArrayList<Task>();
        String selectQuery = "SELECT * FROM " + TABLE_TASKS +
                             " WHERE " + COLUMN_TASK_SUBJECT_ID + " = ?" +
                             " ORDER BY " + COLUMN_TASK_DUE_DATE + " ASC"; // Order by due date
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, new String[]{String.valueOf(subjectId)});

        if (cursor.moveToFirst()) {
            do {
                Task task = new Task(
                        cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TASK_ID)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TASK_SUBJECT_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TASK_TITLE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TASK_DESCRIPTION)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TASK_DUE_DATE)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TASK_PRIORITY)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TASK_STATUS))
                );
                tasks.add(task);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return tasks;
    }

    public int updateTask(Task task) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TASK_SUBJECT_ID, task.getSubjectId());
        values.put(COLUMN_TASK_TITLE, task.getTitle());
        values.put(COLUMN_TASK_DESCRIPTION, task.getDescription());
        values.put(COLUMN_TASK_DUE_DATE, task.getDueDate());
        values.put(COLUMN_TASK_PRIORITY, task.getPriority());
        values.put(COLUMN_TASK_STATUS, task.getStatus());
        int rowsAffected = db.update(TABLE_TASKS, values, COLUMN_TASK_ID + " = ?",
                new String[]{String.valueOf(task.getId())});
        db.close();
        return rowsAffected;
    }

    public void deleteTask(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        // Deleting a task should cascade delete its reminders
        db.delete(TABLE_TASKS, COLUMN_TASK_ID + " = ?",
                new String[]{String.valueOf(id)});
        db.close();
    }

    public int getCompletedTasksCountForSubject(long subjectId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_TASKS +
                " WHERE " + COLUMN_TASK_SUBJECT_ID + " = ? AND " + COLUMN_TASK_STATUS + " = 1",
                new String[]{String.valueOf(subjectId)});
        int count = 0;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
            cursor.close();
        }
        db.close();
        return count;
    }

    public int getTotalTasksCountForSubject(long subjectId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_TASKS +
                " WHERE " + COLUMN_TASK_SUBJECT_ID + " = ?",
                new String[]{String.valueOf(subjectId)});
        int count = 0;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
            cursor.close();
        }
        db.close();
        return count;
    }


    // --- Reminder Operations ---
    public long addReminder(Reminder reminder) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_REMINDER_TASK_ID, reminder.getTaskId());
        values.put(COLUMN_REMINDER_MESSAGE, reminder.getMessage());
        values.put(COLUMN_REMINDER_TIME, reminder.getReminderTime());
        values.put(COLUMN_REMINDER_IS_ACTIVE, reminder.getIsActive());
        long id = db.insert(TABLE_REMINDERS, null, values);
        db.close();
        return id;
    }

    public Reminder getReminderByTaskId(long taskId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_REMINDERS,
                new String[]{COLUMN_REMINDER_ID, COLUMN_REMINDER_TASK_ID, COLUMN_REMINDER_MESSAGE,
                        COLUMN_REMINDER_TIME, COLUMN_REMINDER_IS_ACTIVE},
                COLUMN_REMINDER_TASK_ID + "=?",
                new String[]{String.valueOf(taskId)}, null, null, null, null);

        Reminder reminder = null;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                reminder = new Reminder(
                        cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_REMINDER_ID)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_REMINDER_TASK_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_REMINDER_MESSAGE)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_REMINDER_TIME)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_REMINDER_IS_ACTIVE))
                );
            }
            cursor.close();
        }
        db.close();
        return reminder;
    }

    public int updateReminder(Reminder reminder) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_REMINDER_TASK_ID, reminder.getTaskId());
        values.put(COLUMN_REMINDER_MESSAGE, reminder.getMessage());
        values.put(COLUMN_REMINDER_TIME, reminder.getReminderTime());
        values.put(COLUMN_REMINDER_IS_ACTIVE, reminder.getIsActive());
        int rowsAffected = db.update(TABLE_REMINDERS, values, COLUMN_REMINDER_ID + " = ?",
                new String[]{String.valueOf(reminder.getId())});
        db.close();
        return rowsAffected;
    }

    public void deleteReminder(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_REMINDERS, COLUMN_REMINDER_ID + " = ?",
                new String[]{String.valueOf(id)});
        db.close();
    }

    public List<Reminder> getAllActiveReminders() {
        List<Reminder> reminders = new ArrayList<Reminder>();
        String selectQuery = "SELECT * FROM " + TABLE_REMINDERS + " WHERE " + COLUMN_REMINDER_IS_ACTIVE + " = 1";
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                Reminder reminder = new Reminder(
                        cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_REMINDER_ID)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_REMINDER_TASK_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_REMINDER_MESSAGE)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_REMINDER_TIME)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_REMINDER_IS_ACTIVE))
                );
                reminders.add(reminder);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return reminders;
    }
}