package com.tapcoinarcade.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

public class MainActivity extends Activity {

    private static final String GAME_PREFS = "arcade_game_scores";
    private static final String PREF_HIGH_SCORE = "key_high_score";
    private static final String PREF_TAP_POWER = "key_tap_power";
    private static final String PREF_CLOUD_BALANCE = "key_cloud_balance";
    private static final int MILESTONE_INTERVAL = 100; // Trigger auto sync every 100 score points

    private BackendApi backendApi;
    private Vibrator vibrator;
    private final Handler tapAnimationHandler = new Handler();

    // Local Game State Variables
    private int currentScore = 0;
    private int highScore = 0;
    private int tapPower = 1;
    private int comboCount = 0;
    private long lastTapTime = 0;
    private double cloudBalance = 0.00;
    private int lastSyncedMilestone = 0;
    private boolean isSyncInProgress = false;

    // UI Widgets
    private TextView tvAdminNotice;
    private TextView tvHighScore;
    private TextView tvCloudBalance;
    private TextView tvMultiplier;
    private TextView tvCurrentScore;
    private TextView tvTapFeedback;
    private TextView tvUpgradeCost;
    private ImageButton btnTapCoin;
    private Button btnUpgradeTap;
    private Button btnResetRun;
    private Button btnAccountAction;
    private TextView tvSyncStatus;
    private ProgressBar pbActionLoading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        backendApi = new BackendApi(this);
        try {
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        } catch (Exception ignored) {
        }

        // Initialize UI References
        tvAdminNotice = (TextView) findViewById(R.id.tvAdminNotice);
        tvHighScore = (TextView) findViewById(R.id.tvHighScore);
        tvCloudBalance = (TextView) findViewById(R.id.tvCloudBalance);
        tvMultiplier = (TextView) findViewById(R.id.tvMultiplier);
        tvCurrentScore = (TextView) findViewById(R.id.tvCurrentScore);
        tvTapFeedback = (TextView) findViewById(R.id.tvTapFeedback);
        tvUpgradeCost = (TextView) findViewById(R.id.tvUpgradeCost);
        btnTapCoin = (ImageButton) findViewById(R.id.btnTapCoin);
        btnUpgradeTap = (Button) findViewById(R.id.btnUpgradeTap);
        btnResetRun = (Button) findViewById(R.id.btnResetRun);
        btnAccountAction = (Button) findViewById(R.id.btnAccountAction);
        tvSyncStatus = (TextView) findViewById(R.id.tvSyncStatus);
        pbActionLoading = (ProgressBar) findViewById(R.id.pbActionLoading);

        loadLocalGameState();
        updateUI();
        loadAdminSettings();

        // Responsive touch listener on arcade coin
        btnTapCoin.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    v.setScaleX(0.92f);
                    v.setScaleY(0.92f);
                    handleCoinTap();
                    return true;
                } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                    v.setScaleX(1.0f);
                    v.setScaleY(1.0f);
                    return true;
                }
                return false;
            }
        });

        // Upgrade Tap Power
        btnUpgradeTap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleUpgradeTapPower();
            }
        });

        // Reset Run (Automatically triggers cloud sync if points are pending)
        btnResetRun.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetRunAndAutoSync();
            }
        });

        // Account management / Login modal
        btnAccountAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (backendApi.isLoggedIn()) {
                    showPlayerSessionDialog();
                } else {
                    showAuthDialog(true);
                }
            }
        });
    }

    // MANDATORY DETERMINISTIC ARCADE CALCULATION (Java 8 Native Logic)
    private void handleCoinTap() {
        long now = System.currentTimeMillis();
        // Combo multiplier calculation logic: If tapped within 750ms, build combo
        if (now - lastTapTime < 750) {
            comboCount++;
        } else {
            comboCount = 1;
        }
        lastTapTime = now;

        // Calculate dynamic multiplier from combo
        int multiplier = 1;
        if (comboCount > 25) {
            multiplier = 4;
        } else if (comboCount > 12) {
            multiplier = 3;
        } else if (comboCount > 5) {
            multiplier = 2;
        }

        // Calculate earned points: score_gain = tapPower * multiplier
        int earnedScore = tapPower * multiplier;
        currentScore += earnedScore;

        // Check and persist new High Score immediately
        if (currentScore > highScore) {
            highScore = currentScore;
            saveHighScore(highScore);
        }

        // Haptic feedback pulse
        if (vibrator != null) {
            try {
                vibrator.vibrate(25);
            } catch (Exception ignored) {
            }
        }

        // Visual arcade feedback
        tvMultiplier.setText(multiplier + "X");
        tvCurrentScore.setText(String.valueOf(currentScore));
        tvHighScore.setText(String.valueOf(highScore));

        showFloatingTapFeedback("+" + earnedScore + (multiplier > 1 ? " (" + multiplier + "X COMBO!)" : ""));

        // Check Milestone for Automated Background Cloud Sync
        if (currentScore - lastSyncedMilestone >= MILESTONE_INTERVAL) {
            int milestoneDelta = currentScore - lastSyncedMilestone;
            lastSyncedMilestone = currentScore;
            performBackgroundCloudSync(milestoneDelta, "Milestone (" + currentScore + " pts) Auto-Sync");
        }
    }

    private void showFloatingTapFeedback(String text) {
        tvTapFeedback.setText(text);
        tvTapFeedback.setVisibility(View.VISIBLE);
        tapAnimationHandler.removeCallbacksAndMessages(null);
        tapAnimationHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                tvTapFeedback.setVisibility(View.INVISIBLE);
            }
        }, 600);
    }

    // Local deterministic upgrade calculation
    private void handleUpgradeTapPower() {
        int cost = tapPower * 25;
        if (currentScore >= cost) {
            currentScore -= cost;
            if (lastSyncedMilestone > currentScore) {
                lastSyncedMilestone = currentScore;
            }
            tapPower++;
            saveTapPower(tapPower);
            updateUI();
            Toast.makeText(this, "Tapper Upgraded to Level " + tapPower + "! (+" + tapPower + "/tap)", Toast.LENGTH_SHORT).show();
        } else {
            int needed = cost - currentScore;
            Toast.makeText(this, "Need " + needed + " more score to upgrade!", Toast.LENGTH_SHORT).show();
        }
    }

    // AUTOMATIC CLOUD SYNC ON RUN RESET / SESSION END
    private void resetRunAndAutoSync() {
        int unsyncedScore = currentScore - lastSyncedMilestone;
        if (unsyncedScore > 0 && backendApi.isLoggedIn()) {
            performBackgroundCloudSync(unsyncedScore, "Session End Run Reset Auto-Sync");
        }

        currentScore = 0;
        comboCount = 0;
        lastSyncedMilestone = 0;
        tvMultiplier.setText("1X");
        tvCurrentScore.setText("0");
        showFloatingTapFeedback("RUN RESET");
        Toast.makeText(this, "Run finished! Resetting for new session.", Toast.LENGTH_SHORT).show();
    }

    // NON-BLOCKING ASYNCHRONOUS CLOUD AUTO-SYNC
    private void performBackgroundCloudSync(final double amount, final String purpose) {
        if (!backendApi.isLoggedIn() || amount <= 0 || isSyncInProgress) {
            return;
        }

        isSyncInProgress = true;
        pbActionLoading.setVisibility(View.VISIBLE);
        tvSyncStatus.setText(R.string.auto_syncing);
        tvSyncStatus.setTextColor(getResources().getColor(R.color.neon_cyan));

        // Call backend Action-Based endpoint asynchronously
        backendApi.executeAppAction("credit", amount, purpose, "player_profile", new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                isSyncInProgress = false;
                pbActionLoading.setVisibility(View.GONE);
                tvSyncStatus.setText("⚡ Auto-Synced +" + (int) amount + " pts");
                tvSyncStatus.setTextColor(getResources().getColor(R.color.neon_green));

                double newBalance = response.optDouble("new_balance", cloudBalance + amount);
                cloudBalance = newBalance;
                saveCloudBalance(cloudBalance);
                tvCloudBalance.setText(String.format("%.2f", cloudBalance));

                tapAnimationHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        tvSyncStatus.setText(R.string.auto_sync_active);
                    }
                }, 3000);
            }

            @Override
            public void onError(String errorMessage) {
                isSyncInProgress = false;
                pbActionLoading.setVisibility(View.GONE);
                tvSyncStatus.setText("⚡ Auto-Sync will retry");
                tvSyncStatus.setTextColor(getResources().getColor(R.color.neon_red));

                tapAnimationHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        tvSyncStatus.setText(R.string.auto_sync_active);
                        tvSyncStatus.setTextColor(getResources().getColor(R.color.neon_green));
                    }
                }, 3000);
            }
        });
    }

    private void loadLocalGameState() {
        SharedPreferences sp = getSharedPreferences(GAME_PREFS, Context.MODE_PRIVATE);
        highScore = sp.getInt(PREF_HIGH_SCORE, 0);
        tapPower = sp.getInt(PREF_TAP_POWER, 1);
        cloudBalance = Double.longBitsToDouble(sp.getLong(PREF_CLOUD_BALANCE, Double.doubleToLongBits(0.00)));
    }

    private void saveHighScore(int score) {
        SharedPreferences sp = getSharedPreferences(GAME_PREFS, Context.MODE_PRIVATE);
        sp.edit().putInt(PREF_HIGH_SCORE, score).apply();
    }

    private void saveTapPower(int power) {
        SharedPreferences sp = getSharedPreferences(GAME_PREFS, Context.MODE_PRIVATE);
        sp.edit().putInt(PREF_TAP_POWER, power).apply();
    }

    private void saveCloudBalance(double balance) {
        SharedPreferences sp = getSharedPreferences(GAME_PREFS, Context.MODE_PRIVATE);
        sp.edit().putLong(PREF_CLOUD_BALANCE, Double.doubleToRawLongBits(balance)).apply();
    }

    private void updateUI() {
        tvCurrentScore.setText(String.valueOf(currentScore));
        tvHighScore.setText(String.valueOf(highScore));
        tvCloudBalance.setText(String.format("%.2f", cloudBalance));

        int upgradeCost = tapPower * 25;
        btnUpgradeTap.setText("UPGRADE TAP (Lv. " + (tapPower + 1) + ")");
        tvUpgradeCost.setText("Upgrade Cost: " + upgradeCost + " Score | Current: +" + tapPower + " / tap");

        if (backendApi.isLoggedIn()) {
            String email = backendApi.getLoggedEmail();
            String label = (email != null && email.contains("@")) ? email.substring(0, email.indexOf('@')) : "Player";
            btnAccountAction.setText("👤 " + label.toUpperCase());
            tvSyncStatus.setText(R.string.auto_sync_active);
        } else {
            btnAccountAction.setText("PLAYER SIGN IN");
            tvSyncStatus.setText("⚡ Sign in for Auto Cloud Sync");
        }
    }

    private void loadAdminSettings() {
        backendApi.fetchAdminConfig(new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                JSONObject settings = response.optJSONObject("settings");
                if (settings != null) {
                    String notice = settings.optString("arcade_banner_notice", "⚡ 2X FRENZY EVENT ACTIVE! TAP TO EARN REWARDS! ⚡");
                    tvAdminNotice.setText(notice);
                }
            }

            @Override
            public void onError(String errorMessage) {
                // Keep default layout announcement notice
            }
        });
    }

    // Authentication Dialog (Login / Register)
    private void showAuthDialog(final boolean startInLoginMode) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_auth, null);
        builder.setView(dialogView);

        final AlertDialog dialog = builder.create();

        final TextView tvAuthDialogTitle = (TextView) dialogView.findViewById(R.id.tvAuthDialogTitle);
        final TextView tvAuthDialogSubtitle = (TextView) dialogView.findViewById(R.id.tvAuthDialogSubtitle);
        final EditText etPlayerEmail = (EditText) dialogView.findViewById(R.id.etPlayerEmail);
        final EditText etPlayerPassword = (EditText) dialogView.findViewById(R.id.etPlayerPassword);
        final Button btnSubmitPlayerAuth = (Button) dialogView.findViewById(R.id.btnSubmitPlayerAuth);
        final TextView tvToggleSignup = (TextView) dialogView.findViewById(R.id.tvToggleSignup);
        final TextView tvDialogForgotPass = (TextView) dialogView.findViewById(R.id.tvDialogForgotPass);
        final ProgressBar pbDialogAuth = (ProgressBar) dialogView.findViewById(R.id.pbDialogAuth);
        final TextView tvAuthError = (TextView) dialogView.findViewById(R.id.tvAuthError);

        final boolean[] isLoginMode = new boolean[]{startInLoginMode};

        final Runnable refreshMode = new Runnable() {
            @Override
            public void run() {
                tvAuthError.setVisibility(View.GONE);
                if (isLoginMode[0]) {
                    tvAuthDialogTitle.setText("Arcade Player Login");
                    tvAuthDialogSubtitle.setText("Sign in to sync high scores and enable auto cloud sync.");
                    btnSubmitPlayerAuth.setText("SIGN IN");
                    tvToggleSignup.setText("Need Account? Sign Up");
                    tvDialogForgotPass.setVisibility(View.VISIBLE);
                } else {
                    tvAuthDialogTitle.setText("Create Player Account");
                    tvAuthDialogSubtitle.setText("Register to secure your arcade points in the cloud.");
                    btnSubmitPlayerAuth.setText("CREATE ACCOUNT");
                    tvToggleSignup.setText("Already Player? Sign In");
                    tvDialogForgotPass.setVisibility(View.GONE);
                }
            }
        };

        refreshMode.run();

        tvToggleSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isLoginMode[0] = !isLoginMode[0];
                refreshMode.run();
            }
        });

        tvDialogForgotPass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                showForgotPasswordDialog();
            }
        });

        btnSubmitPlayerAuth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String email = etPlayerEmail.getText().toString().trim();
                final String password = etPlayerPassword.getText().toString().trim();

                if (email.isEmpty() || !email.contains("@")) {
                    tvAuthError.setText("Please enter a valid player email.");
                    tvAuthError.setVisibility(View.VISIBLE);
                    return;
                }
                if (password.length() < 6) {
                    tvAuthError.setText("Password must be at least 6 characters.");
                    tvAuthError.setVisibility(View.VISIBLE);
                    return;
                }

                pbDialogAuth.setVisibility(View.VISIBLE);
                btnSubmitPlayerAuth.setEnabled(false);
                tvAuthError.setVisibility(View.GONE);

                if (isLoginMode[0]) {
                    executeLogin(email, password, dialog, pbDialogAuth, btnSubmitPlayerAuth, tvAuthError);
                } else {
                    backendApi.register(email, password, new BackendApi.ApiCallback() {
                        @Override
                        public void onSuccess(JSONObject response) {
                            executeLogin(email, password, dialog, pbDialogAuth, btnSubmitPlayerAuth, tvAuthError);
                        }

                        @Override
                        public void onError(String errorMessage) {
                            pbDialogAuth.setVisibility(View.GONE);
                            btnSubmitPlayerAuth.setEnabled(true);
                            tvAuthError.setText("Registration error: " + errorMessage);
                            tvAuthError.setVisibility(View.VISIBLE);
                        }
                    });
                }
            }
        });

        dialog.show();
    }

    private void executeLogin(final String email, final String password, final AlertDialog dialog, final ProgressBar pb, final Button submitBtn, final TextView errText) {
        backendApi.login(email, password, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                pb.setVisibility(View.GONE);
                submitBtn.setEnabled(true);
                String token = response.optString("token", "");
                if (token.isEmpty()) {
                    errText.setText("Login response missing token.");
                    errText.setVisibility(View.VISIBLE);
                    return;
                }

                backendApi.saveAuthSession(token, email);
                dialog.dismiss();
                updateUI();
                Toast.makeText(MainActivity.this, "Welcome back, " + email + "!", Toast.LENGTH_SHORT).show();

                // Trigger sync if score accumulated offline
                if (currentScore > 0) {
                    performBackgroundCloudSync(currentScore, "Login Offline Score Auto-Sync");
                }
            }

            @Override
            public void onError(String errorMessage) {
                pb.setVisibility(View.GONE);
                submitBtn.setEnabled(true);
                errText.setText("Login failed: " + errorMessage);
                errText.setVisibility(View.VISIBLE);
            }
        });
    }

    // Gmail OTP Password Reset Modal
    private void showForgotPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_reset_password, null);
        builder.setView(dialogView);

        final AlertDialog dialog = builder.create();

        final EditText etResetEmail = (EditText) dialogView.findViewById(R.id.etResetEmail);
        final Button btnSendOtp = (Button) dialogView.findViewById(R.id.btnSendOtp);
        final LinearLayout layoutResetInputs = (LinearLayout) dialogView.findViewById(R.id.layoutResetInputs);
        final EditText etResetOtp = (EditText) dialogView.findViewById(R.id.etResetOtp);
        final EditText etNewPassword = (EditText) dialogView.findViewById(R.id.etNewPassword);
        final Button btnConfirmPasswordReset = (Button) dialogView.findViewById(R.id.btnConfirmPasswordReset);
        final ProgressBar pbResetOtpProgress = (ProgressBar) dialogView.findViewById(R.id.pbResetOtpProgress);
        final TextView tvResetStatus = (TextView) dialogView.findViewById(R.id.tvResetStatus);

        btnSendOtp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String email = etResetEmail.getText().toString().trim();
                if (email.isEmpty() || !email.contains("@")) {
                    tvResetStatus.setText("Enter a valid email address.");
                    tvResetStatus.setVisibility(View.VISIBLE);
                    return;
                }

                pbResetOtpProgress.setVisibility(View.VISIBLE);
                tvResetStatus.setVisibility(View.GONE);

                backendApi.requestOtp(email, new BackendApi.ApiCallback() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        pbResetOtpProgress.setVisibility(View.GONE);
                        layoutResetInputs.setVisibility(View.VISIBLE);
                        btnSendOtp.setVisibility(View.GONE);
                        etResetEmail.setEnabled(false);
                        tvResetStatus.setTextColor(getResources().getColor(R.color.neon_green));
                        tvResetStatus.setText("OTP sent to your Gmail! Enter OTP and new password.");
                        tvResetStatus.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onError(String errorMessage) {
                        pbResetOtpProgress.setVisibility(View.GONE);
                        tvResetStatus.setTextColor(getResources().getColor(R.color.neon_red));
                        tvResetStatus.setText("Failed to send OTP: " + errorMessage);
                        tvResetStatus.setVisibility(View.VISIBLE);
                    }
                });
            }
        });

        btnConfirmPasswordReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String email = etResetEmail.getText().toString().trim();
                final String otp = etResetOtp.getText().toString().trim();
                final String newPass = etNewPassword.getText().toString().trim();

                if (otp.length() < 4) {
                    tvResetStatus.setTextColor(getResources().getColor(R.color.neon_red));
                    tvResetStatus.setText("Please enter valid OTP code.");
                    tvResetStatus.setVisibility(View.VISIBLE);
                    return;
                }

                if (newPass.length() < 6) {
                    tvResetStatus.setTextColor(getResources().getColor(R.color.neon_red));
                    tvResetStatus.setText("New password must be at least 6 characters.");
                    tvResetStatus.setVisibility(View.VISIBLE);
                    return;
                }

                pbResetOtpProgress.setVisibility(View.VISIBLE);
                tvResetStatus.setVisibility(View.GONE);

                backendApi.resetPassword(email, otp, newPass, new BackendApi.ApiCallback() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        pbResetOtpProgress.setVisibility(View.GONE);
                        Toast.makeText(MainActivity.this, "Password updated successfully! Please log in.", Toast.LENGTH_LONG).show();
                        dialog.dismiss();
                        showAuthDialog(true);
                    }

                    @Override
                    public void onError(String errorMessage) {
                        pbResetOtpProgress.setVisibility(View.GONE);
                        tvResetStatus.setTextColor(getResources().getColor(R.color.neon_red));
                        tvResetStatus.setText("Reset error: " + errorMessage);
                        tvResetStatus.setVisibility(View.VISIBLE);
                    }
                });
            }
        });

        dialog.show();
    }

    private void showPlayerSessionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Arcade Player Session");
        builder.setMessage("Logged in as:\n" + backendApi.getLoggedEmail() + "\n\nSynced Cloud Balance: " + String.format("%.2f", cloudBalance) + " Points\nAuto Cloud Sync: Enabled");

        builder.setPositiveButton("OK", null);
        builder.setNegativeButton("LOGOUT", new android.content.DialogInterface.OnClickListener() {
            @Override
            public void onClick(android.content.DialogInterface dialog, int which) {
                backendApi.logout(new BackendApi.ApiCallback() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        updateUI();
                        Toast.makeText(MainActivity.this, "Logged out of player account", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String errorMessage) {
                        updateUI();
                    }
                });
            }
        });

        builder.show();
    }
}