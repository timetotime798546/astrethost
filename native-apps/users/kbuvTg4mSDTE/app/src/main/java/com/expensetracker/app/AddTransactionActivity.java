package com.expensetracker.app;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AddTransactionActivity extends Activity {

    private EditText amountEditText;
    private EditText descriptionEditText;
    private TextView dateTextView;
    private Spinner categorySpinner;
    private Button saveTransactionButton;
    private TextView addTransactionTitle;

    private DatabaseHelper dbHelper;
    private String transactionType;
    private Calendar selectedDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_transaction);

        dbHelper = new DatabaseHelper(this);

        amountEditText = (EditText) findViewById(R.id.amountEditText);
        descriptionEditText = (EditText) findViewById(R.id.descriptionEditText);
        dateTextView = (TextView) findViewById(R.id.dateTextView);
        categorySpinner = (Spinner) findViewById(R.id.categorySpinner);
        saveTransactionButton = (Button) findViewById(R.id.saveTransactionButton);
        addTransactionTitle = (TextView) findViewById(R.id.addTransactionTitle);

        transactionType = getIntent().getStringExtra(\"transaction_type\");
        if (\"income\".equals(transactionType)) {
            addTransactionTitle.setText(\"Add Income\");
            saveTransactionButton.setBackgroundResource(android.R.color.holo_green_dark);
        } else {
            addTransactionTitle.setText(\"Add Expense\");
            saveTransactionButton.setBackgroundResource(android.R.color.holo_red_dark);
        }

        selectedDate = Calendar.getInstance();
        updateDateTextView(selectedDate.getTime());

        dateTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog();
            }
        });

        loadCategoriesIntoSpinner();

        saveTransactionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveTransaction();
            }
        });
    }

    private void showDatePickerDialog() {
        new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                selectedDate.set(Calendar.YEAR, year);
                selectedDate.set(Calendar.MONTH, monthOfYear);
                selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                updateDateTextView(selectedDate.getTime());
            }
        }, selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH), selectedDate.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void updateDateTextView(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat(\"yyyy-MM-dd\", Locale.getDefault());
        dateTextView.setText(sdf.format(date));
    }

    private void loadCategoriesIntoSpinner() {
        List<String> categoryNames = new ArrayList<String>();
        final List<Long> categoryIds = new ArrayList<Long>();

        Cursor cursor = dbHelper.getAllCategories();
        if (cursor != null && cursor.moveToFirst()) {
            do {
                categoryIds.add(cursor.getLong(cursor.getColumnIndex(DatabaseHelper.COLUMN_CATEGORY_ID)));
                categoryNames.add(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_CATEGORY_NAME)));
            } while (cursor.moveToNext());
            cursor.close();
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, categoryNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);

        categorySpinner.setTag(categoryIds); // Store IDs as tag for retrieval
    }

    private void saveTransaction() {
        String amountStr = amountEditText.getText().toString();
        String description = descriptionEditText.getText().toString();
        String date = dateTextView.getText().toString();

        if (amountStr.trim().isEmpty()) {
            Toast.makeText(this, \"Please enter an amount\", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = Double.parseDouble(amountStr);
        long categoryId = -1;

        List<Long> categoryIds = (List<Long>) categorySpinner.getTag();
        if (categoryIds != null && categoryIds.size() > 0 && categorySpinner.getSelectedItemPosition() >= 0) {
            categoryId = categoryIds.get(categorySpinner.getSelectedItemPosition());
        }

        long result = dbHelper.addTransaction(transactionType, amount, description, date, categoryId);

        if (result > 0) {
            Toast.makeText(this, \"Transaction saved!\", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, \"Failed to save transaction.\", Toast.LENGTH_SHORT).show();
        }
    }
}