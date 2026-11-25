package com.kkgstore.store;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ReportsActivity extends Activity {

    private static final int REQUEST_PICK_RESTORE_ZIP = 201;

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
                createFullZipBackup();
            }
        });

        btnRestoreZip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchRestoreZipPicker();
            }
        });

        btnExportCsv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exportCsvSummary();
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

    private void createFullZipBackup() {
        try {
            File backupDir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            if (backupDir == null) backupDir = getFilesDir();

            String backupFileName = "kkg_store_backup_" + System.currentTimeMillis() + ".zip";
            File zipFile = new File(backupDir, backupFileName);

            FileOutputStream fos = new FileOutputStream(zipFile);
            ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(fos));

            // 1. Pack Database file
            File dbFile = getDatabasePath("kkg_store.db");
            if (dbFile.exists()) {
                addFileToZip(zos, dbFile, "kkg_store.db");
            }

            // 2. Pack all Udhar Photos folder & files
            File photosDir = ImageUtils.getPhotosDir(this);
            if (photosDir.exists() && photosDir.isDirectory()) {
                File[] photos = photosDir.listFiles();
                if (photos != null) {
                    for (File photo : photos) {
                        if (photo.isFile()) {
                            addFileToZip(zos, photo, "udhar_photos/" + photo.getName());
                        }
                    }
                }
            }

            zos.close();
            fos.close();

            Toast.makeText(this, "बैकअप पूरा हुआ: " + zipFile.getName(), Toast.LENGTH_LONG).show();

            // Share zip file via chooser
            try {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("application/zip");
                Uri zipUri = Uri.fromFile(zipFile);
                shareIntent.putExtra(Intent.EXTRA_STREAM, zipUri);
                startActivity(Intent.createChooser(shareIntent, "Share Backup ZIP File"));
            } catch (Exception ignored) {}

        } catch (Exception e) {
            Toast.makeText(this, "बैकअप बनाने में त्रुटि: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void addFileToZip(ZipOutputStream zos, File file, String entryName) throws Exception {
        FileInputStream fis = new FileInputStream(file);
        BufferedInputStream bis = new BufferedInputStream(fis);
        ZipEntry entry = new ZipEntry(entryName);
        zos.putNextEntry(entry);

        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = bis.read(buffer)) != -1) {
            zos.write(buffer, 0, bytesRead);
        }
        zos.closeEntry();
        bis.close();
        fis.close();
    }

    private void launchRestoreZipPicker() {
        try {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            startActivityForResult(Intent.createChooser(intent, "Select Backup ZIP File"), REQUEST_PICK_RESTORE_ZIP);
        } catch (Exception e) {
            Toast.makeText(this, "फाइल पिकर खोलने में समस्या", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_RESTORE_ZIP && resultCode == RESULT_OK && data != null && data.getData() != null) {
            restoreZipFromUri(data.getData());
        }
    }

    private void restoreZipFromUri(Uri zipUri) {
        try {
            InputStream is = getContentResolver().openInputStream(zipUri);
            if (is == null) {
                Toast.makeText(this, "जिप फाइल पढ़ने में विफल", Toast.LENGTH_SHORT).show();
                return;
            }

            ZipInputStream zis = new ZipInputStream(new BufferedInputStream(is));
            ZipEntry ze;

            File targetDbFile = getDatabasePath("kkg_store.db");
            File photosDir = ImageUtils.getPhotosDir(this);

            dbHelper.close();

            int restoredPhotosCount = 0;
            boolean dbRestored = false;

            byte[] buffer = new byte[4096];
            while ((ze = zis.getNextEntry()) != null) {
                String name = ze.getName();
                if (name.equals("kkg_store.db") || name.endsWith(".db")) {
                    FileOutputStream fos = new FileOutputStream(targetDbFile);
                    int count;
                    while ((count = zis.read(buffer)) != -1) {
                        fos.write(buffer, 0, count);
                    }
                    fos.close();
                    dbRestored = true;
                } else if (name.startsWith("udhar_photos/") || name.endsWith(".jpg") || name.endsWith(".jpeg")) {
                    String fileName = new File(name).getName();
                    if (!fileName.isEmpty()) {
                        File destPhotoFile = new File(photosDir, fileName);
                        FileOutputStream fos = new FileOutputStream(destPhotoFile);
                        int count;
                        while ((count = zis.read(buffer)) != -1) {
                            fos.write(buffer, 0, count);
                        }
                        fos.close();
                        restoredPhotosCount++;
                    }
                }
                zis.closeEntry();
            }
            zis.close();
            is.close();

            dbHelper = new DatabaseHelper(this);
            loadReportsData();

            if (dbRestored) {
                Toast.makeText(this, "डेटा रीस्टोर सफल! " + restoredPhotosCount + " फोटो पुनर्प्राप्त हुए।", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "वैध बैकअप डेटाबेस नहीं मिला", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Toast.makeText(this, "रीस्टोर विफल रहा: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void exportCsvSummary() {
        try {
            File docsDir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            if (docsDir == null) docsDir = getFilesDir();
            File csvFile = new File(docsDir, "kkg_store_summary.csv");

            FileOutputStream out = new FileOutputStream(csvFile);
            StringBuilder sb = new StringBuilder();
            sb.append("Metric,Amount\n");
            sb.append("Cash Sales,").append(tvCashSales.getText().toString().replace("₹ ", "")).append("\n");
            sb.append("Credit Sales,").append(tvCreditSales.getText().toString().replace("₹ ", "")).append("\n");
            sb.append("Jama Received,").append(tvJamaReceived.getText().toString().replace("₹ ", "")).append("\n");
            sb.append("Total Revenue,").append(tvTotalSales.getText().toString().replace("₹ ", "")).append("\n");
            sb.append("Total Procurement,").append(tvTotalPurchases.getText().toString().replace("₹ ", "")).append("\n");
            sb.append("Total Expenses,").append(tvTotalExpenses.getText().toString().replace("₹ ", "")).append("\n");
            sb.append("Net Margin,").append(tvNetProfit.getText().toString().replace("₹ ", "")).append("\n");

            out.write(sb.toString().getBytes());
            out.flush();
            out.close();

            Toast.makeText(this, "CSV एक्सपोर्ट सफल: " + csvFile.getName(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "CSV एक्सपोर्ट विफल रहा", Toast.LENGTH_SHORT).show();
        }
    }
}