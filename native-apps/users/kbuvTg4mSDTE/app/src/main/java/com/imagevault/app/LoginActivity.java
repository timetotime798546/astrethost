package com.imagevault.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import org.json.JSONObject;

public class LoginActivity extends Activity {

    private enum Mode {
        LOGIN, REGISTER, FORGOT_REQUEST, FORGOT_VERIFY
    }

    private Mode currentMode = Mode.LOGIN;
    private BackendApi backendApi;

    private TextView tvTitle;
    private TextView tvSubtitle;
    private EditText etEmail;
    private EditText etPassword;
    private LinearLayout layoutResetFields;
    private EditText etOtp;
    private EditText etNewPassword;
    private ProgressBar authProgress;
    private TextView tvStatusMessage;
    private Button btnPrimaryAction;
    private Button btnToggleMode;
    private Button btnForgotPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        backendApi = new BackendApi(this);

        tvTitle = (TextView) findViewById(R.id.tv_auth_title);
        tvSubtitle = (TextView) findViewById(R.id.tv_auth_subtitle);
        etEmail = (EditText) findViewById(R.id.et_email);
        etPassword = (EditText) findViewById(R.id.et_password);
        layoutResetFields = (LinearLayout) findViewById(R.id.layout_reset_fields);
        etOtp = (EditText) findViewById(R.id.et_otp);
        etNewPassword = (EditText) findViewById(R.id.et_new_password);
        authProgress = (ProgressBar) findViewById(R.id.auth_progress);
        tvStatusMessage = (TextView) findViewById(R.id.tv_status_message);
        btnPrimaryAction = (Button) findViewById(R.id.btn_primary_action);
        btnToggleMode = (Button) findViewById(R.id.btn_toggle_mode);
        btnForgotPassword = (Button) findViewById(R.id.btn_forgot_password);

        etEmail.setText(backendApi.getSavedEmail());

        btnPrimaryAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handlePrimaryAction();
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
                if (currentMode == Mode.FORGOT_REQUEST || currentMode == Mode.FORGOT_VERIFY) {
                    setMode(Mode.LOGIN);
                } else {
                    setMode(Mode.FORGOT_REQUEST);
                }
            }
        });

        setMode(Mode.LOGIN);
    }

    private void setMode(Mode mode) {
        currentMode = mode;
        clearMessage();
        authProgress.setVisibility(View.GONE);

        switch (currentMode) {
            case LOGIN:
                tvTitle.setText("Sign In");
                tvSubtitle.setText("Access your private photo vault");
                etPassword.setVisibility(View.VISIBLE);
                layoutResetFields.setVisibility(View.GONE);
                btnPrimaryAction.setText("Sign In");
                btnToggleMode.setVisibility(View.VISIBLE);
                btnToggleMode.setText("Need an account? Register");
                btnForgotPassword.setVisibility(View.VISIBLE);
                btnForgotPassword.setText("Forgot Password?");
                break;
            case REGISTER:
                tvTitle.setText("Create Account");
                tvSubtitle.setText("Start saving your private photos securely");
                etPassword.setVisibility(View.VISIBLE);
                layoutResetFields.setVisibility(View.GONE);
                btnPrimaryAction.setText("Create Account");
                btnToggleMode.setVisibility(View.VISIBLE);
                btnToggleMode.setText("Already registered? Sign In");
                btnForgotPassword.setVisibility(View.GONE);
                break;
            case FORGOT_REQUEST:
                tvTitle.setText("Reset Password");
                tvSubtitle.setText("Enter your email to receive a reset OTP");
                etPassword.setVisibility(View.GONE);
                layoutResetFields.setVisibility(View.GONE);
                btnPrimaryAction.setText("Send Reset OTP");
                btnToggleMode.setVisibility(View.GONE);
                btnForgotPassword.setVisibility(View.VISIBLE);
                btnForgotPassword.setText("Back to Login");
                break;
            case FORGOT_VERIFY:
                tvTitle.setText("Verify & Reset");
                tvSubtitle.setText("Enter OTP and choose a new password");
                etPassword.setVisibility(View.GONE);
                layoutResetFields.setVisibility(View.VISIBLE);
                btnPrimaryAction.setText("Update Password");
                btnToggleMode.setVisibility(View.GONE);
                btnForgotPassword.setVisibility(View.VISIBLE);
                btnForgotPassword.setText("Cancel");
                break;
        }
    }

    private void showMessage(String text, boolean error) {
        tvStatusMessage.setVisibility(View.VISIBLE);
        tvStatusMessage.setTextColor(getResources().getColor(error ? R.color.danger : R.color.success));
        tvStatusMessage.setText(text);
    }

    private void clearMessage() {
        tvStatusMessage.setVisibility(View.GONE);
        tvStatusMessage.setText("");
    }

    private void setLoading(boolean loading) {
        authProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnPrimaryAction.setEnabled(!loading);
    }

    private void handlePrimaryAction() {
        final String email = etEmail.getText().toString().trim();
        final String password = etPassword.getText().toString().trim();

        if (email.isEmpty()) {
            showMessage("Please enter your email", true);
            return;
        }

        if (currentMode == Mode.LOGIN) {
            if (password.isEmpty()) {
                showMessage("Please enter your password", true);
                return;
            }
            setLoading(true);
            backendApi.login(email, password, new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setLoading(false);
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finish();
                        }
                    });
                }

                @Override
                public void onError(final String errorMessage) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setLoading(false);
                            showMessage(errorMessage, true);
                        }
                    });
                }
            });
        } else if (currentMode == Mode.REGISTER) {
            if (password.isEmpty() || password.length() < 6) {
                showMessage("Password must be at least 6 characters", true);
                return;
            }
            setLoading(true);
            backendApi.register(email, password, new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    // Registration does NOT return token. Perform auto-login using the same credentials.
                    backendApi.login(email, password, new BackendApi.ApiCallback() {
                        @Override
                        public void onSuccess(JSONObject loginResp) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    setLoading(false);
                                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                    finish();
                                }
                            });
                        }

                        @Override
                        public void onError(final String loginErr) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    setLoading(false);
                                    setMode(Mode.LOGIN);
                                    showMessage("Account created! Please log in now.", false);
                                }
                            });
                        }
                    });
                }

                @Override
                public void onError(final String errorMessage) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setLoading(false);
                            showMessage(errorMessage, true);
                        }
                    });
                }
            });
        } else if (currentMode == Mode.FORGOT_REQUEST) {
            setLoading(true);
            backendApi.requestOtp(email, new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setLoading(false);
                            setMode(Mode.FORGOT_VERIFY);
                            showMessage("OTP sent to your email. Check your inbox.", false);
                        }
                    });
                }

                @Override
                public void onError(final String errorMessage) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setLoading(false);
                            showMessage(errorMessage, true);
                        }
                    });
                }
            });
        } else if (currentMode == Mode.FORGOT_VERIFY) {
            String otp = etOtp.getText().toString().trim();
            String newPass = etNewPassword.getText().toString().trim();
            if (otp.isEmpty() || newPass.isEmpty()) {
                showMessage("Please provide the OTP code and new password", true);
                return;
            }
            setLoading(true);
            backendApi.resetPassword(email, otp, newPass, new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setLoading(false);
                            setMode(Mode.LOGIN);
                            showMessage("Password reset successfully. Sign in with your new password.", false);
                        }
                    });
                }

                @Override
                public void onError(final String errorMessage) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setLoading(false);
                            showMessage(errorMessage, true);
                        }
                    });
                }
            });
        }
    }
}