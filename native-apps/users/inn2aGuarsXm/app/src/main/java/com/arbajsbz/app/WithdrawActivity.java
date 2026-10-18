package com.arbajsbz.app;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class WithdrawActivity extends Activity {

    private TextView tvEligible;
    private EditText etAmount;
    private RadioGroup rgMethod;
    private RadioButton rbUpi;
    private RadioButton rbBank;
    private TextView tvRecipientLabel;
    private EditText etRecipientDetail;
    private Button btnSubmit;

    private BackendApi backendApi;
    private SoundManager soundManager;

    private double availableBalance = 0.0;
    private double bonusBalance = 0.0;
    private String walletRecordId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_withdraw);

        backendApi = new BackendApi(this);
        soundManager = SoundManager.getInstance(this);

        tvEligible = (TextView) findViewById(R.id.tv_eligible_withdraw);
        etAmount = (EditText) findViewById(R.id.et_withdraw_amount);
        rgMethod = (RadioGroup) findViewById(R.id.rg_payout_type);
        rbUpi = (RadioButton) findViewById(R.id.rb_upi);
        rbBank = (RadioButton) findViewById(R.id.rb_bank);
        tvRecipientLabel = (TextView) findViewById(R.id.tv_recipient_label);
        etRecipientDetail = (EditText) findViewById(R.id.et_recipient_detail);
        btnSubmit = (Button) findViewById(R.id.btn_submit_withdraw);

        findViewById(R.id.btn_withdraw_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        rgMethod.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.rb_upi) {
                    tvRecipientLabel.setText("Enter UPI ID (e.g. pilot@okhdfcbank)");
                    etRecipientDetail.setHint("pilot@upi");
                } else {
                    tvRecipientLabel.setText("Enter Bank A/C & IFSC (e.g. 1029384756, SBIN0001)");
                    etRecipientDetail.setHint("A/C Number, IFSC Code");
                }
            }
        });

        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                soundManager.playButtonClick();
                processWithdrawal();
            }
        });

        loadBalance();
    }

    private void loadBalance() {
        backendApi.readRecords("wallets", new BackendApi.ReadCallback() {
            @Override
            public void onSuccess(JSONArray records) {
                try {
                    String email = backendApi.getUserEmail();
                    for (int i = 0; i < records.length(); i++) {
                        JSONObject rec = records.getJSONObject(i);
                        JSONObject data = rec.optJSONObject("data");
                        if (data != null && data.optString("email", "").equalsIgnoreCase(email)) {
                            walletRecordId = rec.optString("id");
                            availableBalance = data.optDouble("deposited_balance", 0.0);
                            bonusBalance = data.optDouble("bonus_balance", 0.0);
                            break;
                        }
                    }
                    tvEligible.setText("₹ " + String.format(Locale.US, "%.2f", availableBalance));
                } catch (Exception ignored) {}
            }

            @Override
            public void onError(String message) {}
        });
    }

    private void processWithdrawal() {
        String amtStr = etAmount.getText().toString().trim();
        final String details = etRecipientDetail.getText().toString().trim();

        if (amtStr.isEmpty() || details.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        final double amount;
        try {
            amount = Double.parseDouble(amtStr);
            if (amount < 500) {
                Toast.makeText(this, "Minimum withdrawal limit is ₹500", Toast.LENGTH_SHORT).show();
                return;
            }
            if (amount > availableBalance) {
                Toast.makeText(this, "Withdrawal amount exceeds eligible balance", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (Exception e) {
            Toast.makeText(this, "Invalid withdrawal amount", Toast.LENGTH_SHORT).show();
            return;
        }

        final ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Creating withdrawal request...");
        pd.show();

        // 1. Deduct from wallet
        availableBalance -= amount;
        tvEligible.setText("₹ " + String.format(Locale.US, "%.2f", availableBalance));

        try {
            JSONObject walletUpdate = new JSONObject();
            walletUpdate.put("email", backendApi.getUserEmail());
            walletUpdate.put("deposited_balance", availableBalance);
            walletUpdate.put("bonus_balance", bonusBalance);

            if (walletRecordId != null) {
                backendApi.updateRecord(walletRecordId, walletUpdate, new BackendApi.ApiCallback() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        recordWithdrawalRecord(amount, details, pd);
                    }

                    @Override
                    public void onError(String message) {
                        recordWithdrawalRecord(amount, details, pd);
                    }
                });
            } else {
                recordWithdrawalRecord(amount, details, pd);
            }
        } catch (Exception e) {
            pd.dismiss();
        }
    }

    private void recordWithdrawalRecord(final double amount, String details, final ProgressDialog pd) {
        try {
            final String txRef = "WTH-" + System.currentTimeMillis();
            JSONObject wth = new JSONObject();
            wth.put("email", backendApi.getUserEmail());
            wth.put("tx_ref", txRef);
            wth.put("amount", amount);
            wth.put("payout_method", rbUpi.isChecked() ? "UPI" : "BANK_ACCOUNT");
            wth.put("recipient_detail", details);
            wth.put("fee", 0.0);
            wth.put("net_amount", amount);
            wth.put("status", "PENDING");
            wth.put("timestamp", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date()));

            backendApi.createRecord("withdrawals", wth, new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    // Record in transaction log
                    createTransactionEntry(txRef, amount, pd);
                }

                @Override
                public void onError(String message) {
                    pd.dismiss();
                    Toast.makeText(WithdrawActivity.this, "Request failed: " + message, Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            pd.dismiss();
        }
    }

    private void createTransactionEntry(String ref, double amount, final ProgressDialog pd) {
        try {
            JSONObject tx = new JSONObject();
            tx.put("email", backendApi.getUserEmail());
            tx.put("tx_ref", ref);
            tx.put("category", "WITHDRAWAL");
            tx.put("amount", -amount);
            tx.put("status", "PENDING");
            tx.put("details", "Payout pending administrative verification");
            tx.put("timestamp", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date()));

            backendApi.createRecord("transactions", tx, new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    pd.dismiss();
                    Toast.makeText(WithdrawActivity.this, "Withdrawal Request Submitted (Pending Review)", Toast.LENGTH_LONG).show();
                    finish();
                }

                @Override
                public void onError(String message) {
                    pd.dismiss();
                    finish();
                }
            });
        } catch (Exception ignored) {
            pd.dismiss();
        }
    }
}