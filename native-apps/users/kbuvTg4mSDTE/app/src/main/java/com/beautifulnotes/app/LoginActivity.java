package com.beautifulnotes.app;

import android.app.Activity;
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

public class LoginActivity extends Activity {

    private EditText etEmail;
    private EditText etPassword;
    private Button btnAction;
    private TextView tvSwitchMode;
    private ProgressBar pbLogin;

    private BackendApi backendApi;
    private boolean isLoginMode = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = (EditText) findViewById(R.id.et_email);
        etPassword = (EditText) findViewById(R.id.et_password);
        btnAction = (Button) findViewById(R.id.btn_action);
        tvSwitchMode = (TextView) findViewById(R.id.tv_switch_mode);
        pbLogin = (ProgressBar) findViewById(R.id.pb_login);

        backendApi = new BackendApi(this);

        // Auto-login validation check using Secure token persistence
        SharedPreferences prefs = getSharedPreferences("beautiful_notes_prefs", MODE_PRIVATE);
        String savedToken = prefs.getString("auth_token", null);
        if (savedToken != null) {
            proceedToMain(savedToken);
            return;
        }

        btnAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performAuthentication();
            }
        });

        tvSwitchMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isLoginMode = !isLoginMode;
                if (isLoginMode) {
                    btnAction.setText("Sign In");
                    tvSwitchMode.setText("Don't have an account? Create one");
                } else {
                    btnAction.setText("Create Account");
                    tvSwitchMode.setText("Already have an account? Sign In");
                }
            }
        });
    }

    private void performAuthentication() {
        final String email = etEmail.getText().toString().trim();
        final String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Complete all inputs to continue", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoadingState(true);

        if (isLoginMode) {
            // Straightforward execution of authorization call
            backendApi.login(email, password, new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(final JSONObject response) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setLoadingState(false);
                            String token = response.optString("token", "");
                            if (!token.isEmpty()) {
                                saveToken(token);
                                proceedToMain(token);
                            } else {
                                Toast.makeText(LoginActivity.this, "Authentication sequence rejected token parse.", Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                }

                @Override
                public void onError(final String errorMessage) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setLoadingState(false);
                            Toast.makeText(LoginActivity.this, "Login issue: " + errorMessage, Toast.LENGTH_LONG).show();
                        }
                    });
                }
            });
        } else {
            // Register flow -> Clean register without expecting a token, followed by structured auto-login
            backendApi.register(email, password, new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(final JSONObject response) {
                    // Registration succeeded, now run separate authorization handshake
                    backendApi.login(email, password, new BackendApi.ApiCallback() {
                        @Override
                        public void onSuccess(final JSONObject loginResponse) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    setLoadingState(false);
                                    String token = loginResponse.optString("token", "");
                                    if (!token.isEmpty()) {
                                        saveToken(token);
                                        proceedToMain(token);
                                    } else {
                                        Toast.makeText(LoginActivity.this, "Could not fetch session security handshake.", Toast.LENGTH_LONG).show();
                                    }
                                }
                            });
                        }

                        @Override
                        public void onError(final String errorMsg) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    setLoadingState(false);
                                    Toast.makeText(LoginActivity.this, "Registration built, session authorization failed: " + errorMsg, Toast.LENGTH_LONG).show();
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
                            setLoadingState(false);
                            Toast.makeText(LoginActivity.this, "Account registry mismatch: " + errorMessage, Toast.LENGTH_LONG).show();
                        }
                    });
                }
            });
        }
    }

    private void saveToken(String token) {
        SharedPreferences.Editor editor = getSharedPreferences("beautiful_notes_prefs", MODE_PRIVATE).edit();
        editor.putString("auth_token", token);
        editor.apply();
    }

    private void setLoadingState(boolean isLoading) {
        if (isLoading) {
            btnAction.setEnabled(false);
            pbLogin.setVisibility(View.VISIBLE);
        } else {
            btnAction.setEnabled(true);
            pbLogin.setVisibility(View.GONE);
        }
    }

    private void proceedToMain(String token) {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.putExtra("auth_token", token);
        startActivity(intent);
        finish();
    }
}