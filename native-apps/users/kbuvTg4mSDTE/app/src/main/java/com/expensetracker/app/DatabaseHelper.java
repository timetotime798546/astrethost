package com.expensetracker.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = \"expense_tracker.db\";
    private static final int DATABASE_VERSION = 1;

    // Categories Table
    public static final String TABLE_CATEGORIES = \"categories\";
    public static final String COLUMN_CATEGORY_ID = \"_id\";
    public static final String COLUMN_CATEGORY_NAME = \"name\";

    // Transactions Table
    public static final String TABLE_TRANSACTIONS = \"transactions\";
    public static final String COLUMN_TRANSACTION_ID = \"_id\";
    public static final String COLUMN_TRANSACTION_TYPE = \"type\"; // \"income\" or \"expense\"
    public static final String COLUMN_TRANSACTION_AMOUNT = \"amount\";
    public static final String COLUMN_TRANSACTION_DESCRIPTION = \"description\";
    public static final String COLUMN_TRANSACTION_DATE = \"date\"; // YYYY-MM-DD
    public static final String COLUMN_TRANSACTION_CATEGORY_ID = \"category_id\"; // Foreign key

    // Create Categories Table
    private static final String CREATE_TABLE_CATEGORIES = \"CREATE TABLE \" + TABLE_CATEGORIES + \"(\" +
            COLUMN_CATEGORY_ID + \" INTEGER PRIMARY KEY AUTOINCREMENT,\" +
            COLUMN_CATEGORY_NAME + \" TEXT NOT NULL UNIQUE\" +
            \")\";

    // Create Transactions Table
    private static final String CREATE_TABLE_TRANSACTIONS = \"CREATE TABLE \" + TABLE_TRANSACTIONS + \"(\" +
            COLUMN_TRANSACTION_ID + \" INTEGER PRIMARY KEY AUTOINCREMENT,\" +
            COLUMN_TRANSACTION_TYPE + \" TEXT NOT NULL,\" +
            COLUMN_TRANSACTION_AMOUNT + \" REAL NOT NULL,\" +
            COLUMN_TRANSACTION_DESCRIPTION + \" TEXT,\" +
            COLUMN_TRANSACTION_DATE + \" TEXT NOT NULL,\" +
            COLUMN_TRANSACTION_CATEGORY_ID + \" INTEGER,\" +
            \"FOREIGN KEY(\" + COLUMN_TRANSACTION_CATEGORY_ID + \") REFERENCES \" +
            TABLE_CATEGORIES + \"(\" + COLUMN_CATEGORY_ID + \") ON DELETE SET NULL\" +
            \")\";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_CATEGORIES);
        db.execSQL(CREATE_TABLE_TRANSACTIONS);

        // Insert some default categories
        insertCategory(db, \"Food\");
        insertCategory(db, \"Transport\");
        insertCategory(db, \"Salary\");
        insertCategory(db, \"Utilities\");
        insertCategory(db, \"Entertainment\");
    }

    private void insertCategory(SQLiteDatabase db, String categoryName) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_CATEGORY_NAME, categoryName);
        db.insert(TABLE_CATEGORIES, null, values);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(\"DROP TABLE IF EXISTS \" + TABLE_TRANSACTIONS);
        db.execSQL(\"DROP TABLE IF EXISTS \" + TABLE_CATEGORIES);
        onCreate(db);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        if (!db.isReadOnly()) {
            // Enable foreign key constraints
            db.execSQL(\"PRAGMA foreign_keys=ON;\");
        }
    }

    // --- Category Operations ---

    public long addCategory(String name) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_CATEGORY_NAME, name);
        long id = db.insert(TABLE_CATEGORIES, null, values);
        db.close();
        return id;
    }

    public Cursor getAllCategories() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_CATEGORIES,
                new String[]{COLUMN_CATEGORY_ID, COLUMN_CATEGORY_NAME},
                null, null, null, null, COLUMN_CATEGORY_NAME + \" ASC\");
    }

    public String getCategoryName(long categoryId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String name = null;
        Cursor cursor = db.query(TABLE_CATEGORIES,
                new String[]{COLUMN_CATEGORY_NAME},
                COLUMN_CATEGORY_ID + \" = ?\",
                new String[]{String.valueOf(categoryId)},
                null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            name = cursor.getString(cursor.getColumnIndex(COLUMN_CATEGORY_NAME));
            cursor.close();
        }
        db.close();
        return name;
    }

    public int updateCategory(long id, String newName) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_CATEGORY_NAME, newName);
        int rowsAffected = db.update(TABLE_CATEGORIES, values, COLUMN_CATEGORY_ID + \" = ?\",
                new String[]{String.valueOf(id)});
        db.close();
        return rowsAffected;
    }

    public int deleteCategory(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsAffected = db.delete(TABLE_CATEGORIES, COLUMN_CATEGORY_ID + \" = ?\",
                new String[]{String.valueOf(id)});
        db.close();
        return rowsAffected;
    }

    // --- Transaction Operations ---

    public long addTransaction(String type, double amount, String description, String date, long categoryId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TRANSACTION_TYPE, type);
        values.put(COLUMN_TRANSACTION_AMOUNT, amount);
        values.put(COLUMN_TRANSACTION_DESCRIPTION, description);
        values.put(COLUMN_TRANSACTION_DATE, date);
        values.put(COLUMN_TRANSACTION_CATEGORY_ID, categoryId);
        long id = db.insert(TABLE_TRANSACTIONS, null, values);
        db.close();
        return id;
    }

    public Cursor getTransactions(int year, int month, String type) {
        SQLiteDatabase db = this.getReadableDatabase();
        String monthString = String.format(Locale.getDefault(), \"%02d\", month);
        String yearMonthPattern = year + \"-\" + monthString + \"%\";
        String selection = COLUMN_TRANSACTION_DATE + \" LIKE ? AND \" + COLUMN_TRANSACTION_TYPE + \" = ?\";
        String[] selectionArgs = {yearMonthPattern, type};

        return db.query(TABLE_TRANSACTIONS,
                null, // all columns
                selection,
                selectionArgs,
                null, null, COLUMN_TRANSACTION_DATE + \" DESC\");
    }

    public Cursor getAllTransactionsForMonth(int year, int month) {
        SQLiteDatabase db = this.getReadableDatabase();
        String monthString = String.format(Locale.getDefault(), \"%02d\", month);
        String yearMonthPattern = year + \"-\" + monthString + \"%\";
        String selection = COLUMN_TRANSACTION_DATE + \" LIKE ?\";
        String[] selectionArgs = {yearMonthPattern};

        return db.rawQuery(\"SELECT T._id, T.type, T.amount, T.description, T.date, C.name AS category_name \" +
                \"FROM \" + TABLE_TRANSACTIONS + \" T \" +
                \"LEFT JOIN \" + TABLE_CATEGORIES + \" C \" +
                \"ON T.\" + COLUMN_TRANSACTION_CATEGORY_ID + \" = C.\" + COLUMN_CATEGORY_ID +
                \" WHERE T.\" + COLUMN_TRANSACTION_DATE + \" LIKE ?\" +
                \" ORDER BY T.\" + COLUMN_TRANSACTION_DATE + \" DESC\", selectionArgs);
    }

    public double getMonthlyTotal(int year, int month, String type) {
        SQLiteDatabase db = this.getReadableDatabase();
        double total = 0;
        String monthString = String.format(Locale.getDefault(), \"%02d\", month);
        String yearMonthPattern = year + \"-\" + monthString + \"%\";

        String query = \"SELECT SUM(\" + COLUMN_TRANSACTION_AMOUNT + \") FROM \" + TABLE_TRANSACTIONS +
                \" WHERE \" + COLUMN_TRANSACTION_TYPE + \" = ? AND \" + COLUMN_TRANSACTION_DATE + \" LIKE ?\";
        Cursor cursor = db.rawQuery(query, new String[]{type, yearMonthPattern});

        if (cursor != null && cursor.moveToFirst()) {
            total = cursor.getDouble(0);
            cursor.close();
        }
        db.close();
        return total;
    }
}