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
import android.widget.TextView;
import android.widget.Toast;

public class AddProductActivity extends Activity {

    private DatabaseHelper dbHelper;
    private EditText edtProdName, edtProdSku, edtProdCategory, edtProdPurchasePrice, edtProdSellingPrice, edtProdStock, edtProdMinStock, edtProdUnit;
    private Button btnSaveProduct, btnDeleteProduct;
    private int editProductId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_product);

        dbHelper = new DatabaseHelper(this);

        TextView txtFormTitle = (TextView) findViewById(R.id.txtFormTitle);
        edtProdName = (EditText) findViewById(R.id.edtProdName);
        edtProdSku = (EditText) findViewById(R.id.edtProdSku);
        edtProdCategory = (EditText) findViewById(R.id.edtProdCategory);
        edtProdPurchasePrice = (EditText) findViewById(R.id.edtProdPurchasePrice);
        edtProdSellingPrice = (EditText) findViewById(R.id.edtProdSellingPrice);
        edtProdStock = (EditText) findViewById(R.id.edtProdStock);
        edtProdMinStock = (EditText) findViewById(R.id.edtProdMinStock);
        edtProdUnit = (EditText) findViewById(R.id.edtProdUnit);

        btnSaveProduct = (Button) findViewById(R.id.btnSaveProduct);
        Button btnCancel = (Button) findViewById(R.id.btnCancel);
        btnDeleteProduct = (Button) findViewById(R.id.btnDeleteProduct);

        if (getIntent().hasExtra("product_id")) {
            editProductId = getIntent().getIntExtra("product_id", -1);
            txtFormTitle.setText("Edit Product Details");
            btnDeleteProduct.setVisibility(View.VISIBLE);
            loadProductDetails();
        }

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnSaveProduct.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveProduct();
            }
        });

        btnDeleteProduct.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmDelete();
            }
        });
    }

    private void loadProductDetails() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM products WHERE id = ?", new String[]{String.valueOf(editProductId)});
        if (cursor.moveToFirst()) {
            edtProdName.setText(cursor.getString(cursor.getColumnIndexOrThrow("name")));
            edtProdSku.setText(cursor.getString(cursor.getColumnIndexOrThrow("sku")));
            edtProdCategory.setText(cursor.getString(cursor.getColumnIndexOrThrow("category")));
            edtProdPurchasePrice.setText(String.valueOf(cursor.getDouble(cursor.getColumnIndexOrThrow("purchase_price"))));
            edtProdSellingPrice.setText(String.valueOf(cursor.getDouble(cursor.getColumnIndexOrThrow("selling_price"))));
            edtProdStock.setText(String.valueOf(cursor.getInt(cursor.getColumnIndexOrThrow("stock"))));
            edtProdMinStock.setText(String.valueOf(cursor.getInt(cursor.getColumnIndexOrThrow("min_stock"))));
            edtProdUnit.setText(cursor.getString(cursor.getColumnIndexOrThrow("unit")));
        }
        cursor.close();
    }

    private void saveProduct() {
        String name = edtProdName.getText().toString().trim();
        String sku = edtProdSku.getText().toString().trim();
        String category = edtProdCategory.getText().toString().trim();
        String pPriceStr = edtProdPurchasePrice.getText().toString().trim();
        String sPriceStr = edtProdSellingPrice.getText().toString().trim();
        String stockStr = edtProdStock.getText().toString().trim();
        String minStockStr = edtProdMinStock.getText().toString().trim();
        String unit = edtProdUnit.getText().toString().trim();

        if (name.isEmpty() || sku.isEmpty() || pPriceStr.isEmpty() || sPriceStr.isEmpty() || stockStr.isEmpty()) {
            Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        double purchasePrice = Double.parseDouble(pPriceStr);
        double sellingPrice = Double.parseDouble(sPriceStr);
        int stock = Integer.parseInt(stockStr);
        int minStock = minStockStr.isEmpty() ? 0 : Integer.parseInt(minStockStr);

        if (purchasePrice < 0 || sellingPrice < 0 || stock < 0 || minStock < 0) {
            Toast.makeText(this, "Negative amounts/quantities are not allowed", Toast.LENGTH_SHORT).show();
            return;
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("sku", sku);
        values.put("category", category);
        values.put("purchase_price", purchasePrice);
        values.put("selling_price", sellingPrice);
        values.put("stock", stock);
        values.put("min_stock", minStock);
        values.put("unit", unit.isEmpty() ? "pcs" : unit);

        try {
            if (editProductId == -1) {
                long result = db.insertOrThrow("products", null, values);
                if (result != -1) {
                    Toast.makeText(this, "Product Added Successfully", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(this, "Error inserting product. Duplicate SKU?", Toast.LENGTH_LONG).show();
                }
            } else {
                int rows = db.update("products", values, "id = ?", new String[]{String.valueOf(editProductId)});
                if (rows > 0) {
                    Toast.makeText(this, "Product Details Updated", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "Constraint error. SKU already exists?", Toast.LENGTH_LONG).show();
        }
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Product")
                .setMessage("Are you sure you want to delete this product? Action cannot be undone.")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SQLiteDatabase db = dbHelper.getWritableDatabase();
                        db.delete("products", "id = ?", new String[]{String.valueOf(editProductId)});
                        Toast.makeText(AddProductActivity.this, "Product Deleted", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}