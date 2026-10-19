package com.trirunner.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends Activity {

    private TextView tvUserEmail, tvPoints, tvCrashes, tvHighScore;
    private Button btnPlay, btnLogout;
    private SessionManager sessionManager;
    private BackendApi backendApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);
        backendApi = new BackendApi(this);

        if (!sessionManager.isLoggedIn()) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        tvUserEmail = (TextView) findViewById(R.id.tvUserEmail);
        tvPoints = (TextView) findViewById(R.id.tvPoints);
        tvCrashes = (TextView) findViewById(R.id.tvCrashes);
        tvHighScore = (TextView) findViewById(R.id.tvHighScore);
        btnPlay = (Button) findViewById(R.id.btnPlay);
        btnLogout = (Button) findViewById(R.id.btnLogout);

        tvUserEmail.setText(sessionManager.getEmail());

        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, GameActivity.class);
                startActivity(intent);
            }
        });

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
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateDisplay();
        fetchProfileFromCloud();
    }

    private void updateDisplay() {
        tvPoints.setText(String.valueOf(sessionManager.getScore()));
        tvCrashes.setText(String.valueOf(sessionManager.getCrashes()));
        tvHighScore.setText(sessionManager.getHighScore() + " m");
    }

    private void fetchProfileFromCloud() {
        String token = sessionManager.getToken();
        if (token == null) return;

        backendApi.getData(token, "player_profiles", new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(final JSONObject response) {
                try {
                    JSONArray records = response.optJSONArray("records");
                    if (records != null && records.length() > 0) {
                        JSONObject record = records.getJSONObject(0);
                        String id = record.optString("id");
                        sessionManager.setProfileRecordId(id);

                        JSONObject data = record.optJSONObject("data");
                        if (data != null) {
                            int score = data.optInt("score", 100);
                            int crashes = data.optInt("crashes", 0);
                            int highScore = data.optInt("high_score", 0);
                            sessionManager.updateLocalStats(score, crashes, highScore);

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    updateDisplay();
                                }
                            });
                        }
                    } else {
                        // Create initial profile record if not found
                        JSONObject initData = new JSONObject();
                        initData.put("score", 100);
                        initData.put("crashes", 0);
                        initData.put("high_score", 0);

                        backendApi.createData(token, "player_profiles", initData, new BackendApi.ApiCallback() {
                            @Override
                            public void onSuccess(JSONObject createResp) {
                                JSONObject rec = createResp.optJSONObject("record");
                                if (rec != null) {
                                    sessionManager.setProfileRecordId(rec.optString("id"));
                                }
                                sessionManager.updateLocalStats(100, 0, 0);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        updateDisplay();
                                    }
                                });
                            }

                            @Override
                            public void onError(String errorMessage) {}
                        });
                    }
                } catch (Exception ignored) {}
            }

            @Override
            public void onError(String errorMessage) {
                // Keep local cached values on network offline
            }
        });
    }
}