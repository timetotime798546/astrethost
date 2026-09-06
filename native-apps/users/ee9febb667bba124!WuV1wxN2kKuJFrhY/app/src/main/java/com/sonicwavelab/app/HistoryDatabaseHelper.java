package com.sonicwavelab.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HistoryDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "sonicwave_log.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_HISTORY = "history";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_PRESET = "preset";
    public static final String COLUMN_PITCH = "pitch";
    public static final String COLUMN_DURATION = "duration";
    public static final String COLUMN_ECHO = "echo";
    public static final String COLUMN_TIME = "timestamp";

    public HistoryDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_HISTORY + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_PRESET + " TEXT, " +
                COLUMN_PITCH + " INTEGER, " +
                COLUMN_DURATION + " REAL, " +
                COLUMN_ECHO + " INTEGER, " +
                COLUMN_TIME + " TEXT" +
                ")";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_HISTORY);
        onCreate(db);
    }

    public void insertHistoryItem(String preset, int pitch, double duration, int echo, String timestamp) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_PRESET, preset);
        values.put(COLUMN_PITCH, pitch);
        values.put(COLUMN_DURATION, duration);
        values.put(COLUMN_ECHO, echo);
        values.put(COLUMN_TIME, timestamp);
        db.insert(TABLE_HISTORY, null, values);
    }

    public List<Map<String, String>> getAllHistory() {
        List<Map<String, String>> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_HISTORY + " ORDER BY " + COLUMN_ID + " DESC", null);

        if (cursor.moveToFirst()) {
            do {
                Map<String, String> map = new HashMap<>();
                map.put("id", cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ID)));
                map.put("preset", cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PRESET)));
                map.put("pitch", cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PITCH)));
                map.put("duration", cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DURATION)));
                map.put("echo", cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ECHO)));
                map.put("timestamp", cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIME)));
                list.add(map);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    public void clearAllHistory() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM " + TABLE_HISTORY);
    }
}