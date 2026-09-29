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

public class SignupActivity extends Activity {
    private EditText etEmail, etPassword;
    private Button btnSignup;
    private ProgressBar loading;
    private BackendApi api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnSignup = findViewById(R.id.btnSignup);
        loading = findViewById(R.id.loading);
        TextView tvLogin = findViewById(R.id.tvLogin);

        api = new BackendApi(this);

        btnSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String email = etEmail.getText().toString().trim();
                final String pass = etPassword.getText().toString().trim();

                if (email.isEmpty() || pass.length() < 6) {
                    Toast.makeText(SignupActivity.this, "Invalid fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                loading.setVisibility(View.VISIBLE);
                btnSignup.setEnabled(false);

                api.register(email, pass, new BackendApi.ApiCallback() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        // Registration success, now login to get token
                        api.login(email, pass, new BackendApi.ApiCallback() {
                            @Override
                            public void onSuccess(final JSONObject loginRes) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        loading.setVisibility(View.GONE);
                                        SessionManager session = new SessionManager(SignupActivity.this);
                                        session.createSession(loginRes.optString("token"), email);
                                        startActivity(new Intent(SignupActivity.this, DashboardActivity.class));
                                        finish();
                                    }
                                });
                            }

                            @Override
                            public void onError(String message) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        loading.setVisibility(View.GONE);
                                        btnSignup.setEnabled(true);
                                        Toast.makeText(SignupActivity.this, "Reg success, please login manually.", Toast.LENGTH_LONG).show();
                                        finish();
                                    }
                                });
                            }
                        });
                    }

                    @Override
                    public void onError(final String message) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                loading.setVisibility(View.GONE);
                                btnSignup.setEnabled(true);
                                Toast.makeText(SignupActivity.this, "Error: " + message, Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                });
            }
        });

        tvLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}