package com.expensetrackerpro.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {

    private DatabaseHelper dbHelper;

    // View Components
    private ScrollViewWithCheck layoutDashboard;
    private View layoutAddTransaction;
    private View layoutReports;

    private Button navDashboard;
    private Button navAdd;
    private Button navReports;

    // Dashboard widgets
    private TextView tvDashBalance;
    private TextView tvDashIncome;
    private TextView tvDashExpenses;
    private ListView listTransactions;
    private TextView tvEmptyTransactions;

    // Add Form widgets
    private Spinner spinnerType;
    private Spinner spinnerCategory;
    private EditText editAmount;
    private EditText editDate;
    private EditText editDescription;
    private Button btnSaveTransaction;

    // Reports widgets
    private Spinner spinnerFilterMonth;
    private TextView tvReportTotalIncome;
    private TextView tvReportTotalExpenses;
    private TextView tvReportNetSavings;
    private LinearLayout containerCategoryReports;

    // Cached lists & properties
    private List<Transaction> allTransactions = new ArrayList<>();
    private TransactionAdapter transactionAdapter;
    private DecimalFormat currencyFormat = new DecimalFormat("$#,##0.00");

    private final String[] incomeCategories = {"Salary", "Investments", "Gifts", "Other Income"};
    private final String[] expenseCategories = {"Food", "Rent", "Utilities", "Transportation", "Entertainment", "Health", "Shopping", "Other Expense"};
    private final String[] monthsFilter = {"2024-12", "2024-11", "2024-10", "2024-09", "2024-08", "2024-07", "2024-06", "2024-05", "2024-04", "2024-03", "2024-02", "2024-01", "2023-12", "2023-11", "2023-10"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);

        // Core Layout views
        layoutDashboard = findViewById(R.id.layout_dashboard);
        layoutAddTransaction = findViewById(R.id.layout_add_transaction);
        layoutReports = findViewById(R.id.layout_reports);

        // Nav buttons
        navDashboard = findViewById(R.id.nav_dashboard);
        navAdd = findViewById(R.id.nav_add);
        navReports = findViewById(R.id.nav_reports);

        // Dashboard setup
        tvDashBalance = findViewById(R.id.tv_dash_balance);
        tvDashIncome = findViewById(R.id.tv_dash_income);
        tvDashExpenses = findViewById(R.id.tv_dash_expenses);
        listTransactions = findViewById(R.id.list_transactions);
        tvEmptyTransactions = findViewById(R.id.tv_empty_transactions);

        // Input Form Setup
        spinnerType = findViewById(R.id.spinner_type);
        spinnerCategory = findViewById(R.id.spinner_category);
        editAmount = findViewById(R.id.edit_amount);
        editDate = findViewById(R.id.edit_date);
        editDescription = findViewById(R.id.edit_description);
        btnSaveTransaction = findViewById(R.id.btn_save_transaction);

        // Reports Setup
        spinnerFilterMonth = findViewById(R.id.spinner_filter_month);
        tvReportTotalIncome = findViewById(R.id.tv_report_total_income);
        tvReportTotalExpenses = findViewById(R.id.tv_report_total_expenses);
        tvReportNetSavings = findViewById(R.id.tv_report_net_savings);
        containerCategoryReports = findViewById(R.id.container_category_reports);

        // Set up Listeners
        setupNavigation();
        setupFormElements();
        setupReportsFilter();

        // Feed basic list view
        allTransactions = dbHelper.getAllTransactions();
        transactionAdapter = new TransactionAdapter();
        listTransactions.setAdapter(transactionAdapter);

        // Refresh components state
        refreshDashboardData();
        setCurrentDateInForm();
    }

    private void setCurrentDateInForm() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        editDate.setText(sdf.format(new Date()));
    }

    private void setupNavigation() {
        navDashboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showView(0);
            }
        });

        navAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showView(1);
            }
        });

        navReports.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showView(2);
            }
        });
    }

    private void showView(int viewIndex) {
        layoutDashboard.setVisibility(viewIndex == 0 ? View.VISIBLE : View.GONE);
        layoutAddTransaction.setVisibility(viewIndex == 1 ? View.VISIBLE : View.GONE);
        layoutReports.setVisibility(viewIndex == 2 ? View.VISIBLE : View.GONE);

        // Adjust navigation visual active markers
        navDashboard.setTextColor(viewIndex == 0 ? 0xFF263238 : 0xFF666666);
        navDashboard.setTextSize(viewIndex == 0 ? 14 : 12);

        navAdd.setTextColor(viewIndex == 1 ? 0xFF263238 : 0xFF666666);
        navAdd.setTextSize(viewIndex == 1 ? 14 : 12);

        navReports.setTextColor(viewIndex == 2 ? 0xFF263238 : 0xFF666666);
        navReports.setTextSize(viewIndex == 2 ? 14 : 12);

        // Trigger loading when shifting view focus
        if (viewIndex == 0) {
            refreshDashboardData();
        } else if (viewIndex == 2) {
            loadReportsForSelectedMonth();
        }
    }

    private void setupFormElements() {
        // Types spinner Setup
        String[] types = {"Expense", "Income"};
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, types);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerType.setAdapter(typeAdapter);

        spinnerType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedType = parent.getItemAtPosition(position).toString();
                updateCategoryDropdown(selectedType);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Save Action listener
        btnSaveTransaction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveTransaction();
            }
        });
    }

    private void updateCategoryDropdown(String type) {
        String[] categories = type.equals("Income") ? incomeCategories : expenseCategories;
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(categoryAdapter);
    }

    private void setupReportsFilter() {
        ArrayAdapter<String> monthAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, monthsFilter);
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilterMonth.setAdapter(monthAdapter);

        // Default to current month/year if matchable
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM", Locale.US);
        String currentFormatted = sdf.format(cal.getTime());
        for (int i = 0; i < monthsFilter.length; i++) {
            if (monthsFilter[i].equals(currentFormatted)) {
                spinnerFilterMonth.setSelection(i);
                break;
            }
        }

        spinnerFilterMonth.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadReportsForSelectedMonth();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void refreshDashboardData() {
        allTransactions = dbHelper.getAllTransactions();
        transactionAdapter.notifyDataSetChanged();

        if (allTransactions.isEmpty()) {
            tvEmptyTransactions.setVisibility(View.VISIBLE);
            listTransactions.setVisibility(View.GONE);
        } else {
            tvEmptyTransactions.setVisibility(View.GONE);
            listTransactions.setVisibility(View.VISIBLE);
        }

        double totalIncome = 0;
        double totalExpenses = 0;

        for (int i = 0; i < allTransactions.size(); i++) {
            Transaction t = allTransactions.get(i);
            if (t.getType().equalsIgnoreCase("Income")) {
                totalIncome += t.getAmount();
            } else {
                totalExpenses += t.getAmount();
            }
        }

        double netBalance = totalIncome - totalExpenses;

        tvDashBalance.setText(currencyFormat.format(netBalance));
        tvDashIncome.setText(currencyFormat.format(totalIncome));
        tvDashExpenses.setText(currencyFormat.format(totalExpenses));

        // Dynamically color code total balance layout
        if (netBalance >= 0) {
            tvDashBalance.setTextColor(0xFF2E7D32); // Positive dark green
        } else {
            tvDashBalance.setTextColor(0xFFC62828); // Negative deep red
        }
    }

    private void saveTransaction() {
        String amountStr = editAmount.getText().toString().trim();
        String dateStr = editDate.getText().toString().trim();
        String descStr = editDescription.getText().toString().trim();
        String typeStr = spinnerType.getSelectedItem().toString();
        String categoryStr = spinnerCategory.getSelectedItem().toString();

        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Please write a valid amount", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid number pattern, rewrite", Toast.LENGTH_SHORT).show();
            return;
        }

        if (amount <= 0) {
            Toast.makeText(this, "Amount should be higher than 0", Toast.LENGTH_SHORT).show();
            return;
        }

        if (dateStr.isEmpty() || !dateStr.matches("\\d{4}-\\d{2}-\\d{2}")) {
            Toast.makeText(this, "Use correct date form (YYYY-MM-DD)", Toast.LENGTH_SHORT).show();
            return;
        }

        if (descStr.isEmpty()) {
            descStr = categoryStr + " Entry";
        }

        long resultId = dbHelper.insertTransaction(amount, typeStr, categoryStr, descStr, dateStr);
        if (resultId > 0) {
            Toast.makeText(this, "Transaction Saved Successfully", Toast.LENGTH_SHORT).show();
            // Clear entry inputs
            editAmount.setText("");
            editDescription.setText("");
            setCurrentDateInForm();

            // Redirect home
            showView(0);
        } else {
            Toast.makeText(this, "Error storing transaction detail", Toast.LENGTH_LONG).show();
        }
    }

    private void loadReportsForSelectedMonth() {
        String targetFilter = spinnerFilterMonth.getSelectedItem().toString();

        double totalIncome = dbHelper.getTotalAmount("Income", targetFilter);
        double totalExpenses = dbHelper.getTotalAmount("Expense", targetFilter);
        double netSavings = totalIncome - totalExpenses;

        tvReportTotalIncome.setText("Income: " + currencyFormat.format(totalIncome));
        tvReportTotalExpenses.setText("Expenses: " + currencyFormat.format(totalExpenses));
        tvReportNetSavings.setText("Net Savings: " + currencyFormat.format(netSavings));

        if (netSavings >= 0) {
            tvReportNetSavings.setTextColor(0xFF2E7D32);
        } else {
            tvReportNetSavings.setTextColor(0xFFC62828);
        }

        // Render Category wise spending breakdown
        containerCategoryReports.removeAllViews();
        Map<String, Double> categoryStats = dbHelper.getCategoryStats("Expense", targetFilter);

        if (categoryStats.isEmpty()) {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("No categorization metrics to demonstrate.");
            tvEmpty.setPadding(16, 16, 16, 16);
            tvEmpty.setTextSize(14sp);
            tvEmpty.setTextColor(0xFF888888);
            containerCategoryReports.addView(tvEmpty);
        } else {
            LayoutInflater inflater = LayoutInflater.from(this);
            for (Map.Entry<String, Double> entry : categoryStats.entrySet()) {
                View row = inflater.inflate(R.layout.item_report, containerCategoryReports, false);
                TextView categoryName = row.findViewById(R.id.report_tv_category);
                TextView categorySum = row.findViewById(R.id.report_tv_amount);

                categoryName.setText(entry.getKey());
                categorySum.setText(currencyFormat.format(entry.getValue()));

                containerCategoryReports.addView(row);
            }
        }
    }

    // Adapt layout views to ensure smooth rendering
    private static class ScrollViewWithCheck extends android.widget.ScrollView {
        public ScrollViewWithCheck(android.content.Context context, android.util.AttributeSet attrs) {
            super(context, attrs);
        }
    }

    // List view Adapter Implementation targeting Transaction Models
    private class TransactionAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return allTransactions.size();
        }

        @Override
        public Object getItem(int position) {
            return allTransactions.get(position);
        }

        @Override
        public long getItemId(int position) {
            return allTransactions.get(position).getId();
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(MainActivity.this).inflate(R.layout.item_transaction, parent, false);
            }

            final Transaction item = allTransactions.get(position);

            TextView tvCategory = convertView.findViewById(R.id.tv_category);
            TextView tvDescription = convertView.findViewById(R.id.tv_description);
            TextView tvDate = convertView.findViewById(R.id.tv_date);
            TextView tvAmount = convertView.findViewById(R.id.tv_amount);
            Button btnDelete = convertView.findViewById(R.id.btn_delete);

            tvCategory.setText(item.getCategory());
            tvDescription.setText(item.getDescription());
            tvDate.setText(item.getDate());

            if (item.getType().equalsIgnoreCase("Income")) {
                tvAmount.setText("+" + currencyFormat.format(item.getAmount()));
                tvAmount.setTextColor(0xFF2E7D32); // Green
            } else {
                tvAmount.setText("-" + currencyFormat.format(item.getAmount()));
                tvAmount.setTextColor(0xFFC62828); // Red
            }

            btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean status = dbHelper.deleteTransaction(item.getId());
                    if (status) {
                        Toast.makeText(MainActivity.this, "Transaction Removed", Toast.LENGTH_SHORT).show();
                        refreshDashboardData();
                    } else {
                        Toast.makeText(MainActivity.this, "Could not delete row entry", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            return convertView;
        }
    }
}