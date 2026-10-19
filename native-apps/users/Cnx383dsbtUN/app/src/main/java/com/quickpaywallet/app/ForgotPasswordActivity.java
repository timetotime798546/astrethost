package com.quickpaywallet.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

public class ForgotPasswordActivity extends Activity {

    private EditText etEmail, etOtp, etNewPassword;
    private Button btnSendOtp, btnResetSubmit;
    private LinearLayout layoutStep1, layoutStep2;
    private ProgressBar pbLoading;
    private TextView tvBackToLogin;
    private BackendApi api;
    private String requestedEmail = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        api = new BackendApi(this);

        etEmail = (EditText) findViewById(R.id.etForgotEmail);
        etOtp = (EditText) findViewById(R.id.etOtpCode);
        etNewPassword = (EditText) findViewById(R.id.etNewPassword);

        btnSendOtp = (Button) findViewById(R.id.btnSendOtp);
        btnResetSubmit = (Button) findViewById(R.id.btnResetSubmit);

        layoutStep1 = (LinearLayout) findViewById(R.id.layoutStep1);
        layoutStep2 = (LinearLayout) findViewById(R.id.layoutStep2);
        pbLoading = (ProgressBar) findViewById(R.id.pbForgotLoading);
        tvBackToLogin = (TextView) findViewById(R.id.tvBackToLogin);

        btnSendOtp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestOtp();
            }
        });

        btnResetSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetPassword();
            }
        });

        tvBackToLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void requestOtp() {
        requestedEmail = etEmail.getText().toString().trim();
        if (requestedEmail.isEmpty() || !requestedEmail.contains("@")) {
            Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_SHORT).show();
            return;
        }

        pbLoading.setVisibility(View.VISIBLE);
        btnSendOtp.setEnabled(false);

        api.requestOtp(requestedEmail, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                pbLoading.setVisibility(View.GONE);
                btnSendOtp.setEnabled(true);
                Toast.makeText(ForgotPasswordActivity.this, "OTP sent to your email!", Toast.LENGTH_LONG).show();
                layoutStep2.setVisibility(View.VISIBLE);
            }

            @Override
            public void onError(String errorMessage) {
                pbLoading.setVisibility(View.GONE);
                btnSendOtp.setEnabled(true);
                Toast.makeText(ForgotPasswordActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void resetPassword() {
        String otp = etOtp.getText().toString().trim();
        String newPass = etNewPassword.getText().toString().trim();

        if (otp.length() < 4) {
            Toast.makeText(this, "Please enter the valid OTP code", Toast.LENGTH_SHORT).show();
            return;
        }
        if (newPass.length() < 4) {
            Toast.makeText(this, "New password must be at least 4 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        pbLoading.setVisibility(View.VISIBLE);
        btnResetSubmit.setEnabled(false);

        api.resetPassword(requestedEmail, otp, newPass, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                pbLoading.setVisibility(View.GONE);
                btnResetSubmit.setEnabled(true);
                Toast.makeText(ForgotPasswordActivity.this, "Password reset successful! Please log in.", Toast.LENGTH_LONG).show();
                finish();
            }

            @Override
            public void onError(String errorMessage) {
                pbLoading.setVisibility(View.GONE);
                btnResetSubmit.setEnabled(true);
                Toast.makeText(ForgotPasswordActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }
}