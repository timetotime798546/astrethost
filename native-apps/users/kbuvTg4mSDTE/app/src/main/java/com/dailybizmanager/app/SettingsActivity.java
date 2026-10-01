package com.dailybizmanager.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class SettingsActivity extends Activity {

    private DatabaseHelper dbHelper;
    private EditText edtSettingsShopName, edtSettingsShopPhone, edtSettingsShopAddress, edtSettingsCurrency, edtSettingsTax;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        dbHelper = new DatabaseHelper(this);

        edtSettingsShopName = (EditText) findViewById(R.id.edtSettingsShopName);
        edtSettingsShopPhone = (EditText) findViewById(R.id.edtSettingsShopPhone);
        edtSettingsShopAddress = (EditText) findViewById(R.id.edtSettingsShopAddress);
        edtSettingsCurrency = (EditText) findViewById(R.id.edtSettingsCurrency);
        edtSettingsTax = (EditText) findViewById(R.id.edtSettingsTax);

        Button btnSaveSettings = (Button) findViewById(R.id.btnSaveSettings);
        Button btnLoadSampleData = (Button) findViewById(R.id.btnLoadSampleData);
        Button btnClearAll = (Button) findViewById(R.id.btnClearAll);

        loadSystemSettings();

        btnSaveSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveSystemSettings();
            }
        });

        btnLoadSampleData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmDemoLoad();
            }
        });

        btnClearAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmHardReset();
            }
        });
    }

    private void loadSystemSettings() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM settings LIMIT 1", null);
        if (cursor.moveToFirst()) {
            edtSettingsShopName.setText(cursor.getString(cursor.getColumnIndexOrThrow("shop_name")));
            edtSettingsShopPhone.setText(cursor.getString(cursor.getColumnIndexOrThrow("shop_phone")));
            edtSettingsShopAddress.setText(cursor.getString(cursor.getColumnIndexOrThrow("shop_address")));
            edtSettingsCurrency.setText(cursor.getString(cursor.getColumnIndexOrThrow("currency")));
            edtSettingsTax.setText(String.valueOf(cursor.getDouble(cursor.getColumnIndexOrThrow("tax"))));
        }
        cursor.close();
    }

    private void saveSystemSettings() {
        String name = edtSettingsShopName.getText().toString().trim();
        String phone = edtSettingsShopPhone.getText().toString().trim();
        String address = edtSettingsShopAddress.getText().toString().trim();
        String curr = edtSettingsCurrency.getText().toString().trim();
        String taxStr = edtSettingsTax.getText().toString().trim();

        if (name.isEmpty() || curr.isEmpty() || taxStr.isEmpty()) {
            Toast.makeText(this, "Shop name, currency and tax are required", Toast.LENGTH_SHORT).show();
            return;
        }

        double tax = Double.parseDouble(taxStr);

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("shop_name", name);
        values.put("shop_phone", phone);
        values.put("shop_address", address);
        values.put("currency", curr);
        values.put("tax", tax);

        db.update("settings", values, "id = 1", null);
        Toast.makeText(this, "Preferences Saved!", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void confirmDemoLoad() {
        new AlertDialog.Builder(this)
                .setTitle("Load Demonstration Dataset")
                .setMessage("This will inject initial products, customers, sales ledger entries, and expenses to easily test the features. Continue?")
                .setPositiveButton("Inject Demo Records", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        loadDemoDataset();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void loadDemoDataset() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            // Check if products exist to prevent duplication
            Cursor c = db.rawQuery("SELECT COUNT(*) FROM products", null);
            c.moveToFirst();
            if (c.getInt(0) > 0) {
                Toast.makeText(this, "Demo injection skipped. Products table is already populated.", Toast.LENGTH_LONG).show();
                c.close();
                db.endTransaction();
                return;
            }
            c.close();

            // Insert 5 Products
            db.execSQL("INSERT INTO products (name, sku, category, purchase_price, selling_price, stock, min_stock, unit) VALUES " +
                    "('Instant Premium Coffee', 'SKU-COFF-01', 'Beverages', 4.50, 8.00, 50, 10, 'jar')," +
                    "('Wholegrain Cereal Oats', 'SKU-OAT-02', 'Groceries', 2.20, 4.50, 30, 5, 'box')," +
                    "('Organic Sweet Honey', 'SKU-HON-03', 'Groceries', 6.00, 12.00, 15, 3, 'pcs')," +
                    "('Scented Body Soap', 'SKU-SOAP-04', 'Personal Care', 1.10, 2.50, 80, 20, 'pcs')," +
                    "('Whole Milk 1L Carton', 'SKU-MILK-05', 'Beverages', 0.90, 1.80, 2, 8, 'box')");

            // Insert 3 Customers
            db.execSQL("INSERT INTO customers (name, phone, address, total_purchases, amount_paid, balance) VALUES " +
                    "('John Doe', '555-0199', 'Elm Street Plaza', 100.0, 80.0, 20.0)," +
                    "('Alice Smith', '555-0245', 'Garden State Ave', 250.0, 250.0, 0.0)," +
                    "('Bob Johnson', '555-3321', 'Industrial Drive', 0.0, 0.0, 0.0)");

            // Insert Sample Expenses
            db.execSQL("INSERT INTO expenses (title, category, amount, date, notes) VALUES " +
                    "('Weekly Shop Rent', 'Rent & Space', 150.00, '2024-01-10', 'Payment to landlord')," +
                    "('Broadband Internet', 'Utilities', 45.00, '2024-01-12', 'Monthly connection bill')");

            db.setTransactionSuccessful();
            Toast.makeText(this, "Offline demo records populated successfully!", Toast.LENGTH_SHORT).show();
            finish();

        } catch (Exception e) {
            Toast.makeText(this, "Failed to load demographic dataset.", Toast.LENGTH_SHORT).show();
        } finally {
            db.endTransaction();
        }
    }

    private void confirmHardReset() {
        new AlertDialog.Builder(this)
                .setTitle("Hard Factory Reset")
                .setMessage("Are you absolutely sure you want to clean and clear all database tables? This action is irreversible.")
                .setPositiveButton("Reset Database", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        clearAllData();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void clearAllData() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        dbHelper.onUpgrade(db, 1, 1);
        Toast.makeText(this, "Database cleared! Application restarted with initial configuration.", Toast.LENGTH_LONG).show();
        finish();
    }
}