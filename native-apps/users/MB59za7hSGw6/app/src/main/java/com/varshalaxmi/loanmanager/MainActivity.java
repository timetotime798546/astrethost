package com.varshalaxmi.loanmanager;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class MainActivity extends Activity {

    private DatabaseHelper dbHelper;

    private TextView tvTotalCustomers;
    private TextView tvActiveLoans;
    private TextView tvTotalGiven;
    private TextView tvTotalCollected;
    private TextView tvTotalOutstanding;
    private TextView tvTodayCollection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        dbHelper = new DatabaseHelper(this);

        // Check PIN protection security setting
        checkPinProtection();

        setContentView(R.layout.activity_main);

        tvTotalCustomers = (TextView) findViewById(R.id.tv_stat_total_customers);
        tvActiveLoans = (TextView) findViewById(R.id.tv_stat_active_loans);
        tvTotalGiven = (TextView) findViewById(R.id.tv_stat_total_given);
        tvTotalCollected = (TextView) findViewById(R.id.tv_stat_total_collected);
        tvTotalOutstanding = (TextView) findViewById(R.id.tv_stat_total_outstanding);
        tvTodayCollection = (TextView) findViewById(R.id.tv_stat_today_collection);

        // Setup Buttons
        Button btnAddCustomer = (Button) findViewById(R.id.btn_add_customer);
        Button btnNewLoan = (Button) findViewById(R.id.btn_new_loan);
        Button btnCollectPayment = (Button) findViewById(R.id.btn_collect_payment);
        Button btnCustomerList = (Button) findViewById(R.id.btn_customer_list);
        Button btnLoanRecords = (Button) findViewById(R.id.btn_loan_records);
        Button btnDailyCollection = (Button) findViewById(R.id.btn_daily_collection);
        Button btnReports = (Button) findViewById(R.id.btn_reports);
        Button btnBackupRestore = (Button) findViewById(R.id.btn_backup_restore);
        Button btnSettings = (Button) findViewById(R.id.btn_settings);

        btnAddCustomer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, AddCustomerActivity.class));
            }
        });

        btnNewLoan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, NewLoanActivity.class));
            }
        });

        btnCollectPayment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, CollectPaymentActivity.class));
            }
        });

        btnCustomerList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, CustomerListActivity.class));
            }
        });

        btnLoanRecords.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, LoanRecordsActivity.class));
            }
        });

        btnDailyCollection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, PaymentHistoryActivity.class));
            }
        });

        btnReports.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, ReportsActivity.class));
            }
        });

        btnBackupRestore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, BackupRestoreActivity.class));
            }
        });

        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDashboardStats();
    }

    private void loadDashboardStats() {
        int totalCustomers = dbHelper.getTotalCustomerCount();
        int activeLoans = dbHelper.getActiveLoanCount();
        double totalGiven = dbHelper.getTotalLoanGivenAmount();
        double totalCollected = dbHelper.getTotalAmountCollected();
        double totalOutstanding = dbHelper.getTotalOutstandingAmountAll();
        double todayCollection = dbHelper.getTodayCollectionAmount();

        tvTotalCustomers.setText(String.valueOf(totalCustomers));
        tvActiveLoans.setText(String.valueOf(activeLoans));
        tvTotalGiven.setText(String.format(Locale.US, "₹ %,.2f", totalGiven));
        tvTotalCollected.setText(String.format(Locale.US, "₹ %,.2f", totalCollected));
        tvTotalOutstanding.setText(String.format(Locale.US, "₹ %,.2f", totalOutstanding));
        tvTodayCollection.setText(String.format(Locale.US, "₹ %,.2f", todayCollection));
    }

    private void checkPinProtection() {
        boolean isPinEnabled = "true".equalsIgnoreCase(dbHelper.getSetting("pin_enabled", "false"));
        final String savedPin = dbHelper.getSetting("app_pin", "");

        if (isPinEnabled && savedPin.length() > 0) {
            final EditText pinInput = new EditText(this);
            pinInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);

            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle("VarshaLaxmi PIN Lock")
                    .setMessage("Enter Security PIN to unlock:")
                    .setView(pinInput)
                    .setCancelable(false)
                    .setPositiveButton("Unlock", null)
                    .create();

            dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(final DialogInterface dialogInterface) {
                    Button button = ((AlertDialog) dialogInterface).getButton(AlertDialog.BUTTON_POSITIVE);
                    button.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            String entered = pinInput.getText().toString().trim();
                            if (entered.equals(savedPin)) {
                                dialogInterface.dismiss();
                            } else {
                                Toast.makeText(MainActivity.this, "Incorrect PIN!", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            });

            dialog.show();
        }
    }
}