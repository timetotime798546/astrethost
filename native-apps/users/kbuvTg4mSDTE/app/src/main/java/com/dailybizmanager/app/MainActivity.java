package com.dailybizmanager.app;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {

    private DatabaseHelper dbHelper;
    private TextView txtShopName, txtTodaySales, txtTodayPurchases, txtTodayExpenses, txtTodayProfit, txtLowStock, txtPendingPayments;
    private String currency = "$";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);

        // Views
        txtShopName = (TextView) findViewById(R.id.txtShopName);
        txtTodaySales = (TextView) findViewById(R.id.txtTodaySales);
        txtTodayPurchases = (TextView) findViewById(R.id.txtTodayPurchases);
        txtTodayExpenses = (TextView) findViewById(R.id.txtTodayExpenses);
        txtTodayProfit = (TextView) findViewById(R.id.txtTodayProfit);
        txtLowStock = (TextView) findViewById(R.id.txtLowStock);
        txtPendingPayments = (TextView) findViewById(R.id.txtPendingPayments);

        // Buttons
        Button btnQuickSale = (Button) findViewById(R.id.btnQuickSale);
        Button btnQuickProduct = (Button) findViewById(R.id.btnQuickProduct);
        Button btnProducts = (Button) findViewById(R.id.btnProducts);
        Button btnCustomers = (Button) findViewById(R.id.btnCustomers);
        Button btnPurchases = (Button) findViewById(R.id.btnPurchases);
        Button btnExpenses = (Button) findViewById(R.id.btnExpenses);
        Button btnReports = (Button) findViewById(R.id.btnReports);
        Button btnSettings = (Button) findViewById(R.id.btnSettings);

        // Click Handlers
        btnQuickSale.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SalesActivity.class));
            }
        });

        btnQuickProduct.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, AddProductActivity.class));
            }
        });

        btnProducts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, ProductsActivity.class));
            }
        });

        btnCustomers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, CustomersActivity.class));
            }
        });

        btnPurchases.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, PurchasesActivity.class));
            }
        });

        btnExpenses.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, ExpensesActivity.class));
            }
        });

        btnReports.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, ReportsActivity.class));
            }
        });

        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            }
        });

        // Set low stock indicator click
        txtLowStock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, LowStockActivity.class));
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDashboardData();
    }

    private void loadDashboardData() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Load Settings
        Cursor settingsCursor = db.rawQuery("SELECT shop_name, currency FROM settings LIMIT 1", null);
        if (settingsCursor.moveToFirst()) {
            txtShopName.setText(settingsCursor.getString(0));
            currency = settingsCursor.getString(1);
        }
        settingsCursor.close();

        // Get Current Date
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        // 1. Today's Sales
        double salesTotal = 0;
        Cursor salesCursor = db.rawQuery("SELECT SUM(total) FROM sales WHERE date = ?", new String[]{today});
        if (salesCursor.moveToFirst()) {
            salesTotal = salesCursor.getDouble(0);
        }
        salesCursor.close();
        txtTodaySales.setText(currency + String.format("%.2f", salesTotal));

        // 2. Today's Purchases
        double purchaseTotal = 0;
        Cursor purchaseCursor = db.rawQuery("SELECT SUM(quantity * purchase_price) FROM purchases WHERE date = ?", new String[]{today});
        if (purchaseCursor.moveToFirst()) {
            purchaseTotal = purchaseCursor.getDouble(0);
        }
        purchaseCursor.close();
        txtTodayPurchases.setText(currency + String.format("%.2f", purchaseTotal));

        // 3. Today's Expenses
        double expenseTotal = 0;
        Cursor expenseCursor = db.rawQuery("SELECT SUM(amount) FROM expenses WHERE date = ?", new String[]{today});
        if (expenseCursor.moveToFirst()) {
            expenseTotal = expenseCursor.getDouble(0);
        }
        expenseCursor.close();
        txtTodayExpenses.setText(currency + String.format("%.2f", expenseTotal));

        // 4. Today's Profit (Calculated dynamically offline)
        // Profit = Sales - Purchases cost of items sold (Estimated Profit) or direct differential
        double todayProfit = salesTotal - purchaseTotal - expenseTotal;
        txtTodayProfit.setText(currency + String.format("%.2f", todayProfit));

        // 5. Low Stock Count
        int lowStockCount = 0;
        Cursor stockCursor = db.rawQuery("SELECT COUNT(*) FROM products WHERE stock <= min_stock", null);
        if (stockCursor.moveToFirst()) {
            lowStockCount = stockCursor.getInt(0);
        }
        stockCursor.close();
        txtLowStock.setText(lowStockCount + " Items");

        // 6. Outstanding customer balance (Pending Payments)
        double outstanding = 0;
        Cursor customerCursor = db.rawQuery("SELECT SUM(balance) FROM customers", null);
        if (customerCursor.moveToFirst()) {
            outstanding = customerCursor.getDouble(0);
        }
        customerCursor.close();
        txtPendingPayments.setText(currency + String.format("%.2f", outstanding));
    }
}