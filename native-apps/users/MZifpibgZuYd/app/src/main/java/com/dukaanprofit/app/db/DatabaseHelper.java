package com.dukaanprofit.app.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.dukaanprofit.app.models.Customer;
import com.dukaanprofit.app.models.Expense;
import com.dukaanprofit.app.models.Product;
import com.dukaanprofit.app.models.Sale;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "dukaanprofit.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_PRODUCTS = "products";
    public static final String TABLE_SALES = "sales";
    public static final String TABLE_SALE_ITEMS = "sale_items";
    public static final String TABLE_EXPENSES = "expenses";
    public static final String TABLE_CUSTOMERS = "customers";
    public static final String TABLE_LEDGER = "ledger";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_PRODUCTS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, " +
                "sku TEXT UNIQUE, " +
                "cost_price REAL NOT NULL, " +
                "sell_price REAL NOT NULL, " +
                "quantity INTEGER NOT NULL DEFAULT 0, " +
                "threshold INTEGER NOT NULL DEFAULT 5);");

        db.execSQL("CREATE TABLE " + TABLE_CUSTOMERS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, " +
                "phone TEXT, " +
                "balance REAL DEFAULT 0.0);");

        db.execSQL("CREATE TABLE " + TABLE_SALES + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "customer_id INTEGER, " +
                "customer_name TEXT, " +
                "total_amount REAL NOT NULL, " +
                "total_cost REAL NOT NULL, " +
                "discount REAL DEFAULT 0.0, " +
                "final_amount REAL NOT NULL, " +
                "payment_type TEXT NOT NULL, " +
                "created_at TEXT NOT NULL);");

        db.execSQL("CREATE TABLE " + TABLE_SALE_ITEMS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "sale_id INTEGER NOT NULL, " +
                "product_id INTEGER NOT NULL, " +
                "product_name TEXT NOT NULL, " +
                "quantity INTEGER NOT NULL, " +
                "cost_price REAL NOT NULL, " +
                "sell_price REAL NOT NULL, " +
                "subtotal REAL NOT NULL);");

        db.execSQL("CREATE TABLE " + TABLE_EXPENSES + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "category TEXT NOT NULL, " +
                "amount REAL NOT NULL, " +
                "notes TEXT, " +
                "date TEXT NOT NULL);");

        db.execSQL("CREATE TABLE " + TABLE_LEDGER + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "customer_id INTEGER NOT NULL, " +
                "type TEXT NOT NULL, " + // 'CREDIT_SALE' or 'PAYMENT'
                "amount REAL NOT NULL, " +
                "notes TEXT, " +
                "date TEXT NOT NULL);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LEDGER);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_EXPENSES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SALE_ITEMS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SALES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CUSTOMERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PRODUCTS);
        onCreate(db);
    }

    // --- Product Operations ---
    public long insertProduct(String name, String sku, double cost, double sell, int qty, int threshold) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("sku", sku);
        cv.put("cost_price", cost);
        cv.put("sell_price", sell);
        cv.put("quantity", qty);
        cv.put("threshold", threshold);
        return db.insert(TABLE_PRODUCTS, null, cv);
    }

    public boolean updateProduct(long id, String name, String sku, double cost, double sell, int qty, int threshold) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("sku", sku);
        cv.put("cost_price", cost);
        cv.put("sell_price", sell);
        cv.put("quantity", qty);
        cv.put("threshold", threshold);
        return db.update(TABLE_PRODUCTS, cv, "id = ?", new String[]{String.valueOf(id)}) > 0;
    }

    public boolean deleteProduct(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_PRODUCTS, "id = ?", new String[]{String.valueOf(id)}) > 0;
    }

    public List<Product> getAllProducts(String query) {
        List<Product> list = new ArrayList<Product>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor;
        if (query == null || query.trim().isEmpty()) {
            cursor = db.rawQuery("SELECT * FROM " + TABLE_PRODUCTS + " ORDER BY name ASC", null);
        } else {
            String q = "%" + query.trim() + "%";
            cursor = db.rawQuery("SELECT * FROM " + TABLE_PRODUCTS + " WHERE name LIKE ? OR sku LIKE ? ORDER BY name ASC", new String[]{q, q});
        }

        if (cursor != null && cursor.moveToFirst()) {
            do {
                Product p = new Product(
                        cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                        cursor.getString(cursor.getColumnIndexOrThrow("name")),
                        cursor.getString(cursor.getColumnIndexOrThrow("sku")),
                        cursor.getDouble(cursor.getColumnIndexOrThrow("cost_price")),
                        cursor.getDouble(cursor.getColumnIndexOrThrow("sell_price")),
                        cursor.getInt(cursor.getColumnIndexOrThrow("quantity")),
                        cursor.getInt(cursor.getColumnIndexOrThrow("threshold"))
                );
                list.add(p);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return list;
    }

    public List<Product> getLowStockProducts() {
        List<Product> list = new ArrayList<Product>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_PRODUCTS + " WHERE quantity <= threshold ORDER BY quantity ASC", null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                list.add(new Product(
                        cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                        cursor.getString(cursor.getColumnIndexOrThrow("name")),
                        cursor.getString(cursor.getColumnIndexOrThrow("sku")),
                        cursor.getDouble(cursor.getColumnIndexOrThrow("cost_price")),
                        cursor.getDouble(cursor.getColumnIndexOrThrow("sell_price")),
                        cursor.getInt(cursor.getColumnIndexOrThrow("quantity")),
                        cursor.getInt(cursor.getColumnIndexOrThrow("threshold"))
                ));
            } while (cursor.moveToNext());
            cursor.close();
        }
        return list;
    }

    // --- Customer & Ledger Operations ---
    public long insertCustomer(String name, String phone) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("phone", phone);
        cv.put("balance", 0.0);
        return db.insert(TABLE_CUSTOMERS, null, cv);
    }

    public List<Customer> getAllCustomers() {
        List<Customer> list = new ArrayList<Customer>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + TABLE_CUSTOMERS + " ORDER BY name ASC", null);
        if (c != null && c.moveToFirst()) {
            do {
                list.add(new Customer(
                        c.getLong(c.getColumnIndexOrThrow("id")),
                        c.getString(c.getColumnIndexOrThrow("name")),
                        c.getString(c.getColumnIndexOrThrow("phone")),
                        c.getDouble(c.getColumnIndexOrThrow("balance"))
                ));
            } while (c.moveToNext());
            c.close();
        }
        return list;
    }

    public boolean recordCustomerPayment(long customerId, double amount, String notes) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            // Get current balance
            Cursor cursor = db.rawQuery("SELECT balance FROM " + TABLE_CUSTOMERS + " WHERE id = ?", new String[]{String.valueOf(customerId)});
            if (cursor == null || !cursor.moveToFirst()) {
                if (cursor != null) cursor.close();
                return false;
            }
            double curBalance = cursor.getDouble(0);
            cursor.close();

            double newBalance = curBalance - amount;
            if (newBalance < 0) {
                newBalance = 0; // prevent negative unless advance
            }

            ContentValues cvCust = new ContentValues();
            cvCust.put("balance", newBalance);
            db.update(TABLE_CUSTOMERS, cvCust, "id = ?", new String[]{String.valueOf(customerId)});

            // Insert Ledger Entry
            String now = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());
            ContentValues cvLedger = new ContentValues();
            cvLedger.put("customer_id", customerId);
            cvLedger.put("type", "PAYMENT");
            cvLedger.put("amount", amount);
            cvLedger.put("notes", notes);
            cvLedger.put("date", now);
            db.insert(TABLE_LEDGER, null, cvLedger);

            db.setTransactionSuccessful();
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            db.endTransaction();
        }
    }

    // --- Fast Sales Execution with Atomic Transaction ---
    public static class CartItem {
        public Product product;
        public int quantity;

        public CartItem(Product product, int quantity) {
            this.product = product;
            this.quantity = quantity;
        }

        public double getLineTotal() {
            return product.getSellPrice() * quantity;
        }

        public double getLineCost() {
            return product.getCostPrice() * quantity;
        }
    }

    public boolean executeSale(long customerId, String customerName, List<CartItem> cartItems, double discount, String paymentType) {
        if (cartItems == null || cartItems.isEmpty()) return false;
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            double totalAmount = 0.0;
            double totalCost = 0.0;
            for (CartItem item : cartItems) {
                totalAmount += item.getLineTotal();
                totalCost += item.getLineCost();
            }

            double finalAmount = totalAmount - discount;
            if (finalAmount < 0) finalAmount = 0.0;

            String now = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());

            // 1. Insert into Sales
            ContentValues cvSale = new ContentValues();
            cvSale.put("customer_id", customerId);
            cvSale.put("customer_name", (customerName == null || customerName.trim().isEmpty()) ? "Walk-in Cash" : customerName);
            cvSale.put("total_amount", totalAmount);
            cvSale.put("total_cost", totalCost);
            cvSale.put("discount", discount);
            cvSale.put("final_amount", finalAmount);
            cvSale.put("payment_type", paymentType);
            cvSale.put("created_at", now);
            long saleId = db.insert(TABLE_SALES, null, cvSale);
            if (saleId == -1) return false;

            // 2. Insert items and decrement stock
            for (CartItem item : cartItems) {
                ContentValues cvItem = new ContentValues();
                cvItem.put("sale_id", saleId);
                cvItem.put("product_id", item.product.getId());
                cvItem.put("product_name", item.product.getName());
                cvItem.put("quantity", item.quantity);
                cvItem.put("cost_price", item.product.getCostPrice());
                cvItem.put("sell_price", item.product.getSellPrice());
                cvItem.put("subtotal", item.getLineTotal());
                db.insert(TABLE_SALE_ITEMS, null, cvItem);

                // Reduce inventory stock
                db.execSQL("UPDATE " + TABLE_PRODUCTS + " SET quantity = quantity - " + item.quantity + " WHERE id = " + item.product.getId());
            }

            // 3. If credit sale, update customer balance and ledger
            if ("CREDIT".equalsIgnoreCase(paymentType) && customerId > 0) {
                db.execSQL("UPDATE " + TABLE_CUSTOMERS + " SET balance = balance + " + finalAmount + " WHERE id = " + customerId);

                ContentValues cvLedger = new ContentValues();
                cvLedger.put("customer_id", customerId);
                cvLedger.put("type", "CREDIT_SALE");
                cvLedger.put("amount", finalAmount);
                cvLedger.put("notes", "Bill #" + saleId);
                cvLedger.put("date", now);
                db.insert(TABLE_LEDGER, null, cvLedger);
            }

            db.setTransactionSuccessful();
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            db.endTransaction();
        }
    }

    // --- Expense Operations ---
    public long insertExpense(String category, double amount, String notes) {
        SQLiteDatabase db = this.getWritableDatabase();
        String dateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        ContentValues cv = new ContentValues();
        cv.put("category", category);
        cv.put("amount", amount);
        cv.put("notes", notes);
        cv.put("date", dateStr);
        return db.insert(TABLE_EXPENSES, null, cv);
    }

    public List<Expense> getAllExpenses() {
        List<Expense> list = new ArrayList<Expense>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + TABLE_EXPENSES + " ORDER BY id DESC", null);
        if (c != null && c.moveToFirst()) {
            do {
                list.add(new Expense(
                        c.getLong(c.getColumnIndexOrThrow("id")),
                        c.getString(c.getColumnIndexOrThrow("category")),
                        c.getDouble(c.getColumnIndexOrThrow("amount")),
                        c.getString(c.getColumnIndexOrThrow("notes")),
                        c.getString(c.getColumnIndexOrThrow("date"))
                ));
            } while (c.moveToNext());
            c.close();
        }
        return list;
    }

    public List<Sale> getAllSales() {
        List<Sale> list = new ArrayList<Sale>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + TABLE_SALES + " ORDER BY id DESC", null);
        if (c != null && c.moveToFirst()) {
            do {
                list.add(new Sale(
                        c.getLong(c.getColumnIndexOrThrow("id")),
                        c.getLong(c.getColumnIndexOrThrow("customer_id")),
                        c.getString(c.getColumnIndexOrThrow("customer_name")),
                        c.getDouble(c.getColumnIndexOrThrow("total_amount")),
                        c.getDouble(c.getColumnIndexOrThrow("total_cost")),
                        c.getDouble(c.getColumnIndexOrThrow("discount")),
                        c.getDouble(c.getColumnIndexOrThrow("final_amount")),
                        c.getString(c.getColumnIndexOrThrow("payment_type")),
                        c.getString(c.getColumnIndexOrThrow("created_at"))
                ));
            } while (c.moveToNext());
            c.close();
        }
        return list;
    }

    public double getTotalUdhaarOutstanding() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT SUM(balance) FROM " + TABLE_CUSTOMERS, null);
        double total = 0.0;
        if (c != null && c.moveToFirst()) {
            total = c.getDouble(0);
            c.close();
        }
        return total;
    }

    public List<String> getCustomerLedgerHistory(long customerId) {
        List<String> entries = new ArrayList<String>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT type, amount, notes, date FROM " + TABLE_LEDGER + " WHERE customer_id = ? ORDER BY id DESC", new String[]{String.valueOf(customerId)});
        if (c != null && c.moveToFirst()) {
            do {
                String type = c.getString(0);
                double amount = c.getDouble(1);
                String notes = c.getString(2);
                String date = c.getString(3);
                String line = date + " | " + (type.equals("CREDIT_SALE") ? "+ Udhaar: ₹" : "- Received: ₹") + String.format("%.2f", amount) + " (" + notes + ")";
                entries.add(line);
            } while (c.moveToNext());
            c.close();
        }
        return entries;
    }
}