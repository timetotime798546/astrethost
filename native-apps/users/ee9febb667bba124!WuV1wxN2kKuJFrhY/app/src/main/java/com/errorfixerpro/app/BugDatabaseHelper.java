package com.errorfixerpro.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class BugDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "bugs_db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_BUGS = "bugs";
    private static final String KEY_ID = "id";
    private static final String KEY_TITLE = "title";
    private static final String KEY_DESC = "description";
    private static final String KEY_PRIORITY = "priority";
    private static final String KEY_STATUS = "status";

    public BugDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_BUGS_TABLE = "CREATE TABLE " + TABLE_BUGS + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_TITLE + " TEXT,"
                + KEY_DESC + " TEXT,"
                + KEY_PRIORITY + " TEXT,"
                + KEY_STATUS + " TEXT" + ")";
        db.execSQL(CREATE_BUGS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BUGS);
        onCreate(db);
    }

    public void addBug(String title, String desc, String priority, String status) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_TITLE, title);
        values.put(KEY_DESC, desc);
        values.put(KEY_PRIORITY, priority);
        values.put(KEY_STATUS, status);
        db.insert(TABLE_BUGS, null, values);
        db.close();
    }

    public List<Bug> getAllBugs() {
        List<Bug> bugList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_BUGS + " ORDER BY " + KEY_ID + " DESC";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                Bug bug = new Bug();
                bug.setId(cursor.getLong(0));
                bug.setTitle(cursor.getString(1));
                bug.setDescription(cursor.getString(2));
                bug.setPriority(cursor.getString(3));
                bug.setStatus(cursor.getString(4));
                bugList.add(bug);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return bugList;
    }

    public void deleteBug(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_BUGS, KEY_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    public void updateBugStatus(long id, String newStatus) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_STATUS, newStatus);
        db.update(TABLE_BUGS, values, KEY_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }
}