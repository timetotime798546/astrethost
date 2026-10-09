package com.barberloyalty.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONObject;

public class ForgotPasswordActivity extends Activity {

    private LinearLayout llStep1, llStep2;
    private EditText etForgotEmail, etOtpCode, etNewPassword;
    private Button btnSendOtp, btnSubmitReset;
    private ProgressBar pbLoading;
    private TextView btnBackToLogin;
    private BackendApi backendApi;
    private String emailAddress = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        backendApi = new BackendApi(this);

        llStep1 = (LinearLayout) findViewById(R.id.llStep1);
        llStep2 = (LinearLayout) findViewById(R.id.llStep2);
        etForgotEmail = (EditText) findViewById(R.id.etForgotEmail);
        etOtpCode = (EditText) findViewById(R.id.etOtpCode);
        etNewPassword = (EditText) findViewById(R.id.etNewPassword);
        btnSendOtp = (Button) findViewById(R.id.btnSendOtp);
        btnSubmitReset = (Button) findViewById(R.id.btnSubmitReset);
        pbLoading = (ProgressBar) findViewById(R.id.pbLoading);
        btnBackToLogin = (TextView) findViewById(R.id.btnBackToLogin);

        btnSendOtp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestResetCode();
            }
        });

        btnSubmitReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                executePasswordReset();
            }
        });

        btnBackToLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void requestResetCode() {
        emailAddress = etForgotEmail.getText().toString().trim();
        if (emailAddress.isEmpty()) {
            Toast.makeText(this, "Email is required", Toast.LENGTH_SHORT).show();
            return;
        }

        pbLoading.setVisibility(View.VISIBLE);
        btnSendOtp.setEnabled(false);

        backendApi.requestOtp(emailAddress, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pbLoading.setVisibility(View.GONE);
                        btnSendOtp.setEnabled(true);
                        llStep1.setVisibility(View.GONE);
                        llStep2.setVisibility(View.VISIBLE);
                        Toast.makeText(ForgotPasswordActivity.this, "OTP sent to your email!", Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onError(final String errorMessage) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pbLoading.setVisibility(View.GONE);
                        btnSendOtp.setEnabled(true);
                        Toast.makeText(ForgotPasswordActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private void executePasswordReset() {
        String otp = etOtpCode.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();

        if (otp.isEmpty() || newPassword.isEmpty()) {
            Toast.makeText(this, "Complete all reset fields", Toast.LENGTH_SHORT).show();
            return;
        }

        pbLoading.setVisibility(View.VISIBLE);
        btnSubmitReset.setEnabled(false);

        backendApi.resetPassword(emailAddress, otp, newPassword, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pbLoading.setVisibility(View.GONE);
                        btnSubmitReset.setEnabled(true);
                        Toast.makeText(ForgotPasswordActivity.this, "Password updated successfully! Log in to continue.", Toast.LENGTH_LONG).show();
                        finish();
                    }
                });
            }

            @Override
            public void onError(final String errorMessage) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pbLoading.setVisibility(View.GONE);
                        btnSubmitReset.setEnabled(true);
                        Toast.makeText(ForgotPasswordActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }
}