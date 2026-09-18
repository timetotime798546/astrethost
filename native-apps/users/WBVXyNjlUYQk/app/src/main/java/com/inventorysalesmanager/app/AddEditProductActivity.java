package com.inventorysalesmanager.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class AddEditProductActivity extends Activity {

    private EditText etName, etSku, etPurchasePrice, etSellingPrice, etStockQty, etMinStock;
    private TextView tvFormTitle;
    private DatabaseHelper dbHelper;
    private long editProductId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_product);

        dbHelper = new DatabaseHelper(this);

        tvFormTitle = (TextView) findViewById(R.id.tv_form_title);
        etName = (EditText) findViewById(R.id.et_name);
        etSku = (EditText) findViewById(R.id.et_sku);
        etPurchasePrice = (EditText) findViewById(R.id.et_purchase_price);
        etSellingPrice = (EditText) findViewById(R.id.et_selling_price);
        etStockQty = (EditText) findViewById(R.id.et_stock_qty);
        etMinStock = (EditText) findViewById(R.id.et_min_stock);

        Button btnSave = (Button) findViewById(R.id.btn_save);
        Button btnCancel = (Button) findViewById(R.id.btn_cancel);

        // Check if editing
        if (getIntent().hasExtra("product_id")) {
            editProductId = getIntent().getLongExtra("product_id", -1);
            loadProductDetails(editProductId);
            tvFormTitle.setText("Edit Product");
        } else {
            tvFormTitle.setText("Add New Product");
            // Set default alert limit values
            etMinStock.setText("5");
        }

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveProduct();
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void loadProductDetails(long id) {
        Product p = dbHelper.getProduct(id);
        if (p != null) {
            etName.setText(p.getName());
            etSku.setText(p.getSku());
            etPurchasePrice.setText(String.valueOf(p.getPurchasePrice()));
            etSellingPrice.setText(String.valueOf(p.getSellingPrice()));
            etStockQty.setText(String.valueOf(p.getStockQty()));
            etMinStock.setText(String.valueOf(p.getMinStock()));
        }
    }

    private void saveProduct() {
        String name = etName.getText().toString().trim();
        String sku = etSku.getText().toString().trim();
        String purchaseStr = etPurchasePrice.getText().toString().trim();
        String sellingStr = etSellingPrice.getText().toString().trim();
        String stockStr = etStockQty.getText().toString().trim();
        String minStockStr = etMinStock.getText().toString().trim();

        // Validation
        if (name.isEmpty()) {
            etName.setError("Product name is required.");
            return;
        }

        double purchasePrice = 0;
        try {
            purchasePrice = Double.parseDouble(purchaseStr);
            if (purchasePrice < 0) {
                etPurchasePrice.setError("Must be positive.");
                return;
            }
        } catch (NumberFormatException e) {
            etPurchasePrice.setError("Valid price required.");
            return;
        }

        double sellingPrice = 0;
        try {
            sellingPrice = Double.parseDouble(sellingStr);
            if (sellingPrice < 0) {
                etSellingPrice.setError("Must be positive.");
                return;
            }
        } catch (NumberFormatException e) {
            etSellingPrice.setError("Valid price required.");
            return;
        }

        int stockQty = 0;
        try {
            stockQty = Integer.parseInt(stockStr);
            if (stockQty < 0) {
                etStockQty.setError("Must be 0 or more.");
                return;
            }
        } catch (NumberFormatException e) {
            etStockQty.setError("Valid quantity required.");
            return;
        }

        int minStock = 0;
        try {
            minStock = Integer.parseInt(minStockStr);
            if (minStock < 0) {
                etMinStock.setError("Must be 0 or more.");
                return;
            }
        } catch (NumberFormatException e) {
            etMinStock.setError("Valid level required.");
            return;
        }

        Product p = new Product();
        p.setName(name);
        p.setSku(sku.isEmpty() ? "GEN-" + System.currentTimeMillis() / 1000 : sku);
        p.setPurchasePrice(purchasePrice);
        p.setSellingPrice(sellingPrice);
        p.setStockQty(stockQty);
        p.setMinStock(minStock);

        if (editProductId == -1) {
            // New insert
            long id = dbHelper.insertProduct(p);
            if (id > -1) {
                Toast.makeText(this, "Product created successfully", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Failed to create product. Check SKU uniqueness.", Toast.LENGTH_LONG).show();
            }
        } else {
            // Update
            p.setId(editProductId);
            int rows = dbHelper.updateProduct(p);
            if (rows > 0) {
                Toast.makeText(this, "Product updated successfully", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Failed to update product.", Toast.LENGTH_LONG).show();
            }
        }
    }
}