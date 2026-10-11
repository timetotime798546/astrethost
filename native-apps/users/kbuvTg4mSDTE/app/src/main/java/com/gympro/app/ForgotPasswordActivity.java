package com.gympro.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.json.JSONObject;

public class ForgotPasswordActivity extends Activity {

    private EditText etResetEmail, etOtpCode, etNewPassword;
    private Button btnRequestOtp, btnResetPassword, btnBackToLogin;
    private ProgressBar resetProgressBar;
    private BackendApi backendApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        backendApi = new BackendApi(this);

        etResetEmail = findViewById(R.id.etResetEmail);
        etOtpCode = findViewById(R.id.etOtpCode);
        etNewPassword = findViewById(R.id.etNewPassword);
        btnRequestOtp = findViewById(R.id.btnRequestOtp);
        btnResetPassword = findViewById(R.id.btnResetPassword);
        btnBackToLogin = findViewById(R.id.btnBackToLogin);
        resetProgressBar = findViewById(R.id.resetProgressBar);

        btnRequestOtp.setOnClickListener(new View.OnClickListener() {
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

        btnBackToLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void requestOtp() {
        String email = etResetEmail.getText().toString().trim();
        if (email.isEmpty()) {
            Toast.makeText(this, "Enter your email address", Toast.LENGTH_SHORT).show();
            return;
        }

        resetProgressBar.setVisibility(View.VISIBLE);
        btnRequestOtp.setEnabled(false);

        backendApi.requestOtp(email, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(String responseStr, JSONObject jsonResponse) {
                resetProgressBar.setVisibility(View.GONE);
                btnRequestOtp.setEnabled(true);
                Toast.makeText(ForgotPasswordActivity.this, "OTP sent to your email!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String errorMessage) {
                resetProgressBar.setVisibility(View.GONE);
                btnRequestOtp.setEnabled(true);
                Toast.makeText(ForgotPasswordActivity.this, "Failed to send OTP: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void resetPassword() {
        String email = etResetEmail.getText().toString().trim();
        String otp = etOtpCode.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();

        if (email.isEmpty() || otp.isEmpty() || newPassword.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        resetProgressBar.setVisibility(View.VISIBLE);
        btnResetPassword.setEnabled(false);

        backendApi.resetPassword(email, otp, newPassword, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(String responseStr, JSONObject jsonResponse) {
                resetProgressBar.setVisibility(View.GONE);
                btnResetPassword.setEnabled(true);
                Toast.makeText(ForgotPasswordActivity.this, "Password reset successful! Please login.", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(String errorMessage) {
                resetProgressBar.setVisibility(View.GONE);
                btnResetPassword.setEnabled(true);
                Toast.makeText(ForgotPasswordActivity.this, "Reset failed: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }
}