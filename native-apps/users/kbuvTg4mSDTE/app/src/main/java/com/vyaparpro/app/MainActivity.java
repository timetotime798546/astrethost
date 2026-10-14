package com.vyaparpro.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends Activity {

    private BackendApi api;
    private TextView tvShopSubtitle;
    private TextView tvTotalSales;
    private TextView tvSalesCount;
    private TextView tvTotalUdhaar;
    private TextView tvTotalStockItems;
    private TextView tvLowStockBadge;
    private TextView tvNetProfit;
    private ProgressBar dashProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        api = new BackendApi(this);

        if (!api.isLoggedIn()) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        initViews();
        setupActions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (api.isLoggedIn()) {
            loadDashboardData();
        }
    }

    private void initViews() {
        tvShopSubtitle = (TextView) findViewById(R.id.tvShopSubtitle);
        tvTotalSales = (TextView) findViewById(R.id.tvTotalSales);
        tvSalesCount = (TextView) findViewById(R.id.tvSalesCount);
        tvTotalUdhaar = (TextView) findViewById(R.id.tvTotalUdhaar);
        tvTotalStockItems = (TextView) findViewById(R.id.tvTotalStockItems);
        tvLowStockBadge = (TextView) findViewById(R.id.tvLowStockBadge);
        tvNetProfit = (TextView) findViewById(R.id.tvNetProfit);
        dashProgressBar = (ProgressBar) findViewById(R.id.dashProgressBar);

        String email = api.getUserEmail();
        if (!email.isEmpty()) {
            tvShopSubtitle.setText("Logged in as " + email);
        }
    }

    private void setupActions() {
        findViewById(R.id.btnLogout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                api.logout(new BackendApi.ApiCallback() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                        startActivity(intent);
                        finish();
                    }
                });
            }
        });

        findViewById(R.id.btnQuickAddSale).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, PosActivity.class));
            }
        });

        findViewById(R.id.btnQuickAddUdhaar).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, KhataActivity.class));
            }
        });

        findViewById(R.id.btnQuickAddStock).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, InventoryActivity.class));
            }
        });

        findViewById(R.id.btnQuickViewKhata).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, KhataActivity.class));
            }
        });

        findViewById(R.id.btnSyncRefresh).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadDashboardData();
            }
        });
    }

    private void loadDashboardData() {
        dashProgressBar.setVisibility(View.VISIBLE);

        // Fetch Sales for Sales total calculation
        api.getData("sales", new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                JSONArray records = response.optJSONArray("records");
                double salesTotal = 0;
                int count = 0;
                if (records != null) {
                    count = records.length();
                    for (int i = 0; i < records.length(); i++) {
                        JSONObject r = records.optJSONObject(i);
                        if (r != null) {
                            JSONObject data = r.optJSONObject("data");
                            if (data != null) {
                                salesTotal += data.optDouble("total", 0.0);
                            }
                        }
                    }
                }
                tvTotalSales.setText(BusinessLogic.getFormattedCurrency(salesTotal));
                tvSalesCount.setText(count + " invoices generated");

                // Next, fetch inventory stock and calculate profit potential
                fetchInventoryMetrics();
            }

            @Override
            public void onError(String errorMessage) {
                dashProgressBar.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, "Sales sync: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchInventoryMetrics() {
        api.getData("products", new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                JSONArray records = response.optJSONArray("records");
                int itemCount = 0;
                int lowStockCount = 0;
                double totalEstimatedMargin = 0;

                if (records != null) {
                    itemCount = records.length();
                    for (int i = 0; i < records.length(); i++) {
                        JSONObject r = records.optJSONObject(i);
                        if (r != null) {
                            JSONObject data = r.optJSONObject("data");
                            if (data != null) {
                                int stock = data.optInt("stock", 0);
                                double buy = data.optDouble("buy_price", 0.0);
                                double sell = data.optDouble("sell_price", 0.0);

                                if (BusinessLogic.isLowStock(stock)) {
                                    lowStockCount++;
                                }
                                totalEstimatedMargin += BusinessLogic.calculateProfit(sell, buy, stock);
                            }
                        }
                    }
                }

                tvTotalStockItems.setText(itemCount + " Items");
                tvLowStockBadge.setText(lowStockCount + " items low on stock");
                tvNetProfit.setText(BusinessLogic.getFormattedCurrency(totalEstimatedMargin));

                // Next, fetch transactions to compute total outstanding udhaar
                fetchUdhaarLedger();
            }

            @Override
            public void onError(String errorMessage) {
                dashProgressBar.setVisibility(View.GONE);
            }
        });
    }

    private void fetchUdhaarLedger() {
        api.getData("transactions", new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                dashProgressBar.setVisibility(View.GONE);
                JSONArray records = response.optJSONArray("records");
                double totalGiven = 0.0;
                double totalGot = 0.0;

                if (records != null) {
                    for (int i = 0; i < records.length(); i++) {
                        JSONObject r = records.optJSONObject(i);
                        if (r != null) {
                            JSONObject data = r.optJSONObject("data");
                            if (data != null) {
                                String type = data.optString("type", "");
                                double amount = data.optDouble("amount", 0.0);
                                if ("give".equalsIgnoreCase(type)) {
                                    totalGiven += amount;
                                } else {
                                    totalGot += amount;
                                }
                            }
                        }
                    }
                }

                double netUdhaar = BusinessLogic.calculateCustomerBalance(totalGiven, totalGot);
                if (netUdhaar < 0) netUdhaar = 0;
                tvTotalUdhaar.setText(BusinessLogic.getFormattedCurrency(netUdhaar));
            }

            @Override
            public void onError(String errorMessage) {
                dashProgressBar.setVisibility(View.GONE);
            }
        });
    }
}