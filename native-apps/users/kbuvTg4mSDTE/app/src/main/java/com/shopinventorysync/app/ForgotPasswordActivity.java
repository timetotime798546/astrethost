package com.shopinventorysync.app;

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

    private EditText etEmail, etOtp, etNewPassword;
    private Button btnRequestOtp, btnResetPassword;
    private LinearLayout layoutStep1, layoutStep2;
    private TextView btnBack;
    private BackendApi backendApi;
    private ProgressDialog progressDialog;
    private String emailInput = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        backendApi = new BackendApi(this);

        etEmail = (EditText) findViewById(R.id.et_forgot_email);
        etOtp = (EditText) findViewById(R.id.et_otp);
        etNewPassword = (EditText) findViewById(R.id.et_new_password);

        btnRequestOtp = (Button) findViewById(R.id.btn_request_otp);
        btnResetPassword = (Button) findViewById(R.id.btn_reset_password);
        btnBack = (TextView) findViewById(R.id.btn_forgot_back);

        layoutStep1 = (LinearLayout) findViewById(R.id.layout_step1);
        layoutStep2 = (LinearLayout) findViewById(R.id.layout_step2);

        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);

        btnRequestOtp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestOtp();
            }
        });

        btnResetPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetPassword();
            }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void requestOtp() {
        emailInput = etEmail.getText().toString().trim();
        if (emailInput.isEmpty()) {
            Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.setMessage("Requesting OTP...");
        progressDialog.show();

        backendApi.requestOtp(emailInput, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.dismiss();
                        Toast.makeText(ForgotPasswordActivity.this, "OTP sent successfully", Toast.LENGTH_SHORT).show();
                        layoutStep1.setVisibility(View.GONE);
                        layoutStep2.setVisibility(View.VISIBLE);
                    }
                });
            }

            @Override
            public void onError(final String error) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.dismiss();
                        Toast.makeText(ForgotPasswordActivity.this, error, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private void resetPassword() {
        String otp = etOtp.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();

        if (otp.isEmpty() || newPassword.isEmpty()) {
            Toast.makeText(this, "Please fill in all details", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.setMessage("Resetting password...");
        progressDialog.show();

        backendApi.resetPassword(emailInput, otp, newPassword, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.dismiss();
                        Toast.makeText(ForgotPasswordActivity.this, "Password updated successfully! Please login.", Toast.LENGTH_LONG).show();
                        finish();
                    }
                });
            }

            @Override
            public void onError(final String error) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.dismiss();
                        Toast.makeText(ForgotPasswordActivity.this, error, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }
}