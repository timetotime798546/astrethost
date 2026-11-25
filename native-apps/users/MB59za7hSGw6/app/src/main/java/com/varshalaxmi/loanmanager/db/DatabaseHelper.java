package com.varshalaxmi.loanmanager.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.varshalaxmi.loanmanager.model.Customer;
import com.varshalaxmi.loanmanager.model.Loan;
import com.varshalaxmi.loanmanager.model.LoanAddition;
import com.varshalaxmi.loanmanager.model.Payment;
import com.varshalaxmi.loanmanager.util.InterestCalculator;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "varshalaxmi.db";
    private static final int DATABASE_VERSION = 2; // Incremented for top-up additions & payment types

    public static final String TABLE_CUSTOMERS = "customers";
    public static final String TABLE_LOANS = "loans";
    public static final String TABLE_LOAN_ADDITIONS = "loan_additions";
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

        db.execSQL("CREATE TABLE " + TABLE_LOAN_ADDITIONS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "loan_id INTEGER, " +
                "amount REAL, " +
                "addition_date TEXT, " +
                "note TEXT, " +
                "FOREIGN KEY(loan_id) REFERENCES " + TABLE_LOANS + "(id));");

        db.execSQL("CREATE TABLE " + TABLE_PAYMENTS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "loan_id INTEGER, " +
                "customer_id INTEGER, " +
                "amount REAL, " +
                "payment_type TEXT DEFAULT 'Combined Payment', " +
                "payment_date TEXT, " +
                "note TEXT, " +
                "FOREIGN KEY(loan_id) REFERENCES " + TABLE_LOANS + "(id), " +
                "FOREIGN KEY(customer_id) REFERENCES " + TABLE_CUSTOMERS + "(id));");

        db.execSQL("CREATE TABLE " + TABLE_SETTINGS + " (" +
                "key_name TEXT PRIMARY KEY, " +
                "val_data TEXT);");

        db.execSQL("INSERT INTO " + TABLE_SETTINGS + " (key_name, val_data) VALUES ('language', 'hi');");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_LOAN_ADDITIONS + " (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "loan_id INTEGER, " +
                    "amount REAL, " +
                    "addition_date TEXT, " +
                    "note TEXT, " +
                    "FOREIGN KEY(loan_id) REFERENCES " + TABLE_LOANS + "(id));");
            try {
                db.execSQL("ALTER TABLE " + TABLE_PAYMENTS + " ADD COLUMN payment_type TEXT DEFAULT 'Combined Payment';");
            } catch (Exception e) {
                // Column might exist
            }
        }
    }

    public String getCurrentDate() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
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

    public boolean deleteCustomer(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_LOANS + " WHERE customer_id=?", new String[]{String.valueOf(id)});
        if (c.moveToFirst() && c.getInt(0) > 0) {
            c.close();
            return false;
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
                calculateCustomerFinancials(cust);
                list.add(cust);
            } while (c.moveToNext());
        }
        c.close();
        return list;
    }

    private void calculateCustomerFinancials(Customer cust) {
        List<Loan> customerLoans = getLoansForCustomer(cust.getId());
        double totalGiven = 0;
        double totalPaid = 0;
        double totalOutstanding = 0;

        for (Loan l : customerLoans) {
            totalGiven += l.getTotalDisbursed();
            totalPaid += l.getTotalPaid();
            totalOutstanding += l.getRemainingBalance();
        }

        cust.setTotalGiven(totalGiven);
        cust.setTotalPaid(totalPaid);
        cust.setOutstandingBalance(totalOutstanding);
    }

    // --- LOAN OPERATIONS ---
    public long addLoan(long customerId, double principal, double rate, String method, String issueDate, String notes) {
        SQLiteDatabase db = this.getWritableDatabase();
        // Standardize clean interest method strings
        String cleanMethod = "No Interest";
        if (method != null) {
            if (method.toLowerCase().contains("daily")) cleanMethod = "Daily Interest";
            else if (method.toLowerCase().contains("monthly")) cleanMethod = "Monthly Interest";
        }

        ContentValues cv = new ContentValues();
        cv.put("customer_id", customerId);
        cv.put("principal_amount", principal);
        cv.put("interest_rate", rate);
        cv.put("interest_method", cleanMethod);
        cv.put("issue_date", (issueDate == null || issueDate.isEmpty()) ? getCurrentDate() : issueDate);
        cv.put("notes", notes);
        cv.put("status", "ACTIVE");
        return db.insert(TABLE_LOANS, null, cv);
    }

    public long addLoanAddition(long loanId, double amount, String date, String note) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("loan_id", loanId);
        cv.put("amount", amount);
        cv.put("addition_date", (date == null || date.isEmpty()) ? getCurrentDate() : date);
        cv.put("note", note);
        return db.insert(TABLE_LOAN_ADDITIONS, null, cv);
    }

    public List<LoanAddition> getAdditionsForLoan(long loanId) {
        List<LoanAddition> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + TABLE_LOAN_ADDITIONS + " WHERE loan_id=? ORDER BY addition_date ASC", new String[]{String.valueOf(loanId)});
        if (c.moveToFirst()) {
            do {
                list.add(new LoanAddition(
                        c.getLong(c.getColumnIndexOrThrow("id")),
                        c.getLong(c.getColumnIndexOrThrow("loan_id")),
                        c.getDouble(c.getColumnIndexOrThrow("amount")),
                        c.getString(c.getColumnIndexOrThrow("addition_date")),
                        c.getString(c.getColumnIndexOrThrow("note"))
                ));
            } while (c.moveToNext());
        }
        c.close();
        return list;
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
        List<LoanAddition> additions = getAdditionsForLoan(loan.getId());
        List<Payment> payments = getPaymentsForLoan(loan.getId());

        loan.setAdditions(additions);
        loan.setPayments(payments);

        InterestCalculator.CalculationResult res = InterestCalculator.calculateLoanLedger(
                loan.getPrincipalAmount(),
                loan.getInterestRate(),
                loan.getInterestMethod(),
                loan.getIssueDate(),
                additions,
                payments,
                getCurrentDate()
        );

        loan.setCombinedPrincipal(res.currentPrincipal);
        loan.setTotalDisbursed(res.totalDisbursed);
        loan.setAccruedInterest(res.totalAccruedInterest);
        loan.setTotalPaid(res.totalPaid);
        loan.setPrincipalPaid(res.totalPrincipalPaid);
        loan.setInterestPaid(res.totalInterestPaid);
        loan.setRemainingBalance(res.remainingBalance);
    }

    // --- PAYMENT OPERATIONS ---
    public long addPayment(long loanId, long customerId, double amount, String paymentType, String note) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("loan_id", loanId);
        cv.put("customer_id", customerId);
        cv.put("amount", amount);
        cv.put("payment_type", (paymentType != null && !paymentType.isEmpty()) ? paymentType : "Combined Payment");
        cv.put("payment_date", getCurrentDate());
        cv.put("note", note);
        return db.insert(TABLE_PAYMENTS, null, cv);
    }

    public List<Payment> getPaymentsForLoan(long loanId) {
        List<Payment> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String sql = "SELECT p.*, c.name as customer_name FROM " + TABLE_PAYMENTS + " p " +
                "JOIN " + TABLE_CUSTOMERS + " c ON p.customer_id = c.id WHERE p.loan_id=? ORDER BY p.id ASC";
        Cursor c = db.rawQuery(sql, new String[]{String.valueOf(loanId)});
        if (c.moveToFirst()) {
            do {
                list.add(new Payment(
                        c.getLong(c.getColumnIndexOrThrow("id")),
                        c.getLong(c.getColumnIndexOrThrow("loan_id")),
                        c.getLong(c.getColumnIndexOrThrow("customer_id")),
                        c.getString(c.getColumnIndexOrThrow("customer_name")),
                        c.getDouble(c.getColumnIndexOrThrow("amount")),
                        c.getString(c.getColumnIndexOrThrow("payment_type")),
                        c.getString(c.getColumnIndexOrThrow("payment_date")),
                        c.getString(c.getColumnIndexOrThrow("note"))
                ));
            } while (c.moveToNext());
        }
        c.close();
        return list;
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
                        c.getString(c.getColumnIndexOrThrow("payment_type")),
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
        List<Loan> loans = getAllLoans();
        double sum = 0;
        for (Loan l : loans) {
            sum += l.getTotalDisbursed();
        }
        return sum;
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