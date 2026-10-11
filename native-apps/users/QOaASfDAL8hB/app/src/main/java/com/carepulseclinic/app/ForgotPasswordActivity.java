package com.carepulseclinic.app;

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
    private Button btnSendOtp, btnResetPassword;
    private LinearLayout layoutStep2;
    private ProgressBar progressBar;
    private TextView tvBackToLogin;
    private BackendApi backendApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        backendApi = new BackendApi(this);

        etEmail = findViewById(R.id.etForgotEmail);
        etOtp = findViewById(R.id.etOtp);
        etNewPassword = findViewById(R.id.etNewPassword);
        btnSendOtp = findViewById(R.id.btnSendOtp);
        btnResetPassword = findViewById(R.id.btnResetPassword);
        layoutStep2 = findViewById(R.id.layoutStep2);
        progressBar = findViewById(R.id.forgotProgressBar);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);

        btnSendOtp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestOtp();
            }
        });

        btnResetPassword.setOnClickListener(new View.OnClickListener() {
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
        String email = etEmail.getText().toString().trim();
        if (email.isEmpty()) {
            Toast.makeText(this, "Enter your email address", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnSendOtp.setEnabled(false);

        backendApi.requestOtp(email, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                progressBar.setVisibility(View.GONE);
                btnSendOtp.setEnabled(true);
                Toast.makeText(ForgotPasswordActivity.this, "OTP sent to your email!", Toast.LENGTH_SHORT).show();
                layoutStep2.setVisibility(View.VISIBLE);
                etEmail.setEnabled(false);
            }

            @Override
            public void onError(String errorMessage) {
                progressBar.setVisibility(View.GONE);
                btnSendOtp.setEnabled(true);
                Toast.makeText(ForgotPasswordActivity.this, "Error: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void resetPassword() {
        String email = etEmail.getText().toString().trim();
        String otp = etOtp.getText().toString().trim();
        String newPass = etNewPassword.getText().toString().trim();

        if (otp.isEmpty() || newPass.isEmpty()) {
            Toast.makeText(this, "Please enter OTP and new password", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnResetPassword.setEnabled(false);

        backendApi.resetPassword(email, otp, newPass, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                progressBar.setVisibility(View.GONE);
                btnResetPassword.setEnabled(true);
                Toast.makeText(ForgotPasswordActivity.this, "Password reset successful! Please log in.", Toast.LENGTH_LONG).show();
                finish();
            }

            @Override
            public void onError(String errorMessage) {
                progressBar.setVisibility(View.GONE);
                btnResetPassword.setEnabled(true);
                Toast.makeText(ForgotPasswordActivity.this, "Reset failed: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }
}