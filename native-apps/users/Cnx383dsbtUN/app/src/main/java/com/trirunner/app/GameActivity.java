package com.trirunner.app;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import org.json.JSONObject;

public class GameActivity extends Activity implements GameView.GameListener {

    private GameView gameView;
    private TextView tvGameScore, tvDistance, tvSyncNotice;
    private Button btnLeft, btnRight;

    private SessionManager sessionManager;
    private BackendApi backendApi;
    private Vibrator vibrator;

    private int currentPoints;
    private int currentCrashes;
    private int highestDistance;
    private int sessionMaxDistance = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        sessionManager = new SessionManager(this);
        backendApi = new BackendApi(this);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        currentPoints = sessionManager.getScore();
        currentCrashes = sessionManager.getCrashes();
        highestDistance = sessionManager.getHighScore();

        gameView = (GameView) findViewById(R.id.gameView);
        tvGameScore = (TextView) findViewById(R.id.tvGameScore);
        tvDistance = (TextView) findViewById(R.id.tvDistance);
        tvSyncNotice = (TextView) findViewById(R.id.tvSyncNotice);
        btnLeft = (Button) findViewById(R.id.btnLeft);
        btnRight = (Button) findViewById(R.id.btnRight);

        tvGameScore.setText(String.valueOf(currentPoints));
        gameView.setGameListener(this);

        btnLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gameView.moveLeft();
            }
        });

        btnRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gameView.moveRight();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        gameView.startGame();
    }

    @Override
    protected void onPause() {
        super.onPause();
        gameView.stopGame();
        saveAndSyncCloud();
    }

    /**
     * Mandatory Native Offline Calculation:
     * On crash, deduct 10 points locally (native Java business logic)
     * and update balance to cloud.
     */
    @Override
    public void onCrashOccurred() {
        // Native Java deterministic calculation: current_score - 10
        currentPoints = currentPoints - 10;
        currentCrashes = currentCrashes + 1;

        if (vibrator != null) {
            try {
                vibrator.vibrate(120);
            } catch (Exception ignored) {}
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvGameScore.setText(String.valueOf(currentPoints));
                tvSyncNotice.setText("CRASH! -10 PTS -> Syncing Cloud...");
                tvSyncNotice.setVisibility(View.VISIBLE);

                tvSyncNotice.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        tvSyncNotice.setVisibility(View.GONE);
                    }
                }, 2200);
            }
        });

        // Update local session
        sessionManager.updateLocalStats(currentPoints, currentCrashes, highestDistance);

        // Update balance to cloud on each crash
        syncCrashToCloud();
    }

    @Override
    public void onScoreUpdated(final int distanceMeters) {
        if (distanceMeters > sessionMaxDistance) {
            sessionMaxDistance = distanceMeters;
            if (sessionMaxDistance > highestDistance) {
                highestDistance = sessionMaxDistance;
                sessionManager.updateLocalStats(currentPoints, currentCrashes, highestDistance);
            }
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvDistance.setText(distanceMeters + " m");
            }
        });
    }

    private void syncCrashToCloud() {
        final String token = sessionManager.getToken();
        final String recordId = sessionManager.getProfileRecordId();
        if (token == null || token.isEmpty()) return;

        try {
            final JSONObject dataObj = new JSONObject();
            dataObj.put("score", currentPoints);
            dataObj.put("crashes", currentCrashes);
            dataObj.put("high_score", highestDistance);

            if (recordId != null && !recordId.isEmpty()) {
                // UPDATE existing cloud record
                backendApi.updateData(token, recordId, dataObj, new BackendApi.ApiCallback() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                tvSyncNotice.setText("Cloud Balance Synced: " + currentPoints);
                            }
                        });
                    }

                    @Override
                    public void onError(String errorMessage) {}
                });
            } else {
                // CREATE cloud record if not yet set
                backendApi.createData(token, "player_profiles", dataObj, new BackendApi.ApiCallback() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        JSONObject rec = response.optJSONObject("record");
                        if (rec != null) {
                            sessionManager.setProfileRecordId(rec.optString("id"));
                        }
                    }

                    @Override
                    public void onError(String errorMessage) {}
                });
            }
        } catch (Exception ignored) {}
    }

    private void saveAndSyncCloud() {
        syncCrashToCloud();
    }
}