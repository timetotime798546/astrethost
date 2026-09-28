package com.studentdatasaver.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends Activity {

    private static final String TAG = "LoginActivity";
    private EditText editTextEmail, editTextPassword;
    private Button buttonLogin;
    private TextView textViewRegister;
    private BackendApi backendApi;
    private SharedPrefsManager prefsManager;
    private String appId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        prefsManager = new SharedPrefsManager(this);
        appId = prefsManager.getAppId();
        backendApi = new BackendApi(appId, null); // No token initially for login

        editTextEmail = (EditText) findViewById(R.id.editTextEmail);
        editTextPassword = (EditText) findViewById(R.id.editTextPassword);
        buttonLogin = (Button) findViewById(R.id.buttonLogin);
        textViewRegister = (TextView) findViewById(R.id.textViewRegister);

        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginUser();
            }
        });

        textViewRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
                finish();
            }
        });

        // If already logged in, redirect to MainActivity
        if (prefsManager.isLoggedIn()) {
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    }

    private void loginUser() {
        final String email = editTextEmail.getText().toString().trim();
        final String password = editTextPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email and password.", Toast.LENGTH_SHORT).show();
            return;
        }

        backendApi.login(email, password, new BackendApi.BackendApiCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    Log.d(TAG, "Login success: " + response);
                    Gson gson = new Gson();
                    Type responseType = new TypeToken<AuthResponse>() {}.getType();
                    AuthResponse authResponse = gson.fromJson(response, responseType);

                    if (authResponse != null && authResponse.success && authResponse.token != null) {
                        prefsManager.saveAuthToken(authResponse.token);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                startActivity(intent);
                                finish();
                            }
                        });
                    } else {
                        Log.e(TAG, "API Response indicates failure or missing token: " + response);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(LoginActivity.this, "Login failed: " + (authResponse != null ? authResponse.message : "Missing token"), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing login response: " + e.getMessage(), e);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(LoginActivity.this, "Error parsing login response.", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onError(final String error) {
                Log.e(TAG, "Login error: " + error);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(LoginActivity.this, "Login failed: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private static class AuthResponse {
        boolean success;
        String message;
        String token;
    }
}