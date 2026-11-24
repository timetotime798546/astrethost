package com.varshalaxmi.loanmanager;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CollectPaymentActivity extends Activity {

    private DatabaseHelper dbHelper;
    private Spinner spCustomer, spLoan, spMethod;
    private RadioGroup rgSplit;
    private LinearLayout layoutManualSplit;
    private TextView tvSummary, tvOutstandingTotal;
    private EditText etAmount, etPrincipal, etInterest, etDate, etNotes;

    private List<Long> customerIdList = new ArrayList<>();
    private List<String> customerNameList = new ArrayList<>();

    private List<Long> loanIdList = new ArrayList<>();
    private List<String> loanDescList = new ArrayList<>();

    private long preselectedCustomerId = -1;
    private long preselectedLoanId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collect_payment);

        dbHelper = new DatabaseHelper(this);
        preselectedCustomerId = getIntent().getLongExtra("customer_id", -1);
        preselectedLoanId = getIntent().getLongExtra("loan_id", -1);

        spCustomer = (Spinner) findViewById(R.id.sp_pay_customer);
        spLoan = (Spinner) findViewById(R.id.sp_pay_loan);
        spMethod = (Spinner) findViewById(R.id.sp_pay_method);

        rgSplit = (RadioGroup) findViewById(R.id.rg_pay_split);
        layoutManualSplit = (LinearLayout) findViewById(R.id.layout_manual_split);

        tvSummary = (TextView) findViewById(R.id.tv_pay_loan_summary);
        tvOutstandingTotal = (TextView) findViewById(R.id.tv_pay_outstanding_total);

        etAmount = (EditText) findViewById(R.id.et_pay_amount);
        etPrincipal = (EditText) findViewById(R.id.et_pay_principal_component);
        etInterest = (EditText) findViewById(R.id.et_pay_interest_component);
        etDate = (EditText) findViewById(R.id.et_pay_date);
        etNotes = (EditText) findViewById(R.id.et_pay_notes);

        etDate.setText(DatabaseHelper.getTodayDateString());

        setupPaymentMethodsSpinner();
        loadCustomerSpinner();

        rgSplit.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.rb_split_manual) {
                    layoutManualSplit.setVisibility(View.VISIBLE);
                } else {
                    layoutManualSplit.setVisibility(View.GONE);
                }
            }
        });

        spCustomer.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadLoansForCustomer();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spLoan.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateLoanSummaryCard();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        Button btnSave = (Button) findViewById(R.id.btn_save_payment);
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmAndSavePayment();
            }
        });
    }

    private void setupPaymentMethodsSpinner() {
        String[] methods = new String[]{"Cash", "UPI", "Bank Transfer", "Cheque", "Other"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, methods);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spMethod.setAdapter(adapter);
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

    private void loadLoansForCustomer() {
        loanIdList.clear();
        loanDescList.clear();

        int selectedCustPos = spCustomer.getSelectedItemPosition();
        if (selectedCustPos < 0 || selectedCustPos >= customerIdList.size()) return;

        long customerId = customerIdList.get(selectedCustPos);
        Cursor cursor = dbHelper.getLoansForCustomer(customerId);

        int preselectIndex = 0;
        int count = 0;

        if (cursor != null && cursor.moveToFirst()) {
            do {
                long lId = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                double due = dbHelper.getLoanOutstandingAmount(lId);
                String loanDate = cursor.getString(cursor.getColumnIndexOrThrow("loan_date"));
                String status = cursor.getString(cursor.getColumnIndexOrThrow("status"));

                loanIdList.add(lId);
                loanDescList.add(String.format(Locale.US, "Loan #%d - Due: ₹%.2f (%s) [%s]", lId, due, loanDate, status));

                if (lId == preselectedLoanId) {
                    preselectIndex = count;
                }
                count++;
            } while (cursor.moveToNext());
            cursor.close();
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, loanDescList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spLoan.setAdapter(adapter);

        if (!loanIdList.isEmpty()) {
            spLoan.setSelection(preselectIndex);
        } else {
            tvSummary.setText("No active loan account found for this borrower.");
            tvOutstandingTotal.setText("Total Outstanding: ₹ 0.00");
        }
    }

    private void updateLoanSummaryCard() {
        int selectedLoanPos = spLoan.getSelectedItemPosition();
        if (selectedLoanPos < 0 || selectedLoanPos >= loanIdList.size()) return;

        long loanId = loanIdList.get(selectedLoanPos);

        double totalPrincipal = dbHelper.getTotalPrincipalGivenForLoan(loanId);
        double accruedInterest = dbHelper.calculateAccruedInterestForLoan(loanId);
        double paidPrincipal = dbHelper.getTotalPrincipalPaidForLoan(loanId);
        double paidInterest = dbHelper.getTotalInterestPaidForLoan(loanId);

        double remainingPrincipal = totalPrincipal - paidPrincipal;
        double remainingInterest = accruedInterest - paidInterest;
        if (remainingPrincipal < 0) remainingPrincipal = 0;
        if (remainingInterest < 0) remainingInterest = 0;

        double totalDue = dbHelper.getLoanOutstandingAmount(loanId);

        tvSummary.setText(String.format(Locale.US,
                "Principal Remaining: ₹%,.2f | Accrued Interest Due: ₹%,.2f",
                remainingPrincipal, remainingInterest));
        tvOutstandingTotal.setText(String.format(Locale.US, "Total Outstanding: ₹ %,.2f", totalDue));
    }

    private void confirmAndSavePayment() {
        int selectedCustPos = spCustomer.getSelectedItemPosition();
        int selectedLoanPos = spLoan.getSelectedItemPosition();

        if (selectedCustPos < 0 || customerIdList.isEmpty() || selectedLoanPos < 0 || loanIdList.isEmpty()) {
            Toast.makeText(this, "Please select valid customer and loan account", Toast.LENGTH_SHORT).show();
            return;
        }

        final long customerId = customerIdList.get(selectedCustPos);
        final long loanId = loanIdList.get(selectedLoanPos);

        String amountStr = etAmount.getText().toString().trim();
        final String payDate = etDate.getText().toString().trim();
        final String method = spMethod.getSelectedItem().toString();
        final String notes = etNotes.getText().toString().trim();

        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Please enter payment amount", Toast.LENGTH_SHORT).show();
            return;
        }

        final double totalPay = Double.parseDouble(amountStr);
        if (totalPay <= 0) {
            Toast.makeText(this, "Payment amount must be greater than 0", Toast.LENGTH_SHORT).show();
            return;
        }

        final double principalPaid;
        final double interestPaid;

        if (rgSplit.getCheckedRadioButtonId() == R.id.rb_split_manual) {
            String pStr = etPrincipal.getText().toString().trim();
            String iStr = etInterest.getText().toString().trim();
            double pVal = pStr.isEmpty() ? 0 : Double.parseDouble(pStr);
            double iVal = iStr.isEmpty() ? 0 : Double.parseDouble(iStr);

            if (Math.abs((pVal + iVal) - totalPay) > 0.01) {
                Toast.makeText(this, "Sum of Principal & Interest must equal total payment amount!", Toast.LENGTH_LONG).show();
                return;
            }
            principalPaid = pVal;
            interestPaid = iVal;
        } else {
            // Automatic Allocation: Pay accrued interest first, remainder goes to principal
            double accruedInterest = dbHelper.calculateAccruedInterestForLoan(loanId);
            double paidInterestSoFar = dbHelper.getTotalInterestPaidForLoan(loanId);
            double remainingInterestDue = accruedInterest - paidInterestSoFar;
            if (remainingInterestDue < 0) remainingInterestDue = 0;

            if (totalPay <= remainingInterestDue) {
                interestPaid = totalPay;
                principalPaid = 0;
            } else {
                interestPaid = remainingInterestDue;
                principalPaid = totalPay - remainingInterestDue;
            }
        }

        // Confirmation Dialog
        new AlertDialog.Builder(this)
                .setTitle("Confirm Payment Receipt")
                .setMessage(String.format(Locale.US,
                        "Record payment of ₹%.2f?\n\n" +
                                "• Principal Portion: ₹%.2f\n" +
                                "• Interest Portion: ₹%.2f\n" +
                                "• Payment Method: %s", totalPay, principalPaid, interestPaid, method))
                .setPositiveButton("Confirm & Save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        boolean saved = dbHelper.recordPayment(customerId, loanId, payDate, totalPay, principalPaid, interestPaid, method, notes);
                        if (saved) {
                            Toast.makeText(CollectPaymentActivity.this, "Payment receipt saved successfully!", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(CollectPaymentActivity.this, "Error saving payment record", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}