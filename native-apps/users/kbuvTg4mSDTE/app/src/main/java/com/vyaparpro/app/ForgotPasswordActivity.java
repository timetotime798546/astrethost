package com.vyaparpro.app;

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
    private Button btnConfirmReset;
    private ProgressBar progressBar;
    private TextView tvForgotMsg;
    private BackendApi api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        api = new BackendApi(this);

        etEmail = (EditText) findViewById(R.id.etForgotEmail);
        etOtp = (EditText) findViewById(R.id.etOtpCode);
        etNewPassword = (EditText) findViewById(R.id.etNewPassword);
        btnSendOtp = (Button) findViewById(R.id.btnSendOtp);
        btnConfirmReset = (Button) findViewById(R.id.btnConfirmReset);
        progressBar = (ProgressBar) findViewById(R.id.progressForgot);
        tvForgotMsg = (TextView) findViewById(R.id.tvForgotMsg);

        findViewById(R.id.btnBackFromForgot).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnSendOtp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestOtp();
            }
        });

        btnConfirmReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmReset();
            }
        });
    }

    private void requestOtp() {
        String email = etEmail.getText().toString().trim();
        if (email.isEmpty()) {
            Toast.makeText(this, "Please enter your registered email.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        api.requestOtp(email, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                progressBar.setVisibility(View.GONE);
                tvForgotMsg.setText("OTP sent to " + email + ". Check inbox or spam.");
                tvForgotMsg.setVisibility(View.VISIBLE);
            }

            @Override
            public void onError(String errorMessage) {
                progressBar.setVisibility(View.GONE);
                tvForgotMsg.setText(errorMessage);
                tvForgotMsg.setVisibility(View.VISIBLE);
            }
        });
    }

    private void confirmReset() {
        String email = etEmail.getText().toString().trim();
        String otp = etOtp.getText().toString().trim();
        String newPass = etNewPassword.getText().toString().trim();

        if (email.isEmpty() || otp.isEmpty() || newPass.length() < 6) {
            Toast.makeText(this, "Fill email, OTP, and new password (min 6 chars)", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        api.resetPassword(email, otp, newPass, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ForgotPasswordActivity.this, "Password reset successful! Please login.", Toast.LENGTH_LONG).show();
                finish();
            }

            @Override
            public void onError(String errorMessage) {
                progressBar.setVisibility(View.GONE);
                tvForgotMsg.setText(errorMessage);
                tvForgotMsg.setVisibility(View.VISIBLE);
            }
        });
    }
}