package com.modernexpensetracker.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class ForgotPasswordActivity extends Activity {

    private LinearLayout layoutStep1, layoutStep2;
    private EditText etForgotEmail, etOtpCode, etNewPassword;
    private Button btnSendOtp, btnResetPassword;
    private TextView tvBackToLogin;
    private ProgressBar progressBar;
    private BackendApi api;
    private String savedEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        api = new BackendApi(this);

        layoutStep1 = (LinearLayout) findViewById(R.id.layoutStep1);
        layoutStep2 = (LinearLayout) findViewById(R.id.layoutStep2);
        etForgotEmail = (EditText) findViewById(R.id.etForgotEmail);
        etOtpCode = (EditText) findViewById(R.id.etOtpCode);
        etNewPassword = (EditText) findViewById(R.id.etNewPassword);
        btnSendOtp = (Button) findViewById(R.id.btnSendOtp);
        btnResetPassword = (Button) findViewById(R.id.btnResetPassword);
        tvBackToLogin = (TextView) findViewById(R.id.tvBackToLogin);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        btnSendOtp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestOTP();
            }
        });

        btnResetPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetPasswordWithOTP();
            }
        });

        tvBackToLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void requestOTP() {
        final String email = etForgotEmail.getText().toString().trim();
        if (email.isEmpty()) {
            Toast.makeText(this, "Email is required", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnSendOtp.setEnabled(false);

        api.requestOtp(email, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                progressBar.setVisibility(View.GONE);
                btnSendOtp.setEnabled(true);
                savedEmail = email;
                Toast.makeText(ForgotPasswordActivity.this, "OTP sent via Gmail template successfully!", Toast.LENGTH_LONG).show();
                layoutStep1.setVisibility(View.GONE);
                layoutStep2.setVisibility(View.VISIBLE);
            }

            @Override
            public void onError(String error) {
                progressBar.setVisibility(View.GONE);
                btnSendOtp.setEnabled(true);
                Toast.makeText(ForgotPasswordActivity.this, "Error requesting OTP: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void resetPasswordWithOTP() {
        String otp = etOtpCode.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();

        if (otp.isEmpty() || newPassword.isEmpty()) {
            Toast.makeText(this, "All parameters are mandatory", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnResetPassword.setEnabled(false);

        api.resetPassword(savedEmail, otp, newPassword, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                progressBar.setVisibility(View.GONE);
                btnResetPassword.setEnabled(true);
                Toast.makeText(ForgotPasswordActivity.this, "Password Reset Complete! Please login.", Toast.LENGTH_LONG).show();
                finish();
            }

            @Override
            public void onError(String error) {
                progressBar.setVisibility(View.GONE);
                btnResetPassword.setEnabled(true);
                Toast.makeText(ForgotPasswordActivity.this, "Error: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }
}