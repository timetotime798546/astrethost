package com.morgbilling.app;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class InventoryActivity extends Activity {

    private BackendApi backendApi;

    private EditText etProdName;
    private EditText etProdPrice;
    private EditText etProdStock;
    private Button btnAddProduct;
    private Button btnRefreshInventory;
    private ProgressBar inventoryProgress;
    private LinearLayout productsContainer;

    private static class ProductModel {
        String id;
        String name;
        double price;
        int stock;
    }

    private final List<ProductModel> productList = new ArrayList<ProductModel>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);

        backendApi = new BackendApi(this);

        etProdName = (EditText) findViewById(R.id.etProdName);
        etProdPrice = (EditText) findViewById(R.id.etProdPrice);
        etProdStock = (EditText) findViewById(R.id.etProdStock);
        btnAddProduct = (Button) findViewById(R.id.btnAddProduct);
        btnRefreshInventory = (Button) findViewById(R.id.btnRefreshInventory);
        inventoryProgress = (ProgressBar) findViewById(R.id.inventoryProgress);
        productsContainer = (LinearLayout) findViewById(R.id.productsContainer);

        setupDefaults();

        btnAddProduct.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addProduct();
            }
        });

        btnRefreshInventory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadProducts();
            }
        });

        loadProducts();
    }

    private void setupDefaults() {
        if (productList.isEmpty()) {
            ProductModel p1 = new ProductModel();
            p1.name = "Thermal Receipt Paper Roll";
            p1.price = 12.50;
            p1.stock = 45;
            productList.add(p1);

            ProductModel p2 = new ProductModel();
            p2.name = "Standard Consulting Hour";
            p2.price = 95.00;
            p2.stock = 999;
            productList.add(p2);
        }
        renderProducts();
    }

    private void loadProducts() {
        if (!backendApi.isLoggedIn()) {
            renderProducts();
            return;
        }

        inventoryProgress.setVisibility(View.VISIBLE);
        backendApi.readRecords("products", new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject jsonResponse) {
                inventoryProgress.setVisibility(View.GONE);
                productList.clear();
                try {
                    JSONArray arr = jsonResponse.optJSONArray("records");
                    if (arr != null) {
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject rec = arr.getJSONObject(i);
                            JSONObject data = rec.optJSONObject("data");
                            if (data != null) {
                                ProductModel p = new ProductModel();
                                p.id = rec.optString("id");
                                p.name = data.optString("name", "Product");
                                p.price = data.optDouble("unit_price", 0.0);
                                p.stock = data.optInt("stock_qty", 0);
                                productList.add(p);
                            }
                        }
                    }
                } catch (Exception ignored) {}
                renderProducts();
            }

            @Override
            public void onError(String errorMessage) {
                inventoryProgress.setVisibility(View.GONE);
                Toast.makeText(InventoryActivity.this, "Sync: " + errorMessage, Toast.LENGTH_SHORT).show();
                renderProducts();
            }
        });
    }

    private void addProduct() {
        final String name = etProdName.getText().toString().trim();
        String priceStr = etProdPrice.getText().toString().trim();
        String stockStr = etProdStock.getText().toString().trim();

        if (name.isEmpty() || priceStr.isEmpty()) {
            Toast.makeText(this, "Please enter name and price", Toast.LENGTH_SHORT).show();
            return;
        }

        double price = 0.0;
        int stock = 0;
        try {
            price = Double.parseDouble(priceStr);
            stock = stockStr.isEmpty() ? 0 : Integer.parseInt(stockStr);
        } catch (Exception e) {
            Toast.makeText(this, "Invalid number format", Toast.LENGTH_SHORT).show();
            return;
        }

        final ProductModel pm = new ProductModel();
        pm.name = name;
        pm.price = price;
        pm.stock = stock;

        if (backendApi.isLoggedIn()) {
            inventoryProgress.setVisibility(View.VISIBLE);
            JSONObject data = new JSONObject();
            try {
                data.put("name", name);
                data.put("unit_price", price);
                data.put("stock_qty", stock);
                data.put("unit", "pcs");
            } catch (Exception ignored) {}

            backendApi.createRecord("products", data, new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(JSONObject jsonResponse) {
                    inventoryProgress.setVisibility(View.GONE);
                    JSONObject rec = jsonResponse.optJSONObject("record");
                    if (rec != null) {
                        pm.id = rec.optString("id");
                    }
                    productList.add(0, pm);
                    renderProducts();
                    clearInputs();
                    Toast.makeText(InventoryActivity.this, "Product added to catalog", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(String errorMessage) {
                    inventoryProgress.setVisibility(View.GONE);
                    productList.add(0, pm);
                    renderProducts();
                    clearInputs();
                    Toast.makeText(InventoryActivity.this, "Added locally", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            productList.add(0, pm);
            renderProducts();
            clearInputs();
            Toast.makeText(this, "Product added locally", Toast.LENGTH_SHORT).show();
        }
    }

    private void clearInputs() {
        etProdName.setText("");
        etProdPrice.setText("");
        etProdStock.setText("");
    }

    private void renderProducts() {
        productsContainer.removeAllViews();
        if (productList.isEmpty()) {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("No products cataloged yet.");
            tvEmpty.setTextColor(getResources().getColor(R.color.text_muted));
            tvEmpty.setPadding(20, 20, 20, 20);
            productsContainer.addView(tvEmpty);
            return;
        }

        for (int i = 0; i < productList.size(); i++) {
            final ProductModel p = productList.get(i);

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setBackgroundResource(R.drawable.bg_card);
            card.setPadding(32, 28, 32, 28);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            lp.setMargins(0, 0, 0, 16);
            card.setLayoutParams(lp);

            LinearLayout left = new LinearLayout(this);
            left.setOrientation(LinearLayout.VERTICAL);
            left.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));

            TextView tvName = new TextView(this);
            tvName.setText(p.name);
            tvName.setTextSize(15);
            tvName.setTextColor(getResources().getColor(R.color.primary));

            TextView tvStock = new TextView(this);
            tvStock.setText("Stock: " + p.stock + " units");
            tvStock.setTextSize(12);
            tvStock.setTextColor(getResources().getColor(R.color.text_muted));
            tvStock.setPadding(0, 4, 0, 0);

            left.addView(tvName);
            left.addView(tvStock);

            TextView tvPrice = new TextView(this);
            tvPrice.setText(BillingCalculator.formatMoney(p.price));
            tvPrice.setTextSize(16);
            tvPrice.setTypeface(null, Typeface.BOLD);
            tvPrice.setTextColor(getResources().getColor(R.color.accent_blue));
            tvPrice.setGravity(android.view.Gravity.CENTER_VERTICAL);

            card.addView(left);
            card.addView(tvPrice);

            productsContainer.addView(card);
        }
    }
}