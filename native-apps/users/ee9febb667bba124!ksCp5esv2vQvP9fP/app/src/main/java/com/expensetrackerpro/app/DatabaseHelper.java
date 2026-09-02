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

    public static final String TABLE_TRANSACTIONS = "transactions";
    public static final String COL_ID = "id";
    public static final String COL_TYPE = "type";
    public static final String COL_AMOUNT = "amount";
    public static final String COL_CATEGORY = "category";
    public static final String COL_DATE = "date";
    public static final String COL_NOTE = "note";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_TRANSACTIONS + " ( " +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_TYPE + " TEXT, " +
                COL_AMOUNT + " REAL, " +
                COL_CATEGORY + " TEXT, " +
                COL_DATE + " TEXT, " +
                COL_NOTE + " TEXT )";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRANSACTIONS);
        onCreate(db);
    }

    public boolean insertTransaction(String type, double amount, String category, String date, String note) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_TYPE, type);
        cv.put(COL_AMOUNT, amount);
        cv.put(COL_CATEGORY, category);
        cv.put(COL_DATE, date);
        cv.put(COL_NOTE, note);

        long result = db.insert(TABLE_TRANSACTIONS, null, cv);
        return result != -1;
    }

    public List<Transaction> getAllTransactions() {
        List<Transaction> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_TRANSACTIONS + " ORDER BY " + COL_DATE + " DESC, " + COL_ID + " DESC", null);

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID));
                String type = cursor.getString(cursor.getColumnIndexOrThrow(COL_TYPE));
                double amount = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_AMOUNT));
                String category = cursor.getString(cursor.getColumnIndexOrThrow(COL_CATEGORY));
                String date = cursor.getString(cursor.getColumnIndexOrThrow(COL_DATE));
                String note = cursor.getString(cursor.getColumnIndexOrThrow(COL_NOTE));

                list.add(new Transaction(id, type, amount, category, date, note));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    public double getTotalAmountByType(String type, String monthFilter) {
        SQLiteDatabase db = this.getReadableDatabase();
        double total = 0;
        Cursor cursor;
        if (monthFilter.equals("All Months")) {
            cursor = db.rawQuery("SELECT SUM(" + COL_AMOUNT + ") FROM " + TABLE_TRANSACTIONS + " WHERE " + COL_TYPE + " = ?", new String[]{type});
        } else {
            cursor = db.rawQuery("SELECT SUM(" + COL_AMOUNT + ") FROM " + TABLE_TRANSACTIONS + " WHERE " + COL_TYPE + " = ? AND " + COL_DATE + " LIKE ?", new String[]{type, monthFilter + "-%"});
        }

        if (cursor.moveToFirst()) {
            total = cursor.getDouble(0);
        }
        cursor.close();
        return total;
    }

    public List<CategoryReport> getCategoryReport(String monthFilter) {
        List<CategoryReport> reports = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        double totalExpense = getTotalAmountByType("EXPENSE", monthFilter);
        if (totalExpense <= 0) return reports;

        Cursor cursor;
        if (monthFilter.equals("All Months")) {
            cursor = db.rawQuery("SELECT " + COL_CATEGORY + ", SUM(" + COL_AMOUNT + ") FROM " + TABLE_TRANSACTIONS + " WHERE " + COL_TYPE + " = 'EXPENSE' GROUP BY " + COL_CATEGORY + " ORDER BY SUM(" + COL_AMOUNT + ") DESC", null);
        } else {
            cursor = db.rawQuery("SELECT " + COL_CATEGORY + ", SUM(" + COL_AMOUNT + ") FROM " + TABLE_TRANSACTIONS + " WHERE " + COL_TYPE + " = 'EXPENSE' AND " + COL_DATE + " LIKE ? GROUP BY " + COL_CATEGORY + " ORDER BY SUM(" + COL_AMOUNT + ") DESC", new String[]{monthFilter + "-%"});
        }

        if (cursor.moveToFirst()) {
            do {
                String category = cursor.getString(0);
                double amount = cursor.getDouble(1);
                int percentage = (int) Math.round((amount / totalExpense) * 100.0);
                reports.add(new CategoryReport(category, amount, percentage));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return reports;
    }

    public List<String> getAvailableMonths() {
        List<String> months = new ArrayList<>();
        months.add("All Months");
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT DISTINCT SUBSTR(" + COL_DATE + ", 1, 7) FROM " + TABLE_TRANSACTIONS + " ORDER BY SUBSTR(" + COL_DATE + ", 1, 7) DESC", null);
        if (cursor.moveToFirst()) {
            do {
                String m = cursor.getString(0);
                if (m != null && m.length() == 7 && !months.contains(m)) {
                    months.add(m);
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        return months;
    }

    public boolean deleteTransaction(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_TRANSACTIONS, COL_ID + " = ?", new String[]{String.valueOf(id)}) > 0;
    }
}