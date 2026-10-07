package com.financely.app;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends Activity {

    public static class TransactionItem {
        String id;
        String type;
        double amount;
        String category;
        String description;
        String date;

        public TransactionItem(String id, String type, double amount, String category, String description, String date) {
            this.id = id;
            this.type = type;
            this.amount = amount;
            this.category = category;
            this.description = description;
            this.date = date;
        }
    }

    private TextView tvTotalBalance, tvIncomeBalance, tvExpenseBalance, tvBreakdownSummary, tvEmptyState;
    private ListView listTransactions;
    private Button btnLogout, btnAddTransaction;
    private Spinner spinnerCategoryFilter, spinnerMonthFilter;

    private BackendApi api;
    private String token;
    private List<TransactionItem> allTransactions = new ArrayList<>();
    private List<TransactionItem> filteredTransactions = new ArrayList<>();
    private TransactionAdapter adapter;
    private ProgressDialog progressDialog;

    private List<String> categoriesForFilter = new ArrayList<>();
    private List<String> monthsForFilter = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences prefs = getSharedPreferences("financely_prefs", Context.MODE_PRIVATE);
        token = prefs.getString("token", null);

        if (token == null || token.isEmpty()) {
            goToLogin();
            return;
        }

        api = new BackendApi(this);

        tvTotalBalance = (TextView) findViewById(R.id.tv_total_balance);
        tvIncomeBalance = (TextView) findViewById(R.id.tv_income_balance);
        tvExpenseBalance = (TextView) findViewById(R.id.tv_expense_balance);
        tvBreakdownSummary = (TextView) findViewById(R.id.tv_breakdown_summary);
        tvEmptyState = (TextView) findViewById(R.id.tv_empty_state);
        listTransactions = (ListView) findViewById(R.id.list_transactions);
        btnLogout = (Button) findViewById(R.id.btn_logout);
        btnAddTransaction = (Button) findViewById(R.id.btn_add_transaction);
        spinnerCategoryFilter = (Spinner) findViewById(R.id.spinner_category_filter);
        spinnerMonthFilter = (Spinner) findViewById(R.id.spinner_month_filter);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Synchronizing data...");
        progressDialog.setCancelable(false);

        adapter = new TransactionAdapter();
        listTransactions.setAdapter(adapter);

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performLogout();
            }
        });

        btnAddTransaction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, AddTransactionActivity.class);
                startActivity(intent);
            }
        });

        spinnerCategoryFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applyLocalFiltersAndCalculate();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerMonthFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applyLocalFiltersAndCalculate();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchTransactions();
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void performLogout() {
        progressDialog.setMessage("Logging out...");
        progressDialog.show();
        api.logout(token, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                progressDialog.dismiss();
                clearLocalSession();
            }

            @Override
            public void onError(String errorMessage) {
                progressDialog.dismiss();
                // Even on error, safe option is clear session locally
                clearLocalSession();
            }
        });
    }

    private void clearLocalSession() {
        SharedPreferences.Editor editor = getSharedPreferences("financely_prefs", Context.MODE_PRIVATE).edit();
        editor.remove("token");
        editor.remove("email");
        editor.apply();
        Toast.makeText(MainActivity.this, "Successfully logged out.", Toast.LENGTH_SHORT).show();
        goToLogin();
    }

    private void fetchTransactions() {
        progressDialog.setMessage("Loading ledger...");
        progressDialog.show();
        api.getTransactions(token, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                progressDialog.dismiss();
                try {
                    boolean success = response.optBoolean("success", false);
                    if (success) {
                        allTransactions.clear();
                        JSONArray arr = response.optJSONArray("records");
                        if (arr != null) {
                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject record = arr.getJSONObject(i);
                                String id = record.optString("id", "");
                                JSONObject data = record.optJSONObject("data");
                                if (data != null) {
                                    String type = data.optString("type", "Expense");
                                    double amount = data.optDouble("amount", 0.0);
                                    String category = data.optString("category", "General");
                                    String description = data.optString("description", "");
                                    String date = data.optString("date", "");

                                    allTransactions.add(new TransactionItem(id, type, amount, category, description, date));
                                }
                            }
                        }
                        updateFilterSpinners();
                        applyLocalFiltersAndCalculate();
                    } else {
                        Toast.makeText(MainActivity.this, "Failed to retrieve ledger data.", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Parsing Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onError(String errorMessage) {
                progressDialog.dismiss();
                Toast.makeText(MainActivity.this, "Data Fetch Error: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void updateFilterSpinners() {
        // Collect dynamic categories and months for list filtering
        Set<String> categories = new HashSet<>();
        categories.add("All Categories");

        Set<String> months = new HashSet<>();
        months.add("All Months");

        for (TransactionItem item : allTransactions) {
            if (item.category != null && !item.category.trim().isEmpty()) {
                categories.add(item.category);
            }
            if (item.date != null && item.date.length() >= 7) {
                // Extracts YYYY-MM
                months.add(item.date.substring(0, 7));
            }
        }

        // Setup Categories Spinner
        categoriesForFilter.clear();
        categoriesForFilter.addAll(categories);
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categoriesForFilter);
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategoryFilter.setAdapter(catAdapter);

        // Setup Months Spinner
        monthsForFilter.clear();
        monthsForFilter.addAll(months);
        ArrayAdapter<String> monthAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, monthsForFilter);
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMonthFilter.setAdapter(monthAdapter);
    }

    /**
     * MANDATORY OFFLINE CALCULATION:
     * Evaluates total income, total expenses, balance and aggregates month reports on local data thread.
     */
    private void applyLocalFiltersAndCalculate() {
        String selectedCategory = spinnerCategoryFilter.getSelectedItem() != null ? spinnerCategoryFilter.getSelectedItem().toString() : "All Categories";
        String selectedMonth = spinnerMonthFilter.getSelectedItem() != null ? spinnerMonthFilter.getSelectedItem().toString() : "All Months";

        filteredTransactions.clear();
        double runningIncomeTotal = 0.0;
        double runningExpenseTotal = 0.0;

        for (TransactionItem item : allTransactions) {
            boolean matchesCategory = selectedCategory.equals("All Categories") || item.category.equalsIgnoreCase(selectedCategory);
            
            boolean matchesMonth = selectedMonth.equals("All Months") || (item.date != null && item.date.startsWith(selectedMonth));

            if (matchesCategory && matchesMonth) {
                filteredTransactions.add(item);
                if (item.type.equalsIgnoreCase("Income")) {
                    runningIncomeTotal += item.amount;
                } else {
                    runningExpenseTotal += item.amount;
                }
            }
        }

        double netBalance = runningIncomeTotal - runningExpenseTotal;

        // UI Updates with precise formatted strings
        tvTotalBalance.setText(String.format("$%.2f", netBalance));
        tvIncomeBalance.setText(String.format("+$%.2f", runningIncomeTotal));
        tvExpenseBalance.setText(String.format("-$%.2f", runningExpenseTotal));

        if (filteredTransactions.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
        }

        // Offline summary explanation generator
        String summaryText = "Displaying " + filteredTransactions.size() + " items (" + selectedCategory + " / " + selectedMonth + "):\n";
        if (netBalance >= 0) {
            summaryText += "Saving Rate is positive. You stored " + String.format("$%.2f", netBalance) + " in this dynamic tier.";
        } else {
            summaryText += "Over-budget danger! Spending exceeded earnings by " + String.format("$%.2f", Math.abs(netBalance)) + ".";
        }
        tvBreakdownSummary.setText(summaryText);

        adapter.notifyDataSetChanged();
    }

    private void deleteTransactionItem(final TransactionItem item) {
        progressDialog.setMessage("Deleting transaction record...");
        progressDialog.show();
        api.deleteTransaction(token, item.id, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                progressDialog.dismiss();
                if (response.optBoolean("success", false)) {
                    Toast.makeText(MainActivity.this, "Item successfully removed.", Toast.LENGTH_SHORT).show();
                    fetchTransactions();
                } else {
                    Toast.makeText(MainActivity.this, "Unable to delete server record.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String errorMessage) {
                progressDialog.dismiss();
                Toast.makeText(MainActivity.this, "Delete failed: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private class TransactionAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return filteredTransactions.size();
        }

        @Override
        public Object getItem(int position) {
            return filteredTransactions.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(MainActivity.this).inflate(R.layout.transaction_list_item, parent, false);
            }

            final TransactionItem item = filteredTransactions.get(position);

            TextView tvCategory = (TextView) convertView.findViewById(R.id.tv_item_category);
            TextView tvDesc = (TextView) convertView.findViewById(R.id.tv_item_desc);
            TextView tvDate = (TextView) convertView.findViewById(R.id.tv_item_date);
            TextView tvAmount = (TextView) convertView.findViewById(R.id.tv_item_amount);
            Button btnDelete = (Button) convertView.findViewById(R.id.btn_delete_item);

            tvCategory.setText(item.category);
            if (item.description != null && !item.description.isEmpty()) {
                tvDesc.setText(item.description);
                tvDesc.setVisibility(View.VISIBLE);
            } else {
                tvDesc.setVisibility(View.GONE);
            }

            tvDate.setText(item.date);

            if (item.type.equalsIgnoreCase("Income")) {
                tvAmount.setText(String.format("+$%.2f", item.amount));
                tvAmount.setTextColor(getResources().getColor(R.color.income_green));
            } else {
                tvAmount.setText(String.format("-$%.2f", item.amount));
                tvAmount.setTextColor(getResources().getColor(R.color.expense_red));
            }

            btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    deleteTransactionItem(item);
                }
            });

            return convertView;
        }
    }
}