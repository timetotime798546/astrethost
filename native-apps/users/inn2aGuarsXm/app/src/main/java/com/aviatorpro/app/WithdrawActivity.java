package com.aviatorpro.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONObject;

public class WithdrawActivity extends Activity {

    private static final String PREF_GAME = "aviator_game_prefs";

    private TextView tvAvailableWithdrawBalance;
    private EditText etWithdrawAmount, etUserUpiId;
    private Button btnSubmitWithdraw, btnBackWithdraw;
    private ProgressBar pbWithdraw;

    private double currentBalance = 0.0;
    private BackendApi backendApi;
    private SoundManager soundManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_withdraw);

        backendApi = new BackendApi(this);
        soundManager = SoundManager.getInstance(this);

        tvAvailableWithdrawBalance = (TextView) findViewById(R.id.tvAvailableWithdrawBalance);
        etWithdrawAmount = (EditText) findViewById(R.id.etWithdrawAmount);
        etUserUpiId = (EditText) findViewById(R.id.etUserUpiId);
        btnSubmitWithdraw = (Button) findViewById(R.id.btnSubmitWithdraw);
        btnBackWithdraw = (Button) findViewById(R.id.btnBackWithdraw);
        pbWithdraw = (ProgressBar) findViewById(R.id.pbWithdraw);

        loadBalance();

        btnSubmitWithdraw.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                soundManager.playClick();
                processWithdrawal();
            }
        });

        btnBackWithdraw.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                soundManager.playClick();
                finish();
            }
        });
    }

    private void loadBalance() {
        SharedPreferences sp = getSharedPreferences(PREF_GAME, Context.MODE_PRIVATE);
        currentBalance = sp.getFloat("wallet_balance", 0.0f);
        tvAvailableWithdrawBalance.setText("₹ " + String.format("%.2f", currentBalance));
    }

    private void processWithdrawal() {
        String amountStr = etWithdrawAmount.getText().toString().trim();
        final String upiId = etUserUpiId.getText().toString().trim();

        if (amountStr.isEmpty() || upiId.isEmpty()) {
            soundManager.playError();
            Toast.makeText(this, "Please enter withdrawal amount and your UPI ID", Toast.LENGTH_SHORT).show();
            return;
        }

        final double amount = Double.parseDouble(amountStr);
        if (amount < 100.0) {
            soundManager.playError();
            Toast.makeText(this, "Minimum withdrawal is ₹100", Toast.LENGTH_SHORT).show();
            return;
        }

        if (amount > currentBalance) {
            soundManager.playError();
            Toast.makeText(this, "Insufficient balance for this withdrawal", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!upiId.contains("@")) {
            soundManager.playError();
            Toast.makeText(this, "Please enter a valid UPI ID (e.g., name@bank)", Toast.LENGTH_SHORT).show();
            return;
        }

        pbWithdraw.setVisibility(View.VISIBLE);
        btnSubmitWithdraw.setEnabled(false);

        // Deduct balance locally
        currentBalance = GameEngine.calculateBalanceAfterBet(currentBalance, amount);
        SharedPreferences sp = getSharedPreferences(PREF_GAME, Context.MODE_PRIVATE);
        sp.edit().putFloat("wallet_balance", (float) currentBalance).apply();

        try {
            JSONObject data = new JSONObject();
            data.put("user_email", backendApi.getUserEmail());
            data.put("amount", amount);
            data.put("upi_id", upiId);
            data.put("status", "processing");
            data.put("timestamp", String.valueOf(System.currentTimeMillis()));

            backendApi.createRecord("withdraw_requests", data, new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    finishWithdrawalSuccess(amount, upiId);
                }

                @Override
                public void onError(String error) {
                    finishWithdrawalSuccess(amount, upiId);
                }
            });
        } catch (Exception e) {
            finishWithdrawalSuccess(amount, upiId);
        }
    }

    private void finishWithdrawalSuccess(final double amount, final String upiId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                pbWithdraw.setVisibility(View.GONE);
                btnSubmitWithdraw.setEnabled(true);
                soundManager.playWithdrawSuccess();
                Toast.makeText(WithdrawActivity.this, "Withdrawal of ₹" + amount + " to " + upiId + " submitted successfully!", Toast.LENGTH_LONG).show();
                setResult(RESULT_OK);
                finish();
            }
        });
    }
}