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
    private static final int DATABASE_VERSION = 5;

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
                "unit TEXT DEFAULT 'किलो (KG)')");

        db.execSQL("CREATE TABLE customers (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, " +
                "mobile TEXT, " +
                "pending_balance REAL DEFAULT 0, " +
                "is_active INTEGER DEFAULT 1)");

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
                "type TEXT, " +
                "amount REAL, " +
                "pending_amount REAL DEFAULT 0, " +
                "date_time TEXT, " +
                "notes TEXT, " +
                "payment_method TEXT DEFAULT 'Cash', " +
                "parent_udhaar_id INTEGER DEFAULT 0, " +
                "sale_id INTEGER DEFAULT 0)");

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

        insertSampleProducts(db);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        try {
            db.execSQL("ALTER TABLE udhaar_transactions ADD COLUMN payment_method TEXT DEFAULT 'Cash'");
        } catch (Exception e) {}
        try {
            db.execSQL("ALTER TABLE udhaar_transactions ADD COLUMN pending_amount REAL DEFAULT 0");
        } catch (Exception e) {}
        try {
            db.execSQL("ALTER TABLE udhaar_transactions ADD COLUMN parent_udhaar_id INTEGER DEFAULT 0");
        } catch (Exception e) {}
        try {
            db.execSQL("ALTER TABLE udhaar_transactions ADD COLUMN sale_id INTEGER DEFAULT 0");
        } catch (Exception e) {}
        try {
            db.execSQL("ALTER TABLE customers ADD COLUMN is_active INTEGER DEFAULT 1");
        } catch (Exception e) {}
    }

    private void insertSampleProducts(SQLiteDatabase db) {
        insertProductDirect(db, "Basmati Rice (Daawat)", "Grains & Pulses", 75.0, 95.0, 50.0, "किलो (KG)");
        insertProductDirect(db, "Chana Dal (Organic)", "Grains & Pulses", 65.0, 80.0, 40.0, "किलो (KG)");
        insertProductDirect(db, "Toor Dal (Premium)", "Grains & Pulses", 110.0, 135.0, 35.0, "किलो (KG)");
        insertProductDirect(db, "Sunflower Cooking Oil (1L)", "Oil & Ghee", 125.0, 145.0, 24.0, "पीस (Piece)");
        insertProductDirect(db, "Tata Salt (1kg)", "Spices & Essentials", 22.0, 28.0, 60.0, "पीस (Piece)");
        insertProductDirect(db, "Refined Sugar", "Essentials", 38.0, 44.0, 100.0, "किलो (KG)");
        insertProductDirect(db, "Red Chilli Powder (Everest)", "Spices", 40.0, 52.0, 30.0, "पीस (Piece)");
        insertProductDirect(db, "Haldi / Turmeric Powder", "Spices", 32.0, 45.0, 25.0, "पीस (Piece)");
        insertProductDirect(db, "Aashirvaad Atta (5kg)", "Flour", 210.0, 245.0, 20.0, "पीस (Piece)");
        insertProductDirect(db, "Amul Butter (100g)", "Dairy", 50.0, 56.0, 15.0, "पीस (Piece)");
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
        try {
            db.execSQL("ALTER TABLE udhaar_transactions ADD COLUMN payment_method TEXT DEFAULT 'Cash'");
        } catch (Exception e) {}
        try {
            db.execSQL("ALTER TABLE udhaar_transactions ADD COLUMN pending_amount REAL DEFAULT 0");
        } catch (Exception e) {}
        try {
            db.execSQL("ALTER TABLE udhaar_transactions ADD COLUMN parent_udhaar_id INTEGER DEFAULT 0");
        } catch (Exception e) {}
        try {
            db.execSQL("ALTER TABLE udhaar_transactions ADD COLUMN sale_id INTEGER DEFAULT 0");
        } catch (Exception e) {}
        try {
            db.execSQL("ALTER TABLE customers ADD COLUMN is_active INTEGER DEFAULT 1");
        } catch (Exception e) {}
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

    // --- CUSTOMERS & ACCURATE UDHAAR/JAMA ACCOUNTING ---
    public List<Customer> getAllCustomers(String search) {
        List<Customer> list = new ArrayList<Customer>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c;
        if (search == null || search.trim().isEmpty()) {
            c = db.rawQuery("SELECT id, name, mobile FROM customers WHERE (is_active IS NULL OR is_active = 1) ORDER BY name ASC", null);
        } else {
            c = db.rawQuery("SELECT id, name, mobile FROM customers WHERE (is_active IS NULL OR is_active = 1) AND (name LIKE ? OR mobile LIKE ?) ORDER BY name ASC",
                    new String[]{"%" + search + "%", "%" + search + "%"});
        }

        if (c.moveToFirst()) {
            do {
                long id = c.getLong(0);
                String name = c.getString(1);
                String mobile = c.getString(2);

                Customer cust = calculateCustomerSummary(db, id, name, mobile);
                list.add(cust);
            } while (c.moveToNext());
        }
        c.close();
        return list;
    }

    public Customer getCustomerById(long id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT id, name, mobile FROM customers WHERE id = ?", new String[]{String.valueOf(id)});
        Customer cust = null;
        if (c.moveToFirst()) {
            String name = c.getString(1);
            String mobile = c.getString(2);
            cust = calculateCustomerSummary(db, id, name, mobile);
        }
        c.close();
        return cust;
    }

    public boolean softDeleteCustomer(long customerId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("is_active", 0);
        int rows = db.update("customers", cv, "id = ?", new String[]{String.valueOf(customerId)});
        return rows > 0;
    }

    public Customer calculateCustomerSummary(SQLiteDatabase db, long id, String name, String mobile) {
        double totalUdhaar = 0.0;
        Cursor cU = db.rawQuery("SELECT SUM(amount) FROM udhaar_transactions WHERE customer_id = ? AND type = 'CREDIT'", new String[]{String.valueOf(id)});
        if (cU.moveToFirst()) {
            totalUdhaar = cU.getDouble(0);
        }
        cU.close();

        double totalJama = 0.0;
        Cursor cJ = db.rawQuery("SELECT SUM(amount) FROM udhaar_transactions WHERE customer_id = ? AND type = 'PAYMENT'", new String[]{String.valueOf(id)});
        if (cJ.moveToFirst()) {
            totalJama = cJ.getDouble(0);
        }
        cJ.close();

        double pendingBalance = 0.0;
        Cursor cP = db.rawQuery("SELECT SUM(pending_amount) FROM udhaar_transactions WHERE customer_id = ? AND type = 'CREDIT'", new String[]{String.valueOf(id)});
        if (cP.moveToFirst()) {
            pendingBalance = cP.getDouble(0);
        }
        cP.close();

        double advanceBalance = Math.max(0.0, totalJama - totalUdhaar);

        ContentValues cv = new ContentValues();
        cv.put("pending_balance", pendingBalance);
        db.update("customers", cv, "id = ?", new String[]{String.valueOf(id)});

        return new Customer(id, name, mobile, totalUdhaar, totalJama, pendingBalance, advanceBalance);
    }

    public long saveCustomer(Customer c) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", c.getName());
        cv.put("mobile", c.getMobile());
        cv.put("pending_balance", c.getPendingBalance());
        cv.put("is_active", 1);

        long custId = -1;
        if (c.getId() > 0) {
            db.update("customers", cv, "id = ?", new String[]{String.valueOf(c.getId())});
            custId = c.getId();
        } else {
            custId = db.insert("customers", null, cv);
            if (c.getPendingBalance() > 0) {
                ContentValues cvTx = new ContentValues();
                cvTx.put("customer_id", custId);
                cvTx.put("type", "CREDIT");
                cvTx.put("amount", c.getPendingBalance());
                cvTx.put("pending_amount", c.getPendingBalance());
                cvTx.put("date_time", getCurrentDateTime());
                cvTx.put("notes", "Opening Udhaar Balance");
                cvTx.put("payment_method", "Udhaar");
                cvTx.put("parent_udhaar_id", 0);
                cvTx.put("sale_id", 0);
                db.insert("udhaar_transactions", null, cvTx);
            }
        }
        return custId;
    }

    public long addUdhaarTransaction(long customerId, double amount, String notes, String dateTime) {
        if (amount <= 0) return -1;
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        long txId = -1;
        try {
            ContentValues cv = new ContentValues();
            cv.put("customer_id", customerId);
            cv.put("type", "CREDIT");
            cv.put("amount", amount);
            cv.put("pending_amount", amount);
            cv.put("date_time", (dateTime != null && !dateTime.trim().isEmpty()) ? dateTime : getCurrentDateTime());
            cv.put("notes", (notes != null && !notes.trim().isEmpty()) ? notes : "नया उधार");
            cv.put("payment_method", "Udhaar");
            cv.put("parent_udhaar_id", 0);
            cv.put("sale_id", 0);
            txId = db.insert("udhaar_transactions", null, cv);

            calculateCustomerSummary(db, customerId, "", "");

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return txId;
    }

    public boolean addJamaForUdhaar(long udhaarId, long customerId, double amount, String paymentMethod, String notes, String dateTime) {
        if (amount <= 0) return false;
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            Cursor c = db.rawQuery("SELECT pending_amount FROM udhaar_transactions WHERE id = ? AND type = 'CREDIT'", new String[]{String.valueOf(udhaarId)});
            if (!c.moveToFirst()) {
                c.close();
                return false;
            }
            double currentPending = c.getDouble(0);
            c.close();

            if (amount > currentPending + 0.001) {
                return false;
            }

            double newPending = Math.max(0.0, currentPending - amount);

            ContentValues cvUpdate = new ContentValues();
            cvUpdate.put("pending_amount", newPending);
            db.update("udhaar_transactions", cvUpdate, "id = ?", new String[]{String.valueOf(udhaarId)});

            ContentValues cvPayment = new ContentValues();
            cvPayment.put("customer_id", customerId);
            cvPayment.put("type", "PAYMENT");
            cvPayment.put("amount", amount);
            cvPayment.put("pending_amount", 0);
            cvPayment.put("date_time", (dateTime != null && !dateTime.trim().isEmpty()) ? dateTime : getCurrentDateTime());
            cvPayment.put("notes", (notes != null && !notes.trim().isEmpty()) ? notes : "पैसा जमा");
            cvPayment.put("payment_method", (paymentMethod != null && !paymentMethod.trim().isEmpty()) ? paymentMethod : "Cash");
            cvPayment.put("parent_udhaar_id", udhaarId);
            cvPayment.put("sale_id", 0);
            db.insert("udhaar_transactions", null, cvPayment);

            calculateCustomerSummary(db, customerId, "", "");

            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
        }
    }

    public boolean addGeneralJama(long customerId, double amount, String paymentMethod, String notes, String dateTime) {
        if (amount <= 0) return false;
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            Cursor c = db.rawQuery("SELECT id, pending_amount FROM udhaar_transactions WHERE customer_id = ? AND type = 'CREDIT' AND pending_amount > 0 ORDER BY id ASC", new String[]{String.valueOf(customerId)});
            double remainingToPay = amount;

            while (c.moveToNext() && remainingToPay > 0) {
                long uId = c.getLong(0);
                double pending = c.getDouble(1);
                double payForThis = Math.min(pending, remainingToPay);

                ContentValues cvUpdate = new ContentValues();
                cvUpdate.put("pending_amount", pending - payForThis);
                db.update("udhaar_transactions", cvUpdate, "id = ?", new String[]{String.valueOf(uId)});

                remainingToPay -= payForThis;
            }
            c.close();

            ContentValues cvPayment = new ContentValues();
            cvPayment.put("customer_id", customerId);
            cvPayment.put("type", "PAYMENT");
            cvPayment.put("amount", amount);
            cvPayment.put("pending_amount", 0);
            cvPayment.put("date_time", (dateTime != null && !dateTime.trim().isEmpty()) ? dateTime : getCurrentDateTime());
            cvPayment.put("notes", (notes != null && !notes.trim().isEmpty()) ? notes : "पैसा जमा");
            cvPayment.put("payment_method", (paymentMethod != null && !paymentMethod.trim().isEmpty()) ? paymentMethod : "Cash");
            cvPayment.put("parent_udhaar_id", 0);
            cvPayment.put("sale_id", 0);
            db.insert("udhaar_transactions", null, cvPayment);

            calculateCustomerSummary(db, customerId, "", "");

            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
        }
    }

    public List<UdhaarTransaction> getUdhaarTransactions(long customerId) {
        List<UdhaarTransaction> list = new ArrayList<UdhaarTransaction>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT id, amount, pending_amount, date_time, notes FROM udhaar_transactions WHERE customer_id = ? AND type = 'CREDIT' ORDER BY id DESC",
                new String[]{String.valueOf(customerId)});

        if (c.moveToFirst()) {
            do {
                UdhaarTransaction tx = new UdhaarTransaction(
                        c.getLong(0),
                        customerId,
                        "CREDIT",
                        c.getDouble(1),
                        c.getDouble(2),
                        c.getString(3),
                        c.getString(4),
                        "Udhaar",
                        0
                );
                list.add(tx);
            } while (c.moveToNext());
        }
        c.close();
        return list;
    }

    public List<UdhaarTransaction> getJamaTransactions(long customerId) {
        List<UdhaarTransaction> list = new ArrayList<UdhaarTransaction>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT id, amount, date_time, notes, payment_method, parent_udhaar_id FROM udhaar_transactions WHERE customer_id = ? AND type = 'PAYMENT' ORDER BY id DESC",
                new String[]{String.valueOf(customerId)});

        if (c.moveToFirst()) {
            do {
                UdhaarTransaction tx = new UdhaarTransaction(
                        c.getLong(0),
                        customerId,
                        "PAYMENT",
                        c.getDouble(1),
                        0.0,
                        c.getString(2),
                        c.getString(3),
                        c.getString(4) != null ? c.getString(4) : "Cash",
                        c.getLong(5)
                );
                list.add(tx);
            } while (c.moveToNext());
        }
        c.close();
        return list;
    }

    public List<SaleRecord> getCustomerSales(long customerId) {
        List<SaleRecord> list = new ArrayList<SaleRecord>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT id, customer_id, customer_name, total_amount, discount, final_amount, payment_mode, date_time FROM sales WHERE customer_id = ? ORDER BY id DESC",
                new String[]{String.valueOf(customerId)});

        if (c.moveToFirst()) {
            do {
                SaleRecord s = new SaleRecord(
                        c.getLong(0),
                        c.getLong(1),
                        c.getString(2),
                        c.getDouble(3),
                        c.getDouble(4),
                        c.getDouble(5),
                        c.getString(6),
                        c.getString(7)
                );
                list.add(s);
            } while (c.moveToNext());
        }
        c.close();
        return list;
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

                db.execSQL("UPDATE products SET stock = MAX(0, stock - ?) WHERE id = ?",
                        new Object[]{item.getQuantity(), item.getProductId()});
            }

            if ("Udhaar".equalsIgnoreCase(paymentMode) || "Credit".equalsIgnoreCase(paymentMode)) {
                if (customerId > 0) {
                    Cursor checkC = db.rawQuery("SELECT id FROM udhaar_transactions WHERE sale_id = ? AND sale_id > 0",
                            new String[]{String.valueOf(saleId)});
                    if (!checkC.moveToFirst()) {
                        ContentValues udhaarCv = new ContentValues();
                        udhaarCv.put("customer_id", customerId);
                        udhaarCv.put("type", "CREDIT");
                        udhaarCv.put("amount", finalTotal);
                        udhaarCv.put("pending_amount", finalTotal);
                        udhaarCv.put("date_time", getCurrentDateTime());
                        udhaarCv.put("notes", "Kirana Bill #" + saleId);
                        udhaarCv.put("payment_method", "Udhaar");
                        udhaarCv.put("parent_udhaar_id", 0);
                        udhaarCv.put("sale_id", saleId);
                        db.insert("udhaar_transactions", null, udhaarCv);
                    }
                    checkC.close();

                    calculateCustomerSummary(db, customerId, "", "");
                }
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return saleId;
    }

    // --- PURCHASES & EXPENSES ---
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

    // --- DASHBOARD & REPORT METRICS ---
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

    public double getCashSalesTotal() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT SUM(final_amount) FROM sales WHERE payment_mode NOT IN ('Udhaar', 'Credit')", null);
        double total = 0.0;
        if (c.moveToFirst()) {
            total = c.getDouble(0);
        }
        c.close();
        return total;
    }

    public double getCreditSalesTotal() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT SUM(final_amount) FROM sales WHERE payment_mode IN ('Udhaar', 'Credit')", null);
        double total = 0.0;
        if (c.moveToFirst()) {
            total = c.getDouble(0);
        }
        c.close();
        return total;
    }

    public double getTotalJamaReceivedAllTime() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT SUM(amount) FROM udhaar_transactions WHERE type = 'PAYMENT'", null);
        double total = 0.0;
        if (c.moveToFirst()) {
            total = c.getDouble(0);
        }
        c.close();
        return total;
    }

    public double getTotalUdhaarBalance() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT SUM(pending_amount) FROM udhaar_transactions WHERE type = 'CREDIT'", null);
        double totalPending = 0.0;
        if (c.moveToFirst()) {
            totalPending = c.getDouble(0);
        }
        c.close();
        return totalPending;
    }

    public int getCustomerCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM customers WHERE (is_active IS NULL OR is_active = 1)", null);
        int count = 0;
        if (c.moveToFirst()) {
            count = c.getInt(0);
        }
        c.close();
        return count;
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