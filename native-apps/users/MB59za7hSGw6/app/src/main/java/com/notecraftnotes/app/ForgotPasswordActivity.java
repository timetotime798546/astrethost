package com.notecraftnotes.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

public class ForgotPasswordActivity extends Activity {

    private EditText etEmail, etOtp, etNewPassword;
    private Button btnRequestOtp, btnResetPassword;
    private ProgressBar pbForgot;
    private TextView tvBackToLogin;
    private BackendApi api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        api = new BackendApi(this);

        etEmail = findViewById(R.id.etForgotEmail);
        etOtp = findViewById(R.id.etForgotOtp);
        etNewPassword = findViewById(R.id.etNewPassword);
        btnRequestOtp = findViewById(R.id.btnRequestOtp);
        btnResetPassword = findViewById(R.id.btnResetPassword);
        pbForgot = findViewById(R.id.pbForgot);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);

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
            Toast.makeText(this, "Please enter your registered email", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        api.requestOtp(email, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                setLoading(false);
                Toast.makeText(ForgotPasswordActivity.this, "OTP request sent to your email", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(String error) {
                setLoading(false);
                Toast.makeText(ForgotPasswordActivity.this, "OTP Request Error: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void resetPassword() {
        String email = etEmail.getText().toString().trim();
        String otp = etOtp.getText().toString().trim();
        String newPass = etNewPassword.getText().toString().trim();

        if (email.isEmpty() || otp.isEmpty() || newPass.isEmpty()) {
            Toast.makeText(this, "Please fill in email, OTP, and new password", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        api.resetPassword(email, otp, newPass, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                setLoading(false);
                Toast.makeText(ForgotPasswordActivity.this, "Password reset successfully! Please log in.", Toast.LENGTH_LONG).show();
                finish();
            }

            @Override
            public void onError(String error) {
                setLoading(false);
                Toast.makeText(ForgotPasswordActivity.this, "Reset Error: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        pbForgot.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnRequestOtp.setEnabled(!loading);
        btnResetPassword.setEnabled(!loading);
    }
}