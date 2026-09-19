package com.sqlmanager.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SystemDbHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "_sql_manager_system.db";
    private static final int DATABASE_VERSION = 1;

    public SystemDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS query_history (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "query TEXT NOT NULL," +
                "timestamp TEXT NOT NULL," +
                "status TEXT NOT NULL," +
                "exec_time INTEGER NOT NULL," +
                "db_name TEXT NOT NULL" +
                ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS query_history");
        onCreate(db);
    }

    public void addHistory(String query, String timestamp, String status, long execTime, String dbName) {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("query", query);
            values.put("timestamp", timestamp);
            values.put("status", status);
            values.put("exec_time", execTime);
            values.put("db_name", dbName);
            db.insert("query_history", null, values);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<Map<String, String>> getHistory() {
        List<Map<String, String>> historyList = new ArrayList<>();
        try {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT * FROM query_history ORDER BY id DESC LIMIT 100", null);
            if (cursor.moveToFirst()) {
                do {
                    Map<String, String> map = new HashMap<>();
                    map.put("id", cursor.getString(cursor.getColumnIndexOrThrow("id")));
                    map.put("query", cursor.getString(cursor.getColumnIndexOrThrow("query")));
                    map.put("timestamp", cursor.getString(cursor.getColumnIndexOrThrow("timestamp")));
                    map.put("status", cursor.getString(cursor.getColumnIndexOrThrow("status")));
                    map.put("exec_time", cursor.getString(cursor.getColumnIndexOrThrow("exec_time")));
                    map.put("db_name", cursor.getString(cursor.getColumnIndexOrThrow("db_name")));
                    historyList.add(map);
                } while (cursor.moveToNext());
            }
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return historyList;
    }

    public void clearHistory() {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            db.execSQL("DELETE FROM query_history");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void deleteHistoryItem(int id) {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            db.execSQL("DELETE FROM query_history WHERE id = " + id);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}