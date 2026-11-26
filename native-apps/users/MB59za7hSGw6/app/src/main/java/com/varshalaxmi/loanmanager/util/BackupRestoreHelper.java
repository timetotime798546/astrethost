package com.varshalaxmi.loanmanager.util;

import android.content.Context;
import com.varshalaxmi.loanmanager.db.DatabaseHelper;
import com.varshalaxmi.loanmanager.model.Customer;
import com.varshalaxmi.loanmanager.model.Loan;
import com.varshalaxmi.loanmanager.model.LoanAddition;
import com.varshalaxmi.loanmanager.model.Payment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class BackupRestoreHelper {

    public static boolean writeZipBackupToStream(Context context, DatabaseHelper dbHelper, OutputStream outputStream) {
        try {
            JSONObject backupJson = new JSONObject();

            // Customers
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

            // Loans
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

                // Backup Loan Additions
                List<LoanAddition> additions = dbHelper.getAdditionsForLoan(l.getId());
                JSONArray addArray = new JSONArray();
                for (LoanAddition add : additions) {
                    JSONObject aObj = new JSONObject();
                    aObj.put("id", add.getId());
                    aObj.put("amount", add.getAmount());
                    aObj.put("addition_date", add.getAdditionDate());
                    aObj.put("note", add.getNote());
                    addArray.put(aObj);
                }
                obj.put("additions", addArray);

                loanArray.put(obj);
            }
            backupJson.put("loans", loanArray);

            // Payments
            List<Payment> payments = dbHelper.getAllPayments();
            JSONArray payArray = new JSONArray();
            for (Payment p : payments) {
                JSONObject obj = new JSONObject();
                obj.put("id", p.getId());
                obj.put("loan_id", p.getLoanId());
                obj.put("customer_id", p.getCustomerId());
                obj.put("amount", p.getAmount());
                obj.put("payment_type", p.getPaymentType());
                obj.put("payment_date", p.getPaymentDate());
                obj.put("note", p.getNote());
                payArray.put(obj);
            }
            backupJson.put("payments", payArray);

            ZipOutputStream zos = new ZipOutputStream(outputStream);
            ZipEntry entry = new ZipEntry("varshalaxmi_data.json");
            zos.putNextEntry(entry);
            zos.write(backupJson.toString(2).getBytes("UTF-8"));
            zos.closeEntry();
            zos.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean restoreFromInputStream(Context context, InputStream inputStream, DatabaseHelper dbHelper) {
        try {
            ZipInputStream zis = new ZipInputStream(inputStream);
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

            if (root.has("customers")) {
                JSONArray custArray = root.getJSONArray("customers");
                for (int i = 0; i < custArray.length(); i++) {
                    JSONObject c = custArray.getJSONObject(i);
                    dbHelper.addCustomer(c.optString("name"), c.optString("mobile"), c.optString("address"));
                }
            }

            if (root.has("loans")) {
                JSONArray loanArray = root.getJSONArray("loans");
                for (int i = 0; i < loanArray.length(); i++) {
                    JSONObject l = loanArray.getJSONObject(i);
                    long loanId = dbHelper.addLoan(
                            l.optLong("customer_id"),
                            l.optDouble("principal_amount"),
                            l.optDouble("interest_rate"),
                            l.optString("interest_method"),
                            l.optString("issue_date"),
                            l.optString("notes")
                    );

                    if (l.has("additions")) {
                        JSONArray addArray = l.getJSONArray("additions");
                        for (int j = 0; j < addArray.length(); j++) {
                            JSONObject a = addArray.getJSONObject(j);
                            dbHelper.addLoanAddition(loanId, a.optDouble("amount"), a.optString("addition_date"), a.optString("note"));
                        }
                    }
                }
            }

            if (root.has("payments")) {
                JSONArray payArray = root.getJSONArray("payments");
                for (int i = 0; i < payArray.length(); i++) {
                    JSONObject p = payArray.getJSONObject(i);
                    dbHelper.addPayment(
                            p.optLong("loan_id"),
                            p.optLong("customer_id"),
                            p.optDouble("amount"),
                            p.optString("payment_type", "Combined Payment"),
                            p.optString("payment_date", dbHelper.getCurrentDate()),
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
