package com.payquick.app;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final int REQ_AUTH = 101;

    private BackendApi mBackendApi;

    private TextView mTvMerchantHeader;
    private TextView mTvUserEmail;
    private TextView mTvWalletBalance;
    private TextView mTvUpiDisplay;
    private ImageView mIvMainQr;
    private View mCardWallet;
    private View mLayoutBonusBox;

    private EditText mEtDepositAmount;
    private TextView mTvCalcBonus;
    private TextView mTvCalcTotal;
    private Button mBtnDeposit;

    private LinearLayout mLayoutGallery;
    private LinearLayout mLayoutTxContainer;
    private TextView mTvEmptyTx;
    private ProgressBar mPbLoading;

    private double mCurrentBalance = 0.0;
    private double mDisplayedBalance = 0.0;
    private double mMaxDepositLimit = 50000.0;
    private ValueAnimator mBalanceAnimator;

    private static class ServiceItem {
        String name;
        double price;
        String imageUrl;

        ServiceItem(String name, double price, String imageUrl) {
            this.name = name;
            this.price = price;
            this.imageUrl = imageUrl;
        }
    }

    private final ServiceItem[] mServices = new ServiceItem[]{
            new ServiceItem("Coffee & Bakery", 4.50, "https://images.unsplash.com/photo-1509042239860-f550ce710b93?w=300&auto=format&fit=crop&q=80"),
            new ServiceItem("Supermarket Express", 25.00, "https://images.unsplash.com/photo-1542838132-92c53300491e?w=300&auto=format&fit=crop&q=80"),
            new ServiceItem("Digital Gift Card", 50.00, "https://images.unsplash.com/photo-1556742049-0a67c5574f73?w=300&auto=format&fit=crop&q=80"),
            new ServiceItem("Transit Metro Pass", 12.00, "https://images.unsplash.com/photo-1544620347-c4fd4a3d5957?w=300&auto=format&fit=crop&q=80"),
            new ServiceItem("Cinema & Entertainment", 15.00, "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=300&auto=format&fit=crop&q=80")
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBackendApi = new BackendApi(this);

        if (!mBackendApi.isLoggedIn()) {
            Intent intent = new Intent(this, AuthActivity.class);
            startActivityForResult(intent, REQ_AUTH);
            return;
        }

        initViews();
        fetchDynamicAppConfig();
        setupDynamicGallery();
        fetchTransactions();
    }

    private void initViews() {
        mTvMerchantHeader = findViewById(R.id.tv_merchant_header);
        mTvUserEmail = findViewById(R.id.tv_user_email);
        mTvWalletBalance = findViewById(R.id.tv_wallet_balance);
        mTvUpiDisplay = findViewById(R.id.tv_upi_display);
        mIvMainQr = findViewById(R.id.iv_main_qr);
        mCardWallet = findViewById(R.id.card_wallet_container);
        mLayoutBonusBox = findViewById(R.id.layout_calc_results_box);

        mEtDepositAmount = findViewById(R.id.et_deposit_amount);
        mTvCalcBonus = findViewById(R.id.tv_calc_bonus);
        mTvCalcTotal = findViewById(R.id.tv_calc_total);
        mBtnDeposit = findViewById(R.id.btn_deposit);

        mLayoutGallery = findViewById(R.id.layout_service_gallery);
        mLayoutTxContainer = findViewById(R.id.layout_transactions_container);
        mTvEmptyTx = findViewById(R.id.tv_empty_transactions);
        mPbLoading = findViewById(R.id.pb_loading);

        final Button btnLogout = findViewById(R.id.btn_logout);
        final Button btnRefresh = findViewById(R.id.btn_refresh_transactions);

        mTvUserEmail.setText(mBackendApi.getUserEmail());

        AnimationHelper.popIn(mCardWallet, 100);

        mIvMainQr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AnimationHelper.pulse(mIvMainQr);
                Toast.makeText(MainActivity.this, "Scan merchant QR to pay", Toast.LENGTH_SHORT).show();
            }
        });

        mEtDepositAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                calculateDepositBonus(s.toString().trim());
            }
        });

        mBtnDeposit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AnimationHelper.bounceClick(mBtnDeposit, new Runnable() {
                    @Override
                    public void run() {
                        executeDeposit();
                    }
                });
            }
        });

        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AnimationHelper.bounceClick(btnRefresh, new Runnable() {
                    @Override
                    public void run() {
                        fetchDynamicAppConfig();
                        fetchTransactions();
                    }
                });
            }
        });

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AnimationHelper.bounceClick(btnLogout, new Runnable() {
                    @Override
                    public void run() {
                        mBackendApi.logout(new BackendApi.ApiCallback() {
                            @Override
                            public void onSuccess(JSONObject response) {
                                launchAuth();
                            }

                            @Override
                            public void onError(String errorMessage) {
                                launchAuth();
                            }
                        });
                    }
                });
            }
        });
    }

    private void launchAuth() {
        Intent intent = new Intent(MainActivity.this, AuthActivity.class);
        startActivityForResult(intent, REQ_AUTH);
    }

    private void animateBalanceTo(final double targetBalance) {
        if (mBalanceAnimator != null && mBalanceAnimator.isRunning()) {
            mBalanceAnimator.cancel();
        }

        mBalanceAnimator = ValueAnimator.ofFloat((float) mDisplayedBalance, (float) targetBalance);
        mBalanceAnimator.setDuration(600);
        mBalanceAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (Float) animation.getAnimatedValue();
                mDisplayedBalance = value;
                mTvWalletBalance.setText(String.format(Locale.US, "$%.2f", value));
            }
        });
        mBalanceAnimator.start();
        AnimationHelper.pulse(mTvWalletBalance);
    }

    private void fetchDynamicAppConfig() {
        mBackendApi.fetchAppConfig(new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                JSONObject config = response.optJSONObject("config");
                if (config == null) {
                    config = response;
                }

                String merchant = config.optString("merchant_name", "PayQuick Super Store");
                String upi = config.optString("upi_id", "payquick@okhdfcbank");
                String qr = config.optString("payment_qr_code", "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?w=400&auto=format&fit=crop&q=80");
                mMaxDepositLimit = config.optDouble("max_deposit_limit", 50000.0);

                mTvMerchantHeader.setText(merchant);
                mTvUpiDisplay.setText("UPI: " + upi);
                ImageLoader.getInstance().displayImage(qr, mIvMainQr);
            }

            @Override
            public void onError(String errorMessage) {
                mTvMerchantHeader.setText("PayQuick Super Store");
                mTvUpiDisplay.setText("UPI: payquick@okhdfcbank");
                ImageLoader.getInstance().displayImage("https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?w=400&auto=format&fit=crop&q=80", mIvMainQr);
            }
        });
    }

    private void calculateDepositBonus(String input) {
        if (input.isEmpty()) {
            mTvCalcBonus.setText("+$0.00");
            mTvCalcTotal.setText("$0.00");
            return;
        }

        try {
            double deposit = Double.parseDouble(input);
            double bonus = WalletCalculator.calculateCashback(deposit);
            double total = WalletCalculator.calculateTotalCredit(deposit);

            mTvCalcBonus.setText(String.format(Locale.US, "+$%.2f", bonus));
            mTvCalcTotal.setText(String.format(Locale.US, "$%.2f", total));
            if (mLayoutBonusBox != null) {
                AnimationHelper.pulse(mLayoutBonusBox);
            }
        } catch (Exception e) {
            mTvCalcBonus.setText("+$0.00");
            mTvCalcTotal.setText("$0.00");
        }
    }

    private void executeDeposit() {
        String input = mEtDepositAmount.getText().toString().trim();
        if (input.isEmpty()) {
            AnimationHelper.shake(mEtDepositAmount);
            Toast.makeText(this, "Please enter deposit amount", Toast.LENGTH_SHORT).show();
            return;
        }

        final double deposit;
        try {
            deposit = Double.parseDouble(input);
        } catch (Exception e) {
            AnimationHelper.shake(mEtDepositAmount);
            Toast.makeText(this, "Invalid deposit amount", Toast.LENGTH_SHORT).show();
            return;
        }

        if (deposit <= 0) {
            AnimationHelper.shake(mEtDepositAmount);
            Toast.makeText(this, "Deposit amount must be greater than 0", Toast.LENGTH_SHORT).show();
            return;
        }

        if (deposit > mMaxDepositLimit) {
            AnimationHelper.shake(mEtDepositAmount);
            Toast.makeText(this, "Amount exceeds max deposit limit of $" + (int) mMaxDepositLimit, Toast.LENGTH_LONG).show();
            return;
        }

        final double cashback = WalletCalculator.calculateCashback(deposit);
        final double totalCredit = WalletCalculator.calculateTotalCredit(deposit);

        mBtnDeposit.setEnabled(false);

        try {
            JSONObject data = new JSONObject();
            data.put("title", "Wallet Deposit + Bonus");
            data.put("type", "credit");
            data.put("amount", deposit);
            data.put("cashback", cashback);
            data.put("status", "completed");
            data.put("timestamp", new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date()));

            mBackendApi.createRecord("transactions", data, new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    mBtnDeposit.setEnabled(true);
                    mEtDepositAmount.setText("");
                    Toast.makeText(MainActivity.this, String.format(Locale.US, "Added $%.2f (includes $%.2f bonus)", totalCredit, cashback), Toast.LENGTH_SHORT).show();
                    AnimationHelper.pulse(mCardWallet);
                    fetchTransactions();
                }

                @Override
                public void onError(String errorMessage) {
                    mBtnDeposit.setEnabled(true);
                    AnimationHelper.shake(mBtnDeposit);
                    Toast.makeText(MainActivity.this, "Deposit failed: " + errorMessage, Toast.LENGTH_LONG).show();
                }
            });
        } catch (Exception e) {
            mBtnDeposit.setEnabled(true);
            AnimationHelper.shake(mBtnDeposit);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void setupDynamicGallery() {
        mLayoutGallery.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (int i = 0; i < mServices.length; i++) {
            final ServiceItem item = mServices[i];
            final View card = inflater.inflate(R.layout.item_service_card, mLayoutGallery, false);

            ImageView ivThumb = card.findViewById(R.id.iv_service_thumb);
            TextView tvTitle = card.findViewById(R.id.tv_service_title);
            TextView tvPrice = card.findViewById(R.id.tv_service_price);
            final Button btnPay = card.findViewById(R.id.btn_service_pay);

            tvTitle.setText(item.name);
            tvPrice.setText(String.format(Locale.US, "$%.2f", item.price));
            ImageLoader.getInstance().displayImage(item.imageUrl, ivThumb);

            btnPay.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AnimationHelper.bounceClick(btnPay, new Runnable() {
                        @Override
                        public void run() {
                            handleServicePayment(item);
                        }
                    });
                }
            });

            mLayoutGallery.addView(card);
            AnimationHelper.popIn(card, i * 70);
        }
    }

    private void handleServicePayment(final ServiceItem item) {
        if (mCurrentBalance < item.price) {
            AnimationHelper.shake(mCardWallet);
            Toast.makeText(this, "Insufficient wallet balance. Please add funds!", Toast.LENGTH_LONG).show();
            return;
        }

        try {
            JSONObject data = new JSONObject();
            data.put("title", "Pay: " + item.name);
            data.put("type", "debit");
            data.put("amount", item.price);
            data.put("cashback", 0.0);
            data.put("status", "completed");
            data.put("timestamp", new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date()));

            mPbLoading.setVisibility(View.VISIBLE);
            mBackendApi.createRecord("transactions", data, new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    mPbLoading.setVisibility(View.GONE);
                    Toast.makeText(MainActivity.this, "Paid $" + item.price + " for " + item.name, Toast.LENGTH_SHORT).show();
                    AnimationHelper.pulse(mCardWallet);
                    fetchTransactions();
                }

                @Override
                public void onError(String errorMessage) {
                    mPbLoading.setVisibility(View.GONE);
                    Toast.makeText(MainActivity.this, "Payment error: " + errorMessage, Toast.LENGTH_LONG).show();
                }
            });
        } catch (Exception e) {
            Toast.makeText(this, "Payment creation error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchTransactions() {
        mPbLoading.setVisibility(View.VISIBLE);
        mBackendApi.getRecords("transactions", new BackendApi.DataCallback() {
            @Override
            public void onSuccess(JSONArray records) {
                mPbLoading.setVisibility(View.GONE);
                renderTransactions(records);
            }

            @Override
            public void onError(String errorMessage) {
                mPbLoading.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderTransactions(JSONArray records) {
        mLayoutTxContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        double computedBalance = 0.0;

        if (records == null || records.length() == 0) {
            mTvEmptyTx.setVisibility(View.VISIBLE);
            mCurrentBalance = 0.0;
            animateBalanceTo(0.0);
            return;
        }

        mTvEmptyTx.setVisibility(View.GONE);

        int count = 0;
        for (int i = records.length() - 1; i >= 0; i--) {
            try {
                JSONObject rec = records.getJSONObject(i);
                JSONObject data = rec.optJSONObject("data");
                if (data == null) continue;

                String title = data.optString("title", "Transaction");
                String type = data.optString("type", "credit");
                double amount = data.optDouble("amount", 0.0);
                double cashback = data.optDouble("cashback", 0.0);
                String timestamp = data.optString("timestamp", "");

                if ("credit".equalsIgnoreCase(type)) {
                    computedBalance += (amount + cashback);
                } else {
                    computedBalance -= amount;
                }

                View view = inflater.inflate(R.layout.item_transaction, mLayoutTxContainer, false);
                TextView tvIcon = view.findViewById(R.id.tv_tx_icon);
                TextView tvTitle = view.findViewById(R.id.tv_tx_title);
                TextView tvTime = view.findViewById(R.id.tv_tx_time);
                TextView tvAmount = view.findViewById(R.id.tv_tx_amount);
                TextView tvBonus = view.findViewById(R.id.tv_tx_bonus);

                tvTitle.setText(title);
                tvTime.setText(timestamp.isEmpty() ? "Recent" : timestamp);

                if ("credit".equalsIgnoreCase(type)) {
                    tvIcon.setText("↓");
                    tvIcon.setTextColor(0xFF38BDF8);
                    tvAmount.setText(String.format(Locale.US, "+$%.2f", amount + cashback));
                    tvAmount.setTextColor(0xFF4ADE80);
                    if (cashback > 0) {
                        tvBonus.setText(String.format(Locale.US, "Cashback: $%.2f", cashback));
                        tvBonus.setVisibility(View.VISIBLE);
                    } else {
                        tvBonus.setVisibility(View.GONE);
                    }
                } else {
                    tvIcon.setText("↑");
                    tvIcon.setTextColor(0xFFF87171);
                    tvAmount.setText(String.format(Locale.US, "-$%.2f", amount));
                    tvAmount.setTextColor(0xFFF87171);
                    tvBonus.setVisibility(View.GONE);
                }

                mLayoutTxContainer.addView(view);
                AnimationHelper.entranceSlideUp(view, count * 50);
                count++;

            } catch (Exception ignored) {}
        }

        mCurrentBalance = WalletCalculator.round(Math.max(0.0, computedBalance));
        animateBalanceTo(mCurrentBalance);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_AUTH) {
            if (resultCode == RESULT_OK) {
                initViews();
                fetchDynamicAppConfig();
                setupDynamicGallery();
                fetchTransactions();
            } else {
                finish();
            }
        }
    }
}