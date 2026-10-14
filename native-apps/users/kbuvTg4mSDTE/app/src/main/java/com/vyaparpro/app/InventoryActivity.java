package com.vyaparpro.app;

import android.app.Activity;
import android.graphics.Color;
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

public class InventoryActivity extends Activity {

    private BackendApi api;
    private LinearLayout layoutAddProductForm;
    private LinearLayout containerProducts;
    private ProgressBar progressInventory;
    private EditText etItemName;
    private EditText etBuyPrice;
    private EditText etSellPrice;
    private EditText etStockQty;
    private EditText etUnit;
    private Button btnToggleAddProduct;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);

        api = new BackendApi(this);

        layoutAddProductForm = (LinearLayout) findViewById(R.id.layoutAddProductForm);
        containerProducts = (LinearLayout) findViewById(R.id.containerProducts);
        progressInventory = (ProgressBar) findViewById(R.id.progressInventory);

        etItemName = (EditText) findViewById(R.id.etItemName);
        etBuyPrice = (EditText) findViewById(R.id.etBuyPrice);
        etSellPrice = (EditText) findViewById(R.id.etSellPrice);
        etStockQty = (EditText) findViewById(R.id.etStockQty);
        etUnit = (EditText) findViewById(R.id.etUnit);
        btnToggleAddProduct = (Button) findViewById(R.id.btnToggleAddProduct);

        findViewById(R.id.btnInventoryBack).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnToggleAddProduct.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (layoutAddProductForm.getVisibility() == View.VISIBLE) {
                    layoutAddProductForm.setVisibility(View.GONE);
                    btnToggleAddProduct.setText("+ Add Item");
                } else {
                    layoutAddProductForm.setVisibility(View.VISIBLE);
                    btnToggleAddProduct.setText("Cancel");
                }
            }
        });

        findViewById(R.id.btnSaveProduct).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveProduct();
            }
        });

        loadProducts();
    }

    private void saveProduct() {
        String name = etItemName.getText().toString().trim();
        String buyStr = etBuyPrice.getText().toString().trim();
        String sellStr = etSellPrice.getText().toString().trim();
        String stockStr = etStockQty.getText().toString().trim();
        String unit = etUnit.getText().toString().trim();

        if (name.isEmpty() || buyStr.isEmpty() || sellStr.isEmpty() || stockStr.isEmpty()) {
            Toast.makeText(this, "Please fill in all product fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double buy = Double.parseDouble(buyStr);
            double sell = Double.parseDouble(sellStr);
            int stock = Integer.parseInt(stockStr);
            if (unit.isEmpty()) unit = "pcs";

            JSONObject data = new JSONObject();
            data.put("name", name);
            data.put("buy_price", buy);
            data.put("sell_price", sell);
            data.put("stock", stock);
            data.put("unit", unit);

            progressInventory.setVisibility(View.VISIBLE);
            api.createData("products", data, new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    progressInventory.setVisibility(View.GONE);
                    Toast.makeText(InventoryActivity.this, "Product Added!", Toast.LENGTH_SHORT).show();
                    layoutAddProductForm.setVisibility(View.GONE);
                    btnToggleAddProduct.setText("+ Add Item");
                    etItemName.setText("");
                    etBuyPrice.setText("");
                    etSellPrice.setText("");
                    etStockQty.setText("");
                    etUnit.setText("");
                    loadProducts();
                }

                @Override
                public void onError(String errorMessage) {
                    progressInventory.setVisibility(View.GONE);
                    Toast.makeText(InventoryActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Toast.makeText(this, "Invalid numbers entered.", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadProducts() {
        progressInventory.setVisibility(View.VISIBLE);
        api.getData("products", new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                progressInventory.setVisibility(View.GONE);
                containerProducts.removeAllViews();
                JSONArray records = response.optJSONArray("records");
                if (records == null || records.length() == 0) {
                    TextView empty = new TextView(InventoryActivity.this);
                    empty.setText("No products found in inventory. Tap + Add Item to create one.");
                    empty.setPadding(16, 32, 16, 32);
                    empty.setTextColor(Color.parseColor("#64748B"));
                    containerProducts.addView(empty);
                    return;
                }

                for (int i = 0; i < records.length(); i++) {
                    final JSONObject r = records.optJSONObject(i);
                    if (r == null) continue;
                    final String recordId = r.optString("id", "");
                    final JSONObject data = r.optJSONObject("data");
                    if (data == null) continue;

                    String name = data.optString("name", "Product");
                    double buy = data.optDouble("buy_price", 0.0);
                    double sell = data.optDouble("sell_price", 0.0);
                    int stock = data.optInt("stock", 0);
                    String unit = data.optString("unit", "pcs");

                    // Card Layout
                    LinearLayout card = new LinearLayout(InventoryActivity.this);
                    card.setOrientation(LinearLayout.VERTICAL);
                    card.setBackgroundColor(Color.WHITE);
                    card.setElevation(2f);
                    card.setPadding(24, 20, 24, 20);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    lp.setMargins(0, 0, 0, 16);
                    card.setLayoutParams(lp);

                    // Top Row: Name and Low stock badge
                    LinearLayout topRow = new LinearLayout(InventoryActivity.this);
                    topRow.setOrientation(LinearLayout.HORIZONTAL);

                    TextView tvTitle = new TextView(InventoryActivity.this);
                    tvTitle.setText(name);
                    tvTitle.setTextColor(Color.parseColor("#0F172A"));
                    tvTitle.setTextSize(16f);
                    tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
                    LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                    topRow.addView(tvTitle, titleLp);

                    if (BusinessLogic.isLowStock(stock)) {
                        TextView tvAlert = new TextView(InventoryActivity.this);
                        tvAlert.setText("LOW STOCK");
                        tvAlert.setTextColor(Color.parseColor("#EF4444"));
                        tvAlert.setTextSize(11f);
                        tvAlert.setBackgroundColor(Color.parseColor("#FEE2E2"));
                        tvAlert.setPadding(12, 6, 12, 6);
                        tvAlert.setTypeface(null, android.graphics.Typeface.BOLD);
                        topRow.addView(tvAlert);
                    }
                    card.addView(topRow);

                    // Price and Margin Row
                    double profitPerUnit = sell - buy;
                    TextView tvPrices = new TextView(InventoryActivity.this);
                    tvPrices.setText(String.format("Sell: %s  |  Cost: %s  |  Margin: %s",
                            BusinessLogic.getFormattedCurrency(sell),
                            BusinessLogic.getFormattedCurrency(buy),
                            BusinessLogic.getFormattedCurrency(profitPerUnit)));
                    tvPrices.setTextColor(Color.parseColor("#64748B"));
                    tvPrices.setTextSize(13f);
                    tvPrices.setPadding(0, 8, 0, 8);
                    card.addView(tvPrices);

                    // Bottom Row: Stock Quantity and Delete Button
                    LinearLayout botRow = new LinearLayout(InventoryActivity.this);
                    botRow.setOrientation(LinearLayout.HORIZONTAL);
                    botRow.setGravity(android.view.Gravity.CENTER_VERTICAL);

                    TextView tvStock = new TextView(InventoryActivity.this);
                    tvStock.setText("Available: " + stock + " " + unit);
                    tvStock.setTextColor(BusinessLogic.isLowStock(stock) ? Color.parseColor("#EF4444") : Color.parseColor("#10B981"));
                    tvStock.setTextSize(14f);
                    tvStock.setTypeface(null, android.graphics.Typeface.BOLD);
                    LinearLayout.LayoutParams stockLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                    botRow.addView(tvStock, stockLp);

                    Button btnDel = new Button(InventoryActivity.this);
                    btnDel.setText("Delete");
                    btnDel.setTextSize(12f);
                    btnDel.setTextColor(Color.WHITE);
                    btnDel.setBackgroundColor(Color.parseColor("#94A3B8"));
                    btnDel.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            deleteProductItem(recordId);
                        }
                    });
                    botRow.addView(btnDel);

                    card.addView(botRow);
                    containerProducts.addView(card);
                }
            }

            @Override
            public void onError(String errorMessage) {
                progressInventory.setVisibility(View.GONE);
                Toast.makeText(InventoryActivity.this, "Error fetching inventory: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteProductItem(String id) {
        progressInventory.setVisibility(View.VISIBLE);
        api.deleteData(id, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                progressInventory.setVisibility(View.GONE);
                Toast.makeText(InventoryActivity.this, "Item removed.", Toast.LENGTH_SHORT).show();
                loadProducts();
            }

            @Override
            public void onError(String errorMessage) {
                progressInventory.setVisibility(View.GONE);
                Toast.makeText(InventoryActivity.this, "Failed: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }
}