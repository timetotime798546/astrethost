package com.smartnotes.app;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class LoginActivity extends Activity {

    private EditText etEmail, etPassword;
    private Button btnAction;
    private TextView tvToggleMode, tvForgot;
    private TextView tvAuthTitle;

    // Password reset panel items
    private LinearLayout layoutOtpReset;
    private Button btnRequestOtp, btnResetPasswordSubmit, btnCancelReset;
    private EditText etOtp, etNewPassword;

    private boolean isLoginMode = true;
    private BackendApi api;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        api = new BackendApi(this);

        // Redirect if already logged in
        if (api.isLoggedIn()) {
            launchMainActivity();
            return;
        }

        etEmail = (EditText) findViewById(R.id.et_email);
        etPassword = (EditText) findViewById(R.id.et_password);
        btnAction = (Button) findViewById(R.id.btn_action);
        tvToggleMode = (TextView) findViewById(R.id.tv_toggle_mode);
        tvForgot = (TextView) findViewById(R.id.tv_forgot_password);
        tvAuthTitle = (TextView) findViewById(R.id.tv_auth_title);

        layoutOtpReset = (LinearLayout) findViewById(R.id.layout_otp_reset);
        btnRequestOtp = (Button) findViewById(R.id.btn_request_otp);
        btnResetPasswordSubmit = (Button) findViewById(R.id.btn_reset_password_submit);
        btnCancelReset = (Button) findViewById(R.id.btn_cancel_reset);
        etOtp = (EditText) findViewById(R.id.et_otp);
        etNewPassword = (EditText) findViewById(R.id.et_new_password);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Working...");
        progressDialog.setCancelable(false);

        btnAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleAuthAction();
            }
        });

        tvToggleMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isLoginMode = !isLoginMode;
                if (isLoginMode) {
                    tvAuthTitle.setText("Welcome Back");
                    btnAction.setText("Log In");
                    tvToggleMode.setText("Don't have an account? Sign Up");
                    tvForgot.setVisibility(View.VISIBLE);
                } else {
                    tvAuthTitle.setText("Create Account");
                    btnAction.setText("Sign Up");
                    tvToggleMode.setText("Already have an account? Log In");
                    tvForgot.setVisibility(View.GONE);
                }
            }
        });

        tvForgot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                layoutOtpReset.setVisibility(View.VISIBLE);
                layoutOtpReset.requestFocus();
            }
        });

        btnCancelReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                layoutOtpReset.setVisibility(View.GONE);
            }
        });

        btnRequestOtp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestResetOtp();
            }
        });

        btnResetPasswordSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitPasswordReset();
            }
        });
    }

    private void handleAuthAction() {
        final String email = etEmail.getText().toString().trim();
        final String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter all details", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.show();

        if (isLoginMode) {
            // Login directly
            api.login(email, password, new BackendApi.ApiCallback<String>() {
                @Override
                public void onSuccess(String token) {
                    progressDialog.dismiss();
                    Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
                    launchMainActivity();
                }

                @Override
                public void onError(String error) {
                    progressDialog.dismiss();
                    Toast.makeText(LoginActivity.this, "Authentication failed: " + error, Toast.LENGTH_LONG).show();
                }
            });
        } else {
            // Sign Up Flow: 1. Register -> 2. Login on success (Auto-Login workflow)
            api.register(email, password, new BackendApi.ApiCallback<String>() {
                @Override
                public void onSuccess(String result) {
                    // Registration succeeded. Now trigger Login to get token
                    api.login(email, password, new BackendApi.ApiCallback<String>() {
                        @Override
                        public void onSuccess(String token) {
                            progressDialog.dismiss();
                            Toast.makeText(LoginActivity.this, "Sign up and auto-login complete!", Toast.LENGTH_SHORT).show();
                            launchMainActivity();
                        }

                        @Override
                        public void onError(String error) {
                            progressDialog.dismiss();
                            Toast.makeText(LoginActivity.this, "Registration succeeded. Please Log In.", Toast.LENGTH_LONG).show();
                            isLoginMode = true;
                            tvAuthTitle.setText("Welcome Back");
                            btnAction.setText("Log In");
                            tvToggleMode.setText("Don't have an account? Sign Up");
                            tvForgot.setVisibility(View.VISIBLE);
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    progressDialog.dismiss();
                    Toast.makeText(LoginActivity.this, "Sign up failed: " + error, Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void requestResetOtp() {
        String email = etEmail.getText().toString().trim();
        if (email.isEmpty()) {
            Toast.makeText(this, "Provide your email above first", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.show();
        api.requestOtp(email, new BackendApi.ApiCallback<String>() {
            @Override
            public void onSuccess(String result) {
                progressDialog.dismiss();
                Toast.makeText(LoginActivity.this, result, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(String error) {
                progressDialog.dismiss();
                Toast.makeText(LoginActivity.this, "Error requesting OTP: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void submitPasswordReset() {
        String email = etEmail.getText().toString().trim();
        String otp = etOtp.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();

        if (email.isEmpty() || otp.isEmpty() || newPassword.isEmpty()) {
            Toast.makeText(this, "Please fill in email, OTP, and new password.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.show();
        api.resetPassword(email, otp, newPassword, new BackendApi.ApiCallback<String>() {
            @Override
            public void onSuccess(String result) {
                progressDialog.dismiss();
                Toast.makeText(LoginActivity.this, "Password has been updated. Please Log In.", Toast.LENGTH_LONG).show();
                layoutOtpReset.setVisibility(View.GONE);
                etOtp.setText("");
                etNewPassword.setText("");
            }

            @Override
            public void onError(String error) {
                progressDialog.dismiss();
                Toast.makeText(LoginActivity.this, "Password Reset Failed: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void launchMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}