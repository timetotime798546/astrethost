package com.smartocrscanner.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class ScanHistoryDbHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "ocr_scans.db";
    private static final int DATABASE_VERSION = 2;

    // Existing OCR Scans table
    public static final String TABLE_SCANS = "scans";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_TEXT = "text";
    public static final String COLUMN_SOURCE = "source";
    public static final String COLUMN_TIMESTAMP = "timestamp";
    public static final String COLUMN_CHAR_COUNT = "char_count";

    // Mobiles Inventory Table
    public static final String TABLE_MOBILES = "mobiles";
    public static final String COL_M_BRAND = "brand";
    public static final String COL_M_MODEL = "model";
    public static final String COL_M_MODEL_NO = "model_number";
    public static final String COL_M_IMEI1 = "imei1";
    public static final String COL_M_IMEI2 = "imei2";
    public static final String COL_M_SERIAL = "serial_number";
    public static final String COL_M_RAM = "ram";
    public static final String COL_M_STORAGE = "storage";
    public static final String COL_M_COLOR = "color";
    public static final String COL_M_CODE = "product_code";
    public static final String COL_M_PURCHASE_PRICE = "purchase_price";
    public static final String COL_M_SELLING_PRICE = "selling_price";
    public static final String COL_M_STATUS = "status";
    public static final String COL_M_CUSTOMER_NAME = "customer_name";
    public static final String COL_M_CUSTOMER_PHONE = "customer_phone";
    public static final String COL_M_STOCK_IN_DATE = "stock_in_date";
    public static final String COL_M_STOCK_OUT_DATE = "stock_out_date";
    public static final String COL_M_NOTES = "notes";
    public static final String COL_M_RAW_OCR = "raw_ocr_text";

    // Inventory Logs Table
    public static final String TABLE_LOGS = "inventory_logs";
    public static final String COL_L_MOBILE_ID = "mobile_id";
    public static final String COL_L_ACTION = "action";
    public static final String COL_L_DETAILS = "details";
    public static final String COL_L_TIMESTAMP = "timestamp";

    public ScanHistoryDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_SCANS_TABLE = "CREATE TABLE " + TABLE_SCANS + " ("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_TEXT + " TEXT, "
                + COLUMN_SOURCE + " TEXT, "
                + COLUMN_TIMESTAMP + " TEXT, "
                + COLUMN_CHAR_COUNT + " INTEGER"
                + ")";
        db.execSQL(CREATE_SCANS_TABLE);

        createMobileTables(db);
    }

    private void createMobileTables(SQLiteDatabase db) {
        String CREATE_MOBILES_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_MOBILES + " ("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_M_BRAND + " TEXT, "
                + COL_M_MODEL + " TEXT, "
                + COL_M_MODEL_NO + " TEXT, "
                + COL_M_IMEI1 + " TEXT UNIQUE, "
                + COL_M_IMEI2 + " TEXT, "
                + COL_M_SERIAL + " TEXT, "
                + COL_M_RAM + " TEXT, "
                + COL_M_STORAGE + " TEXT, "
                + COL_M_COLOR + " TEXT, "
                + COL_M_CODE + " TEXT, "
                + COL_M_PURCHASE_PRICE + " REAL, "
                + COL_M_SELLING_PRICE + " REAL, "
                + COL_M_STATUS + " TEXT, "
                + COL_M_CUSTOMER_NAME + " TEXT, "
                + COL_M_CUSTOMER_PHONE + " TEXT, "
                + COL_M_STOCK_IN_DATE + " TEXT, "
                + COL_M_STOCK_OUT_DATE + " TEXT, "
                + COL_M_NOTES + " TEXT, "
                + COL_M_RAW_OCR + " TEXT"
                + ")";
        db.execSQL(CREATE_MOBILES_TABLE);

        String CREATE_LOGS_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_LOGS + " ("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_L_MOBILE_ID + " INTEGER, "
                + COL_L_ACTION + " TEXT, "
                + COL_L_DETAILS + " TEXT, "
                + COL_L_TIMESTAMP + " TEXT"
                + ")";
        db.execSQL(CREATE_LOGS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            createMobileTables(db);
        }
    }

    // --- SCANS OPERATIONS ---
    public long insertScan(String text, String source, String timestamp) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TEXT, text);
        values.put(COLUMN_SOURCE, source);
        values.put(COLUMN_TIMESTAMP, timestamp);
        values.put(COLUMN_CHAR_COUNT, text != null ? text.length() : 0);
        long id = db.insert(TABLE_SCANS, null, values);
        db.close();
        return id;
    }

    public List<ScanModel> getAllScans() {
        List<ScanModel> list = new ArrayList<ScanModel>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_SCANS, null, null, null, null, null, COLUMN_ID + " DESC");
        if (cursor != null && cursor.moveToFirst()) {
            do {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID));
                String text = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TEXT));
                String source = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SOURCE));
                String timestamp = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP));
                int charCount = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_CHAR_COUNT));
                list.add(new ScanModel(id, text, source, timestamp, charCount));
            } while (cursor.moveToNext());
            cursor.close();
        }
        db.close();
        return list;
    }

    public ScanModel getScanById(long id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_SCANS, null, COLUMN_ID + "=?", new String[]{String.valueOf(id)}, null, null, null);
        ScanModel scan = null;
        if (cursor != null && cursor.moveToFirst()) {
            String text = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TEXT));
            String source = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SOURCE));
            String timestamp = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP));
            int charCount = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_CHAR_COUNT));
            scan = new ScanModel(id, text, source, timestamp, charCount);
            cursor.close();
        }
        db.close();
        return scan;
    }

    public int deleteScan(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rows = db.delete(TABLE_SCANS, COLUMN_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
        return rows;
    }

    public void clearAllScans() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_SCANS, null, null);
        db.close();
    }

    public int getScanCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_SCANS, null);
        int count = 0;
        if (cursor != null && cursor.moveToFirst()) {
            count = cursor.getInt(0);
            cursor.close();
        }
        db.close();
        return count;
    }

    public void updateScanText(long id, String newText) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TEXT, newText);
        values.put(COLUMN_CHAR_COUNT, newText != null ? newText.length() : 0);
        db.update(TABLE_SCANS, values, COLUMN_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
    }

    // --- MOBILES INVENTORY OPERATIONS ---

    public MobileModel findMobileByIdentifier(String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }
        String trimmed = code.trim();
        SQLiteDatabase db = this.getReadableDatabase();

        // 1. Check ID if numeric
        if (trimmed.matches("\\d+")) {
            try {
                long id = Long.parseLong(trimmed);
                Cursor cId = db.query(TABLE_MOBILES, null, COLUMN_ID + "=?", new String[]{String.valueOf(id)}, null, null, null);
                if (cId != null && cId.moveToFirst()) {
                    MobileModel m = parseMobileFromCursor(cId);
                    cId.close();
                    db.close();
                    return m;
                }
                if (cId != null) cId.close();
            } catch (Exception ignored) {}
        }

        // 2. Search Product Code / SKU, Barcode, IMEI 1, IMEI 2, Serial Number
        String selection = COL_M_CODE + "=? OR " + COL_M_IMEI1 + "=? OR " + COL_M_IMEI2 + "=? OR " + COL_M_SERIAL + "=?";
        String[] selectionArgs = new String[]{trimmed, trimmed, trimmed, trimmed};

        Cursor cursor = db.query(TABLE_MOBILES, null, selection, selectionArgs, null, null, null);
        MobileModel mobile = null;
        if (cursor != null && cursor.moveToFirst()) {
            mobile = parseMobileFromCursor(cursor);
            cursor.close();
        }
        db.close();
        return mobile;
    }

    public boolean isImeiOrSerialExists(String imei1, String imei2, String serial, long excludeId) {
        SQLiteDatabase db = this.getReadableDatabase();
        List<String> args = new ArrayList<String>();
        StringBuilder query = new StringBuilder("SELECT COUNT(*) FROM " + TABLE_MOBILES + " WHERE ");

        List<String> conditions = new ArrayList<String>();
        if (imei1 != null && !imei1.trim().isEmpty()) {
            conditions.add("(" + COL_M_IMEI1 + "=? OR " + COL_M_IMEI2 + "=?)");
            args.add(imei1.trim());
            args.add(imei1.trim());
        }
        if (imei2 != null && !imei2.trim().isEmpty()) {
            conditions.add("(" + COL_M_IMEI1 + "=? OR " + COL_M_IMEI2 + "=?)");
            args.add(imei2.trim());
            args.add(imei2.trim());
        }
        if (serial != null && !serial.trim().isEmpty()) {
            conditions.add(COL_M_SERIAL + "=?");
            args.add(serial.trim());
        }

        if (conditions.isEmpty()) {
            db.close();
            return false;
        }

        query.append("(").append(android.text.TextUtils.join(" OR ", conditions)).append(")");
        if (excludeId > 0) {
            query.append(" AND ").append(COLUMN_ID).append("!=?");
            args.add(String.valueOf(excludeId));
        }

        String[] selectionArgs = args.toArray(new String[0]);
        Cursor cursor = db.rawQuery(query.toString(), selectionArgs);
        boolean exists = false;
        if (cursor != null && cursor.moveToFirst()) {
            exists = cursor.getInt(0) > 0;
            cursor.close();
        }
        db.close();
        return exists;
    }

    public long insertMobile(MobileModel mobile) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_M_BRAND, mobile.getBrand());
        values.put(COL_M_MODEL, mobile.getModel());
        values.put(COL_M_MODEL_NO, mobile.getModelNumber());
        values.put(COL_M_IMEI1, mobile.getImei1());
        values.put(COL_M_IMEI2, mobile.getImei2());
        values.put(COL_M_SERIAL, mobile.getSerialNumber());
        values.put(COL_M_RAM, mobile.getRam());
        values.put(COL_M_STORAGE, mobile.getStorage());
        values.put(COL_M_COLOR, mobile.getColor());
        values.put(COL_M_CODE, mobile.getProductCode());
        values.put(COL_M_PURCHASE_PRICE, mobile.getPurchasePrice());
        values.put(COL_M_SELLING_PRICE, mobile.getSellingPrice());
        values.put(COL_M_STATUS, mobile.getStatus());
        values.put(COL_M_CUSTOMER_NAME, mobile.getCustomerName());
        values.put(COL_M_CUSTOMER_PHONE, mobile.getCustomerPhone());
        values.put(COL_M_STOCK_IN_DATE, mobile.getStockInDate());
        values.put(COL_M_STOCK_OUT_DATE, mobile.getStockOutDate());
        values.put(COL_M_NOTES, mobile.getNotes());
        values.put(COL_M_RAW_OCR, mobile.getRawOcrText());

        long id = db.insert(TABLE_MOBILES, null, values);
        db.close();

        if (id > 0) {
            addInventoryLog(id, "STOCK_IN", "Added stock: " + mobile.getBrand() + " " + mobile.getModel() + " (IMEI: " + mobile.getImei1() + ")", mobile.getStockInDate());
        }
        return id;
    }

    public int updateMobile(MobileModel mobile, String timestamp) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_M_BRAND, mobile.getBrand());
        values.put(COL_M_MODEL, mobile.getModel());
        values.put(COL_M_MODEL_NO, mobile.getModelNumber());
        values.put(COL_M_IMEI1, mobile.getImei1());
        values.put(COL_M_IMEI2, mobile.getImei2());
        values.put(COL_M_SERIAL, mobile.getSerialNumber());
        values.put(COL_M_RAM, mobile.getRam());
        values.put(COL_M_STORAGE, mobile.getStorage());
        values.put(COL_M_COLOR, mobile.getColor());
        values.put(COL_M_CODE, mobile.getProductCode());
        values.put(COL_M_PURCHASE_PRICE, mobile.getPurchasePrice());
        values.put(COL_M_SELLING_PRICE, mobile.getSellingPrice());
        values.put(COL_M_STATUS, mobile.getStatus());
        values.put(COL_M_CUSTOMER_NAME, mobile.getCustomerName());
        values.put(COL_M_CUSTOMER_PHONE, mobile.getCustomerPhone());
        values.put(COL_M_STOCK_IN_DATE, mobile.getStockInDate());
        values.put(COL_M_STOCK_OUT_DATE, mobile.getStockOutDate());
        values.put(COL_M_NOTES, mobile.getNotes());
        values.put(COL_M_RAW_OCR, mobile.getRawOcrText());

        int rows = db.update(TABLE_MOBILES, values, COLUMN_ID + "=?", new String[]{String.valueOf(mobile.getId())});
        db.close();

        if (rows > 0) {
            addInventoryLog(mobile.getId(), "EDIT", "Updated details for: " + mobile.getBrand() + " " + mobile.getModel(), timestamp);
        }
        return rows;
    }

    public boolean markMobileAsSold(long id, double sellingPrice, String customerName, String customerPhone, String notes, String stockOutDate) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_M_STATUS, "SOLD");
        values.put(COL_M_SELLING_PRICE, sellingPrice);
        values.put(COL_M_CUSTOMER_NAME, customerName);
        values.put(COL_M_CUSTOMER_PHONE, customerPhone);
        values.put(COL_M_STOCK_OUT_DATE, stockOutDate);
        if (notes != null && !notes.isEmpty()) {
            values.put(COL_M_NOTES, notes);
        }

        int rows = db.update(TABLE_MOBILES, values, COLUMN_ID + "=?", new String[]{String.valueOf(id)});
        db.close();

        if (rows > 0) {
            addInventoryLog(id, "STOCK_OUT", "Sold mobile to " + (customerName.isEmpty() ? "Customer" : customerName) + " for $" + sellingPrice, stockOutDate);
            return true;
        }
        return false;
    }

    public int deleteMobile(long id, String timestamp) {
        MobileModel m = getMobileById(id);
        SQLiteDatabase db = this.getWritableDatabase();
        int rows = db.delete(TABLE_MOBILES, COLUMN_ID + "=?", new String[]{String.valueOf(id)});
        db.close();

        if (rows > 0 && m != null) {
            addInventoryLog(id, "DELETE", "Deleted record: " + m.getBrand() + " " + m.getModel() + " (IMEI: " + m.getImei1() + ")", timestamp);
        }
        return rows;
    }

    public MobileModel getMobileById(long id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_MOBILES, null, COLUMN_ID + "=?", new String[]{String.valueOf(id)}, null, null, null);
        MobileModel mobile = null;
        if (cursor != null && cursor.moveToFirst()) {
            mobile = parseMobileFromCursor(cursor);
            cursor.close();
        }
        db.close();
        return mobile;
    }

    public List<MobileModel> getAllMobiles(String statusFilter, String searchKey, String brandFilter) {
        List<MobileModel> list = new ArrayList<MobileModel>();
        SQLiteDatabase db = this.getReadableDatabase();

        List<String> conditions = new ArrayList<String>();
        List<String> selectionArgs = new ArrayList<String>();

        if (statusFilter != null && !statusFilter.equals("ALL")) {
            conditions.add(COL_M_STATUS + "=?");
            selectionArgs.add(statusFilter);
        }

        if (brandFilter != null && !brandFilter.equals("ALL")) {
            conditions.add(COL_M_BRAND + "=?");
            selectionArgs.add(brandFilter);
        }

        if (searchKey != null && !searchKey.trim().isEmpty()) {
            String q = "%" + searchKey.trim() + "%";
            conditions.add("(" + COL_M_BRAND + " LIKE ? OR " + COL_M_MODEL + " LIKE ? OR " + COL_M_IMEI1 + " LIKE ? OR " + COL_M_IMEI2 + " LIKE ? OR " + COL_M_SERIAL + " LIKE ? OR " + COL_M_CODE + " LIKE ?)");
            selectionArgs.add(q);
            selectionArgs.add(q);
            selectionArgs.add(q);
            selectionArgs.add(q);
            selectionArgs.add(q);
            selectionArgs.add(q);
        }

        String selection = conditions.isEmpty() ? null : android.text.TextUtils.join(" AND ", conditions);
        String[] args = selectionArgs.isEmpty() ? null : selectionArgs.toArray(new String[0]);

        Cursor cursor = db.query(TABLE_MOBILES, null, selection, args, null, null, COLUMN_ID + " DESC");
        if (cursor != null && cursor.moveToFirst()) {
            do {
                list.add(parseMobileFromCursor(cursor));
            } while (cursor.moveToNext());
            cursor.close();
        }
        db.close();
        return list;
    }

    private MobileModel parseMobileFromCursor(Cursor cursor) {
        long id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID));
        String brand = cursor.getString(cursor.getColumnIndexOrThrow(COL_M_BRAND));
        String model = cursor.getString(cursor.getColumnIndexOrThrow(COL_M_MODEL));
        String modelNo = cursor.getString(cursor.getColumnIndexOrThrow(COL_M_MODEL_NO));
        String imei1 = cursor.getString(cursor.getColumnIndexOrThrow(COL_M_IMEI1));
        String imei2 = cursor.getString(cursor.getColumnIndexOrThrow(COL_M_IMEI2));
        String serial = cursor.getString(cursor.getColumnIndexOrThrow(COL_M_SERIAL));
        String ram = cursor.getString(cursor.getColumnIndexOrThrow(COL_M_RAM));
        String storage = cursor.getString(cursor.getColumnIndexOrThrow(COL_M_STORAGE));
        String color = cursor.getString(cursor.getColumnIndexOrThrow(COL_M_COLOR));
        String code = cursor.getString(cursor.getColumnIndexOrThrow(COL_M_CODE));
        double purchase = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_M_PURCHASE_PRICE));
        double selling = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_M_SELLING_PRICE));
        String status = cursor.getString(cursor.getColumnIndexOrThrow(COL_M_STATUS));
        String custName = cursor.getString(cursor.getColumnIndexOrThrow(COL_M_CUSTOMER_NAME));
        String custPhone = cursor.getString(cursor.getColumnIndexOrThrow(COL_M_CUSTOMER_PHONE));
        String inDate = cursor.getString(cursor.getColumnIndexOrThrow(COL_M_STOCK_IN_DATE));
        String outDate = cursor.getString(cursor.getColumnIndexOrThrow(COL_M_STOCK_OUT_DATE));
        String notes = cursor.getString(cursor.getColumnIndexOrThrow(COL_M_NOTES));
        String rawOcr = cursor.getString(cursor.getColumnIndexOrThrow(COL_M_RAW_OCR));

        return new MobileModel(id, brand, model, modelNo, imei1, imei2, serial, ram, storage, color,
                code, purchase, selling, status, custName, custPhone, inDate, outDate, notes, rawOcr);
    }

    public int getMobileCount(String status) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT COUNT(*) FROM " + TABLE_MOBILES;
        String[] args = null;
        if (status != null && !status.equals("ALL")) {
            query += " WHERE " + COL_M_STATUS + "=?";
            args = new String[]{status};
        }
        Cursor cursor = db.rawQuery(query, args);
        int count = 0;
        if (cursor != null && cursor.moveToFirst()) {
            count = cursor.getInt(0);
            cursor.close();
        }
        db.close();
        return count;
    }

    // --- LOGS OPERATIONS ---

    public void addInventoryLog(long mobileId, String action, String details, String timestamp) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_L_MOBILE_ID, mobileId);
        values.put(COL_L_ACTION, action);
        values.put(COL_L_DETAILS, details);
        values.put(COL_L_TIMESTAMP, timestamp);
        db.insert(TABLE_LOGS, null, values);
        db.close();
    }

    public List<InventoryLogModel> getInventoryLogs(String actionFilter) {
        List<InventoryLogModel> list = new ArrayList<InventoryLogModel>();
        SQLiteDatabase db = this.getReadableDatabase();

        String selection = null;
        String[] args = null;
        if (actionFilter != null && !actionFilter.equals("ALL")) {
            selection = COL_L_ACTION + "=?";
            args = new String[]{actionFilter};
        }

        Cursor cursor = db.query(TABLE_LOGS, null, selection, args, null, null, COLUMN_ID + " DESC");
        if (cursor != null && cursor.moveToFirst()) {
            do {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID));
                long mId = cursor.getLong(cursor.getColumnIndexOrThrow(COL_L_MOBILE_ID));
                String act = cursor.getString(cursor.getColumnIndexOrThrow(COL_L_ACTION));
                String det = cursor.getString(cursor.getColumnIndexOrThrow(COL_L_DETAILS));
                String ts = cursor.getString(cursor.getColumnIndexOrThrow(COL_L_TIMESTAMP));
                list.add(new InventoryLogModel(id, mId, act, det, ts));
            } while (cursor.moveToNext());
            cursor.close();
        }
        db.close();
        return list;
    }

    public void clearInventoryLogs() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_LOGS, null, null);
        db.close();
    }
}