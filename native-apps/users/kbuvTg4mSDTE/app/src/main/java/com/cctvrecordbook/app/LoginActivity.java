package com.cctvrecordbook.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import org.json.JSONObject;

public class LoginActivity extends Activity {

    private EditText mEtEmail;
    private EditText mEtPassword;
    private Button mBtnLogin;
    private Button mBtnRegister;
    private ProgressBar mProgressBar;
    private BackendApi mApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mApi = new BackendApi(this);

        // Session recovery redirection
        if (mApi.isLoggedIn()) {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        mEtEmail = findViewById(R.id.et_email);
        mEtPassword = findViewById(R.id.et_password);
        mBtnLogin = findViewById(R.id.btn_login);
        mBtnRegister = findViewById(R.id.btn_register);
        mProgressBar = findViewById(R.id.progress_bar);

        mBtnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performLogin();
            }
        });

        mBtnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performRegister();
            }
        });
    }

    private void performLogin() {
        final String email = mEtEmail.getText().toString().trim();
        final String password = mEtPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            showErrorDialog("Email or Password aur Password empty nahi hone chahiye.");
            return;
        }

        setLoading(true);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final JSONObject response = mApi.login(email, password);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setLoading(false);
                            if (response.optBoolean("success")) {
                                Toast.makeText(LoginActivity.this, "Login Safal Raha!", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                finish();
                            } else {
                                String err = response.optString("error", "Login fail ho gaya. Kripya checks karein.");
                                showErrorDialog(err);
                            }
                        }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setLoading(false);
                            showErrorDialog("Internet Connection check karein: " + e.getMessage());
                        }
                    });
                }
            }
        }).start();
    }

    private void performRegister() {
        final String email = mEtEmail.getText().toString().trim();
        final String password = mEtPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            showErrorDialog("Email aur Password register karne ke liye empty nahi hone chahiye.");
            return;
        }

        if (password.length() < 6) {
            showErrorDialog("Password ki length kam se kam 6 characters honi chahiye.");
            return;
        }

        setLoading(true);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // 1. Call Register
                    final JSONObject regResponse = mApi.register(email, password);
                    if (regResponse.optBoolean("success")) {
                        // 2. Perform Automatic Login immediately to retrieve token
                        final JSONObject logResponse = mApi.login(email, password);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                setLoading(false);
                                if (logResponse.optBoolean("success")) {
                                    Toast.makeText(LoginActivity.this, "Account ban gaya aur Login Safal!", Toast.LENGTH_LONG).show();
                                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                    finish();
                                } else {
                                    showErrorDialog("Account toh ban gaya, par auto-login fail ho gaya. Manually Login karein.");
                                }
                            }
                        });
                    } else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                setLoading(false);
                                String err = regResponse.optString("error", "Registration fail ho gaya.");
                                showErrorDialog(err);
                            }
                        });
                    }
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setLoading(false);
                            showErrorDialog("Internet/Server Connection issue: " + e.getMessage());
                        }
                    });
                }
            }
        }).start();
    }

    private void setLoading(boolean loading) {
        if (loading) {
            mProgressBar.setVisibility(View.VISIBLE);
            mBtnLogin.setEnabled(false);
            mBtnRegister.setEnabled(false);
        } else {
            mProgressBar.setVisibility(View.GONE);
            mBtnLogin.setEnabled(true);
            mBtnRegister.setEnabled(true);
        }
    }

    private void showErrorDialog(String message) {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("Alert / सावधान");
        b.setMessage(message);
        b.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        b.create().show();
    }
}