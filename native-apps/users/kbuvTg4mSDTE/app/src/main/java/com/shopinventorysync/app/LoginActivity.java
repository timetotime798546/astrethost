package com.shopinventorysync.app;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class LoginActivity extends Activity {

    private EditText edtEmail, edtPassword, edtOtp, edtNewPassword;
    private Button btnSubmit, btnOtpRequest;
    private TextView txtFormTitle, txtToggleMode, txtForgotPassword;
    private LinearLayout layoutRecovery;

    private BackendApi api;
    private ProgressDialog progressDialog;

    // Modes: 0 = LOGIN, 1 = SIGNUP, 2 = RECOVERY
    private int currentMode = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        api = new BackendApi(this);

        // Session check
        if (api.isLoggedIn()) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        edtEmail = (EditText) findViewById(R.id.edtEmail);
        edtPassword = (EditText) findViewById(R.id.edtPassword);
        edtOtp = (EditText) findViewById(R.id.edtOtp);
        edtNewPassword = (EditText) findViewById(R.id.edtNewPassword);
        btnSubmit = (Button) findViewById(R.id.btnSubmit);
        btnOtpRequest = (Button) findViewById(R.id.btnOtpRequest);
        txtFormTitle = (TextView) findViewById(R.id.txtFormTitle);
        txtToggleMode = (TextView) findViewById(R.id.txtToggleMode);
        txtForgotPassword = (TextView) findViewById(R.id.txtForgotPassword);
        layoutRecovery = (LinearLayout) findViewById(R.id.layoutRecovery);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Connecting to server...");
        progressDialog.setCancelable(false);

        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleSubmit();
            }
        });

        btnOtpRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleOtpRequest();
            }
        });

        txtToggleMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentMode == 0) {
                    setMode(1); // switch to signup
                } else {
                    setMode(0); // switch to login
                }
            }
        });

        txtForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentMode != 2) {
                    setMode(2); // Switch to Recovery OTP mode
                } else {
                    setMode(0); // Cancel and switch to Login
                }
            }
        });

        // Default: Login Mode
        setMode(0);
    }

    private void setMode(int mode) {
        currentMode = mode;
        if (mode == 0) {
            txtFormTitle.setText("Merchant Login");
            edtPassword.setVisibility(View.VISIBLE);
            layoutRecovery.setVisibility(View.GONE);
            btnSubmit.setText("Log In");
            btnOtpRequest.setVisibility(View.GONE);
            txtToggleMode.setVisibility(View.VISIBLE);
            txtToggleMode.setText("Don't have an account? Sign Up");
            txtForgotPassword.setText("Forgot Password?");
        } else if (mode == 1) {
            txtFormTitle.setText("Merchant Registration");
            edtPassword.setVisibility(View.VISIBLE);
            layoutRecovery.setVisibility(View.GONE);
            btnSubmit.setText("Register Merchant");
            btnOtpRequest.setVisibility(View.GONE);
            txtToggleMode.setVisibility(View.VISIBLE);
            txtToggleMode.setText("Already registered? Login");
            txtForgotPassword.setText("Forgot Password?");
        } else if (mode == 2) {
            txtFormTitle.setText("Password Reset Console");
            edtPassword.setVisibility(View.GONE);
            layoutRecovery.setVisibility(View.VISIBLE);
            btnSubmit.setText("Perform Reset");
            btnOtpRequest.setVisibility(View.VISIBLE);
            txtToggleMode.setVisibility(View.GONE);
            txtForgotPassword.setText("Go back to Log In");
        }
    }

    private void handleSubmit() {
        final String email = edtEmail.getText().toString().trim();
        if (email.isEmpty()) {
            Toast.makeText(this, "Please enter email address", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentMode == 0) {
            // LOGIN MODE
            String password = edtPassword.getText().toString().trim();
            if (password.isEmpty()) {
                Toast.makeText(this, "Please enter your password", Toast.LENGTH_SHORT).show();
                return;
            }
            progressDialog.show();
            api.login(email, password, new BackendApi.ApiCallback<String>() {
                @Override
                public void onSuccess(String token) {
                    progressDialog.dismiss();
                    Toast.makeText(LoginActivity.this, "Welcome to Shop Stock Sync!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();
                }

                @Override
                public void onError(String error) {
                    progressDialog.dismiss();
                    Toast.makeText(LoginActivity.this, "Authentication Error: " + error, Toast.LENGTH_LONG).show();
                }
            });

        } else if (currentMode == 1) {
            // REGISTER MODE
            final String password = edtPassword.getText().toString().trim();
            if (password.isEmpty()) {
                Toast.makeText(this, "Please write a strong password", Toast.LENGTH_SHORT).show();
                return;
            }
            progressDialog.show();
            // Call register (doesn't output bearer token)
            api.register(email, password, new BackendApi.ApiCallback<String>() {
                @Override
                public void onSuccess(String result) {
                    // Registration succeeded. Proceed immediately to login auto-auth as required
                    api.login(email, password, new BackendApi.ApiCallback<String>() {
                        @Override
                        public void onSuccess(String token) {
                            progressDialog.dismiss();
                            Toast.makeText(LoginActivity.this, "Registered & Logged in successfully!", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finish();
                        }

                        @Override
                        public void onError(String error) {
                            progressDialog.dismiss();
                            // If auto login fails, switch back to login mode so user can type it
                            setMode(0);
                            Toast.makeText(LoginActivity.this, "Registered! Please input credentials to login.", Toast.LENGTH_LONG).show();
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    progressDialog.dismiss();
                    Toast.makeText(LoginActivity.this, "Sign-up Error: " + error, Toast.LENGTH_LONG).show();
                }
            });

        } else if (currentMode == 2) {
            // PASSWORD RESET FORM SUBMIT
            String otp = edtOtp.getText().toString().trim();
            String newPass = edtNewPassword.getText().toString().trim();
            if (otp.isEmpty() || newPass.isEmpty()) {
                Toast.makeText(this, "Provide OTP code and desired password", Toast.LENGTH_SHORT).show();
                return;
            }
            progressDialog.show();
            api.resetPassword(email, otp, newPass, new BackendApi.ApiCallback<String>() {
                @Override
                public void onSuccess(String msg) {
                    progressDialog.dismiss();
                    Toast.makeText(LoginActivity.this, msg, Toast.LENGTH_LONG).show();
                    setMode(0); // Switch back to log in
                }

                @Override
                public void onError(String error) {
                    progressDialog.dismiss();
                    Toast.makeText(LoginActivity.this, "Reset error: " + error, Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void handleOtpRequest() {
        String email = edtEmail.getText().toString().trim();
        if (email.isEmpty()) {
            Toast.makeText(this, "Please enter your email address to send OTP", Toast.LENGTH_SHORT).show();
            return;
        }
        progressDialog.show();
        api.requestOtp(email, new BackendApi.ApiCallback<String>() {
            @Override
            public void onSuccess(String msg) {
                progressDialog.dismiss();
                Toast.makeText(LoginActivity.this, msg, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(String error) {
                progressDialog.dismiss();
                Toast.makeText(LoginActivity.this, "Error sending OTP: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }
}