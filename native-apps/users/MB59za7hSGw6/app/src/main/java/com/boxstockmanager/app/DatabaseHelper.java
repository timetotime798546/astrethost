package com.boxstockmanager.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "DatabaseHelper";
    private static final String DATABASE_NAME = "box_inventory.db";
    private static final int DATABASE_VERSION = 2;

    public static final String TABLE_INVENTORY = "inventory";
    public static final String COLUMN_LEGACY_ID = "_id";
    public static final String COLUMN_LEGACY_QR_CODE = "qr_code";
    public static final String COLUMN_LEGACY_QUANTITY = "quantity";
    public static final String COLUMN_LEGACY_LAST_UPDATED = "last_updated";

    public static final String TABLE_PRODUCTS = "products";
    public static final String COLUMN_PROD_ID = "id";
    public static final String COLUMN_PROD_NAME = "product_name";
    public static final String COLUMN_PROD_BRAND = "brand";
    public static final String COLUMN_PROD_MODEL = "model";
    public static final String COLUMN_PROD_CODE = "product_code";
    public static final String COLUMN_PROD_BARCODE = "barcode";
    public static final String COLUMN_PROD_IMEI1 = "imei1";
    public static final String COLUMN_PROD_IMEI2 = "imei2";
    public static final String COLUMN_PROD_SERIAL = "serial_number";
    public static final String COLUMN_PROD_CATEGORY = "category";
    public static final String COLUMN_PROD_COLOR = "color";
    public static final String COLUMN_PROD_RAM = "ram";
    public static final String COLUMN_PROD_STORAGE = "storage";
    public static final String COLUMN_PROD_PURCHASE_PRICE = "purchase_price";
    public static final String COLUMN_PROD_SELLING_PRICE = "selling_price";
    public static final String COLUMN_PROD_STOCK_QTY = "stock_quantity";
    public static final String COLUMN_PROD_UNIT = "unit";
    public static final String COLUMN_PROD_SUPPLIER_NAME = "supplier_name";
    public static final String COLUMN_PROD_SUPPLIER_PHONE = "supplier_phone";
    public static final String COLUMN_PROD_PURCHASE_DATE = "purchase_date";
    public static final String COLUMN_PROD_WARRANTY = "warranty";
    public static final String COLUMN_PROD_DESCRIPTION = "description";
    public static final String COLUMN_PROD_IMAGE_PATH = "image_path";
    public static final String COLUMN_PROD_LOCATION = "location";
    public static final String COLUMN_PROD_IS_DELETED = "is_deleted";
    public static final String COLUMN_PROD_CREATED_AT = "created_at";
    public static final String COLUMN_PROD_UPDATED_AT = "updated_at";

    public static final String TABLE_TRANSACTIONS = "stock_transactions";
    public static final String COLUMN_TX_ID = "id";
    public static final String COLUMN_TX_PROD_ID = "product_id";
    public static final String COLUMN_TX_TYPE = "transaction_type";
    public static final String COLUMN_TX_QTY = "quantity";
    public static final String COLUMN_TX_PREV_STOCK = "previous_stock";
    public static final String COLUMN_TX_UPD_STOCK = "updated_stock";
    public static final String COLUMN_TX_DATE = "transaction_date";
    public static final String COLUMN_TX_NOTES = "notes";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createLegacy = "CREATE TABLE " + TABLE_INVENTORY + " (" +
                COLUMN_LEGACY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_LEGACY_QR_CODE + " TEXT UNIQUE NOT NULL, " +
                COLUMN_LEGACY_QUANTITY + " INTEGER NOT NULL, " +
                COLUMN_LEGACY_LAST_UPDATED + " TEXT NOT NULL);";

        String createProducts = "CREATE TABLE " + TABLE_PRODUCTS + " (" +
                COLUMN_PROD_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_PROD_NAME + " TEXT NOT NULL, " +
                COLUMN_PROD_BRAND + " TEXT, " +
                COLUMN_PROD_MODEL + " TEXT, " +
                COLUMN_PROD_CODE + " TEXT UNIQUE, " +
                COLUMN_PROD_BARCODE + " TEXT, " +
                COLUMN_PROD_IMEI1 + " TEXT, " +
                COLUMN_PROD_IMEI2 + " TEXT, " +
                COLUMN_PROD_SERIAL + " TEXT, " +
                COLUMN_PROD_CATEGORY + " TEXT, " +
                COLUMN_PROD_COLOR + " TEXT, " +
                COLUMN_PROD_RAM + " TEXT, " +
                COLUMN_PROD_STORAGE + " TEXT, " +
                COLUMN_PROD_PURCHASE_PRICE + " REAL, " +
                COLUMN_PROD_SELLING_PRICE + " REAL, " +
                COLUMN_PROD_STOCK_QTY + " INTEGER NOT NULL DEFAULT 0, " +
                COLUMN_PROD_UNIT + " TEXT, " +
                COLUMN_PROD_SUPPLIER_NAME + " TEXT, " +
                COLUMN_PROD_SUPPLIER_PHONE + " TEXT, " +
                COLUMN_PROD_PURCHASE_DATE + " TEXT, " +
                COLUMN_PROD_WARRANTY + " TEXT, " +
                COLUMN_PROD_DESCRIPTION + " TEXT, " +
                COLUMN_PROD_IMAGE_PATH + " TEXT, " +
                COLUMN_PROD_LOCATION + " TEXT, " +
                COLUMN_PROD_IS_DELETED + " INTEGER DEFAULT 0, " +
                COLUMN_PROD_CREATED_AT + " TEXT, " +
                COLUMN_PROD_UPDATED_AT + " TEXT);";

        String createTransactions = "CREATE TABLE " + TABLE_TRANSACTIONS + " (" +
                COLUMN_TX_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_TX_PROD_ID + " INTEGER NOT NULL, " +
                COLUMN_TX_TYPE + " TEXT NOT NULL, " +
                COLUMN_TX_QTY + " INTEGER NOT NULL, " +
                COLUMN_TX_PREV_STOCK + " INTEGER NOT NULL, " +
                COLUMN_TX_UPD_STOCK + " INTEGER NOT NULL, " +
                COLUMN_TX_DATE + " TEXT NOT NULL, " +
                COLUMN_TX_NOTES + " TEXT);";

        db.execSQL(createLegacy);
        db.execSQL(createProducts);
        db.execSQL(createTransactions);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            String createProducts = "CREATE TABLE IF NOT EXISTS " + TABLE_PRODUCTS + " (" +
                    COLUMN_PROD_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_PROD_NAME + " TEXT NOT NULL, " +
                    COLUMN_PROD_BRAND + " TEXT, " +
                    COLUMN_PROD_MODEL + " TEXT, " +
                    COLUMN_PROD_CODE + " TEXT UNIQUE, " +
                    COLUMN_PROD_BARCODE + " TEXT, " +
                    COLUMN_PROD_IMEI1 + " TEXT, " +
                    COLUMN_PROD_IMEI2 + " TEXT, " +
                    COLUMN_PROD_SERIAL + " TEXT, " +
                    COLUMN_PROD_CATEGORY + " TEXT, " +
                    COLUMN_PROD_COLOR + " TEXT, " +
                    COLUMN_PROD_RAM + " TEXT, " +
                    COLUMN_PROD_STORAGE + " TEXT, " +
                    COLUMN_PROD_PURCHASE_PRICE + " REAL, " +
                    COLUMN_PROD_SELLING_PRICE + " REAL, " +
                    COLUMN_PROD_STOCK_QTY + " INTEGER NOT NULL DEFAULT 0, " +
                    COLUMN_PROD_UNIT + " TEXT, " +
                    COLUMN_PROD_SUPPLIER_NAME + " TEXT, " +
                    COLUMN_PROD_SUPPLIER_PHONE + " TEXT, " +
                    COLUMN_PROD_PURCHASE_DATE + " TEXT, " +
                    COLUMN_PROD_WARRANTY + " TEXT, " +
                    COLUMN_PROD_DESCRIPTION + " TEXT, " +
                    COLUMN_PROD_IMAGE_PATH + " TEXT, " +
                    COLUMN_PROD_LOCATION + " TEXT, " +
                    COLUMN_PROD_IS_DELETED + " INTEGER DEFAULT 0, " +
                    COLUMN_PROD_CREATED_AT + " TEXT, " +
                    COLUMN_PROD_UPDATED_AT + " TEXT);";

            String createTransactions = "CREATE TABLE IF NOT EXISTS " + TABLE_TRANSACTIONS + " (" +
                    COLUMN_TX_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_TX_PROD_ID + " INTEGER NOT NULL, " +
                    COLUMN_TX_TYPE + " TEXT NOT NULL, " +
                    COLUMN_TX_QTY + " INTEGER NOT NULL, " +
                    COLUMN_TX_PREV_STOCK + " INTEGER NOT NULL, " +
                    COLUMN_TX_UPD_STOCK + " INTEGER NOT NULL, " +
                    COLUMN_TX_DATE + " TEXT NOT NULL, " +
                    COLUMN_TX_NOTES + " TEXT);";

            db.execSQL(createProducts);
            db.execSQL(createTransactions);
        }
    }

    private String getCurrentTimestamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
    }

    private String getCurrentDateOnly() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }

    // --- LEGACY BOX STOCK METHOD FOR BACKWARDS COMPATIBILITY ---
    public int updateStock(String qrCode, boolean isStockIn) {
        SQLiteDatabase db = this.getWritableDatabase();
        String currentTime = getCurrentTimestamp();

        Cursor cursor = db.query(
                TABLE_INVENTORY,
                new String[]{COLUMN_LEGACY_ID, COLUMN_LEGACY_QUANTITY},
                COLUMN_LEGACY_QR_CODE + "=?",
                new String[]{qrCode},
                null, null, null
        );

        int newQuantity = 0;
        try {
            if (cursor != null && cursor.moveToFirst()) {
                int currentQty = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_LEGACY_QUANTITY));
                if (isStockIn) {
                    newQuantity = currentQty + 1;
                } else {
                    newQuantity = Math.max(0, currentQty - 1);
                }

                ContentValues values = new ContentValues();
                values.put(COLUMN_LEGACY_QUANTITY, newQuantity);
                values.put(COLUMN_LEGACY_LAST_UPDATED, currentTime);

                db.update(TABLE_INVENTORY, values, COLUMN_LEGACY_QR_CODE + "=?", new String[]{qrCode});
            } else {
                newQuantity = isStockIn ? 1 : 0;
                ContentValues values = new ContentValues();
                values.put(COLUMN_LEGACY_QR_CODE, qrCode);
                values.put(COLUMN_LEGACY_QUANTITY, newQuantity);
                values.put(COLUMN_LEGACY_LAST_UPDATED, currentTime);
                db.insert(TABLE_INVENTORY, null, values);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating legacy stock: " + e.getMessage(), e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        Product p = findProductByCodeOrImei(qrCode);
        if (p != null) {
            recordStockMovement(p.getId(), isStockIn ? "IN" : "OUT", 1, "Scanned via Box Stock Quick Mode");
        }

        return newQuantity;
    }

    public List<BoxItem> getAllInventory() {
        List<BoxItem> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(
                TABLE_INVENTORY,
                new String[]{COLUMN_LEGACY_ID, COLUMN_LEGACY_QR_CODE, COLUMN_LEGACY_QUANTITY, COLUMN_LEGACY_LAST_UPDATED},
                null, null, null, null, COLUMN_LEGACY_LAST_UPDATED + " DESC"
        );

        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    long id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_LEGACY_ID));
                    String code = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LEGACY_QR_CODE));
                    int qty = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_LEGACY_QUANTITY));
                    String updated = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LEGACY_LAST_UPDATED));
                    list.add(new BoxItem(id, code, qty, updated));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error reading legacy inventory: " + e.getMessage(), e);
            } finally {
                cursor.close();
            }
        }
        return list;
    }

    // --- PRODUCTS MANAGEMENT ---
    public long insertProduct(Product p) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        String time = getCurrentTimestamp();

        String code = p.getProductCode();
        if (code == null || code.trim().isEmpty()) {
            code = "PRD-" + System.currentTimeMillis();
            p.setProductCode(code);
        }

        v.put(COLUMN_PROD_NAME, p.getName());
        v.put(COLUMN_PROD_BRAND, p.getBrand());
        v.put(COLUMN_PROD_MODEL, p.getModel());
        v.put(COLUMN_PROD_CODE, code);
        v.put(COLUMN_PROD_BARCODE, p.getBarcode());
        v.put(COLUMN_PROD_IMEI1, p.getImei1());
        v.put(COLUMN_PROD_IMEI2, p.getImei2());
        v.put(COLUMN_PROD_SERIAL, p.getSerialNumber());
        v.put(COLUMN_PROD_CATEGORY, p.getCategory());
        v.put(COLUMN_PROD_COLOR, p.getColor());
        v.put(COLUMN_PROD_RAM, p.getRam());
        v.put(COLUMN_PROD_STORAGE, p.getStorage());
        v.put(COLUMN_PROD_PURCHASE_PRICE, p.getPurchasePrice());
        v.put(COLUMN_PROD_SELLING_PRICE, p.getSellingPrice());
        v.put(COLUMN_PROD_STOCK_QTY, Math.max(0, p.getStockQuantity()));
        v.put(COLUMN_PROD_UNIT, p.getUnit());
        v.put(COLUMN_PROD_SUPPLIER_NAME, p.getSupplierName());
        v.put(COLUMN_PROD_SUPPLIER_PHONE, p.getSupplierPhone());
        v.put(COLUMN_PROD_PURCHASE_DATE, p.getPurchaseDate());
        v.put(COLUMN_PROD_WARRANTY, p.getWarranty());
        v.put(COLUMN_PROD_DESCRIPTION, p.getDescription());
        v.put(COLUMN_PROD_IMAGE_PATH, p.getImagePath());
        v.put(COLUMN_PROD_LOCATION, p.getLocation());
        v.put(COLUMN_PROD_IS_DELETED, 0);
        v.put(COLUMN_PROD_CREATED_AT, time);
        v.put(COLUMN_PROD_UPDATED_AT, time);

        long id = -1;
        try {
            id = db.insert(TABLE_PRODUCTS, null, v);
            if (id > 0 && p.getStockQuantity() > 0) {
                ContentValues tx = new ContentValues();
                tx.put(COLUMN_TX_PROD_ID, id);
                tx.put(COLUMN_TX_TYPE, "IN");
                tx.put(COLUMN_TX_QTY, p.getStockQuantity());
                tx.put(COLUMN_TX_PREV_STOCK, 0);
                tx.put(COLUMN_TX_UPD_STOCK, p.getStockQuantity());
                tx.put(COLUMN_TX_DATE, time);
                tx.put(COLUMN_TX_NOTES, "Initial product stock entry");
                db.insert(TABLE_TRANSACTIONS, null, tx);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error inserting product into SQLite database: " + e.getMessage(), e);
        }
        return id;
    }

    public boolean updateProduct(Product p) {
        if (p == null || p.getId() <= 0) {
            Log.e(TAG, "Cannot update product: invalid product ID (" + (p != null ? p.getId() : "null") + ")");
            return false;
        }

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(COLUMN_PROD_NAME, p.getName());
        v.put(COLUMN_PROD_BRAND, p.getBrand());
        v.put(COLUMN_PROD_MODEL, p.getModel());
        v.put(COLUMN_PROD_CODE, p.getProductCode());
        v.put(COLUMN_PROD_BARCODE, p.getBarcode());
        v.put(COLUMN_PROD_IMEI1, p.getImei1());
        v.put(COLUMN_PROD_IMEI2, p.getImei2());
        v.put(COLUMN_PROD_SERIAL, p.getSerialNumber());
        v.put(COLUMN_PROD_CATEGORY, p.getCategory());
        v.put(COLUMN_PROD_COLOR, p.getColor());
        v.put(COLUMN_PROD_RAM, p.getRam());
        v.put(COLUMN_PROD_STORAGE, p.getStorage());
        v.put(COLUMN_PROD_PURCHASE_PRICE, p.getPurchasePrice());
        v.put(COLUMN_PROD_SELLING_PRICE, p.getSellingPrice());
        v.put(COLUMN_PROD_UNIT, p.getUnit());
        v.put(COLUMN_PROD_SUPPLIER_NAME, p.getSupplierName());
        v.put(COLUMN_PROD_SUPPLIER_PHONE, p.getSupplierPhone());
        v.put(COLUMN_PROD_PURCHASE_DATE, p.getPurchaseDate());
        v.put(COLUMN_PROD_WARRANTY, p.getWarranty());
        v.put(COLUMN_PROD_DESCRIPTION, p.getDescription());
        v.put(COLUMN_PROD_IMAGE_PATH, p.getImagePath());
        v.put(COLUMN_PROD_LOCATION, p.getLocation());
        v.put(COLUMN_PROD_UPDATED_AT, getCurrentTimestamp());

        try {
            int rows = db.update(TABLE_PRODUCTS, v, COLUMN_PROD_ID + "=?", new String[]{String.valueOf(p.getId())});
            return rows > 0;
        } catch (Exception e) {
            Log.e(TAG, "Error updating product ID " + p.getId() + ": " + e.getMessage(), e);
            return false;
        }
    }

    public boolean softDeleteProduct(long productId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(COLUMN_PROD_IS_DELETED, 1);
        v.put(COLUMN_PROD_UPDATED_AT, getCurrentTimestamp());
        try {
            int rows = db.update(TABLE_PRODUCTS, v, COLUMN_PROD_ID + "=?", new String[]{String.valueOf(productId)});
            return rows > 0;
        } catch (Exception e) {
            Log.e(TAG, "Error deleting product ID " + productId + ": " + e.getMessage(), e);
            return false;
        }
    }

    public Product getProductById(long id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_PRODUCTS, null,
                COLUMN_PROD_ID + "=? AND " + COLUMN_PROD_IS_DELETED + "=0",
                new String[]{String.valueOf(id)},
                null, null, null
        );

        Product p = null;
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    p = parseProductCursor(cursor);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching product by ID: " + e.getMessage(), e);
            } finally {
                cursor.close();
            }
        }
        return p;
    }

    public Product findProductByCodeOrImei(String queryStr) {
        if (queryStr == null || queryStr.trim().isEmpty()) return null;
        String term = queryStr.trim();
        SQLiteDatabase db = this.getReadableDatabase();

        String selection = "(" + COLUMN_PROD_CODE + "=? OR " + COLUMN_PROD_BARCODE + "=? OR " +
                COLUMN_PROD_IMEI1 + "=? OR " + COLUMN_PROD_IMEI2 + "=? OR " + COLUMN_PROD_SERIAL + "=?) AND " +
                COLUMN_PROD_IS_DELETED + "=0";

        Cursor cursor = db.query(TABLE_PRODUCTS, null, selection,
                new String[]{term, term, term, term, term}, null, null, null);

        Product p = null;
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    p = parseProductCursor(cursor);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error searching product by code: " + e.getMessage(), e);
            } finally {
                cursor.close();
            }
        }
        return p;
    }

    public List<Product> getAllActiveProducts(String searchQuery, String categoryFilter, String stockStatusFilter, String sortBy) {
        List<Product> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        StringBuilder where = new StringBuilder(COLUMN_PROD_IS_DELETED + "=0");
        List<String> args = new ArrayList<>();

        if (searchQuery != null && !searchQuery.trim().isEmpty()) {
            String q = "%" + searchQuery.trim() + "%";
            where.append(" AND (").append(COLUMN_PROD_NAME).append(" LIKE ? OR ")
                 .append(COLUMN_PROD_CODE).append(" LIKE ? OR ")
                 .append(COLUMN_PROD_BARCODE).append(" LIKE ? OR ")
                 .append(COLUMN_PROD_IMEI1).append(" LIKE ? OR ")
                 .append(COLUMN_PROD_BRAND).append(" LIKE ?)");
            args.add(q); args.add(q); args.add(q); args.add(q); args.add(q);
        }

        if (categoryFilter != null && !categoryFilter.equalsIgnoreCase("All Categories") && !categoryFilter.trim().isEmpty()) {
            where.append(" AND ").append(COLUMN_PROD_CATEGORY).append("=?");
            args.add(categoryFilter.trim());
        }

        if (stockStatusFilter != null && !stockStatusFilter.equalsIgnoreCase("All Status")) {
            if (stockStatusFilter.equalsIgnoreCase("Out of Stock")) {
                where.append(" AND ").append(COLUMN_PROD_STOCK_QTY).append("<=0");
            } else if (stockStatusFilter.equalsIgnoreCase("Low Stock")) {
                where.append(" AND ").append(COLUMN_PROD_STOCK_QTY).append(">0 AND ").append(COLUMN_PROD_STOCK_QTY).append("<=5");
            } else if (stockStatusFilter.equalsIgnoreCase("In Stock")) {
                where.append(" AND ").append(COLUMN_PROD_STOCK_QTY).append(">5");
            }
        }

        String orderBy = COLUMN_PROD_NAME + " ASC";
        if (sortBy != null) {
            if (sortBy.equalsIgnoreCase("Price: Low to High")) {
                orderBy = COLUMN_PROD_SELLING_PRICE + " ASC";
            } else if (sortBy.equalsIgnoreCase("Price: High to Low")) {
                orderBy = COLUMN_PROD_SELLING_PRICE + " DESC";
            } else if (sortBy.equalsIgnoreCase("Stock: High to Low")) {
                orderBy = COLUMN_PROD_STOCK_QTY + " DESC";
            } else if (sortBy.equalsIgnoreCase("Stock: Low to High")) {
                orderBy = COLUMN_PROD_STOCK_QTY + " ASC";
            }
        }

        Cursor cursor = db.query(TABLE_PRODUCTS, null, where.toString(),
                args.toArray(new String[0]), null, null, orderBy);

        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    list.add(parseProductCursor(cursor));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error retrieving active products: " + e.getMessage(), e);
            } finally {
                cursor.close();
            }
        }
        return list;
    }

    public List<String> getUniqueCategories() {
        List<String> categories = new ArrayList<>();
        categories.add("All Categories");
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(true, TABLE_PRODUCTS, new String[]{COLUMN_PROD_CATEGORY},
                COLUMN_PROD_IS_DELETED + "=0", null, null, null, COLUMN_PROD_CATEGORY + " ASC", null);

        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    String cat = cursor.getString(0);
                    if (cat != null && !cat.trim().isEmpty() && !categories.contains(cat.trim())) {
                        categories.add(cat.trim());
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching unique categories: " + e.getMessage(), e);
            } finally {
                cursor.close();
            }
        }
        return categories;
    }

    private Product parseProductCursor(Cursor c) {
        Product p = new Product();
        p.setId(c.getLong(c.getColumnIndexOrThrow(COLUMN_PROD_ID)));
        p.setName(c.getString(c.getColumnIndexOrThrow(COLUMN_PROD_NAME)));
        p.setBrand(c.getString(c.getColumnIndexOrThrow(COLUMN_PROD_BRAND)));
        p.setModel(c.getString(c.getColumnIndexOrThrow(COLUMN_PROD_MODEL)));
        p.setProductCode(c.getString(c.getColumnIndexOrThrow(COLUMN_PROD_CODE)));
        p.setBarcode(c.getString(c.getColumnIndexOrThrow(COLUMN_PROD_BARCODE)));
        p.setImei1(c.getString(c.getColumnIndexOrThrow(COLUMN_PROD_IMEI1)));
        p.setImei2(c.getString(c.getColumnIndexOrThrow(COLUMN_PROD_IMEI2)));
        p.setSerialNumber(c.getString(c.getColumnIndexOrThrow(COLUMN_PROD_SERIAL)));
        p.setCategory(c.getString(c.getColumnIndexOrThrow(COLUMN_PROD_CATEGORY)));
        p.setColor(c.getString(c.getColumnIndexOrThrow(COLUMN_PROD_COLOR)));
        p.setRam(c.getString(c.getColumnIndexOrThrow(COLUMN_PROD_RAM)));
        p.setStorage(c.getString(c.getColumnIndexOrThrow(COLUMN_PROD_STORAGE)));
        p.setPurchasePrice(c.getDouble(c.getColumnIndexOrThrow(COLUMN_PROD_PURCHASE_PRICE)));
        p.setSellingPrice(c.getDouble(c.getColumnIndexOrThrow(COLUMN_PROD_SELLING_PRICE)));
        p.setStockQuantity(c.getInt(c.getColumnIndexOrThrow(COLUMN_PROD_STOCK_QTY)));
        p.setUnit(c.getString(c.getColumnIndexOrThrow(COLUMN_PROD_UNIT)));
        p.setSupplierName(c.getString(c.getColumnIndexOrThrow(COLUMN_PROD_SUPPLIER_NAME)));
        p.setSupplierPhone(c.getString(c.getColumnIndexOrThrow(COLUMN_PROD_SUPPLIER_PHONE)));
        p.setPurchaseDate(c.getString(c.getColumnIndexOrThrow(COLUMN_PROD_PURCHASE_DATE)));
        p.setWarranty(c.getString(c.getColumnIndexOrThrow(COLUMN_PROD_WARRANTY)));
        p.setDescription(c.getString(c.getColumnIndexOrThrow(COLUMN_PROD_DESCRIPTION)));
        p.setImagePath(c.getString(c.getColumnIndexOrThrow(COLUMN_PROD_IMAGE_PATH)));
        p.setLocation(c.getString(c.getColumnIndexOrThrow(COLUMN_PROD_LOCATION)));
        p.setCreatedAt(c.getString(c.getColumnIndexOrThrow(COLUMN_PROD_CREATED_AT)));
        return p;
    }

    // --- STOCK MOVEMENTS & TRANSACTIONS ---
    public synchronized boolean recordStockMovement(long productId, String type, int qty, String notes) {
        if (qty <= 0) return false;
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            Cursor c = db.query(TABLE_PRODUCTS, new String[]{COLUMN_PROD_STOCK_QTY},
                    COLUMN_PROD_ID + "=?", new String[]{String.valueOf(productId)},
                    null, null, null);

            if (c == null || !c.moveToFirst()) {
                if (c != null) c.close();
                return false;
            }

            int currentStock = c.getInt(0);
            c.close();

            int updatedStock = currentStock;
            if ("IN".equalsIgnoreCase(type)) {
                updatedStock = currentStock + qty;
            } else if ("OUT".equalsIgnoreCase(type)) {
                if (currentStock < qty) {
                    db.endTransaction();
                    return false;
                }
                updatedStock = currentStock - qty;
            }

            String time = getCurrentTimestamp();

            ContentValues pv = new ContentValues();
            pv.put(COLUMN_PROD_STOCK_QTY, updatedStock);
            pv.put(COLUMN_PROD_UPDATED_AT, time);
            db.update(TABLE_PRODUCTS, pv, COLUMN_PROD_ID + "=?", new String[]{String.valueOf(productId)});

            ContentValues tv = new ContentValues();
            tv.put(COLUMN_TX_PROD_ID, productId);
            tv.put(COLUMN_TX_TYPE, type.toUpperCase(Locale.US));
            tv.put(COLUMN_TX_QTY, qty);
            tv.put(COLUMN_TX_PREV_STOCK, currentStock);
            tv.put(COLUMN_TX_UPD_STOCK, updatedStock);
            tv.put(COLUMN_TX_DATE, time);
            tv.put(COLUMN_TX_NOTES, notes);

            db.insert(TABLE_TRANSACTIONS, null, tv);

            db.setTransactionSuccessful();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error recording stock movement: " + e.getMessage(), e);
            return false;
        } finally {
            db.endTransaction();
        }
    }

    public List<StockTransaction> getTransactions(String typeFilter, String searchProdQuery, String dateFilter) {
        List<StockTransaction> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT t.*, p." + COLUMN_PROD_NAME + ", p." + COLUMN_PROD_CODE +
                " FROM " + TABLE_TRANSACTIONS + " t INNER JOIN " + TABLE_PRODUCTS + " p ON t." +
                COLUMN_TX_PROD_ID + " = p." + COLUMN_PROD_ID + " WHERE 1=1";

        List<String> args = new ArrayList<>();
        if (typeFilter != null && !typeFilter.equalsIgnoreCase("ALL")) {
            query += " AND t." + COLUMN_TX_TYPE + "=?";
            args.add(typeFilter.toUpperCase(Locale.US));
        }

        if (searchProdQuery != null && !searchProdQuery.trim().isEmpty()) {
            query += " AND (p." + COLUMN_PROD_NAME + " LIKE ? OR p." + COLUMN_PROD_CODE + " LIKE ?)";
            String q = "%" + searchProdQuery.trim() + "%";
            args.add(q); args.add(q);
        }

        if (dateFilter != null && !dateFilter.trim().isEmpty()) {
            query += " AND t." + COLUMN_TX_DATE + " LIKE ?";
            args.add(dateFilter.trim() + "%");
        }

        query += " ORDER BY t." + COLUMN_TX_ID + " DESC";

        Cursor c = db.rawQuery(query, args.toArray(new String[0]));
        if (c != null) {
            try {
                while (c.moveToNext()) {
                    StockTransaction st = new StockTransaction();
                    st.setId(c.getLong(c.getColumnIndexOrThrow(COLUMN_TX_ID)));
                    st.setProductId(c.getLong(c.getColumnIndexOrThrow(COLUMN_TX_PROD_ID)));
                    st.setType(c.getString(c.getColumnIndexOrThrow(COLUMN_TX_TYPE)));
                    st.setQuantity(c.getInt(c.getColumnIndexOrThrow(COLUMN_TX_QTY)));
                    st.setPreviousStock(c.getInt(c.getColumnIndexOrThrow(COLUMN_TX_PREV_STOCK)));
                    st.setUpdatedStock(c.getInt(c.getColumnIndexOrThrow(COLUMN_TX_UPD_STOCK)));
                    st.setTransactionDate(c.getString(c.getColumnIndexOrThrow(COLUMN_TX_DATE)));
                    st.setNotes(c.getString(c.getColumnIndexOrThrow(COLUMN_TX_NOTES)));
                    st.setProductName(c.getString(c.getColumnIndexOrThrow(COLUMN_PROD_NAME)));
                    st.setProductCode(c.getString(c.getColumnIndexOrThrow(COLUMN_PROD_CODE)));
                    list.add(st);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error querying stock transactions: " + e.getMessage(), e);
            } finally {
                c.close();
            }
        }
        return list;
    }

    public synchronized boolean deleteTransactionAndRecalculate(long txId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            Cursor c = db.query(TABLE_TRANSACTIONS, null, COLUMN_TX_ID + "=?", new String[]{String.valueOf(txId)},
                    null, null, null);

            if (c == null || !c.moveToFirst()) {
                if (c != null) c.close();
                return false;
            }

            long productId = c.getLong(c.getColumnIndexOrThrow(COLUMN_TX_PROD_ID));
            String type = c.getString(c.getColumnIndexOrThrow(COLUMN_TX_TYPE));
            int qty = c.getInt(c.getColumnIndexOrThrow(COLUMN_TX_QTY));
            c.close();

            Product prod = getProductById(productId);
            if (prod != null) {
                int currentStock = prod.getStockQuantity();
                int newStock = currentStock;

                if ("IN".equalsIgnoreCase(type)) {
                    newStock = Math.max(0, currentStock - qty);
                } else if ("OUT".equalsIgnoreCase(type)) {
                    newStock = currentStock + qty;
                }

                ContentValues pv = new ContentValues();
                pv.put(COLUMN_PROD_STOCK_QTY, newStock);
                pv.put(COLUMN_PROD_UPDATED_AT, getCurrentTimestamp());
                db.update(TABLE_PRODUCTS, pv, COLUMN_PROD_ID + "=?", new String[]{String.valueOf(productId)});
            }

            db.delete(TABLE_TRANSACTIONS, COLUMN_TX_ID + "=?", new String[]{String.valueOf(txId)});
            db.setTransactionSuccessful();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error reverting transaction: " + e.getMessage(), e);
            return false;
        } finally {
            db.endTransaction();
        }
    }

    // --- DASHBOARD METRICS ---
    public int getTotalProductsCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_PRODUCTS + " WHERE " + COLUMN_PROD_IS_DELETED + "=0", null);
        int count = 0;
        if (c != null) {
            try {
                if (c.moveToFirst()) count = c.getInt(0);
            } finally {
                c.close();
            }
        }
        return count;
    }

    public int getTotalStockQuantity() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT SUM(" + COLUMN_PROD_STOCK_QTY + ") FROM " + TABLE_PRODUCTS + " WHERE " + COLUMN_PROD_IS_DELETED + "=0", null);
        int sum = 0;
        if (c != null) {
            try {
                if (c.moveToFirst()) sum = c.getInt(0);
            } finally {
                c.close();
            }
        }
        return sum;
    }

    public int getStockInToday() {
        return getTransactionSumToday("IN");
    }

    public int getStockOutToday() {
        return getTransactionSumToday("OUT");
    }

    private int getTransactionSumToday(String type) {
        SQLiteDatabase db = this.getReadableDatabase();
        String today = getCurrentDateOnly();
        Cursor c = db.rawQuery("SELECT SUM(" + COLUMN_TX_QTY + ") FROM " + TABLE_TRANSACTIONS +
                " WHERE " + COLUMN_TX_TYPE + "=? AND " + COLUMN_TX_DATE + " LIKE ?",
                new String[]{type, today + "%"});
        int sum = 0;
        if (c != null) {
            try {
                if (c.moveToFirst()) sum = c.getInt(0);
            } finally {
                c.close();
            }
        }
        return sum;
    }

    public int getLowStockProductsCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_PRODUCTS + " WHERE " +
                COLUMN_PROD_IS_DELETED + "=0 AND " + COLUMN_PROD_STOCK_QTY + ">0 AND " + COLUMN_PROD_STOCK_QTY + "<=5", null);
        int count = 0;
        if (c != null) {
            try {
                if (c.moveToFirst()) count = c.getInt(0);
            } finally {
                c.close();
            }
        }
        return count;
    }

    public int getOutOfStockProductsCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_PRODUCTS + " WHERE " +
                COLUMN_PROD_IS_DELETED + "=0 AND " + COLUMN_PROD_STOCK_QTY + "<=0", null);
        int count = 0;
        if (c != null) {
            try {
                if (c.moveToFirst()) count = c.getInt(0);
            } finally {
                c.close();
            }
        }
        return count;
    }
}