package com.varshalaxmi.loanmanager;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class CustomerDetailActivity extends Activity {

    private DatabaseHelper dbHelper;
    private long customerId = -1;

    private TextView tvName, tvPhone, tvAddress;
    private TextView tvPrincipalGiven, tvTotalPaid, tvTotalOutstanding;
    private LinearLayout layoutLoansContainer;
    private String customerName = "";
    private String customerPhone = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_detail);

        dbHelper = new DatabaseHelper(this);
        customerId = getIntent().getLongExtra("customer_id", -1);

        tvName = (TextView) findViewById(R.id.tv_detail_name);
        tvPhone = (TextView) findViewById(R.id.tv_detail_phone);
        tvAddress = (TextView) findViewById(R.id.tv_detail_address);

        tvPrincipalGiven = (TextView) findViewById(R.id.tv_detail_principal_given);
        tvTotalPaid = (TextView) findViewById(R.id.tv_detail_total_paid);
        tvTotalOutstanding = (TextView) findViewById(R.id.tv_detail_total_outstanding);

        layoutLoansContainer = (LinearLayout) findViewById(R.id.layout_loans_container);

        Button btnAddLoan = (Button) findViewById(R.id.btn_detail_add_loan);
        Button btnCollectPayment = (Button) findViewById(R.id.btn_detail_collect_payment);
        Button btnWhatsApp = (Button) findViewById(R.id.btn_detail_whatsapp);
        Button btnEditCust = (Button) findViewById(R.id.btn_detail_edit_cust);

        btnAddLoan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CustomerDetailActivity.this, NewLoanActivity.class);
                intent.putExtra("customer_id", customerId);
                startActivity(intent);
            }
        });

        btnCollectPayment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CustomerDetailActivity.this, CollectPaymentActivity.class);
                intent.putExtra("customer_id", customerId);
                startActivity(intent);
            }
        });

        btnEditCust.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CustomerDetailActivity.this, AddCustomerActivity.class);
                intent.putExtra("customer_id", customerId);
                startActivity(intent);
            }
        });

        btnWhatsApp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendWhatsAppReminder();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCustomerProfile();
        loadCustomerLoans();
    }

    private void loadCustomerProfile() {
        Cursor cursor = dbHelper.getCustomerById(customerId);
        if (cursor != null && cursor.moveToFirst()) {
            customerName = cursor.getString(cursor.getColumnIndexOrThrow("name"));
            customerPhone = cursor.getString(cursor.getColumnIndexOrThrow("phone"));
            String address = cursor.getString(cursor.getColumnIndexOrThrow("address"));

            tvName.setText(customerName);
            tvPhone.setText("📱 " + (customerPhone.isEmpty() ? "No Phone" : customerPhone));
            tvAddress.setText("📍 " + (address.isEmpty() ? "No Address" : address));
            cursor.close();
        }

        double given = dbHelper.getCustomerTotalPrincipalGiven(customerId);
        double paid = dbHelper.getCustomerTotalPaid(customerId);
        double outstanding = dbHelper.getCustomerTotalOutstanding(customerId);

        tvPrincipalGiven.setText(String.format(Locale.US, "₹ %,.2f", given));
        tvTotalPaid.setText(String.format(Locale.US, "₹ %,.2f", paid));
        tvTotalOutstanding.setText(String.format(Locale.US, "₹ %,.2f", outstanding));
    }

    private void loadCustomerLoans() {
        layoutLoansContainer.removeAllViews();
        Cursor cursor = dbHelper.getLoansForCustomer(customerId);
        if (cursor != null && cursor.moveToFirst()) {
            LayoutInflater inflater = LayoutInflater.from(this);
            do {
                final long loanId = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                String loanDate = cursor.getString(cursor.getColumnIndexOrThrow("loan_date"));
                String status = cursor.getString(cursor.getColumnIndexOrThrow("status"));
                String method = cursor.getString(cursor.getColumnIndexOrThrow("interest_method"));

                double totalPrincipal = dbHelper.getTotalPrincipalGivenForLoan(loanId);
                double totalPaidPrincipal = dbHelper.getTotalPrincipalPaidForLoan(loanId);
                double totalPaidInterest = dbHelper.getTotalInterestPaidForLoan(loanId);
                double outstanding = dbHelper.getLoanOutstandingAmount(loanId);

                View card = inflater.inflate(R.layout.item_loan, layoutLoansContainer, false);

                TextView tvLoanHeader = (TextView) card.findViewById(R.id.tv_item_loan_header);
                TextView tvLoanDate = (TextView) card.findViewById(R.id.tv_item_loan_date);
                TextView tvStatus = (TextView) card.findViewById(R.id.tv_item_loan_status);
                TextView tvPrincipal = (TextView) card.findViewById(R.id.tv_item_loan_principal);
                TextView tvMethod = (TextView) card.findViewById(R.id.tv_item_loan_method);
                TextView tvPaid = (TextView) card.findViewById(R.id.tv_item_loan_paid);
                TextView tvDue = (TextView) card.findViewById(R.id.tv_item_loan_due);

                tvLoanHeader.setText("Loan #" + loanId);
                tvLoanDate.setText("Date: " + loanDate);
                tvStatus.setText(status);
                tvStatus.setTextColor("ACTIVE".equalsIgnoreCase(status) ? 0xFF1E8449 : 0xFF7F8C8D);

                tvPrincipal.setText(String.format(Locale.US, "Principal: ₹%,.2f", totalPrincipal));
                tvMethod.setText("Interest: " + method);
                tvPaid.setText(String.format(Locale.US, "Paid: ₹%,.2f (P: ₹%,.2f, I: ₹%,.2f)", (totalPaidPrincipal + totalPaidInterest), totalPaidPrincipal, totalPaidInterest));
                tvDue.setText(String.format(Locale.US, "Outstanding: ₹%,.2f", outstanding));

                card.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(CustomerDetailActivity.this, CollectPaymentActivity.class);
                        intent.putExtra("customer_id", customerId);
                        intent.putExtra("loan_id", loanId);
                        startActivity(intent);
                    }
                });

                layoutLoansContainer.addView(card);

            } while (cursor.moveToNext());
            cursor.close();
        } else {
            TextView emptyTv = new TextView(this);
            emptyTv.setText("No loan accounts recorded for this borrower.");
            emptyTv.setPadding(16, 16, 16, 16);
            emptyTv.setTextColor(0xFF5D6D7E);
            layoutLoansContainer.addView(emptyTv);
        }
    }

    private void sendWhatsAppReminder() {
        if (customerPhone.isEmpty()) {
            Toast.makeText(this, "No mobile number available for WhatsApp", Toast.LENGTH_SHORT).show();
            return;
        }

        double totalDue = dbHelper.getCustomerTotalOutstanding(customerId);
        String message = "Namaste " + customerName + ",\n\n" +
                "This is a friendly reminder from VarshaLaxmi regarding your outstanding loan balance.\n" +
                "Total Amount Due: ₹" + String.format(Locale.US, "%.2f", totalDue) + "\n\n" +
                "Please make your payment at your earliest convenience. Thank you!";

        try {
            String cleanPhone = customerPhone.replaceAll("[^0-9]", "");
            if (cleanPhone.length() == 10) {
                cleanPhone = "91" + cleanPhone; // Default to India international country code if 10 digits
            }
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://api.whatsapp.com/send?phone=" + cleanPhone + "&text=" + Uri.encode(message)));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Could not launch WhatsApp app", Toast.LENGTH_SHORT).show();
        }
    }
}