package com.cricketpro3d.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import org.json.JSONObject;

public class ForgotPasswordActivity extends Activity {

    private EditText etEmail, etOtp, etNewPassword;
    private Button btnSendOtp, btnResetPassword;
    private ProgressBar progressBar;
    private BackendApi backendApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        backendApi = new BackendApi(this);

        etEmail = (EditText) findViewById(R.id.etEmail);
        etOtp = (EditText) findViewById(R.id.etOtp);
        etNewPassword = (EditText) findViewById(R.id.etNewPassword);
        btnSendOtp = (Button) findViewById(R.id.btnSendOtp);
        btnResetPassword = (Button) findViewById(R.id.btnResetPassword);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        btnSendOtp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = etEmail.getText().toString().trim();
                if (email.isEmpty()) {
                    Toast.makeText(ForgotPasswordActivity.this, "Enter email", Toast.LENGTH_SHORT).show();
                    return;
                }
                progressBar.setVisibility(View.VISIBLE);
                btnSendOtp.setEnabled(false);

                backendApi.requestOtp(email, new BackendApi.ApiCallback() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progressBar.setVisibility(View.GONE);
                                btnSendOtp.setEnabled(true);
                                Toast.makeText(ForgotPasswordActivity.this, "OTP sent to your email", Toast.LENGTH_LONG).show();
                            }
                        });
                    }

                    @Override
                    public void onError(final String errorMessage) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progressBar.setVisibility(View.GONE);
                                btnSendOtp.setEnabled(true);
                                Toast.makeText(ForgotPasswordActivity.this, "Error: " + errorMessage, Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                });
            }
        });

        btnResetPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = etEmail.getText().toString().trim();
                String otp = etOtp.getText().toString().trim();
                String newPass = etNewPassword.getText().toString().trim();

                if (email.isEmpty() || otp.isEmpty() || newPass.isEmpty()) {
                    Toast.makeText(ForgotPasswordActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                progressBar.setVisibility(View.VISIBLE);
                btnResetPassword.setEnabled(false);

                backendApi.resetPassword(email, otp, newPass, new BackendApi.ApiCallback() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progressBar.setVisibility(View.GONE);
                                btnResetPassword.setEnabled(true);
                                Toast.makeText(ForgotPasswordActivity.this, "Password reset successful! Please sign in.", Toast.LENGTH_LONG).show();
                                finish();
                            }
                        });
                    }

                    @Override
                    public void onError(final String errorMessage) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progressBar.setVisibility(View.GONE);
                                btnResetPassword.setEnabled(true);
                                Toast.makeText(ForgotPasswordActivity.this, "Failed: " + errorMessage, Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                });
            }
        });
    }
}