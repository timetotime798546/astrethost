package com.expensetrackerpro.app;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "expenses.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_TRANSACTIONS = "transactions";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_AMOUNT = "amount";
    public static final String COLUMN_TYPE = "type"; // "INCOME" or "EXPENSE"
    public static final String COLUMN_CATEGORY = "category";
    public static final String COLUMN_DATE = "date"; // YYYY-MM-DD
    public static final String COLUMN_NOTE = "note";

    private static final String TABLE_CREATE =
            "CREATE TABLE " + TABLE_TRANSACTIONS + " (" +
            COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_AMOUNT + " REAL NOT NULL, " +
            COLUMN_TYPE + " TEXT NOT NULL, " +
            COLUMN_CATEGORY + " TEXT, " +
            COLUMN_DATE + " TEXT NOT NULL, " +
            COLUMN_NOTE + " TEXT);";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE);
        // Seed mock transaction data for demo purposes
        db.execSQL("INSERT INTO " + TABLE_TRANSACTIONS + " (" + COLUMN_AMOUNT + ", " + COLUMN_TYPE + ", " + COLUMN_CATEGORY + ", " + COLUMN_DATE + ", " + COLUMN_NOTE + ") VALUES (3200.00, 'INCOME', 'Salary', '2023-11-01', 'Monthly Salary paycheck');");
        db.execSQL("INSERT INTO " + TABLE_TRANSACTIONS + " (" + COLUMN_AMOUNT + ", " + COLUMN_TYPE + ", " + COLUMN_CATEGORY + ", " + COLUMN_DATE + ", " + COLUMN_NOTE + ") VALUES (1200.00, 'EXPENSE', 'Rent', '2023-11-02', 'Apartment monthly rent');");
        db.execSQL("INSERT INTO " + TABLE_TRANSACTIONS + " (" + COLUMN_AMOUNT + ", " + COLUMN_TYPE + ", " + COLUMN_CATEGORY + ", " + COLUMN_DATE + ", " + COLUMN_NOTE + ") VALUES (145.50, 'EXPENSE', 'Food', '2023-11-03', 'Whole Foods grocery run');");
        db.execSQL("INSERT INTO " + TABLE_TRANSACTIONS + " (" + COLUMN_AMOUNT + ", " + COLUMN_TYPE + ", " + COLUMN_CATEGORY + ", " + COLUMN_DATE + ", " + COLUMN_NOTE + ") VALUES (65.00, 'EXPENSE', 'Utilities', '2023-11-04', 'Electricity bill payment');");
        db.execSQL("INSERT INTO " + TABLE_TRANSACTIONS + " (" + COLUMN_AMOUNT + ", " + COLUMN_TYPE + ", " + COLUMN_CATEGORY + ", " + COLUMN_DATE + ", " + COLUMN_NOTE + ") VALUES (350.00, 'INCOME', 'Freelance', '2023-11-05', 'Client UI consultancy work');");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRANSACTIONS);
        onCreate(db);
    }
}