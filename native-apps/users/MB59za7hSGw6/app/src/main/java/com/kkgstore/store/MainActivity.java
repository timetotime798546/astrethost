package com.kkgstore.store;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {

    private TextView tvPendingUdhaar, tvTotalJama, tvTodaySales, tvTotalCustomers;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DemoLockManager.initInstallTimestamp(this);
        if (DemoLockManager.checkAndEnforceLock(this)) {
            return;
        }

        setContentView(R.layout.activity_main);
        dbHelper = new DatabaseHelper(this);

        tvPendingUdhaar = (TextView) findViewById(R.id.tv_pending_udhaar);
        tvTotalJama = (TextView) findViewById(R.id.tv_total_jama);
        tvTodaySales = (TextView) findViewById(R.id.tv_today_sales);
        tvTotalCustomers = (TextView) findViewById(R.id.tv_total_customers);

        Button btnQuickSale = (Button) findViewById(R.id.btn_quick_sale);
        Button btnCustomers = (Button) findViewById(R.id.btn_customers);
        Button btnProducts = (Button) findViewById(R.id.btn_products);
        Button btnPurchases = (Button) findViewById(R.id.btn_purchases);
        Button btnExpenses = (Button) findViewById(R.id.btn_expenses);
        Button btnReports = (Button) findViewById(R.id.btn_reports);
        Button btnShareSummary = (Button) findViewById(R.id.btn_share_summary);

        btnQuickSale.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, NewSaleActivity.class));
            }
        });

        btnCustomers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, CustomerListActivity.class));
            }
        });

        btnProducts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, ProductListActivity.class));
            }
        });

        btnPurchases.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, PurchaseListActivity.class));
            }
        });

        btnExpenses.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, ExpenseListActivity.class));
            }
        });

        btnReports.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, ReportsActivity.class));
            }
        });

        btnShareSummary.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareStoreSummary();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (DemoLockManager.checkAndEnforceLock(this)) {
            return;
        }
        loadDashboardMetrics();
    }

    private void loadDashboardMetrics() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        double totalUdhaar = 0;
        Cursor cUdhaar = db.rawQuery("SELECT SUM(amount) FROM udhaar_entries", null);
        if (cUdhaar.moveToFirst()) totalUdhaar += cUdhaar.getDouble(0);
        cUdhaar.close();

        Cursor cCustOpen = db.rawQuery("SELECT SUM(opening_balance) FROM customers WHERE is_active=1", null);
        if (cCustOpen.moveToFirst()) totalUdhaar += cCustOpen.getDouble(0);
        cCustOpen.close();

        double totalJama = 0;
        Cursor cJama = db.rawQuery("SELECT SUM(amount) FROM jama_entries", null);
        if (cJama.moveToFirst()) totalJama = cJama.getDouble(0);
        cJama.close();

        double pendingUdhaar = Math.max(0, totalUdhaar - totalJama);

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        double todaySales = 0;
        Cursor cSales = db.rawQuery("SELECT SUM(grand_total) FROM sales WHERE sale_date LIKE '" + today + "%'", null);
        if (cSales.moveToFirst()) todaySales = cSales.getDouble(0);
        cSales.close();

        int custCount = 0;
        Cursor cCustCount = db.rawQuery("SELECT COUNT(*) FROM customers WHERE is_active=1", null);
        if (cCustCount.moveToFirst()) custCount = cCustCount.getInt(0);
        cCustCount.close();

        tvPendingUdhaar.setText(String.format("₹ %.2f", pendingUdhaar));
        tvTotalJama.setText(String.format("₹ %.2f", totalJama));
        tvTodaySales.setText(String.format("₹ %.2f", todaySales));
        tvTotalCustomers.setText(String.valueOf(custCount));
    }

    private void shareStoreSummary() {
        String pending = tvPendingUdhaar.getText().toString();
        String jama = tvTotalJama.getText().toString();
        String today = tvTodaySales.getText().toString();
        String cust = tvTotalCustomers.getText().toString();

        String msg = "🏪 *KKG Store Overall Overview*\n\n"
                + "🔴 कुल बकाया उधार: " + pending + "\n"
                + "🟢 कुल प्राप्त जमा: " + jama + "\n"
                + "🛒 आज की कुल बिक्री: " + today + "\n"
                + "👥 कुल ग्राहक खाते: " + cust + "\n\n"
                + "धन्यवाद!";

        try {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, msg);
            startActivity(Intent.createChooser(intent, "Share via"));
        } catch (Exception e) {
            Toast.makeText(this, "शेयर सुविधा उपलब्ध नहीं है", Toast.LENGTH_SHORT).show();
        }
    }
}