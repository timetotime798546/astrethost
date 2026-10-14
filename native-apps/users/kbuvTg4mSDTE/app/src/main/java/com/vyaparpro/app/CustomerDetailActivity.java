package com.vyaparpro.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

public class CustomerDetailActivity extends Activity {

    private BackendApi api;
    private String customerId;
    private String customerName;
    private String customerPhone;

    private TextView tvDetailCustomerName;
    private TextView tvDetailCustomerPhone;
    private TextView tvCustomerNetBalance;
    private TextView tvBalanceLabel;

    private LinearLayout layoutTxEntry;
    private TextView tvTxEntryTitle;
    private EditText etTxAmount;
    private EditText etTxNote;
    private Button btnSubmitTx;
    private ProgressBar progressTx;
    private LinearLayout containerTxHistory;

    private String currentTxType = "give"; // "give" or "got"
    private double currentNetBalance = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_detail);

        api = new BackendApi(this);

        customerId = getIntent().getStringExtra("customer_id");
        customerName = getIntent().getStringExtra("customer_name");
        customerPhone = getIntent().getStringExtra("customer_phone");

        tvDetailCustomerName = (TextView) findViewById(R.id.tvDetailCustomerName);
        tvDetailCustomerPhone = (TextView) findViewById(R.id.tvDetailCustomerPhone);
        tvCustomerNetBalance = (TextView) findViewById(R.id.tvCustomerNetBalance);
        tvBalanceLabel = (TextView) findViewById(R.id.tvBalanceLabel);

        layoutTxEntry = (LinearLayout) findViewById(R.id.layoutTxEntry);
        tvTxEntryTitle = (TextView) findViewById(R.id.tvTxEntryTitle);
        etTxAmount = (EditText) findViewById(R.id.etTxAmount);
        etTxNote = (EditText) findViewById(R.id.etTxNote);
        btnSubmitTx = (Button) findViewById(R.id.btnSubmitTx);
        progressTx = (ProgressBar) findViewById(R.id.progressTx);
        containerTxHistory = (LinearLayout) findViewById(R.id.containerTxHistory);

        tvDetailCustomerName.setText(customerName);
        tvDetailCustomerPhone.setText(customerPhone);

        findViewById(R.id.btnCustomerDetailBack).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        findViewById(R.id.btnGiveUdhaar).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentTxType = "give";
                tvTxEntryTitle.setText("Record Credit Given (You will get)");
                tvTxEntryTitle.setTextColor(Color.parseColor("#EF4444"));
                layoutTxEntry.setVisibility(View.VISIBLE);
            }
        });

        findViewById(R.id.btnGotPayment).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentTxType = "got";
                tvTxEntryTitle.setText("Record Payment Received (Got)");
                tvTxEntryTitle.setTextColor(Color.parseColor("#10B981"));
                layoutTxEntry.setVisibility(View.VISIBLE);
            }
        });

        btnSubmitTx.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitTransaction();
            }
        });

        findViewById(R.id.btnSendReminder).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendSmsReminder();
            }
        });

        loadTransactions();
    }

    private void submitTransaction() {
        String amtStr = etTxAmount.getText().toString().trim();
        String note = etTxNote.getText().toString().trim();

        if (amtStr.isEmpty()) {
            Toast.makeText(this, "Please enter an amount.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double amount = Double.parseDouble(amtStr);
            if (note.isEmpty()) {
                note = currentTxType.equals("give") ? "Credit given" : "Payment received";
            }

            JSONObject data = new JSONObject();
            data.put("customer_id", customerId);
            data.put("customer_name", customerName);
            data.put("type", currentTxType);
            data.put("amount", amount);
            data.put("note", note);
            data.put("date", BusinessLogic.getCurrentDate());

            progressTx.setVisibility(View.VISIBLE);
            api.createData("transactions", data, new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    progressTx.setVisibility(View.GONE);
                    Toast.makeText(CustomerDetailActivity.this, "Transaction Saved!", Toast.LENGTH_SHORT).show();
                    layoutTxEntry.setVisibility(View.GONE);
                    etTxAmount.setText("");
                    etTxNote.setText("");
                    loadTransactions();
                }

                @Override
                public void onError(String errorMessage) {
                    progressTx.setVisibility(View.GONE);
                    Toast.makeText(CustomerDetailActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Toast.makeText(this, "Invalid number.", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadTransactions() {
        progressTx.setVisibility(View.VISIBLE);
        api.getData("transactions", new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                progressTx.setVisibility(View.GONE);
                containerTxHistory.removeAllViews();
                JSONArray records = response.optJSONArray("records");

                double totalGiven = 0;
                double totalGot = 0;

                if (records != null) {
                    for (int i = 0; i < records.length(); i++) {
                        JSONObject r = records.optJSONObject(i);
                        if (r == null) continue;
                        JSONObject data = r.optJSONObject("data");
                        if (data == null) continue;

                        String cId = data.optString("customer_id", "");
                        if (!cId.equals(customerId)) continue;

                        String type = data.optString("type", "give");
                        double amount = data.optDouble("amount", 0.0);
                        String note = data.optString("note", "");
                        String date = data.optString("date", "");

                        if ("give".equalsIgnoreCase(type)) {
                            totalGiven += amount;
                        } else {
                            totalGot += amount;
                        }

                        // Add transaction item view
                        LinearLayout row = new LinearLayout(CustomerDetailActivity.this);
                        row.setOrientation(LinearLayout.HORIZONTAL);
                        row.setBackgroundColor(Color.WHITE);
                        row.setElevation(1f);
                        row.setPadding(18, 14, 18, 14);
                        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                        lp.setMargins(0, 0, 0, 8);
                        row.setLayoutParams(lp);

                        LinearLayout left = new LinearLayout(CustomerDetailActivity.this);
                        left.setOrientation(LinearLayout.VERTICAL);
                        LinearLayout.LayoutParams leftLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);

                        TextView tvNote = new TextView(CustomerDetailActivity.this);
                        tvNote.setText(note);
                        tvNote.setTextColor(Color.parseColor("#0F172A"));
                        tvNote.setTextSize(14f);
                        tvNote.setTypeface(null, android.graphics.Typeface.BOLD);
                        left.addView(tvNote);

                        TextView tvDate = new TextView(CustomerDetailActivity.this);
                        tvDate.setText(date);
                        tvDate.setTextColor(Color.parseColor("#94A3B8"));
                        tvDate.setTextSize(12f);
                        left.addView(tvDate);

                        row.addView(left, leftLp);

                        TextView tvAmt = new TextView(CustomerDetailActivity.this);
                        boolean isGive = "give".equalsIgnoreCase(type);
                        tvAmt.setText((isGive ? "- " : "+ ") + BusinessLogic.getFormattedCurrency(amount));
                        tvAmt.setTextColor(isGive ? Color.parseColor("#EF4444") : Color.parseColor("#10B981"));
                        tvAmt.setTextSize(15f);
                        tvAmt.setTypeface(null, android.graphics.Typeface.BOLD);
                        row.addView(tvAmt);

                        containerTxHistory.addView(row);
                    }
                }

                currentNetBalance = BusinessLogic.calculateCustomerBalance(totalGiven, totalGot);
                if (currentNetBalance > 0) {
                    tvBalanceLabel.setText("YOU WILL GET (CREDIT OUTSTANDING)");
                    tvCustomerNetBalance.setText(BusinessLogic.getFormattedCurrency(currentNetBalance));
                    tvCustomerNetBalance.setTextColor(Color.parseColor("#EF4444"));
                } else if (currentNetBalance < 0) {
                    tvBalanceLabel.setText("YOU WILL GIVE (ADVANCE PAID)");
                    tvCustomerNetBalance.setText(BusinessLogic.getFormattedCurrency(Math.abs(currentNetBalance)));
                    tvCustomerNetBalance.setTextColor(Color.parseColor("#10B981"));
                } else {
                    tvBalanceLabel.setText("ACCOUNT SETTLED");
                    tvCustomerNetBalance.setText("₹0.00");
                    tvCustomerNetBalance.setTextColor(Color.parseColor("#64748B"));
                }
            }

            @Override
            public void onError(String errorMessage) {
                progressTx.setVisibility(View.GONE);
                Toast.makeText(CustomerDetailActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendSmsReminder() {
        if (customerPhone == null || customerPhone.isEmpty()) {
            Toast.makeText(this, "Customer has no phone number recorded.", Toast.LENGTH_SHORT).show();
            return;
        }

        String message = "Dear " + customerName + ", your outstanding balance at our store is "
                + BusinessLogic.getFormattedCurrency(currentNetBalance)
                + ". Kindly clear your dues at your earliest convenience. Thank you! - VyaparPro";

        Intent smsIntent = new Intent(Intent.ACTION_SENDTO);
        smsIntent.setData(Uri.parse("smsto:" + Uri.encode(customerPhone)));
        smsIntent.putExtra("sms_body", message);
        try {
            startActivity(smsIntent);
        } catch (Exception e) {
            Toast.makeText(this, "Unable to open SMS application.", Toast.LENGTH_SHORT).show();
        }
    }
}