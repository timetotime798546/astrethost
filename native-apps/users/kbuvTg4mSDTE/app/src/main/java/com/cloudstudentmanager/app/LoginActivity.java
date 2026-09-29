package com.cloudstudentmanager.app;

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

public class LoginActivity extends Activity {
    private EditText etEmail, etPassword;
    private Button btnLogin;
    private ProgressBar loading;
    private BackendApi api;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        loading = findViewById(R.id.loading);
        TextView tvSignup = findViewById(R.id.tvSignup);

        api = new BackendApi(this);
        session = new SessionManager(this);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptLogin();
            }
        });

        tvSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, SignupActivity.class));
            }
        });
    }

    private void attemptLogin() {
        final String email = etEmail.getText().toString().trim();
        final String pass = etPassword.getText().toString().trim();

        if (email.isEmpty() || pass.length() < 6) {
            Toast.makeText(this, "Enter valid credentials", Toast.LENGTH_SHORT).show();
            return;
        }

        loading.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);

        api.login(email, pass, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(final JSONObject response) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        loading.setVisibility(View.GONE);
                        String token = response.optString("token");
                        session.createSession(token, email);
                        startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
                        finish();
                    }
                });
            }

            @Override
            public void onError(final String message) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        loading.setVisibility(View.GONE);
                        btnLogin.setEnabled(true);
                        Toast.makeText(LoginActivity.this, "Error: " + message, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }
}