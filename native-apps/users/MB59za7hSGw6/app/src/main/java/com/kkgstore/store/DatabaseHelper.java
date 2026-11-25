package com.kkgstore.store;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "kkg_store.db";
    private static final int DATABASE_VERSION = 2;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS customers ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "name TEXT, "
                + "mobile TEXT, "
                + "opening_balance REAL DEFAULT 0, "
                + "is_active INTEGER DEFAULT 1)");

        db.execSQL("CREATE TABLE IF NOT EXISTS products ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "name TEXT, "
                + "category TEXT, "
                + "unit TEXT, "
                + "cost_price REAL, "
                + "sell_price REAL, "
                + "stock REAL)");

        db.execSQL("CREATE TABLE IF NOT EXISTS udhaar_entries ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "customer_id INTEGER, "
                + "amount REAL, "
                + "notes TEXT, "
                + "entry_date TEXT, "
                + "photo_path TEXT)");

        db.execSQL("CREATE TABLE IF NOT EXISTS jama_entries ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "customer_id INTEGER, "
                + "amount REAL, "
                + "method TEXT, "
                + "notes TEXT, "
                + "entry_date TEXT)");

        db.execSQL("CREATE TABLE IF NOT EXISTS sales ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "bill_no INTEGER, "
                + "customer_id INTEGER, "
                + "subtotal REAL, "
                + "discount REAL, "
                + "grand_total REAL, "
                + "payment_mode TEXT, "
                + "sale_date TEXT)");

        db.execSQL("CREATE TABLE IF NOT EXISTS sale_items ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "sale_id INTEGER, "
                + "product_id INTEGER, "
                + "product_name TEXT, "
                + "unit TEXT, "
                + "quantity REAL, "
                + "price REAL, "
                + "total REAL)");

        db.execSQL("CREATE TABLE IF NOT EXISTS purchases ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "supplier TEXT, "
                + "product_id INTEGER, "
                + "quantity REAL, "
                + "amount REAL, "
                + "purchase_date TEXT)");

        db.execSQL("CREATE TABLE IF NOT EXISTS expenses ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "category TEXT, "
                + "amount REAL, "
                + "notes TEXT, "
                + "expense_date TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            try {
                db.execSQL("ALTER TABLE udhaar_entries ADD COLUMN photo_path TEXT");
            } catch (Exception ignored) {
                // Column might already exist
            }
        }
    }
}