package com.vaultlauncher.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import org.json.JSONObject;

public class LoginActivity extends Activity {

    private EditText etEmail;
    private EditText etPassword;
    private Button btnLogin;
    private Button btnRegister;
    private Button btnOffline;
    private ProgressBar loadingProgress;
    private BackendApi api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = (EditText) findViewById(R.id.et_email);
        etPassword = (EditText) findViewById(R.id.et_password);
        btnLogin = (Button) findViewById(R.id.btn_login);
        btnRegister = (Button) findViewById(R.id.btn_register);
        btnOffline = (Button) findViewById(R.id.btn_offline);
        loadingProgress = (ProgressBar) findViewById(R.id.loading_progress);

        api = new BackendApi(this);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptLogin();
            }
        });

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptRegister();
            }
        });

        btnOffline.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveOfflineFlow();
            }
        });
    }

    private void showProgress(final boolean show) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                loadingProgress.setVisibility(show ? View.VISIBLE : View.GONE);
                btnLogin.setEnabled(!show);
                btnRegister.setEnabled(!show);
                btnOffline.setEnabled(!show);
            }
        });
    }

    private void attemptLogin() {
        final String email = etEmail.getText().toString().trim();
        final String pass = etPassword.getText().toString().trim();

        if (email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Complete login details.", Toast.LENGTH_SHORT).show();
            return;
        }

        showProgress(true);
        api.login(email, pass, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(final String response) {
                showProgress(false);
                try {
                    JSONObject json = new JSONObject(response);
                    if (json.optBoolean("success", false)) {
                        String token = json.getString("token");
                        SharedPreferences.Editor editor = getSharedPreferences("VaultPrefs", MODE_PRIVATE).edit();
                        editor.putString("sync_token", token);
                        editor.putBoolean("vault_setup", true);
                        editor.putBoolean("offline_mode", false);
                        editor.putString("account_email", email);
                        editor.apply();

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(LoginActivity.this, "Cloud Sync Enabled!", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(LoginActivity.this, VaultActivity.class));
                                finish();
                            }
                        });
                    } else {
                        showError("Login failed: " + json.optString("message", "Incorrect credentials."));
                    }
                } catch (Exception e) {
                    showError("Parsing failed: " + e.getMessage());
                }
            }

            @Override
            public void onError(String error) {
                showProgress(false);
                showError("Connection failed: " + error);
            }
        });
    }

    private void attemptRegister() {
        final String email = etEmail.getText().toString().trim();
        final String pass = etPassword.getText().toString().trim();

        if (email.isEmpty() || pass.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters.", Toast.LENGTH_SHORT).show();
            return;
        }

        showProgress(true);
        // Register does NOT return token, we must authenticate post registration
        api.register(email, pass, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject json = new JSONObject(response);
                    if (json.optBoolean("success", true)) {
                        // Registration success. Now logging in instantly:
                        api.login(email, pass, new BackendApi.ApiCallback() {
                            @Override
                            public void onSuccess(String loginRes) {
                                showProgress(false);
                                try {
                                    JSONObject loginJson = new JSONObject(loginRes);
                                    if (loginJson.optBoolean("success", false)) {
                                        String token = loginJson.getString("token");
                                        SharedPreferences.Editor editor = getSharedPreferences("VaultPrefs", MODE_PRIVATE).edit();
                                        editor.putString("sync_token", token);
                                        editor.putBoolean("vault_setup", true);
                                        editor.putBoolean("offline_mode", false);
                                        editor.putString("account_email", email);
                                        editor.apply();

                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                Toast.makeText(LoginActivity.this, "Cloud Sync Setup Successful!", Toast.LENGTH_SHORT).show();
                                                startActivity(new Intent(LoginActivity.this, VaultActivity.class));
                                                finish();
                                            }
                                        });
                                    } else {
                                        showError("Cloud setup incomplete. Proceed in offline mode.");
                                    }
                                } catch (Exception e) {
                                    showError("Sync initiation error: " + e.getMessage());
                                }
                            }

                            @Override
                            public void onError(String err) {
                                showProgress(false);
                                showError("Register succeeded, but login failed: " + err);
                            }
                        });
                    } else {
                        showProgress(false);
                        showError("Error: " + json.optString("message", "Account registration denied."));
                    }
                } catch (Exception e) {
                    showProgress(false);
                    showError("Sync error: " + e.getMessage());
                }
            }

            @Override
            public void onError(String error) {
                showProgress(false);
                showError("Registration Error: " + error);
            }
        });
    }

    private void saveOfflineFlow() {
        SharedPreferences.Editor editor = getSharedPreferences("VaultPrefs", MODE_PRIVATE).edit();
        editor.putBoolean("vault_setup", true);
        editor.putBoolean("offline_mode", true);
        editor.apply();

        Toast.makeText(this, "Operating Offline Locally.", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, VaultActivity.class));
        finish();
    }

    private void showError(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(LoginActivity.this, msg, Toast.LENGTH_LONG).show();
            }
        });
    }
}