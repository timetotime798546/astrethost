package com.expensetrackerpro.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "expense_tracker.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_TRANSACTIONS = "transactions";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_TITLE = "title";
    private static final String COLUMN_AMOUNT = "amount";
    private static final String COLUMN_TYPE = "type";
    private static final String COLUMN_CATEGORY = "category";
    private static final String COLUMN_DATE = "date";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE = "CREATE TABLE " + TABLE_TRANSACTIONS + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_TITLE + " TEXT,"
                + COLUMN_AMOUNT + " REAL,"
                + COLUMN_TYPE + " TEXT,"
                + COLUMN_CATEGORY + " TEXT,"
                + COLUMN_DATE + " TEXT" + ")";
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRANSACTIONS);
        onCreate(db);
    }

    public long insertTransaction(String title, double amount, String type, String category, String date) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, title);
        values.put(COLUMN_AMOUNT, amount);
        values.put(COLUMN_TYPE, type);
        values.put(COLUMN_CATEGORY, category);
        values.put(COLUMN_DATE, date);
        long id = db.insert(TABLE_TRANSACTIONS, null, values);
        db.close();
        return id;
    }

    public List<Transaction> getAllTransactionsSorted() {
        List<Transaction> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_TRANSACTIONS + " ORDER BY " + COLUMN_DATE + " DESC, " + COLUMN_ID + " DESC", null);

        if (cursor.moveToFirst()) {
            do {
                Transaction transaction = new Transaction(
                        cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE)),
                        cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_AMOUNT)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TYPE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE))
                );
                list.add(transaction);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }

    public List<Transaction> getTransactionsForMonth(String yearMonth) { // yearMonth formatted as YYYY-MM
        List<Transaction> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_TRANSACTIONS + " WHERE " + COLUMN_DATE + " LIKE ? ORDER BY " + COLUMN_DATE + " DESC", new String[]{yearMonth + "%"});

        if (cursor.moveToFirst()) {
            do {
                Transaction transaction = new Transaction(
                        cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE)),
                        cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_AMOUNT)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TYPE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE))
                );
                list.add(transaction);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }

    public void deleteTransaction(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_TRANSACTIONS, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }
}