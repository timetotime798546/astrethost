package com.studyplanner.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONObject;

public class RegisterActivity extends Activity {
    private EditText etRegEmail, etRegPassword, etRegConfirmPassword;
    private Button btnRegister;
    private TextView tvLoginLink;
    private ProgressBar pbRegLoading;
    private BackendApi backendApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etRegEmail = (EditText) findViewById(R.id.etRegEmail);
        etRegPassword = (EditText) findViewById(R.id.etRegPassword);
        etRegConfirmPassword = (EditText) findViewById(R.id.etRegConfirmPassword);
        btnRegister = (Button) findViewById(R.id.btnRegister);
        tvLoginLink = (TextView) findViewById(R.id.tvLoginLink);
        pbRegLoading = (ProgressBar) findViewById(R.id.pbRegLoading);

        backendApi = new BackendApi(this);

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performRegistration();
            }
        });

        tvLoginLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void performRegistration() {
        final String email = etRegEmail.getText().toString().trim();
        final String password = etRegPassword.getText().toString().trim();
        String confirmPassword = etRegConfirmPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        pbRegLoading.setVisibility(View.VISIBLE);
        btnRegister.setEnabled(false);

        // Register Account
        backendApi.register(email, password, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                // Once successfully registered, auto-login to receive auth token
                backendApi.login(email, password, new BackendApi.ApiCallback() {
                    @Override
                    public void onSuccess(String loginResponse) {
                        pbRegLoading.setVisibility(View.GONE);
                        btnRegister.setEnabled(true);
                        try {
                            JSONObject json = new JSONObject(loginResponse);
                            String token = json.getString("token");

                            SharedPreferences.Editor editor = getSharedPreferences("StudyPlanner", MODE_PRIVATE).edit();
                            editor.putString("token", token);
                            editor.putString("email", email);
                            editor.apply();

                            Toast.makeText(RegisterActivity.this, "Account configured successfully!", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            finish();
                        } catch (Exception e) {
                            Toast.makeText(RegisterActivity.this, "Login Parse Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onError(String loginError) {
                        pbRegLoading.setVisibility(View.GONE);
                        btnRegister.setEnabled(true);
                        Toast.makeText(RegisterActivity.this, "Auto-login failed: " + loginError, Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onError(String error) {
                pbRegLoading.setVisibility(View.GONE);
                btnRegister.setEnabled(true);
                Toast.makeText(RegisterActivity.this, "Registration error: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }
}