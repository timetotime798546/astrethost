package com.trirunner.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONObject;

public class RegisterActivity extends Activity {

    private EditText etEmail, etPassword;
    private Button btnRegister;
    private TextView tvBackToLogin;
    private ProgressBar progressBar;
    private BackendApi backendApi;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        backendApi = new BackendApi(this);
        sessionManager = new SessionManager(this);

        etEmail = (EditText) findViewById(R.id.etEmail);
        etPassword = (EditText) findViewById(R.id.etPassword);
        btnRegister = (Button) findViewById(R.id.btnRegister);
        tvBackToLogin = (TextView) findViewById(R.id.tvBackToLogin);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptRegister();
            }
        });

        tvBackToLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void attemptRegister() {
        final String email = etEmail.getText().toString().trim();
        final String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnRegister.setEnabled(false);

        // Step 1: Register account
        backendApi.register(email, password, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                // Step 2: Login to obtain token
                backendApi.login(email, password, new BackendApi.ApiCallback() {
                    @Override
                    public void onSuccess(final JSONObject loginResponse) {
                        final String token = loginResponse.optString("token", "");
                        sessionManager.saveSession(token, email);

                        // Step 3: Initialize 100 points balance on signup
                        try {
                            JSONObject initData = new JSONObject();
                            initData.put("score", 100);
                            initData.put("crashes", 0);
                            initData.put("high_score", 0);

                            backendApi.createData(token, "player_profiles", initData, new BackendApi.ApiCallback() {
                                @Override
                                public void onSuccess(JSONObject createResp) {
                                    JSONObject rec = createResp.optJSONObject("record");
                                    if (rec != null) {
                                        String recordId = rec.optString("id", "");
                                        sessionManager.setProfileRecordId(recordId);
                                    }
                                    sessionManager.updateLocalStats(100, 0, 0);

                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            progressBar.setVisibility(View.GONE);
                                            Toast.makeText(RegisterActivity.this, "Welcome! 100 Points Awarded!", Toast.LENGTH_SHORT).show();
                                            Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                            startActivity(intent);
                                            finish();
                                        }
                                    });
                                }

                                @Override
                                public void onError(String errorMessage) {
                                    sessionManager.updateLocalStats(100, 0, 0);
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            progressBar.setVisibility(View.GONE);
                                            Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                                            startActivity(intent);
                                            finish();
                                        }
                                    });
                                }
                            });
                        } catch (Exception e) {
                            sessionManager.updateLocalStats(100, 0, 0);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    progressBar.setVisibility(View.GONE);
                                    Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                                    startActivity(intent);
                                    finish();
                                }
                            });
                        }
                    }

                    @Override
                    public void onError(final String errorMessage) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progressBar.setVisibility(View.GONE);
                                btnRegister.setEnabled(true);
                                Toast.makeText(RegisterActivity.this, "Auto-login failed: " + errorMessage, Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                });
            }

            @Override
            public void onError(final String errorMessage) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(View.GONE);
                        btnRegister.setEnabled(true);
                        Toast.makeText(RegisterActivity.this, "Registration failed: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }
}