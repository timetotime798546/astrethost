package com.expensetracker.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.json.JSONObject;

public class ForgotPasswordActivity extends Activity {

    private EditText etForgotEmail, etOtpCode, etNewPassword;
    private Button btnSendOtp, btnResetPassword;
    private ProgressBar otpProgressBar;
    private BackendApi api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        api = new BackendApi(this);

        etForgotEmail = (EditText) findViewById(R.id.etForgotEmail);
        etOtpCode = (EditText) findViewById(R.id.etOtpCode);
        etNewPassword = (EditText) findViewById(R.id.etNewPassword);
        btnSendOtp = (Button) findViewById(R.id.btnSendOtp);
        btnResetPassword = (Button) findViewById(R.id.btnResetPassword);
        otpProgressBar = (ProgressBar) findViewById(R.id.otpProgressBar);

        btnSendOtp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = etForgotEmail.getText().toString().trim();
                if (email.isEmpty()) {
                    Toast.makeText(ForgotPasswordActivity.this, "Enter your email address", Toast.LENGTH_SHORT).show();
                    return;
                }
                otpProgressBar.setVisibility(View.VISIBLE);
                btnSendOtp.setEnabled(false);

                api.requestOtp(email, new BackendApi.ApiCallback() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        otpProgressBar.setVisibility(View.GONE);
                        btnSendOtp.setEnabled(true);
                        Toast.makeText(ForgotPasswordActivity.this, "OTP sent to your email!", Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onError(String errorMessage) {
                        otpProgressBar.setVisibility(View.GONE);
                        btnSendOtp.setEnabled(true);
                        Toast.makeText(ForgotPasswordActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        btnResetPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = etForgotEmail.getText().toString().trim();
                String otp = etOtpCode.getText().toString().trim();
                String newPassword = etNewPassword.getText().toString().trim();

                if (email.isEmpty() || otp.isEmpty() || newPassword.length() < 6) {
                    Toast.makeText(ForgotPasswordActivity.this, "Fill all fields (min 6 char password)", Toast.LENGTH_SHORT).show();
                    return;
                }

                otpProgressBar.setVisibility(View.VISIBLE);
                btnResetPassword.setEnabled(false);

                api.resetPassword(email, otp, newPassword, new BackendApi.ApiCallback() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        otpProgressBar.setVisibility(View.GONE);
                        btnResetPassword.setEnabled(true);
                        Toast.makeText(ForgotPasswordActivity.this, "Password updated successfully!", Toast.LENGTH_LONG).show();
                        finish();
                    }

                    @Override
                    public void onError(String errorMessage) {
                        otpProgressBar.setVisibility(View.GONE);
                        btnResetPassword.setEnabled(true);
                        Toast.makeText(ForgotPasswordActivity.this, "Reset failed: " + errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
}