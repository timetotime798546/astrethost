package com.quickpaywallet.app;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeActivity extends Activity {

    private TextView tvWalletBalance, tvCashbackBanner, tvMerchantName, tvMerchantUpi;
    private TextView tvDepositLimitHint, tvCalculatedCashback, tvTotalCreditedCalc;
    private TextView tvUserEmailDisplay, tvEmptyTransactions;
    private ImageView ivPaymentQr;
    private EditText etDepositAmount, etUtrNumber;
    private Button btnCopyUpi, btnSubmitRecharge, btnLogout;
    private ProgressBar pbRechargeLoading;
    private LinearLayout layoutTransactionsList;

    private BackendApi api;
    private AdminConfigManager.ConfigData currentConfig;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        api = new BackendApi(this);
        currentConfig = new AdminConfigManager.ConfigData();

        initViews();
        bindEvents();

        // Display user email
        tvUserEmailDisplay.setText(api.getSavedEmail());

        // Fetch dynamic admin configuration with 1-hour caching rule
        fetchAdminConfig();

        // Fetch transaction records from backend
        fetchTransactions();
    }

    private void initViews() {
        tvWalletBalance = (TextView) findViewById(R.id.tvWalletBalance);
        tvCashbackBanner = (TextView) findViewById(R.id.tvCashbackBanner);
        tvMerchantName = (TextView) findViewById(R.id.tvMerchantName);
        tvMerchantUpi = (TextView) findViewById(R.id.tvMerchantUpi);
        tvDepositLimitHint = (TextView) findViewById(R.id.tvDepositLimitHint);
        tvCalculatedCashback = (TextView) findViewById(R.id.tvCalculatedCashback);
        tvTotalCreditedCalc = (TextView) findViewById(R.id.tvTotalCreditedCalc);
        tvUserEmailDisplay = (TextView) findViewById(R.id.tvUserEmailDisplay);
        tvEmptyTransactions = (TextView) findViewById(R.id.tvEmptyTransactions);

        ivPaymentQr = (ImageView) findViewById(R.id.ivPaymentQr);
        etDepositAmount = (EditText) findViewById(R.id.etDepositAmount);
        etUtrNumber = (EditText) findViewById(R.id.etUtrNumber);

        btnCopyUpi = (Button) findViewById(R.id.btnCopyUpi);
        btnSubmitRecharge = (Button) findViewById(R.id.btnSubmitRecharge);
        btnLogout = (Button) findViewById(R.id.btnLogout);

        pbRechargeLoading = (ProgressBar) findViewById(R.id.pbRechargeLoading);
        layoutTransactionsList = (LinearLayout) findViewById(R.id.layoutTransactionsList);
    }

    private void bindEvents() {
        // Copy Merchant UPI ID to clipboard
        btnCopyUpi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("UPI ID", currentConfig.merchantUpiId);
                if (clipboard != null) {
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(HomeActivity.this, "UPI ID copied: " + currentConfig.merchantUpiId, Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Real-time Offline Calculation on Amount Input Change
        etDepositAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                calculateLiveRecharge();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Submit Recharge Form
        btnSubmitRecharge.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitRecharge();
            }
        });

        // Logout
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performLogout();
            }
        });
    }

    private void fetchAdminConfig() {
        AdminConfigManager.loadConfig(this, new AdminConfigManager.ConfigCallback() {
            @Override
            public void onConfigLoaded(AdminConfigManager.ConfigData config) {
                currentConfig = config;
                applyConfigToUI();
            }
        });
    }

    private void applyConfigToUI() {
        tvMerchantName.setText("Merchant: " + currentConfig.merchantName);
        tvMerchantUpi.setText(currentConfig.merchantUpiId);
        tvDepositLimitHint.setText(String.format(Locale.US, "Deposit Range: $%.0f - $%.0f",
                currentConfig.minDepositLimit, currentConfig.maxDepositLimit));
        tvCashbackBanner.setText(String.format(Locale.US, "★ %.1f%% Cashback on All Recharges",
                currentConfig.cashbackPercent));

        // Re-calculate with updated cashback %
        calculateLiveRecharge();

        // Load QR Code Image
        if (currentConfig.qrCodeUrl != null && !currentConfig.qrCodeUrl.isEmpty()) {
            loadQrCodeImage(currentConfig.qrCodeUrl);
        }
    }

    private void loadQrCodeImage(final String imageUrl) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(imageUrl);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setConnectTimeout(6000);
                    connection.setReadTimeout(6000);
                    connection.connect();
                    InputStream input = connection.getInputStream();
                    final Bitmap bitmap = BitmapFactory.decodeStream(input);
                    input.close();
                    connection.disconnect();

                    if (bitmap != null) {
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                ivPaymentQr.setImageBitmap(bitmap);
                            }
                        });
                    }
                } catch (Exception ignored) {
                }
            }
        }).start();
    }

    /**
     * Mandatory Native Java calculation for live form inputs
     */
    private void calculateLiveRecharge() {
        String inputStr = etDepositAmount.getText().toString().trim();
        double amount = 0.0;
        try {
            if (!inputStr.isEmpty()) {
                amount = Double.parseDouble(inputStr);
            }
        } catch (Exception ignored) {}

        double cashback = CalculationHelper.calculateCashback(amount, currentConfig.cashbackPercent);
        double totalCredited = CalculationHelper.calculateTotalCredit(amount, cashback);

        tvCalculatedCashback.setText(String.format(Locale.US, "+$%.2f", cashback));
        tvTotalCreditedCalc.setText(String.format(Locale.US, "$%.2f", totalCredited));
    }

    private void submitRecharge() {
        String inputStr = etDepositAmount.getText().toString().trim();
        String utr = etUtrNumber.getText().toString().trim();

        if (inputStr.isEmpty()) {
            Toast.makeText(this, "Please enter deposit amount", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(inputStr);
        } catch (Exception e) {
            Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!CalculationHelper.isWithinLimits(amount, currentConfig.minDepositLimit, currentConfig.maxDepositLimit)) {
            Toast.makeText(this, String.format(Locale.US, "Amount must be between $%.0f and $%.0f",
                    currentConfig.minDepositLimit, currentConfig.maxDepositLimit), Toast.LENGTH_LONG).show();
            return;
        }

        if (utr.length() < 6) {
            Toast.makeText(this, "Please enter a valid reference / UTR number", Toast.LENGTH_SHORT).show();
            return;
        }

        double cashback = CalculationHelper.calculateCashback(amount, currentConfig.cashbackPercent);
        double totalCredited = CalculationHelper.calculateTotalCredit(amount, cashback);

        pbRechargeLoading.setVisibility(View.VISIBLE);
        btnSubmitRecharge.setEnabled(false);

        try {
            JSONObject data = new JSONObject();
            data.put("amount", amount);
            data.put("cashback", cashback);
            data.put("total_credited", totalCredited);
            data.put("utr_number", utr);
            data.put("upi_id", currentConfig.merchantUpiId);
            data.put("status", "Completed");

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
            data.put("created_time", sdf.format(new Date()));

            api.createData("transactions", data, new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    pbRechargeLoading.setVisibility(View.GONE);
                    btnSubmitRecharge.setEnabled(true);
                    etDepositAmount.setText("");
                    etUtrNumber.setText("");
                    Toast.makeText(HomeActivity.this, "Recharge submitted successfully!", Toast.LENGTH_SHORT).show();

                    // Refresh transactions and balance
                    fetchTransactions();
                }

                @Override
                public void onError(String errorMessage) {
                    pbRechargeLoading.setVisibility(View.GONE);
                    btnSubmitRecharge.setEnabled(true);
                    Toast.makeText(HomeActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
            });

        } catch (Exception e) {
            pbRechargeLoading.setVisibility(View.GONE);
            btnSubmitRecharge.setEnabled(true);
            Toast.makeText(this, "Submission error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Reads backend records from GET /data?collection=transactions
     * and performs local balance aggregation in native Java.
     */
    private void fetchTransactions() {
        api.readData("transactions", new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                List<Double> creditAmounts = new ArrayList<Double>();
                JSONArray records = response.optJSONArray("records");

                layoutTransactionsList.removeAllViews();

                if (records != null && records.length() > 0) {
                    tvEmptyTransactions.setVisibility(View.GONE);
                    LayoutInflater inflater = LayoutInflater.from(HomeActivity.this);

                    for (int i = records.length() - 1; i >= 0; i--) {
                        JSONObject record = records.optJSONObject(i);
                        if (record != null) {
                            JSONObject data = record.optJSONObject("data");
                            if (data != null) {
                                double credited = data.optDouble("total_credited", data.optDouble("amount", 0.0));
                                creditAmounts.add(credited);

                                View itemView = inflater.inflate(R.layout.item_transaction, layoutTransactionsList, false);

                                TextView tvAmt = (TextView) itemView.findViewById(R.id.tvItemAmount);
                                TextView tvCb = (TextView) itemView.findViewById(R.id.tvItemCashback);
                                TextView tvStat = (TextView) itemView.findViewById(R.id.tvItemStatus);
                                TextView tvUtr = (TextView) itemView.findViewById(R.id.tvItemUtr);
                                TextView tvDt = (TextView) itemView.findViewById(R.id.tvItemDate);

                                tvAmt.setText(String.format(Locale.US, "+$%.2f", credited));
                                tvCb.setText(String.format(Locale.US, "Bonus: $%.2f", data.optDouble("cashback", 0.0)));
                                tvStat.setText(data.optString("status", "Completed"));
                                tvUtr.setText("UTR: " + data.optString("utr_number", "-"));
                                tvDt.setText(data.optString("created_time", "Recent"));

                                layoutTransactionsList.addView(itemView);
                            }
                        }
                    }
                } else {
                    layoutTransactionsList.addView(tvEmptyTransactions);
                    tvEmptyTransactions.setVisibility(View.VISIBLE);
                }

                // Mandatory Offline Calculation: Aggregate total wallet balance
                double totalWalletBalance = CalculationHelper.calculateTotalBalance(creditAmounts);
                tvWalletBalance.setText(String.format(Locale.US, "$%.2f", totalWalletBalance));
            }

            @Override
            public void onError(String errorMessage) {
                // If offline or empty, keep current balance
            }
        });
    }

    private void performLogout() {
        api.logout(new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                navigateToLogin();
            }

            @Override
            public void onError(String errorMessage) {
                navigateToLogin();
            }
        });
    }

    private void navigateToLogin() {
        Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}