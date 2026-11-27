package com.boxstockmanager.app;

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

    private static final String DATABASE_NAME = "box_inventory.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_INVENTORY = "inventory";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_QR_CODE = "qr_code";
    public static final String COLUMN_QUANTITY = "quantity";
    public static final String COLUMN_LAST_UPDATED = "last_updated";

    private static final String TABLE_CREATE =
            "CREATE TABLE " + TABLE_INVENTORY + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_QR_CODE + " TEXT UNIQUE NOT NULL, " +
                    COLUMN_QUANTITY + " INTEGER NOT NULL, " +
                    COLUMN_LAST_UPDATED + " TEXT NOT NULL" +
                    ");";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_INVENTORY);
        onCreate(db);
    }

    public int updateStock(String qrCode, boolean isStockIn) {
        SQLiteDatabase db = this.getWritableDatabase();
        String currentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        Cursor cursor = db.query(
                TABLE_INVENTORY,
                new String[]{COLUMN_ID, COLUMN_QUANTITY},
                COLUMN_QR_CODE + "=?",
                new String[]{qrCode},
                null, null, null
        );

        int newQuantity = 0;
        try {
            if (cursor != null && cursor.moveToFirst()) {
                int currentQty = cursor.getInt(cursor.getColumnIndex(COLUMN_QUANTITY));
                if (isStockIn) {
                    newQuantity = currentQty + 1;
                } else {
                    newQuantity = Math.max(0, currentQty - 1);
                }

                ContentValues values = new ContentValues();
                values.put(COLUMN_QUANTITY, newQuantity);
                values.put(COLUMN_LAST_UPDATED, currentTime);

                db.update(TABLE_INVENTORY, values, COLUMN_QR_CODE + "=?", new String[]{qrCode});
            } else {
                if (isStockIn) {
                    newQuantity = 1;
                } else {
                    newQuantity = 0;
                }

                ContentValues values = new ContentValues();
                values.put(COLUMN_QR_CODE, qrCode);
                values.put(COLUMN_QUANTITY, newQuantity);
                values.put(COLUMN_LAST_UPDATED, currentTime);

                db.insert(TABLE_INVENTORY, null, values);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return newQuantity;
    }

    public List<BoxItem> getAllInventory() {
        List<BoxItem> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(
                TABLE_INVENTORY,
                new String[]{COLUMN_ID, COLUMN_QR_CODE, COLUMN_QUANTITY, COLUMN_LAST_UPDATED},
                null, null, null, null, COLUMN_LAST_UPDATED + " DESC"
        );

        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    long id = cursor.getLong(cursor.getColumnIndex(COLUMN_ID));
                    String code = cursor.getString(cursor.getColumnIndex(COLUMN_QR_CODE));
                    int qty = cursor.getInt(cursor.getColumnIndex(COLUMN_QUANTITY));
                    String updated = cursor.getString(cursor.getColumnIndex(COLUMN_LAST_UPDATED));

                    list.add(new BoxItem(id, code, qty, updated));
                }
            } finally {
                cursor.close();
            }
        }

        return list;
    }
}