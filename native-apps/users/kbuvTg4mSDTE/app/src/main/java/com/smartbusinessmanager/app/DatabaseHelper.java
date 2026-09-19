package com.smartbusinessmanager.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "smart_business_manager.db";
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
                "current_stock INTEGER," +
                "minimum_stock INTEGER" +
                ")");

        // Customers Table
        db.execSQL("CREATE TABLE customers (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT," +
                "phone TEXT," +
                "address TEXT" +
                ")");

        // Sales Table
        db.execSQL("CREATE TABLE sales (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "customer_id INTEGER," +
                "date TEXT," + // Format: YYYY-MM-DD
                "subtotal REAL," +
                "discount REAL," +
                "tax REAL," +
                "grand_total REAL," +
                "amount_received REAL," +
                "remaining_balance REAL," +
                "invoice_no TEXT UNIQUE" +
                ")");

        // Sale Items Table (Relational items mapping)
        db.execSQL("CREATE TABLE sale_items (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "sale_id INTEGER," +
                "product_id INTEGER," +
                "quantity INTEGER," +
                "price REAL" +
                ")");

        // Expenses Table
        db.execSQL("CREATE TABLE expenses (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "category TEXT," +
                "amount REAL," +
                "date TEXT," + // Format: YYYY-MM-DD
                "description TEXT" +
                ")");

        // App General Configuration Settings
        db.execSQL("CREATE TABLE settings (" +
                "key TEXT PRIMARY KEY," +
                "value TEXT" +
                ")");

        // Populate Default App System Configurations
        db.execSQL("INSERT INTO settings (key, value) VALUES ('business_name', 'Smart Enterprise Ltd')");
        db.execSQL("INSERT INTO settings (key, value) VALUES ('owner_name', 'Chief Administrator')");
        db.execSQL("INSERT INTO settings (key, value) VALUES ('phone', '+1 555 987 654')");
        db.execSQL("INSERT INTO settings (key, value) VALUES ('currency', '$')");
        db.execSQL("INSERT INTO settings (key, value) VALUES ('tax_rate', '8.0')");
        db.execSQL("INSERT INTO settings (key, value) VALUES ('invoice_prefix', 'INV-')");
        db.execSQL("INSERT INTO settings (key, value) VALUES ('dark_theme', '0')"); // 0=Light, 1=Dark

        // Inject Pre-populated mock items to allow elegant testing right away!
        db.execSQL("INSERT INTO products (name, sku, category, purchase_price, selling_price, current_stock, minimum_stock) VALUES " +
                "('Organic Coffee Beans', 'COF-101', 'Food & Beverage', 12.00, 24.50, 45, 10)");
        db.execSQL("INSERT INTO products (name, sku, category, purchase_price, selling_price, current_stock, minimum_stock) VALUES " +
                "('Precision Office Laser Mouse', 'MOU-302', 'Electronics', 8.50, 19.99, 4, 8)");
        db.execSQL("INSERT INTO products (name, sku, category, purchase_price, selling_price, current_stock, minimum_stock) VALUES " +
                "('Executive Leather Notebook', 'NOT-009', 'Office Supplies', 5.00, 15.00, 30, 5)");

        db.execSQL("INSERT INTO customers (name, phone, address) VALUES " +
                "('Alexander Hamilton', '202-555-0143', '12 Wall Street, NY')");
        db.execSQL("INSERT INTO customers (name, phone, address) VALUES " +
                "('Elizabeth Schuyler', '202-555-0177', 'Albany Mansion, NY')");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS products");
        db.execSQL("DROP TABLE IF EXISTS customers");
        db.execSQL("DROP TABLE IF EXISTS sales");
        db.execSQL("DROP TABLE IF EXISTS sale_items");
        db.execSQL("DROP TABLE IF EXISTS expenses");
        db.execSQL("DROP TABLE IF EXISTS settings");
        onCreate(db);
    }
}