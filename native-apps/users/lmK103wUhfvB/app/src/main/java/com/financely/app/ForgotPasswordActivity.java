package com.financely.app;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONObject;

public class ForgotPasswordActivity extends Activity {

    private LinearLayout layoutRequestOtp, layoutResetPassword;
    private EditText etEmail, etOtp, etNewPassword;
    private Button btnSendOtp, btnResetPassword;
    private TextView tvBackToLogin;
    private BackendApi api;
    private ProgressDialog progressDialog;
    private String savedEmail = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        api = new BackendApi(this);

        layoutRequestOtp = (LinearLayout) findViewById(R.id.layout_request_otp);
        layoutResetPassword = (LinearLayout) findViewById(R.id.layout_reset_password);
        etEmail = (EditText) findViewById(R.id.et_forgot_email);
        etOtp = (EditText) findViewById(R.id.et_verification_otp);
        etNewPassword = (EditText) findViewById(R.id.et_new_password);
        btnSendOtp = (Button) findViewById(R.id.btn_send_otp);
        btnResetPassword = (Button) findViewById(R.id.btn_reset_password);
        tvBackToLogin = (TextView) findViewById(R.id.tv_back_to_login);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Processing requested operations...");
        progressDialog.setCancelable(false);

        btnSendOtp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestVerificationCode();
            }
        });

        btnResetPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetUserCredentials();
            }
        });

        tvBackToLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void requestVerificationCode() {
        final String email = etEmail.getText().toString().trim();
        if (email.isEmpty()) {
            Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show();
            return;
        }

        savedEmail = email;
        progressDialog.setMessage("Requesting OTP...");
        progressDialog.show();

        api.requestOtp(email, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                progressDialog.dismiss();
                if (response.optBoolean("success", false)) {
                    Toast.makeText(ForgotPasswordActivity.this, "OTP Sent! Please check your inbox.", Toast.LENGTH_LONG).show();
                    layoutRequestOtp.setVisibility(View.GONE);
                    layoutResetPassword.setVisibility(View.VISIBLE);
                } else {
                    String msg = response.optString("message", "Error requesting verification");
                    Toast.makeText(ForgotPasswordActivity.this, msg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onError(String errorMessage) {
                progressDialog.dismiss();
                Toast.makeText(ForgotPasswordActivity.this, "Error: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void resetUserCredentials() {
        String otp = etOtp.getText().toString().trim();
        String newPass = etNewPassword.getText().toString().trim();

        if (otp.isEmpty() || newPass.isEmpty()) {
            Toast.makeText(this, "Please enter OTP and the new password", Toast.LENGTH_SHORT).show();
            return;
        }

        if (newPass.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.setMessage("Updating credentials...");
        progressDialog.show();

        api.resetPassword(savedEmail, otp, newPass, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                progressDialog.dismiss();
                if (response.optBoolean("success", false)) {
                    Toast.makeText(ForgotPasswordActivity.this, "Password successfully updated! Login to proceed.", Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    String msg = response.optString("message", "Reset failed");
                    Toast.makeText(ForgotPasswordActivity.this, msg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onError(String errorMessage) {
                progressDialog.dismiss();
                Toast.makeText(ForgotPasswordActivity.this, "Error: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }
}