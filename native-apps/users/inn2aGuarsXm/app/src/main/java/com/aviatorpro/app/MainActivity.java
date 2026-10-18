package com.aviatorpro.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements AviatorFlightView.FlightListener {

    private static final int REQ_DEPOSIT = 102;
    private static final int REQ_WITHDRAW = 103;
    private static final String PREF_GAME = "aviator_game_prefs";

    private AviatorFlightView flightView;
    private TextView tvWalletBalance, tvBetAmountDisplay, tvWinNotice;
    private Button btnNavDeposit, btnNavWithdraw, btnUserAuth;
    private Button btnPreset100, btnPreset200, btnPreset500, btnPreset1000;
    private Button btnMinusBet, btnPlusBet, btnBetOrCashout;
    private LinearLayout layoutWinNotification, layoutHistoryBadges;

    private double walletBalance = 0.0;
    private int currentBet = 100;
    private boolean hasActiveBet = false;
    private boolean isCashedOut = false;
    private double activeBetAmount = 0.0;

    private Handler handler;
    private BackendApi backendApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        backendApi = new BackendApi(this);

        // Security check: App MUST be logged in with email and password first
        if (!backendApi.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        handler = new Handler(Looper.getMainLooper());

        flightView = (AviatorFlightView) findViewById(R.id.flightView);
        tvWalletBalance = (TextView) findViewById(R.id.tvWalletBalance);
        tvBetAmountDisplay = (TextView) findViewById(R.id.tvBetAmountDisplay);
        tvWinNotice = (TextView) findViewById(R.id.tvWinNotice);
        btnNavDeposit = (Button) findViewById(R.id.btnNavDeposit);
        btnNavWithdraw = (Button) findViewById(R.id.btnNavWithdraw);
        btnUserAuth = (Button) findViewById(R.id.btnUserAuth);
        btnPreset100 = (Button) findViewById(R.id.btnPreset100);
        btnPreset200 = (Button) findViewById(R.id.btnPreset200);
        btnPreset500 = (Button) findViewById(R.id.btnPreset500);
        btnPreset1000 = (Button) findViewById(R.id.btnPreset1000);
        btnMinusBet = (Button) findViewById(R.id.btnMinusBet);
        btnPlusBet = (Button) findViewById(R.id.btnPlusBet);
        btnBetOrCashout = (Button) findViewById(R.id.btnBetOrCashout);
        layoutWinNotification = (LinearLayout) findViewById(R.id.layoutWinNotification);
        layoutHistoryBadges = (LinearLayout) findViewById(R.id.layoutHistoryBadges);

        flightView.setFlightListener(this);

        loadWallet();
        updateWalletUI();
        updateBetDisplay();

        setupControls();

        // Start initial round countdown
        scheduleNextRound(2500);
    }

    private void setupControls() {
        btnPreset100.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setBetAmount(100);
            }
        });
        btnPreset200.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setBetAmount(200);
            }
        });
        btnPreset500.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setBetAmount(500);
            }
        });
        btnPreset1000.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setBetAmount(1000);
            }
        });

        btnMinusBet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentBet > 100) {
                    setBetAmount(currentBet - 100);
                }
            }
        });

        btnPlusBet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setBetAmount(currentBet + 100);
            }
        });

        btnBetOrCashout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleBetOrCashoutClick();
            }
        });

        // Deposit screen
        btnNavDeposit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(MainActivity.this, DepositActivity.class), REQ_DEPOSIT);
            }
        });

        // Withdraw screen
        btnNavWithdraw.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(MainActivity.this, WithdrawActivity.class), REQ_WITHDRAW);
            }
        });

        // User Account / Logout Action
        btnUserAuth.setText("LOGOUT (" + backendApi.getUserEmail() + ")");
        btnUserAuth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                backendApi.clearAuth();
                Toast.makeText(MainActivity.this, "Logged out successfully", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });
    }

    private void setBetAmount(int amount) {
        if (!hasActiveBet) {
            currentBet = amount;
            updateBetDisplay();
        }
    }

    private void updateBetDisplay() {
        tvBetAmountDisplay.setText("₹" + currentBet);
        if (!hasActiveBet) {
            btnBetOrCashout.setText("BET ₹" + currentBet);
            btnBetOrCashout.setBackgroundColor(Color.parseColor("#26A65B"));
        }
    }

    private void handleBetOrCashoutClick() {
        if (!flightView.isFlying()) {
            // Place Bet for next round
            if (walletBalance < currentBet) {
                Toast.makeText(this, "Low balance! Please Deposit funds to place bet", Toast.LENGTH_SHORT).show();
                startActivityForResult(new Intent(this, DepositActivity.class), REQ_DEPOSIT);
                return;
            }

            hasActiveBet = true;
            isCashedOut = false;
            activeBetAmount = currentBet;

            // Deduct balance locally
            walletBalance = GameEngine.calculateBalanceAfterBet(walletBalance, activeBetAmount);
            saveWallet();
            updateWalletUI();

            btnBetOrCashout.setText("WAITING FOR TAKEOFF...");
            btnBetOrCashout.setBackgroundColor(Color.parseColor("#F5A623"));
        } else {
            // In Flight: Perform Cash Out!
            if (hasActiveBet && !isCashedOut) {
                double mult = flightView.getCurrentMultiplier();
                double payout = GameEngine.calculateCashout(activeBetAmount, mult);

                isCashedOut = true;
                hasActiveBet = false;

                // Add winnings to wallet
                walletBalance = GameEngine.calculateBalanceAfterWin(walletBalance, payout);
                saveWallet();
                updateWalletUI();

                // Show celebratory popup
                layoutWinNotification.setVisibility(View.VISIBLE);
                tvWinNotice.setText(String.format("CASHED OUT! ₹%.2f (%.2fx)", payout, mult));

                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        layoutWinNotification.setVisibility(View.GONE);
                    }
                }, 3000);

                btnBetOrCashout.setText("BET ₹" + currentBet);
                btnBetOrCashout.setBackgroundColor(Color.parseColor("#26A65B"));
            }
        }
    }

    private void scheduleNextRound(long delayMillis) {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                startNewRound();
            }
        }, delayMillis);
    }

    private void startNewRound() {
        flightView.resetForNextRound();

        // Standard 70% House Edge
        int margin = 70;
        double nextCrash = GameEngine.generateCrashPoint(margin);

        if (hasActiveBet && !isCashedOut) {
            btnBetOrCashout.setText(String.format("CASH OUT ₹%.2f", activeBetAmount));
            btnBetOrCashout.setBackgroundColor(Color.parseColor("#E51B38"));
        } else {
            btnBetOrCashout.setText("BET ₹" + currentBet);
            btnBetOrCashout.setBackgroundColor(Color.parseColor("#26A65B"));
        }

        flightView.startFlight(nextCrash);
    }

    @Override
    public void onMultiplierUpdate(double currentMultiplier) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (hasActiveBet && !isCashedOut) {
                    double liveCashout = GameEngine.calculateCashout(activeBetAmount, flightView.getCurrentMultiplier());
                    btnBetOrCashout.setText(String.format("CASH OUT ₹%.2f", liveCashout));
                    btnBetOrCashout.setBackgroundColor(Color.parseColor("#E51B38"));
                }
            }
        });
    }

    @Override
    public void onCrashed(final double finalMultiplier) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (hasActiveBet && !isCashedOut) {
                    // Bet Lost to House
                    hasActiveBet = false;
                    Toast.makeText(MainActivity.this, "Plane flew away! Bet lost.", Toast.LENGTH_SHORT).show();
                }

                addHistoryBadge(finalMultiplier);

                btnBetOrCashout.setText("BET ₹" + currentBet);
                btnBetOrCashout.setBackgroundColor(Color.parseColor("#26A65B"));

                // Next round in 4 seconds
                scheduleNextRound(4000);
            }
        });
    }

    private void addHistoryBadge(double multiplier) {
        TextView badge = new TextView(this);
        badge.setText(String.format("%.2fx", multiplier));
        badge.setTextSize(12);
        badge.setTypeface(null, android.graphics.Typeface.BOLD);
        badge.setPadding(16, 6, 16, 6);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(0, 0, 12, 0);
        badge.setLayoutParams(lp);

        if (multiplier >= 2.0) {
            badge.setTextColor(Color.parseColor("#26A65B"));
            badge.setBackgroundColor(Color.parseColor("#1B2E24"));
        } else {
            badge.setTextColor(Color.parseColor("#E51B38"));
            badge.setBackgroundColor(Color.parseColor("#2E1B22"));
        }

        layoutHistoryBadges.addView(badge, 0);
        if (layoutHistoryBadges.getChildCount() > 10) {
            layoutHistoryBadges.removeViewAt(10);
        }
    }

    private void loadWallet() {
        SharedPreferences sp = getSharedPreferences(PREF_GAME, Context.MODE_PRIVATE);
        walletBalance = sp.getFloat("wallet_balance", 0.0f);
    }

    private void saveWallet() {
        SharedPreferences sp = getSharedPreferences(PREF_GAME, Context.MODE_PRIVATE);
        sp.edit().putFloat("wallet_balance", (float) walletBalance).apply();
    }

    private void updateWalletUI() {
        tvWalletBalance.setText(String.format("%.2f", walletBalance));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ((requestCode == REQ_DEPOSIT || requestCode == REQ_WITHDRAW) && resultCode == RESULT_OK) {
            loadWallet();
            updateWalletUI();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!backendApi.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        loadWallet();
        updateWalletUI();
    }
}