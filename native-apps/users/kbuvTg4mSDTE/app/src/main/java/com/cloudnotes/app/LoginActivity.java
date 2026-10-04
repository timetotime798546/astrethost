package com.cloudnotes.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONObject;

public class LoginActivity extends Activity {

    private BackendApi api;

    private LinearLayout loginContainer;
    private LinearLayout signupContainer;
    private LinearLayout forgotPasswordContainer;

    private EditText loginEmail;
    private EditText loginPassword;
    private Button btnLogin;

    private EditText signupEmail;
    private EditText signupPassword;
    private EditText signupConfirmPassword;
    private Button btnSignup;

    private LinearLayout otpStep1Layout;
    private LinearLayout otpStep2Layout;
    private EditText forgotEmail;
    private Button btnRequestOtp;
    private EditText resetOtpCode;
    private EditText resetNewPassword;
    private Button btnResetPassword;

    private ProgressBar loginProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        api = new BackendApi(this);

        if (api.hasSession()) {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
            return;
        }

        loginContainer = (LinearLayout) findViewById(R.id.loginContainer);
        signupContainer = (LinearLayout) findViewById(R.id.signupContainer);
        forgotPasswordContainer = (LinearLayout) findViewById(R.id.forgotPasswordContainer);

        loginEmail = (EditText) findViewById(R.id.loginEmail);
        loginPassword = (EditText) findViewById(R.id.loginPassword);
        btnLogin = (Button) findViewById(R.id.btnLogin);

        signupEmail = (EditText) findViewById(R.id.signupEmail);
        signupPassword = (EditText) findViewById(R.id.signupPassword);
        signupConfirmPassword = (EditText) findViewById(R.id.signupConfirmPassword);
        btnSignup = (Button) findViewById(R.id.btnSignup);

        otpStep1Layout = (LinearLayout) findViewById(R.id.otpStep1Layout);
        otpStep2Layout = (LinearLayout) findViewById(R.id.otpStep2Layout);
        forgotEmail = (EditText) findViewById(R.id.forgotEmail);
        btnRequestOtp = (Button) findViewById(R.id.btnRequestOtp);
        resetOtpCode = (EditText) findViewById(R.id.resetOtpCode);
        resetNewPassword = (EditText) findViewById(R.id.resetNewPassword);
        btnResetPassword = (Button) findViewById(R.id.btnResetPassword);

        loginProgress = (ProgressBar) findViewById(R.id.loginProgress);

        findViewById(R.id.btnGoToSignup).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showContainer(signupContainer);
            }
        });

        findViewById(R.id.btnGoToForgotPassword).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showContainer(forgotPasswordContainer);
                otpStep1Layout.setVisibility(View.VISIBLE);
                otpStep2Layout.setVisibility(View.GONE);
            }
        });

        findViewById(R.id.btnBackToLogin).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showContainer(loginContainer);
            }
        });

        findViewById(R.id.btnBackToLoginFromForgot).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showContainer(loginContainer);
            }
        });

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleLogin();
            }
        });

        btnSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleSignup();
            }
        });

        btnRequestOtp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleRequestOtp();
            }
        });

        btnResetPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleResetPassword();
            }
        });
    }

    private void showContainer(View containerToShow) {
        loginContainer.setVisibility(View.GONE);
        signupContainer.setVisibility(View.GONE);
        forgotPasswordContainer.setVisibility(View.GONE);
        containerToShow.setVisibility(View.VISIBLE);
    }

    private void showProgress(boolean show) {
        loginProgress.setVisibility(show ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!show);
        btnSignup.setEnabled(!show);
        btnRequestOtp.setEnabled(!show);
        btnResetPassword.setEnabled(!show);
    }

    private void handleLogin() {
        final String email = loginEmail.getText().toString().trim();
        final String password = loginPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        showProgress(true);
        api.login(email, password, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                showProgress(false);
                try {
                    JSONObject obj = new JSONObject(response);
                    if (obj.has("token")) {
                        String token = obj.getString("token");
                        api.saveToken(token);
                        Toast.makeText(LoginActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish();
                    } else {
                        Toast.makeText(LoginActivity.this, "Authentication response missing token", Toast.LENGTH_LONG).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(LoginActivity.this, "Parse error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onError(String errorMessage) {
                showProgress(false);
                Toast.makeText(LoginActivity.this, "Login error: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void handleSignup() {
        final String email = signupEmail.getText().toString().trim();
        final String password = signupPassword.getText().toString().trim();
        String confirmPassword = signupConfirmPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Please fill in all signup fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        showProgress(true);
        api.register(email, password, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                api.login(email, password, new BackendApi.ApiCallback() {
                    @Override
                    public void onSuccess(String loginRes) {
                        showProgress(false);
                        try {
                            JSONObject obj = new JSONObject(loginRes);
                            if (obj.has("token")) {
                                api.saveToken(obj.getString("token"));
                                Toast.makeText(LoginActivity.this, "Registered and Logged in!", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                finish();
                            } else {
                                Toast.makeText(LoginActivity.this, "Registered successfully! Please log in now.", Toast.LENGTH_LONG).show();
                                showContainer(loginContainer);
                            }
                        } catch (Exception e) {
                            Toast.makeText(LoginActivity.this, "Registered successfully! Please log in now.", Toast.LENGTH_LONG).show();
                            showContainer(loginContainer);
                        }
                    }

                    @Override
                    public void onError(String error) {
                        showProgress(false);
                        Toast.makeText(LoginActivity.this, "Registration succeeded, please log in.", Toast.LENGTH_LONG).show();
                        showContainer(loginContainer);
                    }
                });
            }

            @Override
            public void onError(String errorMessage) {
                showProgress(false);
                Toast.makeText(LoginActivity.this, "Signup failed: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void handleRequestOtp() {
        final String email = forgotEmail.getText().toString().trim();
        if (email.isEmpty()) {
            Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show();
            return;
        }

        showProgress(true);
        api.requestOtp(email, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                showProgress(false);
                Toast.makeText(LoginActivity.this, "OTP security code sent! Check email.", Toast.LENGTH_LONG).show();
                otpStep1Layout.setVisibility(View.GONE);
                otpStep2Layout.setVisibility(View.VISIBLE);
            }

            @Override
            public void onError(String errorMessage) {
                showProgress(false);
                Toast.makeText(LoginActivity.this, "Error requesting OTP: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void handleResetPassword() {
        final String email = forgotEmail.getText().toString().trim();
        final String otp = resetOtpCode.getText().toString().trim();
        final String newPass = resetNewPassword.getText().toString().trim();

        if (otp.isEmpty() || newPass.isEmpty()) {
            Toast.makeText(this, "Please enter OTP and new password", Toast.LENGTH_SHORT).show();
            return;
        }

        showProgress(true);
        api.resetPassword(email, otp, newPass, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                showProgress(false);
                Toast.makeText(LoginActivity.this, "Password reset successfully! Log in now.", Toast.LENGTH_LONG).show();
                showContainer(loginContainer);
            }

            @Override
            public void onError(String errorMessage) {
                showProgress(false);
                Toast.makeText(LoginActivity.this, "Reset failed: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }
}