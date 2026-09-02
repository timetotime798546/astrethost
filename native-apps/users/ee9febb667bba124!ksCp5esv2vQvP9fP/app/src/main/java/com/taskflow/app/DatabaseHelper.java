package com.taskflow.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "taskflow_local_db.db";
    private static final int DATABASE_VERSION = 1;

    // User Table schema fields
    public static final String TABLE_USERS = "users";
    public static final String USER_COL_ID = "id";
    public static final String USER_COL_USERNAME = "username";
    public static final String USER_COL_EMAIL = "email";
    public static final String USER_COL_PHONE = "phone";
    public static final String USER_COL_PASSWORD = "password";

    // Task Table schema fields
    public static final String TABLE_ITEMS = "items";
    public static final String ITEM_COL_ID = "id";
    public static final String ITEM_COL_USER_ID = "user_id";
    public static final String ITEM_COL_TITLE = "title";
    public static final String ITEM_COL_DESC = "description";
    public static final String ITEM_COL_CATEGORY = "category";
    public static final String ITEM_COL_DUE_DATE = "due_date";
    public static final String ITEM_COL_COMPLETED = "is_completed";
    public static final String ITEM_COL_FAVORITE = "is_favorite";

    // History Logs schema fields
    public static final String TABLE_HISTORY = "history";
    public static final String HIST_COL_ID = "id";
    public static final String HIST_COL_USER_ID = "user_id";
    public static final String HIST_COL_ACTION = "action";
    public static final String HIST_COL_DETAIL = "detail";
    public static final String HIST_COL_TIME = "timestamp";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createUsers = "CREATE TABLE " + TABLE_USERS + " (" +
                USER_COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                USER_COL_USERNAME + " TEXT UNIQUE, " +
                USER_COL_EMAIL + " TEXT, " +
                USER_COL_PHONE + " TEXT, " +
                USER_COL_PASSWORD + " TEXT)";

        String createItems = "CREATE TABLE " + TABLE_ITEMS + " (" +
                ITEM_COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                ITEM_COL_USER_ID + " INTEGER, " +
                ITEM_COL_TITLE + " TEXT, " +
                ITEM_COL_DESC + " TEXT, " +
                ITEM_COL_CATEGORY + " TEXT, " +
                ITEM_COL_DUE_DATE + " TEXT, " +
                ITEM_COL_COMPLETED + " INTEGER DEFAULT 0, " +
                ITEM_COL_FAVORITE + " INTEGER DEFAULT 0)";

        String createHistory = "CREATE TABLE " + TABLE_HISTORY + " (" +
                HIST_COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                HIST_COL_USER_ID + " INTEGER, " +
                HIST_COL_ACTION + " TEXT, " +
                HIST_COL_DETAIL + " TEXT, " +
                HIST_COL_TIME + " TEXT)";

        db.execSQL(createUsers);
        db.execSQL(createItems);
        db.execSQL(createHistory);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVer, int newVer) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ITEMS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_HISTORY);
        onCreate(db);
    }

    // Models representation standard class structure inside main database helper
    public static class User {
        public int id;
        public String username;
        public String email;
        public String phone;
    }

    public static class TaskItem {
        public int id;
        public int userId;
        public String title;
        public String description;
        public String category;
        public String dueDate;
        public boolean isCompleted;
        public boolean isFavorite;
    }

    public static class HistoryLog {
        public int id;
        public int userId;
        public String action;
        public String detail;
        public String timestamp;
    }

    // Insert/Register users helper
    public boolean registerUser(String user, String email, String phone, String pass) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(USER_COL_USERNAME, user);
        values.put(USER_COL_EMAIL, email);
        values.put(USER_COL_PHONE, phone);
        values.put(USER_COL_PASSWORD, pass);
        long result = db.insert(TABLE_USERS, null, values);
        return result != -1;
    }

    // Check user login inputs
    public int checkLogin(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, new String[]{USER_COL_ID},
                USER_COL_USERNAME + "=? AND " + USER_COL_PASSWORD + "=?",
                new String[]{username, password}, null, null, null);
        int id = -1;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                id = cursor.getInt(0);
            }
            cursor.close();
        }
        return id;
    }

    // Retrieve User details
    public User getUserProfile(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, null, USER_COL_ID + "=?",
                new String[]{String.valueOf(userId)}, null, null, null);
        User u = null;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                u = new User();
                u.id = cursor.getInt(cursor.getColumnIndexOrThrow(USER_COL_ID));
                u.username = cursor.getString(cursor.getColumnIndexOrThrow(USER_COL_USERNAME));
                u.email = cursor.getString(cursor.getColumnIndexOrThrow(USER_COL_EMAIL));
                u.phone = cursor.getString(cursor.getColumnIndexOrThrow(USER_COL_PHONE));
            }
            cursor.close();
        }
        return u;
    }

    // Log CRUD changes to audit timeline
    public void logActivity(int userId, String action, String detail) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(HIST_COL_USER_ID, userId);
        values.put(HIST_COL_ACTION, action);
        values.put(HIST_COL_DETAIL, detail);
        values.put(HIST_COL_TIME, new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date()));
        db.insert(TABLE_HISTORY, null, values);
    }

    // History fetch utilities
    public List<HistoryLog> fetchHistory(int userId) {
        List<HistoryLog> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_HISTORY, null, HIST_COL_USER_ID + "=?",
                new String[]{String.valueOf(userId)}, null, null, HIST_COL_ID + " DESC");
        if (cursor != null) {
            while (cursor.moveToNext()) {
                HistoryLog log = new HistoryLog();
                log.id = cursor.getInt(cursor.getColumnIndexOrThrow(HIST_COL_ID));
                log.userId = cursor.getInt(cursor.getColumnIndexOrThrow(HIST_COL_USER_ID));
                log.action = cursor.getString(cursor.getColumnIndexOrThrow(HIST_COL_ACTION));
                log.detail = cursor.getString(cursor.getColumnIndexOrThrow(HIST_COL_DETAIL));
                log.timestamp = cursor.getString(cursor.getColumnIndexOrThrow(HIST_COL_TIME));
                list.add(log);
            }
            cursor.close();
        }
        return list;
    }

    public void clearHistory(int userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_HISTORY, HIST_COL_USER_ID + "=?", new String[]{String.valueOf(userId)});
    }
}