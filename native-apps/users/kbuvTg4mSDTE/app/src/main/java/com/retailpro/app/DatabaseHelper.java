package com.retailpro.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "retailpro.db";
    private static final int DATABASE_VERSION = 1;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // 1. settings table
        db.execSQL("CREATE TABLE settings (key_name TEXT PRIMARY KEY, val TEXT)");

        // 2. products table
        db.execSQL("CREATE TABLE products (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT, " +
                "sku TEXT, " +
                "barcode TEXT, " +
                "category TEXT, " +
                "purchase_price REAL, " +
                "selling_price REAL, " +
                "mrp REAL, " +
                "gst_percentage REAL, " +
                "current_stock REAL, " +
                "minimum_stock REAL, " +
                "unit TEXT)");

        // 3. customers table
        db.execSQL("CREATE TABLE customers (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT, " +
                "mobile TEXT, " +
                "address TEXT, " +
                "outstanding_amount REAL DEFAULT 0)");

        // 4. suppliers table
        db.execSQL("CREATE TABLE suppliers (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT, " +
                "mobile TEXT, " +
                "address TEXT, " +
                "outstanding_amount REAL DEFAULT 0)");

        // 5. sales table
        db.execSQL("CREATE TABLE sales (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "invoice_number TEXT, " +
                "date TEXT, " +
                "customer_id INTEGER, " +
                "discount REAL, " +
                "tax_amount REAL, " +
                "subtotal REAL, " +
                "grand_total REAL, " +
                "payment_method TEXT, " +
                "paid_amount REAL, " +
                "due_amount REAL)");

        // 6. sale_items table
        db.execSQL("CREATE TABLE sale_items (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "sale_id INTEGER, " +
                "product_id INTEGER, " +
                "product_name TEXT, " +
                "quantity REAL, " +
                "price REAL, " +
                "gst_percentage REAL, " +
                "total REAL)");

        // 7. purchases table
        db.execSQL("CREATE TABLE purchases (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "supplier_id INTEGER, " +
                "date TEXT, " +
                "grand_total REAL, " +
                "payment_method TEXT, " +
                "paid_amount REAL, " +
                "due_amount REAL)");

        // 8. purchase_items table
        db.execSQL("CREATE TABLE purchase_items (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "purchase_id INTEGER, " +
                "product_id INTEGER, " +
                "product_name TEXT, " +
                "quantity REAL, " +
                "price REAL, " +
                "gst_percentage REAL, " +
                "total REAL)");

        // 9. expenses table
        db.execSQL("CREATE TABLE expenses (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "title TEXT, " +
                "amount REAL, " +
                "category TEXT, " +
                "note TEXT, " +
                "date TEXT)");

        // Insert initial setup settings
        insertDefaultSettings(db);
        // Insert sample demonstration data
        seedDemoData(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS settings");
        db.execSQL("DROP TABLE IF EXISTS products");
        db.execSQL("DROP TABLE IF EXISTS customers");
        db.execSQL("DROP TABLE IF EXISTS suppliers");
        db.execSQL("DROP TABLE IF EXISTS sales");
        db.execSQL("DROP TABLE IF EXISTS sale_items");
        db.execSQL("DROP TABLE IF EXISTS purchases");
        db.execSQL("DROP TABLE IF EXISTS purchase_items");
        db.execSQL("DROP TABLE IF EXISTS expenses");
        onCreate(db);
    }

    private void insertDefaultSettings(SQLiteDatabase db) {
        db.execSQL("INSERT OR REPLACE INTO settings (key_name, val) VALUES ('shop_name', 'Kirana Supermart')");
        db.execSQL("INSERT OR REPLACE INTO settings (key_name, val) VALUES ('owner_name', 'Rahul Sharma')");
        db.execSQL("INSERT OR REPLACE INTO settings (key_name, val) VALUES ('mobile', '9876543210')");
        db.execSQL("INSERT OR REPLACE INTO settings (key_name, val) VALUES ('address', 'Block A, Connaught Place, New Delhi')");
        db.execSQL("INSERT OR REPLACE INTO settings (key_name, val) VALUES ('gst_number', '07AAAAA1111A1Z1')");
        db.execSQL("INSERT OR REPLACE INTO settings (key_name, val) VALUES ('invoice_prefix', 'RP-')");
        db.execSQL("INSERT OR REPLACE INTO settings (key_name, val) VALUES ('default_gst', '18')");
        db.execSQL("INSERT OR REPLACE INTO settings (key_name, val) VALUES ('currency', '₹')");
    }

    private void seedDemoData(SQLiteDatabase db) {
        // Products
        db.execSQL("INSERT INTO products (name, sku, barcode, category, purchase_price, selling_price, mrp, gst_percentage, current_stock, minimum_stock, unit) VALUES " +
                "('Basmati Rice Premium 5KG', 'RICE01', '1001', 'Groceries', 380, 450, 499, 5, 50, 10, 'Pack')");
        db.execSQL("INSERT INTO products (name, sku, barcode, category, purchase_price, selling_price, mrp, gst_percentage, current_stock, minimum_stock, unit) VALUES " +
                "('Fortune Mustard Oil 1L', 'OIL02', '1002', 'Groceries', 135, 160, 185, 5, 75, 15, 'Bottle')");
        db.execSQL("INSERT INTO products (name, sku, barcode, category, purchase_price, selling_price, mrp, gst_percentage, current_stock, minimum_stock, unit) VALUES " +
                "('Dairy Milk Silk Chocolate', 'DM03', '1003', 'Snacks', 32, 40, 40, 18, 110, 20, 'Pcs')");
        db.execSQL("INSERT INTO products (name, sku, barcode, category, purchase_price, selling_price, mrp, gst_percentage, current_stock, minimum_stock, unit) VALUES " +
                "('Tata Salt Refined 1KG', 'SALT04', '1004', 'Groceries', 20, 25, 28, 0, 5, 12, 'Pack')"); // low stock demo
        db.execSQL("INSERT INTO products (name, sku, barcode, category, purchase_price, selling_price, mrp, gst_percentage, current_stock, minimum_stock, unit) VALUES " +
                "('Surf Excel detergent 1KG', 'SURF05', '1005', 'Households', 110, 140, 150, 18, 32, 8, 'Pack')");

        // Customers
        db.execSQL("INSERT INTO customers (name, mobile, address, outstanding_amount) VALUES " +
                "('Amit Singh', '9911223344', 'Mayur Vihar, Delhi', 450)");
        db.execSQL("INSERT INTO customers (name, mobile, address, outstanding_amount) VALUES " +
                "('Pooja Nair', '9898989898', 'Sector 15, Noida', 0)");
        db.execSQL("INSERT INTO customers (name, mobile, address, outstanding_amount) VALUES " +
                "('Jaspreet Singh', '9555111222', 'Gurgaon Phase 2', 120)");

        // Suppliers
        db.execSQL("INSERT INTO suppliers (name, mobile, address, outstanding_amount) VALUES " +
                "('Metro Wholesale Distributors', '9810012345', 'Okhla Industrial Area', 0)");
        db.execSQL("INSERT INTO suppliers (name, mobile, address, outstanding_amount) VALUES " +
                "('Apex FMCG Suppliers Ltd', '9212345678', 'Sadar Bazar, Delhi', 2500)");

        // Expenses
        db.execSQL("INSERT INTO expenses (title, amount, category, note, date) VALUES " +
                "('Shop Electricity Bill', 1250, 'Utilities', 'Payment for June month', '2023-11-20')");
        db.execSQL("INSERT INTO expenses (title, amount, category, note, date) VALUES " +
                "('Drinking Water', 120, 'Snacks', 'Jar bottles water cost', '2023-11-21')");
    }

    public String getSetting(String key) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT val FROM settings WHERE key_name = ?", new String[]{key});
        String value = "";
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                value = cursor.getString(0);
            }
            cursor.close();
        }
        return value;
    }

    public void setSetting(String key, String value) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("key_name", key);
        values.put("val", value);
        db.insertWithOnConflict("settings", null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }
}