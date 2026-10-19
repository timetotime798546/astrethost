package com.payquick.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

public class AuthActivity extends Activity {

    private static final int MODE_LOGIN = 1;
    private static final int MODE_REGISTER = 2;
    private static final int MODE_FORGOT_REQUEST = 3;
    private static final int MODE_FORGOT_CONFIRM = 4;

    private int mCurrentMode = MODE_LOGIN;
    private BackendApi mBackendApi;

    private TextView mTvTitle;
    private TextView mTvPasswordLabel;
    private EditText mEtEmail;
    private EditText mEtPassword;
    private LinearLayout mLayoutOtpFields;
    private EditText mEtOtp;
    private EditText mEtNewPassword;
    private TextView mTvStatus;
    private Button mBtnAction;
    private Button mBtnToggle;
    private Button mBtnForgotPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        mBackendApi = new BackendApi(this);

        mTvTitle = findViewById(R.id.tv_auth_title);
        mTvPasswordLabel = findViewById(R.id.tv_password_label);
        mEtEmail = findViewById(R.id.et_email);
        mEtPassword = findViewById(R.id.et_password);
        mLayoutOtpFields = findViewById(R.id.layout_otp_fields);
        mEtOtp = findViewById(R.id.et_otp);
        mEtNewPassword = findViewById(R.id.et_new_password);
        mTvStatus = findViewById(R.id.tv_auth_status);
        mBtnAction = findViewById(R.id.btn_action);
        mBtnToggle = findViewById(R.id.btn_toggle_mode);
        mBtnForgotPassword = findViewById(R.id.btn_forgot_password);

        updateModeUI();

        mBtnToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCurrentMode == MODE_LOGIN) {
                    mCurrentMode = MODE_REGISTER;
                } else {
                    mCurrentMode = MODE_LOGIN;
                }
                updateModeUI();
            }
        });

        mBtnForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCurrentMode == MODE_FORGOT_REQUEST || mCurrentMode == MODE_FORGOT_CONFIRM) {
                    mCurrentMode = MODE_LOGIN;
                } else {
                    mCurrentMode = MODE_FORGOT_REQUEST;
                }
                updateModeUI();
            }
        });

        mBtnAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleAction();
            }
        });
    }

    private void updateModeUI() {
        mTvStatus.setVisibility(View.GONE);
        if (mCurrentMode == MODE_LOGIN) {
            mTvTitle.setText("Sign In to Wallet");
            mTvPasswordLabel.setVisibility(View.VISIBLE);
            mEtPassword.setVisibility(View.VISIBLE);
            mLayoutOtpFields.setVisibility(View.GONE);
            mBtnAction.setText("LOGIN");
            mBtnToggle.setVisibility(View.VISIBLE);
            mBtnToggle.setText("Don't have an account? Sign Up");
            mBtnForgotPassword.setText("Forgot Password?");
            mBtnForgotPassword.setVisibility(View.VISIBLE);
        } else if (mCurrentMode == MODE_REGISTER) {
            mTvTitle.setText("Create New Account");
            mTvPasswordLabel.setVisibility(View.VISIBLE);
            mEtPassword.setVisibility(View.VISIBLE);
            mLayoutOtpFields.setVisibility(View.GONE);
            mBtnAction.setText("SIGN UP");
            mBtnToggle.setVisibility(View.VISIBLE);
            mBtnToggle.setText("Already have an account? Sign In");
            mBtnForgotPassword.setVisibility(View.GONE);
        } else if (mCurrentMode == MODE_FORGOT_REQUEST) {
            mTvTitle.setText("Request OTP for Reset");
            mTvPasswordLabel.setVisibility(View.GONE);
            mEtPassword.setVisibility(View.GONE);
            mLayoutOtpFields.setVisibility(View.GONE);
            mBtnAction.setText("SEND OTP CODE");
            mBtnToggle.setVisibility(View.GONE);
            mBtnForgotPassword.setText("Back to Login");
            mBtnForgotPassword.setVisibility(View.VISIBLE);
        } else if (mCurrentMode == MODE_FORGOT_CONFIRM) {
            mTvTitle.setText("Reset Password");
            mTvPasswordLabel.setVisibility(View.GONE);
            mEtPassword.setVisibility(View.GONE);
            mLayoutOtpFields.setVisibility(View.VISIBLE);
            mBtnAction.setText("CONFIRM NEW PASSWORD");
            mBtnToggle.setVisibility(View.GONE);
            mBtnForgotPassword.setText("Cancel and Login");
            mBtnForgotPassword.setVisibility(View.VISIBLE);
        }
    }

    private void handleAction() {
        final String email = mEtEmail.getText().toString().trim();
        final String password = mEtPassword.getText().toString().trim();

        if (email.isEmpty()) {
            showStatus("Please enter your email", true);
            return;
        }

        mBtnAction.setEnabled(false);

        if (mCurrentMode == MODE_LOGIN) {
            if (password.isEmpty()) {
                showStatus("Please enter your password", true);
                mBtnAction.setEnabled(true);
                return;
            }
            showStatus("Signing in...", false);
            mBackendApi.login(email, password, new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    mBtnAction.setEnabled(true);
                    String token = response.optString("token", "");
                    if (!token.isEmpty()) {
                        mBackendApi.saveToken(token, email);
                        Toast.makeText(AuthActivity.this, "Welcome back!", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    } else {
                        showStatus("Login failed: missing token", true);
                    }
                }

                @Override
                public void onError(String errorMessage) {
                    mBtnAction.setEnabled(true);
                    showStatus(errorMessage, true);
                }
            });

        } else if (mCurrentMode == MODE_REGISTER) {
            if (password.length() < 6) {
                showStatus("Password must be at least 6 characters", true);
                mBtnAction.setEnabled(true);
                return;
            }
            showStatus("Registering account...", false);
            // Calling POST /register
            mBackendApi.register(email, password, new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(JSONObject registerResponse) {
                    // Registration succeeded. Critical rule: token is ONLY returned by /login!
                    showStatus("Account created! Authenticating...", false);
                    mBackendApi.login(email, password, new BackendApi.ApiCallback() {
                        @Override
                        public void onSuccess(JSONObject loginResponse) {
                            mBtnAction.setEnabled(true);
                            String token = loginResponse.optString("token", "");
                            if (!token.isEmpty()) {
                                mBackendApi.saveToken(token, email);
                                Toast.makeText(AuthActivity.this, "Account created successfully!", Toast.LENGTH_SHORT).show();
                                setResult(RESULT_OK);
                                finish();
                            } else {
                                showStatus("Registered successfully. Please login.", false);
                                mCurrentMode = MODE_LOGIN;
                                updateModeUI();
                            }
                        }

                        @Override
                        public void onError(String errorMessage) {
                            mBtnAction.setEnabled(true);
                            showStatus("Registered! Please login manually.", false);
                            mCurrentMode = MODE_LOGIN;
                            updateModeUI();
                        }
                    });
                }

                @Override
                public void onError(String errorMessage) {
                    mBtnAction.setEnabled(true);
                    showStatus(errorMessage, true);
                }
            });

        } else if (mCurrentMode == MODE_FORGOT_REQUEST) {
            showStatus("Sending OTP...", false);
            mBackendApi.requestOtp(email, new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    mBtnAction.setEnabled(true);
                    Toast.makeText(AuthActivity.this, "OTP sent to " + email, Toast.LENGTH_SHORT).show();
                    mCurrentMode = MODE_FORGOT_CONFIRM;
                    updateModeUI();
                    showStatus("Check your email for the OTP code", false);
                }

                @Override
                public void onError(String errorMessage) {
                    mBtnAction.setEnabled(true);
                    showStatus(errorMessage, true);
                }
            });

        } else if (mCurrentMode == MODE_FORGOT_CONFIRM) {
            String otp = mEtOtp.getText().toString().trim();
            String newPass = mEtNewPassword.getText().toString().trim();

            if (otp.isEmpty() || newPass.isEmpty()) {
                showStatus("Please enter OTP code and new password", true);
                mBtnAction.setEnabled(true);
                return;
            }

            showStatus("Resetting password...", false);
            mBackendApi.resetPassword(email, otp, newPass, new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    mBtnAction.setEnabled(true);
                    Toast.makeText(AuthActivity.this, "Password updated successfully!", Toast.LENGTH_LONG).show();
                    mCurrentMode = MODE_LOGIN;
                    updateModeUI();
                    showStatus("Password reset! You can now log in.", false);
                }

                @Override
                public void onError(String errorMessage) {
                    mBtnAction.setEnabled(true);
                    showStatus(errorMessage, true);
                }
            });
        }
    }

    private void showStatus(String text, boolean isError) {
        mTvStatus.setVisibility(View.VISIBLE);
        mTvStatus.setTextColor(isError ? 0xFFF87171 : 0xFF38BDF8);
        mTvStatus.setText(text);
    }
}