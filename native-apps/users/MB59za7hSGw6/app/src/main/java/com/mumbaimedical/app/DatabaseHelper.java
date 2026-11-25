package com.mumbaimedical.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "mumbaimedical.db";
    private static final int DATABASE_VERSION = 2;

    // Tables
    public static final String TABLE_MEDICINES = "medicines";
    public static final String TABLE_CUSTOMERS = "customers";
    public static final String TABLE_SUPPLIERS = "suppliers";
    public static final String TABLE_SALES = "sales";
    public static final String TABLE_SALE_ITEMS = "sale_items";
    public static final String TABLE_PURCHASES = "purchases";
    public static final String TABLE_KHATA = "khata_transactions";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Medicines Table
        db.execSQL("CREATE TABLE " + TABLE_MEDICINES + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, " +
                "company TEXT, " +
                "category TEXT, " +
                "type TEXT, " +
                "batch_number TEXT, " +
                "purchase_price REAL, " +
                "selling_price REAL, " +
                "quantity INTEGER, " +
                "min_stock INTEGER, " +
                "mfg_date TEXT, " +
                "exp_date TEXT, " +
                "rack_number TEXT, " +
                "notes TEXT" +
                ")");

        // Customers Table
        db.execSQL("CREATE TABLE " + TABLE_CUSTOMERS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, " +
                "phone TEXT, " +
                "address TEXT, " +
                "total_purchases REAL DEFAULT 0, " +
                "paid_amount REAL DEFAULT 0, " +
                "pending_balance REAL DEFAULT 0" +
                ")");

        // Suppliers Table
        db.execSQL("CREATE TABLE " + TABLE_SUPPLIERS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, " +
                "phone TEXT, " +
                "address TEXT, " +
                "total_purchases REAL DEFAULT 0, " +
                "paid_amount REAL DEFAULT 0, " +
                "pending_amount REAL DEFAULT 0" +
                ")");

        // Sales Table
        db.execSQL("CREATE TABLE " + TABLE_SALES + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "sale_date TEXT, " +
                "customer_id INTEGER DEFAULT -1, " +
                "customer_name TEXT, " +
                "total_amount REAL, " +
                "discount REAL, " +
                "grand_total REAL, " +
                "payment_method TEXT" +
                ")");

        // Sale Items Table
        db.execSQL("CREATE TABLE " + TABLE_SALE_ITEMS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "sale_id INTEGER, " +
                "medicine_id INTEGER, " +
                "medicine_name TEXT, " +
                "batch_number TEXT, " +
                "quantity INTEGER, " +
                "price REAL, " +
                "total REAL" +
                ")");

        // Purchases Table
        db.execSQL("CREATE TABLE " + TABLE_PURCHASES + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "purchase_date TEXT, " +
                "supplier_name TEXT, " +
                "total_amount REAL, " +
                "paid_amount REAL, " +
                "payment_status TEXT" +
                ")");

        // Customer Khata Transactions Table
        db.execSQL("CREATE TABLE " + TABLE_KHATA + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "customer_id INTEGER NOT NULL, " +
                "type TEXT NOT NULL, " + // "UDHAR" or "JAMA"
                "amount REAL NOT NULL, " +
                "date TEXT NOT NULL, " +
                "description TEXT, " +
                "payment_mode TEXT, " + // Cash, UPI, Cheque, Other
                "reference TEXT" +
                ")");

        // Note: Demo data seeding removed for clean initial installation.
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // Safe non-destructive upgrade for existing installations
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_KHATA + " (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "customer_id INTEGER NOT NULL, " +
                    "type TEXT NOT NULL, " +
                    "amount REAL NOT NULL, " +
                    "date TEXT NOT NULL, " +
                    "description TEXT, " +
                    "payment_mode TEXT, " +
                    "reference TEXT" +
                    ")");

            try {
                db.execSQL("ALTER TABLE " + TABLE_SALES + " ADD COLUMN customer_id INTEGER DEFAULT -1");
            } catch (Exception ignored) {
                // Column may already exist
            }
        }
    }

    // --- CUSTOMER & KHATA DATABASE METHODS ---

    public long addCustomer(String name, String phone, String address) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("phone", phone);
        cv.put("address", address);
        cv.put("total_purchases", 0.0);
        cv.put("paid_amount", 0.0);
        cv.put("pending_balance", 0.0);
        return db.insert(TABLE_CUSTOMERS, null, cv);
    }

    public boolean updateCustomer(long id, String name, String phone, String address) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("phone", phone);
        cv.put("address", address);
        return db.update(TABLE_CUSTOMERS, cv, "id = ?", new String[]{String.valueOf(id)}) > 0;
    }

    public Cursor searchCustomers(String query) {
        SQLiteDatabase db = getReadableDatabase();
        if (query == null || query.trim().isEmpty()) {
            return db.rawQuery("SELECT id AS _id, name, phone, address, pending_balance FROM " + TABLE_CUSTOMERS + " ORDER BY name ASC", null);
        } else {
            String q = "%" + query.trim() + "%";
            return db.rawQuery("SELECT id AS _id, name, phone, address, pending_balance FROM " + TABLE_CUSTOMERS +
                    " WHERE name LIKE ? OR phone LIKE ? ORDER BY name ASC", new String[]{q, q});
        }
    }

    public Cursor getCustomerById(long customerId) {
        SQLiteDatabase db = getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_CUSTOMERS + " WHERE id = ?", new String[]{String.valueOf(customerId)});
    }

    public void addKhataTransaction(long customerId, String type, double amount, String date, String description, String paymentMode, String reference) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues cv = new ContentValues();
            cv.put("customer_id", customerId);
            cv.put("type", type.toUpperCase()); // UDHAR or JAMA
            cv.put("amount", amount);
            cv.put("date", date);
            cv.put("description", description);
            cv.put("payment_mode", paymentMode);
            cv.put("reference", reference);

            db.insert(TABLE_KHATA, null, cv);
            recalculateCustomerBalance(db, customerId);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void deleteKhataTransaction(long transactionId, long customerId) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete(TABLE_KHATA, "id = ?", new String[]{String.valueOf(transactionId)});
            recalculateCustomerBalance(db, customerId);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public double getCustomerTotalUdhar(long customerId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT SUM(amount) FROM " + TABLE_KHATA + " WHERE customer_id = ? AND type = 'UDHAR'", new String[]{String.valueOf(customerId)});
        double total = 0.0;
        if (cursor.moveToFirst()) {
            total = cursor.getDouble(0);
        }
        cursor.close();
        return total;
    }

    public double getCustomerTotalJama(long customerId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT SUM(amount) FROM " + TABLE_KHATA + " WHERE customer_id = ? AND type = 'JAMA'", new String[]{String.valueOf(customerId)});
        double total = 0.0;
        if (cursor.moveToFirst()) {
            total = cursor.getDouble(0);
        }
        cursor.close();
        return total;
    }

    public void recalculateCustomerBalance(SQLiteDatabase db, long customerId) {
        Cursor cUdhar = db.rawQuery("SELECT SUM(amount) FROM " + TABLE_KHATA + " WHERE customer_id = ? AND type = 'UDHAR'", new String[]{String.valueOf(customerId)});
        double totalUdhar = 0.0;
        if (cUdhar.moveToFirst()) {
            totalUdhar = cUdhar.getDouble(0);
        }
        cUdhar.close();

        Cursor cJama = db.rawQuery("SELECT SUM(amount) FROM " + TABLE_KHATA + " WHERE customer_id = ? AND type = 'JAMA'", new String[]{String.valueOf(customerId)});
        double totalJama = 0.0;
        if (cJama.moveToFirst()) {
            totalJama = cJama.getDouble(0);
        }
        cJama.close();

        double pendingBalance = totalUdhar - totalJama;

        ContentValues cv = new ContentValues();
        cv.put("paid_amount", totalJama);
        cv.put("pending_balance", pendingBalance);

        db.update(TABLE_CUSTOMERS, cv, "id = ?", new String[]{String.valueOf(customerId)});
    }

    public Cursor getCustomerKhataHistory(long customerId) {
        SQLiteDatabase db = getReadableDatabase();
        return db.rawQuery("SELECT id AS _id, type, amount, date, description, payment_mode, reference FROM " + TABLE_KHATA +
                " WHERE customer_id = ? ORDER BY id DESC", new String[]{String.valueOf(customerId)});
    }

    // --- DASHBOARD CALCULATIONS ---

    public int getTotalMedicinesCount() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_MEDICINES, null);
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

    public double getTotalStockValue() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT SUM(purchase_price * quantity) FROM " + TABLE_MEDICINES, null);
        double val = 0.0;
        if (cursor.moveToFirst()) {
            val = cursor.getDouble(0);
        }
        cursor.close();
        return val;
    }

    public double getTodaySalesTotal() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT SUM(grand_total) FROM " + TABLE_SALES + " WHERE sale_date LIKE ?", new String[]{today + "%"});
        double total = 0.0;
        if (cursor.moveToFirst()) {
            total = cursor.getDouble(0);
        }
        cursor.close();
        return total;
    }

    public double getTodayPurchasesTotal() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT SUM(total_amount) FROM " + TABLE_PURCHASES + " WHERE purchase_date LIKE ?", new String[]{today + "%"});
        double total = 0.0;
        if (cursor.moveToFirst()) {
            total = cursor.getDouble(0);
        }
        cursor.close();
        return total;
    }

    public double getTodayProfit() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT SUM((si.price - m.purchase_price) * si.quantity) " +
                "FROM " + TABLE_SALE_ITEMS + " si " +
                "JOIN " + TABLE_SALES + " s ON si.sale_id = s.id " +
                "JOIN " + TABLE_MEDICINES + " m ON si.medicine_id = m.id " +
                "WHERE s.sale_date LIKE ?";
        Cursor cursor = db.rawQuery(query, new String[]{today + "%"});
        double profit = 0.0;
        if (cursor.moveToFirst()) {
            profit = cursor.getDouble(0);
        }
        cursor.close();
        return profit;
    }

    public int getLowStockCount() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_MEDICINES + " WHERE quantity <= min_stock", null);
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

    public int getExpiringMedicinesCount() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_MEDICINES + " WHERE exp_date <= date('now', '+60 days')", null);
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }
}