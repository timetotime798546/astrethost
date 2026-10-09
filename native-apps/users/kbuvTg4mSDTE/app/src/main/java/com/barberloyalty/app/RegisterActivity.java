package com.barberloyalty.app;

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

    private EditText etEmail, etPassword;
    private Button btnSubmitRegister;
    private ProgressBar pbLoading;
    private TextView btnBackLogin;
    private BackendApi backendApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        backendApi = new BackendApi(this);

        etEmail = (EditText) findViewById(R.id.etEmail);
        etPassword = (EditText) findViewById(R.id.etPassword);
        btnSubmitRegister = (Button) findViewById(R.id.btnSubmitRegister);
        pbLoading = (ProgressBar) findViewById(R.id.pbLoading);
        btnBackLogin = (TextView) findViewById(R.id.btnBackLogin);

        btnSubmitRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performRegister();
            }
        });

        btnBackLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void performRegister() {
        final String email = etEmail.getText().toString().trim();
        final String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Fields cannot be blank", Toast.LENGTH_SHORT).show();
            return;
        }

        pbLoading.setVisibility(View.VISIBLE);
        btnSubmitRegister.setEnabled(false);

        backendApi.register(email, password, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(final JSONObject response) {
                // Successful registration does not return a token.
                // We automatically call /login immediately using same parameters.
                backendApi.login(email, password, new BackendApi.ApiCallback() {
                    @Override
                    public void onSuccess(final JSONObject loginResponse) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                pbLoading.setVisibility(View.GONE);
                                btnSubmitRegister.setEnabled(true);
                                try {
                                    String token = loginResponse.optString("token", "");
                                    SharedPreferences.Editor editor = getSharedPreferences("BarberPrefs", MODE_PRIVATE).edit();
                                    editor.putString("token", token);
                                    editor.putString("email", email);
                                    editor.apply();

                                    Toast.makeText(RegisterActivity.this, "Account Created! Welcome dynamic reward wallet.", Toast.LENGTH_LONG).show();
                                    Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                                    startActivity(intent);
                                    finish();
                                } catch (Exception e) {
                                    Toast.makeText(RegisterActivity.this, "Authentication parsing error", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }

                    @Override
                    public void onError(final String loginError) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                pbLoading.setVisibility(View.GONE);
                                btnSubmitRegister.setEnabled(true);
                                Toast.makeText(RegisterActivity.this, "Created successfully but auto-login failed: " + loginError, Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                });
            }

            @Override
            public void onError(final String errorMessage) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pbLoading.setVisibility(View.GONE);
                        btnSubmitRegister.setEnabled(true);
                        Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }
}