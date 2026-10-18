package com.pahadirunner.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends Activity implements GameView.GameListener {

    private static final int REQUEST_LOGIN = 201;
    private static final int REQUEST_RECHARGE = 202;
    private static final String PREF_GAME = "pahadi_game_storage";

    private GameView gameView;
    private TextView tvRunsLeft, tvTotalCoins, tvHighScore;
    private LinearLayout layoutOverlay;
    private TextView tvOverlayTitle, tvOverlayDesc;
    private Button btnPlayGame, btnRechargeOverlay, btnJump, btnRechargeDirect, btnAccount;

    private int freeRunsRemaining = 3;
    private int totalCoins = 0;
    private int highScore = 0;
    private String profileRecordId = null;

    private BackendApi backendApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        backendApi = new BackendApi(this);

        gameView = (GameView) findViewById(R.id.gameView);
        tvRunsLeft = (TextView) findViewById(R.id.tvRunsLeft);
        tvTotalCoins = (TextView) findViewById(R.id.tvTotalCoins);
        tvHighScore = (TextView) findViewById(R.id.tvHighScore);
        layoutOverlay = (LinearLayout) findViewById(R.id.layoutOverlay);
        tvOverlayTitle = (TextView) findViewById(R.id.tvOverlayTitle);
        tvOverlayDesc = (TextView) findViewById(R.id.tvOverlayDesc);
        btnPlayGame = (Button) findViewById(R.id.btnPlayGame);
        btnRechargeOverlay = (Button) findViewById(R.id.btnRechargeOverlay);
        btnJump = (Button) findViewById(R.id.btnJump);
        btnRechargeDirect = (Button) findViewById(R.id.btnRechargeDirect);
        btnAccount = (Button) findViewById(R.id.btnAccount);

        gameView.setGameListener(this);

        loadLocalStats();
        updateUI();

        btnPlayGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (freeRunsRemaining > 0) {
                    startRunningSession();
                } else {
                    openRechargeScreen();
                }
            }
        });

        btnJump.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gameView.jump();
            }
        });

        btnRechargeOverlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openRechargeScreen();
            }
        });

        btnRechargeDirect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openRechargeScreen();
            }
        });

        btnAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (backendApi.isLoggedIn()) {
                    // Show logout or status
                    backendApi.clearAuth();
                    btnAccount.setText("लॉगिन / प्रोफ़ाइल");
                    Toast.makeText(MainActivity.this, "लॉगआउट हो गया", Toast.LENGTH_SHORT).show();
                } else {
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    startActivityForResult(intent, REQUEST_LOGIN);
                }
            }
        });

        // Sync with backend if logged in
        if (backendApi.isLoggedIn()) {
            btnAccount.setText(backendApi.getUserEmail());
            fetchCloudProfile();
        }
    }

    private void loadLocalStats() {
        SharedPreferences sp = getSharedPreferences(PREF_GAME, Context.MODE_PRIVATE);
        freeRunsRemaining = sp.getInt("free_runs", 3);
        totalCoins = sp.getInt("total_coins", 0);
        highScore = sp.getInt("high_score", 0);
    }

    private void saveLocalStats() {
        SharedPreferences sp = getSharedPreferences(PREF_GAME, Context.MODE_PRIVATE);
        sp.edit()
                .putInt("free_runs", freeRunsRemaining)
                .putInt("total_coins", totalCoins)
                .putInt("high_score", highScore)
                .apply();
    }

    private void updateUI() {
        tvRunsLeft.setText(freeRunsRemaining + " / 3");
        tvTotalCoins.setText("₹" + totalCoins);
        tvHighScore.setText(highScore + " m");

        if (freeRunsRemaining <= 0) {
            tvOverlayTitle.setText("3 फ्री चांस समाप्त! (Limit Reached)");
            tvOverlayDesc.setText("आपकी 3 बार की फ्री रनिंग पूरी हो चुकी है। दोबारा दौड़ने के लिए ₹50 का पेमेंट स्क्रीनशॉट अपलोड करें।");
            btnPlayGame.setVisibility(View.GONE);
            btnRechargeOverlay.setVisibility(View.VISIBLE);
            layoutOverlay.setVisibility(View.VISIBLE);
        } else {
            tvOverlayTitle.setText("पहाड़ी रनर तैयार है!");
            tvOverlayDesc.setText("पहाड़ी इलाके में दौड़ें, ₹50 के सिक्के एकत्र करें। चांस बाकी: " + freeRunsRemaining);
            btnPlayGame.setVisibility(View.VISIBLE);
            btnRechargeOverlay.setVisibility(View.GONE);
        }
    }

    private void startRunningSession() {
        // Decrement free run
        freeRunsRemaining--;
        saveLocalStats();
        updateUI();

        layoutOverlay.setVisibility(View.GONE);
        gameView.startRun();
    }

    private void openRechargeScreen() {
        Intent intent = new Intent(this, RechargeActivity.class);
        startActivityForResult(intent, REQUEST_RECHARGE);
    }

    @Override
    public void onGameOver(final int sessionCoins, final int distance) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Mandatory Offline Native Java Business Logic Calculation:
                // Total coins = previous total + session coins collected
                totalCoins = calculateTotalCoins(totalCoins, sessionCoins);
                if (distance > highScore) {
                    highScore = distance;
                }
                saveLocalStats();
                syncProfileToCloud();

                layoutOverlay.setVisibility(View.VISIBLE);
                if (freeRunsRemaining > 0) {
                    tvOverlayTitle.setText("दौड़ समाप्त! (Run Ended)");
                    tvOverlayDesc.setText("दूरी: " + distance + "m | एकत्र सिक्के: ₹" + sessionCoins + "\nशेष चांस: " + freeRunsRemaining);
                    btnPlayGame.setVisibility(View.VISIBLE);
                    btnRechargeOverlay.setVisibility(View.GONE);
                } else {
                    tvOverlayTitle.setText("3 फ्री चांस समाप्त हो गए!");
                    tvOverlayDesc.setText("दूरी: " + distance + "m | एकत्र सिक्के: ₹" + sessionCoins + "\nआगे खेलने के लिए स्क्रीनशॉट अपलोड करें।");
                    btnPlayGame.setVisibility(View.GONE);
                    btnRechargeOverlay.setVisibility(View.VISIBLE);
                }

                updateUI();
            }
        });
    }

    @Override
    public void onCoinCollected(final int currentSessionCoins) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Live UI feedback
            }
        });
    }

    // Mandatory Native Java calculation
    private int calculateTotalCoins(int currentBase, int earned) {
        return currentBase + earned;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_LOGIN && resultCode == RESULT_OK) {
            btnAccount.setText(backendApi.getUserEmail());
            fetchCloudProfile();
        } else if (requestCode == REQUEST_RECHARGE && resultCode == RESULT_OK && data != null) {
            boolean recharged = data.getBooleanExtra("recharge_success", false);
            if (recharged) {
                int addedRuns = data.getIntExtra("added_runs", 3);
                int addedCoins = data.getIntExtra("added_coins", 50);

                freeRunsRemaining += addedRuns;
                totalCoins = calculateTotalCoins(totalCoins, addedCoins);
                saveLocalStats();
                updateUI();
                syncProfileToCloud();

                layoutOverlay.setVisibility(View.VISIBLE);
                tvOverlayTitle.setText("सत्यापन सफल! +3 रन जोड़े गए");
                tvOverlayDesc.setText("आपकी नई दौड़ उपलब्ध हैं। पहाड़ी रनर शुरू करने के लिए बटन दबाएँ!");
                btnPlayGame.setVisibility(View.VISIBLE);
                btnRechargeOverlay.setVisibility(View.GONE);
            }
        }
    }

    private void fetchCloudProfile() {
        if (!backendApi.isLoggedIn()) return;

        backendApi.getRecords("game_profiles", new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                try {
                    if (response.optBoolean("success", false)) {
                        JSONArray records = response.optJSONArray("records");
                        if (records != null && records.length() > 0) {
                            JSONObject firstRec = records.getJSONObject(0);
                            profileRecordId = firstRec.optString("id", null);
                            JSONObject data = firstRec.optJSONObject("data");
                            if (data != null) {
                                final int cloudRuns = data.optInt("runs_left", freeRunsRemaining);
                                final int cloudCoins = data.optInt("coins", totalCoins);
                                final int cloudHigh = data.optInt("high_score", highScore);

                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        freeRunsRemaining = cloudRuns;
                                        totalCoins = cloudCoins;
                                        highScore = cloudHigh;
                                        saveLocalStats();
                                        updateUI();
                                    }
                                });
                            }
                        }
                    }
                } catch (Exception e) {
                    // Use local fallback
                }
            }

            @Override
            public void onError(String error) {
                // Local values remain intact
            }
        });
    }

    private void syncProfileToCloud() {
        if (!backendApi.isLoggedIn()) return;

        try {
            JSONObject data = new JSONObject();
            data.put("runs_left", freeRunsRemaining);
            data.put("coins", totalCoins);
            data.put("high_score", highScore);

            if (profileRecordId != null) {
                backendApi.updateRecord(profileRecordId, data, null);
            } else {
                backendApi.createRecord("game_profiles", data, new BackendApi.ApiCallback() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        try {
                            JSONObject rec = response.optJSONObject("record");
                            if (rec != null) {
                                profileRecordId = rec.optString("id", null);
                            }
                        } catch (Exception ignored) {}
                    }

                    @Override
                    public void onError(String error) {}
                });
            }
        } catch (Exception ignored) {}
    }

    @Override
    protected void onPause() {
        super.onPause();
        gameView.stopRun();
    }
}