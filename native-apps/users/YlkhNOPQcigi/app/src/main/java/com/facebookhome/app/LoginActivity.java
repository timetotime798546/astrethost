package com.facebookhome.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
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
    private Button btnLogin, btnDirectWeb;
    private TextView tvFormTitle, tvToggleMode;
    private ProgressBar progressBar;
    private BackendApi backendApi;

    private boolean isLoginMode = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        backendApi = new BackendApi(this);

        if (backendApi.getAuthToken() != null) {
            startMainActivity();
            finish();
            return;
        }

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnDirectWeb = findViewById(R.id.btnDirectWeb);
        tvFormTitle = findViewById(R.id.tvFormTitle);
        tvToggleMode = findViewById(R.id.tvToggleMode);
        progressBar = findViewById(R.id.progressBar);

        tvToggleMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isLoginMode = !isLoginMode;
                if (isLoginMode) {
                    tvFormTitle.setText("Log Into Facebook");
                    btnLogin.setText("Log In");
                    tvToggleMode.setText("Need an account? Create New Account");
                } else {
                    tvFormTitle.setText("Create a New Account");
                    btnLogin.setText("Sign Up");
                    tvToggleMode.setText("Already have an account? Log In");
                }
            }
        });

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleAuthAction();
            }
        });

        btnDirectWeb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com"));
                startActivity(browserIntent);
            }
        });
    }

    private void handleAuthAction() {
        final String email = etEmail.getText().toString().trim();
        final String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        if (isLoginMode) {
            backendApi.login(email, password, new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    setLoading(false);
                    Toast.makeText(LoginActivity.this, "Welcome back!", Toast.LENGTH_SHORT).show();
                    startMainActivity();
                    finish();
                }

                @Override
                public void onError(String errorMessage) {
                    setLoading(false);
                    Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
            });
        } else {
            backendApi.register(email, password, new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    // Registration succeeded, now perform auto-login
                    backendApi.login(email, password, new BackendApi.ApiCallback() {
                        @Override
                        public void onSuccess(JSONObject loginResponse) {
                            setLoading(false);
                            Toast.makeText(LoginActivity.this, "Account created successfully!", Toast.LENGTH_SHORT).show();
                            startMainActivity();
                            finish();
                        }

                        @Override
                        public void onError(String errorMessage) {
                            setLoading(false);
                            Toast.makeText(LoginActivity.this, "Account created! Please log in manually.", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void onError(String errorMessage) {
                    setLoading(false);
                    Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!isLoading);
    }

    private void startMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
    }
}