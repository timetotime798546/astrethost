package com.expensetracker.app;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.LinearLayout;
import android.view.View;
import android.widget.Toast;
import android.app.Dialog;
import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.app.AlertDialog;
import android.widget.EditText;
import android.widget.AdapterView;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ArrayAdapter;

public class MainActivity extends Activity {

    private DatabaseHelper dbHelper;

    // View Navigation Layout Containers
    private View viewDashboard;
    private View viewHistory;
    private View viewReports;

    // Navigation Buttons and Labels
    private View navDashboard;
    private View navHistory;
    private View navReports;
    private TextView tvNavDashboard;
    private TextView tvNavHistory;
    private TextView tvNavReports;

    // Dashboard references
    private TextView tvTotalBalance;
    private TextView tvDashIncome;
    private TextView tvDashExpense;
    private Button btnIncome;
    private Button btnExpense;
    private ListView lvRecentTransactions;
    private TextView tvViewAllRecent;

    // History references
    private Spinner spFilterMonth;
    private Spinner spFilterCategory;
    private TextView tvHistoryEmptyState;
    private ListView lvAllTransactions;

    // Reports references
    private Spinner spReportMonth;
    private TextView tvReportTitle;
    private TextView tvReportIncome;
    private TextView tvReportExpense;
    private TextView tvReportSavings;
    private LinearLayout containerCategoryReport;
    private TextView tvReportEmptyState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);

        // Map layout elements
        viewDashboard = findViewById(R.id.view_dashboard);
        viewHistory = findViewById(R.id.view_history);
        viewReports = findViewById(R.id.view_reports);

        navDashboard = findViewById(R.id.nav_dashboard);
        navHistory = findViewById(R.id.nav_history);
        navReports = findViewById(R.id.nav_reports);

        tvNavDashboard = findViewById(R.id.tv_nav_dashboard);
        tvNavHistory = findViewById(R.id.tv_nav_history);
        tvNavReports = findViewById(R.id.tv_nav_reports);

        // Dashboard views
        tvTotalBalance = findViewById(R.id.tv_total_balance);
        tvDashIncome = findViewById(R.id.tv_dash_income);
        tvDashExpense = findViewById(R.id.tv_dash_expense);
        btnIncome = findViewById(R.id.btn_add_income);
        btnExpense = findViewById(R.id.btn_add_expense);
        lvRecentTransactions = findViewById(R.id.lv_recent_transactions);
        tvViewAllRecent = findViewById(R.id.tv_view_all_recent);

        // History filters
        spFilterMonth = findViewById(R.id.sp_filter_month);
        spFilterCategory = findViewById(R.id.sp_filter_category);
        tvHistoryEmptyState = findViewById(R.id.tv_history_empty_state);
        lvAllTransactions = findViewById(R.id.lv_all_transactions);

        // Reports views
        spReportMonth = findViewById(R.id.sp_report_month);
        tvReportTitle = findViewById(R.id.tv_report_title);
        tvReportIncome = findViewById(R.id.tv_report_income);
        tvReportExpense = findViewById(R.id.tv_report_expense);
        tvReportSavings = findViewById(R.id.tv_report_savings);
        containerCategoryReport = findViewById(R.id.container_category_report);
        tvReportEmptyState = findViewById(R.id.tv_report_empty_state);

        // Bottom Tab Clicks
        navDashboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab("dashboard");
            }
        });

        navHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab("history");
            }
        });

        navReports.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab("reports");
            }
        });

        // Dashboard Interactions
        btnIncome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddTransactionDialog("income");
            }
        });

        btnExpense.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddTransactionDialog("expense");
            }
        });

        tvViewAllRecent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab("history");
            }
        });

        // Initialize display configuration
        switchTab("dashboard");
    }

    private void switchTab(String tabName) {
        viewDashboard.setVisibility(View.GONE);
        viewHistory.setVisibility(View.GONE);
        viewReports.setVisibility(View.GONE);

        // Revert bottom selection styles
        tvNavDashboard.setTextColor(Color.parseColor("#666666"));
        tvNavHistory.setTextColor(Color.parseColor("#666666"));
        tvNavReports.setTextColor(Color.parseColor("#666666"));

        navDashboard.setBackgroundColor(Color.TRANSPARENT);
        navHistory.setBackgroundColor(Color.TRANSPARENT);
        navReports.setBackgroundColor(Color.TRANSPARENT);

        if ("dashboard".equals(tabName)) {
            viewDashboard.setVisibility(View.VISIBLE);
            tvNavDashboard.setTextColor(Color.parseColor("#008080"));
            navDashboard.setBackgroundColor(Color.parseColor("#F0F9F9"));
            loadDashboardData();
        } else if ("history".equals(tabName)) {
            viewHistory.setVisibility(View.VISIBLE);
            tvNavHistory.setTextColor(Color.parseColor("#008080"));
            navHistory.setBackgroundColor(Color.parseColor("#F0F9F9"));
            setupHistoryFilters();
            loadHistoryData();
        } else if ("reports".equals(tabName)) {
            viewReports.setVisibility(View.VISIBLE);
            tvNavReports.setTextColor(Color.parseColor("#008080"));
            navReports.setBackgroundColor(Color.parseColor("#F0F9F9"));
            setupReportFilters();
            loadReportsData();
        }
    }

    private void loadDashboardData() {
        Map<String, Double> summary = dbHelper.getOverallSummary();
        double balance = summary.get("balance") != null ? summary.get("balance") : 0.0;
        double income = summary.get("income") != null ? summary.get("income") : 0.0;
        double expense = summary.get("expense") != null ? summary.get("expense") : 0.0;

        tvTotalBalance.setText(String.format("$%.2f", balance));
        if (balance >= 0) {
            tvTotalBalance.setTextColor(Color.parseColor("#2E7D32"));
        } else {
            tvTotalBalance.setTextColor(Color.parseColor("#C62828"));
        }

        tvDashIncome.setText(String.format("$%.2f", income));
        tvDashExpense.setText(String.format("$%.2f", expense));

        // Load limited historical entries
        List<DatabaseHelper.Transaction> recentList = dbHelper.getRecentTransactions(10);
        if (recentList.isEmpty()) {
            lvRecentTransactions.setVisibility(View.GONE);
        } else {
            lvRecentTransactions.setVisibility(View.VISIBLE);
            TransactionAdapter adapter = new TransactionAdapter(recentList);
            lvRecentTransactions.setAdapter(adapter);

            lvRecentTransactions.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    DatabaseHelper.Transaction transaction = (DatabaseHelper.Transaction) parent.getItemAtPosition(position);
                    confirmDeletion(transaction);
                }
            });
        }
    }

    private void setupHistoryFilters() {
        // Build Month filtering details
        List<String> months = new ArrayList<>();
        months.add("All Time");
        months.addAll(dbHelper.getAvailableMonths());

        ArrayAdapter<String> monthAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, months);
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spFilterMonth.setAdapter(monthAdapter);

        // Build category categorization details
        List<String> categories = new ArrayList<>();
        categories.add("All Categories");
        categories.addAll(dbHelper.getCategories("income"));
        categories.addAll(dbHelper.getCategories("expense"));

        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, categories);
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spFilterCategory.setAdapter(catAdapter);

        AdapterView.OnItemSelectedListener filterListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadHistoryData();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };

        spFilterMonth.setOnItemSelectedListener(filterListener);
        spFilterCategory.setOnItemSelectedListener(filterListener);
    }

    private void loadHistoryData() {
        Object monthObj = spFilterMonth.getSelectedItem();
        Object catObj = spFilterCategory.getSelectedItem();

        String month = monthObj != null ? monthObj.toString() : "All Time";
        String category = catObj != null ? catObj.toString() : "All Categories";

        List<DatabaseHelper.Transaction> transactions = dbHelper.getFilteredTransactions(month, category);
        if (transactions.isEmpty()) {
            tvHistoryEmptyState.setVisibility(View.VISIBLE);
            lvAllTransactions.setVisibility(View.GONE);
        } else {
            tvHistoryEmptyState.setVisibility(View.GONE);
            lvAllTransactions.setVisibility(View.VISIBLE);
            TransactionAdapter adapter = new TransactionAdapter(transactions);
            lvAllTransactions.setAdapter(adapter);

            lvAllTransactions.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    DatabaseHelper.Transaction transaction = (DatabaseHelper.Transaction) parent.getItemAtPosition(position);
                    confirmDeletion(transaction);
                }
            });
        }
    }

    private void setupReportFilters() {
        List<String> months = dbHelper.getAvailableMonths();
        if (months.isEmpty()) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
            months.add(sdf.format(new Date()));
        }

        ArrayAdapter<String> reportMonthAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, months);
        reportMonthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spReportMonth.setAdapter(reportMonthAdapter);

        spReportMonth.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadReportsData();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void loadReportsData() {
        Object monthObj = spReportMonth.getSelectedItem();
        if (monthObj == null) {
            return;
        }
        String monthStr = monthObj.toString();
        tvReportTitle.setText("Summary for " + monthStr);

        Map<String, Double> summary = dbHelper.getMonthlySummary(monthStr);
        double income = summary.get("income") != null ? summary.get("income") : 0.0;
        double expense = summary.get("expense") != null ? summary.get("expense") : 0.0;
        double savings = summary.get("savings") != null ? summary.get("savings") : 0.0;

        tvReportIncome.setText(String.format("$%.2f", income));
        tvReportExpense.setText(String.format("$%.2f", expense));
        tvReportSavings.setText(String.format("$%.2f", savings));
        if (savings >= 0) {
            tvReportSavings.setTextColor(Color.parseColor("#008080"));
        } else {
            tvReportSavings.setTextColor(Color.parseColor("#C62828"));
        }

        // Render dynamic category item summaries
        List<DatabaseHelper.CategorySummary> categorySummaries = dbHelper.getCategorySummary(monthStr);
        containerCategoryReport.removeAllViews();

        if (categorySummaries.isEmpty()) {
            tvReportEmptyState.setVisibility(View.VISIBLE);
        } else {
            tvReportEmptyState.setVisibility(View.GONE);
            LayoutInflater inflater = LayoutInflater.from(this);

            for (int i = 0; i < categorySummaries.size(); i++) {
                DatabaseHelper.CategorySummary item = categorySummaries.get(i);
                View view = inflater.inflate(R.layout.item_report_category, containerCategoryReport, false);

                TextView tvCatName = view.findViewById(R.id.tv_rep_cat_name);
                TextView tvCatPercent = view.findViewById(R.id.tv_rep_cat_percent);
                TextView tvCatAmount = view.findViewById(R.id.tv_rep_cat_amount);
                View progressBar = view.findViewById(R.id.view_progress_bar);
                View progressEmpty = view.findViewById(R.id.view_progress_empty);

                tvCatName.setText(item.category);
                tvCatAmount.setText(String.format("$%.2f", item.totalAmount));

                int percent = 0;
                if (expense > 0) {
                    percent = (int) Math.round((item.totalAmount / expense) * 100);
                }
                tvCatPercent.setText(percent + "%");

                // Assign layout parameters to render width proportions dynamically
                LinearLayout.LayoutParams progressParams =
                        new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, percent);
                progressBar.setLayoutParams(progressParams);

                LinearLayout.LayoutParams emptyParams =
                        new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 100 - percent);
                progressEmpty.setLayoutParams(emptyParams);

                containerCategoryReport.addView(view);
            }
        }
    }

    private void confirmDeletion(final DatabaseHelper.Transaction transaction) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Transaction");
        builder.setMessage("Are you sure you want to delete this transaction for " + String.format("$%.2f", transaction.amount) + "?");
        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int deleted = dbHelper.deleteTransaction(transaction.id);
                if (deleted > 0) {
                    Toast.makeText(MainActivity.this, "Deleted successfully", Toast.LENGTH_SHORT).show();
                    loadDashboardData();
                    loadHistoryData();
                    setupHistoryFilters();
                    setupReportFilters();
                    loadReportsData();
                } else {
                    Toast.makeText(MainActivity.this, "Deletion failed", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showAddTransactionDialog(final String type) {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_add_transaction);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView tvTitle = dialog.findViewById(R.id.tv_dialog_title);
        final EditText etAmount = dialog.findViewById(R.id.et_dialog_amount);
        final Spinner spCategory = dialog.findViewById(R.id.sp_dialog_category);
        Button btnAddCategory = dialog.findViewById(R.id.btn_dialog_add_category);
        final EditText etDesc = dialog.findViewById(R.id.et_dialog_desc);
        final EditText etDate = dialog.findViewById(R.id.et_dialog_date);
        Button btnCancel = dialog.findViewById(R.id.btn_dialog_cancel);
        Button btnSave = dialog.findViewById(R.id.btn_dialog_save);

        tvTitle.setText("income".equals(type) ? "Add Income" : "Add Expense");

        // Load corresponding selector drop values
        final List<String> categories = dbHelper.getCategories(type);
        final ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, categories);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCategory.setAdapter(spinnerAdapter);

        // Establish Default Date
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        etDate.setText(sdf.format(new Date()));

        etDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Calendar c = Calendar.getInstance();
                int year = c.get(Calendar.YEAR);
                int month = c.get(Calendar.MONTH);
                int day = c.get(Calendar.DAY_OF_MONTH);

                DatePickerDialog datePickerDialog = new DatePickerDialog(MainActivity.this,
                        new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(android.widget.DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                                String dateFormatted = String.format(Locale.US, "%04d-%02d-%02d", year, monthOfYear + 1, dayOfMonth);
                                etDate.setText(dateFormatted);
                            }
                        }, year, month, day);
                datePickerDialog.show();
            }
        });

        // Add custom category listener
        btnAddCategory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddCategoryDialog(type, spCategory, spinnerAdapter);
            }
        });

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
                String desc = etDesc.getText().toString().trim();
                String date = etDate.getText().toString().trim();
                Object selectedCat = spCategory.getSelectedItem();

                if (amountStr.isEmpty()) {
                    etAmount.setError("Amount is required");
                    return;
                }

                double amount;
                try {
                    amount = Double.parseDouble(amountStr);
                } catch (NumberFormatException e) {
                    etAmount.setError("Invalid amount format");
                    return;
                }

                if (amount <= 0) {
                    etAmount.setError("Amount must be greater than zero");
                    return;
                }

                if (selectedCat == null) {
                    Toast.makeText(MainActivity.this, "Please select or add a category", Toast.LENGTH_SHORT).show();
                    return;
                }

                String category = selectedCat.toString();

                long res = dbHelper.addTransaction(amount, type, category, desc, date);
                if (res != -1) {
                    Toast.makeText(MainActivity.this, "Transaction saved!", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    loadDashboardData();
                    setupHistoryFilters();
                    setupReportFilters();
                } else {
                    Toast.makeText(MainActivity.this, "Error saving transaction", Toast.LENGTH_SHORT).show();
                }
            }
        });

        dialog.show();
    }

    private void showAddCategoryDialog(final String type, final Spinner spinner, final ArrayAdapter<String> adapter) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Custom Category");

        final EditText input = new EditText(this);
        input.setHint("Category Name");
        builder.setView(input);

        builder.setPositiveButton("Add", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String catName = input.getText().toString().trim();
                if (!catName.isEmpty()) {
                    long res = dbHelper.addCategory(catName, type);
                    if (res != -1) {
                        List<String> newCats = dbHelper.getCategories(type);
                        adapter.clear();
                        adapter.addAll(newCats);
                        adapter.notifyDataSetChanged();
                        int index = newCats.indexOf(catName);
                        if (index != -1) {
                            spinner.setSelection(index);
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "Category already exists!", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    // Custom Transaction base adapter implementation
    private class TransactionAdapter extends BaseAdapter {
        private List<DatabaseHelper.Transaction> list;
        private LayoutInflater inflater;

        public TransactionAdapter(List<DatabaseHelper.Transaction> list) {
            this.list = list;
            this.inflater = LayoutInflater.from(MainActivity.this);
        }

        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public Object getItem(int position) {
            return list.get(position);
        }

        @Override
        public long getItemId(int position) {
            return list.get(position).id;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.item_transaction, parent, false);
            }

            DatabaseHelper.Transaction transaction = list.get(position);

            TextView tvTitle = convertView.findViewById(R.id.tv_item_title);
            TextView tvDesc = convertView.findViewById(R.id.tv_item_desc);
            TextView tvDate = convertView.findViewById(R.id.tv_item_date);
            TextView tvAmount = convertView.findViewById(R.id.tv_item_amount);
            LinearLayout badgeLayout = convertView.findViewById(R.id.badge_layout);
            TextView tvBadge = convertView.findViewById(R.id.tv_badge_text);

            tvTitle.setText(transaction.category);
            if (transaction.description == null || transaction.description.trim().isEmpty()) {
                tvDesc.setVisibility(View.GONE);
            } else {
                tvDesc.setVisibility(View.VISIBLE);
                tvDesc.setText(transaction.description);
            }
            tvDate.setText(transaction.date);

            if ("income".equals(transaction.type)) {
                tvAmount.setText(String.format("+$%.2f", transaction.amount));
                tvAmount.setTextColor(Color.parseColor("#2E7D32"));
                tvBadge.setText("INC");
                tvBadge.setTextColor(Color.parseColor("#2E7D32"));
                badgeLayout.setBackgroundResource(R.drawable.badge_income);
            } else {
                tvAmount.setText(String.format("-$%.2f", transaction.amount));
                tvAmount.setTextColor(Color.parseColor("#C62828"));
                tvBadge.setText("EXP");
                tvBadge.setTextColor(Color.parseColor("#C62828"));
                badgeLayout.setBackgroundResource(R.drawable.badge_expense);
            }

            return convertView;
        }
    }
}