package com.taskmanager.app;

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

    private EditText etEmail;
    private Button btnSendOtp;
    
    private LinearLayout layoutResetFields;
    private EditText etOtp;
    private EditText etNewPassword;
    private Button btnResetPassword;
    
    private ProgressBar progressBar;
    private TextView tvBackToLogin;
    
    private BackendApi api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        api = new BackendApi(this);

        etEmail = (EditText) findViewById(R.id.etEmail);
        btnSendOtp = (Button) findViewById(R.id.btnSendOtp);
        
        layoutResetFields = (LinearLayout) findViewById(R.id.layoutResetFields);
        etOtp = (EditText) findViewById(R.id.etOtp);
        etNewPassword = (EditText) findViewById(R.id.etNewPassword);
        btnResetPassword = (Button) findViewById(R.id.btnResetPassword);
        
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        tvBackToLogin = (TextView) findViewById(R.id.tvBackToLogin);

        btnSendOtp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestOtpCode();
            }
        });

        btnResetPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performPasswordReset();
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
        String email = etEmail.getText().toString().trim();
        if (email.isEmpty()) {
            etEmail.setError("Email address is required");
            return;
        }

        btnSendOtp.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);

        api.requestOtp(email, new BackendApi.ApiCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                progressBar.setVisibility(View.GONE);
                btnSendOtp.setVisibility(View.VISIBLE);
                Toast.makeText(ForgotPasswordActivity.this, R.string.otp_sent_success, Toast.LENGTH_LONG).show();
                
                // Show standard validation OTP input fields below
                etEmail.setEnabled(false);
                btnSendOtp.setEnabled(false);
                layoutResetFields.setVisibility(View.VISIBLE);
            }

            @Override
            public void onError(String error) {
                progressBar.setVisibility(View.GONE);
                btnSendOtp.setVisibility(View.VISIBLE);
                Toast.makeText(ForgotPasswordActivity.this, error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void performPasswordReset() {
        String email = etEmail.getText().toString().trim();
        String otp = etOtp.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();

        if (otp.isEmpty()) {
            etOtp.setError("OTP is required");
            return;
        }
        if (otp.length() != 6) {
            etOtp.setError("Enter 6-digit verification OTP");
            return;
        }
        if (newPassword.isEmpty()) {
            etNewPassword.setError("New password is required");
            return;
        }
        if (newPassword.length() < 6) {
            etNewPassword.setError("Password must be at least 6 characters");
            return;
        }

        btnResetPassword.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);

        api.resetPassword(email, otp, newPassword, new BackendApi.ApiCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                progressBar.setVisibility(View.GONE);
                btnResetPassword.setVisibility(View.VISIBLE);
                Toast.makeText(ForgotPasswordActivity.this, R.string.reset_success, Toast.LENGTH_LONG).show();
                finish(); // Successfully reset, go back to login automatically
            }

            @Override
            public void onError(String error) {
                progressBar.setVisibility(View.GONE);
                btnResetPassword.setVisibility(View.VISIBLE);
                Toast.makeText(ForgotPasswordActivity.this, error, Toast.LENGTH_LONG).show();
            }
        });
    }
}