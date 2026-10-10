package com.barbercraft.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.json.JSONObject;

public class ForgotPasswordActivity extends Activity {

    private BackendApi backendApi;
    private EditText etEmail;
    private EditText etOtp;
    private EditText etNewPassword;
    private Button btnRequestOtp;
    private Button btnSubmitReset;
    private ProgressBar pbLoading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        backendApi = new BackendApi(this);

        Button btnBack = (Button) findViewById(R.id.btnForgotBack);
        etEmail = (EditText) findViewById(R.id.etForgotEmail);
        etOtp = (EditText) findViewById(R.id.etForgotOtp);
        etNewPassword = (EditText) findViewById(R.id.etForgotNewPassword);
        btnRequestOtp = (Button) findViewById(R.id.btnRequestOtp);
        btnSubmitReset = (Button) findViewById(R.id.btnSubmitResetPassword);
        pbLoading = (ProgressBar) findViewById(R.id.pbForgotLoading);

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnRequestOtp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestOtpCode();
            }
        });

        btnSubmitReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitPasswordReset();
            }
        });
    }

    private void requestOtpCode() {
        String email = etEmail.getText().toString().trim();
        if (email.isEmpty()) {
            Toast.makeText(this, "Please enter your registered email", Toast.LENGTH_SHORT).show();
            return;
        }

        btnRequestOtp.setEnabled(false);
        pbLoading.setVisibility(View.VISIBLE);

        backendApi.requestOtp(email, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                btnRequestOtp.setEnabled(true);
                pbLoading.setVisibility(View.GONE);
                Toast.makeText(ForgotPasswordActivity.this, "OTP sent to your email. Check inbox/spam.", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(String errorMessage) {
                btnRequestOtp.setEnabled(true);
                pbLoading.setVisibility(View.GONE);
                Toast.makeText(ForgotPasswordActivity.this, "Failed to send OTP: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void submitPasswordReset() {
        String email = etEmail.getText().toString().trim();
        String otp = etOtp.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();

        if (email.isEmpty() || otp.isEmpty() || newPassword.isEmpty()) {
            Toast.makeText(this, "Please enter email, OTP code, and new password", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSubmitReset.setEnabled(false);
        pbLoading.setVisibility(View.VISIBLE);

        backendApi.resetPassword(email, otp, newPassword, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                btnSubmitReset.setEnabled(true);
                pbLoading.setVisibility(View.GONE);
                Toast.makeText(ForgotPasswordActivity.this, "Password updated successfully! You can now sign in.", Toast.LENGTH_LONG).show();
                finish();
            }

            @Override
            public void onError(String errorMessage) {
                btnSubmitReset.setEnabled(true);
                pbLoading.setVisibility(View.GONE);
                Toast.makeText(ForgotPasswordActivity.this, "Reset failed: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }
}