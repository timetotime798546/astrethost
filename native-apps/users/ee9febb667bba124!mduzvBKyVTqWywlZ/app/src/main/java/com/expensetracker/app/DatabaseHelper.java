package com.expensetracker.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "expenses.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_TRANSACTIONS = "transactions";
    public static final String TABLE_CATEGORIES = "categories";

    public static final String COLUMN_ID = "id";

    // Transactions attributes
    public static final String COLUMN_AMOUNT = "amount";
    public static final String COLUMN_TYPE = "type"; // "income" or "expense"
    public static final String COLUMN_CATEGORY = "category";
    public static final String COLUMN_DESCRIPTION = "description";
    public static final String COLUMN_DATE = "date"; // Format: YYYY-MM-DD

    // Category mapping attributes
    public static final String COLUMN_CAT_NAME = "name";
    public static final String COLUMN_CAT_TYPE = "type";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createCategoriesTable = "CREATE TABLE " + TABLE_CATEGORIES + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_CAT_NAME + " TEXT, " +
                COLUMN_CAT_TYPE + " TEXT, " +
                "UNIQUE(" + COLUMN_CAT_NAME + ", " + COLUMN_CAT_TYPE + ")" +
                ")";
        db.execSQL(createCategoriesTable);

        String createTransactionsTable = "CREATE TABLE " + TABLE_TRANSACTIONS + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_AMOUNT + " REAL, " +
                COLUMN_TYPE + " TEXT, " +
                COLUMN_CATEGORY + " TEXT, " +
                COLUMN_DESCRIPTION + " TEXT, " +
                COLUMN_DATE + " TEXT" +
                ")";
        db.execSQL(createTransactionsTable);

        insertDefaultCategories(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRANSACTIONS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CATEGORIES);
        onCreate(db);
    }

    private void insertDefaultCategories(SQLiteDatabase db) {
        String[] incomeCategories = {"Salary", "Business", "Freelance", "Gift", "Investments", "Other"};
        for (String cat : incomeCategories) {
            ContentValues values = new ContentValues();
            values.put(COLUMN_CAT_NAME, cat);
            values.put(COLUMN_CAT_TYPE, "income");
            db.insert(TABLE_CATEGORIES, null, values);
        }

        String[] expenseCategories = {"Food", "Transport", "Rent", "Bills", "Shopping", "Entertainment", "Medical", "Education", "Travel", "Other"};
        for (String cat : expenseCategories) {
            ContentValues values = new ContentValues();
            values.put(COLUMN_CAT_NAME, cat);
            values.put(COLUMN_CAT_TYPE, "expense");
            db.insert(TABLE_CATEGORIES, null, values);
        }
    }

    public long addTransaction(double amount, String type, String category, String description, String date) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_AMOUNT, amount);
        values.put(COLUMN_TYPE, type);
        values.put(COLUMN_CATEGORY, category);
        values.put(COLUMN_DESCRIPTION, description);
        values.put(COLUMN_DATE, date);
        long result = db.insert(TABLE_TRANSACTIONS, null, values);
        db.close();
        return result;
    }

    public int deleteTransaction(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        int result = db.delete(TABLE_TRANSACTIONS, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
        return result;
    }

    public long addCategory(String name, String type) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_CAT_NAME, name);
        values.put(COLUMN_CAT_TYPE, type);
        long result = db.insertWithOnConflict(TABLE_CATEGORIES, null, values, SQLiteDatabase.CONFLICT_IGNORE);
        db.close();
        return result;
    }

    public List<String> getCategories(String type) {
        List<String> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_CATEGORIES, new String[]{COLUMN_CAT_NAME},
                COLUMN_CAT_TYPE + " = ?", new String[]{type}, null, null, COLUMN_CAT_NAME + " ASC");

        if (cursor.moveToFirst()) {
            do {
                list.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }

    public List<Transaction> getRecentTransactions(int limit) {
        List<Transaction> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_TRANSACTIONS, null, null, null, null, null,
                COLUMN_DATE + " DESC, " + COLUMN_ID + " DESC", String.valueOf(limit));

        if (cursor.moveToFirst()) {
            do {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID));
                double amount = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_AMOUNT));
                String type = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TYPE));
                String category = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY));
                String description = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION));
                String date = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE));

                list.add(new Transaction(id, amount, type, category, description, date));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }

    public List<Transaction> getFilteredTransactions(String monthYear, String categoryFilter) {
        List<Transaction> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        StringBuilder selection = new StringBuilder();
        List<String> args = new ArrayList<>();

        if (monthYear != null && !monthYear.equals("All Time")) {
            selection.append(COLUMN_DATE).append(" LIKE ?");
            args.add(monthYear + "%");
        }

        if (categoryFilter != null && !categoryFilter.equals("All Categories")) {
            if (selection.length() > 0) {
                selection.append(" AND ");
            }
            selection.append(COLUMN_CATEGORY).append(" = ?");
            args.add(categoryFilter);
        }

        String selectionStr = selection.length() > 0 ? selection.toString() : null;
        String[] selectionArgs = args.size() > 0 ? args.toArray(new String[0]) : null;

        Cursor cursor = db.query(TABLE_TRANSACTIONS, null, selectionStr, selectionArgs, null, null,
                COLUMN_DATE + " DESC, " + COLUMN_ID + " DESC");

        if (cursor.moveToFirst()) {
            do {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID));
                double amount = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_AMOUNT));
                String type = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TYPE));
                String category = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY));
                String description = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION));
                String date = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE));

                list.add(new Transaction(id, amount, type, category, description, date));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }

    public List<String> getAvailableMonths() {
        List<String> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT DISTINCT substr(" + COLUMN_DATE + ", 1, 7) FROM " + TABLE_TRANSACTIONS +
                " ORDER BY " + COLUMN_DATE + " DESC", null);

        if (cursor.moveToFirst()) {
            do {
                String my = cursor.getString(0);
                if (my != null && my.length() >= 7) {
                    list.add(my);
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }

    public Map<String, Double> getOverallSummary() {
        Map<String, Double> summary = new HashMap<>();
        double income = 0;
        double expense = 0;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + COLUMN_TYPE + ", SUM(" + COLUMN_AMOUNT + ") FROM " + TABLE_TRANSACTIONS +
                " GROUP BY " + COLUMN_TYPE, null);

        if (cursor.moveToFirst()) {
            do {
                String type = cursor.getString(0);
                double total = cursor.getDouble(1);
                if ("income".equals(type)) {
                    income = total;
                } else if ("expense".equals(type)) {
                    expense = total;
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();

        summary.put("income", income);
        summary.put("expense", expense);
        summary.put("balance", income - expense);
        return summary;
    }

    public Map<String, Double> getMonthlySummary(String monthYear) {
        Map<String, Double> summary = new HashMap<>();
        double income = 0;
        double expense = 0;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + COLUMN_TYPE + ", SUM(" + COLUMN_AMOUNT + ") FROM " + TABLE_TRANSACTIONS +
                " WHERE " + COLUMN_DATE + " LIKE ?" +
                " GROUP BY " + COLUMN_TYPE, new String[]{monthYear + "%"});

        if (cursor.moveToFirst()) {
            do {
                String type = cursor.getString(0);
                double total = cursor.getDouble(1);
                if ("income".equals(type)) {
                    income = total;
                } else if ("expense".equals(type)) {
                    expense = total;
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();

        summary.put("income", income);
        summary.put("expense", expense);
        summary.put("savings", income - expense);
        return summary;
    }

    public List<CategorySummary> getCategorySummary(String monthYear) {
        List<CategorySummary> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT " + COLUMN_CATEGORY + ", SUM(" + COLUMN_AMOUNT + ") FROM " + TABLE_TRANSACTIONS +
                " WHERE " + COLUMN_TYPE + " = 'expense' AND " + COLUMN_DATE + " LIKE ?" +
                " GROUP BY " + COLUMN_CATEGORY +
                " ORDER BY SUM(" + COLUMN_AMOUNT + ") DESC", new String[]{monthYear + "%"});

        if (cursor.moveToFirst()) {
            do {
                String category = cursor.getString(0);
                double amount = cursor.getDouble(1);
                list.add(new CategorySummary(category, amount));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }

    public static class Transaction {
        public long id;
        public double amount;
        public String type;
        public String category;
        public String description;
        public String date;

        public Transaction(long id, double amount, String type, String category, String description, String date) {
            this.id = id;
            this.amount = amount;
            this.type = type;
            this.category = category;
            this.description = description;
            this.date = date;
        }
    }

    public static class CategorySummary {
        public String category;
        public double totalAmount;

        public CategorySummary(String category, double totalAmount) {
            this.category = category;
            this.totalAmount = totalAmount;
        }
    }
}