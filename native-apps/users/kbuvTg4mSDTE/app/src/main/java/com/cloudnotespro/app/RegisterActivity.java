package com.cloudnotespro.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

public class RegisterActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnRegister, btnBackToLogin;
    private BackendApi backendApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        backendApi = new BackendApi(this);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnRegister = findViewById(R.id.btnRegister);
        btnBackToLogin = findViewById(R.id.btnBackToLogin);

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = etEmail.getText().toString().trim();
                String password = etPassword.getText().toString().trim();

                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(RegisterActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                btnRegister.setEnabled(false);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        JSONObject regResult = backendApi.register(email, password);
                        if (regResult != null && regResult.optBoolean("success", false)) {
                            // Automatically log in after successful registration
                            JSONObject loginResult = backendApi.login(email, password);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    btnRegister.setEnabled(true);
                                    try {
                                        if (loginResult != null && loginResult.optBoolean("success", false)) {
                                            String token = loginResult.optString("token", "");
                                            if (!token.isEmpty()) {
                                                SharedPreferences.Editor editor = getSharedPreferences("CloudNotesPrefs", MODE_PRIVATE).edit();
                                                editor.putString("auth_token", token);
                                                editor.apply();

                                                Toast.makeText(RegisterActivity.this, "Registration Successful!", Toast.LENGTH_SHORT).show();
                                                Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                startActivity(intent);
                                                finish();
                                                return;
                                            }
                                        }
                                        Toast.makeText(RegisterActivity.this, "Registered! Please login.", Toast.LENGTH_SHORT).show();
                                        finish();
                                    } catch (Exception e) {
                                        Toast.makeText(RegisterActivity.this, "Registration successful. Please login.", Toast.LENGTH_SHORT).show();
                                        finish();
                                    }
                                }
                            });
                        } else {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    btnRegister.setEnabled(true);
                                    String err = regResult != null ? regResult.optString("error", "Registration failed") : "Registration failed";
                                    Toast.makeText(RegisterActivity.this, err, Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
                }).start();
            }
        });

        btnBackToLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}