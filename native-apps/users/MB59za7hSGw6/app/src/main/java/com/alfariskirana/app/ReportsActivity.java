package com.alfariskirana.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Locale;

public class ReportsActivity extends Activity {

    private static final int REQUEST_CODE_CREATE_ZIP_BACKUP = 1001;
    private static final int REQUEST_CODE_OPEN_ZIP_RESTORE = 1002;

    private DatabaseHelper dbHelper;
    private TextView tvCashSales;
    private TextView tvCreditSales;
    private TextView tvJamaReceived;
    private TextView tvTotalSales;
    private TextView tvTotalPurchases;
    private TextView tvTotalExpenses;
    private TextView tvNetProfit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);

        dbHelper = new DatabaseHelper(this);

        tvCashSales = (TextView) findViewById(R.id.tv_report_cash_sales);
        tvCreditSales = (TextView) findViewById(R.id.tv_report_credit_sales);
        tvJamaReceived = (TextView) findViewById(R.id.tv_report_jama_received);
        tvTotalSales = (TextView) findViewById(R.id.tv_report_total_sales);
        tvTotalPurchases = (TextView) findViewById(R.id.tv_report_total_purchases);
        tvTotalExpenses = (TextView) findViewById(R.id.tv_report_total_expenses);
        tvNetProfit = (TextView) findViewById(R.id.tv_report_net_profit);

        Button btnShareDailyReport = (Button) findViewById(R.id.btn_share_daily_report);
        Button btnBackupZip = (Button) findViewById(R.id.btn_backup_zip);
        Button btnRestoreZip = (Button) findViewById(R.id.btn_restore_zip);
        Button btnExportCsv = (Button) findViewById(R.id.btn_export_csv);

        btnShareDailyReport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareDailyReport();
            }
        });

        btnBackupZip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerCreateZipBackup();
            }
        });

        btnRestoreZip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerOpenZipRestore();
            }
        });

        btnExportCsv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String path = BackupManager.exportAllDataToCsv(ReportsActivity.this, dbHelper);
                if (path != null) {
                    Toast.makeText(ReportsActivity.this, "CSV exported: " + path, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(ReportsActivity.this, "Failed to export CSV", Toast.LENGTH_SHORT).show();
                }
            }
        });

        calculateMetrics();
    }

    private void shareDailyReport() {
        double cashSales = dbHelper.getCashSalesTotal();
        double creditSales = dbHelper.getCreditSalesTotal();
        double jamaReceived = dbHelper.getTotalJamaReceivedAllTime();
        double sales = dbHelper.getTotalSalesAllTime();
        double purchases = dbHelper.getTotalPurchasesAllTime();
        double expenses = dbHelper.getTotalExpensesAllTime();
        double todaySales = dbHelper.getTodaySalesTotal();
        double pendingUdhaar = dbHelper.getTotalUdhaarBalance();
        double netMargin = sales - (purchases + expenses);

        StringBuilder sb = new StringBuilder();
        sb.append("📊 *AL FARIS KIRANA STORE - DAILY REPORT* 📊\n");
        sb.append("Date: ").append(DatabaseHelper.getTodayDate()).append("\n");
        sb.append("----------------------------------\n");
        sb.append("⚡ *Today's Sales Total (आज की बिक्री): ₹ ").append(String.format(Locale.getDefault(), "%.2f", todaySales)).append("*\n");
        sb.append("🔴 *Total Pending Udhaar (कुल बाकी उधार): ₹ ").append(String.format(Locale.getDefault(), "%.2f", pendingUdhaar)).append("*\n");
        sb.append("----------------------------------\n");
        sb.append("💵 *All-time Sales Summary:*\n");
        sb.append("• Cash Sales (नगद बिक्री): ₹ ").append(String.format(Locale.getDefault(), "%.2f", cashSales)).append("\n");
        sb.append("• Credit Sales (उधार बिक्री): ₹ ").append(String.format(Locale.getDefault(), "%.2f", creditSales)).append("\n");
        sb.append("• Jama Received (प्राप्त जमा): ₹ ").append(String.format(Locale.getDefault(), "%.2f", jamaReceived)).append("\n");
        sb.append("• All Sales Total: ₹ ").append(String.format(Locale.getDefault(), "%.2f", sales)).append("\n\n");
        sb.append("📦 *Stock Procurement & Expenses:*\n");
        sb.append("• Stock Purchases: ₹ ").append(String.format(Locale.getDefault(), "%.2f", purchases)).append("\n");
        sb.append("• Store Expenses: ₹ ").append(String.format(Locale.getDefault(), "%.2f", expenses)).append("\n\n");
        sb.append("✨ *Net Store Margin / Profit: ₹ ").append(String.format(Locale.getDefault(), "%.2f", netMargin)).append("*\n");
        sb.append("----------------------------------\n");
        sb.append("Generated by Al Faris Kirana POS");

        WhatsAppShareHelper.shareText(this, sb.toString(), "Share Daily Report via WhatsApp");
    }

    private void triggerCreateZipBackup() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/zip");
        String defaultFileName = "AlFarisKirana_Backup_" + DatabaseHelper.getTodayDate() + ".zip";
        intent.putExtra(Intent.EXTRA_TITLE, defaultFileName);
        try {
            startActivityForResult(intent, REQUEST_CODE_CREATE_ZIP_BACKUP);
        } catch (Exception e) {
            Toast.makeText(this, "Unable to launch system file picker: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void triggerOpenZipRestore() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        String[] mimeTypes = {"application/zip", "application/x-zip-compressed", "application/x-zip"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        try {
            startActivityForResult(intent, REQUEST_CODE_OPEN_ZIP_RESTORE);
        } catch (Exception e) {
            Toast.makeText(this, "Unable to launch system file picker: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null && data.getData() != null) {
            final Uri fileUri = data.getData();

            if (requestCode == REQUEST_CODE_CREATE_ZIP_BACKUP) {
                BackupManager.BackupResult result = BackupManager.createZipBackup(this, fileUri, dbHelper);
                if (result.success) {
                    Toast.makeText(this, result.message, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Backup Error: " + result.message, Toast.LENGTH_LONG).show();
                }
            } else if (requestCode == REQUEST_CODE_OPEN_ZIP_RESTORE) {
                new AlertDialog.Builder(this)
                        .setTitle("⚠️ Confirm Restore Data")
                        .setMessage("Restoring from this backup ZIP file will replace all existing products, customers, sales, and transaction records in Al Faris Kirana.\n\nAn automatic safety backup of your current database will be created before restoring.\n\nDo you want to proceed?")
                        .setPositiveButton("Yes, Restore Now", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                performDatabaseRestore(fileUri);
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        }
    }

    private void performDatabaseRestore(Uri zipUri) {
        BackupManager.BackupResult result = BackupManager.restoreZipBackup(this, zipUri, dbHelper);
        if (result.success) {
            Toast.makeText(this, result.message, Toast.LENGTH_LONG).show();
            calculateMetrics();
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("Restore Failed")
                    .setMessage(result.message)
                    .setPositiveButton("OK", null)
                    .show();
        }
    }

    private void calculateMetrics() {
        double cashSales = dbHelper.getCashSalesTotal();
        double creditSales = dbHelper.getCreditSalesTotal();
        double jamaReceived = dbHelper.getTotalJamaReceivedAllTime();
        double sales = dbHelper.getTotalSalesAllTime();
        double purchases = dbHelper.getTotalPurchasesAllTime();
        double expenses = dbHelper.getTotalExpensesAllTime();

        double netMargin = sales - (purchases + expenses);

        tvCashSales.setText(String.format(Locale.getDefault(), "₹ %.2f", cashSales));
        tvCreditSales.setText(String.format(Locale.getDefault(), "₹ %.2f", creditSales));
        tvJamaReceived.setText(String.format(Locale.getDefault(), "₹ %.2f", jamaReceived));
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