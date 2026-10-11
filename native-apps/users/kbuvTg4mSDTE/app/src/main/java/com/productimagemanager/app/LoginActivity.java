package com.productimagemanager.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class LoginActivity extends Activity {

    private EditText editEmail;
    private EditText editPassword;
    private Button btnLogin;
    private Button btnRegister;
    private TextView tvForgotPassword;
    private ProgressBar progressBar;
    private TextView tvStatusMessage;

    private BackendApi backendApi;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        backendApi = new BackendApi(this);
        mainHandler = new Handler(Looper.getMainLooper());

        editEmail = (EditText) findViewById(R.id.editEmail);
        editPassword = (EditText) findViewById(R.id.editPassword);
        btnLogin = (Button) findViewById(R.id.btnLogin);
        btnRegister = (Button) findViewById(R.id.btnRegister);
        tvForgotPassword = (TextView) findViewById(R.id.tvForgotPassword);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        tvStatusMessage = (TextView) findViewById(R.id.tvStatusMessage);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performLogin();
            }
        });

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performRegister();
            }
        });

        tvForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showForgotPasswordDialog();
            }
        });
    }

    private void setLoading(final boolean loading, final String message) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
                btnLogin.setEnabled(!loading);
                btnRegister.setEnabled(!loading);
                if (message != null && !message.isEmpty()) {
                    tvStatusMessage.setVisibility(View.VISIBLE);
                    tvStatusMessage.setText(message);
                } else {
                    tvStatusMessage.setVisibility(View.GONE);
                }
            }
        });
    }

    private void performLogin() {
        final String email = editEmail.getText().toString().trim();
        final String password = editPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true, "Signing in...");
        backendApi.login(email, password, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        setLoading(false, null);
                        Toast.makeText(LoginActivity.this, "Sign-in successful", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    }
                });
            }

            @Override
            public void onError(final String error) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        setLoading(false, error);
                        Toast.makeText(LoginActivity.this, error, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private void performRegister() {
        final String email = editEmail.getText().toString().trim();
        final String password = editPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email and password to register", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true, "Creating account...");
        backendApi.register(email, password, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        // After successful registration, proceed to automatic login
                        setLoading(true, "Account created. Signing in...");
                        backendApi.login(email, password, new BackendApi.ApiCallback() {
                            @Override
                            public void onSuccess(String loginResponse) {
                                mainHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        setLoading(false, null);
                                        Toast.makeText(LoginActivity.this, "Registered and signed in!", Toast.LENGTH_SHORT).show();
                                        setResult(RESULT_OK);
                                        finish();
                                    }
                                });
                            }

                            @Override
                            public void onError(final String loginErr) {
                                mainHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        setLoading(false, loginErr);
                                        Toast.makeText(LoginActivity.this, "Registration succeeded. Please log in.", Toast.LENGTH_LONG).show();
                                    }
                                });
                            }
                        });
                    }
                });
            }

            @Override
            public void onError(final String error) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        setLoading(false, error);
                        Toast.makeText(LoginActivity.this, error, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private void showForgotPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Reset Password");

        final LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);

        final EditText inputEmail = new EditText(this);
        inputEmail.setHint("Enter registered email");
        inputEmail.setText(editEmail.getText().toString().trim());
        layout.addView(inputEmail);

        builder.setView(layout);

        builder.setPositiveButton("Send OTP", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final String targetEmail = inputEmail.getText().toString().trim();
                if (targetEmail.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "Email is required", Toast.LENGTH_SHORT).show();
                    return;
                }
                setLoading(true, "Requesting OTP...");
                backendApi.requestOtp(targetEmail, new BackendApi.ApiCallback() {
                    @Override
                    public void onSuccess(String response) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                setLoading(false, null);
                                Toast.makeText(LoginActivity.this, "OTP sent to " + targetEmail, Toast.LENGTH_SHORT).show();
                                showResetOtpDialog(targetEmail);
                            }
                        });
                    }

                    @Override
                    public void onError(final String error) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                setLoading(false, error);
                                Toast.makeText(LoginActivity.this, error, Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                });
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showResetOtpDialog(final String email) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter OTP & New Password");

        final LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);

        final EditText inputOtp = new EditText(this);
        inputOtp.setHint("6-digit OTP");
        layout.addView(inputOtp);

        final EditText inputNewPass = new EditText(this);
        inputNewPass.setHint("New Password");
        layout.addView(inputNewPass);

        builder.setView(layout);

        builder.setPositiveButton("Reset Password", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String otp = inputOtp.getText().toString().trim();
                String newPass = inputNewPass.getText().toString().trim();
                if (otp.isEmpty() || newPass.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "OTP and new password required", Toast.LENGTH_SHORT).show();
                    return;
                }

                setLoading(true, "Resetting password...");
                backendApi.resetPassword(email, otp, newPass, new BackendApi.ApiCallback() {
                    @Override
                    public void onSuccess(String response) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                setLoading(false, null);
                                Toast.makeText(LoginActivity.this, "Password updated successfully. Please login.", Toast.LENGTH_LONG).show();
                            }
                        });
                    }

                    @Override
                    public void onError(final String error) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                setLoading(false, error);
                                Toast.makeText(LoginActivity.this, error, Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                });
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
}