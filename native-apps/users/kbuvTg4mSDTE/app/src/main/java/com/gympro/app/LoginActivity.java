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

    private EditText etLoginEmail;
    private EditText etLoginPassword;
    private Button btnLogin;
    private ProgressBar pbLogin;
    private TextView tvLoginStatus;
    private TextView tvForgotPassword;
    private TextView tvRegisterLink;

    private AuthManager authManager;
    private BackendApi backendApi;

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

        backendApi = new BackendApi(this);

        etLoginEmail = (EditText) findViewById(R.id.etLoginEmail);
        etLoginPassword = (EditText) findViewById(R.id.etLoginPassword);
        btnLogin = (Button) findViewById(R.id.btnLogin);
        pbLogin = (ProgressBar) findViewById(R.id.pbLogin);
        tvLoginStatus = (TextView) findViewById(R.id.tvLoginStatus);
        tvForgotPassword = (TextView) findViewById(R.id.tvForgotPassword);
        tvRegisterLink = (TextView) findViewById(R.id.tvRegisterLink);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performLogin();
            }
        });

        tvForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class));
            }
        });

        tvRegisterLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            }
        });
    }

    private void performLogin() {
        String email = etLoginEmail.getText().toString().trim();
        String password = etLoginPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            tvLoginStatus.setText("Please fill in all fields");
            tvLoginStatus.setVisibility(View.VISIBLE);
            return;
        }

        tvLoginStatus.setVisibility(View.GONE);
        pbLogin.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);

        backendApi.login(email, password, new BackendApi.ApiCallback() {
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
                tvLoginStatus.setText(errorMessage);
                tvLoginStatus.setVisibility(View.VISIBLE);
            }
        });
    }
}