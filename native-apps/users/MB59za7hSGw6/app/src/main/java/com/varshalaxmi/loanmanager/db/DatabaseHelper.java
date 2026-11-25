package com.varshalaxmi.loanmanager.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.varshalaxmi.loanmanager.model.Customer;
import com.varshalaxmi.loanmanager.model.Loan;
import com.varshalaxmi.loanmanager.model.Payment;
import com.varshalaxmi.loanmanager.util.InterestCalculator;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "varshalaxmi.db";
    private static final int DATABASE_VERSION = 1;

    // Table Names
    public static final String TABLE_CUSTOMERS = "customers";
    public static final String TABLE_LOANS = "loans";
    public static final String TABLE_PAYMENTS = "payments";
    public static final String TABLE_SETTINGS = "settings";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_CUSTOMERS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, " +
                "mobile TEXT, " +
                "address TEXT, " +
                "created_at TEXT);");

        db.execSQL("CREATE TABLE " + TABLE_LOANS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "customer_id INTEGER, " +
                "principal_amount REAL, " +
                "interest_rate REAL, " +
                "interest_method TEXT, " +
                "issue_date TEXT, " +
                "notes TEXT, " +
                "status TEXT, " +
                "FOREIGN KEY(customer_id) REFERENCES " + TABLE_CUSTOMERS + "(id));");

        db.execSQL("CREATE TABLE " + TABLE_PAYMENTS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "loan_id INTEGER, " +
                "customer_id INTEGER, " +
                "amount REAL, " +
                "payment_date TEXT, " +
                "note TEXT, " +
                "FOREIGN KEY(loan_id) REFERENCES " + TABLE_LOANS + "(id), " +
                "FOREIGN KEY(customer_id) REFERENCES " + TABLE_CUSTOMERS + "(id));");

        db.execSQL("CREATE TABLE " + TABLE_SETTINGS + " (" +
                "key_name TEXT PRIMARY KEY, " +
                "val_data TEXT);");

        // Insert default setting language
        db.execSQL("INSERT INTO " + TABLE_SETTINGS + " (key_name, val_data) VALUES ('language', 'hi');");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PAYMENTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOANS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CUSTOMERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SETTINGS);
        onCreate(db);
    }

    public String getCurrentDate() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }

    // --- CUSTOMER OPERATIONS ---
    public long addCustomer(String name, String mobile, String address) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("mobile", mobile);
        cv.put("address", address);
        cv.put("created_at", getCurrentDate());
        return db.insert(TABLE_CUSTOMERS, null, cv);
    }

    public boolean updateCustomer(long id, String name, String mobile, String address) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("mobile", mobile);
        cv.put("address", address);
        return db.update(TABLE_CUSTOMERS, cv, "id=?", new String[]{String.valueOf(id)}) > 0;
    }

    public boolean deleteCustomer(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        // Check if active financial history exists
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_LOANS + " WHERE customer_id=?", new String[]{String.valueOf(id)});
        if (c.moveToFirst() && c.getInt(0) > 0) {
            c.close();
            return false; // Prevent deletion
        }
        c.close();
        return db.delete(TABLE_CUSTOMERS, "id=?", new String[]{String.valueOf(id)}) > 0;
    }

    public List<Customer> getAllCustomers(String searchQuery) {
        List<Customer> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String sql = "SELECT * FROM " + TABLE_CUSTOMERS;
        String[] args = null;
        if (searchQuery != null && searchQuery.trim().length() > 0) {
            sql += " WHERE name LIKE ? OR mobile LIKE ?";
            args = new String[]{"%" + searchQuery + "%", "%" + searchQuery + "%"};
        }
        sql += " ORDER BY id DESC";

        Cursor c = db.rawQuery(sql, args);
        if (c.moveToFirst()) {
            do {
                Customer cust = new Customer(
                        c.getLong(c.getColumnIndexOrThrow("id")),
                        c.getString(c.getColumnIndexOrThrow("name")),
                        c.getString(c.getColumnIndexOrThrow("mobile")),
                        c.getString(c.getColumnIndexOrThrow("address")),
                        c.getString(c.getColumnIndexOrThrow("created_at"))
                );
                // Compute aggregated balance
                calculateCustomerFinancials(cust);
                list.add(cust);
            } while (c.moveToNext());
        }
        c.close();
        return list;
    }

    private void calculateCustomerFinancials(Customer cust) {
        SQLiteDatabase db = this.getReadableDatabase();
        // Sum total loan given
        Cursor cGiven = db.rawQuery("SELECT SUM(principal_amount) FROM " + TABLE_LOANS + " WHERE customer_id=?", new String[]{String.valueOf(cust.getId())});
        double totalGiven = 0;
        if (cGiven.moveToFirst()) {
            totalGiven = cGiven.getDouble(0);
        }
        cGiven.close();

        // Sum total paid
        Cursor cPaid = db.rawQuery("SELECT SUM(amount) FROM " + TABLE_PAYMENTS + " WHERE customer_id=?", new String[]{String.valueOf(cust.getId())});
        double totalPaid = 0;
        if (cPaid.moveToFirst()) {
            totalPaid = cPaid.getDouble(0);
        }
        cPaid.close();

        cust.setTotalGiven(totalGiven);
        cust.setTotalPaid(totalPaid);

        // Sum accrued interest for all loans of this customer
        List<Loan> customerLoans = getLoansForCustomer(cust.getId());
        double totalOutstanding = 0;
        for (Loan l : customerLoans) {
            totalOutstanding += l.getRemainingBalance();
        }
        cust.setOutstandingBalance(totalOutstanding);
    }

    // --- LOAN OPERATIONS ---
    public long addLoan(long customerId, double principal, double rate, String method, String issueDate, String notes) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("customer_id", customerId);
        cv.put("principal_amount", principal);
        cv.put("interest_rate", rate);
        cv.put("interest_method", method);
        cv.put("issue_date", (issueDate == null || issueDate.isEmpty()) ? getCurrentDate() : issueDate);
        cv.put("notes", notes);
        cv.put("status", "ACTIVE");
        return db.insert(TABLE_LOANS, null, cv);
    }

    public List<Loan> getLoansForCustomer(long customerId) {
        List<Loan> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String sql = "SELECT l.*, c.name as customer_name FROM " + TABLE_LOANS + " l " +
                "JOIN " + TABLE_CUSTOMERS + " c ON l.customer_id = c.id " +
                "WHERE l.customer_id=? ORDER BY l.id DESC";
        Cursor c = db.rawQuery(sql, new String[]{String.valueOf(customerId)});
        if (c.moveToFirst()) {
            do {
                Loan l = parseLoanFromCursor(c);
                calculateLoanState(l);
                list.add(l);
            } while (c.moveToNext());
        }
        c.close();
        return list;
    }

    public List<Loan> getAllLoans() {
        List<Loan> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String sql = "SELECT l.*, c.name as customer_name FROM " + TABLE_LOANS + " l " +
                "JOIN " + TABLE_CUSTOMERS + " c ON l.customer_id = c.id ORDER BY l.id DESC";
        Cursor c = db.rawQuery(sql, null);
        if (c.moveToFirst()) {
            do {
                Loan l = parseLoanFromCursor(c);
                calculateLoanState(l);
                list.add(l);
            } while (c.moveToNext());
        }
        c.close();
        return list;
    }

    private Loan parseLoanFromCursor(Cursor c) {
        return new Loan(
                c.getLong(c.getColumnIndexOrThrow("id")),
                c.getLong(c.getColumnIndexOrThrow("customer_id")),
                c.getString(c.getColumnIndexOrThrow("customer_name")),
                c.getDouble(c.getColumnIndexOrThrow("principal_amount")),
                c.getDouble(c.getColumnIndexOrThrow("interest_rate")),
                c.getString(c.getColumnIndexOrThrow("interest_method")),
                c.getString(c.getColumnIndexOrThrow("issue_date")),
                c.getString(c.getColumnIndexOrThrow("notes")),
                c.getString(c.getColumnIndexOrThrow("status"))
        );
    }

    public void calculateLoanState(Loan loan) {
        SQLiteDatabase db = this.getReadableDatabase();
        // Calculate interest accrued based on issue_date and current date
        double accrued = InterestCalculator.computeInterest(
                loan.getPrincipalAmount(),
                loan.getInterestRate(),
                loan.getInterestMethod(),
                loan.getIssueDate(),
                getCurrentDate()
        );

        // Sum payments for this specific loan
        Cursor c = db.rawQuery("SELECT SUM(amount) FROM " + TABLE_PAYMENTS + " WHERE loan_id=?", new String[]{String.valueOf(loan.getId())});
        double paid = 0;
        if (c.moveToFirst()) {
            paid = c.getDouble(0);
        }
        c.close();

        loan.setAccruedInterest(accrued);
        loan.setTotalPaid(paid);
        double netBalance = (loan.getPrincipalAmount() + accrued) - paid;
        loan.setRemainingBalance(Math.max(0.0, netBalance));
    }

    // --- PAYMENT OPERATIONS ---
    public long addPayment(long loanId, long customerId, double amount, String note) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("loan_id", loanId);
        cv.put("customer_id", customerId);
        cv.put("amount", amount);
        cv.put("payment_date", getCurrentDate());
        cv.put("note", note);
        return db.insert(TABLE_PAYMENTS, null, cv);
    }

    public List<Payment> getAllPayments() {
        List<Payment> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String sql = "SELECT p.*, c.name as customer_name FROM " + TABLE_PAYMENTS + " p " +
                "JOIN " + TABLE_CUSTOMERS + " c ON p.customer_id = c.id ORDER BY p.id DESC";
        Cursor c = db.rawQuery(sql, null);
        if (c.moveToFirst()) {
            do {
                list.add(new Payment(
                        c.getLong(c.getColumnIndexOrThrow("id")),
                        c.getLong(c.getColumnIndexOrThrow("loan_id")),
                        c.getLong(c.getColumnIndexOrThrow("customer_id")),
                        c.getString(c.getColumnIndexOrThrow("customer_name")),
                        c.getDouble(c.getColumnIndexOrThrow("amount")),
                        c.getString(c.getColumnIndexOrThrow("payment_date")),
                        c.getString(c.getColumnIndexOrThrow("note"))
                ));
            } while (c.moveToNext());
        }
        c.close();
        return list;
    }

    // --- DASHBOARD STATISTICS ---
    public double getTotalMoneyGiven() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT SUM(principal_amount) FROM " + TABLE_LOANS, null);
        double res = 0;
        if (c.moveToFirst()) res = c.getDouble(0);
        c.close();
        return res;
    }

    public double getTotalMoneyCollected() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT SUM(amount) FROM " + TABLE_PAYMENTS, null);
        double res = 0;
        if (c.moveToFirst()) res = c.getDouble(0);
        c.close();
        return res;
    }

    public double getTodayCollection() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT SUM(amount) FROM " + TABLE_PAYMENTS + " WHERE payment_date=?", new String[]{getCurrentDate()});
        double res = 0;
        if (c.moveToFirst()) res = c.getDouble(0);
        c.close();
        return res;
    }

    public int getCustomerCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_CUSTOMERS, null);
        int res = 0;
        if (c.moveToFirst()) res = c.getInt(0);
        c.close();
        return res;
    }

    // --- SETTINGS ---
    public String getSetting(String key, String defaultVal) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT val_data FROM " + TABLE_SETTINGS + " WHERE key_name=?", new String[]{key});
        String res = defaultVal;
        if (c.moveToFirst()) res = c.getString(0);
        c.close();
        return res;
    }

    public void setSetting(String key, String val) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("key_name", key);
        cv.put("val_data", val);
        db.insertWithOnConflict(TABLE_SETTINGS, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }
}