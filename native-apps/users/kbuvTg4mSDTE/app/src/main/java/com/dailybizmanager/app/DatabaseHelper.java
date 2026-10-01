package com.dailybizmanager.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "dailybiz.db";
    private static final int DATABASE_VERSION = 1;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Products Table
        db.execSQL("CREATE TABLE products (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT," +
                "sku TEXT UNIQUE," +
                "category TEXT," +
                "purchase_price REAL," +
                "selling_price REAL," +
                "stock INTEGER," +
                "min_stock INTEGER," +
                "unit TEXT)");

        // Customers Table
        db.execSQL("CREATE TABLE customers (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT," +
                "phone TEXT," +
                "address TEXT," +
                "total_purchases REAL DEFAULT 0," +
                "amount_paid REAL DEFAULT 0," +
                "balance REAL DEFAULT 0)");

        // Sales Table
        db.execSQL("CREATE TABLE sales (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "invoice_no TEXT UNIQUE," +
                "customer_id INTEGER," +
                "date TEXT," +
                "subtotal REAL," +
                "discount REAL," +
                "tax REAL," +
                "total REAL," +
                "paid REAL," +
                "balance REAL)");

        // Sale Items Table
        db.execSQL("CREATE TABLE sale_items (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "sale_id INTEGER," +
                "product_id INTEGER," +
                "product_name TEXT," +
                "quantity INTEGER," +
                "selling_price REAL," +
                "total REAL)");

        // Purchases Table
        db.execSQL("CREATE TABLE purchases (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "product_id INTEGER," +
                "product_name TEXT," +
                "quantity INTEGER," +
                "purchase_price REAL," +
                "supplier TEXT," +
                "date TEXT)");

        // Expenses Table
        db.execSQL("CREATE TABLE expenses (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "title TEXT," +
                "category TEXT," +
                "amount REAL," +
                "date TEXT," +
                "notes TEXT)");

        // Settings Table
        db.execSQL("CREATE TABLE settings (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "shop_name TEXT," +
                "shop_phone TEXT," +
                "shop_address TEXT," +
                "currency TEXT," +
                "tax REAL," +
                "low_stock_threshold INTEGER)");

        // Insert Default Settings
        ContentValues values = new ContentValues();
        values.put("shop_name", "My Retail Store");
        values.put("shop_phone", "123-456-7890");
        values.put("shop_address", "123 Business Rd");
        values.put("currency", "$");
        values.put("tax", 5.0);
        values.put("low_stock_threshold", 5);
        db.insert("settings", null, values);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS products");
        db.execSQL("DROP TABLE IF EXISTS customers");
        db.execSQL("DROP TABLE IF EXISTS sales");
        db.execSQL("DROP TABLE IF EXISTS sale_items");
        db.execSQL("DROP TABLE IF EXISTS purchases");
        db.execSQL("DROP TABLE IF EXISTS expenses");
        db.execSQL("DROP TABLE IF EXISTS settings");
        onCreate(db);
    }
}