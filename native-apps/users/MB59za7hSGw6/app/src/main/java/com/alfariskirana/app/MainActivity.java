package com.alfariskirana.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import java.util.Locale;

public class MainActivity extends Activity {

    private DatabaseHelper dbHelper;
    private TextView tvTodaySales;
    private TextView tvPendingUdhaar;
    private TextView tvTotalProducts;
    private TextView tvLowStock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);

        tvTodaySales = (TextView) findViewById(R.id.tv_today_sales);
        tvPendingUdhaar = (TextView) findViewById(R.id.tv_pending_udhaar);
        tvTotalProducts = (TextView) findViewById(R.id.tv_total_products);
        tvLowStock = (TextView) findViewById(R.id.tv_low_stock);

        Button btnQuickSale = (Button) findViewById(R.id.btn_quick_sale);
        Button btnProducts = (Button) findViewById(R.id.btn_products);
        Button btnCustomers = (Button) findViewById(R.id.btn_customers);
        Button btnPurchases = (Button) findViewById(R.id.btn_purchases);
        Button btnExpenses = (Button) findViewById(R.id.btn_expenses);
        Button btnReports = (Button) findViewById(R.id.btn_reports);

        btnQuickSale.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, NewSaleActivity.class);
                startActivity(intent);
            }
        });

        btnProducts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ProductListActivity.class);
                startActivity(intent);
            }
        });

        btnCustomers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, CustomerListActivity.class);
                startActivity(intent);
            }
        });

        btnPurchases.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, PurchaseListActivity.class);
                startActivity(intent);
            }
        });

        btnExpenses.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ExpenseListActivity.class);
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
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStats();
    }

    private void refreshStats() {
        double todaySales = dbHelper.getTodaySalesTotal();
        double pendingUdhaar = dbHelper.getTotalUdhaarBalance();
        int totalProducts = dbHelper.getProductCount();
        int lowStock = dbHelper.getLowStockCount();

        tvTodaySales.setText(String.format(Locale.getDefault(), "₹ %.2f", todaySales));
        tvPendingUdhaar.setText(String.format(Locale.getDefault(), "₹ %.2f", pendingUdhaar));
        tvTotalProducts.setText(String.valueOf(totalProducts));
        tvLowStock.setText(String.valueOf(lowStock));
    }
}