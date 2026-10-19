package com.authportal.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

public class AuthActivity extends Activity {

    private boolean isLoginMode = true;
    private BackendApi backendApi;

    private TextView tvFormTitle;
    private TextView tvAppSubHeader;
    private EditText etEmail;
    private EditText etPassword;
    private Button btnSubmitAuth;
    private TextView tvForgotPassword;
    private TextView tvToggleMode;
    private ProgressBar pbAuthProgress;
    private TextView tvStatusMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        backendApi = new BackendApi(this);

        // Check active session
        if (backendApi.isLoggedIn()) {
            launchMainActivity();
            finish();
            return;
        }

        setContentView(R.layout.activity_auth);

        tvFormTitle = (TextView) findViewById(R.id.tvFormTitle);
        tvAppSubHeader = (TextView) findViewById(R.id.tvAppSubHeader);
        etEmail = (EditText) findViewById(R.id.etEmail);
        etPassword = (EditText) findViewById(R.id.etPassword);
        btnSubmitAuth = (Button) findViewById(R.id.btnSubmitAuth);
        tvForgotPassword = (TextView) findViewById(R.id.tvForgotPassword);
        tvToggleMode = (TextView) findViewById(R.id.tvToggleMode);
        pbAuthProgress = (ProgressBar) findViewById(R.id.pbAuthProgress);
        tvStatusMessage = (TextView) findViewById(R.id.tvStatusMessage);

        btnSubmitAuth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleAuthSubmit();
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
                showResetPasswordDialog();
            }
        });
    }

    private void toggleAuthMode() {
        isLoginMode = !isLoginMode;
        tvStatusMessage.setVisibility(View.GONE);

        if (isLoginMode) {
            tvFormTitle.setText(R.string.title_login);
            tvAppSubHeader.setText(R.string.subtitle_login);
            btnSubmitAuth.setText(R.string.btn_login);
            tvForgotPassword.setVisibility(View.VISIBLE);
            tvToggleMode.setText(R.string.toggle_to_register);
        } else {
            tvFormTitle.setText(R.string.title_register);
            tvAppSubHeader.setText(R.string.subtitle_register);
            btnSubmitAuth.setText(R.string.btn_register);
            tvForgotPassword.setVisibility(View.GONE);
            tvToggleMode.setText(R.string.toggle_to_login);
        }
    }

    private void handleAuthSubmit() {
        final String email = etEmail.getText().toString().trim();
        final String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || !email.contains("@")) {
            showError("Please enter a valid email address");
            return;
        }

        if (password.length() < 6) {
            showError("Password must be at least 6 characters long");
            return;
        }

        setLoading(true);

        if (isLoginMode) {
            executeLogin(email, password);
        } else {
            // Register workflow followed by automatic login
            backendApi.register(email, password, new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    // Critical rule: /register response does NOT return a token.
                    // Immediately execute /login to obtain auth Bearer token.
                    executeLogin(email, password);
                }

                @Override
                public void onError(String errorMessage) {
                    setLoading(false);
                    showError("Registration failed: " + errorMessage);
                }
            });
        }
    }

    private void executeLogin(final String email, final String password) {
        backendApi.login(email, password, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                setLoading(false);
                String token = response.optString("token", "");
                if (token.isEmpty()) {
                    showError("Login error: Authentication token missing in server response.");
                    return;
                }

                // Store token securely
                backendApi.saveAuthSession(token, email);
                Toast.makeText(AuthActivity.this, "Authentication successful!", Toast.LENGTH_SHORT).show();
                launchMainActivity();
                finish();
            }

            @Override
            public void onError(String errorMessage) {
                setLoading(false);
                showError("Sign in failed: " + errorMessage);
            }
        });
    }

    private void showResetPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_reset_password, null);
        builder.setView(dialogView);

        final AlertDialog dialog = builder.create();

        final EditText etResetEmail = (EditText) dialogView.findViewById(R.id.etResetEmail);
        final Button btnRequestOtp = (Button) dialogView.findViewById(R.id.btnRequestOtp);
        final LinearLayout layoutOtpSection = (LinearLayout) dialogView.findViewById(R.id.layoutOtpSection);
        final EditText etOtpCode = (EditText) dialogView.findViewById(R.id.etOtpCode);
        final EditText etNewPassword = (EditText) dialogView.findViewById(R.id.etNewPassword);
        final Button btnSubmitReset = (Button) dialogView.findViewById(R.id.btnSubmitReset);
        final ProgressBar pbResetProgress = (ProgressBar) dialogView.findViewById(R.id.pbResetProgress);
        final TextView tvResetFeedback = (TextView) dialogView.findViewById(R.id.tvResetFeedback);

        String currentEmail = etEmail.getText().toString().trim();
        if (!currentEmail.isEmpty()) {
            etResetEmail.setText(currentEmail);
        }

        btnRequestOtp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String email = etResetEmail.getText().toString().trim();
                if (email.isEmpty() || !email.contains("@")) {
                    tvResetFeedback.setText("Enter a valid email address");
                    tvResetFeedback.setVisibility(View.VISIBLE);
                    return;
                }

                pbResetProgress.setVisibility(View.VISIBLE);
                tvResetFeedback.setVisibility(View.GONE);

                backendApi.requestOtp(email, new BackendApi.ApiCallback() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        pbResetProgress.setVisibility(View.GONE);
                        layoutOtpSection.setVisibility(View.VISIBLE);
                        btnRequestOtp.setVisibility(View.GONE);
                        etResetEmail.setEnabled(false);
                        tvResetFeedback.setTextColor(getResources().getColor(R.color.success));
                        tvResetFeedback.setText("OTP sent! Please check your Gmail inbox.");
                        tvResetFeedback.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onError(String errorMessage) {
                        pbResetProgress.setVisibility(View.GONE);
                        tvResetFeedback.setTextColor(getResources().getColor(R.color.danger));
                        tvResetFeedback.setText("Failed to send OTP: " + errorMessage);
                        tvResetFeedback.setVisibility(View.VISIBLE);
                    }
                });
            }
        });

        btnSubmitReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String email = etResetEmail.getText().toString().trim();
                final String otp = etOtpCode.getText().toString().trim();
                final String newPass = etNewPassword.getText().toString().trim();

                if (otp.isEmpty() || otp.length() < 4) {
                    tvResetFeedback.setTextColor(getResources().getColor(R.color.danger));
                    tvResetFeedback.setText("Enter valid OTP code");
                    tvResetFeedback.setVisibility(View.VISIBLE);
                    return;
                }

                if (newPass.length() < 6) {
                    tvResetFeedback.setTextColor(getResources().getColor(R.color.danger));
                    tvResetFeedback.setText("New password must be at least 6 characters");
                    tvResetFeedback.setVisibility(View.VISIBLE);
                    return;
                }

                pbResetProgress.setVisibility(View.VISIBLE);
                tvResetFeedback.setVisibility(View.GONE);

                backendApi.resetPassword(email, otp, newPass, new BackendApi.ApiCallback() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        pbResetProgress.setVisibility(View.GONE);
                        Toast.makeText(AuthActivity.this, "Password reset successfully! Please sign in.", Toast.LENGTH_LONG).show();
                        dialog.dismiss();
                    }

                    @Override
                    public void onError(String errorMessage) {
                        pbResetProgress.setVisibility(View.GONE);
                        tvResetFeedback.setTextColor(getResources().getColor(R.color.danger));
                        tvResetFeedback.setText("Reset failed: " + errorMessage);
                        tvResetFeedback.setVisibility(View.VISIBLE);
                    }
                });
            }
        });

        dialog.show();
    }

    private void launchMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    private void setLoading(boolean loading) {
        pbAuthProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSubmitAuth.setEnabled(!loading);
        etEmail.setEnabled(!loading);
        etPassword.setEnabled(!loading);
    }

    private void showError(String msg) {
        tvStatusMessage.setText(msg);
        tvStatusMessage.setVisibility(View.VISIBLE);
    }
}