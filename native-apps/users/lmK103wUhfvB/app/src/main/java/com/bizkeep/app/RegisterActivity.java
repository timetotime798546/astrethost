package com.bizkeep.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class RegisterActivity extends Activity {

    private EditText etEmail, etPassword, etConfirmPassword;
    private Button btnRegister;
    private ProgressBar registerProgress;
    private TextView tvLoginLink;
    private BackendApi backendApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        backendApi = new BackendApi(this);

        etEmail = (EditText) findViewById(R.id.et_register_email);
        etPassword = (EditText) findViewById(R.id.et_register_password);
        etConfirmPassword = (EditText) findViewById(R.id.et_register_confirm_password);
        btnRegister = (Button) findViewById(R.id.btn_register);
        registerProgress = (ProgressBar) findViewById(R.id.register_progress);
        tvLoginLink = (TextView) findViewById(R.id.tv_go_to_login);

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performRegister();
            }
        });

        tvLoginLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void performRegister() {
        final String email = etEmail.getText().toString().trim();
        final String password = etPassword.getText().toString().trim();
        final String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Please write content to all input slots", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        registerProgress.setVisibility(View.VISIBLE);
        btnRegister.setEnabled(false);

        // Step 1: Request Register
        backendApi.register(email, password, new BackendApi.ApiCallback<String>() {
            @Override
            public void onSuccess(String result) {
                // Step 2: Since successful registration returns no token, perform dynamic internal auto-login
                backendApi.login(email, password, new BackendApi.ApiCallback<String>() {
                    @Override
                    public void onSuccess(final String token) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                registerProgress.setVisibility(View.GONE);
                                btnRegister.setEnabled(true);
                                Toast.makeText(RegisterActivity.this, "Sign-up & login verified!", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                                finish();
                            }
                        });
                    }

                    @Override
                    public void onError(final String error) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                registerProgress.setVisibility(View.GONE);
                                btnRegister.setEnabled(true);
                                Toast.makeText(RegisterActivity.this, "Account generated. Please sign-in manually.", Toast.LENGTH_LONG).show();
                                finish();
                            }
                        });
                    }
                });
            }

            @Override
            public void onError(final String error) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        registerProgress.setVisibility(View.GONE);
                        btnRegister.setEnabled(true);
                        Toast.makeText(RegisterActivity.this, "Registration Failure: " + error, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }
}