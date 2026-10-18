package com.quicknotes.app;

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
    private EditText etEmail;
    private EditText etOtp;
    private EditText etNewPassword;
    private Button btnSendOtp;
    private Button btnResetPassword;
    private ProgressBar progressBar;
    private TextView tvBack;
    private BackendApi backendApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        backendApi = new BackendApi(this);

        etEmail = (EditText) findViewById(R.id.et_fp_email);
        etOtp = (EditText) findViewById(R.id.et_fp_otp);
        etNewPassword = (EditText) findViewById(R.id.et_fp_new_password);
        btnSendOtp = (Button) findViewById(R.id.btn_send_otp);
        btnResetPassword = (Button) findViewById(R.id.btn_reset_password);
        progressBar = (ProgressBar) findViewById(R.id.progress_fp);
        tvBack = (TextView) findViewById(R.id.tv_fp_back);

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

        tvBack.setOnClickListener(new View.OnClickListener() {
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

        setLoading(true);
        backendApi.requestOtp(email, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                setLoading(false);
                Toast.makeText(ForgotPasswordActivity.this, "OTP sent to your email. Check inbox/spam.", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(String message) {
                setLoading(false);
                Toast.makeText(ForgotPasswordActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void performPasswordReset() {
        String email = etEmail.getText().toString().trim();
        String otp = etOtp.getText().toString().trim();
        String newPass = etNewPassword.getText().toString().trim();

        if (email.isEmpty() || otp.isEmpty() || newPass.isEmpty()) {
            Toast.makeText(this, "Please fill in all recovery fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (newPass.length() < 6) {
            Toast.makeText(this, "New password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        backendApi.resetPassword(email, otp, newPass, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                setLoading(false);
                Toast.makeText(ForgotPasswordActivity.this, "Password updated successfully. Please log in.", Toast.LENGTH_LONG).show();
                finish();
            }

            @Override
            public void onError(String message) {
                setLoading(false);
                Toast.makeText(ForgotPasswordActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSendOtp.setEnabled(!loading);
        btnResetPassword.setEnabled(!loading);
    }
}