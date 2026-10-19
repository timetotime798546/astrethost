package com.mkdeviceapp.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import org.json.JSONObject;

public class AuthActivity extends Activity {

    private boolean isLoginMode = true;
    private BackendApi api;
    private TextView tvAuthTitle;
    private EditText etEmail;
    private EditText etPassword;
    private Button btnSubmit;
    private Button btnToggle;
    private Button btnForgot;
    private ProgressBar pbLoading;
    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        api = new BackendApi(this);

        tvAuthTitle = (TextView) findViewById(R.id.tv_auth_title);
        etEmail = (EditText) findViewById(R.id.et_auth_email);
        etPassword = (EditText) findViewById(R.id.et_auth_password);
        btnSubmit = (Button) findViewById(R.id.btn_auth_submit);
        btnToggle = (Button) findViewById(R.id.btn_toggle_mode);
        btnForgot = (Button) findViewById(R.id.btn_forgot_password);
        pbLoading = (ProgressBar) findViewById(R.id.pb_auth_loading);
        tvStatus = (TextView) findViewById(R.id.tv_auth_status);

        btnToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isLoginMode = !isLoginMode;
                if (isLoginMode) {
                    tvAuthTitle.setText("Controller Sign In");
                    btnSubmit.setText("Sign In");
                    btnToggle.setText("Need a new account? Register");
                    btnForgot.setVisibility(View.VISIBLE);
                } else {
                    tvAuthTitle.setText("Register Master Controller");
                    btnSubmit.setText("Create Account");
                    btnToggle.setText("Already have an account? Sign In");
                    btnForgot.setVisibility(View.GONE);
                }
                tvStatus.setVisibility(View.GONE);
            }
        });

        btnForgot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(AuthActivity.this, ForgotPasswordActivity.class);
                i.putExtra("email", etEmail.getText().toString().trim());
                startActivity(i);
            }
        });

        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleAuth();
            }
        });
    }

    private void handleAuth() {
        final String email = etEmail.getText().toString().trim();
        final String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Please enter both email and password");
            return;
        }

        setLoading(true);

        if (isLoginMode) {
            api.login(email, password, new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setLoading(false);
                            Intent intent = new Intent(AuthActivity.this, MainActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
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
                            showError(errorMessage);
                        }
                    });
                }
            });
        } else {
            // Register flow
            api.register(email, password, new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    // Critical rule: /register does not return token. Automatically call /login
                    api.login(email, password, new BackendApi.ApiCallback() {
                        @Override
                        public void onSuccess(JSONObject loginResponse) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    setLoading(false);
                                    Intent intent = new Intent(AuthActivity.this, MainActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(intent);
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
                                    showError("Registered successfully. Please log in manually.");
                                    isLoginMode = true;
                                    tvAuthTitle.setText("Controller Sign In");
                                    btnSubmit.setText("Sign In");
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
                            showError(errorMessage);
                        }
                    });
                }
            });
        }
    }

    private void setLoading(boolean loading) {
        pbLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSubmit.setEnabled(!loading);
        btnToggle.setEnabled(!loading);
        if (loading) {
            tvStatus.setVisibility(View.GONE);
        }
    }

    private void showError(String msg) {
        tvStatus.setText(msg);
        tvStatus.setVisibility(View.VISIBLE);
    }
}