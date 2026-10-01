package com.dailybizmanager.app;

import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class PurchasesActivity extends Activity {

    private DatabaseHelper dbHelper;
    private Spinner spnRestockProducts;
    private EditText edtSupplier, edtPurchaseCost, edtPurchaseQty;
    private LinearLayout layoutPurchaseHistory;
    private ArrayList<ProductsActivity.Product> products;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_purchases);

        dbHelper = new DatabaseHelper(this);

        spnRestockProducts = (Spinner) findViewById(R.id.spnRestockProducts);
        edtSupplier = (EditText) findViewById(R.id.edtSupplier);
        edtPurchaseCost = (EditText) findViewById(R.id.edtPurchaseCost);
        edtPurchaseQty = (EditText) findViewById(R.id.edtPurchaseQty);
        layoutPurchaseHistory = (LinearLayout) findViewById(R.id.layoutPurchaseHistory);
        Button btnSavePurchase = (Button) findViewById(R.id.btnSavePurchase);

        products = new ArrayList<>();

        loadProducts();
        renderPurchaseHistory();

        btnSavePurchase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveRestockPurch();
            }
        });
    }

    private void loadProducts() {
        products.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query("products", null, null, null, null, null, "name ASC");

        ArrayList<String> names = new ArrayList<>();
        while (cursor.moveToNext()) {
            ProductsActivity.Product p = new ProductsActivity.Product();
            p.id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
            p.name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
            p.purchasePrice = cursor.getDouble(cursor.getColumnIndexOrThrow("purchase_price"));
            p.stock = cursor.getInt(cursor.getColumnIndexOrThrow("stock"));
            products.add(p);
            names.add(p.name + " (current stock: " + p.stock + ")");
        }
        cursor.close();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnRestockProducts.setAdapter(adapter);
    }

    private void saveRestockPurch() {
        if (products.isEmpty()) {
            Toast.makeText(this, "No products registered yet", Toast.LENGTH_SHORT).show();
            return;
        }

        int selIdx = spnRestockProducts.getSelectedItemPosition();
        if (selIdx == -1) return;
        ProductsActivity.Product p = products.get(selIdx);

        String supplier = edtSupplier.getText().toString().trim();
        String costStr = edtPurchaseCost.getText().toString().trim();
        String qtyStr = edtPurchaseQty.getText().toString().trim();

        if (supplier.isEmpty() || costStr.isEmpty() || qtyStr.isEmpty()) {
            Toast.makeText(this, "Complete all field values", Toast.LENGTH_SHORT).show();
            return;
        }

        double cost = Double.parseDouble(costStr);
        int qty = Integer.parseInt(qtyStr);

        if (cost < 0 || qty <= 0) {
            Toast.makeText(this, "Invalid quantity or cost", Toast.LENGTH_SHORT).show();
            return;
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

            ContentValues vals = new ContentValues();
            vals.put("product_id", p.id);
            vals.put("product_name", p.name);
            vals.put("quantity", qty);
            vals.put("purchase_price", cost);
            vals.put("supplier", supplier);
            vals.put("date", today);

            db.insert("purchases", null, vals);

            // Increment local database stocks
            db.execSQL("UPDATE products SET stock = stock + ?, purchase_price = ? WHERE id = ?", new Object[]{qty, cost, p.id});

            db.setTransactionSuccessful();
            Toast.makeText(this, "Restocked inventory records", Toast.LENGTH_SHORT).show();

            // Clear inputs
            edtSupplier.setText("");
            edtPurchaseCost.setText("");
            edtPurchaseQty.setText("");

            loadProducts();
            renderPurchaseHistory();

        } catch (Exception e) {
            Toast.makeText(this, "Stock storage update failed", Toast.LENGTH_SHORT).show();
        } finally {
            db.endTransaction();
        }
    }

    private void renderPurchaseHistory() {
        layoutPurchaseHistory.removeAllViews();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM purchases ORDER BY id DESC LIMIT 20", null);

        while (cursor.moveToNext()) {
            String name = cursor.getString(cursor.getColumnIndexOrThrow("product_name"));
            int qty = cursor.getInt(cursor.getColumnIndexOrThrow("quantity"));
            double price = cursor.getDouble(cursor.getColumnIndexOrThrow("purchase_price"));
            String supplier = cursor.getString(cursor.getColumnIndexOrThrow("supplier"));
            String date = cursor.getString(cursor.getColumnIndexOrThrow("date"));

            TextView row = new TextView(this);
            row.setText(date + ": " + name + " (Qty: " + qty + ") from " + supplier + " @ $" + String.format("%.2f", price));
            row.setPadding(8, 8, 8, 8);
            row.setBackgroundColor(Color.WHITE);
            row.setTextSize(13);

            View divider = new View(this);
            divider.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1));
            divider.setBackgroundColor(Color.LTGRAY);

            layoutPurchaseHistory.addView(row);
            layoutPurchaseHistory.addView(divider);
        }
        cursor.close();
    }
}