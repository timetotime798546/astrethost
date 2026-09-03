package com.expensetracker.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {

    // Database
    private DatabaseHelper dbHelper;

    // View Switching Layouts
    private LinearLayout layoutDashboard;
    private View layoutAddTransaction;
    private View layoutReports;

    // Tabs
    private Button btnTabDashboard;
    private Button btnTabAdd;
    private Button btnTabReports;

    // Dashboard Views
    private TextView txtTotalBalance;
    private TextView txtTotalIncome;
    private TextView txtTotalExpense;
    private ListView listTransactions;
    private TextView txtEmptyState;

    // Add Transaction Views
    private RadioGroup rgType;
    private RadioButton rbExpense;
    private RadioButton rbIncome;
    private EditText edtTitle;
    private EditText edtAmount;
    private Spinner spnCategory;
    private TextView txtDateSelected;
    private Button btnChangeDate;
    private Button btnSaveTransaction;

    // Reports Views
    private Spinner spnReportMonth;
    private TextView txtReportSavings;
    private TextView txtReportSavingsRate;
    private LinearLayout layoutCategoryBars;
    private TextView txtNoReportData;

    // Header buttons
    private TextView btnActionClear;

    // Spinners Lists
    private String[] expenseCategories = {"Food", "Transport", "Shopping", "Rent & Bills", "Entertainment", "Health", "Utilities", "Other"};
    private String[] incomeCategories = {"Salary", "Business", "Investment", "Gifts", "Other"};

    // Current Date Selection Tracker
    private int mYear, mMonth, mDay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);

        // Bind layouts
        layoutDashboard = (LinearLayout) findViewById(R.id.layout_dashboard);
        layoutAddTransaction = findViewById(R.id.layout_add_transaction);
        layoutReports = findViewById(R.id.layout_reports);

        // Bind Tabs
        btnTabDashboard = (Button) findViewById(R.id.btn_tab_dashboard);
        btnTabAdd = (Button) findViewById(R.id.btn_tab_add);
        btnTabReports = (Button) findViewById(R.id.btn_tab_reports);

        // Bind Dashboard elements
        txtTotalBalance = (TextView) findViewById(R.id.txt_total_balance);
        txtTotalIncome = (TextView) findViewById(R.id.txt_total_income);
        txtTotalExpense = (TextView) findViewById(R.id.txt_total_expense);
        listTransactions = (ListView) findViewById(R.id.list_transactions);
        txtEmptyState = (TextView) findViewById(R.id.txt_empty_state);

        // Bind Add Transaction elements
        rgType = (RadioGroup) findViewById(R.id.rg_type);
        rbExpense = (RadioButton) findViewById(R.id.rb_expense);
        rbIncome = (RadioButton) findViewById(R.id.rb_income);
        edtTitle = (EditText) findViewById(R.id.edt_title);
        edtAmount = (EditText) findViewById(R.id.edt_amount);
        spnCategory = (Spinner) findViewById(R.id.spn_category);
        txtDateSelected = (TextView) findViewById(R.id.txt_date_selected);
        btnChangeDate = (Button) findViewById(R.id.btn_change_date);
        btnSaveTransaction = (Button) findViewById(R.id.btn_save_transaction);

        // Bind Reports elements
        spnReportMonth = (Spinner) findViewById(R.id.spn_report_month);
        txtReportSavings = (TextView) findViewById(R.id.txt_report_savings);
        txtReportSavingsRate = (TextView) findViewById(R.id.txt_report_savings_rate);
        layoutCategoryBars = (LinearLayout) findViewById(R.id.layout_category_bars);
        txtNoReportData = (TextView) findViewById(R.id.txt_no_report_data);

        // Clear button
        btnActionClear = (TextView) findViewById(R.id.btn_action_clear);

        // Initialize calendar variables
        Calendar c = Calendar.getInstance();
        mYear = c.get(Calendar.YEAR);
        mMonth = c.get(Calendar.MONTH);
        mDay = c.get(Calendar.DAY_OF_MONTH);
        updateDateDisplay();

        // Init Category Spinner
        setSpinnerCategories(true); // Default starting radio selection is Expense

        // Bind tab actions
        btnTabDashboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(1);
            }
        });

        btnTabAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(2);
            }
        });

        btnTabReports.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(3);
            }
        });

        // Add form Radio changes
        rgType.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.rb_expense) {
                    setSpinnerCategories(true);
                } else {
                    setSpinnerCategories(false);
                }
            }
        });

        // Date select button click listener
        btnChangeDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatePickerDialog datePickerDialog = new DatePickerDialog(MainActivity.this,
                        new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                                mYear = year;
                                mMonth = monthOfYear;
                                mDay = dayOfMonth;
                                updateDateDisplay();
                            }
                        }, mYear, mMonth, mDay);
                datePickerDialog.show();
            }
        });

        // Save transaction record listener
        btnSaveTransaction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveTransactionForm();
            }
        });

        // Clear app database listener
        btnActionClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmResetData();
            }
        });

        // Hold transaction item in ListView to initiate delete action
        listTransactions.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Transaction transaction = (Transaction) parent.getItemAtPosition(position);
                promptDeleteTransaction(transaction);
                return true;
            }
        });

        // Load initially dashboard calculations & rows
        reloadDashboard();
    }

    private void switchTab(int tabIndex) {
        // Reset navigation buttons colors
        btnTabDashboard.setBackgroundResource(R.drawable.tab_unselected_bg);
        btnTabDashboard.setTextColor(Color.parseColor("#4A5568"));
        btnTabAdd.setBackgroundResource(R.drawable.tab_unselected_bg);
        btnTabAdd.setTextColor(Color.parseColor("#4A5568"));
        btnTabReports.setBackgroundResource(R.drawable.tab_unselected_bg);
        btnTabReports.setTextColor(Color.parseColor("#4A5568"));

        // Hide screen views
        layoutDashboard.setVisibility(View.GONE);
        layoutAddTransaction.setVisibility(View.GONE);
        layoutReports.setVisibility(View.GONE);

        if (tabIndex == 1) {
            btnTabDashboard.setBackgroundResource(R.drawable.tab_selected_bg);
            btnTabDashboard.setTextColor(Color.parseColor("#2B6CB0"));
            layoutDashboard.setVisibility(View.VISIBLE);
            reloadDashboard();
        } else if (tabIndex == 2) {
            btnTabAdd.setBackgroundResource(R.drawable.tab_selected_bg);
            btnTabAdd.setTextColor(Color.parseColor("#2B6CB0"));
            layoutAddTransaction.setVisibility(View.VISIBLE);
        } else if (tabIndex == 3) {
            btnTabReports.setBackgroundResource(R.drawable.tab_selected_bg);
            btnTabReports.setTextColor(Color.parseColor("#2B6CB0"));
            layoutReports.setVisibility(View.VISIBLE);
            reloadReportsTab();
        }
    }

    private void setSpinnerCategories(boolean isExpense) {
        String[] categories = isExpense ? expenseCategories : incomeCategories;
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnCategory.setAdapter(adapter);
    }

    private void updateDateDisplay() {
        String formattedDate = String.format(Locale.US, "%d-%02d-%02d", mYear, (mMonth + 1), mDay);
        txtDateSelected.setText(formattedDate);
    }

    private void saveTransactionForm() {
        String title = edtTitle.getText().toString().trim();
        String amountStr = edtAmount.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(this, "Please enter a valid description.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Please enter a transaction amount.", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter a valid numerical value.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (amount <= 0.0) {
            Toast.makeText(this, "Amount should be greater than zero.", Toast.LENGTH_SHORT).show();
            return;
        }

        String type = (rgType.getCheckedRadioButtonId() == R.id.rb_expense) ? "EXPENSE" : "INCOME";
        String category = spnCategory.getSelectedItem().toString();
        String date = txtDateSelected.getText().toString();

        boolean success = dbHelper.addTransaction(title, amount, type, category, date);
        if (success) {
            Toast.makeText(this, "Transaction saved successfully!", Toast.LENGTH_SHORT).show();
            // Reset input values
            edtTitle.setText("");
            edtAmount.setText("");
            // Return back to dashboard view
            switchTab(1);
        } else {
            Toast.makeText(this, "Failed to write database record. Try again.", Toast.LENGTH_SHORT).show();
        }
    }

    private void reloadDashboard() {
        // Fetch values
        double totalInc = dbHelper.getTotalIncome();
        double totalExp = dbHelper.getTotalExpense();
        double netBalance = totalInc - totalExp;

        // Populate Dashboard balance banner fields
        txtTotalBalance.setText(String.format(Locale.US, "$%,.2f", netBalance));
        txtTotalIncome.setText(String.format(Locale.US, "+$%,.2f", totalInc));
        txtTotalExpense.setText(String.format(Locale.US, "-$%,.2f", totalExp));

        // Format warning indicators if balance goes negative
        if (netBalance < 0.0) {
            txtTotalBalance.setTextColor(Color.parseColor("#FEB2B2")); // soft coral red warning color
        } else {
            txtTotalBalance.setTextColor(Color.colorToHSV(new float[]{0f, 0f, 1f}) == 0 ? Color.WHITE : Color.WHITE);
        }

        // List View Records populate
        List<Transaction> recordsList = dbHelper.getAllTransactions();
        if (recordsList.isEmpty()) {
            txtEmptyState.setVisibility(View.VISIBLE);
            listTransactions.setVisibility(View.GONE);
        } else {
            txtEmptyState.setVisibility(View.GONE);
            listTransactions.setVisibility(View.VISIBLE);
            TransactionAdapter adapter = new TransactionAdapter(this, recordsList);
            listTransactions.setAdapter(adapter);
        }
    }

    private void reloadReportsTab() {
        // Populate available months spinner dynamically
        final List<String> availableMonths = dbHelper.getAvailableMonths();

        if (availableMonths.isEmpty()) {
            // Display empty layout indicators inside monthly reports tab
            spnReportMonth.setEnabled(false);
            ArrayAdapter<String> emptyMonthAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new String[]{"No Months Recorded"});
            emptyMonthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spnReportMonth.setAdapter(emptyMonthAdapter);
            txtReportSavings.setText("$0.00");
            txtReportSavingsRate.setText("0%");
            layoutCategoryBars.removeAllViews();
            txtNoReportData.setVisibility(View.VISIBLE);
            return;
        }

        spnReportMonth.setEnabled(true);
        ArrayAdapter<String> monthAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, availableMonths);
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnReportMonth.setAdapter(monthAdapter);

        spnReportMonth.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedMonth = availableMonths.get(position);
                renderMonthDetails(selectedMonth);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Trigger immediate draw for the first element
        renderMonthDetails(availableMonths.get(0));
    }

    private void renderMonthDetails(String yearMonth) {
        List<Transaction> all = dbHelper.getAllTransactions();
        double incTotal = 0.0;
        double expTotal = 0.0;
        Map<String, Double> categoryTotals = new HashMap<>();

        // Process data records filtered by select month YYYY-MM prefix
        for (int i = 0; i < all.size(); i++) {
            Transaction t = all.get(i);
            if (t.getDate().startsWith(yearMonth)) {
                if (t.getType().equals("INCOME")) {
                    incTotal += t.getAmount();
                } else if (t.getType().equals("EXPENSE")) {
                    expTotal += t.getAmount();
                    double prev = categoryTotals.containsKey(t.getCategory()) ? categoryTotals.get(t.getCategory()) : 0.0;
                    categoryTotals.put(t.getCategory(), prev + t.getAmount());
                }
            }
        }

        // Output saving calculations
        double savings = incTotal - expTotal;
        txtReportSavings.setText(String.format(Locale.US, "$%,.2f", savings));
        if (savings >= 0) {
            txtReportSavings.setTextColor(Color.parseColor("#2F855A"));
        } else {
            txtReportSavings.setTextColor(Color.parseColor("#C53030"));
        }

        double ratePercent = 0.0;
        if (incTotal > 0.0) {
            ratePercent = (savings / incTotal) * 100.0;
        } else if (savings < 0.0) {
            ratePercent = -100.0; // Negatively overdrawn
        }

        txtReportSavingsRate.setText(String.format(Locale.US, "%.1f%%", ratePercent));
        if (ratePercent >= 20.0) {
            txtReportSavingsRate.setTextColor(Color.parseColor("#319795")); // excellent save rating
        } else if (ratePercent >= 0.0) {
            txtReportSavingsRate.setTextColor(Color.parseColor("#4A5568")); // average save rating
        } else {
            txtReportSavingsRate.setTextColor(Color.parseColor("#E53E3E")); // overspending warning
        }

        // Draw horizontal visual bar chart programmatically inside layoutCategoryBars
        layoutCategoryBars.removeAllViews();

        if (categoryTotals.isEmpty()) {
            txtNoReportData.setVisibility(View.VISIBLE);
        } else {
            txtNoReportData.setVisibility(View.GONE);

            // Find maximum category amount to scale visual layout bars correctly
            double maxExpAmount = 0.0;
            for (Map.Entry<String, Double> entry : categoryTotals.entrySet()) {
                if (entry.getValue() > maxExpAmount) {
                    maxExpAmount = entry.getValue();
                }
            }

            // Create linear containers for each categories details programmatically
            for (Map.Entry<String, Double> entry : categoryTotals.entrySet()) {
                String catName = entry.getKey();
                double catVal = entry.getValue();

                // Compute scaling percentage relative to max entry
                int percentage = (int) ((catVal / maxExpAmount) * 100.0);
                if (percentage < 3) percentage = 3; // Ensure thin bar visibility is represented

                // Outer box
                LinearLayout rowContainer = new LinearLayout(this);
                rowContainer.setOrientation(LinearLayout.VERTICAL);
                rowContainer.setPadding(0, 8, 0, 12);

                // Headline text values row layout
                RelativeLayout txtRow = new RelativeLayout(this);
                txtRow.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

                TextView labelName = new TextView(this);
                labelName.setText(catName);
                labelName.setTextColor(Color.parseColor("#2D3748"));
                labelName.setTypeface(null, Typeface.BOLD);
                labelName.setTextSize(13sp);

                TextView labelVal = new TextView(this);
                labelVal.setText(String.format(Locale.US, "$%,.2f", catVal));
                labelVal.setTextColor(Color.parseColor("#4A5568"));
                labelVal.setTextSize(13sp);
                labelVal.setTypeface(null, Typeface.BOLD);

                // Positioning layout rules
                RelativeLayout.LayoutParams leftParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                leftParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                labelName.setLayoutParams(leftParams);

                RelativeLayout.LayoutParams rightParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                rightParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                labelVal.setLayoutParams(rightParams);

                txtRow.addView(labelName);
                txtRow.addView(labelVal);

                // Horizontal visual tracking bar element
                View bar = new View(this);
                LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(0, dpToPx(8));
                barParams.weight = percentage;
                barParams.topMargin = dpToPx(6);
                bar.setLayoutParams(barParams);
                bar.setBackgroundColor(Color.parseColor("#E53E3E")); // bright coral-red category indicators

                // Unfilled track backdrop
                LinearLayout barWrapper = new LinearLayout(this);
                barWrapper.setOrientation(LinearLayout.HORIZONTAL);
                barWrapper.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                barWrapper.setBackgroundColor(Color.parseColor("#EDF2F7")); // soft grey empty backdrop bar track
                barWrapper.addView(bar);

                // Wrap up dynamic items
                rowContainer.addView(txtRow);
                rowContainer.addView(barWrapper);

                layoutCategoryBars.addView(rowContainer);
            }
        }
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }

    private void promptDeleteTransaction(final Transaction transaction) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Transaction");
        builder.setMessage("Are you sure you want to delete '" + transaction.getTitle() + "' ($" + transaction.getAmount() + ")?");
        builder.setPositiveButton("Yes, Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                boolean status = dbHelper.deleteTransaction(transaction.getId());
                if (status) {
                    Toast.makeText(MainActivity.this, "Transaction deleted successfully.", Toast.LENGTH_SHORT).show();
                    reloadDashboard();
                } else {
                    Toast.makeText(MainActivity.this, "Deletion failed. Try again.", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void confirmResetData() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Reset App Ledger");
        builder.setMessage("This will clear ALL stored transaction records. Are you absolutely sure?");
        builder.setPositiveButton("Reset All", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dbHelper.clearAllData();
                Toast.makeText(MainActivity.this, "All records have been cleared.", Toast.LENGTH_SHORT).show();
                reloadDashboard();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    // Custom Row ListAdapter for high fidelity list items
    private static class TransactionAdapter extends BaseAdapter {
        private Context context;
        private List<Transaction> list;

        public TransactionAdapter(Context context, List<Transaction> list) {
            this.context = context;
            this.list = list;
        }

        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public Object getItem(int position) {
            return list.get(position);
        }

        @Override
        public long getItemId(int position) {
            return list.get(position).getId();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_2, parent, false);
            }

            Transaction currentItem = list.get(position);

            TextView titleText = (TextView) convertView.findViewById(android.R.id.text1);
            TextView subText = (TextView) convertView.findViewById(android.R.id.text2);

            titleText.setText(currentItem.getTitle());
            titleText.setTypeface(null, Typeface.BOLD);
            titleText.setTextColor(Color.parseColor("#2D3748"));
            titleText.setTextSize(15sp);

            String tagSymbol = currentItem.getType().equals("INCOME") ? "+" : "-";
            String amountFormatted = String.format(Locale.US, "%s$%,.2f", tagSymbol, currentItem.getAmount());

            subText.setText(currentItem.getDate() + " • " + currentItem.getCategory() + "   (" + amountFormatted + ")");
            subText.setTextSize(12sp);

            if (currentItem.getType().equals("INCOME")) {
                subText.setTextColor(Color.parseColor("#38A169")); // subtle forest green for positive entries
            } else {
                subText.setTextColor(Color.parseColor("#E53E3E")); // warm warning red for expenses
            }

            return convertView;
        }
    }
}