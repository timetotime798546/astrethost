package com.modernexpensetracker.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private TextView tvNetBalance, tvTotalIncome, tvTotalExpenses, tvNoTransactions;
    private ListView lvTransactions;
    private Button btnAddTransaction, btnViewReports;
    private ImageView btnLogout;
    private ProgressBar listProgressBar;

    private BackendApi api;
    private String token;
    private List<Transaction> transactionsList = new ArrayList<>();
    private TransactionAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences prefs = getSharedPreferences("ExpenseTrackerPrefs", MODE_PRIVATE);
        token = prefs.getString("token", "");
        if (token.isEmpty()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        api = new BackendApi(this);

        tvNetBalance = (TextView) findViewById(R.id.tvNetBalance);
        tvTotalIncome = (TextView) findViewById(R.id.tvTotalIncome);
        tvTotalExpenses = (TextView) findViewById(R.id.tvTotalExpenses);
        tvNoTransactions = (TextView) findViewById(R.id.tvNoTransactions);
        lvTransactions = (ListView) findViewById(R.id.lvTransactions);
        btnAddTransaction = (Button) findViewById(R.id.btnAddTransaction);
        btnViewReports = (Button) findViewById(R.id.btnViewReports);
        btnLogout = (ImageView) findViewById(R.id.btnLogout);
        listProgressBar = (ProgressBar) findViewById(R.id.listProgressBar);

        adapter = new TransactionAdapter();
        lvTransactions.setAdapter(adapter);

        btnAddTransaction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, AddTransactionActivity.class));
            }
        });

        btnViewReports.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, ReportsActivity.class));
            }
        });

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performLogout();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchTransactions();
    }

    private void fetchTransactions() {
        listProgressBar.setVisibility(View.VISIBLE);
        tvNoTransactions.setVisibility(View.GONE);

        api.getTransactions(token, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                listProgressBar.setVisibility(View.GONE);
                try {
                    JSONObject resObj = new JSONObject(response);
                    if (resObj.optBoolean("success", false)) {
                        JSONArray recordsArray = resObj.getJSONArray("records");
                        transactionsList.clear();
                        
                        for (int i = 0; i < recordsArray.length(); i++) {
                            JSONObject rec = recordsArray.getJSONObject(i);
                            Transaction transaction = Transaction.fromJson(rec);
                            if (transaction != null) {
                                transactionsList.add(transaction);
                            }
                        }
                        
                        adapter.notifyDataSetChanged();
                        
                        if (transactionsList.isEmpty()) {
                            tvNoTransactions.setVisibility(View.VISIBLE);
                        } else {
                            tvNoTransactions.setVisibility(View.GONE);
                        }

                        // Run calculations locally! (Strict Rule compliance)
                        runLocalFinancialCalculations();
                    }
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Data compilation warning: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String error) {
                listProgressBar.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, "Error fetching data: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Mandatory local financial calculation routines (running locally inside native Java)
     */
    private void runLocalFinancialCalculations() {
        double totalIncome = 0.0;
        double totalExpense = 0.0;

        for (int i = 0; i < transactionsList.size(); i++) {
            Transaction t = transactionsList.get(i);
            if ("income".equalsIgnoreCase(t.getType())) {
                totalIncome += t.getAmount();
            } else {
                totalExpense += t.getAmount();
            }
        }

        double netBalance = totalIncome - totalExpense;

        tvTotalIncome.setText(String.format("+$%.2f", totalIncome));
        tvTotalExpenses.setText(String.format("-$%.2f", totalExpense));
        tvNetBalance.setText(String.format("$%.2f", netBalance));

        if (netBalance >= 0) {
            tvNetBalance.setTextColor(Color.parseColor("#1B5E20"));
        } else {
            tvNetBalance.setTextColor(Color.parseColor("#B71C1C"));
        }
    }

    private void performLogout() {
        api.logout(token, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                clearSession();
            }

            @Override
            public void onError(String error) {
                // Clear session anyway for safe client exit
                clearSession();
            }
        });
    }

    private void clearSession() {
        SharedPreferences prefs = getSharedPreferences("ExpenseTrackerPrefs", MODE_PRIVATE);
        prefs.edit().remove("token").apply();
        Toast.makeText(MainActivity.this, "Logged out safely.", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(MainActivity.this, LoginActivity.class));
        finish();
    }

    private void deleteTransactionRecord(final String recordId) {
        listProgressBar.setVisibility(View.VISIBLE);
        api.deleteTransaction(token, recordId, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                listProgressBar.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, "Record successfully deleted.", Toast.LENGTH_SHORT).show();
                fetchTransactions(); // Refresh and recalculate
            }

            @Override
            public void onError(String error) {
                listProgressBar.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, "Failed to delete: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private class TransactionAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return transactionsList.size();
        }

        @Override
        public Object getItem(int position) {
            return transactionsList.get(position);
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

            final Transaction t = transactionsList.get(position);

            View vIndicator = convertView.findViewById(R.id.vIndicator);
            TextView tvItemCategory = (TextView) convertView.findViewById(R.id.tvItemCategory);
            TextView tvItemNote = (TextView) convertView.findViewById(R.id.tvItemNote);
            TextView tvItemDate = (TextView) convertView.findViewById(R.id.tvItemDate);
            TextView tvItemAmount = (TextView) convertView.findViewById(R.id.tvItemAmount);
            ImageView btnDeleteItem = (ImageView) convertView.findViewById(R.id.btnDeleteItem);

            tvItemCategory.setText(t.getCategory());
            tvItemNote.setText(t.getNote().isEmpty() ? "No description provided." : t.getNote());
            tvItemDate.setText(t.getDate());

            if ("income".equalsIgnoreCase(t.getType())) {
                vIndicator.setBackgroundColor(Color.parseColor("#388E3C"));
                tvItemAmount.setTextColor(Color.parseColor("#388E3C"));
                tvItemAmount.setText(String.format("+$%.2f", t.getAmount()));
            } else {
                vIndicator.setBackgroundColor(Color.parseColor("#D32F2F"));
                tvItemAmount.setTextColor(Color.parseColor("#D32F2F"));
                tvItemAmount.setText(String.format("-$%.2f", t.getAmount()));
            }

            btnDeleteItem.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Confirm Deletion")
                        .setMessage("Do you want to permanently delete this financial record?")
                        .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                deleteTransactionRecord(t.getId());
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