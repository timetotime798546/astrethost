package com.expensemanager.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity {
    private TextView tvTotalAmount;
    private TextView tvTotalCount;
    private LinearLayout categoriesContainer;
    private LinearLayout expensesContainer;
    private ProgressBar pbLoading;
    private TextView tvEmptyState;
    private Button btnLogout;
    private Button btnAddExpense;

    private BackendApi api;
    private List<Expense> expenseList;
    private static final int REQUEST_CODE_REFRESH = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        api = new BackendApi(this);
        expenseList = new ArrayList<Expense>();

        if (!api.isLoggedIn()) {
            goToLogin();
            return;
        }

        tvTotalAmount = (TextView) findViewById(R.id.tv_total_amount);
        tvTotalCount = (TextView) findViewById(R.id.tv_total_count);
        categoriesContainer = (LinearLayout) findViewById(R.id.categories_list_container);
        expensesContainer = (LinearLayout) findViewById(R.id.expenses_list_container);
        pbLoading = (ProgressBar) findViewById(R.id.pb_main_loading);
        tvEmptyState = (TextView) findViewById(R.id.tv_empty_state);
        btnLogout = (Button) findViewById(R.id.btn_logout);
        btnAddExpense = (Button) findViewById(R.id.btn_add_expense);

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performLogout();
            }
        });

        btnAddExpense.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, AddEditExpenseActivity.class);
                intent.putExtra("is_edit", false);
                startActivityForResult(intent, REQUEST_CODE_REFRESH);
            }
        });

        fetchExpenses();
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void performLogout() {
        api.clearToken();
        goToLogin();
        
        // Attempt backend session cleanup asynchronously
        api.logout(new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {}
            @Override
            public void onError(String error) {}
        });
    }

    private void fetchExpenses() {
        setLoading(true);
        api.getExpenses(new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                setLoading(false);
                expenseList.clear();
                JSONArray records = response.optJSONArray("records");
                if (records != null) {
                    for (int i = 0; i < records.length(); i++) {
                        JSONObject record = records.optJSONObject(i);
                        if (record != null) {
                            Expense exp = Expense.fromJson(record);
                            expenseList.add(exp);
                        }
                    }
                }
                updateUI();
            }

            @Override
            public void onError(String error) {
                setLoading(false);
                Toast.makeText(MainActivity.this, "Sync failure: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void updateUI() {
        double totalSum = 0.0;
        int count = expenseList.size();
        Map<String, Double> categorySums = new HashMap<String, Double>();

        expensesContainer.removeAllViews();

        if (count == 0) {
            tvEmptyState.setVisibility(View.VISIBLE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            LayoutInflater inflater = LayoutInflater.from(this);

            for (int i = 0; i < expenseList.size(); i++) {
                final Expense exp = expenseList.get(i);
                totalSum += exp.getAmount();

                String category = exp.getCategory();
                if (category == null || category.isEmpty()) category = "Others";
                Double currentVal = categorySums.get(category);
                if (currentVal == null) currentVal = 0.0;
                categorySums.put(category, currentVal + exp.getAmount());

                View view = inflater.inflate(R.layout.item_expense, expensesContainer, false);
                TextView tvTitle = (TextView) view.findViewById(R.id.tv_item_title);
                TextView tvCategory = (TextView) view.findViewById(R.id.tv_item_category);
                TextView tvDate = (TextView) view.findViewById(R.id.tv_item_date);
                TextView tvAmount = (TextView) view.findViewById(R.id.tv_item_amount);
                TextView tvNote = (TextView) view.findViewById(R.id.tv_item_note);

                tvTitle.setText(exp.getTitle());
                tvCategory.setText(category);
                tvDate.setText(exp.getDate());
                tvAmount.setText(String.format(java.util.Locale.US, "$%.2f", exp.getAmount()));
                
                if (exp.getNote() != null && !exp.getNote().isEmpty()) {
                    tvNote.setText(exp.getNote());
                    tvNote.setVisibility(View.VISIBLE);
                } else {
                    tvNote.setVisibility(View.GONE);
                }

                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(MainActivity.this, AddEditExpenseActivity.class);
                        intent.putExtra("is_edit", true);
                        intent.putExtra("id", exp.getId());
                        intent.putExtra("title", exp.getTitle());
                        intent.putExtra("amount", exp.getAmount());
                        intent.putExtra("category", exp.getCategory());
                        intent.putExtra("date", exp.getDate());
                        intent.putExtra("note", exp.getNote());
                        startActivityForResult(intent, REQUEST_CODE_REFRESH);
                    }
                });

                expensesContainer.addView(view);
            }
        }

        tvTotalAmount.setText(String.format(java.util.Locale.US, "$%.2f", totalSum));
        tvTotalCount.setText(count + (count == 1 ? " item" : " items"));

        populateCategoryBreakdown(categorySums);
    }

    private void populateCategoryBreakdown(Map<String, Double> categoryTotals) {
        categoriesContainer.removeAllViews();

        if (categoryTotals == null || categoryTotals.isEmpty()) {
            TextView tv = new TextView(this);
            tv.setText("No categorization data.");
            tv.setTextColor(0xFF757575);
            tv.setTextSize(12);
            categoriesContainer.addView(tv);
            return;
        }

        for (Map.Entry<String, Double> entry : categoryTotals.entrySet()) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, 4, 0, 4);

            TextView nameTv = new TextView(this);
            nameTv.setText(entry.getKey());
            nameTv.setTextColor(0xFF212121);
            nameTv.setTextSize(14);
            LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
            nameTv.setLayoutParams(lp1);

            TextView valTv = new TextView(this);
            valTv.setText(String.format(java.util.Locale.US, "$%.2f", entry.getValue()));
            valTv.setTextColor(0xFF3F51B5);
            valTv.setTextSize(14);
            valTv.setTypeface(null, android.graphics.Typeface.BOLD);

            row.addView(nameTv);
            row.addView(valTv);
            categoriesContainer.addView(row);
        }
    }

    private void setLoading(boolean loading) {
        pbLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnAddExpense.setEnabled(!loading);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_REFRESH && resultCode == RESULT_OK) {
            fetchExpenses();
        }
    }
}