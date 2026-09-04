package com.expensetrackerpro.app;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {

    private DatabaseHelper dbHelper;
    private List<Transaction> transactionList;
    private TransactionAdapter transactionAdapter;

    // Content container panels
    private View layoutHome;
    private View layoutAdd;
    private View layoutReports;

    // Bottom Navigation Elements
    private View tabHomeTrigger;
    private View tabAddTrigger;
    private View tabReportsTrigger;
    private TextView txtTabHome;
    private TextView txtTabAdd;
    private TextView txtTabReports;

    // Top metrics Dashboard TextView references
    private TextView txtNetBalance;
    private TextView txtIncomeSummary;
    private TextView txtExpenseSummary;

    // Form selection variables
    private Button btnTypeExpense;
    private Button btnTypeIncome;
    private EditText edtAmount;
    private Spinner spnCategory;
    private Button btnSelectDate;
    private EditText edtNote;
    private Button btnSaveTransaction;

    private String selectedType = "EXPENSE"; // Default
    private Calendar selectedCalendar;
    private String[] categories = {"Food", "Rent", "Salary", "Entertainment", "Utilities", "Transport", "Freelance", "Other"};
    private Map<String, Integer> categoryColors;

    // Reports layout elements
    private ChartView chartView;
    private LinearLayout layoutBreakdownList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);
        transactionList = new ArrayList<>();
        selectedCalendar = Calendar.getInstance();

        initCategoryColors();
        initViews();
        setupNavigationListeners();
        setupFormControllers();
        
        // Initial setup data synchronization
        loadTransactionsData();
        switchToTab("HOME");
    }

    private void initCategoryColors() {
        categoryColors = new HashMap<>();
        categoryColors.put("Food", Color.parseColor("#E74C3C"));
        categoryColors.put("Rent", Color.parseColor("#3498DB"));
        categoryColors.put("Salary", Color.parseColor("#2ECC71"));
        categoryColors.put("Entertainment", Color.parseColor("#9B59B6"));
        categoryColors.put("Utilities", Color.parseColor("#F1C40F"));
        categoryColors.put("Transport", Color.parseColor("#1ABC9C"));
        categoryColors.put("Freelance", Color.parseColor("#34495E"));
        categoryColors.put("Other", Color.parseColor("#95A5A6"));
    }

    private void initViews() {
        // Tab screens panels
        layoutHome = findViewById(R.id.layoutHome);
        layoutAdd = findViewById(R.id.layoutAdd);
        layoutReports = findViewById(R.id.layoutReports);

        // Tab item trigger blocks
        tabHomeTrigger = findViewById(R.id.tabHomeTrigger);
        tabAddTrigger = findViewById(R.id.tabAddTrigger);
        tabReportsTrigger = findViewById(R.id.tabReportsTrigger);
        txtTabHome = (TextView) findViewById(R.id.txtTabHome);
        txtTabAdd = (TextView) findViewById(R.id.txtTabAdd);
        txtTabReports = (TextView) findViewById(R.id.txtTabReports);

        // Header analytics labels
        txtNetBalance = (TextView) findViewById(R.id.txtNetBalance);
        txtIncomeSummary = (TextView) findViewById(R.id.txtIncomeSummary);
        txtExpenseSummary = (TextView) findViewById(R.id.txtExpenseSummary);

        // Input Form attributes
        btnTypeExpense = (Button) findViewById(R.id.btnTypeExpense);
        btnTypeIncome = (Button) findViewById(R.id.btnTypeIncome);
        edtAmount = (EditText) findViewById(R.id.edtAmount);
        spnCategory = (Spinner) findViewById(R.id.spnCategory);
        btnSelectDate = (Button) findViewById(R.id.btnSelectDate);
        edtNote = (EditText) findViewById(R.id.edtNote);
        btnSaveTransaction = (Button) findViewById(R.id.btnSaveTransaction);

        // Report components
        chartView = (ChartView) findViewById(R.id.chartView);
        layoutBreakdownList = (LinearLayout) findViewById(R.id.layoutBreakdownList);

        // Setup Main Listing Adapter
        ListView lstTransactions = (ListView) findViewById(R.id.lstTransactions);
        transactionAdapter = new TransactionAdapter(this, transactionList);
        lstTransactions.setAdapter(transactionAdapter);

        TextView btnDeleteAll = (TextView) findViewById(R.id.btnDeleteAll);
        btnDeleteAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                db.delete(DatabaseHelper.TABLE_TRANSACTIONS, null, null);
                db.close();
                Toast.makeText(MainActivity.this, "Local database flushed", Toast.LENGTH_SHORT).show();
                loadTransactionsData();
            }
        });
    }

    private void setupNavigationListeners() {
        tabHomeTrigger.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchToTab("HOME");
            }
        });

        tabAddTrigger.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchToTab("ADD");
            }
        });

        tabReportsTrigger.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchToTab("REPORTS");
            }
        });
    }

    private void switchToTab(String tabName) {
        // Toggle view container panels layout visibility
        layoutHome.setVisibility(View.GONE);
        layoutAdd.setVisibility(View.GONE);
        layoutReports.setVisibility(View.GONE);

        // Clear active highlighting labels colors
        txtTabHome.setTextColor(Color.parseColor("#7F8C8D"));
        txtTabAdd.setTextColor(Color.parseColor("#7F8C8D"));
        txtTabReports.setTextColor(Color.parseColor("#7F8C8D"));

        if (tabName.equals("HOME")) {
            layoutHome.setVisibility(View.VISIBLE);
            txtTabHome.setTextColor(Color.parseColor("#1E272E"));
        } else if (tabName.equals("ADD")) {
            layoutAdd.setVisibility(View.VISIBLE);
            txtTabAdd.setTextColor(Color.parseColor("#1E272E"));
            resetFormFields();
        } else if (tabName.equals("REPORTS")) {
            layoutReports.setVisibility(View.VISIBLE);
            txtTabReports.setTextColor(Color.parseColor("#1E272E"));
            generateReports();
        }
    }

    private void setupFormControllers() {
        // Form Transaction Type selection toggle switches
        btnTypeExpense.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedType = "EXPENSE";
                btnTypeExpense.setBackgroundColor(Color.parseColor("#E74C3C"));
                btnTypeExpense.setTextColor(Color.WHITE);
                btnTypeIncome.setBackgroundColor(Color.parseColor("#BDC3C7"));
                btnTypeIncome.setTextColor(Color.parseColor("#7F8C8D"));
            }
        });

        btnTypeIncome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedType = "INCOME";
                btnTypeIncome.setBackgroundColor(Color.parseColor("#2ECC71"));
                btnTypeIncome.setTextColor(Color.WHITE);
                btnTypeExpense.setBackgroundColor(Color.parseColor("#BDC3C7"));
                btnTypeExpense.setTextColor(Color.parseColor("#7F8C8D"));
            }
        });

        // Initialize Category dropdown items Spinner layout adapter
        ArrayAdapter<String> spinAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        spinAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnCategory.setAdapter(spinAdapter);

        // Wire Date picker dialog action popup triggers
        updateDateButtonText();
        btnSelectDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new DatePickerDialog(MainActivity.this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        selectedCalendar.set(Calendar.YEAR, year);
                        selectedCalendar.set(Calendar.MONTH, month);
                        selectedCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        updateDateButtonText();
                    }
                }, selectedCalendar.get(Calendar.YEAR), selectedCalendar.get(Calendar.MONTH), selectedCalendar.get(Calendar.DAY_OF_MONTH)).show();
            }
        });

        // Click handler listener mapping storing forms
        btnSaveTransaction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveTransaction();
            }
        });
    }

    private void updateDateButtonText() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        btnSelectDate.setText("Date: " + sdf.format(selectedCalendar.getTime()));
    }

    private void resetFormFields() {
        edtAmount.setText("");
        edtNote.setText("");
        selectedCalendar = Calendar.getInstance();
        updateDateButtonText();
        spnCategory.setSelection(0);
        
        // Default select status back to Expense
        selectedType = "EXPENSE";
        btnTypeExpense.setBackgroundColor(Color.parseColor("#E74C3C"));
        btnTypeExpense.setTextColor(Color.WHITE);
        btnTypeIncome.setBackgroundColor(Color.parseColor("#BDC3C7"));
        btnTypeIncome.setTextColor(Color.parseColor("#7F8C8D"));
    }

    private void saveTransaction() {
        String amountStr = edtAmount.getText().toString().trim();
        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Please insert valid transaction amount", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Number calculation format error", Toast.LENGTH_SHORT).show();
            return;
        }

        if (amount <= 0) {
            Toast.makeText(this, "Value must exceed 0", Toast.LENGTH_SHORT).show();
            return;
        }

        String category = spnCategory.getSelectedItem().toString();
        String noteStr = edtNote.getText().toString().trim();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String dateStr = sdf.format(selectedCalendar.getTime());

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_AMOUNT, amount);
        values.put(DatabaseHelper.COLUMN_TYPE, selectedType);
        values.put(DatabaseHelper.COLUMN_CATEGORY, category);
        values.put(DatabaseHelper.COLUMN_DATE, dateStr);
        values.put(DatabaseHelper.COLUMN_NOTE, noteStr);

        long resultRowId = db.insert(DatabaseHelper.TABLE_TRANSACTIONS, null, values);
        db.close();

        if (resultRowId != -1) {
            Toast.makeText(this, "Transaction logged", Toast.LENGTH_SHORT).show();
            loadTransactionsData();
            switchToTab("HOME");
        } else {
            Toast.makeText(this, "Fatal Database Storage Error", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadTransactionsData() {
        transactionList.clear();

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                DatabaseHelper.TABLE_TRANSACTIONS,
                null, null, null, null, null,
                DatabaseHelper.COLUMN_DATE + " DESC, " + DatabaseHelper.COLUMN_ID + " DESC"
        );

        double totalIncome = 0;
        double totalExpenses = 0;

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID));
                double amount = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_AMOUNT));
                String type = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TYPE));
                String category = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CATEGORY));
                String date = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DATE));
                String note = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_NOTE));

                Transaction tx = new Transaction(id, amount, type, category, date, note);
                transactionList.add(tx);

                if (type.equals("INCOME")) {
                    totalIncome += amount;
                } else {
                    totalExpenses += amount;
                }

            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();

        // Refresh dynamic components elements UI listings
        transactionAdapter.notifyDataSetChanged();

        // Format and render summary layouts
        txtIncomeSummary.setText("$" + String.format("%.2f", totalIncome));
        txtExpenseSummary.setText("$" + String.format("%.2f", totalExpenses));

        double balance = totalIncome - totalExpenses;
        txtNetBalance.setText((balance < 0 ? "-" : "") + "$" + String.format("%.2f", Math.abs(balance)));
        if (balance < 0) {
            txtNetBalance.setTextColor(Color.parseColor("#E74C3C"));
        } else {
            txtNetBalance.setTextColor(Color.parseColor("#2ECC71"));
        }
    }

    private void generateReports() {
        layoutBreakdownList.removeAllViews();

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT " + DatabaseHelper.COLUMN_CATEGORY + ", SUM(" + DatabaseHelper.COLUMN_AMOUNT + ") AS total_val FROM " +
                        DatabaseHelper.TABLE_TRANSACTIONS +
                        " WHERE " + DatabaseHelper.COLUMN_TYPE + " = 'EXPENSE' " +
                        " GROUP BY " + DatabaseHelper.COLUMN_CATEGORY +
                        " ORDER BY total_val DESC", null
        );

        List<ChartView.ChartData> chartDataList = new ArrayList<>();

        if (cursor.moveToFirst()) {
            do {
                String category = cursor.getString(0);
                double sum = cursor.getDouble(1);

                int color = Color.GRAY;
                if (categoryColors.containsKey(category)) {
                    color = categoryColors.get(category);
                }

                chartDataList.add(new ChartView.ChartData(category, sum, color));

                // Generate linear breakdown text views list dynamically
                LinearLayout itemRow = new LinearLayout(this);
                itemRow.setOrientation(LinearLayout.HORIZONTAL);
                itemRow.setPadding(10, 16, 10, 16);

                // Small color bullet point indicator
                View indicator = new View(this);
                LinearLayout.LayoutParams indicatorParams = new LinearLayout.LayoutParams(26, 26);
                indicatorParams.gravity = android.view.Gravity.CENTER_VERTICAL;
                indicatorParams.rightMargin = 22;
                indicator.setLayoutParams(indicatorParams);
                indicator.setBackgroundColor(color);
                itemRow.addView(indicator);

                // Category Text block details
                TextView labelView = new TextView(this);
                labelView.setText(category);
                labelView.setTextSize(14);
                labelView.setTextColor(Color.parseColor("#2C3E50"));
                itemRow.addView(labelView, new LinearLayout.LayoutParams(0, -2, 1.0f));

                // Formatted valuation text
                TextView valView = new TextView(this);
                valView.setText("$" + String.format("%.2f", sum));
                valView.setTextSize(14);
                valView.setTextColor(Color.parseColor("#2C3E50"));
                valView.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
                itemRow.addView(valView);

                layoutBreakdownList.addView(itemRow);

            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();

        chartView.setData(chartDataList);
    }

    // Inner Transactions BaseAdapter class
    private class TransactionAdapter extends BaseAdapter {
        private List<Transaction> list;

        public TransactionAdapter(android.content.Context context, List<Transaction> list) {
            this.list = list;
        }

        @Override
        public int getCount() { return list.size(); }
        @Override
        public Object getItem(int position) { return list.get(position); }
        @Override
        public long getItemId(int position) { return list.get(position).getId(); }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                // Programmatic linear layout configuration for stable item layout display
                LinearLayout rowLayout = new LinearLayout(MainActivity.this);
                rowLayout.setOrientation(LinearLayout.HORIZONTAL);
                rowLayout.setPadding(32, 24, 32, 24);
                rowLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);

                // Category Title Box
                TextView txtCategory = new TextView(MainActivity.this);
                txtCategory.setId(101);
                txtCategory.setTextSize(16);
                txtCategory.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
                txtCategory.setTextColor(Color.parseColor("#2C3E50"));
                LinearLayout.LayoutParams paramsCat = new LinearLayout.LayoutParams(0, -2, 1.0f);
                rowLayout.addView(txtCategory, paramsCat);

                // Details Text block containing notes & timestamps
                LinearLayout detailsLayout = new LinearLayout(MainActivity.this);
                detailsLayout.setOrientation(LinearLayout.VERTICAL);
                detailsLayout.setPadding(20, 0, 20, 0);

                TextView txtNote = new TextView(MainActivity.this);
                txtNote.setId(102);
                txtNote.setTextSize(13);
                txtNote.setTextColor(Color.parseColor("#7F8C8D"));
                detailsLayout.addView(txtNote);

                TextView txtDate = new TextView(MainActivity.this);
                txtDate.setId(103);
                txtDate.setTextSize(11);
                txtDate.setTextColor(Color.parseColor("#BDC3C7"));
                detailsLayout.addView(txtDate);

                LinearLayout.LayoutParams paramsDetails = new LinearLayout.LayoutParams(0, -2, 1.5f);
                rowLayout.addView(detailsLayout, paramsDetails);

                // Numeric Amount Box
                TextView txtAmount = new TextView(MainActivity.this);
                txtAmount.setId(104);
                txtAmount.setTextSize(16);
                txtAmount.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
                txtAmount.setGravity(android.view.Gravity.RIGHT);
                LinearLayout.LayoutParams paramsAmount = new LinearLayout.LayoutParams(-2, -2);
                rowLayout.addView(txtAmount, paramsAmount);

                convertView = rowLayout;
            }

            Transaction item = list.get(position);
            TextView cat = (TextView) convertView.findViewById(101);
            TextView note = (TextView) convertView.findViewById(102);
            TextView date = (TextView) convertView.findViewById(103);
            TextView amt = (TextView) convertView.findViewById(104);

            cat.setText(item.getCategory());
            note.setText(item.getNote() != null && !item.getNote().isEmpty() ? item.getNote() : "No details entered");
            date.setText(item.getDate());

            if (item.getType().equals("INCOME")) {
                amt.setText("+$" + String.format("%.2f", item.getAmount()));
                amt.setTextColor(Color.parseColor("#2ECC71"));
            } else {
                amt.setText("-$" + String.format("%.2f", item.getAmount()));
                amt.setTextColor(Color.parseColor("#E74C3C"));
            }

            return convertView;
        }
    }
}