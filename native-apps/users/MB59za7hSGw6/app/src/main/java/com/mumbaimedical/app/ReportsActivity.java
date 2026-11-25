package com.mumbaimedical.app;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import java.util.Locale;

public class ReportsActivity extends Activity {

    private DatabaseHelper dbHelper;
    private TextView tvReportSales, tvReportPurchases, tvReportProfit, tvReportValuation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);

        dbHelper = new DatabaseHelper(this);

        tvReportSales = findViewById(R.id.tvReportSales);
        tvReportPurchases = findViewById(R.id.tvReportPurchases);
        tvReportProfit = findViewById(R.id.tvReportProfit);
        tvReportValuation = findViewById(R.id.tvReportValuation);

        double sales = dbHelper.getTodaySalesTotal();
        double purchases = dbHelper.getTodayPurchasesTotal();
        double profit = dbHelper.getTodayProfit();
        double valuation = dbHelper.getTotalStockValue();

        tvReportSales.setText(String.format(Locale.getDefault(), "Total Sales Today: ₹ %.2f", sales));
        tvReportPurchases.setText(String.format(Locale.getDefault(), "Total Purchases Today: ₹ %.2f", purchases));
        tvReportProfit.setText(String.format(Locale.getDefault(), "Gross Profit Today: ₹ %.2f", profit));
        tvReportValuation.setText(String.format(Locale.getDefault(), "Total Inventory Valuation: ₹ %.2f", valuation));
    }
}