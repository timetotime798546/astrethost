package com.alfariskirana.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Locale;

public class ReportsActivity extends Activity {

    private DatabaseHelper dbHelper;
    private TextView tvTotalSales;
    private TextView tvTotalPurchases;
    private TextView tvTotalExpenses;
    private TextView tvNetProfit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);

        dbHelper = new DatabaseHelper(this);

        tvTotalSales = (TextView) findViewById(R.id.tv_report_total_sales);
        tvTotalPurchases = (TextView) findViewById(R.id.tv_report_total_purchases);
        tvTotalExpenses = (TextView) findViewById(R.id.tv_report_total_expenses);
        tvNetProfit = (TextView) findViewById(R.id.tv_report_net_profit);
        Button btnExport = (Button) findViewById(R.id.btn_export_csv);

        btnExport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String path = BackupManager.exportAllDataToCsv(ReportsActivity.this, dbHelper);
                if (path != null) {
                    Toast.makeText(ReportsActivity.this, "CSV exported: " + path, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(ReportsActivity.this, "Failed to export data", Toast.LENGTH_SHORT).show();
                }
            }
        });

        calculateMetrics();
    }

    private void calculateMetrics() {
        double sales = dbHelper.getTotalSalesAllTime();
        double purchases = dbHelper.getTotalPurchasesAllTime();
        double expenses = dbHelper.getTotalExpensesAllTime();
        double netMargin = sales - (purchases + expenses);

        tvTotalSales.setText(String.format(Locale.getDefault(), "₹ %.2f", sales));
        tvTotalPurchases.setText(String.format(Locale.getDefault(), "₹ %.2f", purchases));
        tvTotalExpenses.setText(String.format(Locale.getDefault(), "₹ %.2f", expenses));
        tvNetProfit.setText(String.format(Locale.getDefault(), "₹ %.2f", netMargin));

        if (netMargin >= 0) {
            tvNetProfit.setTextColor(getResources().getColor(R.color.success));
        } else {
            tvNetProfit.setTextColor(getResources().getColor(R.color.danger));
        }
    }
}