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

import java.lang.reflect.Type;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends Activity {

    private static final String TAG = "RegisterActivity";
    private EditText editTextEmail, editTextPassword;
    private Button buttonRegister;
    private TextView textViewLogin;
    private BackendApi backendApi;
    private SharedPrefsManager prefsManager;
    private String appId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        prefsManager = new SharedPrefsManager(this);
        appId = prefsManager.getAppId();
        backendApi = new BackendApi(appId, null); // No token initially for registration

        editTextEmail = (EditText) findViewById(R.id.editTextEmail);
        editTextPassword = (EditText) findViewById(R.id.editTextPassword);
        buttonRegister = (Button) findViewById(R.id.buttonRegister);
        textViewLogin = (TextView) findViewById(R.id.textViewLogin);

        buttonRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerUser();
            }
        });

        textViewLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    private void registerUser() {
        final String email = editTextEmail.getText().toString().trim();
        final String password = editTextPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email and password.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, String> params = new HashMap<String, String>();
        params.put("email", email);
        params.put("password", password);

        backendApi.register(email, password, new BackendApi.BackendApiCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    Log.d(TAG, "Register success: " + response);
                    Gson gson = new Gson();
                    Type responseType = new TypeToken<AuthResponse>() {}.getType();
                    AuthResponse authResponse = gson.fromJson(response, responseType);

                    if (authResponse != null && authResponse.success) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(RegisterActivity.this, "Registration successful!", Toast.LENGTH_SHORT).show();
                                // Automatically log in after successful registration
                                loginUserAfterRegistration(email, password);
                            }
                        });
                    } else {
                        Log.e(TAG, "API Response indicates failure: " + response);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(RegisterActivity.this, "Registration failed: " + (authResponse != null ? authResponse.message : "Unknown error"), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing registration response: " + e.getMessage(), e);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(RegisterActivity.this, "Error parsing registration response.", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onError(final String error) {
                Log.e(TAG, "Registration error: " + error);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(RegisterActivity.this, "Registration failed: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void loginUserAfterRegistration(String email, String password) {
        backendApi.login(email, password, new BackendApi.BackendApiCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    Log.d(TAG, "Login after registration success: " + response);
                    Gson gson = new Gson();
                    Type responseType = new TypeToken<AuthResponse>() {}.getType();
                    AuthResponse authResponse = gson.fromJson(response, responseType);

                    if (authResponse != null && authResponse.success && authResponse.token != null) {
                        prefsManager.saveAuthToken(authResponse.token);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(RegisterActivity.this, "Logged in automatically.", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                                startActivity(intent);
                                finish();
                            }
                        });
                    } else {
                        Log.e(TAG, "API Response indicates failure or missing token after auto-login: " + response);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(RegisterActivity.this, "Auto-login failed: " + (authResponse != null ? authResponse.message : "Missing token"), Toast.LENGTH_SHORT).show();
                                // Redirect to login activity if auto-login fails
                                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                                startActivity(intent);
                                finish();
                            }
                        });
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing auto-login response: " + e.getMessage(), e);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(RegisterActivity.this, "Error during auto-login.", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                            startActivity(intent);
                            finish();
                        }
                    });
                }
            }

            @Override
            public void onError(final String error) {
                Log.e(TAG, "Auto-login error: " + error);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(RegisterActivity.this, "Auto-login failed: " + error, Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                        startActivity(intent);
                        finish();
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