package com.cloudnotespro.app;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.json.JSONObject;

public class ForgotPasswordActivity extends Activity {

    private static final String TAG = "ForgotPasswordActivity";
    private EditText etEmail, etOtp, etNewPassword;
    private Button btnRequestOtp, btnResetPassword, btnBackToLogin;
    private LinearLayout layoutStep1, layoutStep2;
    private BackendApi backendApi;
    private String verifiedEmail = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_forgot_password);
        } catch (Exception e) {
            Toast.makeText(this, "Layout error", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        backendApi = new BackendApi(this);

        etEmail = findViewById(R.id.etEmail);
        etOtp = findViewById(R.id.etOtp);
        etNewPassword = findViewById(R.id.etNewPassword);
        btnRequestOtp = findViewById(R.id.btnRequestOtp);
        btnResetPassword = findViewById(R.id.btnResetPassword);
        btnBackToLogin = findViewById(R.id.btnBackToLogin);
        layoutStep1 = findViewById(R.id.layoutStep1);
        layoutStep2 = findViewById(R.id.layoutStep2);

        if (btnRequestOtp != null) {
            btnRequestOtp.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final String email = (etEmail != null && etEmail.getText() != null) ? etEmail.getText().toString().trim() : "";
                    if (email.isEmpty()) {
                        Toast.makeText(ForgotPasswordActivity.this, "Please enter your email address", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    btnRequestOtp.setEnabled(false);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            JSONObject result = null;
                            try {
                                result = backendApi.requestOtp(email);
                            } catch (Exception e) {
                                Log.e(TAG, "Request OTP exception", e);
                            }

                            final JSONObject finalResult = result;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (btnRequestOtp != null) {
                                        btnRequestOtp.setEnabled(true);
                                    }
                                    if (finalResult != null && finalResult.optBoolean("success", false)) {
                                        verifiedEmail = email;
                                        Toast.makeText(ForgotPasswordActivity.this, "OTP sent to your email!", Toast.LENGTH_LONG).show();
                                        if (layoutStep1 != null) layoutStep1.setVisibility(View.GONE);
                                        if (layoutStep2 != null) layoutStep2.setVisibility(View.VISIBLE);
                                    } else {
                                        String err = "Failed to send OTP";
                                        if (finalResult != null) {
                                            if (finalResult.has("error") && !finalResult.isNull("error")) {
                                                Object errObj = finalResult.opt("error");
                                                if (errObj != null) err = errObj.toString();
                                            } else if (finalResult.has("message") && !finalResult.isNull("message")) {
                                                Object msgObj = finalResult.opt("message");
                                                if (msgObj != null) err = msgObj.toString();
                                            }
                                        }
                                        Toast.makeText(ForgotPasswordActivity.this, err, Toast.LENGTH_LONG).show();
                                    }
                                }
                            });
                        }
                    }).start();
                }
            });
        }

        if (btnResetPassword != null) {
            btnResetPassword.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final String otp = (etOtp != null && etOtp.getText() != null) ? etOtp.getText().toString().trim() : "";
                    final String newPassword = (etNewPassword != null && etNewPassword.getText() != null) ? etNewPassword.getText().toString().trim() : "";

                    if (otp.isEmpty() || newPassword.isEmpty()) {
                        Toast.makeText(ForgotPasswordActivity.this, "Please enter OTP and new password", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    btnResetPassword.setEnabled(false);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            JSONObject result = null;
                            try {
                                result = backendApi.resetPassword(verifiedEmail, otp, newPassword);
                            } catch (Exception e) {
                                Log.e(TAG, "Reset password exception", e);
                            }

                            final JSONObject finalResult = result;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (btnResetPassword != null) {
                                        btnResetPassword.setEnabled(true);
                                    }
                                    if (finalResult != null && finalResult.optBoolean("success", false)) {
                                        Toast.makeText(ForgotPasswordActivity.this, "Password reset successfully! Please sign in.", Toast.LENGTH_LONG).show();
                                        finish();
                                    } else {
                                        String err = "Failed to reset password";
                                        if (finalResult != null) {
                                            if (finalResult.has("error") && !finalResult.isNull("error")) {
                                                Object errObj = finalResult.opt("error");
                                                if (errObj != null) err = errObj.toString();
                                            } else if (finalResult.has("message") && !finalResult.isNull("message")) {
                                                Object msgObj = finalResult.opt("message");
                                                if (msgObj != null) err = msgObj.toString();
                                            }
                                        }
                                        Toast.makeText(ForgotPasswordActivity.this, err, Toast.LENGTH_LONG).show();
                                    }
                                }
                            });
                        }
                    }).start();
                }
            });
        }

        if (btnBackToLogin != null) {
            btnBackToLogin.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        }
    }
}