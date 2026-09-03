package com.expensetrackerpro.app;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
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

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {

    // Tab Navigation
    private Button btnTabDashboard;
    private Button btnTabAdd;
    private Button btnTabReports;

    private View panelDashboard;
    private View panelAddTransaction;
    private View panelReports;

    // Database helper
    private DatabaseHelper dbHelper;

    // Dashboard widgets
    private TextView txtTotalBalance;
    private TextView txtTotalIncome;
    private TextView txtTotalExpense;
    private LinearLayout containerTransactions;
    private TextView txtEmptyTransactions;

    // Add Form elements
    private RadioGroup radioGroupType;
    private RadioButton radioExpense;
    private RadioButton radioIncome;
    private EditText editTitle;
    private EditText editAmount;
    private Spinner spinnerCategory;
    private TextView txtSelectedDate;
    private Button btnChangeDate;
    private Button btnSaveTransaction;

    // Categories list arrays
    private final String[] EXPENSE_CATEGORIES = {"Food", "Transport", "Rent", "Shopping", "Entertainment", "Utilities", "Other"};
    private final String[] INCOME_CATEGORIES = {"Salary", "Business", "Freelance", "Investment", "Gifts", "Other"};

    // Date Tracker variables
    private Calendar formCalendar;
    private Calendar reportCalendar;
    private SimpleDateFormat dbDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    // Reports Panel elements
    private TextView txtReportMonthHeader;
    private Button btnPrevMonth;
    private Button btnNextMonth;
    private TextView txtReportTotalIncome;
    private TextView txtReportTotalExpense;
    private LinearLayout containerCategoryReports;
    private TextView txtEmptyReports;

    private DecimalFormat moneyFormat = new DecimalFormat("$#,##0.00");
    private SimpleDateFormat reportHeaderFormat = new SimpleDateFormat("MMMM yyyy", Locale.US);
    private SimpleDateFormat queryMonthFormat = new SimpleDateFormat("yyyy-MM", Locale.US);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);
        formCalendar = Calendar.getInstance();
        reportCalendar = Calendar.getInstance();

        initUI();
        registerEvents();
        refreshAllData();
    }

    private void initUI() {
        // Tab Layouts
        btnTabDashboard = (Button) findViewById(R.id.btn_tab_dashboard);
        btnTabAdd = (Button) findViewById(R.id.btn_tab_add);
        btnTabReports = (Button) findViewById(R.id.btn_tab_reports);

        panelDashboard = findViewById(R.id.panel_dashboard);
        panelAddTransaction = findViewById(R.id.panel_add_transaction);
        panelReports = findViewById(R.id.panel_reports);

        // Dashboard content elements
        txtTotalBalance = (TextView) findViewById(R.id.txt_total_balance);
        txtTotalIncome = (TextView) findViewById(R.id.txt_total_income);
        txtTotalExpense = (TextView) findViewById(R.id.txt_total_expense);
        containerTransactions = (LinearLayout) findViewById(R.id.container_transactions);
        txtEmptyTransactions = (TextView) findViewById(R.id.txt_empty_transactions);

        // Add form inputs
        radioGroupType = (RadioGroup) findViewById(R.id.radio_group_type);
        radioExpense = (RadioButton) findViewById(R.id.radio_expense);
        radioIncome = (RadioButton) findViewById(R.id.radio_income);
        editTitle = (EditText) findViewById(R.id.edit_title);
        editAmount = (EditText) findViewById(R.id.edit_amount);
        spinnerCategory = (Spinner) findViewById(R.id.spinner_category);
        txtSelectedDate = (TextView) findViewById(R.id.txt_selected_date);
        btnChangeDate = (Button) findViewById(R.id.btn_change_date);
        btnSaveTransaction = (Button) findViewById(R.id.btn_save_transaction);

        txtSelectedDate.setText(dbDateFormat.format(formCalendar.getTime()));

        // Reports View components
        txtReportMonthHeader = (TextView) findViewById(R.id.txt_report_month_header);
        btnPrevMonth = (Button) findViewById(R.id.btn_prev_month);
        btnNextMonth = (Button) findViewById(R.id.btn_next_month);
        txtReportTotalIncome = (TextView) findViewById(R.id.txt_report_total_income);
        txtReportTotalExpense = (TextView) findViewById(R.id.txt_report_total_expense);
        containerCategoryReports = (LinearLayout) findViewById(R.id.container_category_reports);
        txtEmptyReports = (TextView) findViewById(R.id.txt_empty_reports);

        setupCategorySpinner(true); // default layout is Expense
    }

    private void registerEvents() {
        // Navigation Bar Actions
        btnTabDashboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switchTab(0);
            }
        });
        btnTabAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switchTab(1);
            }
        });
        btnTabReports.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switchTab(2);
            }
        });

        // Add Transaction: Toggle Radio types
        radioGroupType.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.radio_income) {
                    setupCategorySpinner(false);
                } else {
                    setupCategorySpinner(true);
                }
            }
        });

        // Add Transaction: Pick custom dates
        btnChangeDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatePickerDialog dateDialog = new DatePickerDialog(MainActivity.this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        formCalendar.set(Calendar.YEAR, year);
                        formCalendar.set(Calendar.MONTH, monthOfYear);
                        formCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        txtSelectedDate.setText(dbDateFormat.format(formCalendar.getTime()));
                    }
                }, formCalendar.get(Calendar.YEAR), formCalendar.get(Calendar.MONTH), formCalendar.get(Calendar.DAY_OF_MONTH));
                dateDialog.show();
            }
        });

        // Add Transaction: Save event logic
        btnSaveTransaction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveTransactionRecord();
            }
        });

        // Month control actions
        btnPrevMonth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reportCalendar.add(Calendar.MONTH, -1);
                updateReportsPanel();
            }
        });

        btnNextMonth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reportCalendar.add(Calendar.MONTH, 1);
                updateReportsPanel();
            }
        });
    }

    private void switchTab(int index) {
        // Set highlights
        btnTabDashboard.setTextColor(index == 0 ? getResources().getColor(R.color.primary) : getResources().getColor(R.color.text_secondary));
        btnTabDashboard.setTextStyle(index == 0 ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);

        btnTabAdd.setTextColor(index == 1 ? getResources().getColor(R.color.primary) : getResources().getColor(R.color.text_secondary));
        btnTabAdd.setTextStyle(index == 1 ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);

        btnTabReports.setTextColor(index == 2 ? getResources().getColor(R.color.primary) : getResources().getColor(R.color.text_secondary));
        btnTabReports.setTextStyle(index == 2 ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);

        // Hide or Show Panels
        panelDashboard.setVisibility(index == 0 ? View.VISIBLE : View.GONE);
        panelAddTransaction.setVisibility(index == 1 ? View.VISIBLE : View.GONE);
        panelReports.setVisibility(index == 2 ? View.VISIBLE : View.GONE);

        if (index == 0 || index == 2) {
            refreshAllData();
        }
    }

    private void setupCategorySpinner(boolean isExpense) {
        String[] selectedCategories = isExpense ? EXPENSE_CATEGORIES : INCOME_CATEGORIES;
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, selectedCategories) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text = (TextView) view.findViewById(android.R.id.text1);
                text.setTextColor(getResources().getColor(R.color.text_primary));
                text.setTextSize(14sp);
                return view;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);
    }

    private void saveTransactionRecord() {
        String title = editTitle.getText().toString().trim();
        String amountText = editAmount.getText().toString().trim();
        String category = spinnerCategory.getSelectedItem() != null ? spinnerCategory.getSelectedItem().toString() : "Other";
        String dateString = txtSelectedDate.getText().toString();

        if (title.isEmpty()) {
            Toast.makeText(this, "Please insert a transaction title.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (amountText.isEmpty()) {
            Toast.makeText(this, "Please enter an amount.", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountText);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid number formatted amount.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (amount <= 0) {
            Toast.makeText(this, "Amount should be greater than $0.00.", Toast.LENGTH_SHORT).show();
            return;
        }

        String type = radioExpense.isChecked() ? "EXPENSE" : "INCOME";

        long result = dbHelper.insertTransaction(title, amount, type, category, dateString);
        if (result != -1) {
            Toast.makeText(this, "Record saved successfully!", Toast.LENGTH_SHORT).show();
            // Reset fields
            editTitle.setText("");
            editAmount.setText("");
            formCalendar = Calendar.getInstance();
            txtSelectedDate.setText(dbDateFormat.format(formCalendar.getTime()));
            radioExpense.setChecked(true);

            // Go back to dashboard screen
            switchTab(0);
        } else {
            Toast.makeText(this, "Failed saving transaction, try again.", Toast.LENGTH_SHORT).show();
        }
    }

    private void refreshAllData() {
        updateDashboardPanel();
        updateReportsPanel();
    }

    private void updateDashboardPanel() {
        List<Transaction> transactions = dbHelper.getAllTransactionsSorted();
        containerTransactions.removeAllViews();

        double totalIncome = 0;
        double totalExpense = 0;

        if (transactions.isEmpty()) {
            txtEmptyTransactions.setVisibility(View.VISIBLE);
        } else {
            txtEmptyTransactions.setVisibility(View.GONE);
            LayoutInflater inflater = LayoutInflater.from(this);

            for (int i = 0; i < transactions.size(); i++) {
                final Transaction t = transactions.get(i);
                View row = inflater.inflate(R.layout.item_transaction, containerTransactions, false);

                TextView txtTitle = (TextView) row.findViewById(R.id.txt_item_title);
                TextView txtDetails = (TextView) row.findViewById(R.id.txt_item_details);
                TextView txtAmount = (TextView) row.findViewById(R.id.txt_item_amount);

                txtTitle.setText(t.getTitle());
                txtDetails.setText(t.getCategory() + "  •  " + t.getDate());

                if (t.getType().equals("INCOME")) {
                    totalIncome += t.getAmount();
                    txtAmount.setText("+" + moneyFormat.format(t.getAmount()));
                    txtAmount.setTextColor(getResources().getColor(R.color.income_green));
                } else {
                    totalExpense += t.getAmount();
                    txtAmount.setText("-" + moneyFormat.format(t.getAmount()));
                    txtAmount.setTextColor(getResources().getColor(R.color.expense_red));
                }

                // Delete records on long press
                row.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        confirmDeleteTransaction(t);
                        return true;
                    }
                });

                containerTransactions.addView(row);
            }
        }

        double balance = totalIncome - totalExpense;
        txtTotalBalance.setText(moneyFormat.format(balance));
        txtTotalIncome.setText(moneyFormat.format(totalIncome));
        txtTotalExpense.setText(moneyFormat.format(totalExpense));

        if (balance >= 0) {
            txtTotalBalance.setTextColor(getResources().getColor(R.color.text_primary));
        } else {
            txtTotalBalance.setTextColor(getResources().getColor(R.color.expense_red));
        }
    }

    private void confirmDeleteTransaction(final Transaction transaction) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Transaction");
        builder.setMessage("Are you sure you want to delete '" + transaction.getTitle() + "'?");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dbHelper.deleteTransaction(transaction.getId());
                Toast.makeText(MainActivity.this, "Transaction deleted", Toast.LENGTH_SHORT).show();
                refreshAllData();
            }
        });
        builder.setNegativeButton("No", null);
        builder.show();
    }

    private void updateReportsPanel() {
        String queryDate = queryMonthFormat.format(reportCalendar.getTime());
        txtReportMonthHeader.setText(reportHeaderFormat.format(reportCalendar.getTime()));

        List<Transaction> monthlyList = dbHelper.getTransactionsForMonth(queryDate);
        containerCategoryReports.removeAllViews();

        double mIncome = 0;
        double mExpense = 0;

        // Group expenses by category
        HashMap<String, Double> categoryTotals = new HashMap<>();

        for (int i = 0; i < monthlyList.size(); i++) {
            Transaction t = monthlyList.get(i);
            if (t.getType().equals("INCOME")) {
                mIncome += t.getAmount();
            } else {
                mExpense += t.getAmount();
                Double current = categoryTotals.get(t.getCategory());
                if (current == null) current = 0.0;
                categoryTotals.put(t.getCategory(), current + t.getAmount());
            }
        }

        txtReportTotalIncome.setText(moneyFormat.format(mIncome));
        txtReportTotalExpense.setText(moneyFormat.format(mExpense));

        if (monthlyList.isEmpty()) {
            txtEmptyReports.setVisibility(View.VISIBLE);
        } else {
            txtEmptyReports.setVisibility(View.GONE);

            // Sort map values descending
            List<Map.Entry<String, Double>> sortedCategories = new ArrayList<>(categoryTotals.entrySet());
            Collections.sort(sortedCategories, new Comparator<Map.Entry<String, Double>>() {
                @Override
                public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
                    return o2.getValue().compareTo(o1.getValue());
                }
            });

            LayoutInflater inflater = LayoutInflater.from(this);
            for (int k = 0; k < sortedCategories.size(); k++) {
                Map.Entry<String, Double> entry = sortedCategories.get(k);
                View categoryRow = inflater.inflate(R.layout.item_report_category, containerCategoryReports, false);

                TextView name = (TextView) categoryRow.findViewById(R.id.txt_report_cat_name);
                TextView amount = (TextView) categoryRow.findViewById(R.id.txt_report_cat_amount);
                ProgressBar progress = (ProgressBar) categoryRow.findViewById(R.id.progress_report_bar);
                TextView percentageText = (TextView) categoryRow.findViewById(R.id.txt_report_cat_percentage);

                name.setText(entry.getKey());
                amount.setText(moneyFormat.format(entry.getValue()));

                double percent = mExpense > 0 ? (entry.getValue() / mExpense) * 100 : 0;
                progress.setProgress((int) percent);
                percentageText.setText(String.format(Locale.US, "%.1f%% of total monthly expenses", percent));

                containerCategoryReports.addView(categoryRow);
            }
        }
    }
}