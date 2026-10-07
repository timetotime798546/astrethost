package com.shozicapremor.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

public class ForgotPasswordActivity extends Activity {

    private TextView btnBack, otpStatus;
    private LinearLayout containerStep1, containerStep2;
    private EditText etRecoverEmail, etOtp, etNewPassword;
    private Button btnRequestOtp, btnResetPassword;
    private ProgressBar otpProgress;

    private BackendApi api;
    private String userEmail = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        api = new BackendApi(this);

        btnBack = findViewById(R.id.btn_back);
        otpStatus = findViewById(R.id.otp_status);
        containerStep1 = findViewById(R.id.container_step1);
        containerStep2 = findViewById(R.id.container_step2);
        etRecoverEmail = findViewById(R.id.et_recover_email);
        etOtp = findViewById(R.id.et_otp);
        etNewPassword = findViewById(R.id.et_new_password);
        btnRequestOtp = findViewById(R.id.btn_request_otp);
        btnResetPassword = findViewById(R.id.btn_reset_password);
        otpProgress = findViewById(R.id.otp_progress);

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnRequestOtp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleOtpRequest();
            }
        });

        btnResetPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handlePasswordReset();
            }
        });
    }

    private void handleOtpRequest() {
        final String email = etRecoverEmail.getText().toString().trim();
        if (email.isEmpty()) {
            showStatus("Please enter your email", true);
            return;
        }

        userEmail = email;
        otpProgress.setVisibility(View.VISIBLE);
        btnRequestOtp.setEnabled(false);

        api.requestOtp(email, new BackendApi.ApiCallback<String>() {
            @Override
            public void onSuccess(final String result) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        otpProgress.setVisibility(View.GONE);
                        showStatus(result, false);
                        containerStep1.setVisibility(View.GONE);
                        containerStep2.setVisibility(View.VISIBLE);
                    }
                });
            }

            @Override
            public void onError(final String error) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        otpProgress.setVisibility(View.GONE);
                        btnRequestOtp.setEnabled(true);
                        showStatus("Failed: " + error, true);
                    }
                });
            }
        });
    }

    private void handlePasswordReset() {
        String otp = etOtp.getText().toString().trim();
        String newPass = etNewPassword.getText().toString().trim();

        if (otp.isEmpty() || newPass.isEmpty()) {
            showStatus("OTP and password must be filled.", true);
            return;
        }

        otpProgress.setVisibility(View.VISIBLE);
        btnResetPassword.setEnabled(false);

        api.resetPassword(userEmail, otp, newPass, new BackendApi.ApiCallback<String>() {
            @Override
            public void onSuccess(final String result) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        otpProgress.setVisibility(View.GONE);
                        showStatus("Success! " + result + ". Go back to login.", false);
                    }
                });
            }

            @Override
            public void onError(final String error) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        otpProgress.setVisibility(View.GONE);
                        btnResetPassword.setEnabled(true);
                        showStatus("Error resetting: " + error, true);
                    }
                });
            }
        });
    }

    private void showStatus(String text, boolean isError) {
        otpStatus.setText(text);
        otpStatus.setTextColor(isError ? getResources().getColor(R.color.status_red) : getResources().getColor(R.color.status_green));
        otpStatus.setVisibility(View.VISIBLE);
    }
}