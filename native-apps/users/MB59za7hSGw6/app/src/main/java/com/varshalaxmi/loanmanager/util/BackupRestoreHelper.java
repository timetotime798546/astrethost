package com.varshalaxmi.loanmanager.util;

import android.content.Context;
import com.varshalaxmi.loanmanager.db.DatabaseHelper;
import com.varshalaxmi.loanmanager.model.Customer;
import com.varshalaxmi.loanmanager.model.Loan;
import com.varshalaxmi.loanmanager.model.Payment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class BackupRestoreHelper {

    public static File createZipBackup(Context context, DatabaseHelper dbHelper) throws Exception {
        JSONObject backupJson = new JSONObject();

        // Backup Customers
        List<Customer> customers = dbHelper.getAllCustomers(null);
        JSONArray custArray = new JSONArray();
        for (Customer c : customers) {
            JSONObject obj = new JSONObject();
            obj.put("id", c.getId());
            obj.put("name", c.getName());
            obj.put("mobile", c.getMobile());
            obj.put("address", c.getAddress());
            obj.put("created_at", c.getCreatedAt());
            custArray.put(obj);
        }
        backupJson.put("customers", custArray);

        // Backup Loans
        List<Loan> loans = dbHelper.getAllLoans();
        JSONArray loanArray = new JSONArray();
        for (Loan l : loans) {
            JSONObject obj = new JSONObject();
            obj.put("id", l.getId());
            obj.put("customer_id", l.getCustomerId());
            obj.put("principal_amount", l.getPrincipalAmount());
            obj.put("interest_rate", l.getInterestRate());
            obj.put("interest_method", l.getInterestMethod());
            obj.put("issue_date", l.getIssueDate());
            obj.put("notes", l.getNotes());
            obj.put("status", l.getStatus());
            loanArray.put(obj);
        }
        backupJson.put("loans", loanArray);

        // Backup Payments
        List<Payment> payments = dbHelper.getAllPayments();
        JSONArray payArray = new JSONArray();
        for (Payment p : payments) {
            JSONObject obj = new JSONObject();
            obj.put("id", p.getId());
            obj.put("loan_id", p.getLoanId());
            obj.put("customer_id", p.getCustomerId());
            obj.put("amount", p.getAmount());
            obj.put("payment_date", p.getPaymentDate());
            obj.put("note", p.getNote());
            payArray.put(obj);
        }
        backupJson.put("payments", payArray);

        // Compress into VarshaLaxmi_Backup.zip file
        File backupDir = new File(context.getExternalFilesDir(null), "backups");
        if (!backupDir.exists()) backupDir.mkdirs();

        File zipFile = new File(backupDir, "VarshaLaxmi_Backup.zip");
        FileOutputStream fos = new FileOutputStream(zipFile);
        ZipOutputStream zos = new ZipOutputStream(fos);

        ZipEntry entry = new ZipEntry("varshalaxmi_data.json");
        zos.putNextEntry(entry);
        zos.write(backupJson.toString(2).getBytes("UTF-8"));
        zos.closeEntry();
        zos.close();

        return zipFile;
    }

    public static boolean restoreFromZip(Context context, File zipFile, DatabaseHelper dbHelper) {
        try {
            FileInputStream fis = new FileInputStream(zipFile);
            ZipInputStream zis = new ZipInputStream(fis);
            ZipEntry entry;
            String jsonStr = null;

            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().endsWith(".json")) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        baos.write(buffer, 0, len);
                    }
                    jsonStr = baos.toString("UTF-8");
                    break;
                }
            }
            zis.close();

            if (jsonStr == null) return false;

            JSONObject root = new JSONObject(jsonStr);

            // Import Customers
            if (root.has("customers")) {
                JSONArray custArray = root.getJSONArray("customers");
                for (int i = 0; i < custArray.length(); i++) {
                    JSONObject c = custArray.getJSONObject(i);
                    dbHelper.addCustomer(c.optString("name"), c.optString("mobile"), c.optString("address"));
                }
            }

            // Import Loans
            if (root.has("loans")) {
                JSONArray loanArray = root.getJSONArray("loans");
                for (int i = 0; i < loanArray.length(); i++) {
                    JSONObject l = loanArray.getJSONObject(i);
                    dbHelper.addLoan(
                            l.optLong("customer_id"),
                            l.optDouble("principal_amount"),
                            l.optDouble("interest_rate"),
                            l.optString("interest_method"),
                            l.optString("issue_date"),
                            l.optString("notes")
                    );
                }
            }

            // Import Payments
            if (root.has("payments")) {
                JSONArray payArray = root.getJSONArray("payments");
                for (int i = 0; i < payArray.length(); i++) {
                    JSONObject p = payArray.getJSONObject(i);
                    dbHelper.addPayment(
                            p.optLong("loan_id"),
                            p.optLong("customer_id"),
                            p.optDouble("amount"),
                            p.optString("note")
                    );
                }
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}