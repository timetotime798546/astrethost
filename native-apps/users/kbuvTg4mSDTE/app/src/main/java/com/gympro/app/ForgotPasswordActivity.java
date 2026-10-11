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

    private EditText etResetEmail;
    private Button btnRequestOtp;
    private LinearLayout layoutResetOtp;
    private EditText etResetOtp;
    private EditText etResetNewPassword;
    private Button btnSubmitReset;
    private ProgressBar pbForgot;
    private TextView tvForgotStatus;
    private TextView tvForgotBack;

    private BackendApi backendApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        backendApi = new BackendApi(this);

        etResetEmail = (EditText) findViewById(R.id.etResetEmail);
        btnRequestOtp = (Button) findViewById(R.id.btnRequestOtp);
        layoutResetOtp = (LinearLayout) findViewById(R.id.layoutResetOtp);
        etResetOtp = (EditText) findViewById(R.id.etResetOtp);
        etResetNewPassword = (EditText) findViewById(R.id.etResetNewPassword);
        btnSubmitReset = (Button) findViewById(R.id.btnSubmitReset);
        pbForgot = (ProgressBar) findViewById(R.id.pbForgot);
        tvForgotStatus = (TextView) findViewById(R.id.tvForgotStatus);
        tvForgotBack = (TextView) findViewById(R.id.tvForgotBack);

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

        tvForgotBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void requestOtp() {
        String email = etResetEmail.getText().toString().trim();
        if (email.isEmpty()) {
            tvForgotStatus.setText("Please enter your email");
            tvForgotStatus.setVisibility(View.VISIBLE);
            return;
        }

        pbForgot.setVisibility(View.VISIBLE);
        btnRequestOtp.setEnabled(false);
        tvForgotStatus.setVisibility(View.GONE);

        backendApi.requestOtp(email, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                pbForgot.setVisibility(View.GONE);
                btnRequestOtp.setEnabled(true);
                layoutResetOtp.setVisibility(View.VISIBLE);
                tvForgotStatus.setText("OTP sent to your email!");
                tvForgotStatus.setVisibility(View.VISIBLE);
            }

            @Override
            public void onError(String errorMessage) {
                pbForgot.setVisibility(View.GONE);
                btnRequestOtp.setEnabled(true);
                tvForgotStatus.setText(errorMessage);
                tvForgotStatus.setVisibility(View.VISIBLE);
            }
        });
    }

    private void resetPassword() {
        String email = etResetEmail.getText().toString().trim();
        String otp = etResetOtp.getText().toString().trim();
        String newPassword = etResetNewPassword.getText().toString().trim();

        if (email.isEmpty() || otp.isEmpty() || newPassword.isEmpty()) {
            tvForgotStatus.setText("Please complete all OTP fields");
            tvForgotStatus.setVisibility(View.VISIBLE);
            return;
        }

        pbForgot.setVisibility(View.VISIBLE);
        btnSubmitReset.setEnabled(false);

        backendApi.resetPassword(email, otp, newPassword, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                pbForgot.setVisibility(View.GONE);
                btnSubmitReset.setEnabled(true);
                tvForgotStatus.setText("Password reset successful! You can now log in.");
                tvForgotStatus.setVisibility(View.VISIBLE);
            }

            @Override
            public void onError(String errorMessage) {
                pbForgot.setVisibility(View.GONE);
                btnSubmitReset.setEnabled(true);
                tvForgotStatus.setText(errorMessage);
                tvForgotStatus.setVisibility(View.VISIBLE);
            }
        });
    }
}