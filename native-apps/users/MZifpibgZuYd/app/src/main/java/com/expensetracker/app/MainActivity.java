package com.expensetracker.app;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final int REQ_ADD_TRANSACTION = 101;

    private TextView tvUserEmail;
    private TextView tvTotalBalance;
    private TextView tvTotalIncome;
    private TextView tvTotalExpense;
    private TextView chipAll;
    private TextView chipExpense;
    private TextView chipIncome;
    private ListView lvTransactions;
    private ProgressBar pbLoading;
    private TextView tvEmptyState;
    private Button btnAdd;
    private Button btnReports;
    private Button btnLogout;

    private BackendApi backendApi;
    private SessionManager sessionManager;
    private final List<TransactionModel> allList = new ArrayList<TransactionModel>();
    private final List<TransactionModel> displayList = new ArrayList<TransactionModel>();
    private TransactionAdapter adapter;

    private String currentFilter = "all"; // "all", "expense", "income"

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);
        if (!sessionManager.isLoggedIn()) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_main);
        backendApi = new BackendApi(this);

        initViews();
        setupClickListeners();

        adapter = new TransactionAdapter(this, displayList, new TransactionAdapter.OnDeleteClickListener() {
            @Override
            public void onDelete(final TransactionModel transaction) {
                confirmDeleteTransaction(transaction);
            }
        });
        lvTransactions.setAdapter(adapter);

        tvUserEmail.setText(sessionManager.getUserEmail());
        fetchTransactions();
    }

    private void initViews() {
        tvUserEmail = (TextView) findViewById(R.id.tvUserEmailDisplay);
        tvTotalBalance = (TextView) findViewById(R.id.tvTotalBalance);
        tvTotalIncome = (TextView) findViewById(R.id.tvTotalIncome);
        tvTotalExpense = (TextView) findViewById(R.id.tvTotalExpense);
        chipAll = (TextView) findViewById(R.id.chipFilterAll);
        chipExpense = (TextView) findViewById(R.id.chipFilterExpense);
        chipIncome = (TextView) findViewById(R.id.chipFilterIncome);
        lvTransactions = (ListView) findViewById(R.id.lvTransactions);
        pbLoading = (ProgressBar) findViewById(R.id.pbMainLoading);
        tvEmptyState = (TextView) findViewById(R.id.tvEmptyState);
        btnAdd = (Button) findViewById(R.id.btnAddNewTransaction);
        btnReports = (Button) findViewById(R.id.btnViewReports);
        btnLogout = (Button) findViewById(R.id.btnLogout);
    }

    private void setupClickListeners() {
        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, AddTransactionActivity.class);
                startActivityForResult(intent, REQ_ADD_TRANSACTION);
            }
        });

        btnReports.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ReportsActivity.class);
                startActivity(intent);
            }
        });

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sessionManager.clearSession();
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });

        chipAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setFilter("all");
            }
        });

        chipExpense.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setFilter("expense");
            }
        });

        chipIncome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setFilter("income");
            }
        });
    }

    private void setFilter(String filter) {
        currentFilter = filter;
        updateFilterChipStyles();
        applyFilterAndRecalculate();
    }

    private void updateFilterChipStyles() {
        chipAll.setBackgroundResource("all".equals(currentFilter) ? R.drawable.filter_chip_active : R.drawable.filter_chip_inactive);
        chipAll.setTextColor("all".equals(currentFilter) ? Color.WHITE : Color.parseColor("#475569"));

        chipExpense.setBackgroundResource("expense".equals(currentFilter) ? R.drawable.filter_chip_active : R.drawable.filter_chip_inactive);
        chipExpense.setTextColor("expense".equals(currentFilter) ? Color.WHITE : Color.parseColor("#475569"));

        chipIncome.setBackgroundResource("income".equals(currentFilter) ? R.drawable.filter_chip_active : R.drawable.filter_chip_inactive);
        chipIncome.setTextColor("income".equals(currentFilter) ? Color.WHITE : Color.parseColor("#475569"));
    }

    public void fetchTransactions() {
        setLoading(true);
        backendApi.getTransactions(sessionManager.getToken(), new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                setLoading(false);
                allList.clear();
                JSONArray records = response.optJSONArray("records");
                if (records != null) {
                    for (int i = 0; i < records.length(); i++) {
                        JSONObject rec = records.optJSONObject(i);
                        if (rec != null) {
                            String id = rec.optString("id");
                            JSONObject data = rec.optJSONObject("data");
                            if (data != null) {
                                String title = data.optString("title", "Untitled");
                                double amount = data.optDouble("amount", 0.0);
                                String type = data.optString("type", "expense");
                                String category = data.optString("category", "General");
                                String date = data.optString("date", "");
                                String note = data.optString("note", "");

                                allList.add(new TransactionModel(id, title, amount, type, category, date, note));
                            }
                        }
                    }
                }
                applyFilterAndRecalculate();
            }

            @Override
            public void onError(String errorMessage) {
                setLoading(false);
                Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void applyFilterAndRecalculate() {
        displayList.clear();
        for (int i = 0; i < allList.size(); i++) {
            TransactionModel item = allList.get(i);
            if ("all".equals(currentFilter)) {
                displayList.add(item);
            } else if ("expense".equals(currentFilter) && !item.isIncome()) {
                displayList.add(item);
            } else if ("income".equals(currentFilter) && item.isIncome()) {
                displayList.add(item);
            }
        }

        adapter.notifyDataSetChanged();
        tvEmptyState.setVisibility(displayList.isEmpty() ? View.VISIBLE : View.GONE);

        // Perform ALL deterministic financial calculations strictly inside Android Native Java
        double totalIncome = ExpenseCalculator.calculateTotalIncome(allList);
        double totalExpense = ExpenseCalculator.calculateTotalExpense(allList);
        double netBalance = ExpenseCalculator.calculateBalance(totalIncome, totalExpense);

        tvTotalIncome.setText(String.format(Locale.US, "$%.2f", totalIncome));
        tvTotalExpense.setText(String.format(Locale.US, "$%.2f", totalExpense));

        if (netBalance >= 0) {
            tvTotalBalance.setText(String.format(Locale.US, "$%.2f", netBalance));
            tvTotalBalance.setTextColor(Color.WHITE);
        } else {
            tvTotalBalance.setText(String.format(Locale.US, "-$%.2f", Math.abs(netBalance)));
            tvTotalBalance.setTextColor(Color.parseColor("#FCA5A5"));
        }
    }

    private void confirmDeleteTransaction(final TransactionModel item) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Transaction")
                .setMessage("Are you sure you want to remove '" + item.getTitle() + "'?")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteTransactionRecord(item);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteTransactionRecord(final TransactionModel item) {
        setLoading(true);
        backendApi.deleteTransaction(sessionManager.getToken(), item.getId(), new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                setLoading(false);
                Toast.makeText(MainActivity.this, "Transaction deleted", Toast.LENGTH_SHORT).show();
                fetchTransactions();
            }

            @Override
            public void onError(String errorMessage) {
                setLoading(false);
                Toast.makeText(MainActivity.this, "Delete error: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        pbLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_ADD_TRANSACTION && resultCode == RESULT_OK) {
            fetchTransactions();
        }
    }
}