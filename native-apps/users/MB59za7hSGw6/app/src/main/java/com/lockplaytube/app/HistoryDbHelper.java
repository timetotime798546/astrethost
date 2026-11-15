package com.lockplaytube.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class HistoryDbHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "yt_history.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_HISTORY = "history";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_TITLE = "title";
    private static final String COLUMN_URL = "url";
    private static final String COLUMN_TIMESTAMP = "timestamp";

    public HistoryDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE = "CREATE TABLE " + TABLE_HISTORY + " ("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_TITLE + " TEXT, "
                + COLUMN_URL + " TEXT, "
                + COLUMN_TIMESTAMP + " INTEGER" + ")";
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_HISTORY);
        onCreate(db);
    }

    public void addHistory(String title, String url) {
        if (url == null || url.trim().isEmpty()) {
            return;
        }

        SQLiteDatabase db = this.getWritableDatabase();

        Cursor cursor = db.query(TABLE_HISTORY, new String[]{COLUMN_URL}, null, null, null, null, COLUMN_ID + " DESC", "1");
        if (cursor != null && cursor.moveToFirst()) {
            String lastUrl = cursor.getString(0);
            cursor.close();
            if (url.equalsIgnoreCase(lastUrl)) {
                return;
            }
        }

        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, (title != null && !title.trim().isEmpty()) ? title : url);
        values.put(COLUMN_URL, url);
        values.put(COLUMN_TIMESTAMP, System.currentTimeMillis());

        db.insert(TABLE_HISTORY, null, values);
    }

    public List<HistoryItem> getAllHistory() {
        List<HistoryItem> list = new ArrayList<HistoryItem>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_HISTORY, null, null, null, null, null, COLUMN_TIMESTAMP + " DESC");

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int idIdx = cursor.getColumnIndex(COLUMN_ID);
                int titleIdx = cursor.getColumnIndex(COLUMN_TITLE);
                int urlIdx = cursor.getColumnIndex(COLUMN_URL);
                int timeIdx = cursor.getColumnIndex(COLUMN_TIMESTAMP);

                do {
                    long id = cursor.getLong(idIdx);
                    String title = cursor.getString(titleIdx);
                    String url = cursor.getString(urlIdx);
                    long timestamp = cursor.getLong(timeIdx);

                    list.add(new HistoryItem(id, title, url, timestamp));
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        return list;
    }

    public void deleteHistory(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_HISTORY, COLUMN_ID + "=?", new String[]{String.valueOf(id)});
    }

    public void clearAllHistory() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_HISTORY, null, null);
    }
}