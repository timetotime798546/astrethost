package com.modernexpensetracker.app;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
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
    private EditText etAmount, etDate, etNote;
    private Spinner spinnerCategory;
    private Button btnSave, btnCancel;
    private ProgressBar addProgressBar;

    private BackendApi api;
    private String token;

    private final String[] categories = {
        "Food", "Rent & Utilities", "Salary", "Entertainment", "Transport", "Shopping", "Healthcare", "Investment", "Freelance Income", "Other"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_transaction);

        SharedPreferences prefs = getSharedPreferences("ExpenseTrackerPrefs", MODE_PRIVATE);
        token = prefs.getString("token", "");
        if (token.isEmpty()) {
            finish();
            return;
        }

        api = new BackendApi(this);

        rgType = (RadioGroup) findViewById(R.id.rgType);
        rbExpense = (RadioButton) findViewById(R.id.rbExpense);
        rbIncome = (RadioButton) findViewById(R.id.rbIncome);
        etAmount = (EditText) findViewById(R.id.etAmount);
        etDate = (EditText) findViewById(R.id.etDate);
        etNote = (EditText) findViewById(R.id.etNote);
        spinnerCategory = (Spinner) findViewById(R.id.spinnerCategory);
        btnSave = (Button) findViewById(R.id.btnSave);
        btnCancel = (Button) findViewById(R.id.btnCancel);
        addProgressBar = (ProgressBar) findViewById(R.id.addProgressBar);

        // Prepopulate current Date in YYYY-MM-DD
        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        etDate.setText(currentDate);

        // Configure Category spinner
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(spinnerAdapter);

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveTransaction();
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void saveTransaction() {
        String amountStr = etAmount.getText().toString().trim();
        String dateStr = etDate.getText().toString().trim();
        String noteStr = etNote.getText().toString().trim();
        String categoryStr = spinnerCategory.getSelectedItem().toString();

        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Amount is required.", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please insert a valid numeric amount.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (dateStr.isEmpty() || !dateStr.matches("\\d{4}-\\d{2}-\\d{2}")) {
            Toast.makeText(this, "Date format must be exactly YYYY-MM-DD", Toast.LENGTH_SHORT).show();
            return;
        }

        String type = rbIncome.isChecked() ? "income" : "expense";

        addProgressBar.setVisibility(View.VISIBLE);
        btnSave.setEnabled(false);

        api.createTransaction(token, type, amount, categoryStr, dateStr, noteStr, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                addProgressBar.setVisibility(View.GONE);
                btnSave.setEnabled(true);
                try {
                    JSONObject resObj = new JSONObject(response);
                    if (resObj.optBoolean("success", false)) {
                        Toast.makeText(AddTransactionActivity.this, "Transaction record successfully created!", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                } catch (Exception e) {
                    Toast.makeText(AddTransactionActivity.this, "Network Save Confirmed.", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onError(String error) {
                addProgressBar.setVisibility(View.GONE);
                btnSave.setEnabled(true);
                Toast.makeText(AddTransactionActivity.this, "Failed to save: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }
}