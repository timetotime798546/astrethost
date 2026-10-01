package com.dailybizmanager.app;

import android.app.Activity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class ReportsActivity extends Activity {

    private DatabaseHelper dbHelper;
    private EditText edtStartDate, edtEndDate;
    private TextView txtReportPeriodLabel, txtRepSales, txtRepPurchases, txtRepExpenses, txtRepProfit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);

        dbHelper = new DatabaseHelper(this);

        edtStartDate = (EditText) findViewById(R.id.edtStartDate);
        edtEndDate = (EditText) findViewById(R.id.edtEndDate);
        txtReportPeriodLabel = (TextView) findViewById(R.id.txtReportPeriodLabel);
        txtRepSales = (TextView) findViewById(R.id.txtRepSales);
        txtRepPurchases = (TextView) findViewById(R.id.txtRepPurchases);
        txtRepExpenses = (TextView) findViewById(R.id.txtRepExpenses);
        txtRepProfit = (TextView) findViewById(R.id.txtRepProfit);
        Button btnGenerateReport = (Button) findViewById(R.id.btnGenerateReport);

        // Pre-fill last 30 days
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar cal = Calendar.getInstance();
        String end = sdf.format(cal.getTime());
        cal.add(Calendar.DAY_OF_MONTH, -30);
        String start = sdf.format(cal.getTime());

        edtStartDate.setText(start);
        edtEndDate.setText(end);

        generateOfflineMetrics(start, end);

        btnGenerateReport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String s = edtStartDate.getText().toString().trim();
                String e = edtEndDate.getText().toString().trim();
                if (s.isEmpty() || e.isEmpty()) {
                    Toast.makeText(ReportsActivity.this, "Fill in both query dates", Toast.LENGTH_SHORT).show();
                    return;
                }
                generateOfflineMetrics(s, e);
            }
        });
    }

    private void generateOfflineMetrics(String start, String end) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        txtReportPeriodLabel.setText("Consolidated Range: " + start + " to " + end);

        // Sales Summation
        double salesTotal = 0;
        Cursor sCursor = db.rawQuery("SELECT SUM(total) FROM sales WHERE date >= ? AND date <= ?", new String[]{start, end});
        if (sCursor.moveToFirst()) {
            salesTotal = sCursor.getDouble(0);
        }
        sCursor.close();
        txtRepSales.setText("$" + String.format("%.2f", salesTotal));

        // Purchasing summation
        double purchaseTotal = 0;
        Cursor pCursor = db.rawQuery("SELECT SUM(quantity * purchase_price) FROM purchases WHERE date >= ? AND date <= ?", new String[]{start, end});
        if (pCursor.moveToFirst()) {
            purchaseTotal = pCursor.getDouble(0);
        }
        pCursor.close();
        txtRepPurchases.setText("$" + String.format("%.2f", purchaseTotal));

        // Expenses summation
        double expensesTotal = 0;
        Cursor eCursor = db.rawQuery("SELECT SUM(amount) FROM expenses WHERE date >= ? AND date <= ?", new String[]{start, end});
        if (eCursor.moveToFirst()) {
            expensesTotal = eCursor.getDouble(0);
        }
        eCursor.close();
        txtRepExpenses.setText("$" + String.format("%.2f", expensesTotal));

        // Estimated profit calculated locally
        double netProfit = salesTotal - purchaseTotal - expensesTotal;
        txtRepProfit.setText("$" + String.format("%.2f", netProfit));
    }
}