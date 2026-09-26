package com.expensetracker.app;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.SimpleCursorAdapter;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.List;

public class ReportsActivity extends Activity {

    private Spinner monthSpinner;
    private Spinner yearSpinner;
    private TextView reportTotalIncomeTextView;
    private TextView reportTotalExpensesTextView;
    private TextView reportNetBalanceTextView;
    private ListView transactionsListView;

    private DatabaseHelper dbHelper;
    private SimpleCursorAdapter transactionAdapter;

    private int selectedMonth; // 1-12
    private int selectedYear;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);

        dbHelper = new DatabaseHelper(this);

        monthSpinner = (Spinner) findViewById(R.id.monthSpinner);
        yearSpinner = (Spinner) findViewById(R.id.yearSpinner);
        reportTotalIncomeTextView = (TextView) findViewById(R.id.reportTotalIncomeTextView);
        reportTotalExpensesTextView = (TextView) findViewById(R.id.reportTotalExpensesTextView);
        reportNetBalanceTextView = (TextView) findViewById(R.id.reportNetBalanceTextView);
        transactionsListView = (ListView) findViewById(R.id.transactionsListView);

        setupMonthSpinner();
        setupYearSpinner();

        // Set initial selection to current month/year
        Calendar calendar = Calendar.getInstance();
        selectedMonth = calendar.get(Calendar.MONTH) + 1;
        selectedYear = calendar.get(Calendar.YEAR);

        monthSpinner.setSelection(selectedMonth - 1); // 0-indexed for adapter
        // Assuming current year is in the list, find its position
        ArrayAdapter<Integer> yearAdapter = (ArrayAdapter<Integer>) yearSpinner.getAdapter();
        for (int i = 0; i < yearAdapter.getCount(); i++) {
            if (yearAdapter.getItem(i).intValue() == selectedYear) {
                yearSpinner.setSelection(i);
                break;
            }
        }

        AdapterView.OnItemSelectedListener spinnerListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedMonth = monthSpinner.getSelectedItemPosition() + 1;
                selectedYear = (Integer) yearSpinner.getSelectedItem();
                loadMonthlyReport();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        };

        monthSpinner.setOnItemSelectedListener(spinnerListener);
        yearSpinner.setOnItemSelectedListener(spinnerListener);

        loadMonthlyReport();
    }

    private void setupMonthSpinner() {
        String[] months = new String[12];
        for (int i = 0; i < 12; i++) {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.MONTH, i);
            months[i] = cal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault());
        }
        ArrayAdapter<String> monthAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, months);
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        monthSpinner.setAdapter(monthAdapter);
    }

    private void setupYearSpinner() {
        List<Integer> years = new ArrayList<Integer>();
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        for (int i = currentYear - 5; i <= currentYear + 5; i++) {
            years.add(Integer.valueOf(i));
        }
        ArrayAdapter<Integer> yearAdapter = new ArrayAdapter<Integer>(this,
                android.R.layout.simple_spinner_item, years);
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        yearSpinner.setAdapter(yearAdapter);
    }

    private void loadMonthlyReport() {
        double totalIncome = dbHelper.getMonthlyTotal(selectedYear, selectedMonth, \"income\");
        double totalExpenses = dbHelper.getMonthlyTotal(selectedYear, selectedMonth, \"expense\");
        double netBalance = totalIncome - totalExpenses;

        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance();

        reportTotalIncomeTextView.setText(currencyFormat.format(totalIncome));
        reportTotalExpensesTextView.setText(currencyFormat.format(totalExpenses));
        reportNetBalanceTextView.setText(currencyFormat.format(netBalance));

        // Load transactions for the month
        Cursor transactionsCursor = dbHelper.getAllTransactionsForMonth(selectedYear, selectedMonth);

        String[] fromColumns = {
                DatabaseHelper.COLUMN_TRANSACTION_DATE,
                DatabaseHelper.COLUMN_TRANSACTION_DESCRIPTION,
                DatabaseHelper.COLUMN_TRANSACTION_AMOUNT,
                DatabaseHelper.COLUMN_TRANSACTION_TYPE,
                \"category_name\" // This is aliased in the SQL query
        };
        int[] toViews = {
                android.R.id.text1, // For date and description
                android.R.id.text2  // For amount and type
        };

        // Custom view to display transactions more neatly
        // For simplicity, we'll use a standard layout and combine strings in the adapter
        // A custom CursorAdapter would be ideal here, but SimpleCursorAdapter is restricted
        // to mapping directly to views. Let's create a custom adapter class for this.

        // Since custom CursorAdapter is too complex given the strict rules and time,
        // let's create a simple list of strings for now.
        // Or, more accurately, define a specific layout for transaction items if possible.
        // For this response, I'll use a ListView that shows \"Date - Description: Amount (Category)\"
        // This will require a custom Adapter if I want separate text views, or concat strings.
        // Let's create a simple layout for the list item and use a custom adapter that extends BaseAdapter.
        // However, the rule is \"Use only Android SDK built-in classes\" for SimpleCursorAdapter, it's fine.

        // To display multiple columns in android.R.layout.simple_list_item_2, we can't just map
        // five columns to two TextViews. We need to create a custom layout for each list item.

        // Re-thinking: The prompt limits to `SimpleCursorAdapter` or similar, implies basic mapping.
        // To display details like \"Date | Type | Amount | Description | Category\" in a list view item without a custom adapter that formats text,
        // it means I need to create a layout that has enough TextViews and then map using `SimpleCursorAdapter`.
        // Let's create a new layout `list_item_transaction.xml`

        String[] from = new String[]{
            DatabaseHelper.COLUMN_TRANSACTION_DATE,
            DatabaseHelper.COLUMN_TRANSACTION_DESCRIPTION,
            DatabaseHelper.COLUMN_TRANSACTION_AMOUNT,
            DatabaseHelper.COLUMN_TRANSACTION_TYPE,
            \"category_name\"
        };
        int[] to = new int[]{
            R.id.transactionDate,
            R.id.transactionDescription,
            R.id.transactionAmount,
            R.id.transactionType,
            R.id.transactionCategory
        };

        if (transactionAdapter == null) {
            transactionAdapter = new SimpleCursorAdapter(
                    this,
                    R.layout.list_item_transaction, // Assuming this layout exists
                    transactionsCursor,
                    from,
                    to,
                    0
            );
            transactionsListView.setAdapter(transactionAdapter);
        } else {
            transactionAdapter.changeCursor(transactionsCursor);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (transactionAdapter != null && transactionAdapter.getCursor() != null) {
            transactionAdapter.getCursor().close();
        }
        dbHelper.close();
    }
}