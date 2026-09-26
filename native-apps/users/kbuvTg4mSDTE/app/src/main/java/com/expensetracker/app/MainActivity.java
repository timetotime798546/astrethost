package com.expensetracker.app;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends Activity {

    private TextView currentMonthTextView;
    private TextView totalIncomeTextView;
    private TextView totalExpensesTextView;
    private TextView netBalanceTextView;
    private Button addIncomeButton;
    private Button addExpenseButton;
    private Button manageCategoriesButton;
    private Button viewReportsButton;

    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);

        currentMonthTextView = (TextView) findViewById(R.id.currentMonthTextView);
        totalIncomeTextView = (TextView) findViewById(R.id.totalIncomeTextView);
        totalExpensesTextView = (TextView) findViewById(R.id.totalExpensesTextView);
        netBalanceTextView = (TextView) findViewById(R.id.netBalanceTextView);
        addIncomeButton = (Button) findViewById(R.id.addIncomeButton);
        addExpenseButton = (Button) findViewById(R.id.addExpenseButton);
        manageCategoriesButton = (Button) findViewById(R.id.manageCategoriesButton);
        viewReportsButton = (Button) findViewById(R.id.viewReportsButton);

        addIncomeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, AddTransactionActivity.class);
                intent.putExtra(\"transaction_type\", \"income\");
                startActivity(intent);
            }
        });

        addExpenseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, AddTransactionActivity.class);
                intent.putExtra(\"transaction_type\", \"expense\");
                startActivity(intent);
            }
        });

        manageCategoriesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ManageCategoriesActivity.class);
                startActivity(intent);
            }
        });

        viewReportsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ReportsActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadMonthlySummary();
    }

    private void loadMonthlySummary() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1; // Month is 0-indexed

        String monthName = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault());
        currentMonthTextView.setText(monthName + \" \" + year);

        double totalIncome = dbHelper.getMonthlyTotal(year, month, \"income\");
        double totalExpenses = dbHelper.getMonthlyTotal(year, month, \"expense\");
        double netBalance = totalIncome - totalExpenses;

        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance();

        totalIncomeTextView.setText(currencyFormat.format(totalIncome));
        totalExpensesTextView.setText(currencyFormat.format(totalExpenses));
        netBalanceTextView.setText(currencyFormat.format(netBalance));
    }
}