package com.cricketpro3d.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends Activity {

    private TextView tvPlayerName, tvTotalRuns, tvHighScore, tvWins, tvBoundaries;
    private Button btnLogout, btnPlaySuperOver, btnPlayT20, btnPlayNets;
    private SessionManager sessionManager;
    private BackendApi backendApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);
        backendApi = new BackendApi(this);

        if (!sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        tvPlayerName = (TextView) findViewById(R.id.tvPlayerName);
        tvTotalRuns = (TextView) findViewById(R.id.tvTotalRuns);
        tvHighScore = (TextView) findViewById(R.id.tvHighScore);
        tvWins = (TextView) findViewById(R.id.tvWins);
        tvBoundaries = (TextView) findViewById(R.id.tvBoundaries);

        btnLogout = (Button) findViewById(R.id.btnLogout);
        btnPlaySuperOver = (Button) findViewById(R.id.btnPlaySuperOver);
        btnPlayT20 = (Button) findViewById(R.id.btnPlayT20);
        btnPlayNets = (Button) findViewById(R.id.btnPlayNets);

        tvPlayerName.setText("Player: " + sessionManager.getEmail());

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sessionManager.clear();
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        });

        btnPlaySuperOver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchGame(6, 18, 2, "Super Over");
            }
        });

        btnPlayT20.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchGame(12, 32, 3, "2-Over Chase");
            }
        });

        btnPlayNets.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchGame(999, 0, 99, "Practice Nets");
            }
        });
    }

    private void launchGame(int totalBalls, int targetRuns, int maxWickets, String modeName) {
        Intent intent = new Intent(MainActivity.this, GameActivity.class);
        intent.putExtra("TOTAL_BALLS", totalBalls);
        intent.putExtra("TARGET_RUNS", targetRuns);
        intent.putExtra("MAX_WICKETS", maxWickets);
        intent.putExtra("MODE_NAME", modeName);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatsDisplay();
        fetchCareerFromCloud();
    }

    private void updateStatsDisplay() {
        tvTotalRuns.setText(String.valueOf(sessionManager.getTotalRuns()));
        tvHighScore.setText(sessionManager.getHighScore() + "*");
        tvWins.setText(String.valueOf(sessionManager.getWins()));
        tvBoundaries.setText("Boundaries: " + sessionManager.getSixes() + " Sixes | " + sessionManager.getFours() + " Fours");
    }

    private void fetchCareerFromCloud() {
        String token = sessionManager.getToken();
        if (token == null) return;

        backendApi.getData(token, "career_stats", new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(final JSONObject response) {
                try {
                    JSONArray records = response.optJSONArray("records");
                    if (records != null && records.length() > 0) {
                        JSONObject record = records.getJSONObject(0);
                        sessionManager.setCareerRecordId(record.optString("id"));

                        JSONObject data = record.optJSONObject("data");
                        if (data != null) {
                            int matches = data.optInt("matches_played", 0);
                            int wins = data.optInt("matches_won", 0);
                            int highScore = data.optInt("highest_score", 0);
                            int totalRuns = data.optInt("total_runs", 0);
                            int sixes = data.optInt("total_sixes", 0);
                            int fours = data.optInt("total_fours", 0);

                            sessionManager.updateCareer(matches, wins, highScore, totalRuns, sixes, fours);

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    updateStatsDisplay();
                                }
                            });
                        }
                    }
                } catch (Exception ignored) {}
            }

            @Override
            public void onError(String errorMessage) {}
        });
    }
}