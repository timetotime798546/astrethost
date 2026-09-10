package com.expensetrackerpro.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity {

    private DatabaseHelper dbHelper;
    private List<Transaction> allTransactions = new ArrayList<>();

    // Tab view sections
    private ScrollView containerDashboard;
    private LinearLayout containerTransactions;
    private ScrollView containerReports;

    // Tabs navigation buttons
    private LinearLayout tabDashboard;
    private LinearLayout tabTransactions;
    private LinearLayout tabReports;

    private TextView lblTabDashboard;
    private TextView lblTabTransactions;
    private TextView lblTabReports;

    private View indicatorDashboard;
    private View indicatorTransactions;
    private View indicatorReports;

    // Dashboard data elements
    private TextView txtDashboardBalance;
    private TextView txtDashboardIncome;
    private TextView txtDashboardExpense;
    private LinearLayout recentTransactionsContainer;
    private TextView txtNoRecent;

    // Transaction lists elements
    private ListView listTransactions;
    private TextView txtTransactionsEmpty;
    private Spinner spinnerFilterType;
    private Spinner spinnerFilterCategory;
    private TransactionAdapter transactionAdapter;

    private String filterType = "All";
    private String filterCategory = "All";

    // Reports screen elements
    private TextView txtSelectedMonth;
    private Button btnPrevMonth;
    private Button btnNextMonth;
    private PieChartView pieChartView;
    private LinearLayout categoryBreakdownContainer;

    // Date filters for reporting ( -1, -1 details "All Time" )
    private int selectedMonth = -1;
    private int selectedYear = -1;

    private final String[] MONTH_NAMES = {
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
    };

    private final String[] CATEGORIES_LIST = {
            "Salary", "Food", "Shopping", "Bills", "Entertainment", "Travel", "Others"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);

        // Map layout variables
        containerDashboard = findViewById(R.id.container_dashboard);
        containerTransactions = findViewById(R.id.container_transactions);
        containerReports = findViewById(R.id.container_reports);

        tabDashboard = findViewById(R.id.tab_dashboard);
        tabTransactions = findViewById(R.id.tab_transactions);
        tabReports = findViewById(R.id.tab_reports);

        lblTabDashboard = findViewById(R.id.lbl_tab_dashboard);
        lblTabTransactions = findViewById(R.id.lbl_tab_transactions);
        lblTabReports = findViewById(R.id.lbl_tab_reports);

        indicatorDashboard = findViewById(R.id.indicator_dashboard);
        indicatorTransactions = findViewById(R.id.indicator_transactions);
        indicatorReports = findViewById(R.id.indicator_reports);

        // Dashboard setup variables
        txtDashboardBalance = findViewById(R.id.txt_dashboard_balance);
        txtDashboardIncome = findViewById(R.id.txt_dashboard_income);
        txtDashboardExpense = findViewById(R.id.txt_dashboard_expense);
        recentTransactionsContainer = findViewById(R.id.layout_recent_transactions_container);
        txtNoRecent = findViewById(R.id.txt_no_recent_transactions);

        // Transactions list elements
        listTransactions = findViewById(R.id.list_transactions);
        txtTransactionsEmpty = findViewById(R.id.txt_transactions_empty);
        spinnerFilterType = findViewById(R.id.spinner_filter_type);
        spinnerFilterCategory = findViewById(R.id.spinner_filter_category);

        // Reports components
        txtSelectedMonth = findViewById(R.id.txt_selected_month);
        btnPrevMonth = findViewById(R.id.btn_prev_month);
        btnNextMonth = findViewById(R.id.btn_next_month);
        pieChartView = findViewById(R.id.pie_chart_view);
        categoryBreakdownContainer = findViewById(R.id.layout_category_breakdown_container);

        // Initialize adapters and structures
        transactionAdapter = new TransactionAdapter(new ArrayList<Transaction>());
        listTransactions.setAdapter(transactionAdapter);

        // Primary actions setup
        findViewById(R.id.btn_add_quick).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddTransactionDialog();
            }
        });

        findViewById(R.id.btn_add_transaction_main).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddTransactionDialog();
            }
        });

        findViewById(R.id.btn_view_all_transactions).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectTab(1);
            }
        });

        // Click actions for bottom toggles
        tabDashboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectTab(0);
            }
        });

        tabTransactions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectTab(1);
            }
        });

        tabReports.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectTab(2);
            }
        });

        // Initialize Filter spinners
        initFiltersSpinners();

        // Month selector click events
        btnPrevMonth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedMonth == -1) {
                    Calendar cal = Calendar.getInstance();
                    selectedMonth = cal.get(Calendar.MONTH);
                    selectedYear = cal.get(Calendar.YEAR);
                } else {
                    selectedMonth--;
                    if (selectedMonth < 0) {
                        selectedMonth = 11;
                        selectedYear--;
                    }
                }
                updateReports();
            }
        });

        btnNextMonth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedMonth != -1) {
                    selectedMonth++;
                    if (selectedMonth > 11) {
                        selectedMonth = 0;
                        selectedYear++;
                    }
                    Calendar cal = Calendar.getInstance();
                    if (selectedYear > cal.get(Calendar.YEAR) ||
                            (selectedYear == cal.get(Calendar.YEAR) && selectedMonth > cal.get(Calendar.MONTH))) {
                        selectedMonth = -1;
                        selectedYear = -1;
                    }
                }
                updateReports();
            }
        });

        // Populating dynamic data elements
        refreshData();
        selectTab(0);
    }

    private void initFiltersSpinners() {
        final String[] filterTypes = {"All", "Income", "Expense"};
        ArrayAdapter<String> typeFilterAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, filterTypes
        );
        typeFilterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilterType.setAdapter(typeFilterAdapter);

        final List<String> categoryFilters = new ArrayList<>();
        categoryFilters.add("All");
        for (int i = 0; i < CATEGORIES_LIST.length; i++) {
            categoryFilters.add(CATEGORIES_LIST[i]);
        }
        ArrayAdapter<String> catFilterAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, categoryFilters
        );
        catFilterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilterCategory.setAdapter(catFilterAdapter);

        spinnerFilterType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filterType = filterTypes[position];
                filterTransactions();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerFilterCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filterCategory = categoryFilters.get(position);
                filterTransactions();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void selectTab(int tabIndex) {
        // Clear previous configurations
        lblTabDashboard.setTextColor(0xFF757575);
        lblTabTransactions.setTextColor(0xFF757575);
        lblTabReports.setTextColor(0xFF757575);

        indicatorDashboard.setVisibility(View.INVISIBLE);
        indicatorTransactions.setVisibility(View.INVISIBLE);
        indicatorReports.setVisibility(View.INVISIBLE);

        containerDashboard.setVisibility(View.GONE);
        containerTransactions.setVisibility(View.GONE);
        containerReports.setVisibility(View.GONE);

        if (tabIndex == 0) {
            lblTabDashboard.setTextColor(0xFF2E7D32);
            indicatorDashboard.setVisibility(View.VISIBLE);
            containerDashboard.setVisibility(View.VISIBLE);
        } else if (tabIndex == 1) {
            lblTabTransactions.setTextColor(0xFF2E7D32);
            indicatorTransactions.setVisibility(View.VISIBLE);
            containerTransactions.setVisibility(View.VISIBLE);
            filterTransactions();
        } else if (tabIndex == 2) {
            lblTabReports.setTextColor(0xFF2E7D32);
            indicatorReports.setVisibility(View.VISIBLE);
            containerReports.setVisibility(View.VISIBLE);
            updateReports();
        }
    }

    private void refreshData() {
        allTransactions = dbHelper.getAllTransactions();

        double totalIncome = 0;
        double totalExpense = 0;

        for (int i = 0; i < allTransactions.size(); i++) {
            Transaction t = allTransactions.get(i);
            if ("INCOME".equals(t.getType())) {
                totalIncome += t.getAmount();
            } else {
                totalExpense += t.getAmount();
            }
        }

        double totalBalance = totalIncome - totalExpense;

        txtDashboardBalance.setText(String.format("$%.2f", totalBalance));
        txtDashboardIncome.setText(String.format("$%.2f", totalIncome));
        txtDashboardExpense.setText(String.format("$%.2f", totalExpense));

        if (totalBalance >= 0) {
            txtDashboardBalance.setTextColor(0xFF2E7D32);
        } else {
            txtDashboardBalance.setTextColor(0xFFF44336);
        }

        populateRecentTransactions();
        filterTransactions();
        updateReports();
    }

    private void populateRecentTransactions() {
        recentTransactionsContainer.removeAllViews();
        int count = 0;
        for (int i = 0; i < allTransactions.size() && count < 3; i++) {
            final Transaction transaction = allTransactions.get(i);
            View itemView = getLayoutInflater().inflate(R.layout.item_transaction, recentTransactionsContainer, false);

            View indicator = itemView.findViewById(R.id.view_type_indicator);
            TextView txtCategory = itemView.findViewById(R.id.txt_item_category);
            TextView txtNote = itemView.findViewById(R.id.txt_item_note);
            TextView txtDate = itemView.findViewById(R.id.txt_item_date);
            TextView txtAmount = itemView.findViewById(R.id.txt_item_amount);
            TextView btnDelete = itemView.findViewById(R.id.btn_item_delete);

            txtCategory.setText(transaction.getCategory());
            txtNote.setText(transaction.getNote().trim().isEmpty() ? "No description" : transaction.getNote());
            txtDate.setText(transaction.getDate());

            if ("INCOME".equals(transaction.getType())) {
                indicator.setBackgroundColor(0xFF4CAF50);
                txtAmount.setText(String.format("+$%.2f", transaction.getAmount()));
                txtAmount.setTextColor(0xFF4CAF50);
            } else {
                indicator.setBackgroundColor(0xFFF44336);
                txtAmount.setText(String.format("-$%.2f", transaction.getAmount()));
                txtAmount.setTextColor(0xFFF44336);
            }

            btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showDeleteConfirmDialog(transaction);
                }
            });

            recentTransactionsContainer.addView(itemView);
            count++;
        }

        if (count == 0) {
            txtNoRecent.setVisibility(View.VISIBLE);
        } else {
            txtNoRecent.setVisibility(View.GONE);
        }
    }

    private void filterTransactions() {
        List<Transaction> filtered = new ArrayList<>();
        for (int i = 0; i < allTransactions.size(); i++) {
            Transaction t = allTransactions.get(i);
            boolean matchesType = "All".equals(filterType) || t.getType().equalsIgnoreCase(filterType);
            boolean matchesCategory = "All".equals(filterCategory) || t.getCategory().equalsIgnoreCase(filterCategory);
            if (matchesType && matchesCategory) {
                filtered.add(t);
            }
        }

        transactionAdapter.updateList(filtered);
        if (filtered.isEmpty()) {
            txtTransactionsEmpty.setVisibility(View.VISIBLE);
        } else {
            txtTransactionsEmpty.setVisibility(View.GONE);
        }
    }

    private void updateReports() {
        String prefix = "";
        if (selectedMonth >= 0 && selectedYear > 0) {
            String monthStr = String.format("%02d", selectedMonth + 1);
            prefix = selectedYear + "-" + monthStr;
            txtSelectedMonth.setText(MONTH_NAMES[selectedMonth] + " " + selectedYear);
        } else {
            txtSelectedMonth.setText("All Time");
        }

        Map<String, Float> categoryMap = new HashMap<>();
        float totalPeriodExpenses = 0f;

        for (int i = 0; i < allTransactions.size(); i++) {
            Transaction t = allTransactions.get(i);
            if ("EXPENSE".equals(t.getType())) {
                if (prefix.isEmpty() || t.getDate().startsWith(prefix)) {
                    float val = (float) t.getAmount();
                    totalPeriodExpenses += val;
                    Float current = categoryMap.get(t.getCategory());
                    if (current == null) {
                        categoryMap.put(t.getCategory(), val);
                    } else {
                        categoryMap.put(t.getCategory(), current + val);
                    }
                }
            }
        }

        List<PieChartView.Slice> slices = new ArrayList<>();
        for (Map.Entry<String, Float> entry : categoryMap.entrySet()) {
            slices.add(new PieChartView.Slice(entry.getKey(), entry.getValue(), getCategoryColor(entry.getKey())));
        }

        pieChartView.setSlices(slices);

        // Breakdown listings below charts
        categoryBreakdownContainer.removeAllViews();
        for (Map.Entry<String, Float> entry : categoryMap.entrySet()) {
            String category = entry.getKey();
            float amount = entry.getValue();
            float pct = totalPeriodExpenses > 0 ? (amount / totalPeriodExpenses) * 100 : 0f;

            View rowView = getLayoutInflater().inflate(R.layout.item_category_breakdown, categoryBreakdownContainer, false);
            View colorMarker = rowView.findViewById(R.id.view_cat_color);
            TextView txtName = rowView.findViewById(R.id.txt_cat_name);
            TextView txtAmt = rowView.findViewById(R.id.txt_cat_amount);
            TextView txtPct = rowView.findViewById(R.id.txt_cat_percentage);

            colorMarker.setBackgroundColor(getCategoryColor(category));
            txtName.setText(category);
            txtAmt.setText(String.format("$%.2f", amount));
            txtPct.setText(String.format("%.1f%%", pct));

            categoryBreakdownContainer.addView(rowView);
        }
    }

    private int getCategoryColor(String category) {
        if (category == null) return 0xFF8D6E63;
        switch (category) {
            case "Salary": return 0xFF2E7D32; // Green
            case "Food": return 0xFFFF7043; // Orange/Red
            case "Shopping": return 0xFF29B6F6; // Blue
            case "Bills": return 0xFFFFCA28; // Amber
            case "Entertainment": return 0xFFAB47BC; // Purple
            case "Travel": return 0xFF26A69A; // Teal
            default: return 0xFF8D6E63; // Brown
        }
    }

    private void showAddTransactionDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(MainActivity.this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_transaction, null);
        builder.setView(dialogView);

        final RadioGroup rgType = dialogView.findViewById(R.id.rg_type);
        final EditText etAmount = dialogView.findViewById(R.id.et_amount);
        final Spinner spinnerCategory = dialogView.findViewById(R.id.spinner_category);
        final TextView txtDialogDate = dialogView.findViewById(R.id.txt_dialog_date);
        Button btnChangeDate = dialogView.findViewById(R.id.btn_change_date);
        final EditText etNote = dialogView.findViewById(R.id.et_note);

        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(
                MainActivity.this,
                android.R.layout.simple_spinner_item,
                CATEGORIES_LIST
        );
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(catAdapter);

        final Calendar cal = Calendar.getInstance();
        final String[] currentDateStr = {String.format("%d-%02d-%02d",
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH))};
        txtDialogDate.setText(currentDateStr[0]);

        btnChangeDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int year = cal.get(Calendar.YEAR);
                int month = cal.get(Calendar.MONTH);
                int day = cal.get(Calendar.DAY_OF_MONTH);

                android.app.DatePickerDialog datePickerDialog = new android.app.DatePickerDialog(MainActivity.this,
                        new android.app.DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(android.widget.DatePicker view, int yr, int mo, int dy) {
                                currentDateStr[0] = String.format("%d-%02d-%02d", yr, mo + 1, dy);
                                txtDialogDate.setText(currentDateStr[0]);
                            }
                        }, year, month, day);
                datePickerDialog.show();
            }
        });

        builder.setPositiveButton("Save", null);
        builder.setNegativeButton("Cancel", null);

        final android.app.AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String amountStr = etAmount.getText().toString().trim();
                if (amountStr.isEmpty()) {
                    etAmount.setError("Required field");
                    return;
                }

                double amount;
                try {
                    amount = Double.parseDouble(amountStr);
                } catch (NumberFormatException e) {
                    etAmount.setError("Invalid number format");
                    return;
                }

                if (amount <= 0) {
                    etAmount.setError("Value must be greater than 0");
                    return;
                }

                int checkedId = rgType.getCheckedRadioButtonId();
                String type = (checkedId == R.id.rb_income) ? "INCOME" : "EXPENSE";
                String category = CATEGORIES_LIST[spinnerCategory.getSelectedItemPosition()];
                String note = etNote.getText().toString().trim();
                String dateVal = currentDateStr[0];

                dbHelper.insertTransaction(type, amount, category, dateVal, note);
                Toast.makeText(MainActivity.this, "Transaction logged successfully", Toast.LENGTH_SHORT).show();

                refreshData();
                dialog.dismiss();
            }
        });
    }

    private void showDeleteConfirmDialog(final Transaction transaction) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Delete Transaction");
        builder.setMessage("Are you sure you want to delete this transaction of " + String.format("$%.2f", transaction.getAmount()) + "?");
        builder.setPositiveButton("Delete", new android.content.DialogInterface.OnClickListener() {
            @Override
            public void onClick(android.content.DialogInterface dialog, int which) {
                dbHelper.deleteTransaction(transaction.getId());
                Toast.makeText(MainActivity.this, "Transaction removed", Toast.LENGTH_SHORT).show();
                refreshData();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private class TransactionAdapter extends android.widget.BaseAdapter {
        private List<Transaction> list;

        public TransactionAdapter(List<Transaction> list) {
            this.list = list;
        }

        public void updateList(List<Transaction> list) {
            this.list = list;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() { return list.size(); }

        @Override
        public Object getItem(int position) { return list.get(position); }

        @Override
        public long getItemId(int position) { return list.get(position).getId(); }

        @Override
        public View getView(int position, View convertView, android.view.ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.item_transaction, parent, false);
            }
            final Transaction transaction = list.get(position);

            View indicator = convertView.findViewById(R.id.view_type_indicator);
            TextView txtCategory = convertView.findViewById(R.id.txt_item_category);
            TextView txtNote = convertView.findViewById(R.id.txt_item_note);
            TextView txtDate = convertView.findViewById(R.id.txt_item_date);
            TextView txtAmount = convertView.findViewById(R.id.txt_item_amount);
            TextView btnDelete = convertView.findViewById(R.id.btn_item_delete);

            txtCategory.setText(transaction.getCategory());
            txtNote.setText(transaction.getNote().trim().isEmpty() ? "No description" : transaction.getNote());
            txtDate.setText(transaction.getDate());

            if ("INCOME".equals(transaction.getType())) {
                indicator.setBackgroundColor(0xFF4CAF50);
                txtAmount.setText(String.format("+$%.2f", transaction.getAmount()));
                txtAmount.setTextColor(0xFF4CAF50);
            } else {
                indicator.setBackgroundColor(0xFFF44336);
                txtAmount.setText(String.format("-$%.2f", transaction.getAmount()));
                txtAmount.setTextColor(0xFFF44336);
            }

            btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showDeleteConfirmDialog(transaction);
                }
            });

            return convertView;
        }
    }
}