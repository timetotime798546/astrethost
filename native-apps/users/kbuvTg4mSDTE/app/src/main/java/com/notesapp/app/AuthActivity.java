package com.notesapp.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

public class AuthActivity extends Activity {
    private EditText etEmail, etPassword;
    private Button btnLogin, btnRegister;
    private TextView tvForgotPassword;
    private ProgressBar pbAuthProgress;
    private BackendApi backendApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences("notes_prefs", Context.MODE_PRIVATE);
        String token = prefs.getString("auth_token", null);
        if (token != null && !token.isEmpty()) {
            startActivity(new Intent(AuthActivity.this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_auth);

        backendApi = new BackendApi(this);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        pbAuthProgress = findViewById(R.id.pbAuthProgress);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performLogin();
            }
        });

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performRegister();
            }
        });

        tvForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AuthActivity.this, PasswordResetActivity.class);
                startActivity(intent);
            }
        });
    }

    private void performLogin() {
        final String email = etEmail.getText().toString().trim();
        final String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        backendApi.login(email, password, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(final JSONObject response) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setLoading(false);
                        try {
                            if (response.optBoolean("success", false) || response.has("token")) {
                                String token = response.optString("token", "");
                                if (!token.isEmpty()) {
                                    SharedPreferences prefs = getSharedPreferences("notes_prefs", Context.MODE_PRIVATE);
                                    prefs.edit().putString("auth_token", token).putString("user_email", email).apply();

                                    Toast.makeText(AuthActivity.this, "Welcome back!", Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(AuthActivity.this, MainActivity.class));
                                    finish();
                                } else {
                                    Toast.makeText(AuthActivity.this, "Login failed: No token received", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                String msg = response.optString("message", "Login failed");
                                Toast.makeText(AuthActivity.this, msg, Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Toast.makeText(AuthActivity.this, "Parsing error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }

            @Override
            public void onError(final String error) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setLoading(false);
                        Toast.makeText(AuthActivity.this, error, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private void performRegister() {
        final String email = etEmail.getText().toString().trim();
        final String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        backendApi.register(email, password, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(final JSONObject response) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (response.optBoolean("success", true)) {
                            Toast.makeText(AuthActivity.this, "Account created! Logging in...", Toast.LENGTH_SHORT).show();
                            backendApi.login(email, password, new BackendApi.ApiCallback() {
                                @Override
                                public void onSuccess(final JSONObject loginResp) {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            setLoading(false);
                                            String token = loginResp.optString("token", "");
                                            if (!token.isEmpty()) {
                                                SharedPreferences prefs = getSharedPreferences("notes_prefs", Context.MODE_PRIVATE);
                                                prefs.edit().putString("auth_token", token).putString("user_email", email).apply();

                                                startActivity(new Intent(AuthActivity.this, MainActivity.class));
                                                finish();
                                            } else {
                                                Toast.makeText(AuthActivity.this, "Auto-login failed. Please sign in.", Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });
                                }

                                @Override
                                public void onError(final String loginErr) {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            setLoading(false);
                                            Toast.makeText(AuthActivity.this, "Created! Please log in: " + loginErr, Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                            });
                        } else {
                            setLoading(false);
                            String msg = response.optString("error", "Registration failed");
                            Toast.makeText(AuthActivity.this, msg, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }

            @Override
            public void onError(final String error) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setLoading(false);
                        Toast.makeText(AuthActivity.this, error, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private void setLoading(boolean loading) {
        pbAuthProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!loading);
        btnRegister.setEnabled(!loading);
    }
}