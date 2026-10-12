package com.notesapp.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.json.JSONObject;

public class PasswordResetActivity extends Activity {
    private EditText etResetEmail, etOtpCode, etNewPassword;
    private Button btnSendOtp, btnResetPassword;
    private ProgressBar pbResetProgress;
    private BackendApi backendApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password_reset);

        backendApi = new BackendApi(this);

        etResetEmail = findViewById(R.id.etResetEmail);
        etOtpCode = findViewById(R.id.etOtpCode);
        etNewPassword = findViewById(R.id.etNewPassword);
        btnSendOtp = findViewById(R.id.btnSendOtp);
        btnResetPassword = findViewById(R.id.btnResetPassword);
        pbResetProgress = findViewById(R.id.pbResetProgress);

        btnSendOtp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestOtp();
            }
        });

        btnResetPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performPasswordReset();
            }
        });
    }

    private void requestOtp() {
        String email = etResetEmail.getText().toString().trim();
        if (email.isEmpty()) {
            Toast.makeText(this, "Enter your email address", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        backendApi.requestOtp(email, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(final JSONObject response) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setLoading(false);
                        Toast.makeText(PasswordResetActivity.this, "OTP Code sent to email!", Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onError(final String error) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setLoading(false);
                        Toast.makeText(PasswordResetActivity.this, error, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private void performPasswordReset() {
        String email = etResetEmail.getText().toString().trim();
        String otp = etOtpCode.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();

        if (email.isEmpty() || otp.isEmpty() || newPassword.isEmpty()) {
            Toast.makeText(this, "Please complete all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        backendApi.resetPassword(email, otp, newPassword, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(final JSONObject response) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setLoading(false);
                        Toast.makeText(PasswordResetActivity.this, "Password reset successful! Please log in.", Toast.LENGTH_LONG).show();
                        finish();
                    }
                });
            }

            @Override
            public void onError(final String error) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setLoading(false);
                        Toast.makeText(PasswordResetActivity.this, error, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private void setLoading(boolean loading) {
        pbResetProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSendOtp.setEnabled(!loading);
        btnResetPassword.setEnabled(!loading);
    }
}