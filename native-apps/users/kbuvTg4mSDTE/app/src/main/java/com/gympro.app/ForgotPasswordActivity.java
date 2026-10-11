package com.gympro.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONObject;

public class ForgotPasswordActivity extends Activity {
    private EditText etEmail, etOtp, etNewPassword;
    private Button btnRequestOtp, btnSubmitReset;
    private LinearLayout layoutOtp;
    private TextView tvStatus, tvBack;
    private ProgressBar pbForgot;
    private BackendApi api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        api = new BackendApi(this);

        etEmail = findViewById(R.id.etResetEmail);
        etOtp = findViewById(R.id.etResetOtp);
        etNewPassword = findViewById(R.id.etResetNewPassword);
        btnRequestOtp = findViewById(R.id.btnRequestOtp);
        btnSubmitReset = findViewById(R.id.btnSubmitReset);
        layoutOtp = findViewById(R.id.layoutResetOtp);
        tvStatus = findViewById(R.id.tvForgotStatus);
        tvBack = findViewById(R.id.tvForgotBack);
        pbForgot = findViewById(R.id.pbForgot);

        btnRequestOtp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestOtp();
            }
        });

        btnSubmitReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetPassword();
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
            showMessage("Please enter email address", true);
            return;
        }

        pbForgot.setVisibility(View.VISIBLE);
        btnRequestOtp.setEnabled(false);

        api.requestOtp(email, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                pbForgot.setVisibility(View.GONE);
                btnRequestOtp.setEnabled(true);
                layoutOtp.setVisibility(View.VISIBLE);
                showMessage("OTP code sent to email!", false);
            }

            @Override
            public void onError(String errorMessage) {
                pbForgot.setVisibility(View.GONE);
                btnRequestOtp.setEnabled(true);
                showMessage(errorMessage, true);
            }
        });
    }

    private void resetPassword() {
        String email = etEmail.getText().toString().trim();
        String otp = etOtp.getText().toString().trim();
        String newPass = etNewPassword.getText().toString().trim();

        if (otp.isEmpty() || newPass.isEmpty()) {
            showMessage("Enter OTP and new password", true);
            return;
        }

        pbForgot.setVisibility(View.VISIBLE);
        btnSubmitReset.setEnabled(false);

        api.resetPassword(email, otp, newPass, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                pbForgot.setVisibility(View.GONE);
                btnSubmitReset.setEnabled(true);
                showMessage("Password updated! Return to login.", false);
            }

            @Override
            public void onError(String errorMessage) {
                pbForgot.setVisibility(View.GONE);
                btnSubmitReset.setEnabled(true);
                showMessage(errorMessage, true);
            }
        });
    }

    private void showMessage(String msg, boolean isError) {
        tvStatus.setText(msg);
        tvStatus.setTextColor(isError ? 0xFFFF5B64 : 0xFF55D98B);
        tvStatus.setVisibility(View.VISIBLE);
    }
}