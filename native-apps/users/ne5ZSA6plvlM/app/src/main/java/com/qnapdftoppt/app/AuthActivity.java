package com.qnapdftoppt.app;

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

public class AuthActivity extends Activity {

    private EditText etEmail, etPassword, etOtp;
    private Button btnSubmit;
    private TextView tvFormTitle, tvToggleMode, tvForgotPassword, tvAppHeader;
    private ProgressBar progressBar;

    private BackendApi backendApi;
    private boolean isLoginMode = true;
    private boolean isOtpMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        backendApi = new BackendApi(this);

        if (backendApi.isLoggedIn()) {
            openDashboard();
            return;
        }

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etOtp = findViewById(R.id.etOtp);
        btnSubmit = findViewById(R.id.btnSubmit);
        tvFormTitle = findViewById(R.id.tvFormTitle);
        tvToggleMode = findViewById(R.id.tvToggleMode);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvAppHeader = findViewById(R.id.tvAppHeader);
        progressBar = findViewById(R.id.progressBar);

        // Fetch dynamic title from backend admin config
        backendApi.fetchAdminConfig(new BackendApi.ConfigCallback() {
            @Override
            public void onConfigLoaded(String appTitle, String watermark) {
                if (tvAppHeader != null) {
                    tvAppHeader.setText(appTitle);
                }
            }
        });

        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleFormSubmit();
            }
        });

        tvToggleMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isOtpMode = false;
                isLoginMode = !isLoginMode;
                updateUiState();
            }
        });

        tvForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isOtpMode) {
                    requestPasswordOtp();
                }
            }
        });
    }

    private void updateUiState() {
        etOtp.setVisibility(isOtpMode ? View.VISIBLE : View.GONE);
        if (isOtpMode) {
            tvFormTitle.setText("Reset Password via OTP");
            btnSubmit.setText("Reset Password");
            etPassword.setHint("New Password");
            tvToggleMode.setText("Back to Login");
            tvForgotPassword.setVisibility(View.GONE);
        } else if (isLoginMode) {
            tvFormTitle.setText("Account Login");
            btnSubmit.setText("Login");
            etPassword.setHint("Password");
            tvToggleMode.setText("Don't have an account? Create one");
            tvForgotPassword.setVisibility(View.VISIBLE);
        } else {
            tvFormTitle.setText("Create New Account");
            btnSubmit.setText("Register");
            etPassword.setHint("Password");
            tvToggleMode.setText("Already have an account? Login");
            tvForgotPassword.setVisibility(View.GONE);
        }
    }

    private void handleFormSubmit() {
        final String email = etEmail.getText().toString().trim();
        final String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnSubmit.setEnabled(false);

        if (isOtpMode) {
            String otp = etOtp.getText().toString().trim();
            if (otp.isEmpty()) {
                progressBar.setVisibility(View.GONE);
                btnSubmit.setEnabled(true);
                Toast.makeText(this, "Please enter the OTP sent to your email", Toast.LENGTH_SHORT).show();
                return;
            }
            backendApi.resetPassword(email, otp, password, new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(String responseText, JSONObject jsonResponse) {
                    progressBar.setVisibility(View.GONE);
                    btnSubmit.setEnabled(true);
                    Toast.makeText(AuthActivity.this, "Password reset successful! Please login.", Toast.LENGTH_LONG).show();
                    isOtpMode = false;
                    isLoginMode = true;
                    updateUiState();
                }

                @Override
                public void onError(String errorMessage) {
                    progressBar.setVisibility(View.GONE);
                    btnSubmit.setEnabled(true);
                    Toast.makeText(AuthActivity.this, "OTP Error: " + errorMessage, Toast.LENGTH_LONG).show();
                }
            });
        } else if (isLoginMode) {
            backendApi.login(email, password, new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(String responseText, JSONObject jsonResponse) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(AuthActivity.this, "Login Successful!", Toast.LENGTH_SHORT).show();
                    openDashboard();
                }

                @Override
                public void onError(String errorMessage) {
                    progressBar.setVisibility(View.GONE);
                    btnSubmit.setEnabled(true);
                    Toast.makeText(AuthActivity.this, "Login Failed: " + errorMessage, Toast.LENGTH_LONG).show();
                }
            });
        } else {
            // Registration mode - call register then login for token
            backendApi.register(email, password, new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(String responseText, JSONObject jsonResponse) {
                    // Registration succeeded - now perform auto login to fetch token
                    backendApi.login(email, password, new BackendApi.ApiCallback() {
                        @Override
                        public void onSuccess(String responseText, JSONObject jsonResponse) {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(AuthActivity.this, "Registration & Login Successful!", Toast.LENGTH_SHORT).show();
                            openDashboard();
                        }

                        @Override
                        public void onError(String errorMessage) {
                            progressBar.setVisibility(View.GONE);
                            btnSubmit.setEnabled(true);
                            Toast.makeText(AuthActivity.this, "Registered! Please login manually.", Toast.LENGTH_LONG).show();
                            isLoginMode = true;
                            updateUiState();
                        }
                    });
                }

                @Override
                public void onError(String errorMessage) {
                    progressBar.setVisibility(View.GONE);
                    btnSubmit.setEnabled(true);
                    Toast.makeText(AuthActivity.this, "Registration Failed: " + errorMessage, Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void requestPasswordOtp() {
        String email = etEmail.getText().toString().trim();
        if (email.isEmpty()) {
            Toast.makeText(this, "Enter your email to receive OTP", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        backendApi.requestOtp(email, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(String responseText, JSONObject jsonResponse) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(AuthActivity.this, "OTP sent to your email!", Toast.LENGTH_LONG).show();
                isOtpMode = true;
                updateUiState();
            }

            @Override
            public void onError(String errorMessage) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(AuthActivity.this, "Failed to send OTP: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void openDashboard() {
        Intent intent = new Intent(AuthActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}