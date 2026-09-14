package com.expensetracker.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {

    private DatabaseHelper dbHelper;
    private Calendar currentCalendar;

    // View Components
    private TextView tvCurrentMonth;
    private TextView tvNetBalance;
    private TextView tvTotalIncome;
    private TextView tvTotalExpense;
    private LinearLayout categoryReportContainer;
    private LinearLayout transactionsContainer;
    private TextView tvNoReport;
    private TextView tvNoTransactions;

    private Button btnPrevMonth;
    private Button btnNextMonth;
    private Button btnAddTransaction;

    // Category Arrays
    private final String[] expenseCategories = {"Food", "Shopping", "Rent & Bills", "Entertainment", "Transport", "Health", "Education", "Others"};
    private final String[] incomeCategories = {"Salary", "Business", "Gifts", "Other"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);
        currentCalendar = Calendar.getInstance();

        // Initialize Views
        tvCurrentMonth = (TextView) findViewById(R.id.tv_current_month);
        tvNetBalance = (TextView) findViewById(R.id.tv_net_balance);
        tvTotalIncome = (TextView) findViewById(R.id.tv_total_income);
        tvTotalExpense = (TextView) findViewById(R.id.tv_total_expense);
        categoryReportContainer = (LinearLayout) findViewById(R.id.category_report_container);
        transactionsContainer = (LinearLayout) findViewById(R.id.transactions_container);
        tvNoReport = (TextView) findViewById(R.id.tv_no_report);
        tvNoTransactions = (TextView) findViewById(R.id.tv_no_transactions);

        btnPrevMonth = (Button) findViewById(R.id.btn_prev_month);
        btnNextMonth = (Button) findViewById(R.id.btn_next_month);
        btnAddTransaction = (Button) findViewById(R.id.btn_add_transaction);

        // Prepopulate standard data if database is empty to make it look visually outstanding on start
        prepopulateIfEmpty();

        // Register Click Listeners
        btnPrevMonth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentCalendar.add(Calendar.MONTH, -1);
                updateUI();
            }
        });

        btnNextMonth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentCalendar.add(Calendar.MONTH, 1);
                updateUI();
            }
        });

        btnAddTransaction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddTransactionDialog();
            }
        });

        updateUI();
    }

    private void prepopulateIfEmpty() {
        if (dbHelper.getTransactionCount() == 0) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            String today = dateFormat.format(new Date());

            // Add Mock Income
            dbHelper.addTransaction("INCOME", "Salary", 3200.00, "Monthly Salary Payment", today);
            dbHelper.addTransaction("INCOME", "Business", 450.00, "Freelance design job", today);

            // Add Mock Expenses
            dbHelper.addTransaction("EXPENSE", "Rent & Bills", 1200.00, "Appartment rent payment", today);
            dbHelper.addTransaction("EXPENSE", "Food", 184.30, "Weekly grocery hypermarket", today);
            dbHelper.addTransaction("EXPENSE", "Shopping", 95.00, "Winter jacket sale", today);
            dbHelper.addTransaction("EXPENSE", "Entertainment", 48.50, "Movie tickets and popcorn", today);
            dbHelper.addTransaction("EXPENSE", "Transport", 35.00, "Gas fill-up", today);
        }
    }

    private void updateUI() {
        // Formatted Month Header
        SimpleDateFormat monthYearFormat = new SimpleDateFormat("MMMM yyyy", Locale.US);
        tvCurrentMonth.setText(monthYearFormat.format(currentCalendar.getTime()));

        String monthPrefix = getMonthPrefix();

        // Retrieve summary statistics
        double incomeTotal = dbHelper.getMonthlyTotal("INCOME", monthPrefix);
        double expenseTotal = dbHelper.getMonthlyTotal("EXPENSE", monthPrefix);
        double netBalance = incomeTotal - expenseTotal;

        // Render Summary Cards
        tvTotalIncome.setText(String.format(Locale.US, "+$%.2f", incomeTotal));
        tvTotalExpense.setText(String.format(Locale.US, "-$%.2f", expenseTotal));

        if (netBalance >= 0) {
            tvNetBalance.setText(String.format(Locale.US, "$%.2f", netBalance));
            tvNetBalance.setTextColor(0xFF2E7D32); // Deep Green
        } else {
            tvNetBalance.setText(String.format(Locale.US, "-$%.2f", Math.abs(netBalance)));
            tvNetBalance.setTextColor(0xFFC62828); // Deep Red
        }

        // Render Reports and Lists
        renderCategoryReports(monthPrefix, expenseTotal);
        renderTransactionsHistory(monthPrefix);
    }

    private String getMonthPrefix() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM", Locale.US);
        return format.format(currentCalendar.getTime());
    }

    private void renderCategoryReports(String monthPrefix, double totalExpenses) {
        categoryReportContainer.removeAllViews();
        Map<String, Double> categoryTotals = dbHelper.getCategoryTotals(monthPrefix);

        if (categoryTotals.isEmpty()) {
            tvNoReport.setVisibility(View.VISIBLE);
        } else {
            tvNoReport.setVisibility(View.GONE);
            LayoutInflater inflater = LayoutInflater.from(this);

            for (Map.Entry<String, Double> entry : categoryTotals.entrySet()) {
                String category = entry.getKey();
                double amount = entry.getValue();
                int percentage = totalExpenses > 0 ? (int) Math.round((amount / totalExpenses) * 100) : 0;

                View reportRow = inflater.inflate(R.layout.item_category_report, categoryReportContainer, false);
                TextView tvCategory = (TextView) reportRow.findViewById(R.id.tv_report_category);
                TextView tvAmount = (TextView) reportRow.findViewById(R.id.tv_report_amount);
                ProgressBar pbProgress = (ProgressBar) reportRow.findViewById(R.id.pb_report_progress);

                tvCategory.setText(category + " (" + percentage + "%)");
                tvAmount.setText(String.format(Locale.US, "$%.2f", amount));
                pbProgress.setProgress(percentage);

                // Dynamically color-code category bar progress colors
                int color = getCategoryColor(category);
                pbProgress.getProgressDrawable().setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_IN);

                categoryReportContainer.addView(reportRow);
            }
        }
    }

    private void renderTransactionsHistory(String monthPrefix) {
        transactionsContainer.removeAllViews();
        Cursor cursor = null;

        try {
            cursor = dbHelper.getTransactionsForMonth(monthPrefix);

            if (cursor != null && cursor.getCount() > 0) {
                tvNoTransactions.setVisibility(View.GONE);
                LayoutInflater inflater = LayoutInflater.from(this);

                int idIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_ID);
                int typeIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_TYPE);
                int categoryIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_CATEGORY);
                int amountIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_AMOUNT);
                int noteIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_NOTE);
                int dateIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_DATE);

                while (cursor.moveToNext()) {
                    final long id = idIndex >= 0 ? cursor.getLong(idIndex) : 0;
                    final String type = typeIndex >= 0 ? cursor.getString(typeIndex) : "";
                    final String category = categoryIndex >= 0 ? cursor.getString(categoryIndex) : "";
                    final double amount = amountIndex >= 0 ? cursor.getDouble(amountIndex) : 0.0;
                    final String note = noteIndex >= 0 ? cursor.getString(noteIndex) : "";
                    final String dateStr = dateIndex >= 0 ? cursor.getString(dateIndex) : "";

                    final String safeCategory = (category == null) ? "Others" : category;

                    View itemRow = inflater.inflate(R.layout.item_transaction, transactionsContainer, false);

                    TextView tvBadge = (TextView) itemRow.findViewById(R.id.tv_badge);
                    TextView tvTitle = (TextView) itemRow.findViewById(R.id.tv_item_title);
                    TextView tvSub = (TextView) itemRow.findViewById(R.id.tv_item_sub);
                    TextView tvAmount = (TextView) itemRow.findViewById(R.id.tv_item_amount);
                    Button btnDelete = (Button) itemRow.findViewById(R.id.btn_item_delete);

                    // Render Circular Badge dynamically
                    String firstChar = safeCategory.length() > 0 ? safeCategory.substring(0, 1).toUpperCase(Locale.US) : "T";
                    tvBadge.setText(firstChar);

                    GradientDrawable shapeDrawable = new GradientDrawable();
                    shapeDrawable.setShape(GradientDrawable.OVAL);
                    shapeDrawable.setColor(getCategoryColor(safeCategory));
                    tvBadge.setBackground(shapeDrawable);

                    tvTitle.setText((note == null || note.trim().isEmpty()) ? safeCategory : note);
                    tvSub.setText(safeCategory + " • " + (dateStr == null ? "" : dateStr));

                    if ("INCOME".equals(type)) {
                        tvAmount.setText(String.format(Locale.US, "+$%.2f", amount));
                        tvAmount.setTextColor(0xFF4CAF50); // Green
                    } else {
                        tvAmount.setText(String.format(Locale.US, "-$%.2f", amount));
                        tvAmount.setTextColor(0xFFF44336); // Red
                    }

                    // Delete Action Handler
                    btnDelete.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            String displayNote = (note == null || note.trim().isEmpty()) ? safeCategory : note;
                            showDeleteConfirmation(id, displayNote);
                        }
                    });

                    transactionsContainer.addView(itemRow);
                }
            } else {
                tvNoTransactions.setVisibility(View.VISIBLE);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private void showDeleteConfirmation(final long id, String description) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Transaction");
        builder.setMessage("Are you sure you want to delete '" + description + "'?");
        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dbHelper.deleteTransaction(id);
                updateUI();
                Toast.makeText(MainActivity.this, "Transaction deleted successfully", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showAddTransactionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_add_transaction, null);
        builder.setView(dialogView);

        final RadioGroup rgType = (RadioGroup) dialogView.findViewById(R.id.rg_type);
        final EditText etAmount = (EditText) dialogView.findViewById(R.id.et_amount);
        final Spinner spCategory = (Spinner) dialogView.findViewById(R.id.sp_category);
        final EditText etNote = (EditText) dialogView.findViewById(R.id.et_note);
        final Button btnSelectDate = (Button) dialogView.findViewById(R.id.btn_select_date);

        final Calendar selectedDateCalendar = Calendar.getInstance();
        final SimpleDateFormat sqlDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

        // Prepopulate Spinners
        final ArrayAdapter<String> expenseAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, expenseCategories);
        expenseAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        final ArrayAdapter<String> incomeAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, incomeCategories);
        incomeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Default to Expense categories
        spCategory.setAdapter(expenseAdapter);

        // Swap adapter based on transaction type selected
        rgType.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.rb_income) {
                    spCategory.setAdapter(incomeAdapter);
                } else {
                    spCategory.setAdapter(expenseAdapter);
                }
            }
        });

        // Initialize Date text view
        btnSelectDate.setText(sqlDateFormat.format(selectedDateCalendar.getTime()));
        btnSelectDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatePickerDialog datePickerDialog = new DatePickerDialog(
                        MainActivity.this,
                        new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                                selectedDateCalendar.set(Calendar.YEAR, year);
                                selectedDateCalendar.set(Calendar.MONTH, monthOfYear);
                                selectedDateCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                                btnSelectDate.setText(sqlDateFormat.format(selectedDateCalendar.getTime()));
                            }
                        },
                        selectedDateCalendar.get(Calendar.YEAR),
                        selectedDateCalendar.get(Calendar.MONTH),
                        selectedDateCalendar.get(Calendar.DAY_OF_MONTH)
                );
                datePickerDialog.show();
            }
        });

        final AlertDialog dialog = builder.create();

        Button btnCancel = (Button) dialogView.findViewById(R.id.btn_dialog_cancel);
        Button btnSave = (Button) dialogView.findViewById(R.id.btn_dialog_save);

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String amountStr = etAmount.getText().toString().trim();
                if (amountStr.isEmpty()) {
                    etAmount.setError("Amount is required");
                    return;
                }

                double amount;
                try {
                    amount = Double.parseDouble(amountStr);
                } catch (NumberFormatException e) {
                    etAmount.setError("Enter a valid number");
                    return;
                }

                if (amount <= 0) {
                    etAmount.setError("Amount must be greater than zero");
                    return;
                }

                String type = (rgType.getCheckedRadioButtonId() == R.id.rb_income) ? "INCOME" : "EXPENSE";
                String category = spCategory.getSelectedItem().toString();
                String note = etNote.getText().toString().trim();
                String dateString = btnSelectDate.getText().toString();

                dbHelper.addTransaction(type, category, amount, note, dateString);
                dialog.dismiss();

                // Sync currentCalendar year/month to match transaction input for easy visual audit
                currentCalendar.setTime(selectedDateCalendar.getTime());

                updateUI();
                Toast.makeText(MainActivity.this, "Transaction added!", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private int getCategoryColor(String category) {
        if ("Salary".equals(category)) return 0xFF2E7D32;       // Green
        if ("Business".equals(category)) return 0xFF1565C0;     // Blue
        if ("Gifts".equals(category)) return 0xFF8E24AA;        // Purple
        if ("Food".equals(category)) return 0xFFEF6C00;         // Orange
        if ("Shopping".equals(category)) return 0xFFEC407A;     // Pink
        if ("Rent & Bills".equals(category)) return 0xFF455A64; // Blue Grey
        if ("Entertainment".equals(category)) return 0xFFAD1457;// Deep Pink
        if ("Transport".equals(category)) return 0xFF00838F;    // Teal
        if ("Health".equals(category)) return 0xFFD84315;       // Deep Orange
        if ("Education".equals(category)) return 0xFF6A1B9A;    // Deep Purple
        return 0xFF757575; // Slate Grey
    }

    // SQLite Helper Definition
    public static class DatabaseHelper extends SQLiteOpenHelper {
        private static final String DATABASE_NAME = "expenses.db";
        private static final int DATABASE_VERSION = 1;

        public static final String TABLE_TRANSACTIONS = "transactions";
        public static final String COLUMN_ID = "id";
        public static final String COLUMN_TYPE = "type";         // "INCOME" or "EXPENSE"
        public static final String COLUMN_CATEGORY = "category";
        public static final String COLUMN_AMOUNT = "amount";
        public static final String COLUMN_NOTE = "note";
        public static final String COLUMN_DATE = "date";         // String formatted: "YYYY-MM-DD"

        public DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            String createTable = "CREATE TABLE " + TABLE_TRANSACTIONS + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_TYPE + " TEXT, " +
                    COLUMN_CATEGORY + " TEXT, " +
                    COLUMN_AMOUNT + " REAL, " +
                    COLUMN_NOTE + " TEXT, " +
                    COLUMN_DATE + " TEXT)";
            db.execSQL(createTable);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRANSACTIONS);
            onCreate(db);
        }

        public void addTransaction(String type, String category, double amount, String note, String date) {
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(COLUMN_TYPE, type);
            values.put(COLUMN_CATEGORY, category);
            values.put(COLUMN_AMOUNT, amount);
            values.put(COLUMN_NOTE, note);
            values.put(COLUMN_DATE, date);
            db.insert(TABLE_TRANSACTIONS, null, values);
            db.close();
        }

        public void deleteTransaction(long id) {
            SQLiteDatabase db = this.getWritableDatabase();
            db.delete(TABLE_TRANSACTIONS, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
            db.close();
        }

        public int getTransactionCount() {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = null;
            int count = 0;
            try {
                cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_TRANSACTIONS, null);
                if (cursor != null && cursor.moveToFirst()) {
                    count = cursor.getInt(0);
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
            return count;
        }

        public double getMonthlyTotal(String type, String monthPrefix) {
            double total = 0;
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = null;
            try {
                cursor = db.rawQuery(
                        "SELECT SUM(" + COLUMN_AMOUNT + ") FROM " + TABLE_TRANSACTIONS +
                                " WHERE " + COLUMN_TYPE + " = ? AND " + COLUMN_DATE + " LIKE ?",
                        new String[]{type, monthPrefix + "%"}
                );
                if (cursor != null && cursor.moveToFirst()) {
                    total = cursor.getDouble(0);
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
            return total;
        }

        public Map<String, Double> getCategoryTotals(String monthPrefix) {
            Map<String, Double> totals = new LinkedHashMap<String, Double>();
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = null;
            try {
                cursor = db.rawQuery(
                        "SELECT " + COLUMN_CATEGORY + ", SUM(" + COLUMN_AMOUNT + ") FROM " + TABLE_TRANSACTIONS +
                                " WHERE " + COLUMN_TYPE + " = 'EXPENSE' AND " + COLUMN_DATE + " LIKE ?" +
                                " GROUP BY " + COLUMN_CATEGORY + " ORDER BY SUM(" + COLUMN_AMOUNT + ") DESC",
                        new String[]{monthPrefix + "%"}
                );
                if (cursor != null && cursor.moveToFirst()) {
                    int catIndex = cursor.getColumnIndex(COLUMN_CATEGORY);
                    int sumIndex = 1;
                    do {
                        String category = catIndex >= 0 ? cursor.getString(catIndex) : "Others";
                        if (category == null) category = "Others";
                        totals.put(category, cursor.getDouble(sumIndex));
                    } while (cursor.moveToNext());
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
            return totals;
        }

        public Cursor getTransactionsForMonth(String monthPrefix) {
            SQLiteDatabase db = this.getReadableDatabase();
            return db.rawQuery(
                    "SELECT * FROM " + TABLE_TRANSACTIONS +
                            " WHERE " + COLUMN_DATE + " LIKE ?" +
                            " ORDER BY " + COLUMN_DATE + " DESC, " + COLUMN_ID + " DESC",
                    new String[]{monthPrefix + "%"}
            );
        }
    }
}