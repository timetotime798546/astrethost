package com.smartbudgettracker.app;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import org.json.JSONObject;

public class RegisterActivity extends Activity {

    private EditText etRegEmail, etRegPassword, etRegConfirmPassword;
    private Button btnDoRegister, btnBackToLogin;
    private BackendApi backendApi;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        backendApi = new BackendApi(this);

        etRegEmail = (EditText) findViewById(R.id.etRegEmail);
        etRegPassword = (EditText) findViewById(R.id.etRegPassword);
        etRegConfirmPassword = (EditText) findViewById(R.id.etRegConfirmPassword);
        btnDoRegister = (Button) findViewById(R.id.btnDoRegister);
        btnBackToLogin = (Button) findViewById(R.id.btnBackToLogin);

        btnDoRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptRegistration();
            }
        });

        btnBackToLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void attemptRegistration() {
        final String email = etRegEmail.getText().toString().trim();
        final String password = etRegPassword.getText().toString().trim();
        String confirmPassword = etRegConfirmPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "All fields are required.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters.", Toast.LENGTH_SHORT).show();
            return;
        }

        showProgress("Creating Account...");

        backendApi.register(email, password, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(final String response) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        hideProgress();
                        try {
                            JSONObject json = new JSONObject(response);
                            if (json.optBoolean("success", false)) {
                                // Register successful. Immediately sign in to fetch token.
                                performAutoLogin(email, password);
                            } else {
                                Toast.makeText(RegisterActivity.this, "Registration failed.", Toast.LENGTH_LONG).show();
                            }
                        } catch (Exception e) {
                            Toast.makeText(RegisterActivity.this, "Error parsing server details.", Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }

            @Override
            public void onError(final String errorMessage) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        hideProgress();
                        Toast.makeText(RegisterActivity.this, "Registration Error: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private void performAutoLogin(String email, String password) {
        showProgress("Setting up environment...");
        backendApi.login(email, password, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(final String response) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        hideProgress();
                        try {
                            JSONObject json = new JSONObject(response);
                            if (json.optBoolean("success", false)) {
                                String token = json.getString("token");
                                backendApi.setToken(token);
                                Toast.makeText(RegisterActivity.this, "Registration Successful!", Toast.LENGTH_LONG).show();
                                Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            } else {
                                Toast.makeText(RegisterActivity.this, "Created successfully. Please log in.", Toast.LENGTH_LONG).show();
                                finish();
                            }
                        } catch (Exception e) {
                            Toast.makeText(RegisterActivity.this, "Registration successful. Please log in.", Toast.LENGTH_LONG).show();
                            finish();
                        }
                    }
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        hideProgress();
                        Toast.makeText(RegisterActivity.this, "Created successfully. Please log in manually.", Toast.LENGTH_LONG).show();
                        finish();
                    }
                });
            }
        });
    }

    private void showProgress(String message) {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(message);
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    private void hideProgress() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}