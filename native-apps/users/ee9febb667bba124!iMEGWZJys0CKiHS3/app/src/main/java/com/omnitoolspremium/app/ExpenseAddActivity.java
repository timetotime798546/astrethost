package com.omnitoolspremium.app;

import android.app.Activity;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

public class ExpenseAddActivity extends Activity {

    private EditText editTitle;
    private EditText editAmount;
    private Spinner spinnerExpCategory;
    private BudgetActivity.DatabaseHelper dbHelper;
    private final String[] categories = {"Food &amp; Dining", "Utilities &amp; Rent", "Entertainment", "Transit", "Others"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expense_add);

        editTitle = (EditText) findViewById(R.id.edit_title);
        editAmount = (EditText) findViewById(R.id.edit_amount);
        spinnerExpCategory = (Spinner) findViewById(R.id.spinner_exp_category);
        Button btnSaveExpense = (Button) findViewById(R.id.btn_save_expense);

        dbHelper = new BudgetActivity.DatabaseHelper(this);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, categories);
        spinnerExpCategory.setAdapter(adapter);

        btnSaveExpense.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveExpense();
            }
        });
    }

    private void saveExpense() {
        String title = editTitle.getText().toString().trim();
        String amountStr = editAmount.getText().toString().trim();
        String cat = spinnerExpCategory.getSelectedItem().toString();

        if (title.isEmpty() || amountStr.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid cost amount", Toast.LENGTH_SHORT).show();
            return;
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("title", title);
        values.put("amount", amount);
        values.put("category", cat);

        long newRowId = db.insert("expenses", null, values);
        if (newRowId != -1) {
            Toast.makeText(this, "Expense Logged!", Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        } else {
            Toast.makeText(this, "DB Write Error", Toast.LENGTH_SHORT).show();
        }
    }
}