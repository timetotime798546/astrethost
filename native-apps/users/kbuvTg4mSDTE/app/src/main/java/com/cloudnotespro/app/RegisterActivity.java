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

public class RegisterActivity extends Activity {

    private EditText etEmail, etPassword;
    private Button btnRegister, btnBackToLogin;
    private BackendApi backendApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_register);
        } catch (Exception e) {
            Toast.makeText(this, "Layout error", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        backendApi = new BackendApi(this);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnRegister = findViewById(R.id.btnRegister);
        btnBackToLogin = findViewById(R.id.btnBackToLogin);

        if (btnRegister != null) {
            btnRegister.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final String email = (etEmail != null && etEmail.getText() != null) ? etEmail.getText().toString().trim() : "";
                    final String password = (etPassword != null && etPassword.getText() != null) ? etPassword.getText().toString().trim() : "";

                    if (email.isEmpty() || password.isEmpty()) {
                        Toast.makeText(RegisterActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    btnRegister.setEnabled(false);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            JSONObject regResult = null;
                            try {
                                regResult = backendApi.register(email, password);
                            } catch (Exception e) {
                                regResult = null;
                            }

                            final boolean regSuccess = regResult != null && regResult.optBoolean("success", false);

                            if (regSuccess) {
                                JSONObject loginResult = null;
                                try {
                                    loginResult = backendApi.login(email, password);
                                } catch (Exception e) {
                                    loginResult = null;
                                }

                                final JSONObject finalLoginResult = loginResult;
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (btnRegister != null) {
                                            btnRegister.setEnabled(true);
                                        }
                                        try {
                                            String token = "";
                                            if (finalLoginResult != null && finalLoginResult.optBoolean("success", false)) {
                                                token = finalLoginResult.optString("token", "");
                                                if (token.isEmpty() && finalLoginResult.has("access_token")) {
                                                    token = finalLoginResult.optString("access_token", "");
                                                }
                                                if (token.isEmpty() && finalLoginResult.has("data")) {
                                                    JSONObject dataObj = finalLoginResult.optJSONObject("data");
                                                    if (dataObj != null) {
                                                        token = dataObj.optString("token", dataObj.optString("access_token", ""));
                                                    }
                                                }
                                            }

                                            if (token.isEmpty()) {
                                                token = "active_session";
                                            }

                                            SharedPreferences.Editor editor = getSharedPreferences("CloudNotesPrefs", MODE_PRIVATE).edit();
                                            editor.putString("auth_token", token);
                                            editor.apply();

                                            Toast.makeText(RegisterActivity.this, "Registration Successful!", Toast.LENGTH_SHORT).show();
                                            Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                            startActivity(intent);
                                            finish();
                                        } catch (Exception e) {
                                            Toast.makeText(RegisterActivity.this, "Registration successful. Please login.", Toast.LENGTH_SHORT).show();
                                            finish();
                                        }
                                    }
                                });
                            } else {
                                final JSONObject finalRegResult = regResult;
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (btnRegister != null) {
                                            btnRegister.setEnabled(true);
                                        }
                                        String err = "Registration failed";
                                        if (finalRegResult != null) {
                                            if (finalRegResult.has("error")) {
                                                Object errObj = finalRegResult.opt("error");
                                                if (errObj != null) err = errObj.toString();
                                            } else if (finalRegResult.has("message")) {
                                                Object msgObj = finalRegResult.opt("message");
                                                if (msgObj != null) err = msgObj.toString();
                                            }
                                        }
                                        Toast.makeText(RegisterActivity.this, err, Toast.LENGTH_LONG).show();
                                    }
                                });
                            }
                        }
                    }).start();
                }
            });
        }

        if (btnBackToLogin != null) {
            btnBackToLogin.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        }
    }
}