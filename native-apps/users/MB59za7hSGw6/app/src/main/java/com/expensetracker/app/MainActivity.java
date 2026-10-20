package com.expensetracker.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
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

    private TextView tvUserEmail, tvTotalBalance, tvTotalIncome, tvTotalExpense, tvEmptyState;
    private Button btnReports, btnLogout, btnAddTransaction;
    private ListView lvTransactions;
    private ProgressBar loadingProgress;

    private BackendApi api;
    private final List<TransactionModel> transactionList = new ArrayList<TransactionModel>();
    private TransactionAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        api = new BackendApi(this);

        if (!api.isLoggedIn()) {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
            return;
        }

        tvUserEmail = (TextView) findViewById(R.id.tvUserEmail);
        tvTotalBalance = (TextView) findViewById(R.id.tvTotalBalance);
        tvTotalIncome = (TextView) findViewById(R.id.tvTotalIncome);
        tvTotalExpense = (TextView) findViewById(R.id.tvTotalExpense);
        tvEmptyState = (TextView) findViewById(R.id.tvEmptyState);
        btnReports = (Button) findViewById(R.id.btnReports);
        btnLogout = (Button) findViewById(R.id.btnLogout);
        btnAddTransaction = (Button) findViewById(R.id.btnAddTransaction);
        lvTransactions = (ListView) findViewById(R.id.lvTransactions);
        loadingProgress = (ProgressBar) findViewById(R.id.loadingProgress);

        tvUserEmail.setText(api.getUserEmail());

        adapter = new TransactionAdapter();
        lvTransactions.setAdapter(adapter);

        btnAddTransaction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, AddTransactionActivity.class));
            }
        });

        btnReports.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, ReportsActivity.class));
            }
        });

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Logout")
                    .setMessage("Are you sure you want to sign out?")
                    .setPositiveButton("Logout", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            api.logout(new BackendApi.ApiCallback() {
                                @Override
                                public void onSuccess(JSONObject response) {
                                    startActivity(new Intent(MainActivity.this, LoginActivity.class));
                                    finish();
                                }

                                @Override
                                public void onError(String errorMessage) {
                                    startActivity(new Intent(MainActivity.this, LoginActivity.class));
                                    finish();
                                }
                            });
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (api.isLoggedIn()) {
            loadTransactions();
        }
    }

    private void loadTransactions() {
        loadingProgress.setVisibility(View.VISIBLE);
        api.getTransactions(new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                loadingProgress.setVisibility(View.GONE);
                transactionList.clear();

                // Android READ contract: root "success" -> "records" array -> "id" + nested "data"
                JSONArray records = response.optJSONArray("records");
                if (records != null) {
                    for (int i = 0; i < records.length(); i++) {
                        JSONObject rec = records.optJSONObject(i);
                        if (rec != null) {
                            String id = rec.optString("id", "");
                            JSONObject data = rec.optJSONObject("data");
                            TransactionModel model = TransactionModel.fromJson(id, data);
                            if (model != null) {
                                transactionList.add(model);
                            }
                        }
                    }
                }

                // Mandatory Offline Business Logic: Calculate totals in native Java
                updateSummaryUi();
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onError(String errorMessage) {
                loadingProgress.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, "Failed to load: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateSummaryUi() {
        double income = ExpenseCalculator.calculateTotalIncome(transactionList);
        double expense = ExpenseCalculator.calculateTotalExpense(transactionList);
        double balance = ExpenseCalculator.calculateBalance(transactionList);

        tvTotalIncome.setText(String.format(Locale.getDefault(), "$%.2f", income));
        tvTotalExpense.setText(String.format(Locale.getDefault(), "$%.2f", expense));
        tvTotalBalance.setText(String.format(Locale.getDefault(), "$%.2f", balance));

        if (balance >= 0) {
            tvTotalBalance.setTextColor(getResources().getColor(R.color.accent_income));
        } else {
            tvTotalBalance.setTextColor(getResources().getColor(R.color.accent_expense));
        }

        tvEmptyState.setVisibility(transactionList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private class TransactionAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return transactionList.size();
        }

        @Override
        public Object getItem(int position) {
            return transactionList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(MainActivity.this).inflate(R.layout.item_transaction, parent, false);
            }

            final TransactionModel item = transactionList.get(position);

            TextView tvCategoryIcon = (TextView) convertView.findViewById(R.id.tvCategoryIcon);
            TextView tvItemTitle = (TextView) convertView.findViewById(R.id.tvItemTitle);
            TextView tvItemMeta = (TextView) convertView.findViewById(R.id.tvItemMeta);
            TextView tvItemAmount = (TextView) convertView.findViewById(R.id.tvItemAmount);
            TextView tvItemDelete = (TextView) convertView.findViewById(R.id.tvItemDelete);

            tvItemTitle.setText(item.getTitle());
            tvItemMeta.setText(item.getCategory() + " • " + (item.getDate().isEmpty() ? "Recent" : item.getDate()));

            // Determine emoji icon based on category
            String cat = item.getCategory().toLowerCase(Locale.getDefault());
            if (cat.contains("food") || cat.contains("grocer") || cat.contains("dining")) {
                tvCategoryIcon.setText("🍔");
            } else if (cat.contains("transport") || cat.contains("fuel") || cat.contains("car")) {
                tvCategoryIcon.setText("🚗");
            } else if (cat.contains("bill") || cat.contains("rent") || cat.contains("util")) {
                tvCategoryIcon.setText("💡");
            } else if (cat.contains("salary") || cat.contains("wage") || cat.contains("invest")) {
                tvCategoryIcon.setText("💵");
            } else if (cat.contains("health") || cat.contains("doctor")) {
                tvCategoryIcon.setText("💊");
            } else {
                tvCategoryIcon.setText("🏷️");
            }

            if (item.isExpense()) {
                tvItemAmount.setText(String.format(Locale.getDefault(), "-$%.2f", item.getAmount()));
                tvItemAmount.setTextColor(getResources().getColor(R.color.accent_expense));
            } else {
                tvItemAmount.setText(String.format(Locale.getDefault(), "+$%.2f", item.getAmount()));
                tvItemAmount.setTextColor(getResources().getColor(R.color.accent_income));
            }

            tvItemDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Delete Entry")
                        .setMessage("Delete '" + item.getTitle() + "'?")
                        .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                api.deleteTransaction(item.getId(), new BackendApi.ApiCallback() {
                                    @Override
                                    public void onSuccess(JSONObject response) {
                                        Toast.makeText(MainActivity.this, "Transaction deleted", Toast.LENGTH_SHORT).show();
                                        loadTransactions();
                                    }

                                    @Override
                                    public void onError(String errorMessage) {
                                        Toast.makeText(MainActivity.this, "Delete failed: " + errorMessage, Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                }
            });

            return convertView;
        }
    }
}