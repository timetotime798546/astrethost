package com.mumbaimedical.app;

import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class AddEditMedicineActivity extends Activity {

    private DatabaseHelper dbHelper;
    private TextView tvTitle;
    private EditText etName, etCompany, etCategory, etBatch, etPurchasePrice, etSellingPrice, etQuantity, etMinStock, etMfgDate, etExpDate, etRack, etNotes;
    private Button btnSave;
    private long medicineId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_medicine);

        dbHelper = new DatabaseHelper(this);

        tvTitle = findViewById(R.id.tvTitle);
        etName = findViewById(R.id.etName);
        etCompany = findViewById(R.id.etCompany);
        etCategory = findViewById(R.id.etCategory);
        etBatch = findViewById(R.id.etBatch);
        etPurchasePrice = findViewById(R.id.etPurchasePrice);
        etSellingPrice = findViewById(R.id.etSellingPrice);
        etQuantity = findViewById(R.id.etQuantity);
        etMinStock = findViewById(R.id.etMinStock);
        etMfgDate = findViewById(R.id.etMfgDate);
        etExpDate = findViewById(R.id.etExpDate);
        etRack = findViewById(R.id.etRack);
        etNotes = findViewById(R.id.etNotes);
        btnSave = findViewById(R.id.btnSave);

        if (getIntent().hasExtra("medicine_id")) {
            medicineId = getIntent().getLongExtra("medicine_id", -1);
            tvTitle.setText("Edit Medicine");
            loadMedicineDetails();
        }

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveMedicine();
            }
        });
    }

    private void loadMedicineDetails() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + DatabaseHelper.TABLE_MEDICINES + " WHERE id = ?", new String[]{String.valueOf(medicineId)});
        if (cursor.moveToFirst()) {
            etName.setText(cursor.getString(cursor.getColumnIndexOrThrow("name")));
            etCompany.setText(cursor.getString(cursor.getColumnIndexOrThrow("company")));
            etCategory.setText(cursor.getString(cursor.getColumnIndexOrThrow("category")));
            etBatch.setText(cursor.getString(cursor.getColumnIndexOrThrow("batch_number")));
            etPurchasePrice.setText(String.valueOf(cursor.getDouble(cursor.getColumnIndexOrThrow("purchase_price"))));
            etSellingPrice.setText(String.valueOf(cursor.getDouble(cursor.getColumnIndexOrThrow("selling_price"))));
            etQuantity.setText(String.valueOf(cursor.getInt(cursor.getColumnIndexOrThrow("quantity"))));
            etMinStock.setText(String.valueOf(cursor.getInt(cursor.getColumnIndexOrThrow("min_stock"))));
            etMfgDate.setText(cursor.getString(cursor.getColumnIndexOrThrow("mfg_date")));
            etExpDate.setText(cursor.getString(cursor.getColumnIndexOrThrow("exp_date")));
            etRack.setText(cursor.getString(cursor.getColumnIndexOrThrow("rack_number")));
            etNotes.setText(cursor.getString(cursor.getColumnIndexOrThrow("notes")));
        }
        cursor.close();
    }

    private void saveMedicine() {
        String name = etName.getText().toString().trim();
        String company = etCompany.getText().toString().trim();
        String category = etCategory.getText().toString().trim();
        String batch = etBatch.getText().toString().trim();
        String pPriceStr = etPurchasePrice.getText().toString().trim();
        String sPriceStr = etSellingPrice.getText().toString().trim();
        String qtyStr = etQuantity.getText().toString().trim();
        String minStockStr = etMinStock.getText().toString().trim();
        String mfgDate = etMfgDate.getText().toString().trim();
        String expDate = etExpDate.getText().toString().trim();
        String rack = etRack.getText().toString().trim();
        String notes = etNotes.getText().toString().trim();

        if (name.isEmpty() || batch.isEmpty() || sPriceStr.isEmpty() || qtyStr.isEmpty() || expDate.isEmpty()) {
            Toast.makeText(this, "Please fill required fields (Name, Batch, Price, Qty, Expiry)", Toast.LENGTH_SHORT).show();
            return;
        }

        double purchasePrice = pPriceStr.isEmpty() ? 0.0 : Double.parseDouble(pPriceStr);
        double sellingPrice = Double.parseDouble(sPriceStr);
        int quantity = Integer.parseInt(qtyStr);
        int minStock = minStockStr.isEmpty() ? 10 : Integer.parseInt(minStockStr);

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("company", company);
        cv.put("category", category);
        cv.put("batch_number", batch);
        cv.put("purchase_price", purchasePrice);
        cv.put("selling_price", sellingPrice);
        cv.put("quantity", quantity);
        cv.put("min_stock", minStock);
        cv.put("mfg_date", mfgDate);
        cv.put("exp_date", expDate);
        cv.put("rack_number", rack);
        cv.put("notes", notes);

        if (medicineId == -1) {
            db.insert(DatabaseHelper.TABLE_MEDICINES, null, cv);
            Toast.makeText(this, "Medicine Added", Toast.LENGTH_SHORT).show();
        } else {
            db.update(DatabaseHelper.TABLE_MEDICINES, cv, "id = ?", new String[]{String.valueOf(medicineId)});
            Toast.makeText(this, "Medicine Updated", Toast.LENGTH_SHORT).show();
        }
        finish();
    }
}