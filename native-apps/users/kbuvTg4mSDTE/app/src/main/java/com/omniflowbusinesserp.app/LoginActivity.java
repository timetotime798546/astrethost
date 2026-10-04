package com.omniflowbusinesserp.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONObject;

public class LoginActivity extends Activity {

    private enum Mode { LOGIN, REGISTER, FORGOT_OTP, RESET_PASS }
    private Mode currentMode = Mode.LOGIN;

    private EditText etEmail, etPassword, etOtp;
    private TextView txtFormHeader, btnToggleMode, btnForgotPassword, txtStatus, lblPassword;
    private Button btnSubmit;
    private LinearLayout layoutOtp;

    private BackendApi api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        api = new BackendApi(this);

        // Check persistent session
        SharedPreferences prefs = getSharedPreferences("OmniPrefs", MODE_PRIVATE);
        String token = prefs.getString("token", null);
        if (token != null) {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
            return;
        }

        etEmail = (EditText) findViewById(R.id.etEmail);
        etPassword = (EditText) findViewById(R.id.etPassword);
        etOtp = (EditText) findViewById(R.id.etOtp);
        txtFormHeader = (TextView) findViewById(R.id.txtFormHeader);
        btnToggleMode = (TextView) findViewById(R.id.btnToggleMode);
        btnForgotPassword = (TextView) findViewById(R.id.btnForgotPassword);
        txtStatus = (TextView) findViewById(R.id.txtStatus);
        lblPassword = (TextView) findViewById(R.id.lblPassword);
        btnSubmit = (Button) findViewById(R.id.btnSubmit);
        layoutOtp = (LinearLayout) findViewById(R.id.layoutOtp);

        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleSubmit();
            }
        });

        btnToggleMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentMode == Mode.LOGIN) {
                    setMode(Mode.REGISTER);
                } else {
                    setMode(Mode.LOGIN);
                }
            }
        });

        btnForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentMode == Mode.LOGIN || currentMode == Mode.REGISTER) {
                    setMode(Mode.FORGOT_OTP);
                } else {
                    setMode(Mode.LOGIN);
                }
            }
        });

        setMode(Mode.LOGIN);
    }

    private void setMode(Mode mode) {
        currentMode = mode;
        txtStatus.setText("");
        switch (mode) {
            case LOGIN:
                txtFormHeader.setText("Sign In");
                btnSubmit.setText("Log In");
                lblPassword.setText("Password");
                etPassword.setVisibility(View.VISIBLE);
                lblPassword.setVisibility(View.VISIBLE);
                layoutOtp.setVisibility(View.GONE);
                btnToggleMode.setVisibility(View.VISIBLE);
                btnToggleMode.setText("Need an ERP Account? Sign Up");
                btnForgotPassword.setText("Forgot Password?");
                break;
            case REGISTER:
                txtFormHeader.setText("Create Enterprise Account");
                btnSubmit.setText("Register Manager");
                lblPassword.setText("Create Password");
                etPassword.setVisibility(View.VISIBLE);
                lblPassword.setVisibility(View.VISIBLE);
                layoutOtp.setVisibility(View.GONE);
                btnToggleMode.setVisibility(View.VISIBLE);
                btnToggleMode.setText("Back to Login");
                btnForgotPassword.setText("Forgot Password?");
                break;
            case FORGOT_OTP:
                txtFormHeader.setText("Request Reset OTP");
                btnSubmit.setText("Send OTP to Email");
                etPassword.setVisibility(View.GONE);
                lblPassword.setVisibility(View.GONE);
                layoutOtp.setVisibility(View.GONE);
                btnToggleMode.setVisibility(View.GONE);
                btnForgotPassword.setText("Back to Login");
                break;
            case RESET_PASS:
                txtFormHeader.setText("Reset ERP Password");
                btnSubmit.setText("Reset Password");
                lblPassword.setText("New Password");
                etPassword.setVisibility(View.VISIBLE);
                lblPassword.setVisibility(View.VISIBLE);
                layoutOtp.setVisibility(View.VISIBLE);
                btnToggleMode.setVisibility(View.GONE);
                btnForgotPassword.setText("Back to Login");
                break;
        }
    }

    private void handleSubmit() {
        final String email = etEmail.getText().toString().trim();
        final String password = etPassword.getText().toString().trim();

        if (email.isEmpty()) {
            Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show();
            return;
        }

        txtStatus.setText("Executing secure request...");

        if (currentMode == Mode.LOGIN) {
            if (password.isEmpty()) {
                Toast.makeText(this, "Please enter password", Toast.LENGTH_SHORT).show();
                return;
            }
            api.login(email, password, new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(String response) {
                    try {
                        JSONObject json = new JSONObject(response);
                        if (json.getBoolean("success")) {
                            String token = json.getString("token");
                            saveSession(token, email);
                            Toast.makeText(LoginActivity.this, "Access Granted", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finish();
                        } else {
                            txtStatus.setText("Authentication failed.");
                        }
                    } catch (Exception e) {
                        txtStatus.setText("Parser issue: " + e.getMessage());
                    }
                }

                @Override
                public void onError(String errorMessage) {
                    txtStatus.setText("Error: " + errorMessage);
                }
            });

        } else if (currentMode == Mode.REGISTER) {
            if (password.isEmpty()) {
                Toast.makeText(this, "Please write a password", Toast.LENGTH_SHORT).show();
                return;
            }
            api.register(email, password, new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(String response) {
                    txtStatus.setText("User Registered. Automatically logging in...");
                    // Perform automatic login following exact API flow rules
                    api.login(email, password, new BackendApi.ApiCallback() {
                        @Override
                        public void onSuccess(String loginResponse) {
                            try {
                                JSONObject json = new JSONObject(loginResponse);
                                if (json.getBoolean("success")) {
                                    String token = json.getString("token");
                                    saveSession(token, email);
                                    Toast.makeText(LoginActivity.this, "Welcome to OmniFlow", Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                    finish();
                                } else {
                                    txtStatus.setText("Account created, please log in manually.");
                                }
                            } catch (Exception e) {
                                txtStatus.setText("Autologin issue: " + e.getMessage());
                            }
                        }

                        @Override
                        public void onError(String errorMessage) {
                            txtStatus.setText("Auth failed: " + errorMessage);
                        }
                    });
                }

                @Override
                public void onError(String errorMessage) {
                    txtStatus.setText("Error: " + errorMessage);
                }
            });

        } else if (currentMode == Mode.FORGOT_OTP) {
            api.requestOtp(email, new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(String response) {
                    txtStatus.setText("OTP sent to Gmail check your inbox.");
                    setMode(Mode.RESET_PASS);
                }

                @Override
                public void onError(String errorMessage) {
                    txtStatus.setText("Error: " + errorMessage);
                }
            });

        } else if (currentMode == Mode.RESET_PASS) {
            String otp = etOtp.getText().toString().trim();
            if (otp.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Write new password and OTP code", Toast.LENGTH_SHORT).show();
                return;
            }
            api.resetPassword(email, otp, password, new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(String response) {
                    Toast.makeText(LoginActivity.this, "Password Changed. Sign in now.", Toast.LENGTH_LONG).show();
                    setMode(Mode.LOGIN);
                }

                @Override
                public void onError(String errorMessage) {
                    txtStatus.setText("Error: " + errorMessage);
                }
            });
        }
    }

    private void saveSession(String token, String email) {
        SharedPreferences prefs = getSharedPreferences("OmniPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("token", token);
        editor.putString("email", email);
        editor.apply();
    }
}