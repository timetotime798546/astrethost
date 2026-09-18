package com.expensetracker.app;

import android.app.Activity;
import android.graphics.Color;
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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private DatabaseHelper dbHelper;
    private TextView tvTotalAmount;
    private LinearLayout listContainer;
    private LinearLayout formContainer;
    private Button btnAddExpenseToggle;
    
    // Form fields
    private EditText etTitle;
    private EditText etAmount;
    private Spinner spinnerCategory;
    private Button btnSave;
    private Button btnCancel;

    private final String[] categories = {"Food", "Transport", "Shopping", "Entertainment", "Bills", "Others"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Hide native top bar to show customizable custom title area
        if (getActionBar() != null) {
            getActionBar().hide();
        }
        
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);

        // Bind Views
        tvTotalAmount = findViewById(R.id.tvTotalAmount);
        listContainer = findViewById(R.id.listContainer);
        formContainer = findViewById(R.id.formContainer);
        btnAddExpenseToggle = findViewById(R.id.btnAddExpenseToggle);
        
        etTitle = findViewById(R.id.etTitle);
        etAmount = findViewById(R.id.etAmount);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);

        // Populate system spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);

        // Toggle Form visibility
        btnAddExpenseToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showForm(true);
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showForm(false);
                clearForm();
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveExpense();
            }
        });

        // Pull and present existing local database records
        loadData();
    }

    private void showForm(boolean show) {
        if (show) {
            formContainer.setVisibility(View.VISIBLE);
            btnAddExpenseToggle.setVisibility(View.GONE);
        } else {
            formContainer.setVisibility(View.GONE);
            btnAddExpenseToggle.setVisibility(View.VISIBLE);
        }
    }

    private void clearForm() {
        etTitle.setText("");
        etAmount.setText("");
        spinnerCategory.setSelection(0);
    }

    private void saveExpense() {
        String title = etTitle.getText().toString().trim();
        String amountStr = etAmount.getText().toString().trim();
        String category = spinnerCategory.getSelectedItem().toString();

        if (TextUtils.isEmpty(title)) {
            etTitle.setError("Title is required");
            return;
        }

        if (TextUtils.isEmpty(amountStr)) {
            etAmount.setError("Amount is required");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                etAmount.setError("Amount must be greater than zero");
                return;
            }
        } catch (NumberFormatException e) {
            etAmount.setError("Invalid amount format");
            return;
        }

        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        boolean isInserted = dbHelper.insertExpense(title, amount, category, currentDate);
        if (isInserted) {
            Toast.makeText(this, "Expense added successfully!", Toast.LENGTH_SHORT).show();
            clearForm();
            showForm(false);
            loadData();
        } else {
            Toast.makeText(this, "Failed to save expense", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadData() {
        // Compute and update dashboard sum balance representation
        double total = dbHelper.getTotalExpenses();
        tvTotalAmount.setText(String.format(Locale.getDefault(), "$%.2f", total));

        // Wipe previous layout bindings and perform raw update representation
        listContainer.removeAllViews();
        List<Expense> expenses = dbHelper.getAllExpenses();

        if (expenses.isEmpty()) {
            TextView emptyView = new TextView(this);
            emptyView.setText("No expenses recorded yet.\nTap below to add your first expense!");
            emptyView.setGravity(android.view.Gravity.CENTER);
            emptyView.setTextColor(Color.GRAY);
            emptyView.setTextSize(16);
            emptyView.setPadding(0, 80, 0, 80);
            listContainer.addView(emptyView);
        } else {
            LayoutInflater inflater = LayoutInflater.from(this);
            for (int i = 0; i < expenses.size(); i++) {
                final Expense expense = expenses.get(i);
                View itemView = inflater.inflate(R.layout.item_expense, listContainer, false);

                TextView tvTitle = itemView.findViewById(R.id.tvItemTitle);
                TextView tvDetails = itemView.findViewById(R.id.tvItemDetails);
                TextView tvAmount = itemView.findViewById(R.id.tvItemAmount);
                View btnDelete = itemView.findViewById(R.id.btnDelete);

                tvTitle.setText(expense.getTitle());
                tvDetails.setText(expense.getCategory() + " | " + expense.getDate());
                tvAmount.setText(String.format(Locale.getDefault(), "$%.2f", expense.getAmount()));

                btnDelete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        deleteExpense(expense.getId());
                    }
                });

                listContainer.addView(itemView);
            }
        }
    }

    private void deleteExpense(int id) {
        boolean isDeleted = dbHelper.deleteExpense(id);
        if (isDeleted) {
            Toast.makeText(this, "Expense deleted", Toast.LENGTH_SHORT).show();
            loadData();
        } else {
            Toast.makeText(this, "Failed to delete expense", Toast.LENGTH_SHORT).show();
        }
    }
}