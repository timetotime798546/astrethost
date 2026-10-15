package com.modernexpensetracker.app;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AddTransactionActivity extends Activity {

    private RadioGroup rgType;
    private EditText etAmount, etTitle, etNotes;
    private Spinner spCategory;
    private Button btnPickDate, btnSave;
    private ProgressBar pbLoading;

    private BackendApi backendApi;
    private final Calendar selectedCalendar = Calendar.getInstance();

    private final String[] expenseCategories = new String[]{
            "Food & Dining", "Groceries", "Transport", "Rent / Housing", "Bills & Utilities",
            "Shopping", "Health & Medical", "Entertainment", "Personal Care", "Other Expense"
    };

    private final String[] incomeCategories = new String[]{
            "Salary", "Freelance / Gig", "Investment", "Business", "Gift", "Refund", "Other Income"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_transaction);

        backendApi = new BackendApi(this);

        rgType = (RadioGroup) findViewById(R.id.rg_transaction_type);
        etAmount = (EditText) findViewById(R.id.et_amount);
        etTitle = (EditText) findViewById(R.id.et_title);
        etNotes = (EditText) findViewById(R.id.et_notes);
        spCategory = (Spinner) findViewById(R.id.sp_category);
        btnPickDate = (Button) findViewById(R.id.btn_pick_date);
        btnSave = (Button) findViewById(R.id.btn_save_transaction);
        pbLoading = (ProgressBar) findViewById(R.id.pb_save_loading);

        setupCategorySpinner(true);
        updateDateButtonLabel();

        rgType.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                boolean isExpense = (checkedId == R.id.rb_expense);
                setupCategorySpinner(isExpense);
            }
        });

        btnPickDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePicker();
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveTransaction();
            }
        });
    }

    private void setupCategorySpinner(boolean isExpense) {
        String[] categories = isExpense ? expenseCategories : incomeCategories;
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, categories);
        spCategory.setAdapter(adapter);
    }

    private void updateDateButtonLabel() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        btnPickDate.setText("Date: " + sdf.format(selectedCalendar.getTime()));
    }

    private void showDatePicker() {
        DatePickerDialog dialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                selectedCalendar.set(Calendar.YEAR, year);
                selectedCalendar.set(Calendar.MONTH, month);
                selectedCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                updateDateButtonLabel();
            }
        }, selectedCalendar.get(Calendar.YEAR), selectedCalendar.get(Calendar.MONTH), selectedCalendar.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    private void saveTransaction() {
        String amountStr = etAmount.getText().toString().trim();
        String title = etTitle.getText().toString().trim();
        String notes = etNotes.getText().toString().trim();
        String category = (String) spCategory.getSelectedItem();
        boolean isExpense = (rgType.getCheckedRadioButtonId() == R.id.rb_expense);
        String type = isExpense ? "EXPENSE" : "INCOME";

        if (amountStr.isEmpty()) {
            etAmount.setError("Please enter amount");
            etAmount.requestFocus();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                etAmount.setError("Amount must be greater than zero");
                etAmount.requestFocus();
                return;
            }
        } catch (NumberFormatException e) {
            etAmount.setError("Invalid number");
            etAmount.requestFocus();
            return;
        }

        if (title.isEmpty()) {
            title = category;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        String dateStr = sdf.format(selectedCalendar.getTime());

        pbLoading.setVisibility(View.VISIBLE);
        btnSave.setEnabled(false);

        try {
            JSONObject dataObj = new JSONObject();
            dataObj.put("title", title);
            dataObj.put("amount", amount);
            dataObj.put("type", type);
            dataObj.put("category", category);
            dataObj.put("date", dateStr);
            dataObj.put("notes", notes);

            backendApi.createData("transactions", dataObj, new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    pbLoading.setVisibility(View.GONE);
                    Toast.makeText(AddTransactionActivity.this, "Transaction saved!", Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onError(String error) {
                    pbLoading.setVisibility(View.GONE);
                    btnSave.setEnabled(true);
                    Toast.makeText(AddTransactionActivity.this, "Error saving: " + error, Toast.LENGTH_LONG).show();
                }
            });
        } catch (Exception e) {
            pbLoading.setVisibility(View.GONE);
            btnSave.setEnabled(true);
            Toast.makeText(this, "Error building request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}