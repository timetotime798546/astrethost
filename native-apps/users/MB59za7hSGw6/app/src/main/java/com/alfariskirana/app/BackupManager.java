package com.alfariskirana.app;

import android.content.Context;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.Locale;

public class BackupManager {

    public static String exportAllDataToCsv(Context context, DatabaseHelper dbHelper) {
        try {
            File exportDir = context.getExternalFilesDir(null);
            if (exportDir == null) {
                exportDir = context.getFilesDir();
            }
            File file = new File(exportDir, "alfaris_kirana_backup.csv");
            FileOutputStream fos = new FileOutputStream(file);
            OutputStreamWriter osw = new OutputStreamWriter(fos);

            // Export Products
            osw.write("=== PRODUCTS ===\n");
            osw.write("ID,Name,Category,CostPrice,SellingPrice,Stock,Unit\n");
            List<Product> products = dbHelper.getAllProducts("");
            for (int i = 0; i < products.size(); i++) {
                Product p = products.get(i);
                osw.write(String.format(Locale.getDefault(), "%d,\"%s\",\"%s\",%.2f,%.2f,%.2f,\"%s\"\n",
                        p.getId(), p.getName(), p.getCategory(), p.getPurchasePrice(), p.getSellingPrice(), p.getStock(), p.getUnit()));
            }

            // Export Customers
            osw.write("\n=== CUSTOMERS & KHATA ===\n");
            osw.write("ID,Name,Mobile,PendingBalance\n");
            List<Customer> customers = dbHelper.getAllCustomers("");
            for (int i = 0; i < customers.size(); i++) {
                Customer c = customers.get(i);
                osw.write(String.format(Locale.getDefault(), "%d,\"%s\",\"%s\",%.2f\n",
                        c.getId(), c.getName(), c.getMobile(), c.getPendingBalance()));
            }

            // Export Expenses
            osw.write("\n=== EXPENSES ===\n");
            osw.write("ID,Category,Amount,Date,Notes\n");
            List<Expense> expenses = dbHelper.getAllExpenses();
            for (int i = 0; i < expenses.size(); i++) {
                Expense e = expenses.get(i);
                osw.write(String.format(Locale.getDefault(), "%d,\"%s\",%.2f,\"%s\",\"%s\"\n",
                        e.getId(), e.getCategory(), e.getAmount(), e.getExpenseDate(), e.getNotes()));
            }

            osw.flush();
            osw.close();
            fos.close();
            return file.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}