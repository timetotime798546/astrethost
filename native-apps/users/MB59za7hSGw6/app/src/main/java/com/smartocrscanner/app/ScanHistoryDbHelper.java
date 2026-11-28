package com.smartocrscanner.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class ScanHistoryDbHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "ocr_scans.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_SCANS = "scans";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_TEXT = "text";
    public static final String COLUMN_SOURCE = "source";
    public static final String COLUMN_TIMESTAMP = "timestamp";
    public static final String COLUMN_CHAR_COUNT = "char_count";

    public ScanHistoryDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE = "CREATE TABLE " + TABLE_SCANS + " ("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_TEXT + " TEXT, "
                + COLUMN_SOURCE + " TEXT, "
                + COLUMN_TIMESTAMP + " TEXT, "
                + COLUMN_CHAR_COUNT + " INTEGER"
                + ")";
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SCANS);
        onCreate(db);
    }

    public long insertScan(String text, String source, String timestamp) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TEXT, text);
        values.put(COLUMN_SOURCE, source);
        values.put(COLUMN_TIMESTAMP, timestamp);
        values.put(COLUMN_CHAR_COUNT, text != null ? text.length() : 0);
        long id = db.insert(TABLE_SCANS, null, values);
        db.close();
        return id;
    }

    public List<ScanModel> getAllScans() {
        List<ScanModel> list = new ArrayList<ScanModel>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_SCANS, null, null, null, null, null, COLUMN_ID + " DESC");
        if (cursor != null && cursor.moveToFirst()) {
            do {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID));
                String text = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TEXT));
                String source = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SOURCE));
                String timestamp = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP));
                int charCount = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_CHAR_COUNT));
                list.add(new ScanModel(id, text, source, timestamp, charCount));
            } while (cursor.moveToNext());
            cursor.close();
        }
        db.close();
        return list;
    }

    public List<ScanModel> searchScans(String query) {
        List<ScanModel> list = new ArrayList<ScanModel>();
        SQLiteDatabase db = this.getReadableDatabase();
        String selection = COLUMN_TEXT + " LIKE ?";
        String[] selectionArgs = new String[]{"%" + query + "%"};
        Cursor cursor = db.query(TABLE_SCANS, null, selection, selectionArgs, null, null, COLUMN_ID + " DESC");
        if (cursor != null && cursor.moveToFirst()) {
            do {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID));
                String text = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TEXT));
                String source = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SOURCE));
                String timestamp = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP));
                int charCount = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_CHAR_COUNT));
                list.add(new ScanModel(id, text, source, timestamp, charCount));
            } while (cursor.moveToNext());
            cursor.close();
        }
        db.close();
        return list;
    }

    public ScanModel getScanById(long id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_SCANS, null, COLUMN_ID + "=?", new String[]{String.valueOf(id)}, null, null, null);
        ScanModel scan = null;
        if (cursor != null && cursor.moveToFirst()) {
            String text = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TEXT));
            String source = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SOURCE));
            String timestamp = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP));
            int charCount = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_CHAR_COUNT));
            scan = new ScanModel(id, text, source, timestamp, charCount);
            cursor.close();
        }
        db.close();
        return scan;
    }

    public int deleteScan(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rows = db.delete(TABLE_SCANS, COLUMN_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
        return rows;
    }

    public void clearAllScans() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_SCANS, null, null);
        db.close();
    }

    public int getScanCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_SCANS, null);
        int count = 0;
        if (cursor != null && cursor.moveToFirst()) {
            count = cursor.getInt(0);
            cursor.close();
        }
        db.close();
        return count;
    }

    public void updateScanText(long id, String newText) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TEXT, newText);
        values.put(COLUMN_CHAR_COUNT, newText != null ? newText.length() : 0);
        db.update(TABLE_SCANS, values, COLUMN_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
    }
}