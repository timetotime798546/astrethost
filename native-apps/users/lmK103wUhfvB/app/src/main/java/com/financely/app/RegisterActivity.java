package com.financely.app;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONObject;

public class RegisterActivity extends Activity {

    private EditText etEmail, etPassword;
    private Button btnRegister;
    private TextView tvLoginNow;
    private BackendApi api;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        api = new BackendApi(this);

        etEmail = (EditText) findViewById(R.id.et_reg_email);
        etPassword = (EditText) findViewById(R.id.et_reg_password);
        btnRegister = (Button) findViewById(R.id.btn_register);
        tvLoginNow = (TextView) findViewById(R.id.tv_login_now);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Creating account...");
        progressDialog.setCancelable(false);

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptRegister();
            }
        });

        tvLoginNow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void attemptRegister() {
        final String email = etEmail.getText().toString().trim();
        final String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all inputs.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.show();
        api.register(email, password, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                boolean success = response.optBoolean("success", false);
                if (success) {
                    // Automatically log in after registration to retrieve session token
                    progressDialog.setMessage("Signing in...");
                    api.login(email, password, new BackendApi.ApiCallback() {
                        @Override
                        public void onSuccess(JSONObject loginResponse) {
                            progressDialog.dismiss();
                            try {
                                boolean loginSuccess = loginResponse.optBoolean("success", false);
                                if (loginSuccess) {
                                    String token = loginResponse.optString("token", "");
                                    if (!token.isEmpty()) {
                                        SharedPreferences.Editor editor = getSharedPreferences("financely_prefs", Context.MODE_PRIVATE).edit();
                                        editor.putString("token", token);
                                        editor.putString("email", email);
                                        editor.apply();

                                        Toast.makeText(RegisterActivity.this, "Account verified & configured successfully!", Toast.LENGTH_SHORT).show();
                                        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(intent);
                                        finish();
                                    } else {
                                        Toast.makeText(RegisterActivity.this, "Error obtaining session key.", Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    Toast.makeText(RegisterActivity.this, "Auto-login failed. Try logging in manually.", Toast.LENGTH_LONG).show();
                                    finish();
                                }
                            } catch (Exception e) {
                                Toast.makeText(RegisterActivity.this, "Error auto-logging: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                finish();
                            }
                        }

                        @Override
                        public void onError(String errorMessage) {
                            progressDialog.dismiss();
                            Toast.makeText(RegisterActivity.this, "Registration Succeeded. Please login manually.", Toast.LENGTH_LONG).show();
                            finish();
                        }
                    });
                } else {
                    progressDialog.dismiss();
                    String msg = response.optString("message", "Registration failed");
                    Toast.makeText(RegisterActivity.this, msg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onError(String errorMessage) {
                progressDialog.dismiss();
                Toast.makeText(RegisterActivity.this, "Registration Failed: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }
}