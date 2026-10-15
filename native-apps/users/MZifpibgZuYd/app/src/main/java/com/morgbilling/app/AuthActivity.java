package com.morgbilling.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

public class AuthActivity extends Activity {

    private static final int MODE_LOGIN = 1;
    private static final int MODE_REGISTER = 2;
    private static final int MODE_FORGOT_REQUEST = 3;
    private static final int MODE_FORGOT_RESET = 4;

    private int currentMode = MODE_LOGIN;

    private BackendApi backendApi;

    private TextView authTitle;
    private EditText etEmail;
    private EditText etPassword;
    private TextView lblPassword;
    private LinearLayout otpContainer;
    private EditText etOtp;
    private Button btnPrimaryAction;
    private ProgressBar authProgress;
    private TextView tvForgotPassword;
    private TextView tvSwitchMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        backendApi = new BackendApi(this);

        authTitle = (TextView) findViewById(R.id.authTitle);
        etEmail = (EditText) findViewById(R.id.etEmail);
        etPassword = (EditText) findViewById(R.id.etPassword);
        lblPassword = (TextView) findViewById(R.id.lblPassword);
        otpContainer = (LinearLayout) findViewById(R.id.otpContainer);
        etOtp = (EditText) findViewById(R.id.etOtp);
        btnPrimaryAction = (Button) findViewById(R.id.btnPrimaryAction);
        authProgress = (ProgressBar) findViewById(R.id.authProgress);
        tvForgotPassword = (TextView) findViewById(R.id.tvForgotPassword);
        tvSwitchMode = (TextView) findViewById(R.id.tvSwitchMode);

        updateModeUI();

        btnPrimaryAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handlePrimaryAction();
            }
        });

        tvSwitchMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentMode == MODE_LOGIN) {
                    currentMode = MODE_REGISTER;
                } else {
                    currentMode = MODE_LOGIN;
                }
                updateModeUI();
            }
        });

        tvForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentMode == MODE_FORGOT_REQUEST || currentMode == MODE_FORGOT_RESET) {
                    currentMode = MODE_LOGIN;
                } else {
                    currentMode = MODE_FORGOT_REQUEST;
                }
                updateModeUI();
            }
        });
    }

    private void updateModeUI() {
        otpContainer.setVisibility(View.GONE);
        lblPassword.setVisibility(View.VISIBLE);
        etPassword.setVisibility(View.VISIBLE);

        if (currentMode == MODE_LOGIN) {
            authTitle.setText("Sign In to Portal");
            lblPassword.setText("Password");
            etPassword.setHint("••••••••");
            btnPrimaryAction.setText("Sign In");
            tvForgotPassword.setVisibility(View.VISIBLE);
            tvForgotPassword.setText("Forgot Password?");
            tvSwitchMode.setVisibility(View.VISIBLE);
            tvSwitchMode.setText("Don't have an account? Register");
        } else if (currentMode == MODE_REGISTER) {
            authTitle.setText("Register New Account");
            lblPassword.setText("Choose Password");
            etPassword.setHint("••••••••");
            btnPrimaryAction.setText("Create Account");
            tvForgotPassword.setVisibility(View.GONE);
            tvSwitchMode.setVisibility(View.VISIBLE);
            tvSwitchMode.setText("Already registered? Sign In");
        } else if (currentMode == MODE_FORGOT_REQUEST) {
            authTitle.setText("Reset Password - Step 1");
            lblPassword.setVisibility(View.GONE);
            etPassword.setVisibility(View.GONE);
            btnPrimaryAction.setText("Send OTP to Email");
            tvForgotPassword.setVisibility(View.VISIBLE);
            tvForgotPassword.setText("Back to Sign In");
            tvSwitchMode.setVisibility(View.GONE);
        } else if (currentMode == MODE_FORGOT_RESET) {
            authTitle.setText("Reset Password - Step 2");
            otpContainer.setVisibility(View.VISIBLE);
            lblPassword.setVisibility(View.VISIBLE);
            lblPassword.setText("Enter New Password");
            etPassword.setVisibility(View.VISIBLE);
            etPassword.setHint("Enter strong password");
            btnPrimaryAction.setText("Reset Password & Sign In");
            tvForgotPassword.setVisibility(View.VISIBLE);
            tvForgotPassword.setText("Cancel");
            tvSwitchMode.setVisibility(View.GONE);
        }
    }

    private void setLoading(boolean loading) {
        authProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnPrimaryAction.setEnabled(!loading);
    }

    private void handlePrimaryAction() {
        final String email = etEmail.getText().toString().trim();
        final String password = etPassword.getText().toString().trim();

        if (email.isEmpty()) {
            Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentMode == MODE_LOGIN) {
            if (password.isEmpty()) {
                Toast.makeText(this, "Please enter your password", Toast.LENGTH_SHORT).show();
                return;
            }
            setLoading(true);
            backendApi.login(email, password, new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(JSONObject jsonResponse) {
                    setLoading(false);
                    Toast.makeText(AuthActivity.this, "Welcome to Morg Billing!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                }

                @Override
                public void onError(String errorMessage) {
                    setLoading(false);
                    Toast.makeText(AuthActivity.this, "Login failed: " + errorMessage, Toast.LENGTH_LONG).show();
                }
            });

        } else if (currentMode == MODE_REGISTER) {
            if (password.length() < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }
            setLoading(true);
            // Registration -> On success automatically login to get authentication token
            backendApi.register(email, password, new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(JSONObject jsonResponse) {
                    backendApi.login(email, password, new BackendApi.ApiCallback() {
                        @Override
                        public void onSuccess(JSONObject loginResponse) {
                            setLoading(false);
                            Toast.makeText(AuthActivity.this, "Account created & signed in!", Toast.LENGTH_SHORT).show();
                            setResult(RESULT_OK);
                            finish();
                        }

                        @Override
                        public void onError(String errorMessage) {
                            setLoading(false);
                            Toast.makeText(AuthActivity.this, "Registered successfully! Please sign in.", Toast.LENGTH_LONG).show();
                            currentMode = MODE_LOGIN;
                            updateModeUI();
                        }
                    });
                }

                @Override
                public void onError(String errorMessage) {
                    setLoading(false);
                    Toast.makeText(AuthActivity.this, "Registration failed: " + errorMessage, Toast.LENGTH_LONG).show();
                }
            });

        } else if (currentMode == MODE_FORGOT_REQUEST) {
            setLoading(true);
            backendApi.requestOtp(email, new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(JSONObject jsonResponse) {
                    setLoading(false);
                    Toast.makeText(AuthActivity.this, "OTP sent to your email!", Toast.LENGTH_LONG).show();
                    currentMode = MODE_FORGOT_RESET;
                    updateModeUI();
                }

                @Override
                public void onError(String errorMessage) {
                    setLoading(false);
                    Toast.makeText(AuthActivity.this, "OTP Request failed: " + errorMessage, Toast.LENGTH_LONG).show();
                }
            });

        } else if (currentMode == MODE_FORGOT_RESET) {
            String otp = etOtp.getText().toString().trim();
            if (otp.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please provide OTP and new password", Toast.LENGTH_SHORT).show();
                return;
            }
            setLoading(true);
            backendApi.resetPassword(email, otp, password, new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(JSONObject jsonResponse) {
                    // Password updated, now log in automatically
                    backendApi.login(email, password, new BackendApi.ApiCallback() {
                        @Override
                        public void onSuccess(JSONObject loginResp) {
                            setLoading(false);
                            Toast.makeText(AuthActivity.this, "Password reset successfully!", Toast.LENGTH_SHORT).show();
                            setResult(RESULT_OK);
                            finish();
                        }

                        @Override
                        public void onError(String errMsg) {
                            setLoading(false);
                            Toast.makeText(AuthActivity.this, "Password reset. Please sign in.", Toast.LENGTH_LONG).show();
                            currentMode = MODE_LOGIN;
                            updateModeUI();
                        }
                    });
                }

                @Override
                public void onError(String errorMessage) {
                    setLoading(false);
                    Toast.makeText(AuthActivity.this, "Password reset error: " + errorMessage, Toast.LENGTH_LONG).show();
                }
            });
        }
    }
}