package com.shopinventorysync.app;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONObject;

public class AddEditProductActivity extends Activity {

    private EditText edtFormSku, edtFormName, edtFormPurchasePrice, edtFormSellingPrice, edtFormQuantity, edtFormLowStock;
    private Spinner spinFormCategory;
    private TextView txtFormHeader, txtFormMarginResult;
    private Button btnFormSave, btnFormDelete;

    private BackendApi api;
    private ProgressDialog progressDialog;

    private String editProductId = null;
    private static final String[] CATEGORIES = {"Groceries", "Hardware", "Electronics", "General"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit);

        api = new BackendApi(this);

        txtFormHeader = (TextView) findViewById(R.id.txtFormHeader);
        txtFormMarginResult = (TextView) findViewById(R.id.txtFormMarginResult);
        edtFormSku = (EditText) findViewById(R.id.edtFormSku);
        edtFormName = (EditText) findViewById(R.id.edtFormName);
        edtFormPurchasePrice = (EditText) findViewById(R.id.edtFormPurchasePrice);
        edtFormSellingPrice = (EditText) findViewById(R.id.edtFormSellingPrice);
        edtFormQuantity = (EditText) findViewById(R.id.edtFormQuantity);
        edtFormLowStock = (EditText) findViewById(R.id.edtFormLowStock);
        spinFormCategory = (Spinner) findViewById(R.id.spinFormCategory);
        btnFormSave = (Button) findViewById(R.id.btnFormSave);
        btnFormDelete = (Button) findViewById(R.id.btnFormDelete);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Updating stock records...");
        progressDialog.setCancelable(false);

        // Spinner categories config
        ArrayAdapter<String> categoriesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, CATEGORIES);
        spinFormCategory.setAdapter(categoriesAdapter);

        // Price listener for dynamic local margin calculations
        TextWatcher priceWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                calculateLocalMargin();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };
        edtFormPurchasePrice.addTextChangedListener(priceWatcher);
        edtFormSellingPrice.addTextChangedListener(priceWatcher);

        // Check if editing existing item
        Intent intent = getIntent();
        if (intent.hasExtra("productId")) {
            editProductId = intent.getStringExtra("productId");
            txtFormHeader.setText("Edit Product Details");
            btnFormDelete.setVisibility(View.VISIBLE);

            edtFormSku.setText(intent.getStringExtra("sku"));
            edtFormName.setText(intent.getStringExtra("name"));
            edtFormPurchasePrice.setText(String.valueOf(intent.getDoubleExtra("purchasePrice", 0.0)));
            edtFormSellingPrice.setText(String.valueOf(intent.getDoubleExtra("sellingPrice", 0.0)));
            edtFormQuantity.setText(String.valueOf(intent.getIntExtra("quantity", 0)));
            edtFormLowStock.setText(String.valueOf(intent.getIntExtra("lowStockThreshold", 5)));

            String categoryVal = intent.getStringExtra("category");
            for (int i = 0; i < CATEGORIES.length; i++) {
                if (CATEGORIES[i].equalsIgnoreCase(categoryVal)) {
                    spinFormCategory.setSelection(i);
                    break;
                }
            }
            calculateLocalMargin();
        } else {
            txtFormHeader.setText("New Store Product");
            btnFormDelete.setVisibility(View.GONE);
        }

        btnFormSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveProduct();
            }
        });

        btnFormDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteProduct();
            }
        });
    }

    // Local Margin calculations
    private void calculateLocalMargin() {
        try {
            double buy = Double.parseDouble(edtFormPurchasePrice.getText().toString().trim());
            double sell = Double.parseDouble(edtFormSellingPrice.getText().toString().trim());
            if (sell > 0) {
                double profit = sell - buy;
                double margin = (profit / sell) * 100.0;
                txtFormMarginResult.setText(String.format("Calculated Margin: %.2f%% ($%.2f profit per item)", margin, profit));
            } else {
                txtFormMarginResult.setText("Calculated Margin: 0.00% ($0.00 profit)");
            }
        } catch (Exception e) {
            txtFormMarginResult.setText("Calculated Margin: 0.00% ($0.00 profit)");
        }
    }

    private void saveProduct() {
        final String sku = edtFormSku.getText().toString().trim();
        final String name = edtFormName.getText().toString().trim();
        final String buyStr = edtFormPurchasePrice.getText().toString().trim();
        final String sellStr = edtFormSellingPrice.getText().toString().trim();
        final String qtyStr = edtFormQuantity.getText().toString().trim();
        final String alertStr = edtFormLowStock.getText().toString().trim();

        if (sku.isEmpty() || name.isEmpty() || buyStr.isEmpty() || sellStr.isEmpty() || qtyStr.isEmpty()) {
            Toast.makeText(this, "Please satisfy all form inputs", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.show();

        try {
            final double buy = Double.parseDouble(buyStr);
            final double sell = Double.parseDouble(sellStr);
            final int qty = Integer.parseInt(qtyStr);
            final int alert = alertStr.isEmpty() ? 5 : Integer.parseInt(alertStr);
            final String category = spinFormCategory.getSelectedItem().toString();

            JSONObject fields = new JSONObject();
            fields.put("sku", sku);
            fields.put("name", name);
            fields.put("category", category);
            fields.put("purchase_price", buy);
            fields.put("selling_price", sell);
            fields.put("quantity", qty);
            fields.put("low_stock_threshold", alert);

            if (editProductId == null) {
                // CREATE Mode
                api.createRecord("products", fields, new BackendApi.ApiCallback<JSONObject>() {
                    @Override
                    public void onSuccess(JSONObject result) {
                        progressDialog.dismiss();
                        Toast.makeText(AddEditProductActivity.this, "Product registered successfully!", Toast.LENGTH_SHORT).show();
                        finish();
                    }

                    @Override
                    public void onError(String error) {
                        progressDialog.dismiss();
                        Toast.makeText(AddEditProductActivity.this, "Error saving: " + error, Toast.LENGTH_LONG).show();
                    }
                });
            } else {
                // UPDATE Mode
                api.updateRecord(editProductId, fields, new BackendApi.ApiCallback<String>() {
                    @Override
                    public void onSuccess(String result) {
                        progressDialog.dismiss();
                        Toast.makeText(AddEditProductActivity.this, "Stock record modified!", Toast.LENGTH_SHORT).show();
                        finish();
                    }

                    @Override
                    public void onError(String error) {
                        progressDialog.dismiss();
                        Toast.makeText(AddEditProductActivity.this, "Error: " + error, Toast.LENGTH_LONG).show();
                    }
                });
            }

        } catch (Exception e) {
            progressDialog.dismiss();
            Toast.makeText(this, "Invalid form data fields", Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteProduct() {
        if (editProductId == null) return;

        progressDialog.show();
        api.deleteRecord(editProductId, new BackendApi.ApiCallback<String>() {
            @Override
            public void onSuccess(String result) {
                progressDialog.dismiss();
                Toast.makeText(AddEditProductActivity.this, "Product removed from Inventory.", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(String error) {
                progressDialog.dismiss();
                Toast.makeText(AddEditProductActivity.this, "Delete failed: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }
}