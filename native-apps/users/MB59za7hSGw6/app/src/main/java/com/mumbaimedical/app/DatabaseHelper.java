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
    private static final int DATABASE_VERSION = 1;

    // Tables
    public static final String TABLE_MEDICINES = "medicines";
    public static final String TABLE_CUSTOMERS = "customers";
    public static final String TABLE_SUPPLIERS = "suppliers";
    public static final String TABLE_SALES = "sales";
    public static final String TABLE_SALE_ITEMS = "sale_items";
    public static final String TABLE_PURCHASES = "purchases";

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

        // Seed initial sample data if empty
        seedData(db);
    }

    private void seedData(SQLiteDatabase db) {
        ContentValues cv1 = new ContentValues();
        cv1.put("name", "Paracetamol 500mg");
        cv1.put("company", "Cipla Ltd");
        cv1.put("category", "Tablet");
        cv1.put("type", "General");
        cv1.put("batch_number", "PCM-2024-01");
        cv1.put("purchase_price", 12.50);
        cv1.put("selling_price", 20.00);
        cv1.put("quantity", 150);
        cv1.put("min_stock", 20);
        cv1.put("mfg_date", "2023-10-01");
        cv1.put("exp_date", "2025-12-31");
        cv1.put("rack_number", "A-1");
        cv1.put("notes", "Fever reducer");
        db.insert(TABLE_MEDICINES, null, cv1);

        ContentValues cv2 = new ContentValues();
        cv2.put("name", "Amoxicillin 250mg");
        cv2.put("company", "Sun Pharma");
        cv2.put("category", "Capsule");
        cv2.put("type", "Antibiotic");
        cv2.put("batch_number", "AMX-882");
        cv2.put("purchase_price", 45.00);
        cv2.put("selling_price", 65.00);
        cv2.put("quantity", 12);
        cv2.put("min_stock", 15);
        cv2.put("mfg_date", "2023-05-15");
        cv2.put("exp_date", "2024-06-30");
        cv2.put("rack_number", "B-3");
        cv2.put("notes", "Store in cool place");
        db.insert(TABLE_MEDICINES, null, cv2);

        ContentValues cv3 = new ContentValues();
        cv3.put("name", "Cough Syrup 100ml");
        cv3.put("company", "Dabur");
        cv3.put("category", "Syrup");
        cv3.put("type", "OTC");
        cv3.put("batch_number", "CS-991");
        cv3.put("purchase_price", 50.00);
        cv3.put("selling_price", 85.00);
        cv3.put("quantity", 80);
        cv3.put("min_stock", 10);
        cv3.put("mfg_date", "2023-01-10");
        cv3.put("exp_date", "2024-11-20");
        cv3.put("rack_number", "C-2");
        cv3.put("notes", "Ayurvedic relief");
        db.insert(TABLE_MEDICINES, null, cv3);

        // Sample Customer
        ContentValues cust = new ContentValues();
        cust.put("name", "Rajesh Sharma");
        cust.put("phone", "9820012345");
        cust.put("address", "Andheri West, Mumbai");
        cust.put("total_purchases", 450.00);
        cust.put("paid_amount", 400.00);
        cust.put("pending_balance", 50.00);
        db.insert(TABLE_CUSTOMERS, null, cust);

        // Sample Supplier
        ContentValues supp = new ContentValues();
        supp.put("name", "Apex Pharma Distributors");
        supp.put("phone", "9819988776");
        supp.put("address", "Dadar Commercial Hub, Mumbai");
        supp.put("total_purchases", 5000.00);
        supp.put("paid_amount", 5000.00);
        supp.put("pending_amount", 0.00);
        db.insert(TABLE_SUPPLIERS, null, supp);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MEDICINES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CUSTOMERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SUPPLIERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SALES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SALE_ITEMS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PURCHASES);
        onCreate(db);
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
        // Profit = (Sale Item Selling Price - Medicine Purchase Price) * Item Qty
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
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        SQLiteDatabase db = getReadableDatabase();
        // Expired or expiring within 60 days
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_MEDICINES + " WHERE exp_date <= date('now', '+60 days')", null);
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }
}