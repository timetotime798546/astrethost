package com.cashsnap.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class ExpenseDbHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "cashsnap.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_NAME = "expenses";
    public static final String COL_ID = "id";
    public static final String COL_AMOUNT = "amount";
    public static final String COL_CATEGORY = "category";
    public static final String COL_DATE = "date";
    public static final String COL_NOTE = "note";

    public ExpenseDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_NAME + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_AMOUNT + " REAL, " +
                COL_CATEGORY + " TEXT, " +
                COL_DATE + " TEXT, " +
                COL_NOTE + " TEXT)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public boolean insertExpense(double amount, String category, String date, String note) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_AMOUNT, amount);
        values.put(COL_CATEGORY, category);
        values.put(COL_DATE, date);
        values.put(COL_NOTE, note);
        long result = db.insert(TABLE_NAME, null, values);
        return result != -1;
    }

    public boolean deleteExpense(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        int result = db.delete(TABLE_NAME, COL_ID + "=?", new String[]{String.valueOf(id)});
        return result > 0;
    }

    public Cursor getAllExpensesCursor() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_NAME + " ORDER BY " + COL_DATE + " DESC, " + COL_ID + " DESC", null);
    }

    public Cursor getFilteredExpensesCursor(String query, String category) {
        SQLiteDatabase db = this.getReadableDatabase();
        String sql = "SELECT * FROM " + TABLE_NAME + " WHERE 1=1";
        List<String> args = new ArrayList<>();

        if (query != null && !query.trim().isEmpty()) {
            sql += " AND (" + COL_NOTE + " LIKE ? OR " + COL_CATEGORY + " LIKE ?)";
            args.add("%" + query + "%");
            args.add("%" + query + "%");
        }

        if (category != null && !category.equals("All")) {
            sql += " AND " + COL_CATEGORY + " = ?";
            args.add(category);
        }

        sql += " ORDER BY " + COL_DATE + " DESC, " + COL_ID + " DESC";
        return db.rawQuery(sql, args.toArray(new String[0]));
    }

    public double getSpentToday(String todayStr) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT SUM(" + COL_AMOUNT + ") FROM " + TABLE_NAME + " WHERE " + COL_DATE + " = ?", new String[]{todayStr});
        double sum = 0;
        if (cursor.moveToFirst()) {
            sum = cursor.getDouble(0);
        }
        cursor.close();
        return sum;
    }

    public double getSpentRange(String startStr, String endStr) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT SUM(" + COL_AMOUNT + ") FROM " + TABLE_NAME + " WHERE " + COL_DATE + " >= ? AND " + COL_DATE + " <= ?", new String[]{startStr, endStr});
        double sum = 0;
        if (cursor.moveToFirst()) {
            sum = cursor.getDouble(0);
        }
        cursor.close();
        return sum;
    }

    public double getSpentMonth(String yearMonthPattern) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT SUM(" + COL_AMOUNT + ") FROM " + TABLE_NAME + " WHERE " + COL_DATE + " LIKE ?", new String[]{yearMonthPattern + "%"});
        double sum = 0;
        if (cursor.moveToFirst()) {
            sum = cursor.getDouble(0);
        }
        cursor.close();
        return sum;
    }

    public Cursor getCategorySummary() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT " + COL_CATEGORY + ", SUM(" + COL_AMOUNT + ") FROM " + TABLE_NAME + " GROUP BY " + COL_CATEGORY, null);
    }
}