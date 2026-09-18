package com.inventorysalesmanager.app;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.List;
import java.util.Locale;

public class ReportsActivity extends Activity {

    private DatabaseHelper dbHelper;

    private TextView tvGrossToday;
    private TextView tvLowStockCount;
    private TextView tvLifetimeSales;

    private LinearLayout lowStockContainer;
    private LinearLayout bestSellersContainer;
    private LinearLayout dailySummaryContainer;

    private TextView tvLowStockEmpty;
    private TextView tvBestSellersEmpty;
    private TextView tvDailyEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.style.AppTheme.class.getDeclaredFields().length > 0 ? R.layout.activity_reports : R.layout.activity_reports);

        dbHelper = new DatabaseHelper(this);

        tvGrossToday = (TextView) findViewById(R.id.report_gross_today);
        tvLowStockCount = (TextView) findViewById(R.id.report_low_stock_count);
        tvLifetimeSales = (TextView) findViewById(R.id.report_lifetime_sales);

        lowStockContainer = (LinearLayout) findViewById(R.id.report_low_stock_container);
        bestSellersContainer = (LinearLayout) findViewById(R.id.report_best_sellers_container);
        dailySummaryContainer = (LinearLayout) findViewById(R.id.report_daily_summary_container);

        tvLowStockEmpty = (TextView) findViewById(R.id.tv_low_stock_empty);
        tvBestSellersEmpty = (TextView) findViewById(R.id.tv_best_sellers_empty);
        tvDailyEmpty = (TextView) findViewById(R.id.tv_daily_empty);

        loadReports();
    }

    private void loadReports() {
        // Core metrics
        double grossToday = dbHelper.getTodaySalesSum();
        int lowStockCount = dbHelper.getLowStockProductsCount();
        List<Sale> allSales = dbHelper.getAllSales();

        tvGrossToday.setText(String.format(Locale.getDefault(), "$%.2f", grossToday));
        tvLowStockCount.setText(String.valueOf(lowStockCount));
        tvLifetimeSales.setText(allSales.size() + " transactions");

        // Dynamic render warnings
        renderLowStockWarnings();

        // Dynamic render best sellers
        renderBestSellers();

        // Dynamic render daily transactions history
        renderDailyHistory();
    }

    private void renderLowStockWarnings() {
        List<Product> lowStockItems = dbHelper.getLowStockProducts();
        lowStockContainer.removeAllViews();

        if (lowStockItems.isEmpty()) {
            lowStockContainer.addView(tvLowStockEmpty);
        } else {
            for (int i = 0; i < lowStockItems.size(); i++) {
                Product p = lowStockItems.get(i);
                TextView row = new TextView(this);
                row.setText(p.getName() + " (" + p.getSku() + ") - Stock: " + p.getStockQty() + " (Min: " + p.getMinStock() + ")");
                row.setTextColor(getResources().getColor(R.color.alert_red));
                row.setPadding(0, 6, 0, 6);
                row.setTextSize(13);
                lowStockContainer.addView(row);
            }
        }
    }

    private void renderBestSellers() {
        List<DatabaseHelper.ProductPerformance> bestSellers = dbHelper.getBestSellingProducts();
        bestSellersContainer.removeAllViews();

        if (bestSellers.isEmpty()) {
            bestSellersContainer.addView(tvBestSellersEmpty);
        } else {
            for (int i = 0; i < bestSellers.size(); i++) {
                DatabaseHelper.ProductPerformance item = bestSellers.get(i);
                LinearLayout rowLayout = new LinearLayout(this);
                rowLayout.setOrientation(LinearLayout.HORIZONTAL);
                rowLayout.setPadding(0, 8, 0, 8);

                TextView tvName = new TextView(this);
                tvName.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
                tvName.setText((i + 1) + ". " + item.productName);
                tvName.setTextColor(getResources().getColor(R.color.text_primary));
                tvName.setTextSize(13);
                tvName.setTypeface(null, Typeface.BOLD);

                TextView tvVolume = new TextView(this);
                tvVolume.setText(item.unitsSold + " sold ($" + String.format(Locale.getDefault(), "%.2f", item.revenue) + ")");
                tvVolume.setTextColor(getResources().getColor(R.color.text_secondary));
                tvVolume.setTextSize(13);

                rowLayout.addView(tvName);
                rowLayout.addView(tvVolume);
                bestSellersContainer.addView(rowLayout);
            }
        }
    }

    private void renderDailyHistory() {
        List<DatabaseHelper.DailySummary> list = dbHelper.getDailySalesSummary();
        dailySummaryContainer.removeAllViews();

        if (list.isEmpty()) {
            dailySummaryContainer.addView(tvDailyEmpty);
        } else {
            for (int i = 0; i < list.size(); i++) {
                DatabaseHelper.DailySummary item = list.get(i);
                LinearLayout rowLayout = new LinearLayout(this);
                rowLayout.setOrientation(LinearLayout.HORIZONTAL);
                rowLayout.setPadding(0, 8, 0, 8);

                TextView tvDate = new TextView(this);
                tvDate.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
                tvDate.setText(item.date);
                tvDate.setTextColor(getResources().getColor(R.color.text_primary));
                tvDate.setTextSize(13);

                TextView tvSummary = new TextView(this);
                tvSummary.setText(item.transactionCount + " txn / $" + String.format(Locale.getDefault(), "%.2f", item.totalRevenue));
                tvSummary.setTextColor(getResources().getColor(R.color.accent));
                tvSummary.setTypeface(null, Typeface.BOLD);
                tvSummary.setTextSize(13);

                rowLayout.addView(tvDate);
                rowLayout.addView(tvSummary);
                dailySummaryContainer.addView(rowLayout);
            }
        }
    }
}