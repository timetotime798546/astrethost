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
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AddPurchaseActivity extends Activity {

    private DatabaseHelper dbHelper;
    private EditText etSupplier, etQty, etAmount;
    private Spinner spProduct;
    private List<ProductItem> productList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (DemoLockManager.checkAndEnforceLock(this)) return;

        setContentView(R.layout.activity_add_purchase);
        dbHelper = new DatabaseHelper(this);

        etSupplier = (EditText) findViewById(R.id.et_purchase_supplier);
        spProduct = (Spinner) findViewById(R.id.sp_purchase_product);
        etQty = (EditText) findViewById(R.id.et_purchase_qty);
        etAmount = (EditText) findViewById(R.id.et_purchase_amount);
        Button btnSave = (Button) findViewById(R.id.btn_save_purchase);

        loadProductsSpinner();

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { savePurchase(); }
        });
    }

    private void loadProductsSpinner() {
        productList.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT id, name, unit FROM products ORDER BY name ASC", null);
        while (c.moveToNext()) {
            productList.add(new ProductItem(c.getLong(0), c.getString(1), c.getString(2)));
        }
        c.close();

        ArrayAdapter<ProductItem> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, productList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spProduct.setAdapter(adapter);
    }

    private void savePurchase() {
        if (spProduct.getSelectedItem() == null) {
            Toast.makeText(this, "कृपया पहले प्रोडक्ट चुनें", Toast.LENGTH_SHORT).show();
            return;
        }

        ProductItem product = (ProductItem) spProduct.getSelectedItem();
        String supplier = etSupplier.getText().toString().trim();
        String qtyStr = etQty.getText().toString().trim();
        String amtStr = etAmount.getText().toString().trim();

        if (qtyStr.isEmpty() || amtStr.isEmpty()) {
            Toast.makeText(this, "मात्रा और लागत दर्ज करें", Toast.LENGTH_SHORT).show();
            return;
        }

        double qty = Double.parseDouble(qtyStr);
        double amt = Double.parseDouble(amtStr);
        String date = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("supplier", supplier);
        cv.put("product_id", product.id);
        cv.put("quantity", qty);
        cv.put("amount", amt);
        cv.put("purchase_date", date);

        db.insert("purchases", null, cv);
        db.execSQL("UPDATE products SET stock = stock + " + qty + " WHERE id = " + product.id);

        Toast.makeText(this, "स्टॉक खरीदारी दर्ज हो गई", Toast.LENGTH_SHORT).show();
        finish();
    }

    private static class ProductItem {
        long id; String name, unit;
        ProductItem(long id, String name, String unit) { this.id = id; this.name = name; this.unit = unit; }
        @Override public String toString() { return name + " (" + unit + ")"; }
    }
}