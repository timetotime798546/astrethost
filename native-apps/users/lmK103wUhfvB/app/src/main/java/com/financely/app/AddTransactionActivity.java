package com.financely.app;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AddTransactionActivity extends Activity {

    private RadioGroup rgType;
    private RadioButton rbExpense, rbIncome;
    private EditText etAmount, etCustomCategory, etDate, etDescription;
    private Spinner spinnerCategory;
    private Button btnSave, btnCancel;

    private BackendApi api;
    private String token;
    private ProgressDialog progressDialog;

    private final String[] incomeCategories = {"Salary", "Investments", "Freelance", "Gifts", "Other Income", "[Add Custom Category]"};
    private final String[] expenseCategories = {"Food & Groceries", "Rent & Housing", "Utilities", "Transport", "Shopping", "Entertainment", "Medical", "Education", "Other Expense", "[Add Custom Category]"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_transaction);

        SharedPreferences prefs = getSharedPreferences("financely_prefs", Context.MODE_PRIVATE);
        token = prefs.getString("token", null);

        api = new BackendApi(this);

        rgType = (RadioGroup) findViewById(R.id.rg_type);
        rbExpense = (RadioButton) findViewById(R.id.rb_expense);
        rbIncome = (RadioButton) findViewById(R.id.rb_income);
        etAmount = (EditText) findViewById(R.id.et_amount);
        etCustomCategory = (EditText) findViewById(R.id.et_custom_category);
        etDate = (EditText) findViewById(R.id.et_date);
        etDescription = (EditText) findViewById(R.id.et_description);
        spinnerCategory = (Spinner) findViewById(R.id.spinner_category);
        btnSave = (Button) findViewById(R.id.btn_save_transaction);
        btnCancel = (Button) findViewById(R.id.btn_cancel_transaction);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Synchronizing transaction records...");
        progressDialog.setCancelable(false);

        // Prepopulate current date inside native view limits
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        etDate.setText(sdf.format(new Date()));

        updateCategoryDropdown(true); // Defaults to true (Expense) as checked on layout startup

        rgType.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                updateCategoryDropdown(checkedId == R.id.rb_expense);
            }
        });

        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = parent.getItemAtPosition(position).toString();
                if (selected.equals("[Add Custom Category]")) {
                    etCustomCategory.setVisibility(View.VISIBLE);
                } else {
                    etCustomCategory.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                processTransactionForm();
            }
        });
    }

    private void updateCategoryDropdown(boolean isExpense) {
        String[] list = isExpense ? expenseCategories : incomeCategories;
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, list);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);
    }

    private void processTransactionForm() {
        String amountStr = etAmount.getText().toString().trim();
        String dateStr = etDate.getText().toString().trim();
        String descStr = etDescription.getText().toString().trim();

        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Please enter an amount", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid amount format.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (amount <= 0) {
            Toast.makeText(this, "Amount must be strictly greater than zero.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (dateStr.isEmpty() || !dateStr.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
            Toast.makeText(this, "Please insert date format matching YYYY-MM-DD", Toast.LENGTH_LONG).show();
            return;
        }

        String categorySelected = spinnerCategory.getSelectedItem().toString();
        if (categorySelected.equals("[Add Custom Category]")) {
            categorySelected = etCustomCategory.getText().toString().trim();
            if (categorySelected.isEmpty()) {
                Toast.makeText(this, "Please write your customized category name", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        String type = rbIncome.isChecked() ? "Income" : "Expense";

        progressDialog.show();
        api.createTransaction(token, type, amount, categorySelected, descStr, dateStr, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                progressDialog.dismiss();
                if (response.optBoolean("success", false)) {
                    Toast.makeText(AddTransactionActivity.this, "Transaction logged successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(AddTransactionActivity.this, "Server rejected dynamic transaction entry.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onError(String errorMessage) {
                progressDialog.dismiss();
                Toast.makeText(AddTransactionActivity.this, "Log Failed: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }
}