package com.expensetracker.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AddTransactionActivity extends Activity {

    private RadioGroup rgType;
    private EditText etTitle;
    private EditText etAmount;
    private Spinner spCategory;
    private EditText etDate;
    private EditText etNote;
    private Button btnSave;
    private ProgressBar pbLoading;

    private BackendApi backendApi;
    private SessionManager sessionManager;

    private final String[] categories = new String[]{
            "Food & Dining",
            "Transportation",
            "Housing & Rent",
            "Shopping & Retail",
            "Entertainment",
            "Healthcare",
            "Salary / Wages",
            "Investments & Dividends",
            "Freelance",
            "Utilities",
            "Other"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_transaction);

        backendApi = new BackendApi(this);
        sessionManager = new SessionManager(this);

        rgType = (RadioGroup) findViewById(R.id.rgType);
        etTitle = (EditText) findViewById(R.id.etTitle);
        etAmount = (EditText) findViewById(R.id.etAmount);
        spCategory = (Spinner) findViewById(R.id.spCategory);
        etDate = (EditText) findViewById(R.id.etDate);
        etNote = (EditText) findViewById(R.id.etNote);
        btnSave = (Button) findViewById(R.id.btnSaveTransaction);
        pbLoading = (ProgressBar) findViewById(R.id.pbAddLoading);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, categories);
        spCategory.setAdapter(adapter);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        etDate.setText(sdf.format(new Date()));

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveTransaction();
            }
        });
    }

    private void saveTransaction() {
        String title = etTitle.getText().toString().trim();
        String amountStr = etAmount.getText().toString().trim();
        String dateStr = etDate.getText().toString().trim();
        String noteStr = etNote.getText().toString().trim();
        String category = spCategory.getSelectedItem().toString();
        int selectedTypeId = rgType.getCheckedRadioButtonId();
        String type = (selectedTypeId == R.id.rbIncome) ? "income" : "expense";

        if (title.isEmpty()) {
            Toast.makeText(this, "Title cannot be blank", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                Toast.makeText(this, "Amount must be greater than zero", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (Exception e) {
            Toast.makeText(this, "Please enter a valid numeric amount", Toast.LENGTH_SHORT).show();
            return;
        }

        if (dateStr.isEmpty()) {
            Toast.makeText(this, "Please enter date (YYYY-MM-DD)", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            JSONObject dataObj = new JSONObject();
            dataObj.put("title", title);
            dataObj.put("amount", amount);
            dataObj.put("type", type);
            dataObj.put("category", category);
            dataObj.put("date", dateStr);
            dataObj.put("note", noteStr);

            setLoading(true);
            backendApi.createTransaction(sessionManager.getToken(), dataObj, new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    setLoading(false);
                    Toast.makeText(AddTransactionActivity.this, "Transaction saved!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                }

                @Override
                public void onError(String errorMessage) {
                    setLoading(false);
                    Toast.makeText(AddTransactionActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
            });
        } catch (Exception e) {
            Toast.makeText(this, "Error building payload: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void setLoading(boolean loading) {
        pbLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSave.setEnabled(!loading);
    }
}