package com.notesphere.app;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class ForgotPasswordActivity extends Activity {

    private EditText editEmail;
    private Button btnSendOtp;
    private EditText editOtp;
    private EditText editNewPassword;
    private Button btnResetPassword;
    private TextView textBackToLogin;
    private BackendApi api;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        editEmail = (EditText) findViewById(R.id.edit_forgot_email);
        btnSendOtp = (Button) findViewById(R.id.button_send_otp);
        editOtp = (EditText) findViewById(R.id.edit_forgot_otp);
        editNewPassword = (EditText) findViewById(R.id.edit_forgot_new_password);
        btnResetPassword = (Button) findViewById(R.id.button_reset_password);
        textBackToLogin = (TextView) findViewById(R.id.text_back_to_login);

        api = new BackendApi(this);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Processing...");
        progressDialog.setCancelable(false);

        btnSendOtp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendOtp();
            }
        });

        btnResetPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetPassword();
            }
        });

        textBackToLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void sendOtp() {
        String email = editEmail.getText().toString().trim();
        if (email.isEmpty()) {
            Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.setMessage("Sending OTP...");
        progressDialog.show();

        api.requestOtp(email, new ApiCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                progressDialog.dismiss();
                Toast.makeText(ForgotPasswordActivity.this, "OTP sent if account exists. Check your email.", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(String error) {
                progressDialog.dismiss();
                Toast.makeText(ForgotPasswordActivity.this, "Error requesting OTP: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void resetPassword() {
        String email = editEmail.getText().toString().trim();
        String otp = editOtp.getText().toString().trim();
        String newPassword = editNewPassword.getText().toString().trim();

        if (email.isEmpty()) {
            Toast.makeText(this, "Please enter email", Toast.LENGTH_SHORT).show();
            return;
        }
        if (otp.isEmpty()) {
            Toast.makeText(this, "Please enter OTP", Toast.LENGTH_SHORT).show();
            return;
        }
        if (newPassword.isEmpty()) {
            Toast.makeText(this, "Please enter your new password", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.setMessage("Resetting password...");
        progressDialog.show();

        api.resetPassword(email, otp, newPassword, new ApiCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                progressDialog.dismiss();
                Toast.makeText(ForgotPasswordActivity.this, "Password reset successful! You can now log in.", Toast.LENGTH_LONG).show();
                finish();
            }

            @Override
            public void onError(String error) {
                progressDialog.dismiss();
                Toast.makeText(ForgotPasswordActivity.this, "Reset failed: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }
}