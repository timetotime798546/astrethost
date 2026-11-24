package com.varshalaxmi.loanmanager;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NewLoanActivity extends Activity {

    private DatabaseHelper dbHelper;
    private Spinner spCustomer, spExistingLoans, spInterestMethod;
    private RadioGroup rgMode;
    private RadioButton rbModeNew, rbModeAddition;
    private LinearLayout layoutExistingLoans, layoutNewLoanSettings;

    private EditText etAmount, etLoanDate, etInterestRate, etDueDate, etNotes;

    private List<Long> customerIdList = new ArrayList<>();
    private List<String> customerNameList = new ArrayList<>();

    private List<Long> existingLoanIdList = new ArrayList<>();
    private List<String> existingLoanDescList = new ArrayList<>();

    private long preselectedCustomerId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_loan);

        dbHelper = new DatabaseHelper(this);
        preselectedCustomerId = getIntent().getLongExtra("customer_id", -1);

        spCustomer = (Spinner) findViewById(R.id.sp_loan_customer);
        spExistingLoans = (Spinner) findViewById(R.id.sp_existing_loans);
        spInterestMethod = (Spinner) findViewById(R.id.sp_interest_method);

        rgMode = (RadioGroup) findViewById(R.id.rg_loan_mode);
        rbModeNew = (RadioButton) findViewById(R.id.rb_mode_new);
        rbModeAddition = (RadioButton) findViewById(R.id.rb_mode_addition);

        layoutExistingLoans = (LinearLayout) findViewById(R.id.layout_existing_loans);
        layoutNewLoanSettings = (LinearLayout) findViewById(R.id.layout_new_loan_settings);

        etAmount = (EditText) findViewById(R.id.et_loan_amount);
        etLoanDate = (EditText) findViewById(R.id.et_loan_date);
        etInterestRate = (EditText) findViewById(R.id.et_interest_rate);
        etDueDate = (EditText) findViewById(R.id.et_due_date);
        etNotes = (EditText) findViewById(R.id.et_loan_notes);

        etLoanDate.setText(DatabaseHelper.getTodayDateString());

        setupInterestMethodsSpinner();
        loadCustomerSpinner();

        rgMode.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.rb_mode_addition) {
                    layoutExistingLoans.setVisibility(View.VISIBLE);
                    layoutNewLoanSettings.setVisibility(View.GONE);
                    loadExistingLoansForSelectedCustomer();
                } else {
                    layoutExistingLoans.setVisibility(View.GONE);
                    layoutNewLoanSettings.setVisibility(View.VISIBLE);
                }
            }
        });

        spCustomer.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (rbModeAddition.isChecked()) {
                    loadExistingLoansForSelectedCustomer();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        Button btnSave = (Button) findViewById(R.id.btn_save_loan);
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveLoan();
            }
        });
    }

    private void setupInterestMethodsSpinner() {
        String[] methods = new String[]{
                "Monthly Interest",
                "Daily Interest",
                "Yearly Interest",
                "Fixed Interest Amount",
                "Simple Interest",
                "Flat Interest",
                "Reducing Balance Interest",
                "No Interest"
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, methods);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spInterestMethod.setAdapter(adapter);
    }

    private void loadCustomerSpinner() {
        customerIdList.clear();
        customerNameList.clear();

        Cursor cursor = dbHelper.getAllActiveCustomers();
        int preselectIndex = 0;
        int count = 0;

        if (cursor != null && cursor.moveToFirst()) {
            do {
                long cId = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                String phone = cursor.getString(cursor.getColumnIndexOrThrow("phone"));

                customerIdList.add(cId);
                customerNameList.add(name + " (" + phone + ")");

                if (cId == preselectedCustomerId) {
                    preselectIndex = count;
                }
                count++;
            } while (cursor.moveToNext());
            cursor.close();
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, customerNameList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCustomer.setAdapter(adapter);

        if (!customerIdList.isEmpty()) {
            spCustomer.setSelection(preselectIndex);
        }
    }

    private void loadExistingLoansForSelectedCustomer() {
        existingLoanIdList.clear();
        existingLoanDescList.clear();

        int selectedPos = spCustomer.getSelectedItemPosition();
        if (selectedPos < 0 || selectedPos >= customerIdList.size()) return;

        long customerId = customerIdList.get(selectedPos);
        Cursor cursor = dbHelper.getLoansForCustomer(customerId);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                long lId = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                double principal = dbHelper.getTotalPrincipalGivenForLoan(lId);
                String loanDate = cursor.getString(cursor.getColumnIndexOrThrow("loan_date"));

                existingLoanIdList.add(lId);
                existingLoanDescList.add(String.format(Locale.US, "Loan #%d - Principal: ₹%.2f (%s)", lId, principal, loanDate));

            } while (cursor.moveToNext());
            cursor.close();
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, existingLoanDescList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spExistingLoans.setAdapter(adapter);
    }

    private void saveLoan() {
        int selectedCustPos = spCustomer.getSelectedItemPosition();
        if (selectedCustPos < 0 || customerIdList.isEmpty()) {
            Toast.makeText(this, "Please select a borrower", Toast.LENGTH_SHORT).show();
            return;
        }

        long customerId = customerIdList.get(selectedCustPos);

        String amountStr = etAmount.getText().toString().trim();
        String loanDate = etLoanDate.getText().toString().trim();
        String notes = etNotes.getText().toString().trim();

        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Please enter loan amount", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = Double.parseDouble(amountStr);
        if (amount <= 0) {
            Toast.makeText(this, "Amount must be greater than 0", Toast.LENGTH_SHORT).show();
            return;
        }

        if (rbModeAddition.isChecked()) {
            // MODE B: Add to existing loan
            int loanPos = spExistingLoans.getSelectedItemPosition();
            if (loanPos < 0 || existingLoanIdList.isEmpty()) {
                Toast.makeText(this, "No existing loan selected to top-up!", Toast.LENGTH_SHORT).show();
                return;
            }
            long loanId = existingLoanIdList.get(loanPos);
            boolean success = dbHelper.addMoneyToExistingLoan(loanId, customerId, amount, loanDate, notes);
            if (success) {
                Toast.makeText(this, "Top-up amount added to Loan #" + loanId, Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Failed to add loan top-up", Toast.LENGTH_SHORT).show();
            }
        } else {
            // MODE A: Create separate new loan
            String method = spInterestMethod.getSelectedItem().toString();
            String rateStr = etInterestRate.getText().toString().trim();
            String dueDate = etDueDate.getText().toString().trim();

            double rate = 0;
            if (!rateStr.isEmpty()) {
                rate = Double.parseDouble(rateStr);
            }

            long newLoanId = dbHelper.addLoan(customerId, amount, loanDate, dueDate, method, rate, "Monthly", "Monthly", notes);
            if (newLoanId > 0) {
                Toast.makeText(this, "New Loan #" + newLoanId + " created successfully!", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Failed to disburse new loan", Toast.LENGTH_SHORT).show();
            }
        }
    }
}