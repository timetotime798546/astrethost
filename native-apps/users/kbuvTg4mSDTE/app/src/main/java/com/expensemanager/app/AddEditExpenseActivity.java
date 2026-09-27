package com.expensemanager.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONObject;

public class AddEditExpenseActivity extends Activity {
    private TextView tvFormTitle;
    private EditText etTitle;
    private EditText etAmount;
    private Spinner spinnerCategory;
    private EditText etDate;
    private EditText etNote;
    private Button btnSave;
    private Button btnCancel;
    private Button btnDelete;
    private ProgressBar pbLoading;

    private BackendApi api;
    private boolean isEdit = false;
    private String recordId = null;

    private static final String[] CATEGORIES = {"Food", "Travel", "Utilities", "Entertainment", "Others"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit);

        api = new BackendApi(this);

        tvFormTitle = (TextView) findViewById(R.id.tv_form_title);
        etTitle = (EditText) findViewById(R.id.et_title);
        etAmount = (EditText) findViewById(R.id.et_amount);
        spinnerCategory = (Spinner) findViewById(R.id.spinner_category);
        etDate = (EditText) findViewById(R.id.et_date);
        etNote = (EditText) findViewById(R.id.et_note);
        btnSave = (Button) findViewById(R.id.btn_save);
        btnCancel = (Button) findViewById(R.id.btn_cancel);
        btnDelete = (Button) findViewById(R.id.btn_delete);
        pbLoading = (ProgressBar) findViewById(R.id.pb_form_loading);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, CATEGORIES);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);

        etDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePicker();
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.getBoolean("is_edit", false)) {
            isEdit = true;
            recordId = extras.getString("id");
            tvFormTitle.setText("Edit Expense");
            etTitle.setText(extras.getString("title", ""));
            etAmount.setText(String.valueOf(extras.getDouble("amount", 0.0)));
            
            String cat = extras.getString("category", "Others");
            int catIdx = 0;
            for (int i = 0; i < CATEGORIES.length; i++) {
                if (CATEGORIES[i].equalsIgnoreCase(cat)) {
                    catIdx = i;
                    break;
                }
            }
            spinnerCategory.setSelection(catIdx);
            etDate.setText(extras.getString("date", ""));
            etNote.setText(extras.getString("note", ""));

            btnDelete.setVisibility(View.VISIBLE);
            btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    confirmDelete();
                }
            });
        }

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveExpense();
            }
        });
    }

    private void showDatePicker() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        int year = cal.get(java.util.Calendar.YEAR);
        int month = cal.get(java.util.Calendar.MONTH);
        int day = cal.get(java.util.Calendar.DAY_OF_MONTH);

        android.app.DatePickerDialog dpd = new android.app.DatePickerDialog(this,
                new android.app.DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(android.widget.DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        String formatted = String.format(java.util.Locale.US, "%d-%02d-%02d", year, monthOfYear + 1, dayOfMonth);
                        etDate.setText(formatted);
                    }
                }, year, month, day);
        dpd.show();
    }

    private void saveExpense() {
        String title = etTitle.getText().toString().trim();
        String amountStr = etAmount.getText().toString().trim();
        String category = spinnerCategory.getSelectedItem().toString();
        String date = etDate.getText().toString().trim();
        String note = etNote.getText().toString().trim();

        if (title.isEmpty() || amountStr.isEmpty() || date.isEmpty()) {
            Toast.makeText(this, "Title, Amount and Date are required", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Enter a valid amount", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        BackendApi.ApiCallback callback = new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                setLoading(false);
                Toast.makeText(AddEditExpenseActivity.this, "Saved successfully", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            }

            @Override
            public void onError(String error) {
                setLoading(false);
                Toast.makeText(AddEditExpenseActivity.this, "Save failed: " + error, Toast.LENGTH_LONG).show();
            }
        };

        if (isEdit) {
            api.updateExpense(recordId, title, amount, category, date, note, callback);
        } else {
            api.createExpense(title, amount, category, date, note, callback);
        }
    }

    private void confirmDelete() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Delete Record")
                .setMessage("Are you sure you want to delete this expense record?")
                .setPositiveButton("Yes", new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface dialog, int which) {
                        deleteExpense();
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void deleteExpense() {
        if (recordId == null) return;
        setLoading(true);

        api.deleteExpense(recordId, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                setLoading(false);
                Toast.makeText(AddEditExpenseActivity.this, "Deleted successfully", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            }

            @Override
            public void onError(String error) {
                setLoading(false);
                Toast.makeText(AddEditExpenseActivity.this, "Delete failed: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        pbLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSave.setEnabled(!loading);
        btnCancel.setEnabled(!loading);
        btnDelete.setEnabled(!loading);
    }
}