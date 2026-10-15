package com.modernexpensetracker.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private TextView tvCurrentMonth, tvNetBalance, tvIncome, tvExpenses, tvEmptyState;
    private Button btnPrevMonth, btnNextMonth, btnReports, btnLogout, btnAdd;
    private Button filterAll, filterIncome, filterExpense;
    private ListView lvTransactions;
    private ProgressBar pbLoading;

    private BackendApi backendApi;
    private final List<TransactionModel> allTransactions = new ArrayList<TransactionModel>();
    private final List<TransactionModel> displayedTransactions = new ArrayList<TransactionModel>();
    private TransactionAdapter adapter;

    private Calendar currentCalendar = Calendar.getInstance();
    private String currentFilter = "ALL"; // ALL, INCOME, EXPENSE

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        backendApi = new BackendApi(this);

        tvCurrentMonth = (TextView) findViewById(R.id.tv_current_month);
        tvNetBalance = (TextView) findViewById(R.id.tv_main_net_balance);
        tvIncome = (TextView) findViewById(R.id.tv_main_income);
        tvExpenses = (TextView) findViewById(R.id.tv_main_expenses);
        tvEmptyState = (TextView) findViewById(R.id.tv_empty_state);

        btnPrevMonth = (Button) findViewById(R.id.btn_prev_month);
        btnNextMonth = (Button) findViewById(R.id.btn_next_month);
        btnReports = (Button) findViewById(R.id.btn_reports);
        btnLogout = (Button) findViewById(R.id.btn_logout);
        btnAdd = (Button) findViewById(R.id.btn_add_transaction);

        filterAll = (Button) findViewById(R.id.filter_chip_all);
        filterIncome = (Button) findViewById(R.id.filter_chip_income);
        filterExpense = (Button) findViewById(R.id.filter_chip_expense);

        lvTransactions = (ListView) findViewById(R.id.lv_transactions);
        pbLoading = (ProgressBar) findViewById(R.id.pb_main_loading);

        adapter = new TransactionAdapter(this, displayedTransactions, new TransactionAdapter.OnDeleteClickListener() {
            @Override
            public void onDeleteClick(final TransactionModel transaction) {
                confirmDelete(transaction);
            }
        });
        lvTransactions.setAdapter(adapter);

        updateMonthLabel();
        setupListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTransactions();
    }

    private void setupListeners() {
        btnPrevMonth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentCalendar.add(Calendar.MONTH, -1);
                updateMonthLabel();
                filterAndCalculateDisplay();
            }
        });

        btnNextMonth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentCalendar.add(Calendar.MONTH, 1);
                updateMonthLabel();
                filterAndCalculateDisplay();
            }
        });

        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, AddTransactionActivity.class));
            }
        });

        btnReports.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ReportsActivity.class);
                intent.putExtra("year", currentCalendar.get(Calendar.YEAR));
                intent.putExtra("month", currentCalendar.get(Calendar.MONTH));
                startActivity(intent);
            }
        });

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performLogout();
            }
        });

        filterAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setFilterMode("ALL");
            }
        });

        filterIncome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setFilterMode("INCOME");
            }
        });

        filterExpense.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setFilterMode("EXPENSE");
            }
        });
    }

    private void setFilterMode(String mode) {
        currentFilter = mode;
        filterAll.setBackgroundResource("ALL".equals(mode) ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
        filterAll.setTextColor("ALL".equals(mode) ? Color.WHITE : Color.parseColor("#0F172A"));

        filterIncome.setBackgroundResource("INCOME".equals(mode) ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
        filterIncome.setTextColor("INCOME".equals(mode) ? Color.WHITE : Color.parseColor("#0F172A"));

        filterExpense.setBackgroundResource("EXPENSE".equals(mode) ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
        filterExpense.setTextColor("EXPENSE".equals(mode) ? Color.WHITE : Color.parseColor("#0F172A"));

        filterAndCalculateDisplay();
    }

    private void updateMonthLabel() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.US);
        tvCurrentMonth.setText(sdf.format(currentCalendar.getTime()));
    }

    private void loadTransactions() {
        pbLoading.setVisibility(View.VISIBLE);
        tvEmptyState.setVisibility(View.GONE);

        backendApi.getData("transactions", new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                pbLoading.setVisibility(View.GONE);
                allTransactions.clear();
                try {
                    if (response.optBoolean("success", false) && response.has("records")) {
                        JSONArray records = response.getJSONArray("records");
                        for (int i = 0; i < records.length(); i++) {
                            JSONObject rec = records.getJSONObject(i);
                            String id = rec.optString("id", "");
                            JSONObject data = rec.optJSONObject("data");
                            if (data != null) {
                                allTransactions.add(TransactionModel.fromJson(id, data));
                            }
                        }
                    }
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Parse error", Toast.LENGTH_SHORT).show();
                }
                filterAndCalculateDisplay();
            }

            @Override
            public void onError(String error) {
                pbLoading.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, "Sync error: " + error, Toast.LENGTH_SHORT).show();
                filterAndCalculateDisplay();
            }
        });
    }

    /**
     * Local deterministic calculation and filtering as per architecture specification.
     */
    private void filterAndCalculateDisplay() {
        displayedTransactions.clear();

        SimpleDateFormat ymFormat = new SimpleDateFormat("yyyy-MM", Locale.US);
        String currentYM = ymFormat.format(currentCalendar.getTime());

        List<TransactionModel> monthlyList = new ArrayList<TransactionModel>();
        for (int i = 0; i < allTransactions.size(); i++) {
            TransactionModel t = allTransactions.get(i);
            if (t.getDate() != null && t.getDate().startsWith(currentYM)) {
                monthlyList.add(t);
            }
        }

        // Native Java offline calculations
        double totalIncome = CalculationHelper.calculateTotalIncome(monthlyList);
        double totalExpense = CalculationHelper.calculateTotalExpense(monthlyList);
        double netBalance = CalculationHelper.calculateNetBalance(totalIncome, totalExpense);

        // Update Summary Cards
        tvIncome.setText(String.format(Locale.US, "$%.2f", totalIncome));
        tvExpenses.setText(String.format(Locale.US, "$%.2f", totalExpense));

        if (netBalance >= 0) {
            tvNetBalance.setText(String.format(Locale.US, "$%.2f", netBalance));
            tvNetBalance.setTextColor(Color.WHITE);
        } else {
            tvNetBalance.setText(String.format(Locale.US, "-$%.2f", Math.abs(netBalance)));
            tvNetBalance.setTextColor(Color.parseColor("#FCA5A5"));
        }

        // Apply Tab Filter for list view
        for (int i = 0; i < monthlyList.size(); i++) {
            TransactionModel item = monthlyList.get(i);
            if ("ALL".equals(currentFilter)) {
                displayedTransactions.add(item);
            } else if ("INCOME".equals(currentFilter) && !item.isExpense()) {
                displayedTransactions.add(item);
            } else if ("EXPENSE".equals(currentFilter) && item.isExpense()) {
                displayedTransactions.add(item);
            }
        }

        adapter.notifyDataSetChanged();
        tvEmptyState.setVisibility(displayedTransactions.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void confirmDelete(final TransactionModel transaction) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Transaction")
                .setMessage("Are you sure you want to delete '" + transaction.getTitle() + "'?")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        pbLoading.setVisibility(View.VISIBLE);
                        backendApi.deleteData(transaction.getId(), new BackendApi.ApiCallback() {
                            @Override
                            public void onSuccess(JSONObject response) {
                                pbLoading.setVisibility(View.GONE);
                                Toast.makeText(MainActivity.this, "Deleted successfully", Toast.LENGTH_SHORT).show();
                                loadTransactions();
                            }

                            @Override
                            public void onError(String error) {
                                pbLoading.setVisibility(View.GONE);
                                Toast.makeText(MainActivity.this, "Failed to delete: " + error, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performLogout() {
        backendApi.logout(new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                Toast.makeText(MainActivity.this, "Logged out", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(String error) {
                // Session cleared anyway
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });
    }
}