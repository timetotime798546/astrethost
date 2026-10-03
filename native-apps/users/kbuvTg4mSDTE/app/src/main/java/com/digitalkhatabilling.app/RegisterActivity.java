package com.digitalkhatabilling.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONObject;

public class RegisterActivity extends Activity {
    private EditText etEmail, etPassword, etConfirmPassword;
    private Button btnRegister;
    private TextView tvLoginLink;
    private BackendApi backendApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        backendApi = new BackendApi(this);

        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        btnRegister = findViewById(R.id.btn_register);
        tvLoginLink = findViewById(R.id.tv_login_link);

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String email = etEmail.getText().toString().trim();
                final String password = etPassword.getText().toString().trim();
                String confirmPassword = etConfirmPassword.getText().toString().trim();

                if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                    Toast.makeText(RegisterActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!password.equals(confirmPassword)) {
                    Toast.makeText(RegisterActivity.this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                    return;
                }

                btnRegister.setEnabled(false);
                btnRegister.setText("Registering...");

                backendApi.register(email, password, new BackendApi.ApiCallback() {
                    @Override
                    public void onSuccess(final String registerResponse) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                backendApi.login(email, password, new BackendApi.ApiCallback() {
                                    @Override
                                    public void onSuccess(final String loginResponse) {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                btnRegister.setEnabled(true);
                                                btnRegister.setText("Register");
                                                try {
                                                    JSONObject obj = new JSONObject(loginResponse);
                                                    if (obj.optBoolean("success")) {
                                                        String token = obj.optString("token");
                                                        backendApi.saveToken(token);
                                                        Toast.makeText(RegisterActivity.this, "Registration Successful!", Toast.LENGTH_SHORT).show();
                                                        startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                                                        finish();
                                                    } else {
                                                        Toast.makeText(RegisterActivity.this, "Automatic login failed", Toast.LENGTH_LONG).show();
                                                        finish();
                                                    }
                                                } catch (Exception e) {
                                                    Toast.makeText(RegisterActivity.this, "Parse error during login", Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        });
                                    }

                                    @Override
                                    public void onFailure(final String errorMessage) {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                btnRegister.setEnabled(true);
                                                btnRegister.setText("Register");
                                                Toast.makeText(RegisterActivity.this, "Login failed: " + errorMessage, Toast.LENGTH_LONG).show();
                                                finish();
                                            }
                                        });
                                    }
                                });
                            }
                        });
                    }

                    @Override
                    public void onFailure(final String errorMessage) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                btnRegister.setEnabled(true);
                                btnRegister.setText("Register");
                                Toast.makeText(RegisterActivity.this, "Registration Error: " + errorMessage, Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                });
            }
        });

        tvLoginLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}