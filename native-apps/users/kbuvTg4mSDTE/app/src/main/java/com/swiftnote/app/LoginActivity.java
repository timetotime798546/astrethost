package com.swiftnote.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

public class LoginActivity extends Activity {

    private EditText etEmail;
    private EditText etPassword;
    private Button btnSubmit;
    private TextView tvForgotPassword;
    private TextView tvToggleMode;
    private TextView tvHeaderSubtitle;
    private ProgressBar pbLoading;

    private boolean isSignUpMode = false;
    private BackendApi backendApi;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        backendApi = new BackendApi(this);
        sessionManager = new SessionManager(this);

        etEmail = (EditText) findViewById(R.id.etEmail);
        etPassword = (EditText) findViewById(R.id.etPassword);
        btnSubmit = (Button) findViewById(R.id.btnSubmit);
        tvForgotPassword = (TextView) findViewById(R.id.tvForgotPassword);
        tvToggleMode = (TextView) findViewById(R.id.tvToggleMode);
        tvHeaderSubtitle = (TextView) findViewById(R.id.tvHeaderSubtitle);
        pbLoading = (ProgressBar) findViewById(R.id.pbLoading);

        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleAuthAction();
            }
        });

        tvToggleMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleAuthMode();
            }
        });

        tvForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showForgotPasswordDialog();
            }
        });
    }

    private void toggleAuthMode() {
        isSignUpMode = !isSignUpMode;
        if (isSignUpMode) {
            btnSubmit.setText("Create Account");
            tvToggleMode.setText("Already have an account? Sign In");
            tvHeaderSubtitle.setText("Create a new SwiftNote account");
            tvForgotPassword.setVisibility(View.GONE);
        } else {
            btnSubmit.setText("Sign In");
            tvToggleMode.setText("Don't have an account? Sign Up");
            tvHeaderSubtitle.setText("Sign in to your private notes cloud");
            tvForgotPassword.setVisibility(View.VISIBLE);
        }
    }

    private void handleAuthAction() {
        final String email = etEmail.getText().toString().trim();
        final String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter both email and password.", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        if (isSignUpMode) {
            // STEP 1: /register does not return a token.
            backendApi.register(email, password, new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    // STEP 2: Registration confirmed. Automatically login to obtain authentication token.
                    backendApi.login(email, password, new BackendApi.ApiCallback() {
                        @Override
                        public void onSuccess(JSONObject loginResp) {
                            setLoading(false);
                            String token = loginResp.optString("token", "");
                            if (!token.isEmpty()) {
                                sessionManager.saveSession(token, email);
                                navigateToMain();
                            } else {
                                Toast.makeText(LoginActivity.this, "Registered! Please sign in.", Toast.LENGTH_SHORT).show();
                                toggleAuthMode();
                            }
                        }

                        @Override
                        public void onError(String errorMessage) {
                            setLoading(false);
                            Toast.makeText(LoginActivity.this, "Account created. Please log in.", Toast.LENGTH_SHORT).show();
                            toggleAuthMode();
                        }
                    });
                }

                @Override
                public void onError(String errorMessage) {
                    setLoading(false);
                    Toast.makeText(LoginActivity.this, "Registration failed: " + errorMessage, Toast.LENGTH_LONG).show();
                }
            });
        } else {
            // SIGN IN FLOW
            backendApi.login(email, password, new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    setLoading(false);
                    String token = response.optString("token", "");
                    if (!token.isEmpty()) {
                        sessionManager.saveSession(token, email);
                        navigateToMain();
                    } else {
                        Toast.makeText(LoginActivity.this, "Login failed: No token returned", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onError(String errorMessage) {
                    setLoading(false);
                    Toast.makeText(LoginActivity.this, "Login error: " + errorMessage, Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void showForgotPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Reset Password");

        final EditText inputEmail = new EditText(this);
        inputEmail.setHint("Enter your account email");
        inputEmail.setText(etEmail.getText().toString().trim());
        inputEmail.setPadding(32, 24, 32, 24);
        builder.setView(inputEmail);

        builder.setPositiveButton("Request OTP", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final String email = inputEmail.getText().toString().trim();
                if (email.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "Email is required.", Toast.LENGTH_SHORT).show();
                    return;
                }
                setLoading(true);
                backendApi.requestOtp(email, new BackendApi.ApiCallback() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        setLoading(false);
                        Toast.makeText(LoginActivity.this, "OTP sent to your email!", Toast.LENGTH_SHORT).show();
                        showOtpResetDialog(email);
                    }

                    @Override
                    public void onError(String errorMessage) {
                        setLoading(false);
                        Toast.makeText(LoginActivity.this, "OTP request failed: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showOtpResetDialog(final String email) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter OTP & New Password");

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(32, 16, 32, 16);

        final EditText etOtp = new EditText(this);
        etOtp.setHint("6-digit OTP code");
        layout.addView(etOtp);

        final EditText etNewPass = new EditText(this);
        etNewPass.setHint("New Password");
        etNewPass.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(etNewPass);

        builder.setView(layout);

        builder.setPositiveButton("Update Password", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String otp = etOtp.getText().toString().trim();
                String newPass = etNewPass.getText().toString().trim();
                if (otp.isEmpty() || newPass.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "Both fields are required", Toast.LENGTH_SHORT).show();
                    return;
                }
                setLoading(true);
                backendApi.resetPassword(email, otp, newPass, new BackendApi.ApiCallback() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        setLoading(false);
                        Toast.makeText(LoginActivity.this, "Password updated successfully. Please sign in.", Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onError(String errorMessage) {
                        setLoading(false);
                        Toast.makeText(LoginActivity.this, "Password reset failed: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void setLoading(boolean loading) {
        pbLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSubmit.setEnabled(!loading);
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}