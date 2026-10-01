package com.dailybizmanager.app;

import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ExpensesActivity extends Activity {

    private DatabaseHelper dbHelper;
    private EditText edtExpenseTitle, edtExpenseCategory, edtExpenseAmount, edtExpenseNotes;
    private LinearLayout layoutExpenseHistory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expenses);

        dbHelper = new DatabaseHelper(this);

        edtExpenseTitle = (EditText) findViewById(R.id.edtExpenseTitle);
        edtExpenseCategory = (EditText) findViewById(R.id.edtExpenseCategory);
        edtExpenseAmount = (EditText) findViewById(R.id.edtExpenseAmount);
        edtExpenseNotes = (EditText) findViewById(R.id.edtExpenseNotes);
        layoutExpenseHistory = (LinearLayout) findViewById(R.id.layoutExpenseHistory);
        Button btnSaveExpense = (Button) findViewById(R.id.btnSaveExpense);

        renderExpensesHistory();

        btnSaveExpense.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveExpenseLog();
            }
        });
    }

    private void saveExpenseLog() {
        String title = edtExpenseTitle.getText().toString().trim();
        String cat = edtExpenseCategory.getText().toString().trim();
        String amtStr = edtExpenseAmount.getText().toString().trim();
        String notes = edtExpenseNotes.getText().toString().trim();

        if (title.isEmpty() || cat.isEmpty() || amtStr.isEmpty()) {
            Toast.makeText(this, "Complete required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = Double.parseDouble(amtStr);
        if (amount < 0) {
            Toast.makeText(this, "Expense amount cannot be negative", Toast.LENGTH_SHORT).show();
            return;
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        ContentValues vals = new ContentValues();
        vals.put("title", title);
        vals.put("category", cat);
        vals.put("amount", amount);
        vals.put("date", today);
        vals.put("notes", notes);

        long res = db.insert("expenses", null, vals);
        if (res != -1) {
            Toast.makeText(this, "Expense logged successfully", Toast.LENGTH_SHORT).show();
            edtExpenseTitle.setText("");
            edtExpenseCategory.setText("");
            edtExpenseAmount.setText("");
            edtExpenseNotes.setText("");
            renderExpensesHistory();
        } else {
            Toast.makeText(this, "Failed to write database logs", Toast.LENGTH_SHORT).show();
        }
    }

    private void renderExpensesHistory() {
        layoutExpenseHistory.removeAllViews();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM expenses ORDER BY id DESC LIMIT 20", null);

        while (cursor.moveToNext()) {
            String title = cursor.getString(cursor.getColumnIndexOrThrow("title"));
            String cat = cursor.getString(cursor.getColumnIndexOrThrow("category"));
            double amount = cursor.getDouble(cursor.getColumnIndexOrThrow("amount"));
            String date = cursor.getString(cursor.getColumnIndexOrThrow("date"));

            TextView row = new TextView(this);
            row.setText(date + ": " + title + " [" + cat + "] @ $" + String.format("%.2f", amount));
            row.setPadding(8, 8, 8, 8);
            row.setBackgroundColor(Color.WHITE);
            row.setTextSize(13);

            View divider = new View(this);
            divider.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1));
            divider.setBackgroundColor(Color.LTGRAY);

            layoutExpenseHistory.addView(row);
            layoutExpenseHistory.addView(divider);
        }
        cursor.close();
    }
}