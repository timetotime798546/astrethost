package com.smartkhata.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "smart_khata.db";
    private static final int DATABASE_VERSION = 1;

    // Customer table definition
    public static final String TABLE_CUSTOMERS = "customers";
    public static final String COL_CUST_ID = "id";
    public static final String COL_CUST_NAME = "name";
    public static final String COL_CUST_PHONE = "phone";
    public static final String COL_CUST_ADDRESS = "address";
    public static final String COL_CUST_NOTES = "notes";

    // Transactions table definition
    public static final String TABLE_TRANSACTIONS = "transactions";
    public static final String COL_TX_ID = "id";
    public static final String COL_TX_CUST_ID = "customer_id";
    public static final String COL_TX_TYPE = "type"; // "UDHAAR" or "JAMA"
    public static final String COL_TX_AMOUNT = "amount";
    public static final String COL_TX_NOTE = "note";
    public static final String COL_TX_TIMESTAMP = "timestamp";

    // Helper Object definitions
    public static class Customer {
        public int id;
        public String name;
        public String phone;
        public String address;
        public String notes;
        public double balance; // Calculated dynamically: sum(Udhaar) - sum(Jama)
    }

    public static class Transaction {
        public int id;
        public int customerId;
        public String customerName;
        public String type;
        public double amount;
        public String note;
        public long timestamp;
    }

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create customers table
        String createCustomers = "CREATE TABLE " + TABLE_CUSTOMERS + " (" +
                COL_CUST_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_CUST_NAME + " TEXT NOT NULL, " +
                COL_CUST_PHONE + " TEXT, " +
                COL_CUST_ADDRESS + " TEXT, " +
                COL_CUST_NOTES + " TEXT" +
                ");";
        db.execSQL(createCustomers);

        // Create transactions table
        String createTransactions = "CREATE TABLE " + TABLE_TRANSACTIONS + " (" +
                COL_TX_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_TX_CUST_ID + " INTEGER NOT NULL, " +
                COL_TX_TYPE + " TEXT NOT NULL, " +
                COL_TX_AMOUNT + " REAL NOT NULL, " +
                COL_TX_NOTE + " TEXT, " +
                COL_TX_TIMESTAMP + " INTEGER NOT NULL, " +
                "FOREIGN KEY(" + COL_TX_CUST_ID + ") REFERENCES " + TABLE_CUSTOMERS + "(" + COL_CUST_ID + ") ON DELETE CASCADE" +
                ");";
        db.execSQL(createTransactions);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRANSACTIONS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CUSTOMERS);
        onCreate(db);
    }

    // CRUD Customer
    public long addCustomer(String name, String phone, String address, String notes) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_CUST_NAME, name);
        cv.put(COL_CUST_PHONE, phone);
        cv.put(COL_CUST_ADDRESS, address);
        cv.put(COL_CUST_NOTES, notes);
        return db.insert(TABLE_CUSTOMERS, null, cv);
    }

    public int updateCustomer(int id, String name, String phone, String address, String notes) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_CUST_NAME, name);
        cv.put(COL_CUST_PHONE, phone);
        cv.put(COL_CUST_ADDRESS, address);
        cv.put(COL_CUST_NOTES, notes);
        return db.update(TABLE_CUSTOMERS, cv, COL_CUST_ID + "=?", new String[]{String.valueOf(id)});
    }

    public void deleteCustomer(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        // Since foreign keys are configuration enabled with ON DELETE CASCADE, 
        // transactions will automatically get purged.
        db.delete(TABLE_CUSTOMERS, COL_CUST_ID + "=?", new String[]{String.valueOf(id)});
    }

    public Customer getCustomerById(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        String sql = "SELECT c.id, c.name, c.phone, c.address, c.notes, " +
                "COALESCE((SELECT SUM(t.amount) FROM transactions t WHERE t.customer_id = c.id AND t.type = 'UDHAAR'), 0) - " +
                "COALESCE((SELECT SUM(t.amount) FROM transactions t WHERE t.customer_id = c.id AND t.type = 'JAMA'), 0) AS balance " +
                "FROM customers c WHERE c.id = ?";
        
        Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(id)});
        Customer cust = null;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                cust = new Customer();
                cust.id = cursor.getInt(0);
                cust.name = cursor.getString(1);
                cust.phone = cursor.getString(2);
                cust.address = cursor.getString(3);
                cust.notes = cursor.getString(4);
                cust.balance = cursor.getDouble(5);
            }
            cursor.close();
        }
        return cust;
    }

    public List<Customer> getCustomers(String query) {
        List<Customer> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        String sql = "SELECT c.id, c.name, c.phone, c.address, c.notes, " +
                "COALESCE((SELECT SUM(t.amount) FROM transactions t WHERE t.customer_id = c.id AND t.type = 'UDHAAR'), 0) - " +
                "COALESCE((SELECT SUM(t.amount) FROM transactions t WHERE t.customer_id = c.id AND t.type = 'JAMA'), 0) AS balance " +
                "FROM customers c";
                
        if (query != null && !query.trim().isEmpty()) {
            sql += " WHERE c.name LIKE ? OR c.phone LIKE ?";
        }
        sql += " ORDER BY c.name ASC";
        
        Cursor cursor;
        if (query != null && !query.trim().isEmpty()) {
            String likeParam = "%" + query + "%";
            cursor = db.rawQuery(sql, new String[]{likeParam, likeParam});
        } else {
            cursor = db.rawQuery(sql, null);
        }
        
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    Customer cust = new Customer();
                    cust.id = cursor.getInt(0);
                    cust.name = cursor.getString(1);
                    cust.phone = cursor.getString(2);
                    cust.address = cursor.getString(3);
                    cust.notes = cursor.getString(4);
                    cust.balance = cursor.getDouble(5);
                    list.add(cust);
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        return list;
    }

    // CRUD Transaction
    public long addTransaction(int customerId, String type, double amount, String note, long timestamp) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_TX_CUST_ID, customerId);
        cv.put(COL_TX_TYPE, type);
        cv.put(COL_TX_AMOUNT, amount);
        cv.put(COL_TX_NOTE, note);
        cv.put(COL_TX_TIMESTAMP, timestamp);
        return db.insert(TABLE_TRANSACTIONS, null, cv);
    }

    public List<Transaction> getTransactions(int customerId, String typeFilter, String query) {
        List<Transaction> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        String sql = "SELECT t.id, t.customer_id, c.name, t.type, t.amount, t.note, t.timestamp " +
                "FROM transactions t INNER JOIN customers c ON t.customer_id = c.id";
                
        List<String> selectionArgs = new ArrayList<>();
        boolean hasWhere = false;
        
        if (customerId > 0) {
            sql += " WHERE t.customer_id = ?";
            selectionArgs.add(String.valueOf(customerId));
            hasWhere = true;
        }
        
        if (typeFilter != null && !typeFilter.equals("ALL")) {
            sql += (hasWhere ? " AND" : " WHERE") + " t.type = ?";
            selectionArgs.add(typeFilter);
            hasWhere = true;
        }
        
        if (query != null && !query.trim().isEmpty()) {
            sql += (hasWhere ? " AND" : " WHERE") + " (c.name LIKE ? OR t.note LIKE ?)";
            String likeParam = "%" + query + "%";
            selectionArgs.add(likeParam);
            selectionArgs.add(likeParam);
        }
        
        sql += " ORDER BY t.timestamp DESC";
        
        String[] args = selectionArgs.toArray(new String[0]);
        Cursor cursor = db.rawQuery(sql, args);
        
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    Transaction tx = new Transaction();
                    tx.id = cursor.getInt(0);
                    tx.customerId = cursor.getInt(1);
                    tx.customerName = cursor.getString(2);
                    tx.type = cursor.getString(3);
                    tx.amount = cursor.getDouble(4);
                    tx.note = cursor.getString(5);
                    tx.timestamp = cursor.getLong(6);
                    list.add(tx);
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        return list;
    }

    // Dynamic aggregated overview stats
    public Bundle getSummary() {
        Bundle bundle = new Bundle();
        SQLiteDatabase db = this.getReadableDatabase();
        
        // Customers Count
        Cursor cCust = db.rawQuery("SELECT COUNT(*) FROM customers", null);
        int custCount = 0;
        if (cCust != null) {
            if (cCust.moveToFirst()) custCount = cCust.getInt(0);
            cCust.close();
        }
        
        // Total Udhaar sum
        Cursor cUdhaar = db.rawQuery("SELECT SUM(amount) FROM transactions WHERE type = 'UDHAAR'", null);
        double totalUdhaar = 0;
        if (cUdhaar != null) {
            if (cUdhaar.moveToFirst()) totalUdhaar = cUdhaar.getDouble(0);
            cUdhaar.close();
        }
        
        // Total Jama sum
        Cursor cJama = db.rawQuery("SELECT SUM(amount) FROM transactions WHERE type = 'JAMA'", null);
        double totalJama = 0;
        if (cJama != null) {
            if (cJama.moveToFirst()) totalJama = cJama.getDouble(0);
            cJama.close();
        }
        
        double outstanding = totalUdhaar - totalJama;
        
        bundle.putInt("cust_count", custCount);
        bundle.putDouble("total_udhaar", totalUdhaar);
        bundle.putDouble("total_jama", totalJama);
        bundle.putDouble("outstanding", outstanding);
        
        return bundle;
    }

    public boolean isDatabaseEmpty() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM customers", null);
        boolean empty = true;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                empty = cursor.getInt(0) == 0;
            }
            cursor.close();
        }
        return empty;
    }

    public void clearAllData() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM transactions");
        db.execSQL("DELETE FROM customers");
    }

    // Populate demo data for realistic app demonstration
    public void populateDemoData() {
        clearAllData();
        
        long id1 = addCustomer("Rahul Kumar", "9876543210", "New Delhi, India", "Daily grocery milk buyer");
        long id2 = addCustomer("Amit Sharma", "8765432109", "Mumbai, Maharashtra", "Wholesale client");
        long id3 = addCustomer("Suresh Traders", "7654321098", "Bangalore, Karnataka", "Merchant partner");
        long id4 = addCustomer("Neha Verma", "6543210987", "Chennai, Tamil Nadu", "Tailor shop regular");

        long now = System.currentTimeMillis();
        
        // Rahul: 1200 Udhaar, 500 Jama = Outstanding 700
        addTransaction((int) id1, "UDHAAR", 1200.00, "Rice and Flour sack", now - 86400000L * 5);
        addTransaction((int) id1, "JAMA", 500.00, "UPI payment received", now - 86400000L * 3);

        // Amit: 2500 Udhaar, 1000 Jama = Outstanding 1500
        addTransaction((int) id2, "UDHAAR", 2500.00, "Packaged items & oil bottle", now - 86400000L * 4);
        addTransaction((int) id2, "JAMA", 1000.00, "Cash paid back", now - 86400000L * 1);

        // Suresh: 5000 Udhaar, 5000 Jama = Outstanding 0
        addTransaction((int) id3, "UDHAAR", 5000.00, "Bulk delivery pack", now - 86400000L * 10);
        addTransaction((int) id3, "JAMA", 5000.00, "Full balance cleared", now - 86400000L * 2);

        // Neha: 800 Udhaar = Outstanding 800
        addTransaction((int) id4, "UDHAAR", 800.00, "Thread raw accessories", now - 86400000L * 1);
    }
}