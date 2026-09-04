package com.expensetracker.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "expenses_tracker.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_TRANSACTIONS = "transactions";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_AMOUNT = "amount";
    public static final String COLUMN_TYPE = "type";
    public static final String COLUMN_CATEGORY = "category";
    public static final String COLUMN_DATE = "date";
    public static final String COLUMN_NOTE = "note";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_TRANSACTIONS + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_AMOUNT + " REAL, " +
                COLUMN_TYPE + " TEXT, " +
                COLUMN_CATEGORY + " TEXT, " +
                COLUMN_DATE + " TEXT, " +
                COLUMN_NOTE + " TEXT)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRANSACTIONS);
        onCreate(db);
    }

    public long insertTransaction(double amount, String type, String category, String date, String note) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_AMOUNT, amount);
        values.put(COLUMN_TYPE, type);
        values.put(COLUMN_CATEGORY, category);
        values.put(COLUMN_DATE, date);
        values.put(COLUMN_NOTE, note);
        long id = db.insert(TABLE_TRANSACTIONS, null, values);
        db.close();
        return id;
    }

    public boolean deleteTransaction(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rows = db.delete(TABLE_TRANSACTIONS, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
        return rows > 0;
    }

    public List<Transaction> getTransactionsByMonth(String yyyyMm) {
        List<Transaction> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_TRANSACTIONS, null, COLUMN_DATE + " LIKE ?", new String[]{yyyyMm + "-%"}, null, null, COLUMN_DATE + " DESC, " + COLUMN_ID + " DESC");
        if (cursor.moveToFirst()) {
            do {
                Transaction t = new Transaction();
                t.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)));
                t.setAmount(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_AMOUNT)));
                t.setType(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TYPE)));
                t.setCategory(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY)));
                t.setDate(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE)));
                t.setNote(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOTE)));
                list.add(t);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }
}