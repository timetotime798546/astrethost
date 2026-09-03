package com.expensetrackerpro.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "expense_tracker.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_TRANSACTIONS = "transactions";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_AMOUNT = "amount";
    public static final String COLUMN_TYPE = "type";
    public static final String COLUMN_CATEGORY = "category";
    public static final String COLUMN_DESCRIPTION = "description";
    public static final String COLUMN_DATE = "date";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE = "CREATE TABLE " + TABLE_TRANSACTIONS + " ("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_AMOUNT + " REAL, "
                + COLUMN_TYPE + " TEXT, "
                + COLUMN_CATEGORY + " TEXT, "
                + COLUMN_DESCRIPTION + " TEXT, "
                + COLUMN_DATE + " TEXT" + ")";
        db.execSQL(CREATE_TABLE);

        // Prepopulate with a baseline transaction to make the screen exciting at start
        db.execSQL("INSERT INTO " + TABLE_TRANSACTIONS + " (" + COLUMN_AMOUNT + ", " + COLUMN_TYPE + ", " + COLUMN_CATEGORY + ", " + COLUMN_DESCRIPTION + ", " + COLUMN_DATE + ") VALUES (1200.0, 'Income', 'Salary', 'First baseline paycheck', '2023-11-01')");
        db.execSQL("INSERT INTO " + TABLE_TRANSACTIONS + " (" + COLUMN_AMOUNT + ", " + COLUMN_TYPE + ", " + COLUMN_CATEGORY + ", " + COLUMN_DESCRIPTION + ", " + COLUMN_DATE + ") VALUES (45.50, 'Expense', 'Food', 'Grocery Shopping', '2023-11-02')");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRANSACTIONS);
        onCreate(db);
    }

    public long insertTransaction(double amount, String type, String category, String description, String date) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_AMOUNT, amount);
        values.put(COLUMN_TYPE, type);
        values.put(COLUMN_CATEGORY, category);
        values.put(COLUMN_DESCRIPTION, description);
        values.put(COLUMN_DATE, date);
        long result = db.insert(TABLE_TRANSACTIONS, null, values);
        db.close();
        return result;
    }

    public List<Transaction> getAllTransactions() {
        List<Transaction> transactionsList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String selectQuery = "SELECT * FROM " + TABLE_TRANSACTIONS + " ORDER BY " + COLUMN_DATE + " DESC, " + COLUMN_ID + " DESC";
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                Transaction transaction = new Transaction(
                        cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                        cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_AMOUNT)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TYPE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE))
                );
                transactionsList.add(transaction);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return transactionsList;
    }

    public boolean deleteTransaction(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        int affectedRows = db.delete(TABLE_TRANSACTIONS, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
        return affectedRows > 0;
    }

    public Map<String, Double> getCategoryStats(String type, String monthYearFilter) {
        Map<String, Double> stats = new HashMap<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        // Filter is expected to be YYYY-MM
        String query = "SELECT " + COLUMN_CATEGORY + ", SUM(" + COLUMN_AMOUNT + ") as total FROM " + TABLE_TRANSACTIONS 
                     + " WHERE " + COLUMN_TYPE + " = ? AND " + COLUMN_DATE + " LIKE ? GROUP BY " + COLUMN_CATEGORY;
                     
        Cursor cursor = db.rawQuery(query, new String[]{type, monthYearFilter + "%"});
        
        if (cursor.moveToFirst()) {
            do {
                String category = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY));
                double sum = cursor.getDouble(cursor.getColumnIndexOrThrow("total"));
                stats.put(category, sum);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return stats;
    }

    public double getTotalAmount(String type, String monthYearFilter) {
        double total = 0.0;
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT SUM(" + COLUMN_AMOUNT + ") FROM " + TABLE_TRANSACTIONS + " WHERE " + COLUMN_TYPE + " = ? AND " + COLUMN_DATE + " LIKE ?";
        Cursor cursor = db.rawQuery(query, new String[]{type, monthYearFilter + "%"});
        if (cursor.moveToFirst()) {
            total = cursor.getDouble(0);
        }
        cursor.close();
        db.close();
        return total;
    }
}