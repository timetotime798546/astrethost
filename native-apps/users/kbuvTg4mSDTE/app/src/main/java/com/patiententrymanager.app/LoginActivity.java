package com.patiententrymanager.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

public class LoginActivity extends Activity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvRegisterLink, tvForgotPassword;
    private ProgressBar progressBar;
    private BackendApi backendApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        backendApi = new BackendApi(this);

        if (backendApi.getAuthToken() != null && !backendApi.getAuthToken().isEmpty()) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        etEmail = (EditText) findViewById(R.id.etLoginEmail);
        etPassword = (EditText) findViewById(R.id.etLoginPassword);
        btnLogin = (Button) findViewById(R.id.btnLogin);
        tvRegisterLink = (TextView) findViewById(R.id.tvRegisterLink);
        tvForgotPassword = (TextView) findViewById(R.id.tvForgotPassword);
        progressBar = (ProgressBar) findViewById(R.id.loginProgressBar);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performLogin();
            }
        });

        tvRegisterLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            }
        });

        tvForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showForgotPasswordDialog();
            }
        });
    }

    private void performLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            return;
        }

        setLoading(true);

        backendApi.login(email, password, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                setLoading(false);
                if (backendApi.getAuthToken() != null) {
                    Toast.makeText(LoginActivity.this, "Welcome back!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();
                } else {
                    Toast.makeText(LoginActivity.this, "Login succeeded but token was missing.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onError(String errorMessage) {
                setLoading(false);
                Toast.makeText(LoginActivity.this, "Login Failed: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showForgotPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Reset Password via OTP");

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_forgot_password, null);
        final EditText etResetEmail = (EditText) dialogView.findViewById(R.id.etResetEmail);
        final EditText etOtp = (EditText) dialogView.findViewById(R.id.etResetOtp);
        final EditText etNewPassword = (EditText) dialogView.findViewById(R.id.etResetNewPassword);
        final Button btnRequestOtp = (Button) dialogView.findViewById(R.id.btnRequestOtp);

        String currentEmail = etEmail.getText().toString().trim();
        if (!TextUtils.isEmpty(currentEmail)) {
            etResetEmail.setText(currentEmail);
        }

        btnRequestOtp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = etResetEmail.getText().toString().trim();
                if (TextUtils.isEmpty(email)) {
                    etResetEmail.setError("Enter email to receive OTP");
                    return;
                }
                btnRequestOtp.setEnabled(false);
                btnRequestOtp.setText("Sending...");

                backendApi.requestOtp(email, new BackendApi.ApiCallback() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        btnRequestOtp.setEnabled(true);
                        btnRequestOtp.setText("Resend OTP");
                        Toast.makeText(LoginActivity.this, "OTP requested. Check your email inbox.", Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onError(String errorMessage) {
                        btnRequestOtp.setEnabled(true);
                        btnRequestOtp.setText("Request OTP");
                        Toast.makeText(LoginActivity.this, "OTP Error: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });

        builder.setView(dialogView);
        builder.setPositiveButton("Reset Password", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String email = etResetEmail.getText().toString().trim();
                String otp = etOtp.getText().toString().trim();
                String newPass = etNewPassword.getText().toString().trim();

                if (TextUtils.isEmpty(email) || TextUtils.isEmpty(otp) || TextUtils.isEmpty(newPass)) {
                    Toast.makeText(LoginActivity.this, "All reset fields are required.", Toast.LENGTH_SHORT).show();
                    return;
                }

                setLoading(true);
                backendApi.resetPassword(email, otp, newPass, new BackendApi.ApiCallback() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        setLoading(false);
                        Toast.makeText(LoginActivity.this, "Password reset successfully. Please login with new password.", Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onError(String errorMessage) {
                        setLoading(false);
                        Toast.makeText(LoginActivity.this, "Reset Failed: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.create().show();
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!loading);
    }
}