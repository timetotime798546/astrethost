package com.alfariskirana.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "alfaris_kirana.db";
    private static final int DATABASE_VERSION = 1;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE products (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, " +
                "category TEXT, " +
                "purchase_price REAL DEFAULT 0, " +
                "selling_price REAL NOT NULL, " +
                "stock REAL DEFAULT 0, " +
                "unit TEXT DEFAULT 'kg')");

        db.execSQL("CREATE TABLE customers (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, " +
                "mobile TEXT, " +
                "pending_balance REAL DEFAULT 0)");

        db.execSQL("CREATE TABLE sales (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "customer_id INTEGER, " +
                "customer_name TEXT, " +
                "total_amount REAL, " +
                "discount REAL, " +
                "final_amount REAL, " +
                "payment_mode TEXT, " +
                "date_time TEXT)");

        db.execSQL("CREATE TABLE sale_items (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "sale_id INTEGER, " +
                "product_id INTEGER, " +
                "product_name TEXT, " +
                "quantity REAL, " +
                "unit_price REAL, " +
                "total_price REAL, " +
                "unit TEXT)");

        db.execSQL("CREATE TABLE udhaar_transactions (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "customer_id INTEGER, " +
                "type TEXT, " + // CREDIT or PAYMENT
                "amount REAL, " +
                "date_time TEXT, " +
                "notes TEXT)");

        db.execSQL("CREATE TABLE purchases (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "supplier_name TEXT, " +
                "product_id INTEGER, " +
                "quantity REAL, " +
                "purchase_amount REAL, " +
                "purchase_date TEXT)");

        db.execSQL("CREATE TABLE expenses (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "category TEXT, " +
                "amount REAL, " +
                "notes TEXT, " +
                "expense_date TEXT)");

        // Insert initial standard kirana grocery products
        insertSampleProducts(db);
    }

    private void insertSampleProducts(SQLiteDatabase db) {
        insertProductDirect(db, "Basmati Rice (Daawat)", "Grains & Pulses", 75.0, 95.0, 50.0, "kg");
        insertProductDirect(db, "Chana Dal (Organic)", "Grains & Pulses", 65.0, 80.0, 40.0, "kg");
        insertProductDirect(db, "Toor Dal (Premium)", "Grains & Pulses", 110.0, 135.0, 35.0, "kg");
        insertProductDirect(db, "Sunflower Cooking Oil (1L)", "Oil & Ghee", 125.0, 145.0, 24.0, "pkt");
        insertProductDirect(db, "Tata Salt (1kg)", "Spices & Essentials", 22.0, 28.0, 60.0, "pkt");
        insertProductDirect(db, "Refined Sugar", "Essentials", 38.0, 44.0, 100.0, "kg");
        insertProductDirect(db, "Red Chilli Powder (Everest)", "Spices", 40.0, 52.0, 30.0, "pkt");
        insertProductDirect(db, "Haldi / Turmeric Powder", "Spices", 32.0, 45.0, 25.0, "pkt");
        insertProductDirect(db, "Aashirvaad Atta (5kg)", "Flour", 210.0, 245.0, 20.0, "pkt");
        insertProductDirect(db, "Amul Butter (100g)", "Dairy", 50.0, 56.0, 15.0, "pcs");
    }

    private void insertProductDirect(SQLiteDatabase db, String name, String category, double pPrice, double sPrice, double stock, String unit) {
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("category", category);
        cv.put("purchase_price", pPrice);
        cv.put("selling_price", sPrice);
        cv.put("stock", stock);
        cv.put("unit", unit);
        db.insert("products", null, cv);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS products");
        db.execSQL("DROP TABLE IF EXISTS customers");
        db.execSQL("DROP TABLE IF EXISTS sales");
        db.execSQL("DROP TABLE IF EXISTS sale_items");
        db.execSQL("DROP TABLE IF EXISTS udhaar_transactions");
        db.execSQL("DROP TABLE IF EXISTS purchases");
        db.execSQL("DROP TABLE IF EXISTS expenses");
        onCreate(db);
    }

    public static String getCurrentDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }

    public static String getTodayDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date());
    }

    // --- PRODUCTS ---
    public List<Product> getAllProducts(String search) {
        List<Product> list = new ArrayList<Product>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c;
        if (search == null || search.trim().isEmpty()) {
            c = db.rawQuery("SELECT * FROM products ORDER BY name ASC", null);
        } else {
            c = db.rawQuery("SELECT * FROM products WHERE name LIKE ? OR category LIKE ? ORDER BY name ASC",
                    new String[]{"%" + search + "%", "%" + search + "%"});
        }

        if (c.moveToFirst()) {
            do {
                Product p = new Product(
                        c.getLong(c.getColumnIndexOrThrow("id")),
                        c.getString(c.getColumnIndexOrThrow("name")),
                        c.getString(c.getColumnIndexOrThrow("category")),
                        c.getDouble(c.getColumnIndexOrThrow("purchase_price")),
                        c.getDouble(c.getColumnIndexOrThrow("selling_price")),
                        c.getDouble(c.getColumnIndexOrThrow("stock")),
                        c.getString(c.getColumnIndexOrThrow("unit"))
                );
                list.add(p);
            } while (c.moveToNext());
        }
        c.close();
        return list;
    }

    public Product getProductById(long id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM products WHERE id = ?", new String[]{String.valueOf(id)});
        Product p = null;
        if (c.moveToFirst()) {
            p = new Product(
                    c.getLong(c.getColumnIndexOrThrow("id")),
                    c.getString(c.getColumnIndexOrThrow("name")),
                    c.getString(c.getColumnIndexOrThrow("category")),
                    c.getDouble(c.getColumnIndexOrThrow("purchase_price")),
                    c.getDouble(c.getColumnIndexOrThrow("selling_price")),
                    c.getDouble(c.getColumnIndexOrThrow("stock")),
                    c.getString(c.getColumnIndexOrThrow("unit"))
            );
        }
        c.close();
        return p;
    }

    public long saveProduct(Product p) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", p.getName());
        cv.put("category", p.getCategory());
        cv.put("purchase_price", p.getPurchasePrice());
        cv.put("selling_price", p.getSellingPrice());
        cv.put("stock", p.getStock());
        cv.put("unit", p.getUnit());

        if (p.getId() > 0) {
            db.update("products", cv, "id = ?", new String[]{String.valueOf(p.getId())});
            return p.getId();
        } else {
            return db.insert("products", null, cv);
        }
    }

    public void deleteProduct(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("products", "id = ?", new String[]{String.valueOf(id)});
    }

    // --- CUSTOMERS & UDHAAR ---
    public List<Customer> getAllCustomers(String search) {
        List<Customer> list = new ArrayList<Customer>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c;
        if (search == null || search.trim().isEmpty()) {
            c = db.rawQuery("SELECT * FROM customers ORDER BY name ASC", null);
        } else {
            c = db.rawQuery("SELECT * FROM customers WHERE name LIKE ? OR mobile LIKE ? ORDER BY name ASC",
                    new String[]{"%" + search + "%", "%" + search + "%"});
        }

        if (c.moveToFirst()) {
            do {
                Customer cust = new Customer(
                        c.getLong(c.getColumnIndexOrThrow("id")),
                        c.getString(c.getColumnIndexOrThrow("name")),
                        c.getString(c.getColumnIndexOrThrow("mobile")),
                        c.getDouble(c.getColumnIndexOrThrow("pending_balance"))
                );
                list.add(cust);
            } while (c.moveToNext());
        }
        c.close();
        return list;
    }

    public Customer getCustomerById(long id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM customers WHERE id = ?", new String[]{String.valueOf(id)});
        Customer cust = null;
        if (c.moveToFirst()) {
            cust = new Customer(
                    c.getLong(c.getColumnIndexOrThrow("id")),
                    c.getString(c.getColumnIndexOrThrow("name")),
                    c.getString(c.getColumnIndexOrThrow("mobile")),
                    c.getDouble(c.getColumnIndexOrThrow("pending_balance"))
            );
        }
        c.close();
        return cust;
    }

    public long saveCustomer(Customer c) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", c.getName());
        cv.put("mobile", c.getMobile());
        cv.put("pending_balance", c.getPendingBalance());

        if (c.getId() > 0) {
            db.update("customers", cv, "id = ?", new String[]{String.valueOf(c.getId())});
            return c.getId();
        } else {
            return db.insert("customers", null, cv);
        }
    }

    public void recordCustomerPayment(long customerId, double paymentAmount, String notes) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues cvTx = new ContentValues();
            cvTx.put("customer_id", customerId);
            cvTx.put("type", "PAYMENT");
            cvTx.put("amount", paymentAmount);
            cvTx.put("date_time", getCurrentDateTime());
            cvTx.put("notes", notes == null ? "Cash Payment Received" : notes);
            db.insert("udhaar_transactions", null, cvTx);

            db.execSQL("UPDATE customers SET pending_balance = pending_balance - ? WHERE id = ?",
                    new Object[]{paymentAmount, customerId});

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public Cursor getCustomerLedger(long customerId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT id as _id, type, amount, date_time, notes FROM udhaar_transactions WHERE customer_id = ? ORDER BY id DESC",
                new String[]{String.valueOf(customerId)});
    }

    // --- SALES BILLING ---
    public long processSale(long customerId, String customerName, double total, double discount, double finalTotal, String paymentMode, List<SaleItem> items) {
        SQLiteDatabase db = this.getWritableDatabase();
        long saleId = -1;
        db.beginTransaction();
        try {
            ContentValues cv = new ContentValues();
            cv.put("customer_id", customerId);
            cv.put("customer_name", customerName);
            cv.put("total_amount", total);
            cv.put("discount", discount);
            cv.put("final_amount", finalTotal);
            cv.put("payment_mode", paymentMode);
            cv.put("date_time", getCurrentDateTime());

            saleId = db.insert("sales", null, cv);

            for (int i = 0; i < items.size(); i++) {
                SaleItem item = items.get(i);
                ContentValues itemCv = new ContentValues();
                itemCv.put("sale_id", saleId);
                itemCv.put("product_id", item.getProductId());
                itemCv.put("product_name", item.getProductName());
                itemCv.put("quantity", item.getQuantity());
                itemCv.put("unit_price", item.getUnitPrice());
                itemCv.put("total_price", item.getTotalPrice());
                itemCv.put("unit", item.getUnit());
                db.insert("sale_items", null, itemCv);

                // Deduct Product Stock
                db.execSQL("UPDATE products SET stock = MAX(0, stock - ?) WHERE id = ?",
                        new Object[]{item.getQuantity(), item.getProductId()});
            }

            // If Udhaar payment mode, add to customer pending balance and log credit transaction
            if ("Udhaar".equalsIgnoreCase(paymentMode) && customerId > 0) {
                db.execSQL("UPDATE customers SET pending_balance = pending_balance + ? WHERE id = ?",
                        new Object[]{finalTotal, customerId});

                ContentValues udhaarCv = new ContentValues();
                udhaarCv.put("customer_id", customerId);
                udhaarCv.put("type", "CREDIT");
                udhaarCv.put("amount", finalTotal);
                udhaarCv.put("date_time", getCurrentDateTime());
                udhaarCv.put("notes", "Kirana Bill #" + saleId);
                db.insert("udhaar_transactions", null, udhaarCv);
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return saleId;
    }

    // --- PURCHASES (STOCK INWARD) ---
    public long recordPurchase(String supplier, long productId, double qty, double amount) {
        SQLiteDatabase db = this.getWritableDatabase();
        long purchaseId = -1;
        db.beginTransaction();
        try {
            ContentValues cv = new ContentValues();
            cv.put("supplier_name", supplier);
            cv.put("product_id", productId);
            cv.put("quantity", qty);
            cv.put("purchase_amount", amount);
            cv.put("purchase_date", getCurrentDateTime());
            purchaseId = db.insert("purchases", null, cv);

            // Increase product stock
            db.execSQL("UPDATE products SET stock = stock + ? WHERE id = ?", new Object[]{qty, productId});

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return purchaseId;
    }

    public List<Purchase> getAllPurchases() {
        List<Purchase> list = new ArrayList<Purchase>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT pu.id, pu.supplier_name, pu.product_id, p.name as prod_name, pu.quantity, pu.purchase_amount, pu.purchase_date " +
                "FROM purchases pu LEFT JOIN products p ON pu.product_id = p.id ORDER BY pu.id DESC";
        Cursor c = db.rawQuery(query, null);
        if (c.moveToFirst()) {
            do {
                Purchase p = new Purchase(
                        c.getLong(0),
                        c.getString(1),
                        c.getLong(2),
                        c.getString(3) != null ? c.getString(3) : "Generic Item",
                        c.getDouble(4),
                        c.getDouble(5),
                        c.getString(6)
                );
                list.add(p);
            } while (c.moveToNext());
        }
        c.close();
        return list;
    }

    // --- EXPENSES ---
    public long addExpense(String category, double amount, String notes) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("category", category);
        cv.put("amount", amount);
        cv.put("notes", notes);
        cv.put("expense_date", getCurrentDateTime());
        return db.insert("expenses", null, cv);
    }

    public List<Expense> getAllExpenses() {
        List<Expense> list = new ArrayList<Expense>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT id, category, amount, notes, expense_date FROM expenses ORDER BY id DESC", null);
        if (c.moveToFirst()) {
            do {
                Expense e = new Expense(
                        c.getLong(0),
                        c.getString(1),
                        c.getDouble(2),
                        c.getString(3),
                        c.getString(4)
                );
                list.add(e);
            } while (c.moveToNext());
        }
        c.close();
        return list;
    }

    // --- DASHBOARD & REPORTS METRICS ---
    public double getTodaySalesTotal() {
        SQLiteDatabase db = this.getReadableDatabase();
        String today = getTodayDate();
        Cursor c = db.rawQuery("SELECT SUM(final_amount) FROM sales WHERE date_time LIKE ?", new String[]{today + "%"});
        double total = 0.0;
        if (c.moveToFirst()) {
            total = c.getDouble(0);
        }
        c.close();
        return total;
    }

    public double getTodayExpenseTotal() {
        SQLiteDatabase db = this.getReadableDatabase();
        String today = getTodayDate();
        Cursor c = db.rawQuery("SELECT SUM(amount) FROM expenses WHERE expense_date LIKE ?", new String[]{today + "%"});
        double total = 0.0;
        if (c.moveToFirst()) {
            total = c.getDouble(0);
        }
        c.close();
        return total;
    }

    public double getTotalUdhaarBalance() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT SUM(pending_balance) FROM customers WHERE pending_balance > 0", null);
        double total = 0.0;
        if (c.moveToFirst()) {
            total = c.getDouble(0);
        }
        c.close();
        return total;
    }

    public int getProductCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM products", null);
        int count = 0;
        if (c.moveToFirst()) {
            count = c.getInt(0);
        }
        c.close();
        return count;
    }

    public int getLowStockCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM products WHERE stock <= 5", null);
        int count = 0;
        if (c.moveToFirst()) {
            count = c.getInt(0);
        }
        c.close();
        return count;
    }

    public double getTotalSalesAllTime() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT SUM(final_amount) FROM sales", null);
        double total = 0.0;
        if (c.moveToFirst()) {
            total = c.getDouble(0);
        }
        c.close();
        return total;
    }

    public double getTotalPurchasesAllTime() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT SUM(purchase_amount) FROM purchases", null);
        double total = 0.0;
        if (c.moveToFirst()) {
            total = c.getDouble(0);
        }
        c.close();
        return total;
    }

    public double getTotalExpensesAllTime() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT SUM(amount) FROM expenses", null);
        double total = 0.0;
        if (c.moveToFirst()) {
            total = c.getDouble(0);
        }
        c.close();
        return total;
    }
}