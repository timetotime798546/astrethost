package com.expensetracker.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AddTransactionActivity extends Activity {

    private Spinner spnType, spnCategory;
    private EditText etTitle, etAmount, etDate, etNote;
    private Button btnSave;
    private ProgressBar saveProgress;
    private BackendApi api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_transaction);

        api = new BackendApi(this);

        spnType = (Spinner) findViewById(R.id.spnType);
        spnCategory = (Spinner) findViewById(R.id.spnCategory);
        etTitle = (EditText) findViewById(R.id.etTitle);
        etAmount = (EditText) findViewById(R.id.etAmount);
        etDate = (EditText) findViewById(R.id.etDate);
        etNote = (EditText) findViewById(R.id.etNote);
        btnSave = (Button) findViewById(R.id.btnSave);
        saveProgress = (ProgressBar) findViewById(R.id.saveProgress);

        // Populate Types
        String[] types = new String[]{"Expense", "Income"};
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, types);
        spnType.setAdapter(typeAdapter);

        // Populate Categories
        String[] categories = new String[]{"Food & Groceries", "Transport", "Bills & Utilities", "Salary & Income", "Shopping", "Entertainment", "Health", "Other"};
        ArrayAdapter<String> catAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, categories);
        spnCategory.setAdapter(catAdapter);

        // Auto-fill today's date
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        etDate.setText(sdf.format(new Date()));

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveEntry();
            }
        });
    }

    private void saveEntry() {
        String title = etTitle.getText().toString().trim();
        String amountStr = etAmount.getText().toString().trim();
        String dateStr = etDate.getText().toString().trim();
        String note = etNote.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(this, "Please enter a title", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = 0.0;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (Exception e) {
            Toast.makeText(this, "Please enter a valid numeric amount", Toast.LENGTH_SHORT).show();
            return;
        }

        if (amount <= 0.0) {
            Toast.makeText(this, "Amount must be greater than zero", Toast.LENGTH_SHORT).show();
            return;
        }

        String typeStr = spnType.getSelectedItem().toString().toLowerCase(Locale.getDefault());
        String category = spnCategory.getSelectedItem().toString();

        final TransactionModel transaction = new TransactionModel(null, title, amount, typeStr, category, dateStr, note);

        saveProgress.setVisibility(View.VISIBLE);
        btnSave.setEnabled(false);

        api.createTransaction(transaction, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                saveProgress.setVisibility(View.GONE);
                btnSave.setEnabled(true);
                Toast.makeText(AddTransactionActivity.this, "Transaction saved!", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(String errorMessage) {
                saveProgress.setVisibility(View.GONE);
                btnSave.setEnabled(true);
                Toast.makeText(AddTransactionActivity.this, "Failed to save: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }
}