package com.omniflow.erp;

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

import org.json.JSONObject;

public class LoginActivity extends Activity {
    private BackendApi api;
    
    private EditText etEmail, etPassword;
    private Button btnLogin, btnRegister, btnForgotPassword;
    
    private LinearLayout layoutNormal, layoutOtp;
    private EditText etOtpEmail, etOtpCode, etOtpNewPassword;
    private Button btnSendOtp, btnVerifyAndReset, btnCancelOtp;

    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        api = new BackendApi(this);

        etEmail = (EditText) findViewById(R.id.etEmail);
        etPassword = (EditText) findViewById(R.id.etPassword);
        btnLogin = (Button) findViewById(R.id.btnLogin);
        btnRegister = (Button) findViewById(R.id.btnRegister);
        btnForgotPassword = (Button) findViewById(R.id.btnForgotPassword);

        layoutNormal = (LinearLayout) findViewById(R.id.layoutNormal);
        layoutOtp = (LinearLayout) findViewById(R.id.layoutOtp);
        etOtpEmail = (EditText) findViewById(R.id.etOtpEmail);
        etOtpCode = (EditText) findViewById(R.id.etOtpCode);
        etOtpNewPassword = (EditText) findViewById(R.id.etOtpNewPassword);
        btnSendOtp = (Button) findViewById(R.id.btnSendOtp);
        btnVerifyAndReset = (Button) findViewById(R.id.btnVerifyAndReset);
        btnCancelOtp = (Button) findViewById(R.id.btnCancelOtp);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Processing request...");
        progressDialog.setCancelable(false);

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

        btnForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchToOtpLayout(true);
            }
        });

        btnCancelOtp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchToOtpLayout(false);
            }
        });

        btnSendOtp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestResetOtp();
            }
        });

        btnVerifyAndReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                verifyAndResetPassword();
            }
        });
    }

    private void switchToOtpLayout(boolean otpActive) {
        if (otpActive) {
            layoutNormal.setVisibility(View.GONE);
            layoutOtp.setVisibility(View.VISIBLE);
        } else {
            layoutNormal.setVisibility(View.VISIBLE);
            layoutOtp.setVisibility(View.GONE);
        }
    }

    private void performLogin() {
        final String email = etEmail.getText().toString().trim();
        final String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all credentials", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.show();
        api.login(email, password, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                progressDialog.dismiss();
                try {
                    String token = response.getString("token");
                    api.setToken(token);
                    api.setEmail(email);
                    Toast.makeText(LoginActivity.this, "Authentication successful!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(LoginActivity.this, ERPActivity.class));
                    finish();
                } catch (Exception e) {
                    Toast.makeText(LoginActivity.this, "Authentication parsing failure: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onError(String message) {
                progressDialog.dismiss();
                Toast.makeText(LoginActivity.this, "Login Failed: " + message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void performRegister() {
        final String email = etEmail.getText().toString().trim();
        final String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email and password to register", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.show();
        api.register(email, password, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                api.login(email, password, new BackendApi.ApiCallback() {
                    @Override
                    public void onSuccess(JSONObject loginResponse) {
                        progressDialog.dismiss();
                        try {
                            String token = loginResponse.getString("token");
                            api.setToken(token);
                            api.setEmail(email);
                            Toast.makeText(LoginActivity.this, "Registered & logged in successfully!", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(LoginActivity.this, ERPActivity.class));
                            finish();
                        } catch (Exception e) {
                            Toast.makeText(LoginActivity.this, "Automatic login success, token missing: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onError(String loginError) {
                        progressDialog.dismiss();
                        Toast.makeText(LoginActivity.this, "Registration succeeded, please login manually.", Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onError(String message) {
                progressDialog.dismiss();
                Toast.makeText(LoginActivity.this, "Registration Failed: " + message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void requestResetOtp() {
        final String email = etOtpEmail.getText().toString().trim();
        if (email.isEmpty()) {
            Toast.makeText(this, "Please enter your registered email address", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.show();
        api.requestOtp(email, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                progressDialog.dismiss();
                Toast.makeText(LoginActivity.this, "Reset Code sent to your email! Check spam.", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(String message) {
                progressDialog.dismiss();
                Toast.makeText(LoginActivity.this, "OTP Failed: " + message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void verifyAndResetPassword() {
        final String email = etOtpEmail.getText().toString().trim();
        final String otp = etOtpCode.getText().toString().trim();
        final String newPass = etOtpNewPassword.getText().toString().trim();

        if (email.isEmpty() || otp.isEmpty() || newPass.isEmpty()) {
            Toast.makeText(this, "Please complete all OTP fields", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.show();
        api.resetPassword(email, otp, newPass, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                progressDialog.dismiss();
                Toast.makeText(LoginActivity.this, "Password updated successfully! Log in now.", Toast.LENGTH_LONG).show();
                switchToOtpLayout(false);
            }

            @Override
            public void onError(String message) {
                progressDialog.dismiss();
                Toast.makeText(LoginActivity.this, "Reset Verification failed: " + message, Toast.LENGTH_LONG).show();
            }
        });
    }
}