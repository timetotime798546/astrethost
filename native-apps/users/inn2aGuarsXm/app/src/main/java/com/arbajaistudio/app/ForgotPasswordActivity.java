package com.arbajaistudio.app;

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

    private BackendApi api;
    private EditText etEmail;
    private EditText etOtp;
    private EditText etNewPassword;
    private Button btnRequestOtp;
    private Button btnSubmitReset;
    private Button btnBack;
    private ProgressBar pbLoading;
    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        api = new BackendApi(this);

        etEmail = (EditText) findViewById(R.id.et_reset_email);
        etOtp = (EditText) findViewById(R.id.et_reset_otp);
        etNewPassword = (EditText) findViewById(R.id.et_new_password);
        btnRequestOtp = (Button) findViewById(R.id.btn_request_otp);
        btnSubmitReset = (Button) findViewById(R.id.btn_submit_reset);
        btnBack = (Button) findViewById(R.id.btn_back_auth);
        pbLoading = (ProgressBar) findViewById(R.id.pb_reset_loading);
        tvStatus = (TextView) findViewById(R.id.tv_reset_status);

        String passedEmail = getIntent().getStringExtra("email");
        if (passedEmail != null && !passedEmail.isEmpty()) {
            etEmail.setText(passedEmail);
        }

        btnRequestOtp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = etEmail.getText().toString().trim();
                if (email.isEmpty()) {
                    showError("Please enter your account email");
                    return;
                }
                setLoading(true);
                api.requestOtp(email, new BackendApi.ApiCallback() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                setLoading(false);
                                Toast.makeText(ForgotPasswordActivity.this, "OTP sent to your email", Toast.LENGTH_LONG).show();
                                tvStatus.setText("OTP code dispatched to your mailbox.");
                                tvStatus.setTextColor(0xFF10B981);
                                tvStatus.setVisibility(View.VISIBLE);
                            }
                        });
                    }

                    @Override
                    public void onError(final String errorMessage) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                setLoading(false);
                                showError(errorMessage);
                            }
                        });
                    }
                });
            }
        });

        btnSubmitReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = etEmail.getText().toString().trim();
                String otp = etOtp.getText().toString().trim();
                String newPass = etNewPassword.getText().toString().trim();

                if (email.isEmpty() || otp.isEmpty() || newPass.isEmpty()) {
                    showError("Please provide Email, OTP code, and new password");
                    return;
                }

                setLoading(true);
                api.resetPassword(email, otp, newPass, new BackendApi.ApiCallback() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                setLoading(false);
                                Toast.makeText(ForgotPasswordActivity.this, "Password updated successfully!", Toast.LENGTH_LONG).show();
                                finish();
                            }
                        });
                    }

                    @Override
                    public void onError(final String errorMessage) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                setLoading(false);
                                showError(errorMessage);
                            }
                        });
                    }
                });
            }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void setLoading(boolean loading) {
        pbLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnRequestOtp.setEnabled(!loading);
        btnSubmitReset.setEnabled(!loading);
        if (loading) {
            tvStatus.setVisibility(View.GONE);
        }
    }

    private void showError(String msg) {
        tvStatus.setText(msg);
        tvStatus.setTextColor(0xFFEF4444);
        tvStatus.setVisibility(View.VISIBLE);
    }
}