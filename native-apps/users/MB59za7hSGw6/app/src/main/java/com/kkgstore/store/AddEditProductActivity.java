package com.kkgstore.store;

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
import android.widget.TextView;
import android.widget.Toast;

public class AddEditProductActivity extends Activity {

    private long productId = 0;
    private DatabaseHelper dbHelper;

    private TextView tvFormTitle;
    private EditText etName, etCategory, etCostPrice, etSellPrice, etStock;
    private Spinner spUnit;
    private Button btnSave, btnDelete;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (DemoLockManager.checkAndEnforceLock(this)) return;

        setContentView(R.layout.activity_add_edit_product);
        dbHelper = new DatabaseHelper(this);

        productId = getIntent().getLongExtra("product_id", 0);

        tvFormTitle = (TextView) findViewById(R.id.tv_form_title);
        etName = (EditText) findViewById(R.id.et_product_name);
        etCategory = (EditText) findViewById(R.id.et_product_category);
        spUnit = (Spinner) findViewById(R.id.sp_product_unit);
        etCostPrice = (EditText) findViewById(R.id.et_product_cost);
        etSellPrice = (EditText) findViewById(R.id.et_product_sell_price);
        etStock = (EditText) findViewById(R.id.et_product_stock);
        btnSave = (Button) findViewById(R.id.btn_save_product);
        btnDelete = (Button) findViewById(R.id.btn_delete_product);

        ArrayAdapter<String> unitAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new String[]{"KG (किलो)", "Piece (पीस)", "Packet (पैकेट)", "Litre (लीटर)", "Gram (ग्राम)"});
        unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spUnit.setAdapter(unitAdapter);

        if (productId > 0) {
            tvFormTitle.setText("Edit Grocery Product");
            btnDelete.setVisibility(View.VISIBLE);
            loadProductDetails();
        }

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { saveProduct(); }
        });

        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { deleteProduct(); }
        });
    }

    private void loadProductDetails() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT name, category, unit, cost_price, sell_price, stock FROM products WHERE id=" + productId, null);
        if (c.moveToFirst()) {
            etName.setText(c.getString(0));
            etCategory.setText(c.getString(1));
            etCostPrice.setText(String.valueOf(c.getDouble(3)));
            etSellPrice.setText(String.valueOf(c.getDouble(4)));
            etStock.setText(String.valueOf(c.getDouble(5)));
        }
        c.close();
    }

    private void saveProduct() {
        String name = etName.getText().toString().trim();
        String cat = etCategory.getText().toString().trim();
        String unit = spUnit.getSelectedItem().toString();
        String costStr = etCostPrice.getText().toString().trim();
        String sellStr = etSellPrice.getText().toString().trim();
        String stockStr = etStock.getText().toString().trim();

        if (name.isEmpty() || sellStr.isEmpty()) {
            Toast.makeText(this, "नाम और बिक्री मूल्य दर्ज करना अनिवार्य है", Toast.LENGTH_SHORT).show();
            return;
        }

        double cost = costStr.isEmpty() ? 0 : Double.parseDouble(costStr);
        double sell = Double.parseDouble(sellStr);
        double stock = stockStr.isEmpty() ? 0 : Double.parseDouble(stockStr);

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("category", cat);
        cv.put("unit", unit);
        cv.put("cost_price", cost);
        cv.put("sell_price", sell);
        cv.put("stock", stock);

        if (productId > 0) {
            db.update("products", cv, "id=?", new String[]{String.valueOf(productId)});
            Toast.makeText(this, "प्रोडक्ट अपडेट हो गया", Toast.LENGTH_SHORT).show();
        } else {
            db.insert("products", null, cv);
            Toast.makeText(this, "नया प्रोडक्ट जोड़ा गया", Toast.LENGTH_SHORT).show();
        }
        finish();
    }

    private void deleteProduct() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("products", "id=?", new String[]{String.valueOf(productId)});
        Toast.makeText(this, "प्रोडक्ट हटा दिया गया", Toast.LENGTH_SHORT).show();
        finish();
    }
}