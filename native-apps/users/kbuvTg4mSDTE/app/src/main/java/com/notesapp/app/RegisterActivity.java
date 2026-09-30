package com.notesapp.app;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

public class RegisterActivity extends android.app.Activity {

    private static final String TAG = "RegisterActivity";

    private EditText emailEditText;
    private EditText passwordEditText;
    private EditText confirmPasswordEditText;
    private Button registerButton;
    private TextView loginLinkTextView;
    private BackendApi backendApi;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mainHandler = new Handler(Looper.getMainLooper());
        backendApi = new BackendApi(this);

        emailEditText = (EditText) findViewById(R.id.registerEmailEditText);
        passwordEditText = (EditText) findViewById(R.id.registerPasswordEditText);
        confirmPasswordEditText = (EditText) findViewById(R.id.registerConfirmPasswordEditText);
        registerButton = (Button) findViewById(R.id.registerButton);
        loginLinkTextView = (TextView) findViewById(R.id.loginLinkTextView);

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptRegister();
            }
        });

        loginLinkTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Go back to LoginActivity
            }
        });
    }

    private void attemptRegister() {
        final String email = emailEditText.getText().toString().trim();
        final String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "All fields are required.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match.", Toast.LENGTH_SHORT).show();
            return;
        }

        backendApi.register(email, password, new BackendApi.BackendApiCallback() {
            @Override
            public void onSuccess(final JSONObject response) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(RegisterActivity.this, "Registration successful! Logging in...", Toast.LENGTH_SHORT).show();
                        // Automatically log in after successful registration
                        backendApi.login(email, password, new BackendApi.BackendApiCallback() {
                            @Override
                            public void onSuccess(JSONObject loginResponse) {
                                mainHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(RegisterActivity.this, "Logged in automatically.", Toast.LENGTH_SHORT).show();
                                        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                                        startActivity(intent);
                                        finish();
                                    }
                                });
                            }

                            @Override
                            public void onError(String loginError) {
                                mainHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Log.e(TAG, "Automatic login failed: " + loginError);
                                        Toast.makeText(RegisterActivity.this, "Registration successful, but automatic login failed: " + loginError, Toast.LENGTH_LONG).show();
                                        finish(); // Go back to login screen
                                    }
                                });
                            }
                        });
                    }
                });
            }

            @Override
            public void onError(final String error) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Log.e(TAG, "Registration failed: " + error);
                        Toast.makeText(RegisterActivity.this, "Registration failed: " + error, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }
}