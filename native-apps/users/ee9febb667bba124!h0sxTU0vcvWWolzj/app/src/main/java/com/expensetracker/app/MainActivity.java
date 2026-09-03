package com.expensetracker.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class MainActivity extends Activity {

    private List<Transaction> transactionsList = new ArrayList<>();
    private DecimalFormat currencyFormatter = new DecimalFormat("$#,##0.00");
    private SimpleDateFormat dateFormatter = new SimpleDateFormat("MMM dd, yyyy", Locale.US);

    private TextView tvTotalBalance;
    private TextView tvTotalIncome;
    private TextView tvTotalExpenses;
    private EditText etTitle;
    private EditText etAmount;
    private Spinner spinnerType;
    private Spinner spinnerCategory;
    private Button btnAdd;
    private LinearLayout containerTransactions;
    private TextView tvEmptyState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bind Views
        tvTotalBalance = findViewById(R.id.tv_total_balance);
        tvTotalIncome = findViewById(R.id.tv_total_income);
        tvTotalExpenses = findViewById(R.id.tv_total_expenses);
        etTitle = findViewById(R.id.et_title);
        etAmount = findViewById(R.id.et_amount);
        spinnerType = findViewById(R.id.spinner_type);
        spinnerCategory = findViewById(R.id.spinner_category);
        btnAdd = findViewById(R.id.btn_add);
        containerTransactions = findViewById(R.id.container_transactions);
        tvEmptyState = findViewById(R.id.tv_empty_state);

        // Populate Transaction Type Spinner
        String[] types = {"Income", "Expense"};
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, types);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerType.setAdapter(typeAdapter);

        // Populate Category Spinner
        String[] categories = {
            "Salary & Bonus", 
            "Food & Dining", 
            "Shopping", 
            "Rent & Bills", 
            "Entertainment", 
            "Travel & Transport", 
            "Health & Medical", 
            "Investment", 
            "Others"
        };
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(categoryAdapter);

        // Load Persistent Transactions
        loadTransactionsFromStorage();

        // Setup Buttons Click Action
        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleAddNewTransaction();
            }
        });

        // Initialize user interface
        renderUI();
    }

    private void handleAddNewTransaction() {
        String title = etTitle.getText().toString().trim();
        String amountStr = etAmount.getText().toString().trim();
        String type = spinnerType.getSelectedItem().toString().toUpperCase(Locale.US);
        String category = spinnerCategory.getSelectedItem().toString();

        if (TextUtils.isEmpty(title)) {
            Toast.makeText(this, "Please enter a valid title.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(amountStr)) {
            Toast.makeText(this, "Please enter an amount.", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter a valid numeric amount.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (amount <= 0) {
            Toast.makeText(this, "Amount must be greater than zero.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Generate Transaction Record
        String id = UUID.randomUUID().toString();
        long timestamp = System.currentTimeMillis();

        Transaction newTransaction = new Transaction(id, title, amount, type, category, timestamp);
        transactionsList.add(0, newTransaction); // Prepend to show most recent at top

        saveTransactionsToStorage();
        renderUI();

        // Clean Fields
        etTitle.setText("");
        etAmount.setText("");
        etTitle.clearFocus();
        etAmount.clearFocus();

        Toast.makeText(this, "Transaction added successfully!", Toast.LENGTH_SHORT).show();
    }

    private void deleteTransaction(final int position) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Entry")
                .setMessage("Are you sure you want to delete this transaction?")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        transactionsList.remove(position);
                        saveTransactionsToStorage();
                        renderUI();
                        Toast.makeText(MainActivity.this, "Transaction removed.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void renderUI() {
        double totalIncome = 0;
        double totalExpense = 0;

        for (Transaction t : transactionsList) {
            if ("INCOME".equals(t.getType())) {
                totalIncome += t.getAmount();
            } else {
                totalExpense += t.getAmount();
            }
        }

        double totalBalance = totalIncome - totalExpense;

        // Render Summary Texts
        tvTotalBalance.setText(currencyFormatter.format(totalBalance));
        tvTotalIncome.setText("+" + currencyFormatter.format(totalIncome));
        tvTotalExpenses.setText("-" + currencyFormatter.format(totalExpense));

        if (totalBalance < 0) {
            tvTotalBalance.setTextColor(0xFFFF453A); // Crimson Red
        } else if (totalBalance > 0) {
            tvTotalBalance.setTextColor(0xFF30D158); // Apple Green
        } else {
            tvTotalBalance.setTextColor(0xFFFFFFFF); // Clean White
        }

        // Clear dynamic layout rows
        containerTransactions.removeAllViews();

        if (transactionsList.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
        } else {
            tvEmptyState.setVisibility(View.GONE);

            LayoutInflater inflater = LayoutInflater.from(this);
            for (int i = 0; i < transactionsList.size(); i++) {
                final int index = i;
                Transaction t = transactionsList.get(i);

                View itemView = inflater.inflate(R.layout.item_transaction, containerTransactions, false);

                View viewIndicator = itemView.findViewById(R.id.view_indicator);
                TextView tvTitle = itemView.findViewById(R.id.tv_item_title);
                TextView tvCategory = itemView.findViewById(R.id.tv_item_category);
                TextView tvDate = itemView.findViewById(R.id.tv_item_date);
                TextView tvAmount = itemView.findViewById(R.id.tv_item_amount);
                View btnDelete = itemView.findViewById(R.id.btn_item_delete);

                tvTitle.setText(t.getTitle());
                tvCategory.setText(t.getCategory());
                tvDate.setText(dateFormatter.format(new Date(t.getTimestamp())));

                if ("INCOME".equals(t.getType())) {
                    viewIndicator.setBackgroundColor(0xFF30D158); // Green
                    tvAmount.setText("+" + currencyFormatter.format(t.getAmount()));
                    tvAmount.setTextColor(0xFF30D158);
                } else {
                    viewIndicator.setBackgroundColor(0xFFFF453A); // Red
                    tvAmount.setText("-" + currencyFormatter.format(t.getAmount()));
                    tvAmount.setTextColor(0xFFFF453A);
                }

                btnDelete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        deleteTransaction(index);
                    }
                });

                containerTransactions.addView(itemView);
            }
        }
    }

    private void saveTransactionsToStorage() {
        SharedPreferences prefs = getSharedPreferences("ExpenseTrackerPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        JSONArray array = new JSONArray();
        for (Transaction t : transactionsList) {
            try {
                array.put(t.toJsonObject());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        editor.putString("transactions_json", array.toString());
        editor.apply();
    }

    private void loadTransactionsFromStorage() {
        transactionsList.clear();
        SharedPreferences prefs = getSharedPreferences("ExpenseTrackerPrefs", MODE_PRIVATE);
        String jsonString = prefs.getString("transactions_json", "[]");
        try {
            JSONArray array = new JSONArray(jsonString);
            for (int i = 0; i < array.length(); i++) {
                transactionsList.add(Transaction.fromJsonObject(array.getJSONObject(i)));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}