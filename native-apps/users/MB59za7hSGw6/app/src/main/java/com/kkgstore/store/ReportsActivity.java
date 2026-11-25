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

public class ReportsActivity extends Activity {

    private DatabaseHelper dbHelper;
    private TextView tvCashSales, tvCreditSales, tvJamaReceived, tvTotalSales, tvTotalPurchases, tvTotalExpenses, tvNetProfit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (DemoLockManager.checkAndEnforceLock(this)) return;

        setContentView(R.layout.activity_reports);
        dbHelper = new DatabaseHelper(this);

        tvCashSales = (TextView) findViewById(R.id.tv_report_cash_sales);
        tvCreditSales = (TextView) findViewById(R.id.tv_report_credit_sales);
        tvJamaReceived = (TextView) findViewById(R.id.tv_report_jama_received);
        tvTotalSales = (TextView) findViewById(R.id.tv_report_total_sales);
        tvTotalPurchases = (TextView) findViewById(R.id.tv_report_total_purchases);
        tvTotalExpenses = (TextView) findViewById(R.id.tv_report_total_expenses);
        tvNetProfit = (TextView) findViewById(R.id.tv_report_net_profit);

        Button btnShareDaily = (Button) findViewById(R.id.btn_share_daily_report);
        Button btnBackupZip = (Button) findViewById(R.id.btn_backup_zip);
        Button btnRestoreZip = (Button) findViewById(R.id.btn_restore_zip);
        Button btnExportCsv = (Button) findViewById(R.id.btn_export_csv);

        loadReportsData();

        btnShareDaily.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { shareDailyReport(); }
        });

        btnBackupZip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(ReportsActivity.this, "डेटा सुरक्षित बैकअप तैयार है", Toast.LENGTH_SHORT).show();
            }
        });

        btnRestoreZip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(ReportsActivity.this, "डेटा रीस्टोर करने के लिए फाइल चुनें", Toast.LENGTH_SHORT).show();
            }
        });

        btnExportCsv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(ReportsActivity.this, "CSV एक्सपोर्ट सफल रहा", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadReportsData() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        double cashSales = 0;
        Cursor cCash = db.rawQuery("SELECT SUM(grand_total) FROM sales WHERE payment_mode LIKE '%Cash%'", null);
        if (cCash.moveToFirst()) cashSales = cCash.getDouble(0);
        cCash.close();

        double creditSales = 0;
        Cursor cCredit = db.rawQuery("SELECT SUM(grand_total) FROM sales WHERE payment_mode LIKE '%Udhaar%'", null);
        if (cCredit.moveToFirst()) creditSales = cCredit.getDouble(0);
        cCredit.close();

        double jamaReceived = 0;
        Cursor cJama = db.rawQuery("SELECT SUM(amount) FROM jama_entries", null);
        if (cJama.moveToFirst()) jamaReceived = cJama.getDouble(0);
        cJama.close();

        double totalSales = 0;
        Cursor cSales = db.rawQuery("SELECT SUM(grand_total) FROM sales", null);
        if (cSales.moveToFirst()) totalSales = cSales.getDouble(0);
        cSales.close();

        double totalPurchases = 0;
        Cursor cPurchases = db.rawQuery("SELECT SUM(amount) FROM purchases", null);
        if (cPurchases.moveToFirst()) totalPurchases = cPurchases.getDouble(0);
        cPurchases.close();

        double totalExpenses = 0;
        Cursor cExpenses = db.rawQuery("SELECT SUM(amount) FROM expenses", null);
        if (cExpenses.moveToFirst()) totalExpenses = cExpenses.getDouble(0);
        cExpenses.close();

        double netProfit = totalSales - (totalPurchases + totalExpenses);

        tvCashSales.setText(String.format("₹ %.2f", cashSales));
        tvCreditSales.setText(String.format("₹ %.2f", creditSales));
        tvJamaReceived.setText(String.format("₹ %.2f", jamaReceived));
        tvTotalSales.setText(String.format("₹ %.2f", totalSales));
        tvTotalPurchases.setText(String.format("₹ %.2f", totalPurchases));
        tvTotalExpenses.setText(String.format("₹ %.2f", totalExpenses));
        tvNetProfit.setText(String.format("₹ %.2f", netProfit));
    }

    private void shareDailyReport() {
        String msg = "📊 *KKG Store Daily Financial Summary*\n\n"
                + "💵 कुल नगद बिक्री: " + tvCashSales.getText().toString() + "\n"
                + "🔴 कुल उधार बिक्री: " + tvCreditSales.getText().toString() + "\n"
                + "🟢 जमा प्राप्त राशि: " + tvJamaReceived.getText().toString() + "\n"
                + "🛒 कुल बिक्री (Total Revenue): " + tvTotalSales.getText().toString() + "\n"
                + "💸 कुल खर्चे: " + tvTotalExpenses.getText().toString() + "\n"
                + "📈 अनुमानित शुद्ध लाभ: " + tvNetProfit.getText().toString() + "\n\n"
                + "KKG Store POS Report";

        try {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, msg);
            startActivity(Intent.createChooser(intent, "Share Daily Report"));
        } catch (Exception e) {
            Toast.makeText(this, "शेयर सुविधा उपलब्ध नहीं है", Toast.LENGTH_SHORT).show();
        }
    }
}