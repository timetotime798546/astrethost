package com.omniflowbusinesserp.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends Activity {

    private TextView tvTotalRevenue, tvTotalExpenses, tvNetCashFlow, tvTotalStockValue, tvLowStockAlerts, tvSyncStatus;
    private Button btnLogout, btnInventory, btnBilling, btnExpenses;
    private BackendApi api;
    private String token;

    private double totalRevenue = 0.0;
    private double totalExpenses = 0.0;
    private double totalStockAssetVal = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences prefs = getSharedPreferences("OmniPrefs", MODE_PRIVATE);
        token = prefs.getString("token", null);
        if (token == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        api = new BackendApi(this);

        tvTotalRevenue = (TextView) findViewById(R.id.tvTotalRevenue);
        tvTotalExpenses = (TextView) findViewById(R.id.tvTotalExpenses);
        tvNetCashFlow = (TextView) findViewById(R.id.tvNetCashFlow);
        tvTotalStockValue = (TextView) findViewById(R.id.tvTotalStockValue);
        tvLowStockAlerts = (TextView) findViewById(R.id.tvLowStockAlerts);
        tvSyncStatus = (TextView) findViewById(R.id.tvSyncStatus);

        btnLogout = (Button) findViewById(R.id.btnLogout);
        btnInventory = (Button) findViewById(R.id.btnInventory);
        btnBilling = (Button) findViewById(R.id.btnBilling);
        btnExpenses = (Button) findViewById(R.id.btnExpenses);

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor editor = getSharedPreferences("OmniPrefs", MODE_PRIVATE).edit();
                editor.clear();
                editor.apply();
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
                finish();
            }
        });

        btnInventory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, InventoryActivity.class));
            }
        });

        btnBilling.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, BillingActivity.class));
            }
        });

        btnExpenses.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, ExpenseActivity.class));
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchAndCalculateMetrics();
    }

    private void fetchAndCalculateMetrics() {
        tvSyncStatus.setText("Calculating live indicators from cloud...");
        totalRevenue = 0.0;
        totalExpenses = 0.0;
        totalStockAssetVal = 0.0;

        // Fetch Inventory
        api.readData(token, "inventory", new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject root = new JSONObject(response);
                    if (root.getBoolean("success")) {
                        JSONArray records = root.getJSONArray("records");
                        StringBuilder lowStockBuilder = new StringBuilder();
                        for (int i = 0; i < records.length(); i++) {
                            JSONObject recData = records.getJSONObject(i).getJSONObject("data");
                            double qty = recData.optDouble("quantity", 0);
                            double price = recData.optDouble("price", 0);
                            String name = recData.optString("name", "Product");
                            String warehouse = recData.optString("warehouse", "Main");

                            totalStockAssetVal += (qty * price);

                            if (qty < 5) {
                                lowStockBuilder.append("- ").append(name)
                                        .append(" (Warehouse: ").append(warehouse)
                                        .append(") is low in stock [Qty: ").append((int)qty).append("]\n");
                            }
                        }

                        tvTotalStockValue.setText("Total Asset Value in Warehouse: $" + String.format("%.2f", totalStockAssetVal));
                        if (lowStockBuilder.length() > 0) {
                            tvLowStockAlerts.setText(lowStockBuilder.toString().trim());
                        } else {
                            tvLowStockAlerts.setText("No immediate alerts. Everything is fully stocked.");
                        }
                    }
                    updateFinancialDisplay();
                } catch (Exception e) {
                    tvSyncStatus.setText("Inventory load issue: " + e.getMessage());
                }
            }

            @Override
            public void onError(String errorMessage) {
                tvSyncStatus.setText("Sync failed: " + errorMessage);
            }
        });

        // Fetch Invoices / Sales
        api.readData(token, "sales", new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject root = new JSONObject(response);
                    if (root.getBoolean("success")) {
                        JSONArray records = root.getJSONArray("records");
                        for (int i = 0; i < records.length(); i++) {
                            JSONObject recData = records.getJSONObject(i).getJSONObject("data");
                            double total = recData.optDouble("total", 0);
                            totalRevenue += total;
                        }
                    }
                    updateFinancialDisplay();
                } catch (Exception e) {
                    tvSyncStatus.setText("Sales load issue: " + e.getMessage());
                }
            }

            @Override
            public void onError(String errorMessage) {
                tvSyncStatus.setText("Sales Sync failed: " + errorMessage);
            }
        });

        // Fetch Expenses
        api.readData(token, "expenses", new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject root = new JSONObject(response);
                    if (root.getBoolean("success")) {
                        JSONArray records = root.getJSONArray("records");
                        for (int i = 0; i < records.length(); i++) {
                            JSONObject recData = records.getJSONObject(i).getJSONObject("data");
                            double amt = recData.optDouble("amount", 0);
                            totalExpenses += amt;
                        }
                    }
                    updateFinancialDisplay();
                } catch (Exception e) {
                    tvSyncStatus.setText("Expenses load issue: " + e.getMessage());
                }
            }

            @Override
            public void onError(String errorMessage) {
                tvSyncStatus.setText("Expenses Sync failed: " + errorMessage);
            }
        });
    }

    private void updateFinancialDisplay() {
        tvTotalRevenue.setText("$" + String.format("%.2f", totalRevenue));
        tvTotalExpenses.setText("$" + String.format("%.2f", totalExpenses));

        // Deterministic Cashflow calculation performed natively
        double netCashFlow = totalRevenue - totalExpenses;
        tvNetCashFlow.setText("$" + String.format("%.2f", netCashFlow));
        if (netCashFlow >= 0) {
            tvNetCashFlow.setTextColor(0xFF2E7D32); // Dark Green
        } else {
            tvNetCashFlow.setTextColor(0xFFC62828); // Dark Red
        }
        tvSyncStatus.setText("Dynamic updates calculated locally.");
    }
}