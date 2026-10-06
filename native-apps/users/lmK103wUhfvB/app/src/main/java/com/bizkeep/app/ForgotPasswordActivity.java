package com.bizkeep.app;

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

    private LinearLayout layoutRequest, layoutReset;
    private EditText etEmail, etOtp, etNewPassword;
    private Button btnRequest, btnReset;
    private ProgressBar progress;
    private TextView tvBackLogin;
    private BackendApi backendApi;

    private String currentEmail = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        backendApi = new BackendApi(this);

        layoutRequest = (LinearLayout) findViewById(R.id.layout_step_request);
        layoutReset = (LinearLayout) findViewById(R.id.layout_step_reset);

        etEmail = (EditText) findViewById(R.id.et_otp_email);
        etOtp = (EditText) findViewById(R.id.et_otp_code);
        etNewPassword = (EditText) findViewById(R.id.et_otp_new_password);

        btnRequest = (Button) findViewById(R.id.btn_request_otp);
        btnReset = (Button) findViewById(R.id.btn_reset_password);
        progress = (ProgressBar) findViewById(R.id.forgot_progress);
        tvBackLogin = (TextView) findViewById(R.id.tv_forgot_back_to_login);

        btnRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestOtpFlow();
            }
        });

        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetPasswordFlow();
            }
        });

        tvBackLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void requestOtpFlow() {
        final String email = etEmail.getText().toString().trim();
        if (email.isEmpty()) {
            Toast.makeText(this, "Please fill registered email first.", Toast.LENGTH_SHORT).show();
            return;
        }

        currentEmail = email;
        progress.setVisibility(View.VISIBLE);
        btnRequest.setEnabled(false);

        backendApi.requestOtp(email, new BackendApi.ApiCallback<String>() {
            @Override
            public void onSuccess(final String result) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progress.setVisibility(View.GONE);
                        btnRequest.setEnabled(true);
                        Toast.makeText(ForgotPasswordActivity.this, result, Toast.LENGTH_LONG).show();
                        layoutRequest.setVisibility(View.GONE);
                        layoutReset.setVisibility(View.VISIBLE);
                    }
                });
            }

            @Override
            public void onError(final String error) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progress.setVisibility(View.GONE);
                        btnRequest.setEnabled(true);
                        Toast.makeText(ForgotPasswordActivity.this, "Error: " + error, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private void resetPasswordFlow() {
        final String otp = etOtp.getText().toString().trim();
        final String newPassword = etNewPassword.getText().toString().trim();

        if (otp.isEmpty() || newPassword.isEmpty()) {
            Toast.makeText(this, "Please fill dynamic OTP and new credentials", Toast.LENGTH_SHORT).show();
            return;
        }

        progress.setVisibility(View.VISIBLE);
        btnReset.setEnabled(false);

        backendApi.resetPassword(currentEmail, otp, newPassword, new BackendApi.ApiCallback<String>() {
            @Override
            public void onSuccess(final String result) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progress.setVisibility(View.GONE);
                        btnReset.setEnabled(true);
                        Toast.makeText(ForgotPasswordActivity.this, "Password updated successfully!", Toast.LENGTH_LONG).show();
                        finish();
                    }
                });
            }

            @Override
            public void onError(final String error) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progress.setVisibility(View.GONE);
                        btnReset.setEnabled(true);
                        Toast.makeText(ForgotPasswordActivity.this, "Reset Error: " + error, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }
}