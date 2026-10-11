package com.gympro.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONObject;

public class LoginActivity extends Activity {
    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvRegisterLink, tvForgotPassword, tvStatus;
    private ProgressBar pbLogin;
    private BackendApi api;
    private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        authManager = new AuthManager(this);
        if (authManager.isLoggedIn()) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        api = new BackendApi(this);

        etEmail = findViewById(R.id.etLoginEmail);
        etPassword = findViewById(R.id.etLoginPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegisterLink = findViewById(R.id.tvRegisterLink);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvStatus = findViewById(R.id.tvLoginStatus);
        pbLogin = findViewById(R.id.pbLogin);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performLogin();
            }
        });

        tvRegisterLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            }
        });

        tvForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class));
            }
        });
    }

    private void performLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Please enter email and password");
            return;
        }

        pbLogin.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);
        tvStatus.setVisibility(View.GONE);

        api.login(email, password, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                pbLogin.setVisibility(View.GONE);
                btnLogin.setEnabled(true);
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                finish();
            }

            @Override
            public void onError(String errorMessage) {
                pbLogin.setVisibility(View.GONE);
                btnLogin.setEnabled(true);
                showError(errorMessage);
            }
        });
    }

    private void showError(String msg) {
        tvStatus.setText(msg);
        tvStatus.setVisibility(View.VISIBLE);
    }
}