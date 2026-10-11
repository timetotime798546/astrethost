package com.productimagemanager.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class ProductDbHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "products.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_PRODUCTS = "products";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_PRICE = "price";
    public static final String COLUMN_DESCRIPTION = "description";
    public static final String COLUMN_IMAGE_ID = "image_id";
    public static final String COLUMN_CREATED_AT = "created_at";

    private static final String SQL_CREATE_TABLE =
            "CREATE TABLE " + TABLE_PRODUCTS + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_NAME + " TEXT NOT NULL, " +
                    COLUMN_PRICE + " REAL NOT NULL, " +
                    COLUMN_DESCRIPTION + " TEXT, " +
                    COLUMN_IMAGE_ID + " TEXT, " +
                    COLUMN_CREATED_AT + " INTEGER NOT NULL" +
                    ");";

    public ProductDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PRODUCTS);
        onCreate(db);
    }

    public long insertProduct(Product product) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, product.getName());
        values.put(COLUMN_PRICE, product.getPrice());
        values.put(COLUMN_DESCRIPTION, product.getDescription());
        values.put(COLUMN_IMAGE_ID, product.getImageId());
        values.put(COLUMN_CREATED_AT, System.currentTimeMillis());
        return db.insert(TABLE_PRODUCTS, null, values);
    }

    public int updateProduct(Product product) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, product.getName());
        values.put(COLUMN_PRICE, product.getPrice());
        values.put(COLUMN_DESCRIPTION, product.getDescription());
        values.put(COLUMN_IMAGE_ID, product.getImageId());
        return db.update(TABLE_PRODUCTS, values, COLUMN_ID + " = ?", new String[]{String.valueOf(product.getId())});
    }

    public int deleteProduct(long id) {
        SQLiteDatabase db = getWritableDatabase();
        return db.delete(TABLE_PRODUCTS, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
    }

    public Product getProductById(long id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_PRODUCTS, null, COLUMN_ID + " = ?", new String[]{String.valueOf(id)}, null, null, null);
        Product product = null;
        if (cursor != null && cursor.moveToFirst()) {
            product = cursorToProduct(cursor);
            cursor.close();
        }
        return product;
    }

    public List<Product> getAllProducts() {
        List<Product> list = new ArrayList<Product>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_PRODUCTS, null, null, null, null, null, COLUMN_ID + " DESC");
        if (cursor != null) {
            while (cursor.moveToNext()) {
                list.add(cursorToProduct(cursor));
            }
            cursor.close();
        }
        return list;
    }

    private Product cursorToProduct(Cursor cursor) {
        Product p = new Product();
        p.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)));
        p.setName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)));
        p.setPrice(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_PRICE)));
        p.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION)));
        p.setImageId(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMAGE_ID)));
        p.setCreatedAt(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT)));
        return p;
    }
}