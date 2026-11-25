package com.kkgstore.store;

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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AddExpenseActivity extends Activity {

    private DatabaseHelper dbHelper;
    private Spinner spCategory;
    private EditText etAmount, etNotes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (DemoLockManager.checkAndEnforceLock(this)) return;

        setContentView(R.layout.activity_add_expense);
        dbHelper = new DatabaseHelper(this);

        spCategory = (Spinner) findViewById(R.id.sp_expense_category);
        etAmount = (EditText) findViewById(R.id.et_expense_amount);
        etNotes = (EditText) findViewById(R.id.et_expense_notes);
        Button btnSave = (Button) findViewById(R.id.btn_save_expense);

        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new String[]{"Shop Rent (किराया)", "Electricity (बिजली का बिल)", "Salary / Labor", "Tea & Snacks", "Transport / Freight", "Other Overhead"});
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCategory.setAdapter(catAdapter);

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { saveExpense(); }
        });
    }

    private void saveExpense() {
        String category = spCategory.getSelectedItem().toString();
        String amtStr = etAmount.getText().toString().trim();
        String notes = etNotes.getText().toString().trim();

        if (amtStr.isEmpty()) {
            Toast.makeText(this, "कृपया खर्चा राशि दर्ज करें", Toast.LENGTH_SHORT).show();
            return;
        }

        double amt = Double.parseDouble(amtStr);
        String date = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("category", category);
        cv.put("amount", amt);
        cv.put("notes", notes);
        cv.put("expense_date", date);

        db.insert("expenses", null, cv);
        Toast.makeText(this, "खर्चा दर्ज हो गया", Toast.LENGTH_SHORT).show();
        finish();
    }
}