package com.expensemanager.app;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Calendar;

public class ExpenseDetailActivity extends Activity {
    private TextView tvFormTitle;
    private EditText etTitle, etAmount, etDate, etNote;
    private Spinner spinnerCategory;
    private Button btnSave, btnDelete, btnCancel;
    
    private BackendApi backendApi;
    private ProgressDialog progressDialog;
    private String expenseId = null;
    
    private final String[] categories = {"Food", "Travel", "Utilities", "Entertainment", "Medical", "Other"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expense_detail);

        backendApi = new BackendApi(this);

        tvFormTitle = findViewById(R.id.tv_form_title);
        etTitle = findViewById(R.id.et_expense_title);
        etAmount = findViewById(R.id.et_expense_amount);
        spinnerCategory = findViewById(R.id.spinner_expense_category);
        etDate = findViewById(R.id.et_expense_date);
        etNote = findViewById(R.id.et_expense_note);
        
        btnSave = findViewById(R.id.btn_save_expense);
        btnDelete = findViewById(R.id.btn_delete_expense);
        btnCancel = findViewById(R.id.btn_cancel);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Recording database transactional modification...");
        progressDialog.setCancelable(false);

        // Configure Category Spinner
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(catAdapter);

        // Date Picker Trigger Setup
        etDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openDatePicker();
            }
        });

        // Detect Editing vs Creating
        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.containsKey("id")) {
            expenseId = extras.getString("id");
            tvFormTitle.setText("Edit Expense Entry");
            etTitle.setText(extras.getString("title"));
            etAmount.setText(String.valueOf(extras.getDouble("amount", 0.0)));
            etDate.setText(extras.getString("date"));
            etNote.setText(extras.getString("note"));
            
            String category = extras.getString("category");
            for (int i = 0; i < categories.length; i++) {
                if (categories[i].equalsIgnoreCase(category)) {
                    spinnerCategory.setSelection(i);
                    break;
                }
            }
            btnDelete.setVisibility(View.VISIBLE);
        } else {
            // Set Default Today Date
            Calendar calendar = Calendar.getInstance();
            setDateString(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        }

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveTransaction();
            }
        });

        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteTransaction();
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void openDatePicker() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int yearSelected, int monthOfYear, int dayOfMonth) {
                setDateString(yearSelected, monthOfYear, dayOfMonth);
            }
        }, year, month, day);
        datePickerDialog.show();
    }

    private void setDateString(int year, int month, int day) {
        String formattedMonth = String.valueOf(month + 1);
        if (month + 1 < 10) formattedMonth = "0" + formattedMonth;

        String formattedDay = String.valueOf(day);
        if (day < 10) formattedDay = "0" + formattedDay;

        etDate.setText(year + "-" + formattedMonth + "-" + formattedDay);
    }

    private void saveTransaction() {
        String title = etTitle.getText().toString().trim();
        String amountStr = etAmount.getText().toString().trim();
        String date = etDate.getText().toString().trim();
        String note = etNote.getText().toString().trim();
        String category = spinnerCategory.getSelectedItem().toString();

        if (title.isEmpty() || amountStr.isEmpty() || date.isEmpty()) {
            Toast.makeText(this, "Please verify all required values highlighted (*).", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                Toast.makeText(this, "Amount must be a positive decimal.", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Numeric validation format error.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.show();

        Expense expense = new Expense(expenseId, title, amount, category, date, note);

        if (expenseId == null) {
            backendApi.createExpense(expense, new BackendApi.ApiCallback<Expense>() {
                @Override
                public void onSuccess(Expense result) {
                    progressDialog.dismiss();
                    Toast.makeText(ExpenseDetailActivity.this, "Expense recorded successful", Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onError(String message) {
                    progressDialog.dismiss();
                    Toast.makeText(ExpenseDetailActivity.this, "Failed saving record: " + message, Toast.LENGTH_LONG).show();
                }
            });
        } else {
            backendApi.updateExpense(expense, new BackendApi.ApiCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    progressDialog.dismiss();
                    Toast.makeText(ExpenseDetailActivity.this, "Expense modification processed successfully.", Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onError(String message) {
                    progressDialog.dismiss();
                    Toast.makeText(ExpenseDetailActivity.this, "Failed updating database: " + message, Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void deleteTransaction() {
        if (expenseId == null) return;

        progressDialog.setMessage("Removing transactional reference...");
        progressDialog.show();

        backendApi.deleteExpense(expenseId, new BackendApi.ApiCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                progressDialog.dismiss();
                Toast.makeText(ExpenseDetailActivity.this, "Transaction record erased.", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(String message) {
                progressDialog.dismiss();
                Toast.makeText(ExpenseDetailActivity.this, "Action Refused: " + message, Toast.LENGTH_LONG).show();
            }
        });
    }
}