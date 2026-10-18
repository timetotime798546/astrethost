package com.aviatorpro.app;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class AdminActivity extends Activity {

    private static final String PREF_GAME = "aviator_game_prefs";

    private TextView tvMarginRatioDisplay, tvPendingDepositsCount;
    private Button btnSetMargin70, btnSetMargin80, btnSaveAdminUpi, btnApproveTestDeposit, btnBackFromAdmin;
    private EditText etAdminUpi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        tvMarginRatioDisplay = (TextView) findViewById(R.id.tvMarginRatioDisplay);
        tvPendingDepositsCount = (TextView) findViewById(R.id.tvPendingDepositsCount);
        btnSetMargin70 = (Button) findViewById(R.id.btnSetMargin70);
        btnSetMargin80 = (Button) findViewById(R.id.btnSetMargin80);
        btnSaveAdminUpi = (Button) findViewById(R.id.btnSaveAdminUpi);
        btnApproveTestDeposit = (Button) findViewById(R.id.btnApproveTestDeposit);
        btnBackFromAdmin = (Button) findViewById(R.id.btnBackFromAdmin);
        etAdminUpi = (EditText) findViewById(R.id.etAdminUpi);

        loadSettings();

        btnSetMargin70.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setHouseMargin(70);
            }
        });

        btnSetMargin80.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setHouseMargin(80);
            }
        });

        btnSaveAdminUpi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String upi = etAdminUpi.getText().toString().trim();
                if (!upi.isEmpty()) {
                    SharedPreferences sp = getSharedPreferences(PREF_GAME, Context.MODE_PRIVATE);
                    sp.edit().putString("admin_upi_id", upi).apply();
                    Toast.makeText(AdminActivity.this, "Operator settlement UPI updated!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnApproveTestDeposit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                approvePendingDeposit();
            }
        });

        btnBackFromAdmin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void loadSettings() {
        SharedPreferences sp = getSharedPreferences(PREF_GAME, Context.MODE_PRIVATE);
        int margin = sp.getInt("house_margin", 70);
        tvMarginRatioDisplay.setText("Current Setting: " + margin + "% House Edge / " + (100 - margin) + "% User Wins");

        String upi = sp.getString("admin_upi_id", "aviatorpay.business@okhdfcbank");
        etAdminUpi.setText(upi);

        float pendingAmt = sp.getFloat("pending_deposit_amount", 0.0f);
        String pendingUtr = sp.getString("pending_deposit_utr", "");
        if (pendingAmt > 0 && !pendingUtr.isEmpty()) {
            tvPendingDepositsCount.setText("Pending: 1 Request (₹" + pendingAmt + " - UTR: " + pendingUtr + ")");
            btnApproveTestDeposit.setText("VERIFY & APPROVE (₹" + pendingAmt + ")");
            btnApproveTestDeposit.setEnabled(true);
        } else {
            tvPendingDepositsCount.setText("Pending Verification: No requests currently");
            btnApproveTestDeposit.setEnabled(false);
        }
    }

    private void setHouseMargin(int margin) {
        SharedPreferences sp = getSharedPreferences(PREF_GAME, Context.MODE_PRIVATE);
        sp.edit().putInt("house_margin", margin).apply();
        tvMarginRatioDisplay.setText("Current Setting: " + margin + "% House Edge / " + (100 - margin) + "% User Wins");
        Toast.makeText(this, "Algorithm updated to " + margin + "% House Edge!", Toast.LENGTH_SHORT).show();
    }

    private void approvePendingDeposit() {
        SharedPreferences sp = getSharedPreferences(PREF_GAME, Context.MODE_PRIVATE);
        float pendingAmt = sp.getFloat("pending_deposit_amount", 0.0f);
        if (pendingAmt > 0) {
            float curBal = sp.getFloat("wallet_balance", 0.0f);
            float newBal = curBal + pendingAmt;
            sp.edit()
                    .putFloat("wallet_balance", newBal)
                    .remove("pending_deposit_amount")
                    .remove("pending_deposit_utr")
                    .apply();

            Toast.makeText(this, "Deposit Approved! Credited ₹" + pendingAmt + " to wallet.", Toast.LENGTH_LONG).show();
            loadSettings();
        }
    }
}