package com.inventorysalesmanager.app;

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

    private static final String DATABASE_NAME = "inventory_sales.db";
    private static final int DATABASE_VERSION = 1;

    // Products table
    private static final String TABLE_PRODUCTS = "products";
    private static final String COL_PROD_ID = "id";
    private static final String COL_PROD_NAME = "name";
    private static final String COL_PROD_SKU = "sku";
    private static final String COL_PROD_PURCHASE = "purchase_price";
    private static final String COL_PROD_SELLING = "selling_price";
    private static final String COL_PROD_STOCK = "stock_qty";
    private static final String COL_PROD_MIN_STOCK = "min_stock";

    // Sales table
    private static final String TABLE_SALES = "sales";
    private static final String COL_SALE_ID = "id";
    private static final String COL_SALE_PRODUCT_ID = "product_id";
    private static final String COL_SALE_QUANTITY = "quantity";
    private static final String COL_SALE_TOTAL_PRICE = "total_price";
    private static final String COL_SALE_DATE = "sale_date";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_PRODUCTS_TABLE = "CREATE TABLE " + TABLE_PRODUCTS + "("
                + COL_PROD_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COL_PROD_NAME + " TEXT NOT NULL,"
                + COL_PROD_SKU + " TEXT UNIQUE,"
                + COL_PROD_PURCHASE + " REAL,"
                + COL_PROD_SELLING + " REAL,"
                + COL_PROD_STOCK + " INTEGER DEFAULT 0,"
                + COL_PROD_MIN_STOCK + " INTEGER DEFAULT 0"
                + ")";

        String CREATE_SALES_TABLE = "CREATE TABLE " + TABLE_SALES + "("
                + COL_SALE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COL_SALE_PRODUCT_ID + " INTEGER,"
                + COL_SALE_QUANTITY + " INTEGER,"
                + COL_SALE_TOTAL_PRICE + " REAL,"
                + COL_SALE_DATE + " TEXT,"
                + "FOREIGN KEY(" + COL_SALE_PRODUCT_ID + ") REFERENCES " + TABLE_PRODUCTS + "(" + COL_PROD_ID + ")"
                + ")";

        db.execSQL(CREATE_PRODUCTS_TABLE);
        db.execSQL(CREATE_SALES_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SALES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PRODUCTS);
        onCreate(db);
    }

    // Product CRUD
    public long insertProduct(Product p) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_PROD_NAME, p.getName());
        values.put(COL_PROD_SKU, p.getSku());
        values.put(COL_PROD_PURCHASE, p.getPurchasePrice());
        values.put(COL_PROD_SELLING, p.getSellingPrice());
        values.put(COL_PROD_STOCK, p.getStockQty());
        values.put(COL_PROD_MIN_STOCK, p.getMinStock());

        long id = db.insert(TABLE_PRODUCTS, null, values);
        db.close();
        return id;
    }

    public int updateProduct(Product p) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_PROD_NAME, p.getName());
        values.put(COL_PROD_SKU, p.getSku());
        values.put(COL_PROD_PURCHASE, p.getPurchasePrice());
        values.put(COL_PROD_SELLING, p.getSellingPrice());
        values.put(COL_PROD_STOCK, p.getStockQty());
        values.put(COL_PROD_MIN_STOCK, p.getMinStock());

        int count = db.update(TABLE_PRODUCTS, values, COL_PROD_ID + " = ?", new String[]{String.valueOf(p.getId())});
        db.close();
        return count;
    }

    public void deleteProduct(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_SALES, COL_SALE_PRODUCT_ID + " = ?", new String[]{String.valueOf(id)});
        db.delete(TABLE_PRODUCTS, COL_PROD_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    public Product getProduct(long id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_PRODUCTS, null, COL_PROD_ID + " = ?", new String[]{String.valueOf(id)}, null, null, null);
        Product p = null;
        if (cursor != null && cursor.moveToFirst()) {
            p = new Product(
                    cursor.getLong(cursor.getColumnIndexOrThrow(COL_PROD_ID)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_PROD_NAME)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_PROD_SKU)),
                    cursor.getDouble(cursor.getColumnIndexOrThrow(COL_PROD_PURCHASE)),
                    cursor.getDouble(cursor.getColumnIndexOrThrow(COL_PROD_SELLING)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(COL_PROD_STOCK)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(COL_PROD_MIN_STOCK))
            );
            cursor.close();
        }
        db.close();
        return p;
    }

    public List<Product> getAllProducts(String searchQuery) {
        List<Product> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query;
        String[] selectionArgs = null;

        if (searchQuery == null || searchQuery.trim().isEmpty()) {
            query = "SELECT * FROM " + TABLE_PRODUCTS + " ORDER BY " + COL_PROD_NAME + " ASC";
        } else {
            query = "SELECT * FROM " + TABLE_PRODUCTS + " WHERE " + COL_PROD_NAME + " LIKE ? OR " + COL_PROD_SKU + " LIKE ? ORDER BY " + COL_PROD_NAME + " ASC";
            selectionArgs = new String[]{"%" + searchQuery + "%", "%" + searchQuery + "%"};
        }

        Cursor cursor = db.rawQuery(query, selectionArgs);
        if (cursor.moveToFirst()) {
            do {
                Product p = new Product(
                        cursor.getLong(cursor.getColumnIndexOrThrow(COL_PROD_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_PROD_NAME)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_PROD_SKU)),
                        cursor.getDouble(cursor.getColumnIndexOrThrow(COL_PROD_PURCHASE)),
                        cursor.getDouble(cursor.getColumnIndexOrThrow(COL_PROD_SELLING)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_PROD_STOCK)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_PROD_MIN_STOCK))
                );
                list.add(p);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }

    // Sales Processing
    public boolean recordSale(long productId, int qty, double totalPrice) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            // Check current stock first
            Cursor cursor = db.query(TABLE_PRODUCTS, new String[]{COL_PROD_STOCK}, COL_PROD_ID + " = ?", new String[]{String.valueOf(productId)}, null, null, null);
            if (cursor == null || !cursor.moveToFirst()) {
                if (cursor != null) cursor.close();
                return false;
            }
            int currentStock = cursor.getInt(0);
            cursor.close();

            if (currentStock < qty) {
                return false; // Not enough stock available
            }

            // Update stock level
            ContentValues updateValues = new ContentValues();
            updateValues.put(COL_PROD_STOCK, currentStock - qty);
            db.update(TABLE_PRODUCTS, updateValues, COL_PROD_ID + " = ?", new String[]{String.valueOf(productId)});

            // Insert Sale entry
            ContentValues saleValues = new ContentValues();
            saleValues.put(COL_SALE_PRODUCT_ID, productId);
            saleValues.put(COL_SALE_QUANTITY, qty);
            saleValues.put(COL_SALE_TOTAL_PRICE, totalPrice);

            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
            saleValues.put(COL_SALE_DATE, timestamp);

            db.insert(TABLE_SALES, null, saleValues);
            db.setTransactionSuccessful();
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    // Reports and Dashboard calculations
    public int getTotalProductsCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_PRODUCTS, null);
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        db.close();
        return count;
    }

    public int getTotalStockCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT SUM(" + COL_PROD_STOCK + ") FROM " + TABLE_PRODUCTS, null);
        int sum = 0;
        if (cursor.moveToFirst()) {
            sum = cursor.getInt(0);
        }
        cursor.close();
        db.close();
        return sum;
    }

    public double getTodaySalesSum() {
        SQLiteDatabase db = this.getReadableDatabase();
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        Cursor cursor = db.rawQuery("SELECT SUM(" + COL_SALE_TOTAL_PRICE + ") FROM " + TABLE_SALES + " WHERE " + COL_SALE_DATE + " LIKE ?", new String[]{today + "%"});
        double sum = 0;
        if (cursor.moveToFirst()) {
            sum = cursor.getDouble(0);
        }
        cursor.close();
        db.close();
        return sum;
    }

    public int getLowStockProductsCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_PRODUCTS + " WHERE " + COL_PROD_STOCK + " <= " + COL_PROD_MIN_STOCK, null);
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        db.close();
        return count;
    }

    public List<Product> getLowStockProducts() {
        List<Product> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_PRODUCTS + " WHERE " + COL_PROD_STOCK + " <= " + COL_PROD_MIN_STOCK + " ORDER BY " + COL_PROD_STOCK + " ASC", null);
        if (cursor.moveToFirst()) {
            do {
                Product p = new Product(
                        cursor.getLong(cursor.getColumnIndexOrThrow(COL_PROD_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_PROD_NAME)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_PROD_SKU)),
                        cursor.getDouble(cursor.getColumnIndexOrThrow(COL_PROD_PURCHASE)),
                        cursor.getDouble(cursor.getColumnIndexOrThrow(COL_PROD_SELLING)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_PROD_STOCK)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_PROD_MIN_STOCK))
                );
                list.add(p);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }

    public List<Sale> getTodaySales() {
        List<Sale> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        String query = "SELECT s." + COL_SALE_ID + ", s." + COL_SALE_PRODUCT_ID + ", p." + COL_PROD_NAME + ", s." + COL_SALE_QUANTITY + ", s." + COL_SALE_TOTAL_PRICE + ", s." + COL_SALE_DATE
                + " FROM " + TABLE_SALES + " s "
                + " JOIN " + TABLE_PRODUCTS + " p ON s." + COL_SALE_PRODUCT_ID + " = p." + COL_PROD_ID
                + " WHERE s." + COL_SALE_DATE + " LIKE ?"
                + " ORDER BY s." + COL_SALE_ID + " DESC";

        Cursor cursor = db.rawQuery(query, new String[]{today + "%"});
        if (cursor.moveToFirst()) {
            do {
                Sale s = new Sale(
                        cursor.getLong(0),
                        cursor.getLong(1),
                        cursor.getString(2),
                        cursor.getInt(3),
                        cursor.getDouble(4),
                        cursor.getString(5)
                );
                list.add(s);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }

    public List<Sale> getAllSales() {
        List<Sale> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT s." + COL_SALE_ID + ", s." + COL_SALE_PRODUCT_ID + ", p." + COL_PROD_NAME + ", s." + COL_SALE_QUANTITY + ", s." + COL_SALE_TOTAL_PRICE + ", s." + COL_SALE_DATE
                + " FROM " + TABLE_SALES + " s "
                + " JOIN " + TABLE_PRODUCTS + " p ON s." + COL_SALE_PRODUCT_ID + " = p." + COL_PROD_ID
                + " ORDER BY s." + COL_SALE_ID + " DESC";

        Cursor cursor = db.rawQuery(query, null);
        if (cursor.moveToFirst()) {
            do {
                Sale s = new Sale(
                        cursor.getLong(0),
                        cursor.getLong(1),
                        cursor.getString(2),
                        cursor.getInt(3),
                        cursor.getDouble(4),
                        cursor.getString(5)
                );
                list.add(s);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }

    // Statistics for a single product
    public int getProductUnitsSold(long productId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT SUM(" + COL_SALE_QUANTITY + ") FROM " + TABLE_SALES + " WHERE " + COL_SALE_PRODUCT_ID + " = ?", new String[]{String.valueOf(productId)});
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        db.close();
        return count;
    }

    public double getProductRevenueGenerated(long productId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT SUM(" + COL_SALE_TOTAL_PRICE + ") FROM " + TABLE_SALES + " WHERE " + COL_SALE_PRODUCT_ID + " = ?", new String[]{String.valueOf(productId)});
        double amount = 0;
        if (cursor.moveToFirst()) {
            amount = cursor.getDouble(0);
        }
        cursor.close();
        db.close();
        return amount;
    }

    // Reports Screen: Best Selling Items
    public List<ProductPerformance> getBestSellingProducts() {
        List<ProductPerformance> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT p." + COL_PROD_NAME + ", SUM(s." + COL_SALE_QUANTITY + ") as total_sold, SUM(s." + COL_SALE_TOTAL_PRICE + ") as revenue"
                + " FROM " + TABLE_SALES + " s"
                + " JOIN " + TABLE_PRODUCTS + " p ON s." + COL_SALE_PRODUCT_ID + " = p." + COL_PROD_ID
                + " GROUP BY s." + COL_SALE_PRODUCT_ID
                + " ORDER BY total_sold DESC LIMIT 5";

        Cursor cursor = db.rawQuery(query, null);
        if (cursor.moveToFirst()) {
            do {
                ProductPerformance item = new ProductPerformance(
                        cursor.getString(0),
                        cursor.getInt(1),
                        cursor.getDouble(2)
                );
                list.add(item);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }

    // Class representation for reports
    public static class ProductPerformance {
        public String productName;
        public int unitsSold;
        public double revenue;

        public ProductPerformance(String productName, int unitsSold, double revenue) {
            this.productName = productName;
            this.unitsSold = unitsSold;
            this.revenue = revenue;
        }
    }

    // Reports Screen: Daily Sales summary
    public List<DailySummary> getDailySalesSummary() {
        List<DailySummary> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT SUBSTR(" + COL_SALE_DATE + ", 1, 10) as day, SUM(" + COL_SALE_TOTAL_PRICE + "), COUNT(" + COL_SALE_ID + ")"
                + " FROM " + TABLE_SALES
                + " GROUP BY day"
                + " ORDER BY day DESC LIMIT 7";

        Cursor cursor = db.rawQuery(query, null);
        if (cursor.moveToFirst()) {
            do {
                DailySummary item = new DailySummary(
                        cursor.getString(0),
                        cursor.getDouble(1),
                        cursor.getInt(2)
                );
                list.add(item);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }

    public static class DailySummary {
        public String date;
        public double totalRevenue;
        public int transactionCount;

        public DailySummary(String date, double totalRevenue, int transactionCount) {
            this.date = date;
            this.totalRevenue = totalRevenue;
            this.transactionCount = transactionCount;
        }
    }
}