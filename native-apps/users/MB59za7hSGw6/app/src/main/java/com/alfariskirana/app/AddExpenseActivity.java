package com.alfariskirana.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

public class AddExpenseActivity extends Activity {

    private DatabaseHelper dbHelper;
    private Spinner spCategory;
    private EditText etAmount;
    private EditText etNotes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_expense);

        dbHelper = new DatabaseHelper(this);
        spCategory = (Spinner) findViewById(R.id.sp_expense_category);
        etAmount = (EditText) findViewById(R.id.et_expense_amount);
        etNotes = (EditText) findViewById(R.id.et_expense_notes);
        Button btnSave = (Button) findViewById(R.id.btn_save_expense);

        String[] categories = {"Rent", "Electricity / Power", "Transport / Auto", "Packaging / Bags", "Staff & Wages", "Tea & Refreshments", "Other Miscellaneous"};
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, categories);
        spCategory.setAdapter(adapter);

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveExpense();
            }
        });
    }

    private void saveExpense() {
        String cat = spCategory.getSelectedItem().toString();
        String amtStr = etAmount.getText().toString().trim();
        String notes = etNotes.getText().toString().trim();

        if (amtStr.isEmpty()) {
            Toast.makeText(this, "Enter expense amount", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = Double.parseDouble(amtStr);
        dbHelper.addExpense(cat, amount, notes);
        Toast.makeText(this, "Expense recorded successfully", Toast.LENGTH_SHORT).show();
        finish();
    }
}