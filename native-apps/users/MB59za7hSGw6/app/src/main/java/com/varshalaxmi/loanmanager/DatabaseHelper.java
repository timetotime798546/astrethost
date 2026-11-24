package com.varshalaxmi.loanmanager;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "varshalaxmi_loans.db";
    private static final int DATABASE_VERSION = 1;

    // Table names
    public static final String TABLE_CUSTOMERS = "customers";
    public static final String TABLE_LOANS = "loans";
    public static final String TABLE_LOAN_ADDITIONS = "loan_additions";
    public static final String TABLE_PAYMENTS = "payments";
    public static final String TABLE_SETTINGS = "app_settings";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Customers table
        db.execSQL("CREATE TABLE " + TABLE_CUSTOMERS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, " +
                "phone TEXT, " +
                "address TEXT, " +
                "notes TEXT, " +
                "created_at TEXT, " +
                "status INTEGER DEFAULT 1)");

        // Loans table
        db.execSQL("CREATE TABLE " + TABLE_LOANS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "customer_id INTEGER NOT NULL, " +
                "initial_principal REAL NOT NULL, " +
                "loan_date TEXT NOT NULL, " +
                "due_date TEXT, " +
                "interest_method TEXT NOT NULL, " +
                "interest_rate REAL DEFAULT 0, " +
                "interest_period TEXT, " +
                "repayment_freq TEXT, " +
                "notes TEXT, " +
                "status TEXT DEFAULT 'ACTIVE', " +
                "created_at TEXT, " +
                "FOREIGN KEY(customer_id) REFERENCES " + TABLE_CUSTOMERS + "(id))");

        // Loan Additions table (Mode B support)
        db.execSQL("CREATE TABLE " + TABLE_LOAN_ADDITIONS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "loan_id INTEGER NOT NULL, " +
                "customer_id INTEGER NOT NULL, " +
                "amount REAL NOT NULL, " +
                "addition_date TEXT NOT NULL, " +
                "notes TEXT, " +
                "FOREIGN KEY(loan_id) REFERENCES " + TABLE_LOANS + "(id), " +
                "FOREIGN KEY(customer_id) REFERENCES " + TABLE_CUSTOMERS + "(id))");

        // Payments table
        db.execSQL("CREATE TABLE " + TABLE_PAYMENTS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "customer_id INTEGER NOT NULL, " +
                "loan_id INTEGER NOT NULL, " +
                "payment_date TEXT NOT NULL, " +
                "payment_amount REAL NOT NULL, " +
                "principal_paid REAL NOT NULL, " +
                "interest_paid REAL NOT NULL, " +
                "payment_method TEXT, " +
                "notes TEXT, " +
                "FOREIGN KEY(customer_id) REFERENCES " + TABLE_CUSTOMERS + "(id), " +
                "FOREIGN KEY(loan_id) REFERENCES " + TABLE_LOANS + "(id))");

        // App Settings table
        db.execSQL("CREATE TABLE " + TABLE_SETTINGS + " (" +
                "key_name TEXT PRIMARY KEY, " +
                "key_value TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PAYMENTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOAN_ADDITIONS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOANS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CUSTOMERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SETTINGS);
        onCreate(db);
    }

    public static String getTodayDateString() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        return sdf.format(new Date());
    }

    // --- CUSTOMER CALCULATIONS & DB HELPER METHODS ---

    public long addCustomer(String name, String phone, String address, String notes) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("phone", phone);
        cv.put("address", address);
        cv.put("notes", notes);
        cv.put("created_at", getTodayDateString());
        cv.put("status", 1);
        return db.insert(TABLE_CUSTOMERS, null, cv);
    }

    public boolean updateCustomer(long id, String name, String phone, String address, String notes) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("phone", phone);
        cv.put("address", address);
        cv.put("notes", notes);
        return db.update(TABLE_CUSTOMERS, cv, "id=?", new String[]{String.valueOf(id)}) > 0;
    }

    public Cursor getAllActiveCustomers() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_CUSTOMERS, null, "status=1", null, null, null, "name ASC");
    }

    public Cursor searchCustomers(String query) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_CUSTOMERS, null, "status=1 AND (name LIKE ? OR phone LIKE ?)",
                new String[]{"%" + query + "%", "%" + query + "%"}, null, null, "name ASC");
    }

    public Cursor getCustomerById(long id) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_CUSTOMERS, null, "id=?", new String[]{String.valueOf(id)}, null, null, null);
    }

    // --- LOAN HELPER METHODS ---

    public long addLoan(long customerId, double amount, String loanDate, String dueDate,
                        String interestMethod, double interestRate, String interestPeriod,
                        String repaymentFreq, String notes) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        long loanId = -1;
        try {
            ContentValues cv = new ContentValues();
            cv.put("customer_id", customerId);
            cv.put("initial_principal", amount);
            cv.put("loan_date", loanDate);
            cv.put("due_date", dueDate);
            cv.put("interest_method", interestMethod);
            cv.put("interest_rate", interestRate);
            cv.put("interest_period", interestPeriod);
            cv.put("repayment_freq", repaymentFreq);
            cv.put("notes", notes);
            cv.put("status", "ACTIVE");
            cv.put("created_at", getTodayDateString());

            loanId = db.insert(TABLE_LOANS, null, cv);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return loanId;
    }

    public boolean addMoneyToExistingLoan(long loanId, long customerId, double amount, String additionDate, String notes) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        boolean success = false;
        try {
            ContentValues cv = new ContentValues();
            cv.put("loan_id", loanId);
            cv.put("customer_id", customerId);
            cv.put("amount", amount);
            cv.put("addition_date", additionDate);
            cv.put("notes", notes);
            long res = db.insert(TABLE_LOAN_ADDITIONS, null, cv);
            if (res > 0) {
                // If loan was closed, reopen it as new principal is added
                ContentValues loanCv = new ContentValues();
                loanCv.put("status", "ACTIVE");
                db.update(TABLE_LOANS, loanCv, "id=?", new String[]{String.valueOf(loanId)});
                success = true;
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return success;
    }

    public Cursor getLoansForCustomer(long customerId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_LOANS, null, "customer_id=?", new String[]{String.valueOf(customerId)}, null, null, "id DESC");
    }

    public Cursor getAllLoans(String statusFilter) {
        SQLiteDatabase db = this.getReadableDatabase();
        if (statusFilter == null || statusFilter.equalsIgnoreCase("ALL")) {
            return db.rawQuery("SELECT l.*, c.name as customer_name, c.phone as customer_phone FROM " +
                    TABLE_LOANS + " l JOIN " + TABLE_CUSTOMERS + " c ON l.customer_id = c.id ORDER BY l.id DESC", null);
        } else {
            return db.rawQuery("SELECT l.*, c.name as customer_name, c.phone as customer_phone FROM " +
                    TABLE_LOANS + " l JOIN " + TABLE_CUSTOMERS + " c ON l.customer_id = c.id WHERE l.status=? ORDER BY l.id DESC", new String[]{statusFilter});
        }
    }

    public Cursor getLoanDetailsById(long loanId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT l.*, c.name as customer_name, c.phone as customer_phone FROM " +
                TABLE_LOANS + " l JOIN " + TABLE_CUSTOMERS + " c ON l.customer_id = c.id WHERE l.id=?", new String[]{String.valueOf(loanId)});
    }

    // --- PAYMENT METHODS ---

    public boolean recordPayment(long customerId, long loanId, String paymentDate, double amount,
                                 double principalPaid, double interestPaid, String method, String notes) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        boolean success = false;
        try {
            ContentValues cv = new ContentValues();
            cv.put("customer_id", customerId);
            cv.put("loan_id", loanId);
            cv.put("payment_date", paymentDate);
            cv.put("payment_amount", amount);
            cv.put("principal_paid", principalPaid);
            cv.put("interest_paid", interestPaid);
            cv.put("payment_method", method);
            cv.put("notes", notes);

            long res = db.insert(TABLE_PAYMENTS, null, cv);
            if (res > 0) {
                // Check if remaining loan outstanding is zero, if so mark loan as CLOSED
                double outstanding = getLoanOutstandingAmount(loanId);
                if (outstanding <= 0.01) {
                    ContentValues loanCv = new ContentValues();
                    loanCv.put("status", "CLOSED");
                    db.update(TABLE_LOANS, loanCv, "id=?", new String[]{String.valueOf(loanId)});
                }
                success = true;
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return success;
    }

    public Cursor getPaymentsForLoan(long loanId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_PAYMENTS, null, "loan_id=?", new String[]{String.valueOf(loanId)}, null, null, "payment_date DESC, id DESC");
    }

    public Cursor getAllPayments() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT p.*, c.name as customer_name FROM " +
                TABLE_PAYMENTS + " p JOIN " + TABLE_CUSTOMERS + " c ON p.customer_id = c.id ORDER BY p.payment_date DESC, p.id DESC", null);
    }

    public Cursor getPaymentsByDate(String dateStr) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT p.*, c.name as customer_name FROM " +
                TABLE_PAYMENTS + " p JOIN " + TABLE_CUSTOMERS + " c ON p.customer_id = c.id WHERE p.payment_date=? ORDER BY p.id DESC", new String[]{dateStr});
    }

    // --- COMPLEX DETERMINISTIC CALCULATION METHODS ---

    public double getTotalPrincipalGivenForLoan(long loanId) {
        SQLiteDatabase db = this.getReadableDatabase();
        double initial = 0;
        Cursor cursor = db.query(TABLE_LOANS, new String[]{"initial_principal"}, "id=?", new String[]{String.valueOf(loanId)}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            initial = cursor.getDouble(0);
            cursor.close();
        }

        double additions = 0;
        Cursor addCursor = db.rawQuery("SELECT SUM(amount) FROM " + TABLE_LOAN_ADDITIONS + " WHERE loan_id=?", new String[]{String.valueOf(loanId)});
        if (addCursor != null && addCursor.moveToFirst()) {
            additions = addCursor.getDouble(0);
            addCursor.close();
        }

        return round(initial + additions);
    }

    public double getTotalPrincipalPaidForLoan(long loanId) {
        SQLiteDatabase db = this.getReadableDatabase();
        double paid = 0;
        Cursor cursor = db.rawQuery("SELECT SUM(principal_paid) FROM " + TABLE_PAYMENTS + " WHERE loan_id=?", new String[]{String.valueOf(loanId)});
        if (cursor != null && cursor.moveToFirst()) {
            paid = cursor.getDouble(0);
            cursor.close();
        }
        return round(paid);
    }

    public double getTotalInterestPaidForLoan(long loanId) {
        SQLiteDatabase db = this.getReadableDatabase();
        double paid = 0;
        Cursor cursor = db.rawQuery("SELECT SUM(interest_paid) FROM " + TABLE_PAYMENTS + " WHERE loan_id=?", new String[]{String.valueOf(loanId)});
        if (cursor != null && cursor.moveToFirst()) {
            paid = cursor.getDouble(0);
            cursor.close();
        }
        return round(paid);
    }

    public double calculateAccruedInterestForLoan(long loanId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_LOANS, null, "id=?", new String[]{String.valueOf(loanId)}, null, null, null);
        if (cursor == null || !cursor.moveToFirst()) {
            if (cursor != null) cursor.close();
            return 0;
        }

        double initialPrincipal = cursor.getDouble(cursor.getColumnIndexOrThrow("initial_principal"));
        String interestMethod = cursor.getString(cursor.getColumnIndexOrThrow("interest_method"));
        double interestRate = cursor.getDouble(cursor.getColumnIndexOrThrow("interest_rate"));
        String loanDateStr = cursor.getString(cursor.getColumnIndexOrThrow("loan_date"));
        cursor.close();

        double totalPrincipal = getTotalPrincipalGivenForLoan(loanId);

        if ("No Interest".equalsIgnoreCase(interestMethod)) {
            return 0.0;
        } else if ("Fixed Interest Amount".equalsIgnoreCase(interestMethod)) {
            return round(interestRate);
        }

        // Calculate days elapsed since loan date
        long days = getDaysBetween(loanDateStr, getTodayDateString());
        if (days < 0) days = 0;

        double interest = 0;
        if ("Daily Interest".equalsIgnoreCase(interestMethod)) {
            interest = (totalPrincipal * interestRate * days) / 100.0;
        } else if ("Monthly Interest".equalsIgnoreCase(interestMethod)) {
            double months = days / 30.0;
            interest = (totalPrincipal * interestRate * months) / 100.0;
        } else if ("Yearly Interest".equalsIgnoreCase(interestMethod) || "Simple Interest".equalsIgnoreCase(interestMethod)) {
            double years = days / 365.0;
            interest = (totalPrincipal * interestRate * years) / 100.0;
        } else if ("Flat Interest".equalsIgnoreCase(interestMethod)) {
            // Flat rate based on period assumption or fixed rate
            interest = (totalPrincipal * interestRate) / 100.0;
        } else if ("Reducing Balance Interest".equalsIgnoreCase(interestMethod)) {
            double remainingPrincipal = totalPrincipal - getTotalPrincipalPaidForLoan(loanId);
            if (remainingPrincipal < 0) remainingPrincipal = 0;
            double months = days / 30.0;
            interest = (remainingPrincipal * interestRate * months) / 100.0;
        }

        return round(interest);
    }

    public double getLoanOutstandingAmount(long loanId) {
        double totalPrincipal = getTotalPrincipalGivenForLoan(loanId);
        double totalInterest = calculateAccruedInterestForLoan(loanId);
        double totalPaidPrincipal = getTotalPrincipalPaidForLoan(loanId);
        double totalPaidInterest = getTotalInterestPaidForLoan(loanId);

        double remainingPrincipal = totalPrincipal - totalPaidPrincipal;
        double remainingInterest = totalInterest - totalPaidInterest;

        if (remainingPrincipal < 0) remainingPrincipal = 0;
        if (remainingInterest < 0) remainingInterest = 0;

        return round(remainingPrincipal + remainingInterest);
    }

    // --- DASHBOARD AGGREGATION METRICS ---

    public int getTotalCustomerCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_CUSTOMERS + " WHERE status=1", null);
        int count = 0;
        if (cursor != null && cursor.moveToFirst()) {
            count = cursor.getInt(0);
            cursor.close();
        }
        return count;
    }

    public int getActiveLoanCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_LOANS + " WHERE status='ACTIVE'", null);
        int count = 0;
        if (cursor != null && cursor.moveToFirst()) {
            count = cursor.getInt(0);
            cursor.close();
        }
        return count;
    }

    public double getTotalLoanGivenAmount() {
        SQLiteDatabase db = this.getReadableDatabase();
        double initialSum = 0;
        Cursor c1 = db.rawQuery("SELECT SUM(initial_principal) FROM " + TABLE_LOANS, null);
        if (c1 != null && c1.moveToFirst()) {
            initialSum = c1.getDouble(0);
            c1.close();
        }
        double additionSum = 0;
        Cursor c2 = db.rawQuery("SELECT SUM(amount) FROM " + TABLE_LOAN_ADDITIONS, null);
        if (c2 != null && c2.moveToFirst()) {
            additionSum = c2.getDouble(0);
            c2.close();
        }
        return round(initialSum + additionSum);
    }

    public double getTotalAmountCollected() {
        SQLiteDatabase db = this.getReadableDatabase();
        double total = 0;
        Cursor c = db.rawQuery("SELECT SUM(payment_amount) FROM " + TABLE_PAYMENTS, null);
        if (c != null && c.moveToFirst()) {
            total = c.getDouble(0);
            c.close();
        }
        return round(total);
    }

    public double getTodayCollectionAmount() {
        SQLiteDatabase db = this.getReadableDatabase();
        double total = 0;
        Cursor c = db.rawQuery("SELECT SUM(payment_amount) FROM " + TABLE_PAYMENTS + " WHERE payment_date=?", new String[]{getTodayDateString()});
        if (c != null && c.moveToFirst()) {
            total = c.getDouble(0);
            c.close();
        }
        return round(total);
    }

    public double getTotalOutstandingAmountAll() {
        SQLiteDatabase db = this.getReadableDatabase();
        double totalOutstanding = 0;
        Cursor cursor = db.query(TABLE_LOANS, new String[]{"id"}, "status='ACTIVE'", null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                long loanId = cursor.getLong(0);
                totalOutstanding += getLoanOutstandingAmount(loanId);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return round(totalOutstanding);
    }

    public double getCustomerTotalOutstanding(long customerId) {
        SQLiteDatabase db = this.getReadableDatabase();
        double totalOutstanding = 0;
        Cursor cursor = db.query(TABLE_LOANS, new String[]{"id"}, "customer_id=? AND status='ACTIVE'", new String[]{String.valueOf(customerId)}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                long loanId = cursor.getLong(0);
                totalOutstanding += getLoanOutstandingAmount(loanId);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return round(totalOutstanding);
    }

    public double getCustomerTotalPrincipalGiven(long customerId) {
        SQLiteDatabase db = this.getReadableDatabase();
        double sum = 0;
        Cursor cursor = db.query(TABLE_LOANS, new String[]{"id"}, "customer_id=?", new String[]{String.valueOf(customerId)}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                long loanId = cursor.getLong(0);
                sum += getTotalPrincipalGivenForLoan(loanId);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return round(sum);
    }

    public double getCustomerTotalPaid(long customerId) {
        SQLiteDatabase db = this.getReadableDatabase();
        double total = 0;
        Cursor c = db.rawQuery("SELECT SUM(payment_amount) FROM " + TABLE_PAYMENTS + " WHERE customer_id=?", new String[]{String.valueOf(customerId)});
        if (c != null && c.moveToFirst()) {
            total = c.getDouble(0);
            c.close();
        }
        return round(total);
    }

    public double getTotalInterestEarned() {
        SQLiteDatabase db = this.getReadableDatabase();
        double total = 0;
        Cursor c = db.rawQuery("SELECT SUM(interest_paid) FROM " + TABLE_PAYMENTS, null);
        if (c != null && c.moveToFirst()) {
            total = c.getDouble(0);
            c.close();
        }
        return round(total);
    }

    // --- SETTINGS STORAGE ---

    public void setSetting(String key, String value) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("key_name", key);
        cv.put("key_value", value);
        db.replace(TABLE_SETTINGS, null, cv);
    }

    public String getSetting(String key, String defaultValue) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_SETTINGS, new String[]{"key_value"}, "key_name=?", new String[]{key}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            String val = cursor.getString(0);
            cursor.close();
            return val;
        }
        if (cursor != null) cursor.close();
        return defaultValue;
    }

    // --- UTILITIES ---

    public static double round(double value) {
        BigDecimal bd = new BigDecimal(Double.toString(value));
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    public static long getDaysBetween(String startDateStr, String endDateStr) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        try {
            Date startDate = sdf.parse(startDateStr);
            Date endDate = sdf.parse(endDateStr);
            if (startDate != null && endDate != null) {
                long diffInMillis = endDate.getTime() - startDate.getTime();
                return diffInMillis / (24 * 60 * 60 * 1000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }
}