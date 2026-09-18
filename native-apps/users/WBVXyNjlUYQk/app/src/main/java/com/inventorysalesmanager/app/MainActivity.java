package com.inventorysalesmanager.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import java.util.Locale;

public class MainActivity extends Activity {

    private TextView tvTotalProducts;
    private TextView tvTotalStock;
    private TextView tvTodaySales;
    private TextView tvLowStock;
    private View layoutLowStockAlert;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);

        tvTotalProducts = (TextView) findViewById(R.id.tv_total_products);
        tvTotalStock = (TextView) findViewById(R.id.tv_total_stock);
        tvTodaySales = (TextView) findViewById(R.id.tv_today_sales);
        tvLowStock = (TextView) findViewById(R.id.tv_low_stock);
        layoutLowStockAlert = findViewById(R.id.layout_low_stock_alert);

        Button btnProducts = (Button) findViewById(R.id.btn_products);
        Button btnSales = (Button) findViewById(R.id.btn_sales);
        Button btnReports = (Button) findViewById(R.id.btn_reports);

        btnProducts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ProductListActivity.class);
                startActivity(intent);
            }
        });

        btnSales.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SalesActivity.class);
                startActivity(intent);
            }
        });

        btnReports.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ReportsActivity.class);
                startActivity(intent);
            }
        });

        layoutLowStockAlert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ReportsActivity.class);
                intent.putExtra("show_low_stock_only", true);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshDashboardData();
    }

    private void refreshDashboardData() {
        int totalProducts = dbHelper.getTotalProductsCount();
        int totalStock = dbHelper.getTotalStockCount();
        double todaySalesSum = dbHelper.getTodaySalesSum();
        int lowStockCount = dbHelper.getLowStockProductsCount();

        tvTotalProducts.setText(String.valueOf(totalProducts));
        tvTotalStock.setText(String.valueOf(totalStock));
        tvTodaySales.setText(String.format(Locale.getDefault(), "$%.2f", todaySalesSum));
        tvLowStock.setText(lowStockCount + " Items");
    }
}