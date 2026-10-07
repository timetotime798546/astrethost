package com.barberq.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
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

    private EditText etName, etEmail, etPassword;
    private EditText etForgotEmail, etForgotOtp, etForgotNewPassword;
    private Button btnSubmit, btnRequestOtp, btnResetPassword;
    private TextView btnToggleAuth, btnForgotPassword, btnBackToLogin, formTitle;
    private LinearLayout authFormContainer, forgotFormContainer;
    private ProgressBar loginProgress;

    private boolean isSignupMode = false;
    private BackendApi api;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        api = new BackendApi(this);
        prefs = getSharedPreferences("BarberQPrefs", MODE_PRIVATE);

        // Check active session
        String token = prefs.getString("token", null);
        if (token != null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        // Bind layouts
        authFormContainer = (LinearLayout) findViewById(R.id.authFormContainer);
        forgotFormContainer = (LinearLayout) findViewById(R.id.forgotFormContainer);
        loginProgress = (ProgressBar) findViewById(R.id.loginProgress);

        etName = (EditText) findViewById(R.id.etName);
        etEmail = (EditText) findViewById(R.id.etEmail);
        etPassword = (EditText) findViewById(R.id.etPassword);

        etForgotEmail = (EditText) findViewById(R.id.etForgotEmail);
        etForgotOtp = (EditText) findViewById(R.id.etForgotOtp);
        etForgotNewPassword = (EditText) findViewById(R.id.etForgotNewPassword);

        btnSubmit = (Button) findViewById(R.id.btnSubmit);
        btnRequestOtp = (Button) findViewById(R.id.btnRequestOtp);
        btnResetPassword = (Button) findViewById(R.id.btnResetPassword);

        btnToggleAuth = (TextView) findViewById(R.id.btnToggleAuth);
        btnForgotPassword = (TextView) findViewById(R.id.btnForgotPassword);
        btnBackToLogin = (TextView) findViewById(R.id.btnBackToLogin);
        formTitle = (TextView) findViewById(R.id.formTitle);

        // Switch to Signup/Login
        btnToggleAuth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isSignupMode = !isSignupMode;
                if (isSignupMode) {
                    formTitle.setText("Create Account");
                    etName.setVisibility(View.VISIBLE);
                    btnSubmit.setText("SIGN UP");
                    btnToggleAuth.setText("Already have an account? Sign In");
                } else {
                    formTitle.setText("Sign In");
                    etName.setVisibility(View.GONE);
                    btnSubmit.setText("LOG IN");
                    btnToggleAuth.setText("Don't have an account? Sign Up");
                }
            }
        });

        // Submit Form
        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleAuth();
            }
        });

        // Toggle Forgot Password view
        btnForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                authFormContainer.setVisibility(View.GONE);
                forgotFormContainer.setVisibility(View.VISIBLE);
            }
        });

        btnBackToLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                forgotFormContainer.setVisibility(View.GONE);
                authFormContainer.setVisibility(View.VISIBLE);
            }
        });

        // Forgot password: OTP steps
        btnRequestOtp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestResetOtp();
            }
        });

        btnResetPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitPasswordReset();
            }
        });
    }

    private void handleAuth() {
        final String email = etEmail.getText().toString().trim();
        final String password = etPassword.getText().toString().trim();
        final String name = etName.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please satisfy all inputs.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isSignupMode && name.isEmpty()) {
            Toast.makeText(this, "Please provide your Full Name.", Toast.LENGTH_SHORT).show();
            return;
        }

        loginProgress.setVisibility(View.VISIBLE);
        btnSubmit.setEnabled(false);

        if (isSignupMode) {
            api.register(email, password, new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    // Registration succeeded. Proceed to log in automatically.
                    prefs.edit().putString("userName", name).apply();
                    performLogin(email, password);
                }

                @Override
                public void onFailure(String error) {
                    loginProgress.setVisibility(View.GONE);
                    btnSubmit.setEnabled(true);
                    Toast.makeText(LoginActivity.this, "Signup Failed: " + error, Toast.LENGTH_LONG).show();
                }
            });
        } else {
            performLogin(email, password);
        }
    }

    private void performLogin(final String email, final String password) {
        api.login(email, password, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                loginProgress.setVisibility(View.GONE);
                btnSubmit.setEnabled(true);
                try {
                    String token = response.getString("token");
                    prefs.edit()
                            .putString("token", token)
                            .putString("userEmail", email)
                            .apply();

                    Toast.makeText(LoginActivity.this, "Authentication successful!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();
                } catch (Exception e) {
                    Toast.makeText(LoginActivity.this, "Malformed login token", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(String error) {
                loginProgress.setVisibility(View.GONE);
                btnSubmit.setEnabled(true);
                Toast.makeText(LoginActivity.this, "Login Failed: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void requestResetOtp() {
        String email = etForgotEmail.getText().toString().trim();
        if (email.isEmpty()) {
            Toast.makeText(this, "Provide registered email.", Toast.LENGTH_SHORT).show();
            return;
        }

        loginProgress.setVisibility(View.VISIBLE);
        btnRequestOtp.setEnabled(false);

        api.requestOtp(email, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                loginProgress.setVisibility(View.GONE);
                btnRequestOtp.setEnabled(true);
                Toast.makeText(LoginActivity.this, "OTP generated and sent to email successfully!", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFailure(String error) {
                loginProgress.setVisibility(View.GONE);
                btnRequestOtp.setEnabled(true);
                Toast.makeText(LoginActivity.this, "Failed: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void submitPasswordReset() {
        String email = etForgotEmail.getText().toString().trim();
        String otp = etForgotOtp.getText().toString().trim();
        String newPass = etForgotNewPassword.getText().toString().trim();

        if (email.isEmpty() || otp.isEmpty() || newPass.isEmpty()) {
            Toast.makeText(this, "Please input email, verification code, and password.", Toast.LENGTH_SHORT).show();
            return;
        }

        loginProgress.setVisibility(View.VISIBLE);
        btnResetPassword.setEnabled(false);

        api.resetPassword(email, otp, newPass, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                loginProgress.setVisibility(View.GONE);
                btnResetPassword.setEnabled(true);
                Toast.makeText(LoginActivity.this, "Password updated. You can now login.", Toast.LENGTH_LONG).show();
                forgotFormContainer.setVisibility(View.GONE);
                authFormContainer.setVisibility(View.VISIBLE);
            }

            @Override
            public void onFailure(String error) {
                loginProgress.setVisibility(View.GONE);
                btnResetPassword.setEnabled(true);
                Toast.makeText(LoginActivity.this, "Failed: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }
}