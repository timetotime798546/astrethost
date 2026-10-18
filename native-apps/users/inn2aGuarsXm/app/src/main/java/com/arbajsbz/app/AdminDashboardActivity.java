package com.arbajsbz.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AdminDashboardActivity extends Activity {

    private EditText etPoolAmount;
    private TextView tvOwnerShare;
    private TextView tvWinnerShare;
    private Button btnCalc;
    private Button btnRecord;
    private LinearLayout containerWithdrawals;
    private TextView tvNoWithdrawals;

    private BackendApi backendApi;
    private SoundManager soundManager;

    private double currentPool = 1000.0;
    private double ownerShare = 600.0;
    private double winnerShare = 400.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        backendApi = new BackendApi(this);
        soundManager = SoundManager.getInstance(this);

        etPoolAmount = (EditText) findViewById(R.id.et_admin_pool_amount);
        tvOwnerShare = (TextView) findViewById(R.id.tv_owner_share_val);
        tvWinnerShare = (TextView) findViewById(R.id.tv_winner_share_val);
        btnCalc = (Button) findViewById(R.id.btn_calc_allocation);
        btnRecord = (Button) findViewById(R.id.btn_record_allocation);
        containerWithdrawals = (LinearLayout) findViewById(R.id.container_admin_withdrawals);
        tvNoWithdrawals = (TextView) findViewById(R.id.tv_admin_no_withdrawals);

        findViewById(R.id.btn_admin_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnCalc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                soundManager.playButtonClick();
                calculateAllocations();
            }
        });

        btnRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                soundManager.playButtonClick();
                recordAllocationEntry();
            }
        });

        loadPendingWithdrawals();
    }

    private void calculateAllocations() {
        try {
            double pool = Double.parseDouble(etPoolAmount.getText().toString().trim());
            if (pool < 0) pool = 0;

            currentPool = pool;
            // Native Java local deterministic 60% Owner / 40% Winner calculations
            ownerShare = currentPool * 0.60;
            winnerShare = currentPool * 0.40;

            tvOwnerShare.setText("₹ " + String.format(Locale.US, "%.2f", ownerShare));
            tvWinnerShare.setText("₹ " + String.format(Locale.US, "%.2f", winnerShare));
        } catch (Exception e) {
            Toast.makeText(this, "Enter valid pool amount", Toast.LENGTH_SHORT).show();
        }
    }

    private void recordAllocationEntry() {
        calculateAllocations();
        try {
            JSONObject data = new JSONObject();
            data.put("reference_id", "ALC-" + System.currentTimeMillis());
            data.put("pool_amount", currentPool);
            data.put("owner_share_amount", ownerShare);
            data.put("winner_share_amount", winnerShare);
            data.put("owner_percentage", 60);
            data.put("winner_percentage", 40);
            data.put("status", "EXECUTED");
            data.put("timestamp", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date()));

            backendApi.createRecord("financial_allocations", data, new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    Toast.makeText(AdminDashboardActivity.this, "60/40 Allocation Audited and Saved!", Toast.LENGTH_LONG).show();
                }

                @Override
                public void onError(String message) {
                    Toast.makeText(AdminDashboardActivity.this, "Failed: " + message, Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception ignored) {}
    }

    private void loadPendingWithdrawals() {
        backendApi.readRecords("withdrawals", new BackendApi.ReadCallback() {
            @Override
            public void onSuccess(JSONArray records) {
                containerWithdrawals.removeAllViews();
                if (records.length() == 0) {
                    tvNoWithdrawals.setText("No pending withdrawal requests found.");
                    containerWithdrawals.addView(tvNoWithdrawals);
                    return;
                }

                try {
                    for (int i = 0; i < records.length(); i++) {
                        JSONObject rec = records.getJSONObject(i);
                        final String docId = rec.optString("id");
                        final JSONObject data = rec.optJSONObject("data");
                        if (data != null) {
                            addWithdrawalCard(docId, data);
                        }
                    }
                } catch (Exception ignored) {}
            }

            @Override
            public void onError(String message) {
                tvNoWithdrawals.setText("Error reading withdrawals: " + message);
            }
        });
    }

    private void addWithdrawalCard(final String docId, final JSONObject data) {
        String email = data.optString("email", "pilot");
        final double amount = data.optDouble("amount", 0.0);
        String method = data.optString("payout_method", "UPI");
        String detail = data.optString("recipient_detail", "");
        final String status = data.optString("status", "PENDING");
        String ref = data.optString("tx_ref", "REF");

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_card_dark);
        card.setPadding(20, 16, 20, 16);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 14);
        card.setLayoutParams(params);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(email + " • ₹" + (int) amount);
        tvTitle.setTextColor(Color.WHITE);
        tvTitle.setTextSize(15f);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        card.addView(tvTitle);

        TextView tvDetails = new TextView(this);
        tvDetails.setText("Method: " + method + " (" + detail + ")\nRef: " + ref + " | Status: " + status);
        tvDetails.setTextColor(getResources().getColor(R.color.text_secondary));
        tvDetails.setTextSize(12f);
        tvDetails.setPadding(0, 6, 0, 10);
        card.addView(tvDetails);

        if (status.equalsIgnoreCase("PENDING")) {
            LinearLayout btnRow = new LinearLayout(this);
            btnRow.setOrientation(LinearLayout.HORIZONTAL);

            Button btnApprove = new Button(this);
            btnApprove.setText("APPROVE & PAY");
            btnApprove.setTextColor(Color.WHITE);
            btnApprove.setTextSize(11f);
            btnApprove.setBackgroundResource(R.drawable.bg_green_button);
            LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(0, 80, 1f);
            p1.setMargins(0, 0, 6, 0);
            btnApprove.setLayoutParams(p1);

            btnApprove.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    confirmPayoutApproval(docId, data, "COMPLETED");
                }
            });

            Button btnReject = new Button(this);
            btnReject.setText("REJECT");
            btnReject.setTextColor(Color.WHITE);
            btnReject.setTextSize(11f);
            btnReject.setBackgroundResource(R.drawable.bg_chip);
            LinearLayout.LayoutParams p2 = new LinearLayout.LayoutParams(0, 80, 1f);
            p2.setMargins(6, 0, 0, 0);
            btnReject.setLayoutParams(p2);

            btnReject.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    confirmPayoutApproval(docId, data, "REJECTED");
                }
            });

            btnRow.addView(btnApprove);
            btnRow.addView(btnReject);
            card.addView(btnRow);
        }

        containerWithdrawals.addView(card);
    }

    private void confirmPayoutApproval(final String docId, final JSONObject originalData, final String newStatus) {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("Admin Payout Confirmation");
        b.setMessage("Confirm updating payout status to " + newStatus + "?");
        b.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    originalData.put("status", newStatus);
                    backendApi.updateRecord(docId, originalData, new BackendApi.ApiCallback() {
                        @Override
                        public void onSuccess(JSONObject response) {
                            Toast.makeText(AdminDashboardActivity.this, "Status updated to " + newStatus, Toast.LENGTH_SHORT).show();
                            loadPendingWithdrawals();
                        }

                        @Override
                        public void onError(String message) {
                            Toast.makeText(AdminDashboardActivity.this, "Update failed: " + message, Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (Exception ignored) {}
            }
        });
        b.setNegativeButton("Cancel", null);
        b.show();
    }
}