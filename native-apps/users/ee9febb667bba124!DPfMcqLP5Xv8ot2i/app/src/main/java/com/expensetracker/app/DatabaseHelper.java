package com.expensetracker.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "spend_tracker.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_NAME = "transactions";
    public static final String COL_ID = "id";
    public static final String COL_TITLE = "title";
    public static final String COL_AMOUNT = "amount";
    public static final String COL_TYPE = "type";
    public static final String COL_CATEGORY = "category";
    public static final String COL_DATE = "date";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTableQuery = "CREATE TABLE " + TABLE_NAME + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_TITLE + " TEXT, " +
                COL_AMOUNT + " REAL, " +
                COL_TYPE + " TEXT, " +
                COL_CATEGORY + " TEXT, " +
                COL_DATE + " TEXT)";
        db.execSQL(createTableQuery);

        // Prepopulate database with realistic dummy indicators to make it immediately interactive
        db.execSQL("INSERT INTO " + TABLE_NAME + " (title, amount, type, category, date) VALUES ('Salary Bonus', 3500.00, 'INCOME', 'Salary', '2023-11-01')");
        db.execSQL("INSERT INTO " + TABLE_NAME + " (title, amount, type, category, date) VALUES ('Monthly Groceries', 184.50, 'EXPENSE', 'Food', '2023-11-02')");
        db.execSQL("INSERT INTO " + TABLE_NAME + " (title, amount, type, category, date) VALUES ('Uber Office Ride', 22.00, 'EXPENSE', 'Transport', '2023-11-03')");
        db.execSQL("INSERT INTO " + TABLE_NAME + " (title, amount, type, category, date) VALUES ('Freelance Design', 620.00, 'INCOME', 'Business', '2023-11-10')");
        db.execSQL("INSERT INTO " + TABLE_NAME + " (title, amount, type, category, date) VALUES ('Gym Membership', 45.00, 'EXPENSE', 'Health', '2023-11-15')");
        db.execSQL("INSERT INTO " + TABLE_NAME + " (title, amount, type, category, date) VALUES ('Cinema Tickets', 28.50, 'EXPENSE', 'Entertainment', '2023-11-18')");
        db.execSQL("INSERT INTO " + TABLE_NAME + " (title, amount, type, category, date) VALUES ('Electric Utility Bill', 115.00, 'EXPENSE', 'Utilities', '2023-11-19')");
    }

    @Override
    public void upgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public boolean addTransaction(String title, double amount, String type, String category, String date) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_TITLE, title);
        values.put(COL_AMOUNT, amount);
        values.put(COL_TYPE, type);
        values.put(COL_CATEGORY, category);
        values.put(COL_DATE, date);

        long result = db.insert(TABLE_NAME, null, values);
        return result != -1;
    }

    public boolean deleteTransaction(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        int affected = db.delete(TABLE_NAME, COL_ID + "=?", new String[]{String.valueOf(id)});
        return affected > 0;
    }

    public void clearAllData() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM " + TABLE_NAME);
    }

    public List<Transaction> getAllTransactions() {
        List<Transaction> transactions = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME + " ORDER BY date DESC, id DESC", null);

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID));
                String title = cursor.getString(cursor.getColumnIndexOrThrow(COL_TITLE));
                double amount = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_AMOUNT));
                String type = cursor.getString(cursor.getColumnIndexOrThrow(COL_TYPE));
                String category = cursor.getString(cursor.getColumnIndexOrThrow(COL_CATEGORY));
                String date = cursor.getString(cursor.getColumnIndexOrThrow(COL_DATE));

                transactions.add(new Transaction(id, title, amount, type, category, date));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return transactions;
    }

    public List<String> getAvailableMonths() {
        List<String> months = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        // Extract YYYY-MM from transaction dates
        Cursor cursor = db.rawQuery("SELECT DISTINCT SUBSTR(date, 1, 7) as ym FROM " + TABLE_NAME + " ORDER BY ym DESC", null);

        if (cursor.moveToFirst()) {
            do {
                String val = cursor.getString(0);
                if (val != null && val.trim().length() == 7) {
                    months.add(val);
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        return months;
    }

    public double getTotalIncome() {
        double total = 0.00;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT SUM(amount) FROM " + TABLE_NAME + " WHERE type='INCOME'", null);
        if (cursor.moveToFirst()) {
            total = cursor.getDouble(0);
        }
        cursor.close();
        return total;
    }

    public double getTotalExpense() {
        double total = 0.00;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT SUM(amount) FROM " + TABLE_NAME + " WHERE type='EXPENSE'", null);
        if (cursor.moveToFirst()) {
            total = cursor.getDouble(0);
        }
        cursor.close();
        return total;
    }
}