package com.shozicapremor.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

public class LoginActivity extends Activity {

    private EditText etEmail, etPassword;
    private Button btnAction;
    private TextView btnSwitchMode, btnForgotPassword, authTitle, errorText;
    private ProgressBar progressLoading;

    private BackendApi api;
    private boolean isLoginMode = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        api = new BackendApi(this);

        if (api.isUserLoggedIn()) {
            launchMain();
            return;
        }

        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnAction = findViewById(R.id.btn_action);
        btnSwitchMode = findViewById(R.id.btn_switch_mode);
        btnForgotPassword = findViewById(R.id.btn_forgot_password);
        authTitle = findViewById(R.id.auth_title);
        errorText = findViewById(R.id.error_text);
        progressLoading = findViewById(R.id.progress_loading);

        btnAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleSubmit();
            }
        });

        btnSwitchMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isLoginMode = !isLoginMode;
                toggleMode();
            }
        });

        btnForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
                startActivity(intent);
            }
        });
    }

    private void toggleMode() {
        errorText.setVisibility(View.GONE);
        if (isLoginMode) {
            authTitle.setText("Welcome Back");
            btnAction.setText("LOG IN");
            btnSwitchMode.setText("Don't have an account? Sign Up");
            btnForgotPassword.setVisibility(View.VISIBLE);
        } else {
            authTitle.setText("Create Shozica Profile");
            btnAction.setText("SIGN UP & JOIN");
            btnSwitchMode.setText("Already registered? Log In");
            btnForgotPassword.setVisibility(View.GONE);
        }
    }

    private void handleSubmit() {
        errorText.setVisibility(View.GONE);
        final String email = etEmail.getText().toString().trim();
        final String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Fields cannot be empty.");
            return;
        }

        progressLoading.setVisibility(View.VISIBLE);
        btnAction.setVisibility(View.GONE);

        if (isLoginMode) {
            api.loginUser(email, password, new BackendApi.ApiCallback<String>() {
                @Override
                public void onSuccess(String token) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressLoading.setVisibility(View.GONE);
                            btnAction.setVisibility(View.VISIBLE);
                            launchMain();
                        }
                    });
                }

                @Override
                public void onError(final String error) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressLoading.setVisibility(View.GONE);
                            btnAction.setVisibility(View.VISIBLE);
                            showError("Login Error: " + error);
                        }
                    });
                }
            });
        } else {
            api.registerUser(email, password, new BackendApi.ApiCallback<org.json.JSONObject>() {
                @Override
                public void onSuccess(org.json.JSONObject result) {
                    // Successful registration doesn't yield a session token, so login immediately
                    api.loginUser(email, password, new BackendApi.ApiCallback<String>() {
                        @Override
                        public void onSuccess(String token) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    progressLoading.setVisibility(View.GONE);
                                    btnAction.setVisibility(View.VISIBLE);
                                    launchMain();
                                }
                            });
                        }

                        @Override
                        public void onError(final String error) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    progressLoading.setVisibility(View.GONE);
                                    btnAction.setVisibility(View.VISIBLE);
                                    showError("Auto login error: " + error);
                                }
                            });
                        }
                    });
                }

                @Override
                public void onError(final String error) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressLoading.setVisibility(View.GONE);
                            btnAction.setVisibility(View.VISIBLE);
                            showError("Sign Up Error: " + error);
                        }
                    });
                }
            });
        }
    }

    private void showError(String message) {
        errorText.setText(message);
        errorText.setVisibility(View.VISIBLE);
    }

    private void launchMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}