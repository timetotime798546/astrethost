package com.cloudnotespro.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import android.app.Activity;

import org.json.JSONObject;

public class LoginActivity extends Activity {

    private EditText etEmail, etPassword;
    private Button btnLogin, btnGoToRegister;
    private BackendApi backendApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            SharedPreferences prefs = getSharedPreferences("CloudNotesPrefs", MODE_PRIVATE);
            String existingToken = prefs.getString("auth_token", null);
            if (existingToken != null && !existingToken.isEmpty()) {
                Intent mainIntent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(mainIntent);
                finish();
                return;
            }
        } catch (Exception e) {
            // ignore preference read errors and proceed to login screen
        }

        try {
            setContentView(R.layout.activity_login);
        } catch (Exception e) {
            Toast.makeText(this, "Layout error, please restart", Toast.LENGTH_SHORT).show();
            return;
        }

        backendApi = new BackendApi(this);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoToRegister = findViewById(R.id.btnGoToRegister);

        if (btnLogin != null) {
            btnLogin.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final String email = (etEmail != null && etEmail.getText() != null) ? etEmail.getText().toString().trim() : "";
                    final String password = (etPassword != null && etPassword.getText() != null) ? etPassword.getText().toString().trim() : "";

                    if (email.isEmpty() || password.isEmpty()) {
                        Toast.makeText(LoginActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    btnLogin.setEnabled(false);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            JSONObject result = null;
                            try {
                                result = backendApi.login(email, password);
                            } catch (Exception e) {
                                result = null;
                            }

                            final JSONObject finalResult = result;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (btnLogin != null) {
                                        btnLogin.setEnabled(true);
                                    }
                                    try {
                                        if (finalResult != null && finalResult.optBoolean("success", false)) {
                                            String token = finalResult.optString("token", "");
                                            if (token.isEmpty() && finalResult.has("access_token")) {
                                                token = finalResult.optString("access_token", "");
                                            }
                                            if (token.isEmpty() && finalResult.has("data")) {
                                                JSONObject dataObj = finalResult.optJSONObject("data");
                                                if (dataObj != null) {
                                                    token = dataObj.optString("token", dataObj.optString("access_token", ""));
                                                }
                                            }
                                            if (token.isEmpty()) {
                                                token = "active_session";
                                            }

                                            SharedPreferences.Editor editor = getSharedPreferences("CloudNotesPrefs", MODE_PRIVATE).edit();
                                            editor.putString("auth_token", token);
                                            editor.apply();

                                            Toast.makeText(LoginActivity.this, "Login Successful!", Toast.LENGTH_SHORT).show();
                                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                            startActivity(intent);
                                            finish();
                                        } else {
                                            String err = "Login failed";
                                            if (finalResult != null) {
                                                if (finalResult.has("error")) {
                                                    Object errObj = finalResult.opt("error");
                                                    if (errObj != null) err = errObj.toString();
                                                } else if (finalResult.has("message")) {
                                                    Object msgObj = finalResult.opt("message");
                                                    if (msgObj != null) err = msgObj.toString();
                                                }
                                            }
                                            Toast.makeText(LoginActivity.this, err, Toast.LENGTH_LONG).show();
                                        }
                                    } catch (Exception e) {
                                        Toast.makeText(LoginActivity.this, "Error processing login response", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                        }
                    }).start();
                }
            });
        }

        if (btnGoToRegister != null) {
            btnGoToRegister.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        Intent regIntent = new Intent(LoginActivity.this, RegisterActivity.class);
                        startActivity(regIntent);
                    } catch (Exception e) {
                        Toast.makeText(LoginActivity.this, "Unable to open registration", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }
}