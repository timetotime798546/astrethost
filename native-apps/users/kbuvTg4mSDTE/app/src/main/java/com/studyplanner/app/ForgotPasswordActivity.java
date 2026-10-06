package com.studyplanner.app;

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
    private ProgressBar pbForgotLoading;
    private BackendApi backendApi;
    private String savedEmail = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        layoutStep1 = (LinearLayout) findViewById(R.id.layoutStep1);
        layoutStep2 = (LinearLayout) findViewById(R.id.layoutStep2);
        etForgotEmail = (EditText) findViewById(R.id.etForgotEmail);
        etOtpCode = (EditText) findViewById(R.id.etOtpCode);
        etNewPassword = (EditText) findViewById(R.id.etNewPassword);
        btnSendOtp = (Button) findViewById(R.id.btnSendOtp);
        btnResetPassword = (Button) findViewById(R.id.btnResetPassword);
        tvBackToLogin = (TextView) findViewById(R.id.tvBackToLogin);
        pbForgotLoading = (ProgressBar) findViewById(R.id.pbForgotLoading);

        backendApi = new BackendApi(this);

        btnSendOtp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestOtpCode();
            }
        });

        btnResetPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performResetPassword();
            }
        });

        tvBackToLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void requestOtpCode() {
        final String email = etForgotEmail.getText().toString().trim();
        if (email.isEmpty()) {
            Toast.makeText(this, "Please enter your email address", Toast.LENGTH_SHORT).show();
            return;
        }

        pbForgotLoading.setVisibility(View.VISIBLE);
        btnSendOtp.setEnabled(false);

        backendApi.requestOtp(email, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                pbForgotLoading.setVisibility(View.GONE);
                btnSendOtp.setEnabled(true);
                savedEmail = email;
                Toast.makeText(ForgotPasswordActivity.this, "OTP sent! Check your inbox.", Toast.LENGTH_LONG).show();
                layoutStep1.setVisibility(View.GONE);
                layoutStep2.setVisibility(View.VISIBLE);
            }

            @Override
            public void onError(String error) {
                pbForgotLoading.setVisibility(View.GONE);
                btnSendOtp.setEnabled(true);
                Toast.makeText(ForgotPasswordActivity.this, "Error sending OTP: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void performResetPassword() {
        String otp = etOtpCode.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();

        if (otp.isEmpty() || newPassword.isEmpty()) {
            Toast.makeText(this, "Please fill in all recovery fields", Toast.LENGTH_SHORT).show();
            return;
        }

        pbForgotLoading.setVisibility(View.VISIBLE);
        btnResetPassword.setEnabled(false);

        backendApi.resetPassword(savedEmail, otp, newPassword, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                pbForgotLoading.setVisibility(View.GONE);
                btnResetPassword.setEnabled(true);
                Toast.makeText(ForgotPasswordActivity.this, "Success! Please log in with your new password.", Toast.LENGTH_LONG).show();
                finish();
            }

            @Override
            public void onError(String error) {
                pbForgotLoading.setVisibility(View.GONE);
                btnResetPassword.setEnabled(true);
                Toast.makeText(ForgotPasswordActivity.this, "Verification error: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }
}