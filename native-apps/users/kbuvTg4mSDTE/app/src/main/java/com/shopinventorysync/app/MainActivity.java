package com.shopinventorysync.app;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;

public class MainActivity extends Activity {

    private TextView txtSyncIndicator, txtTotalValuation, txtTotalProducts, txtLowStockCount;
    private Button btnManageInventory, btnBillingCounter, btnSalesHistory, btnForceRefresh, btnLogout;
    private LinearLayout layoutAlertCard;

    private BackendApi api;
    private ProgressDialog progressDialog;

    private ArrayList<Product> productList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        api = new BackendApi(this);

        txtSyncIndicator = (TextView) findViewById(R.id.txtSyncIndicator);
        txtTotalValuation = (TextView) findViewById(R.id.txtTotalValuation);
        txtTotalProducts = (TextView) findViewById(R.id.txtTotalProducts);
        txtLowStockCount = (TextView) findViewById(R.id.txtLowStockCount);
        layoutAlertCard = (LinearLayout) findViewById(R.id.layoutAlertCard);

        btnManageInventory = (Button) findViewById(R.id.btnManageInventory);
        btnBillingCounter = (Button) findViewById(R.id.btnBillingCounter);
        btnSalesHistory = (Button) findViewById(R.id.btnSalesHistory);
        btnForceRefresh = (Button) findViewById(R.id.btnForceRefresh);
        btnLogout = (Button) findViewById(R.id.btnLogout);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Synchronizing...");
        progressDialog.setCancelable(false);

        // Manage button click
        btnManageInventory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, InventoryActivity.class));
            }
        });

        // Billing counter
        btnBillingCounter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, BillingActivity.class));
            }
        });

        // Sales history
        btnSalesHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SalesHistoryActivity.class));
            }
        });

        // Manual reload
        btnForceRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadCloudData();
            }
        });

        // Logout action
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                api.logout();
                Toast.makeText(MainActivity.this, "Logged out safely", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
                finish();
            }
        });

        // Load data first from offline memory-cache
        loadFromCache();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Load cloud inventory updates automatically on resume
        loadCloudData();
    }

    private void loadFromCache() {
        try {
            JSONArray cachedArr = OfflineCache.getCachedProducts(this);
            processProducts(cachedArr);
            txtSyncIndicator.setText("Stock Sync: Cached");
            txtSyncIndicator.setBackgroundColor(0xFF888888);
            txtSyncIndicator.setTextColor(0xFFFFFFFF);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadCloudData() {
        progressDialog.show();
        api.getRecords("products", new BackendApi.ApiCallback<JSONArray>() {
            @Override
            public void onSuccess(JSONArray result) {
                progressDialog.dismiss();
                OfflineCache.cacheProducts(MainActivity.this, result);
                processProducts(result);

                // Online indicator style
                txtSyncIndicator.setText("Stock Sync: Live");
                txtSyncIndicator.setBackgroundColor(0xFF004D40);
                txtSyncIndicator.setTextColor(0xFFA7FFEB);
            }

            @Override
            public void onError(String error) {
                progressDialog.dismiss();
                // Network error - report offline sync status
                txtSyncIndicator.setText("Stock Sync: Offline");
                txtSyncIndicator.setBackgroundColor(0xFFD32F2F);
                txtSyncIndicator.setTextColor(0xFFFFFFFF);
                Toast.makeText(MainActivity.this, "Offline Mode. Displaying cached stock.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void processProducts(JSONArray productsArray) {
        productList.clear();
        double totalValuation = 0;
        int lowStockCount = 0;

        for (int i = 0; i < productsArray.length(); i++) {
            try {
                JSONObject obj = productsArray.getJSONObject(i);
                Product prod = new Product(obj);
                productList.add(prod);

                // LOCAL CALCULATION: valuation sum
                totalValuation += prod.getValuation();

                // LOCAL CALCULATION: low stock counter check
                if (prod.isLowStock()) {
                    lowStockCount++;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Format metrics views
        txtTotalProducts.setText(productList.size() + " Items");
        txtTotalValuation.setText(String.format("$%.2f", totalValuation));

        if (lowStockCount > 0) {
            txtLowStockCount.setText(lowStockCount + " Products are critical / below limits!");
            layoutAlertCard.setBackgroundColor(0xFFFFEBEE);
        } else {
            txtLowStockCount.setText("All inventory amounts satisfy configured bounds.");
            layoutAlertCard.setBackgroundColor(0xFFFFFFFF);
        }
    }
}