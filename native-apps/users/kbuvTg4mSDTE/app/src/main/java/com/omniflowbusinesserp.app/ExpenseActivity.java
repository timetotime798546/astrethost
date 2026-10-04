package com.omniflowbusinesserp.app;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;

public class ExpenseActivity extends Activity {

    private Spinner spCategory;
    private EditText etAmount, etDescription;
    private Button btnSubmitExpense;
    private LinearLayout layoutExpenseList;

    private BackendApi api;
    private String token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expense);

        SharedPreferences prefs = getSharedPreferences("OmniPrefs", MODE_PRIVATE);
        token = prefs.getString("token", null);

        api = new BackendApi(this);

        spCategory = (Spinner) findViewById(R.id.spCategory);
        etAmount = (EditText) findViewById(R.id.etAmount);
        etDescription = (EditText) findViewById(R.id.etDescription);
        btnSubmitExpense = (Button) findViewById(R.id.btnSubmitExpense);
        layoutExpenseList = (LinearLayout) findViewById(R.id.layoutExpenseList);

        String[] categories = {"Travel & Transport", "Utilities & Power", "Logistics", "Office Supplies", "Operational Cost"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categories);
        spCategory.setAdapter(adapter);

        btnSubmitExpense.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitExpense();
            }
        });

        loadExpenses();
    }

    private void submitExpense() {
        String category = spCategory.getSelectedItem().toString();
        String amountStr = etAmount.getText().toString().trim();
        String description = etDescription.getText().toString().trim();

        if (amountStr.isEmpty() || description.isEmpty()) {
            Toast.makeText(this, "Empty voucher details", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double amount = Double.parseDouble(amountStr);

            JSONObject expense = new JSONObject();
            expense.put("category", category);
            expense.put("amount", amount);
            expense.put("description", description);
            expense.put("status", "Approved");

            api.createData(token, "expenses", expense, new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(String response) {
                    Toast.makeText(ExpenseActivity.this, "Expense Voucher Created", Toast.LENGTH_SHORT).show();
                    etAmount.setText("");
                    etDescription.setText("");
                    loadExpenses();
                }

                @Override
                public void onError(String errorMessage) {
                    Toast.makeText(ExpenseActivity.this, "Voucher submission error: " + errorMessage, Toast.LENGTH_LONG).show();
                }
            });

        } catch (Exception e) {
            Toast.makeText(this, "Voucher amount must be numerical", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadExpenses() {
        layoutExpenseList.removeAllViews();
        api.readData(token, "expenses", new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject json = new JSONObject(response);
                    if (json.getBoolean("success")) {
                        JSONArray records = json.getJSONArray("records");
                        for (int i = 0; i < records.length(); i++) {
                            JSONObject record = records.getJSONObject(i);
                            final String recordId = record.getString("id");
                            JSONObject data = record.getJSONObject("data");

                            String category = data.optString("category", "Unspecified");
                            double amount = data.optDouble("amount", 0.0);
                            String desc = data.optString("description", "No description");
                            String status = data.optString("status", "Pending");

                            addExpenseRow(recordId, category, amount, desc, status);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(ExpenseActivity.this, "Failed parsing operational expenses: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addExpenseRow(final String id, final String category, final double amount, final String description, final String status) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(12, 12, 12, 12);
        row.setBackgroundColor(0xFFFFFFFF);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 8);
        row.setLayoutParams(params);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(category + " - $" + String.format("%.2f", amount));
        tvTitle.setTextSize(14.0f);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTitle.setTextColor(0xFF333333);

        TextView tvDesc = new TextView(this);
        tvDesc.setText("Purpose: " + description + " [" + status + "]");
        tvDesc.setTextSize(12.0f);
        tvDesc.setTextColor(0xFF555555);

        LinearLayout btnLayout = new LinearLayout(this);
        btnLayout.setOrientation(LinearLayout.HORIZONTAL);
        btnLayout.setPadding(0, 8, 0, 0);

        Button btnDelete = new Button(this);
        btnDelete.setText("DELETE VOUCHER");
        btnDelete.setBackgroundColor(0xFFC62828);
        btnDelete.setTextColor(0xFFFFFFFF);
        btnDelete.setTextSize(10.0f);
        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                api.deleteData(token, id, new BackendApi.ApiCallback() {
                    @Override
                    public void onSuccess(String response) {
                        Toast.makeText(ExpenseActivity.this, "Voucher Voided", Toast.LENGTH_SHORT).show();
                        loadExpenses();
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Toast.makeText(ExpenseActivity.this, "Failed voiding voucher: " + errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        btnLayout.addView(btnDelete);

        row.addView(tvTitle);
        row.addView(tvDesc);
        row.addView(btnLayout);

        layoutExpenseList.addView(row);
    }
}