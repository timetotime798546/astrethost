package com.expensetrackerpro.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private DatabaseHelper dbHelper;
    private SoundHelper soundHelper;
    
    // Loader Overlay
    private LinearLayout layoutLoader;
    private ProgressBar loaderProgressBar;
    private TextView txtLoaderPercentage;
    private TextView txtLoaderMessage;
    
    // Dashboard UI
    private TextView txtDashIncome, txtDashExpense, txtDashBalance;
    private ListView listTransactions;
    private TextView txtNoTransactions;
    private Button btnTabTracker, btnTabReports;
    private Button fabAdd;
    private GravityView gravityView;
    
    // Dynamic Screens containers
    private LinearLayout viewTracker, viewReports;
    
    // Reports Screen
    private Spinner spinnerReportMonth;
    private TextView txtReportIncome, txtReportExpense;
    private ListView listReports;
    private TextView txtNoReports;

    private final String[] incomeCategories = {"Salary", "Investments", "Business", "Gifts", "Freelance", "Other"};
    private final String[] expenseCategories = {"Food & Dining", "Rent & Utilities", "Transport", "Shopping", "Entertainment", "Healthcare", "Education", "Travel", "Miscellaneous"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);
        soundHelper = new SoundHelper();
        
        seedSampleData();

        initViews();
        setupListeners();
        
        runLaunchSequence();
    }

    private void initViews() {
        layoutLoader = (LinearLayout) findViewById(R.id.layout_loader);
        loaderProgressBar = (ProgressBar) findViewById(R.id.loader_progress_bar);
        txtLoaderPercentage = (TextView) findViewById(R.id.txt_loader_percentage);
        txtLoaderMessage = (TextView) findViewById(R.id.txt_loader_message);

        btnTabTracker = (Button) findViewById(R.id.btn_tab_tracker);
        btnTabReports = (Button) findViewById(R.id.btn_tab_reports);
        
        viewTracker = (LinearLayout) findViewById(R.id.view_tracker);
        viewReports = (LinearLayout) findViewById(R.id.view_reports);
        
        txtDashIncome = (TextView) findViewById(R.id.txt_dashboard_income);
        txtDashExpense = (TextView) findViewById(R.id.txt_dashboard_expense);
        txtDashBalance = (TextView) findViewById(R.id.txt_dashboard_balance);
        listTransactions = (ListView) findViewById(R.id.list_transactions);
        txtNoTransactions = (TextView) findViewById(R.id.txt_no_transactions);
        fabAdd = (Button) findViewById(R.id.fab_add);
        gravityView = (GravityView) findViewById(R.id.gravity_view);
        gravityView.setSoundHelper(soundHelper);
        
        spinnerReportMonth = (Spinner) findViewById(R.id.spinner_report_month);
        txtReportIncome = (TextView) findViewById(R.id.txt_report_income);
        txtReportExpense = (TextView) findViewById(R.id.txt_report_expense);
        listReports = (ListView) findViewById(R.id.list_reports);
        txtNoReports = (TextView) findViewById(R.id.txt_no_reports);
    }

    private void runLaunchSequence() {
        layoutLoader.setVisibility(View.VISIBLE);
        loaderProgressBar.setProgress(0);
        txtLoaderPercentage.setText("0%");
        soundHelper.playLaunchTone();

        final Handler handler = new Handler(Looper.getMainLooper());
        final int totalSteps = 100;
        
        handler.post(new Runnable() {
            int currentProgress = 0;
            @Override
            public void run() {
                if (currentProgress <= totalSteps) {
                    loaderProgressBar.setProgress(currentProgress);
                    txtLoaderPercentage.setText(currentProgress + "%");

                    if (currentProgress < 20) {
                        txtLoaderMessage.setText("Loading system engine files...");
                    } else if (currentProgress < 40) {
                        txtLoaderMessage.setText("Assembling 3D dimensional gravity space...");
                    } else if (currentProgress < 60) {
                        txtLoaderMessage.setText("Preparing synthetic sound wave filters...");
                    } else if (currentProgress < 85) {
                        txtLoaderMessage.setText("Compiling physical mass parameters...");
                    } else {
                        txtLoaderMessage.setText("Establishing budget matrix parameters...");
                    }

                    currentProgress += 2;
                    handler.postDelayed(this, 30);
                } else {
                    layoutLoader.setVisibility(View.GONE);
                    soundHelper.playSuccessSound();
                    refreshDashboard();
                }
            }
        });
    }

    private void setupListeners() {
        btnTabTracker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchView(true);
            }
        });

        btnTabReports.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchView(false);
            }
        });

        fabAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddTransactionDialog();
            }
        });

        listTransactions.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                final Transaction selectedTx = (Transaction) listTransactions.getAdapter().getItem(position);
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Delete Transaction")
                        .setMessage("Are you sure you want to permanently delete this transaction?")
                        .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                runLoaderAction("Deleting record...", new Runnable() {
                                    @Override
                                    public void run() {
                                        if (dbHelper.deleteTransaction(selectedTx.getId())) {
                                            soundHelper.playDeleteSound();
                                            Toast.makeText(MainActivity.this, "Transaction deleted successfully.", Toast.LENGTH_SHORT).show();
                                            refreshDashboard();
                                            refreshReports();
                                        }
                                    }
                                });
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                return true;
            }
        });
    }

    private void runLoaderAction(String statusMessage, final Runnable action) {
        layoutLoader.setVisibility(View.VISIBLE);
        loaderProgressBar.setProgress(50);
        txtLoaderPercentage.setText("In progress");
        txtLoaderMessage.setText(statusMessage);
        
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                action.run();
                layoutLoader.setVisibility(View.GONE);
            }
        }, 750);
    }

    private void switchView(final boolean isTracker) {
        runLoaderAction(isTracker ? "Generating Dashboard..." : "Analyzing Categories...", new Runnable() {
            @Override
            public void run() {
                soundHelper.playClickSound();
                if (isTracker) {
                    viewTracker.setVisibility(View.VISIBLE);
                    viewReports.setVisibility(View.GONE);
                    btnTabTracker.setBackgroundResource(R.drawable.card_bg_blue);
                    btnTabReports.setBackgroundColor(Color.TRANSPARENT);
                    btnTabTracker.setTextColor(Color.parseColor("#FFFFFF"));
                    btnTabReports.setTextColor(Color.parseColor("#B3FFFFFF"));
                    refreshDashboard();
                } else {
                    viewTracker.setVisibility(View.GONE);
                    viewReports.setVisibility(View.VISIBLE);
                    btnTabTracker.setBackgroundColor(Color.TRANSPARENT);
                    btnTabReports.setBackgroundResource(R.drawable.card_bg_blue);
                    btnTabTracker.setTextColor(Color.parseColor("#B3FFFFFF"));
                    btnTabReports.setTextColor(Color.parseColor("#FFFFFF"));
                    setupReportFilterSpinner();
                    refreshReports();
                }
            }
        });
    }

    private void refreshDashboard() {
        double totalIncome = dbHelper.getTotalAmountByType("INCOME", "All Months");
        double totalExpense = dbHelper.getTotalAmountByType("EXPENSE", "All Months");
        double balance = totalIncome - totalExpense;

        txtDashIncome.setText(String.format("$%.2f", totalIncome));
        txtDashExpense.setText(String.format("$%.2f", totalExpense));
        txtDashBalance.setText(String.format("$%.2f", balance));

        List<Transaction> txList = dbHelper.getAllTransactions();
        if (txList.isEmpty()) {
            txtNoTransactions.setVisibility(View.VISIBLE);
            listTransactions.setVisibility(View.GONE);
        } else {
            txtNoTransactions.setVisibility(View.GONE);
            listTransactions.setVisibility(View.VISIBLE);
            TransactionAdapter adapter = new TransactionAdapter(this, txList);
            listTransactions.setAdapter(adapter);
        }

        gravityView.setupTransactions(txList);
    }

    private void setupReportFilterSpinner() {
        List<String> months = dbHelper.getAvailableMonths();
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, months);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerReportMonth.setAdapter(spinnerAdapter);

        spinnerReportMonth.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                refreshReports();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void refreshReports() {
        String selectedMonth = "All Months";
        if (spinnerReportMonth.getSelectedItem() != null) {
            selectedMonth = spinnerReportMonth.getSelectedItem().toString();
        }

        double totalIncome = dbHelper.getTotalAmountByType("INCOME", selectedMonth);
        double totalExpense = dbHelper.getTotalAmountByType("EXPENSE", selectedMonth);

        txtReportIncome.setText(String.format("$%.2f", totalIncome));
        txtReportExpense.setText(String.format("$%.2f", totalExpense));

        List<CategoryReport> reportList = dbHelper.getCategoryReport(selectedMonth);
        if (reportList.isEmpty()) {
            txtNoReports.setVisibility(View.VISIBLE);
            listReports.setVisibility(View.GONE);
        } else {
            txtNoReports.setVisibility(View.GONE);
            listReports.setVisibility(View.VISIBLE);
            CategoryReportAdapter adapter = new CategoryReportAdapter(this, reportList);
            listReports.setAdapter(adapter);
        }
    }

    private void showAddTransactionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Transaction");

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_transaction, null);
        builder.setView(dialogView);

        final RadioGroup radioGroupType = (RadioGroup) dialogView.findViewById(R.id.radio_group_type);
        final EditText editAmount = (EditText) dialogView.findViewById(R.id.edit_amount);
        final Spinner spinnerCategory = (Spinner) dialogView.findViewById(R.id.spinner_category);
        final EditText editDate = (EditText) dialogView.findViewById(R.id.edit_date);
        final EditText editNote = (EditText) dialogView.findViewById(R.id.edit_note);

        final Calendar calendar = Calendar.getInstance();
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        editDate.setText(sdf.format(calendar.getTime()));

        editDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new DatePickerDialog(MainActivity.this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        calendar.set(Calendar.YEAR, year);
                        calendar.set(Calendar.MONTH, month);
                        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        editDate.setText(sdf.format(calendar.getTime()));
                    }
                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
            }
        });

        final ArrayAdapter<String> expenseAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, expenseCategories);
        expenseAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        final ArrayAdapter<String> incomeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, incomeCategories);
        incomeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinnerCategory.setAdapter(expenseAdapter);

        radioGroupType.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.radio_income) {
                    spinnerCategory.setAdapter(incomeAdapter);
                } else {
                    spinnerCategory.setAdapter(expenseAdapter);
                }
            } 
        });

        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {}
        });
        builder.setNegativeButton("Cancel", null);

        final AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String amountStr = editAmount.getText().toString().trim();
                final String dateStr = editDate.getText().toString().trim();
                final String noteStr = editNote.getText().toString().trim();
                final String categoryStr = spinnerCategory.getSelectedItem().toString();
                
                int checkedId = radioGroupType.getCheckedRadioButtonId();
                final String typeStr = (checkedId == R.id.radio_income) ? "INCOME" : "EXPENSE";

                if (amountStr.isEmpty()) {
                    editAmount.setError("Please input amount value");
                    return;
                }

                final double amount;
                try {
                    amount = Double.parseDouble(amountStr);
                    if (amount <= 0) {
                        editAmount.setError("Amount must be strictly greater than 0");
                        return;
                    }
                } catch (NumberFormatException e) {
                    editAmount.setError("Invalid number amount formatting");
                    return;
                }

                dialog.dismiss();

                runLoaderAction("Archiving transaction entry...", new Runnable() {
                    @Override
                    public void run() {
                        boolean success = dbHelper.insertTransaction(typeStr, amount, categoryStr, dateStr, noteStr);
                        if (success) {
                            soundHelper.playSuccessSound();
                            Toast.makeText(MainActivity.this, "Record saved successfully.", Toast.LENGTH_SHORT).show();
                            if (viewTracker.getVisibility() == View.VISIBLE) {
                                refreshDashboard();
                            } else {
                                setupReportFilterSpinner();
                                refreshReports();
                            }
                        } else {
                            Toast.makeText(MainActivity.this, "Database Insertion Error.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            } 
        });
    }

    private void seedSampleData() {
        List<Transaction> currentList = dbHelper.getAllTransactions();
        if (currentList.isEmpty()) {
            Calendar cal = Calendar.getInstance();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            
            cal.add(Calendar.DAY_OF_MONTH, -1);
            String yesterday = sdf.format(cal.getTime());
            
            cal.add(Calendar.DAY_OF_MONTH, -2);
            String daysAgo3 = sdf.format(cal.getTime());
            
            String today = sdf.format(new Date());

            dbHelper.insertTransaction("INCOME", 4500.00, "Salary", daysAgo3, "Monthly base income payoff");
            dbHelper.insertTransaction("EXPENSE", 1200.00, "Rent & Utilities", daysAgo3, "Apartment rent charge");
            dbHelper.insertTransaction("EXPENSE", 64.50, "Food & Dining", yesterday, "Local Diner lunch");
            dbHelper.insertTransaction("EXPENSE", 150.00, "Transport", yesterday, "Gas and highway toll refills");
            dbHelper.insertTransaction("INCOME", 250.00, "Freelance", today, "Logo project delivery");
            dbHelper.insertTransaction("EXPENSE", 89.99, "Shopping", today, "Electronic desk accessories");
        }
    }

    @Override
    protected void onDestroy() {
        if (soundHelper != null) {
            soundHelper.release();
        }
        super.onDestroy();
    }
}