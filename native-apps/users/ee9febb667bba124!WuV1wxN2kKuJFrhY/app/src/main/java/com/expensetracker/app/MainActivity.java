package com.expensetracker.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
    private DatabaseHelper dbHelper;
    private Calendar currentCalendar = Calendar.getInstance();
    private Calendar transactionCalendar = Calendar.getInstance();

    private SimpleDateFormat monthYearFormatter = new SimpleDateFormat("MMMM yyyy", Locale.US);
    private SimpleDateFormat sqlMonthFormatter = new SimpleDateFormat("yyyy-MM", Locale.US);
    private SimpleDateFormat sqlDateFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    private String selectedDateStr;

    // UI Widgets
    private TextView headerMonth;
    private TextView tvTotalBalance, tvTotalIncome, tvTotalExpense;
    private TextView tvSelectedMonth;
    private Button btnPrevMonth, btnNextMonth;
    private LinearLayout containerTransactions;
    private TextView tvEmptyState;

    // Registration input widgets
    private RadioGroup rgType;
    private EditText etAmount, etNote;
    private Spinner spinnerCategory;
    private LinearLayout layoutDatePicker;
    private TextView tvDate;
    private Button btnSaveTransaction;

    // Reports Widgets
    private TextView tvSavingsRate;
    private ProgressBar pbSavingsRate;
    private TextView tvReportsEmptyState;
    private LinearLayout containerCategoryReports;

    // View panels
    private LinearLayout layoutDashboard, layoutAddTransaction, layoutReports;
    private LinearLayout tabDashboard, tabAdd, tabReports;
    private TextView tvTabDashboardLabel, tvTabAddLabel, tvTabReportsLabel;
    private View indicatorDashboard, indicatorAdd, indicatorReports;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);

        // Header View
        headerMonth = (TextView) findViewById(R.id.header_month);

        // Dashboard Balances
        tvTotalBalance = (TextView) findViewById(R.id.tv_total_balance);
        tvTotalIncome = (TextView) findViewById(R.id.tv_total_income);
        tvTotalExpense = (TextView) findViewById(R.id.tv_total_expense);

        // Month Selector components
        tvSelectedMonth = (TextView) findViewById(R.id.tv_selected_month);
        btnPrevMonth = (Button) findViewById(R.id.btn_prev_month);
        btnNextMonth = (Button) findViewById(R.id.btn_next_month);

        // Transaction Lists
        containerTransactions = (LinearLayout) findViewById(R.id.container_transactions);
        tvEmptyState = (TextView) findViewById(R.id.tv_empty_state);

        // Registration form fields
        rgType = (RadioGroup) findViewById(R.id.rg_type);
        etAmount = (EditText) findViewById(R.id.et_amount);
        etNote = (EditText) findViewById(R.id.et_note);
        spinnerCategory = (Spinner) findViewById(R.id.spinner_category);
        layoutDatePicker = (LinearLayout) findViewById(R.id.layout_date_picker);
        tvDate = (TextView) findViewById(R.id.tv_date);
        btnSaveTransaction = (Button) findViewById(R.id.btn_save_transaction);

        // Analytics Components
        tvSavingsRate = (TextView) findViewById(R.id.tv_savings_rate);
        pbSavingsRate = (ProgressBar) findViewById(R.id.pb_savings_rate);
        tvReportsEmptyState = (TextView) findViewById(R.id.tv_reports_empty_state);
        containerCategoryReports = (LinearLayout) findViewById(R.id.container_category_reports);

        // View Switchers
        layoutDashboard = (LinearLayout) findViewById(R.id.layout_dashboard);
        layoutAddTransaction = (LinearLayout) findViewById(R.id.layout_add_transaction);
        layoutReports = (LinearLayout) findViewById(R.id.layout_reports);

        // Tab indicators
        tabDashboard = (LinearLayout) findViewById(R.id.tab_dashboard);
        tabAdd = (LinearLayout) findViewById(R.id.tab_add);
        tabReports = (LinearLayout) findViewById(R.id.tab_reports);

        tvTabDashboardLabel = (TextView) findViewById(R.id.tv_tab_dashboard_label);
        tvTabAddLabel = (TextView) findViewById(R.id.tv_tab_add_label);
        tvTabReportsLabel = (TextView) findViewById(R.id.tv_tab_reports_label);

        indicatorDashboard = findViewById(R.id.indicator_dashboard);
        indicatorAdd = findViewById(R.id.indicator_add);
        indicatorReports = findViewById(R.id.indicator_reports);

        // Setup current calendar date value
        selectedDateStr = sqlDateFormatter.format(transactionCalendar.getTime());
        tvDate.setText(selectedDateStr);

        // Form categories loading
        updateCategorySpinner("EXPENSE");

        setupListeners();
        loadData();
    }

    private void setupListeners() {
        btnPrevMonth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentCalendar.add(Calendar.MONTH, -1);
                loadData();
            }
        });

        btnNextMonth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentCalendar.add(Calendar.MONTH, 1);
                loadData();
            }
        });

        layoutDatePicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectDate();
            }
        });

        rgType.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.rb_expense) {
                    updateCategorySpinner("EXPENSE");
                } else {
                    updateCategorySpinner("INCOME");
                }
            }
        });

        btnSaveTransaction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveTransaction();
            }
        });

        tabDashboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab("dashboard");
            }
        });

        tabAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab("add");
            }
        });

        tabReports.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab("reports");
            }
        });
    }

    private void selectDate() {
        DatePickerDialog.OnDateSetListener dateSetListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                transactionCalendar.set(Calendar.YEAR, year);
                transactionCalendar.set(Calendar.MONTH, monthOfYear);
                transactionCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                selectedDateStr = sqlDateFormatter.format(transactionCalendar.getTime());
                tvDate.setText(selectedDateStr);
            }
        };
        new DatePickerDialog(MainActivity.this, dateSetListener,
                transactionCalendar.get(Calendar.YEAR),
                transactionCalendar.get(Calendar.MONTH),
                transactionCalendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void updateCategorySpinner(String type) {
        String[] categories;
        if ("EXPENSE".equals(type)) {
            categories = new String[]{"Food", "Transport", "Rent", "Utilities", "Entertainment", "Shopping", "Other"};
        } else {
            categories = new String[]{"Salary", "Business", "Investments", "Other"};
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);
    }

    private void switchTab(String tabName) {
        if ("dashboard".equals(tabName)) {
            layoutDashboard.setVisibility(View.VISIBLE);
            layoutAddTransaction.setVisibility(View.GONE);
            layoutReports.setVisibility(View.GONE);

            indicatorDashboard.setVisibility(View.VISIBLE);
            indicatorAdd.setVisibility(View.INVISIBLE);
            indicatorReports.setVisibility(View.INVISIBLE);

            tvTabDashboardLabel.setTextColor(getResources().getColor(R.color.primary));
            tvTabAddLabel.setTextColor(getResources().getColor(R.color.text_secondary));
            tvTabReportsLabel.setTextColor(getResources().getColor(R.color.text_secondary));

            loadData();
        } else if ("add".equals(tabName)) {
            layoutDashboard.setVisibility(View.GONE);
            layoutAddTransaction.setVisibility(View.VISIBLE);
            layoutReports.setVisibility(View.GONE);

            indicatorDashboard.setVisibility(View.INVISIBLE);
            indicatorAdd.setVisibility(View.VISIBLE);
            indicatorReports.setVisibility(View.INVISIBLE);

            tvTabDashboardLabel.setTextColor(getResources().getColor(R.color.text_secondary));
            tvTabAddLabel.setTextColor(getResources().getColor(R.color.primary));
            tvTabReportsLabel.setTextColor(getResources().getColor(R.color.text_secondary));
        } else if ("reports".equals(tabName)) {
            layoutDashboard.setVisibility(View.GONE);
            layoutAddTransaction.setVisibility(View.GONE);
            layoutReports.setVisibility(View.VISIBLE);

            indicatorDashboard.setVisibility(View.INVISIBLE);
            indicatorAdd.setVisibility(View.INVISIBLE);
            indicatorReports.setVisibility(View.VISIBLE);

            tvTabDashboardLabel.setTextColor(getResources().getColor(R.color.text_secondary));
            tvTabAddLabel.setTextColor(getResources().getColor(R.color.text_secondary));
            tvTabReportsLabel.setTextColor(getResources().getColor(R.color.primary));

            loadData();
        }
    }

    private void loadData() {
        String monthStr = sqlMonthFormatter.format(currentCalendar.getTime());
        tvSelectedMonth.setText(monthYearFormatter.format(currentCalendar.getTime()));
        headerMonth.setText(monthYearFormatter.format(currentCalendar.getTime()));

        List<Transaction> transactions = dbHelper.getTransactionsByMonth(monthStr);

        double totalIncome = 0;
        double totalExpense = 0;

        containerTransactions.removeAllViews();

        if (transactions.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            LayoutInflater inflater = LayoutInflater.from(this);
            for (int i = 0; i < transactions.size(); i++) {
                final Transaction t = transactions.get(i);
                View itemView = inflater.inflate(R.layout.item_transaction, containerTransactions, false);

                View categoryIndicator = itemView.findViewById(R.id.view_category_indicator);
                TextView tvCategory = (TextView) itemView.findViewById(R.id.tv_item_category);
                TextView tvNote = (TextView) itemView.findViewById(R.id.tv_item_note);
                TextView tvDateVal = (TextView) itemView.findViewById(R.id.tv_item_date);
                TextView tvAmount = (TextView) itemView.findViewById(R.id.tv_item_amount);
                Button btnDelete = (Button) itemView.findViewById(R.id.btn_delete_transaction);

                categoryIndicator.setBackgroundColor(getCategoryColor(t.getCategory()));
                tvCategory.setText(t.getCategory());
                tvNote.setText(t.getNote().isEmpty() ? "No Details" : t.getNote());
                tvDateVal.setText(t.getDate());

                if ("INCOME".equals(t.getType())) {
                    totalIncome += t.getAmount();
                    tvAmount.setText(String.format(Locale.US, "+$%.2f", t.getAmount()));
                    tvAmount.setTextColor(getResources().getColor(R.color.income_green));
                } else {
                    totalExpense += t.getAmount();
                    tvAmount.setText(String.format(Locale.US, "-$%.2f", t.getAmount()));
                    tvAmount.setTextColor(getResources().getColor(R.color.expense_red));
                }

                btnDelete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        confirmDelete(t.getId());
                    }
                });

                containerTransactions.addView(itemView);
            }
        }

        double totalBalance = totalIncome - totalExpense;
        tvTotalBalance.setText(String.format(Locale.US, "$%.2f", totalBalance));
        tvTotalIncome.setText(String.format(Locale.US, "+$%.2f", totalIncome));
        tvTotalExpense.setText(String.format(Locale.US, "-$%.2f", totalExpense));

        // Update Reports View
        updateReports(transactions, totalIncome, totalExpense);
    }

    private int getCategoryColor(String category) {
        if ("Food".equals(category)) {
            return getResources().getColor(R.color.color_food);
        } else if ("Transport".equals(category)) {
            return getResources().getColor(R.color.color_transport);
        } else if ("Rent".equals(category)) {
            return getResources().getColor(R.color.color_rent);
        } else if ("Utilities".equals(category)) {
            return getResources().getColor(R.color.color_utilities);
        } else if ("Entertainment".equals(category)) {
            return getResources().getColor(R.color.color_entertainment);
        } else if ("Shopping".equals(category)) {
            return getResources().getColor(R.color.color_shopping);
        } else if ("Salary".equals(category)) {
            return getResources().getColor(R.color.income_green);
        } else if ("Business".equals(category)) {
            return getResources().getColor(R.color.balance_blue);
        } else if ("Investments".equals(category)) {
            return getResources().getColor(R.color.primary);
        } else {
            return getResources().getColor(R.color.color_other);
        }
    }

    private void confirmDelete(final long id) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Remove Record");
        builder.setMessage("Delete transaction entry permanently?");
        builder.setPositiveButton("Remove", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dbHelper.deleteTransaction(id);
                loadData();
                Toast.makeText(MainActivity.this, "Transaction entry removed", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void saveTransaction() {
        String amountStr = etAmount.getText().toString().trim();
        if (amountStr.isEmpty()) {
            Toast.makeText(MainActivity.this, "Please specify transaction amount", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            Toast.makeText(MainActivity.this, "Enter a valid cost amount", Toast.LENGTH_SHORT).show();
            return;
        }

        if (amount <= 0) {
            Toast.makeText(MainActivity.this, "Transaction must exceed zero value", Toast.LENGTH_SHORT).show();
            return;
        }

        String type = rgType.getCheckedRadioButtonId() == R.id.rb_expense ? "EXPENSE" : "INCOME";
        String category = spinnerCategory.getSelectedItem().toString();
        String note = etNote.getText().toString().trim();

        long result = dbHelper.insertTransaction(amount, type, category, selectedDateStr, note);
        if (result != -1) {
            Toast.makeText(MainActivity.this, "Transaction entry saved successfully", Toast.LENGTH_SHORT).show();

            // Form Reset
            etAmount.setText("");
            etNote.setText("");
            transactionCalendar = Calendar.getInstance();
            selectedDateStr = sqlDateFormatter.format(transactionCalendar.getTime());
            tvDate.setText(selectedDateStr);

            switchTab("dashboard");
        } else {
            Toast.makeText(MainActivity.this, "Failed saving database transaction", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateReports(List<Transaction> transactions, double totalIncome, double totalExpense) {
        containerCategoryReports.removeAllViews();

        if (transactions.isEmpty() || totalExpense == 0) {
            tvReportsEmptyState.setVisibility(View.VISIBLE);
            tvSavingsRate.setText("0.0%");
            pbSavingsRate.setProgress(0);
            return;
        }
        tvReportsEmptyState.setVisibility(View.GONE);

        // Savings Rate metric
        double savingsRate = 0;
        if (totalIncome > 0) {
            double savings = totalIncome - totalExpense;
            if (savings > 0) {
                savingsRate = (savings / totalIncome) * 100;
            }
        }
        tvSavingsRate.setText(String.format(Locale.US, "%.1f%%", savingsRate));
        pbSavingsRate.setProgress((int) Math.min(Math.max(savingsRate, 0), 100));

        // Aggregate category expenses
        Map<String, Double> categorySums = new HashMap<>();
        for (int i = 0; i < transactions.size(); i++) {
            Transaction t = transactions.get(i);
            if ("EXPENSE".equals(t.getType())) {
                double prevSum = categorySums.containsKey(t.getCategory()) ? categorySums.get(t.getCategory()) : 0;
                categorySums.put(t.getCategory(), prevSum + t.getAmount());
            }
        }

        if (categorySums.isEmpty()) {
            tvReportsEmptyState.setVisibility(View.VISIBLE);
            return;
        }

        // Build list items represent distribution ratios
        LayoutInflater inflater = LayoutInflater.from(this);
        for (Map.Entry<String, Double> entry : categorySums.entrySet()) {
            String category = entry.getKey();
            double sum = entry.getValue();
            double percentage = (sum / totalExpense) * 100;

            View barView = inflater.inflate(R.layout.item_category_report, containerCategoryReports, false);
            TextView tvCatName = (TextView) barView.findViewById(R.id.tv_report_category_name);
            TextView tvCatAmt = (TextView) barView.findViewById(R.id.tv_report_category_amount);
            ProgressBar pbCat = (ProgressBar) barView.findViewById(R.id.pb_report_category);

            tvCatName.setText(category);
            tvCatAmt.setText(String.format(Locale.US, "$%.2f (%.1f%%)", sum, percentage));

            pbCat.setProgress((int) percentage);
            int color = getCategoryColor(category);
            pbCat.getProgressDrawable().setColorFilter(color, PorterDuff.Mode.SRC_IN);

            containerCategoryReports.addView(barView);
        }
    }
}