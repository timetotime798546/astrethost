package com.studentmanager.app;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class LoginActivity extends Activity {

    private EditText etEmail;
    private EditText etPassword;
    private Button btnAction;
    private TextView tvSwitchMode;
    private TextView tvTitle;
    
    private boolean isLoginMode = true;
    private BackendApi backendApi;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        backendApi = new BackendApi(this);
        if (backendApi.isLoggedIn()) {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        etEmail = (EditText) findViewById(R.id.et_email);
        etPassword = (EditText) findViewById(R.id.et_password);
        btnAction = (Button) findViewById(R.id.btn_action);
        tvSwitchMode = (TextView) findViewById(R.id.tv_switch_mode);
        tvTitle = (TextView) findViewById(R.id.tv_title);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);

        btnAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleAuth();
            }
        });

        tvSwitchMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleMode();
            }
        });
    }

    private void toggleMode() {
        isLoginMode = !isLoginMode;
        if (isLoginMode) {
            tvTitle.setText("Sign In");
            btnAction.setText("Login");
            tvSwitchMode.setText("Don't have an account? Register");
        } else {
            tvTitle.setText("Create Account");
            btnAction.setText("Register");
            tvSwitchMode.setText("Already have an account? Login");
        }
    }

    private void handleAuth() {
        final String email = etEmail.getText().toString().trim();
        final String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter both email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.show();

        if (isLoginMode) {
            backendApi.login(email, password, new ApiCallback<String>() {
                @Override
                public void onSuccess(String result) {
                    progressDialog.dismiss();
                    Toast.makeText(LoginActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();
                }

                @Override
                public void onError(String errorMsg) {
                    progressDialog.dismiss();
                    Toast.makeText(LoginActivity.this, "Login failed: " + errorMsg, Toast.LENGTH_LONG).show();
                }
            });
        } else {
            backendApi.register(email, password, new ApiCallback<String>() {
                @Override
                public void onSuccess(String result) {
                    backendApi.login(email, password, new ApiCallback<String>() {
                        @Override
                        public void onSuccess(String loginResult) {
                            progressDialog.dismiss();
                            Toast.makeText(LoginActivity.this, "Registration & Login successful!", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finish();
                        }

                        @Override
                        public void onError(String errorMsg) {
                            progressDialog.dismiss();
                            Toast.makeText(LoginActivity.this, "Registered! Please sign in with your credentials.", Toast.LENGTH_LONG).show();
                            isLoginMode = true;
                            tvTitle.setText("Sign In");
                            btnAction.setText("Login");
                            tvSwitchMode.setText("Don't have an account? Register");
                        }
                    });
                }

                @Override
                public void onError(String errorMsg) {
                    progressDialog.dismiss();
                    Toast.makeText(LoginActivity.this, "Registration failed: " + errorMsg, Toast.LENGTH_LONG).show();
                }
            });
        }
    }
}