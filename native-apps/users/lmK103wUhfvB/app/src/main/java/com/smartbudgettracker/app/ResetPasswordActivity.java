package com.smartbudgettracker.app;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import org.json.JSONObject;

public class ResetPasswordActivity extends Activity {

    private EditText etResetEmail, etOTP, etNewPassword;
    private Button btnRequestOTP, btnResetPwd, btnResetBackToLogin;
    private LinearLayout layoutStep1, layoutStep2;
    private BackendApi backendApi;
    private ProgressDialog progressDialog;
    private String verifiedEmail = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        backendApi = new BackendApi(this);

        etResetEmail = (EditText) findViewById(R.id.etResetEmail);
        etOTP = (EditText) findViewById(R.id.etOTP);
        etNewPassword = (EditText) findViewById(R.id.etNewPassword);

        btnRequestOTP = (Button) findViewById(R.id.btnRequestOTP);
        btnResetPwd = (Button) findViewById(R.id.btnResetPwd);
        btnResetBackToLogin = (Button) findViewById(R.id.btnResetBackToLogin);

        layoutStep1 = (LinearLayout) findViewById(R.id.layoutStep1);
        layoutStep2 = (LinearLayout) findViewById(R.id.layoutStep2);

        btnRequestOTP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestOTPFlow();
            }
        });

        btnResetPwd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                executeResetPasswordFlow();
            }
        });

        btnResetBackToLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void requestOTPFlow() {
        final String email = etResetEmail.getText().toString().trim();
        if (email.isEmpty()) {
            Toast.makeText(this, "Please enter your email to proceed.", Toast.LENGTH_SHORT).show();
            return;
        }

        showProgress("Sending OTP security code...");
        backendApi.requestOtp(email, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(final String response) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        hideProgress();
                        try {
                            JSONObject json = new JSONObject(response);
                            if (json.optBoolean("success", false)) {
                                verifiedEmail = email;
                                Toast.makeText(ResetPasswordActivity.this, "OTP sent successfully to email.", Toast.LENGTH_LONG).show();
                                layoutStep1.setVisibility(View.GONE);
                                layoutStep2.setVisibility(View.VISIBLE);
                            } else {
                                Toast.makeText(ResetPasswordActivity.this, "Failed requesting OTP code.", Toast.LENGTH_LONG).show();
                            }
                        } catch (Exception e) {
                            Toast.makeText(ResetPasswordActivity.this, "Failed parsing OTP parameters.", Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }

            @Override
            public void onError(final String errorMessage) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        hideProgress();
                        Toast.makeText(ResetPasswordActivity.this, "OTP Error: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private void executeResetPasswordFlow() {
        String otp = etOTP.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();

        if (otp.isEmpty() || newPassword.isEmpty()) {
            Toast.makeText(this, "OTP and New Password are required.", Toast.LENGTH_SHORT).show();
            return;
        }

        showProgress("Updating secret credentials...");
        backendApi.resetPassword(verifiedEmail, otp, newPassword, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(final String response) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        hideProgress();
                        try {
                            JSONObject json = new JSONObject(response);
                            if (json.optBoolean("success", false)) {
                                Toast.makeText(ResetPasswordActivity.this, "Password updated! Please log in now.", Toast.LENGTH_LONG).show();
                                finish();
                            } else {
                                Toast.makeText(ResetPasswordActivity.this, "OTP confirmation failed.", Toast.LENGTH_LONG).show();
                            }
                        } catch (Exception e) {
                            Toast.makeText(ResetPasswordActivity.this, "Verification execution error.", Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }

            @Override
            public void onError(final String errorMessage) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        hideProgress();
                        Toast.makeText(ResetPasswordActivity.this, "Error resetting: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private void showProgress(String message) {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(message);
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    private void hideProgress() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}