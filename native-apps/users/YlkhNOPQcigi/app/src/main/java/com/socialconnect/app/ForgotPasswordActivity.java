package com.socialconnect.app;

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

    private LinearLayout layoutStep1, layoutStep2;
    private EditText etEmail, etOtp, etNewPassword;
    private Button btnSendOtp, btnResetPassword;
    private TextView tvBackToLogin;
    private ProgressBar progressBar;

    private BackendApi backendApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        backendApi = new BackendApi(this);

        layoutStep1 = (LinearLayout) findViewById(R.id.layoutStep1);
        layoutStep2 = (LinearLayout) findViewById(R.id.layoutStep2);

        etEmail = (EditText) findViewById(R.id.etEmail);
        etOtp = (EditText) findViewById(R.id.etOtp);
        etNewPassword = (EditText) findViewById(R.id.etNewPassword);

        btnSendOtp = (Button) findViewById(R.id.btnSendOtp);
        btnResetPassword = (Button) findViewById(R.id.btnResetPassword);
        tvBackToLogin = (TextView) findViewById(R.id.tvBackToLogin);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        btnSendOtp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestOtp();
            }
        });

        btnResetPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmResetPassword();
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
            Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnSendOtp.setEnabled(false);

        backendApi.requestOtp(email, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                progressBar.setVisibility(View.GONE);
                btnSendOtp.setEnabled(true);
                Toast.makeText(ForgotPasswordActivity.this, "OTP requested. Enter details below.", Toast.LENGTH_SHORT).show();
                layoutStep1.setVisibility(View.GONE);
                layoutStep2.setVisibility(View.VISIBLE);
            }

            @Override
            public void onError(String errorMessage) {
                progressBar.setVisibility(View.GONE);
                btnSendOtp.setEnabled(true);
                Toast.makeText(ForgotPasswordActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void confirmResetPassword() {
        String email = etEmail.getText().toString().trim();
        String otp = etOtp.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();

        if (otp.isEmpty() || newPassword.isEmpty()) {
            Toast.makeText(this, "Please enter OTP and new password", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnResetPassword.setEnabled(false);

        backendApi.resetPassword(email, otp, newPassword, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                progressBar.setVisibility(View.GONE);
                btnResetPassword.setEnabled(true);
                Toast.makeText(ForgotPasswordActivity.this, "Password updated successfully. Log in with new password.", Toast.LENGTH_LONG).show();
                finish();
            }

            @Override
            public void onError(String errorMessage) {
                progressBar.setVisibility(View.GONE);
                btnResetPassword.setEnabled(true);
                Toast.makeText(ForgotPasswordActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }
}