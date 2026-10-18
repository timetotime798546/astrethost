package com.arbajsbz.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends Activity {

    private BackendApi backendApi;
    private SoundManager soundManager;

    private AviationFlightView flightCanvas;
    private TextView tvMultiplier;
    private TextView tvRoundStatus;
    private TextView tvMainBalance;
    private EditText etBetAmount;
    private Button btnActionGame;

    private TextView tvHist1;
    private TextView tvHist2;
    private TextView tvHist3;
    private TextView tvHist4;

    private final Handler gameHandler = new Handler();
    private boolean isRoundActive = false;
    private boolean isWaitingNextRound = true;
    private boolean hasActiveBet = false;
    private boolean hasCashedOut = false;

    private float currentMultiplier = 1.00f;
    private float crashPoint = 2.15f;
    private double currentBetAmount = 100.0;
    private double currentBalance = 0.0;
    private double bonusBalance = 0.0;
    private String walletRecordId = null;

    private final Random random = new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        backendApi = new BackendApi(this);
        soundManager = SoundManager.getInstance(this);

        if (!backendApi.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        initViews();
        setupListeners();
        loadWalletData();
        startNextRoundCountdown(3);
    }

    private void initViews() {
        flightCanvas = (AviationFlightView) findViewById(R.id.flight_canvas);
        tvMultiplier = (TextView) findViewById(R.id.tv_multiplier);
        tvRoundStatus = (TextView) findViewById(R.id.tv_round_status);
        tvMainBalance = (TextView) findViewById(R.id.tv_main_balance);
        etBetAmount = (EditText) findViewById(R.id.et_bet_amount);
        btnActionGame = (Button) findViewById(R.id.btn_action_game);

        tvHist1 = (TextView) findViewById(R.id.tv_hist_1);
        tvHist2 = (TextView) findViewById(R.id.tv_hist_2);
        tvHist3 = (TextView) findViewById(R.id.tv_hist_3);
        tvHist4 = (TextView) findViewById(R.id.tv_hist_4);
    }

    private void setupListeners() {
        findViewById(R.id.btn_open_wallet).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                soundManager.playButtonClick();
                startActivity(new Intent(MainActivity.this, WalletActivity.class));
            }
        });

        findViewById(R.id.btn_menu_settings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                soundManager.playButtonClick();
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            }
        });

        findViewById(R.id.nav_wallet).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                soundManager.playButtonClick();
                startActivity(new Intent(MainActivity.this, WalletActivity.class));
            }
        });

        findViewById(R.id.nav_withdraw).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                soundManager.playButtonClick();
                startActivity(new Intent(MainActivity.this, WithdrawActivity.class));
            }
        });

        findViewById(R.id.nav_history).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                soundManager.playButtonClick();
                startActivity(new Intent(MainActivity.this, TransactionsActivity.class));
            }
        });

        findViewById(R.id.nav_admin).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                soundManager.playButtonClick();
                startActivity(new Intent(MainActivity.this, AdminDashboardActivity.class));
            }
        });

        // Quick Bet Chips
        findViewById(R.id.btn_bet_100).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                soundManager.playButtonClick();
                etBetAmount.setText("100");
            }
        });

        findViewById(R.id.btn_bet_200).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                soundManager.playButtonClick();
                etBetAmount.setText("200");
            }
        });

        findViewById(R.id.btn_bet_500).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                soundManager.playButtonClick();
                etBetAmount.setText("500");
            }
        });

        findViewById(R.id.btn_bet_1000).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                soundManager.playButtonClick();
                etBetAmount.setText("1000");
            }
        });

        btnActionGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                soundManager.playButtonClick();
                handleGameAction();
            }
        });
    }

    private void handleGameAction() {
        if (!isRoundActive) {
            // Place Bet for next round
            try {
                double bet = Double.parseDouble(etBetAmount.getText().toString().trim());
                if (bet <= 0) {
                    Toast.makeText(this, "Enter valid bet amount", Toast.LENGTH_SHORT).show();
                    return;
                }
                double totalAvailable = currentBalance + bonusBalance;
                if (bet > totalAvailable) {
                    Toast.makeText(this, "Insufficient balance! Please deposit.", Toast.LENGTH_SHORT).show();
                    return;
                }

                currentBetAmount = bet;
                hasActiveBet = true;
                hasCashedOut = false;

                // Deduct locally and sync
                deductBetBalance(bet);

                btnActionGame.setText("BET QUEUED (₹" + (int) bet + ")");
                btnActionGame.setBackgroundResource(R.drawable.bg_chip);
                btnActionGame.setEnabled(false);

                Toast.makeText(this, "Entry Confirmed for Flight!", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Invalid number", Toast.LENGTH_SHORT).show();
            }
        } else {
            // Cash Out during active flight
            if (hasActiveBet && !hasCashedOut) {
                performCashout();
            }
        }
    }

    private void performCashout() {
        hasCashedOut = true;
        soundManager.playWinCashout();
        final double payout = currentBetAmount * currentMultiplier;

        // Credit to wallet
        creditWinning(payout);

        btnActionGame.setText("WON ₹" + String.format(Locale.US, "%.2f", payout));
        btnActionGame.setBackgroundResource(R.drawable.bg_chip);
        btnActionGame.setEnabled(false);

        Toast.makeText(this, "CASHOUT SUCCESS! Multiplier: " + String.format(Locale.US, "%.2fx", currentMultiplier), Toast.LENGTH_LONG).show();

        // Record Game Round
        recordGameResult(currentMultiplier, payout, "WON");
    }

    private void deductBetBalance(double amount) {
        if (bonusBalance >= amount) {
            bonusBalance -= amount;
        } else {
            double remainder = amount - bonusBalance;
            bonusBalance = 0;
            currentBalance -= remainder;
        }
        updateBalanceDisplay();
        syncWalletToBackend();

        // Record Transaction
        recordTransaction("GAME_ENTRY", -amount, "Flight Entry Bet");
    }

    private void creditWinning(double payout) {
        currentBalance += payout;
        updateBalanceDisplay();
        syncWalletToBackend();

        recordTransaction("GAME_CASHOUT", payout, "Aviation Cashout Win at " + String.format(Locale.US, "%.2fx", currentMultiplier));
    }

    private void startNextRoundCountdown(final int secondsLeft) {
        isWaitingNextRound = true;
        isRoundActive = false;
        hasCashedOut = false;

        if (secondsLeft > 0) {
            tvRoundStatus.setText("TAKING OFF IN " + secondsLeft + "s");
            soundManager.playCountdown();
            gameHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    startNextRoundCountdown(secondsLeft - 1);
                }
            }, 1000);
        } else {
            launchFlightRound();
        }
    }

    private void launchFlightRound() {
        isWaitingNextRound = false;
        isRoundActive = true;
        currentMultiplier = 1.00f;
        flightCanvas.reset();
        soundManager.playTakeoff();

        // Generate crash multiplier
        crashPoint = 1.10f + (float) (random.nextDouble() * 3.50);

        tvRoundStatus.setText("FLIGHT ACTIVE");

        if (hasActiveBet) {
            btnActionGame.setText("CASHOUT (1.00x)");
            btnActionGame.setBackgroundResource(R.drawable.bg_green_button);
            btnActionGame.setEnabled(true);
        } else {
            btnActionGame.setText("WAITING NEXT FLIGHT");
            btnActionGame.setBackgroundResource(R.drawable.bg_chip);
            btnActionGame.setEnabled(false);
        }

        runFlightLoop(System.currentTimeMillis());
    }

    private void runFlightLoop(final long startTime) {
        gameHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isRoundActive) return;

                long elapsed = System.currentTimeMillis() - startTime;
                float progress = elapsed / 6000.0f; // 6s duration standard curve

                // Multiplier increment calculation
                currentMultiplier = 1.00f + (progress * progress * (crashPoint - 1.00f) * 1.5f);
                if (currentMultiplier < 1.00f) currentMultiplier = 1.00f;

                if (currentMultiplier >= crashPoint) {
                    // Aircraft Flew Away / Crashed
                    triggerCrash();
                } else {
                    tvMultiplier.setText(String.format(Locale.US, "%.2fx", currentMultiplier));
                    flightCanvas.updateFlight(progress, currentMultiplier, false);

                    if (hasActiveBet && !hasCashedOut) {
                        double expected = currentBetAmount * currentMultiplier;
                        btnActionGame.setText("CASHOUT ₹" + String.format(Locale.US, "%.1f", expected));
                    }

                    runFlightLoop(startTime);
                }
            }
        }, 50);
    }

    private void triggerCrash() {
        isRoundActive = false;
        soundManager.playCrash();

        tvMultiplier.setText(String.format(Locale.US, "%.2fx", crashPoint));
        tvRoundStatus.setText("FLEW AWAY!");
        flightCanvas.updateFlight(1.0f, crashPoint, true);

        pushHistoryChip(crashPoint);

        if (hasActiveBet && !hasCashedOut) {
            btnActionGame.setText("FLEW AWAY");
            btnActionGame.setBackgroundResource(R.drawable.bg_red_button);
            recordGameResult(crashPoint, 0.0, "LOST");
        }

        hasActiveBet = false;
        hasCashedOut = false;

        // Reset Bet Button
        btnActionGame.setText("PLACE BET");
        btnActionGame.setBackgroundResource(R.drawable.bg_red_button);
        btnActionGame.setEnabled(true);

        // Schedule next round in 4 seconds
        gameHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                startNextRoundCountdown(4);
            }
        }, 4000);
    }

    private void pushHistoryChip(float crash) {
        tvHist4.setText(tvHist3.getText());
        tvHist3.setText(tvHist2.getText());
        tvHist2.setText(tvHist1.getText());
        tvHist1.setText(String.format(Locale.US, "%.2fx", crash));
        if (crash >= 2.0f) {
            tvHist1.setTextColor(getResources().getColor(R.color.accent_green));
        } else {
            tvHist1.setTextColor(getResources().getColor(R.color.accent_red_bright));
        }
    }

    private void updateBalanceDisplay() {
        double total = currentBalance + bonusBalance;
        tvMainBalance.setText(String.format(Locale.US, "%.2f", total));
    }

    private void loadWalletData() {
        backendApi.readRecords("wallets", new BackendApi.ReadCallback() {
            @Override
            public void onSuccess(JSONArray records) {
                try {
                    String userEmail = backendApi.getUserEmail();
                    boolean found = false;
                    for (int i = 0; i < records.length(); i++) {
                        JSONObject rec = records.getJSONObject(i);
                        JSONObject data = rec.optJSONObject("data");
                        if (data != null && data.optString("email", "").equalsIgnoreCase(userEmail)) {
                            walletRecordId = rec.optString("id");
                            currentBalance = data.optDouble("deposited_balance", 0.0);
                            bonusBalance = data.optDouble("bonus_balance", 50.0);
                            found = true;
                            break;
                        }
                    }

                    if (!found) {
                        // Create initial wallet with ₹50 promotional bonus
                        bonusBalance = 50.0;
                        currentBalance = 0.0;
                        JSONObject initData = new JSONObject();
                        initData.put("email", userEmail);
                        initData.put("deposited_balance", currentBalance);
                        initData.put("bonus_balance", bonusBalance);
                        initData.put("total_winnings", 0.0);
                        initData.put("is_admin", userEmail.contains("admin"));

                        backendApi.createRecord("wallets", initData, new BackendApi.ApiCallback() {
                            @Override
                            public void onSuccess(JSONObject response) {
                                JSONObject record = response.optJSONObject("record");
                                if (record != null) {
                                    walletRecordId = record.optString("id");
                                }
                            }

                            @Override
                            public void onError(String message) {}
                        });
                    }

                    updateBalanceDisplay();
                } catch (Exception ignored) {}
            }

            @Override
            public void onError(String message) {
                // Keep local defaults
                updateBalanceDisplay();
            }
        });
    }

    private void syncWalletToBackend() {
        if (walletRecordId != null) {
            try {
                JSONObject data = new JSONObject();
                data.put("email", backendApi.getUserEmail());
                data.put("deposited_balance", currentBalance);
                data.put("bonus_balance", bonusBalance);

                backendApi.updateRecord(walletRecordId, data, new BackendApi.ApiCallback() {
                    @Override
                    public void onSuccess(JSONObject response) {}

                    @Override
                    public void onError(String message) {}
                });
            } catch (Exception ignored) {}
        }
    }

    private void recordTransaction(String category, double amount, String details) {
        try {
            JSONObject tx = new JSONObject();
            tx.put("email", backendApi.getUserEmail());
            tx.put("tx_ref", "TX-" + System.currentTimeMillis());
            tx.put("category", category);
            tx.put("amount", amount);
            tx.put("status", "COMPLETED");
            tx.put("details", details);
            tx.put("timestamp", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date()));

            backendApi.createRecord("transactions", tx, new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {}

                @Override
                public void onError(String message) {}
            });
        } catch (Exception ignored) {}
    }

    private void recordGameResult(float multiplier, double payout, String result) {
        try {
            JSONObject round = new JSONObject();
            round.put("round_id", "RND-" + System.currentTimeMillis());
            round.put("user_email", backendApi.getUserEmail());
            round.put("bet_amount", currentBetAmount);
            round.put("crash_multiplier", multiplier);
            round.put("cashout_multiplier", currentMultiplier);
            round.put("payout_amount", payout);
            round.put("result", result);
            round.put("timestamp", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date()));

            backendApi.createRecord("game_rounds", round, new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {}

                @Override
                public void onError(String message) {}
            });
        } catch (Exception ignored) {}
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadWalletData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        gameHandler.removeCallbacksAndMessages(null);
    }
}