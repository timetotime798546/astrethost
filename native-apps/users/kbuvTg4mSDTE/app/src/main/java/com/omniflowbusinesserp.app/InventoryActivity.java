package com.omniflowbusinesserp.app;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;

public class InventoryActivity extends Activity {

    private EditText etName, etSku, etPrice, etQty;
    private Spinner spWarehouse;
    private Button btnAddItem;
    private LinearLayout layoutInventoryList;

    private BackendApi api;
    private String token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);

        SharedPreferences prefs = getSharedPreferences("OmniPrefs", MODE_PRIVATE);
        token = prefs.getString("token", null);

        api = new BackendApi(this);

        etName = (EditText) findViewById(R.id.etName);
        etSku = (EditText) findViewById(R.id.etSku);
        etPrice = (EditText) findViewById(R.id.etPrice);
        etQty = (EditText) findViewById(R.id.etQty);
        spWarehouse = (Spinner) findViewById(R.id.spWarehouse);
        btnAddItem = (Button) findViewById(R.id.btnAddItem);
        layoutInventoryList = (LinearLayout) findViewById(R.id.layoutInventoryList);

        String[] warehouses = {"Warehouse A (North)", "Warehouse B (South)", "Warehouse C (Central)"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, warehouses);
        spWarehouse.setAdapter(adapter);

        btnAddItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addItem();
            }
        });

        loadInventory();
    }

    private void addItem() {
        String name = etName.getText().toString().trim();
        String sku = etSku.getText().toString().trim();
        String priceStr = etPrice.getText().toString().trim();
        String qtyStr = etQty.getText().toString().trim();
        String warehouse = spWarehouse.getSelectedItem().toString();

        if (name.isEmpty() || sku.isEmpty() || priceStr.isEmpty() || qtyStr.isEmpty()) {
            Toast.makeText(this, "Please fill out all stock details", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double price = Double.parseDouble(priceStr);
            int qty = Integer.parseInt(qtyStr);

            JSONObject item = new JSONObject();
            item.put("name", name);
            item.put("sku", sku);
            item.put("price", price);
            item.put("quantity", qty);
            item.put("warehouse", warehouse);

            api.createData(token, "inventory", item, new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(String response) {
                    Toast.makeText(InventoryActivity.this, "Stock Registered Successfully", Toast.LENGTH_SHORT).show();
                    etName.setText("");
                    etSku.setText("");
                    etPrice.setText("");
                    etQty.setText("");
                    loadInventory();
                }

                @Override
                public void onError(String errorMessage) {
                    Toast.makeText(InventoryActivity.this, "Error registering stock: " + errorMessage, Toast.LENGTH_LONG).show();
                }
            });

        } catch (Exception e) {
            Toast.makeText(this, "Price & Qty must be numbers", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadInventory() {
        layoutInventoryList.removeAllViews();
        api.readData(token, "inventory", new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject json = new JSONObject(response);
                    if (json.getBoolean("success")) {
                        JSONArray records = json.getJSONArray("records");
                        for (int i = 0; i < records.length(); i++) {
                            JSONObject record = records.getJSONObject(i);
                            final String recordId = record.getString("id");
                            JSONObject itemData = record.getJSONObject("data");

                            String name = itemData.optString("name", "N/A");
                            String sku = itemData.optString("sku", "N/A");
                            double price = itemData.optDouble("price", 0.0);
                            int quantity = itemData.optInt("quantity", 0);
                            String warehouse = itemData.optString("warehouse", "N/A");

                            addInventoryRowToLayout(recordId, name, sku, price, quantity, warehouse);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(InventoryActivity.this, "Inventory Sync Error: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addInventoryRowToLayout(final String id, final String name, final String sku, final double price, final int quantity, final String warehouse) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(12, 12, 12, 12);
        row.setBackgroundColor(0xFFFFFFFF);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 8);
        row.setLayoutParams(params);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(name + " [SKU: " + sku + "]");
        tvTitle.setTextSize(14spToFloat());
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTitle.setTextColor(0xFF333333);

        TextView tvDetails = new TextView(this);
        tvDetails.setText("Price: $" + String.format("%.2f", price) + " | In Stock: " + quantity + " | Location: " + warehouse);
        tvDetails.setTextSize(12spToFloat());
        tvDetails.setTextColor(0xFF555555);

        LinearLayout btnLayout = new LinearLayout(this);
        btnLayout.setOrientation(LinearLayout.HORIZONTAL);
        btnLayout.setPadding(0, 8, 0, 0);

        Button btnDelete = new Button(this);
        btnDelete.setText("DELETE STOCK");
        btnDelete.setBackgroundColor(0xFFC62828);
        btnDelete.setTextColor(0xFFFFFFFF);
        btnDelete.setTextSize(10spToFloat());
        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                api.deleteData(token, id, new BackendApi.ApiCallback() {
                    @Override
                    public void onSuccess(String response) {
                        Toast.makeText(InventoryActivity.this, "Item Deleted", Toast.LENGTH_SHORT).show();
                        loadInventory();
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Toast.makeText(InventoryActivity.this, "Failed deletion: " + errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        btnLayout.addView(btnDelete);

        row.addView(tvTitle);
        row.addView(tvDetails);
        row.addView(btnLayout);

        layoutInventoryList.addView(row);
    }

    private float spToFloat() {
        return 14.0f;
    }

    private float spToFloat(float val) {
        return val;
    }

    private float 14spToFloat() {
        return 14.0f;
    }

    private float 12spToFloat() {
        return 12.0f;
    }

    private float 10spToFloat() {
        return 10.0f;
    }
}