package com.cloudnotespro.app;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

public class ForgotPasswordActivity extends Activity {
    private LinearLayout layoutStep1, layoutStep2;
    private EditText forgotEmail, editOtp, editNewPassword;
    private Button btnSendOtp, btnResetPassword;
    private View btnBackFromForgot;
    private BackendApi backendApi;
    private ProgressDialog progressDialog;
    private String savedEmail = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        backendApi = new BackendApi(this);

        layoutStep1 = (LinearLayout) findViewById(R.id.layoutStep1);
        layoutStep2 = (LinearLayout) findViewById(R.id.layoutStep2);

        forgotEmail = (EditText) findViewById(R.id.forgotEmail);
        editOtp = (EditText) findViewById(R.id.editOtp);
        editNewPassword = (EditText) findViewById(R.id.editNewPassword);

        btnSendOtp = (Button) findViewById(R.id.btnSendOtp);
        btnResetPassword = (Button) findViewById(R.id.btnResetPassword);
        btnBackFromForgot = findViewById(R.id.btnBackFromForgot);

        btnSendOtp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SoundHelper.playClick(ForgotPasswordActivity.this);
                requestOtp();
            }
        });

        btnResetPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SoundHelper.playClick(ForgotPasswordActivity.this);
                resetPassword();
            }
        });

        btnBackFromForgot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SoundHelper.playClick(ForgotPasswordActivity.this);
                finish();
            }
        });
    }

    private void requestOtp() {
        final String email = forgotEmail.getText().toString().trim();
        if (email.isEmpty()) {
            SoundHelper.playError(ForgotPasswordActivity.this);
            Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog = ProgressDialog.show(this, "Processing Request", "Verifying email context...", true);

        backendApi.requestOtp(email, new BackendApi.ApiCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.dismiss();
                        SoundHelper.playSuccess(ForgotPasswordActivity.this);
                        savedEmail = email;
                        layoutStep1.setVisibility(View.GONE);
                        layoutStep2.setVisibility(View.VISIBLE);
                        Toast.makeText(ForgotPasswordActivity.this, "A dynamic recovery key has been dispatched to your inbox.", Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onError(final String errorMessage) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.dismiss();
                        SoundHelper.playError(ForgotPasswordActivity.this);
                        Toast.makeText(ForgotPasswordActivity.this, "Failed: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private void resetPassword() {
        final String otp = editOtp.getText().toString().trim();
        final String newPassword = editNewPassword.getText().toString().trim();

        if (otp.isEmpty() || newPassword.isEmpty()) {
            SoundHelper.playError(ForgotPasswordActivity.this);
            Toast.makeText(this, "Ensure both values are entered correctly", Toast.LENGTH_SHORT).show();
            return;
        }

        if (newPassword.length() < 6) {
            SoundHelper.playError(ForgotPasswordActivity.this);
            Toast.makeText(this, "New credential has to be 6 characters or longer", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog = ProgressDialog.show(this, "Resetting", "Saving changes in cloud...", true);

        backendApi.resetPassword(savedEmail, otp, newPassword, new BackendApi.ApiCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.dismiss();
                        SoundHelper.playSuccess(ForgotPasswordActivity.this);
                        Toast.makeText(ForgotPasswordActivity.this, "Successfully updated credentials. Return to Login.", Toast.LENGTH_LONG).show();
                        finish();
                    }
                });
            }

            @Override
            public void onError(final String errorMessage) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.dismiss();
                        SoundHelper.playError(ForgotPasswordActivity.this);
                        Toast.makeText(ForgotPasswordActivity.this, "Error: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }
}