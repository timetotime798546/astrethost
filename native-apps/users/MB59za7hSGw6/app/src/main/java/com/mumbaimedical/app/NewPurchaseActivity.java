package com.mumbaimedical.app;

import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NewPurchaseActivity extends Activity {

    private DatabaseHelper dbHelper;
    private EditText etSupplierName, etPurQty, etPurRate, etPurPaidAmount;
    private Spinner spPurMedicines;
    private Button btnSavePurchase;

    private List<Long> medicineIds = new ArrayList<>();
    private List<String> medicineNames = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_purchase);

        dbHelper = new DatabaseHelper(this);

        etSupplierName = findViewById(R.id.etSupplierName);
        spPurMedicines = findViewById(R.id.spPurMedicines);
        etPurQty = findViewById(R.id.etPurQty);
        etPurRate = findViewById(R.id.etPurRate);
        etPurPaidAmount = findViewById(R.id.etPurPaidAmount);
        btnSavePurchase = findViewById(R.id.btnSavePurchase);

        loadMedicineSpinner();

        btnSavePurchase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                savePurchase();
            }
        });
    }

    private void loadMedicineSpinner() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id, name FROM " + DatabaseHelper.TABLE_MEDICINES + " ORDER BY name ASC", null);

        medicineIds.clear();
        medicineNames.clear();

        while (cursor.moveToNext()) {
            medicineIds.add(cursor.getLong(0));
            medicineNames.add(cursor.getString(1));
        }
        cursor.close();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, medicineNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spPurMedicines.setAdapter(adapter);
    }

    private void savePurchase() {
        String supplier = etSupplierName.getText().toString().trim();
        int selectedIndex = spPurMedicines.getSelectedItemPosition();
        String qtyStr = etPurQty.getText().toString().trim();
        String rateStr = etPurRate.getText().toString().trim();
        String paidStr = etPurPaidAmount.getText().toString().trim();

        if (supplier.isEmpty() || selectedIndex < 0 || qtyStr.isEmpty() || rateStr.isEmpty()) {
            Toast.makeText(this, "Please fill required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        int qty = Integer.parseInt(qtyStr);
        double rate = Double.parseDouble(rateStr);
        double totalAmount = qty * rate;
        double paidAmount = paidStr.isEmpty() ? totalAmount : Double.parseDouble(paidStr);
        long medicineId = medicineIds.get(selectedIndex);

        String date = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues cv = new ContentValues();
            cv.put("purchase_date", date);
            cv.put("supplier_name", supplier);
            cv.put("total_amount", totalAmount);
            cv.put("paid_amount", paidAmount);
            cv.put("payment_status", paidAmount >= totalAmount ? "Paid" : "Pending");

            db.insert(DatabaseHelper.TABLE_PURCHASES, null, cv);

            // Increase stock
            db.execSQL("UPDATE " + DatabaseHelper.TABLE_MEDICINES + " SET quantity = quantity + " + qty + ", purchase_price = " + rate + " WHERE id = " + medicineId);

            db.setTransactionSuccessful();
            Toast.makeText(this, "Purchase Saved & Stock Updated!", Toast.LENGTH_SHORT).show();
            finish();

        } finally {
            db.endTransaction();
        }
    }
}