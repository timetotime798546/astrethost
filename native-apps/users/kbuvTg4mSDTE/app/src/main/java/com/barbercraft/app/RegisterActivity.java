package com.barbercraft.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

public class RegisterActivity extends Activity {

    private BackendApi backendApi;
    private EditText etEmail;
    private EditText etPassword;
    private EditText etConfirmPassword;
    private Button btnSubmit;
    private ProgressBar pbLoading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        backendApi = new BackendApi(this);

        Button btnBack = (Button) findViewById(R.id.btnRegisterBack);
        etEmail = (EditText) findViewById(R.id.etRegisterEmail);
        etPassword = (EditText) findViewById(R.id.etRegisterPassword);
        etConfirmPassword = (EditText) findViewById(R.id.etRegisterConfirmPassword);
        TextView tvGoLogin = (TextView) findViewById(R.id.tvGoToLogin);
        btnSubmit = (Button) findViewById(R.id.btnSubmitRegister);
        pbLoading = (ProgressBar) findViewById(R.id.pbRegisterLoading);

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        tvGoLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });

        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performRegister();
            }
        });
    }

    private void performRegister() {
        final String email = etEmail.getText().toString().trim();
        final String password = etPassword.getText().toString().trim();
        String confirm = etConfirmPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill out all credentials", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirm)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSubmit.setEnabled(false);
        pbLoading.setVisibility(View.VISIBLE);

        // Step 1: Call /register (never expects token)
        backendApi.register(email, password, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                // Step 2: Call /login to extract and store token
                backendApi.login(email, password, new BackendApi.ApiCallback() {
                    @Override
                    public void onSuccess(JSONObject loginResponse) {
                        btnSubmit.setEnabled(true);
                        pbLoading.setVisibility(View.GONE);
                        Toast.makeText(RegisterActivity.this, "Account created successfully!", Toast.LENGTH_SHORT).show();
                        finish();
                    }

                    @Override
                    public void onError(String errorMessage) {
                        btnSubmit.setEnabled(true);
                        pbLoading.setVisibility(View.GONE);
                        Toast.makeText(RegisterActivity.this, "Account created. Please login manually.", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
            }

            @Override
            public void onError(String errorMessage) {
                btnSubmit.setEnabled(true);
                pbLoading.setVisibility(View.GONE);
                Toast.makeText(RegisterActivity.this, "Registration failed: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }
}