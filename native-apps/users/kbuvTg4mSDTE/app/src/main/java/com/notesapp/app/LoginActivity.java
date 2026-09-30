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

public class LoginActivity extends android.app.Activity {

    private static final String TAG = "LoginActivity";

    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private TextView registerLinkTextView;
    private BackendApi backendApi;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mainHandler = new Handler(Looper.getMainLooper());
        backendApi = new BackendApi(this);

        emailEditText = (EditText) findViewById(R.id.loginEmailEditText);
        passwordEditText = (EditText) findViewById(R.id.loginPasswordEditText);
        loginButton = (Button) findViewById(R.id.loginButton);
        registerLinkTextView = (TextView) findViewById(R.id.registerLinkTextView);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptLogin();
            }
        });

        registerLinkTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });

        // If already logged in, go to main activity
        if (backendApi.isLoggedIn()) {
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    }

    private void attemptLogin() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter both email and password.", Toast.LENGTH_SHORT).show();
            return;
        }

        backendApi.login(email, password, new BackendApi.BackendApiCallback() {
            @Override
            public void onSuccess(final JSONObject response) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    }
                });
            }

            @Override
            public void onError(final String error) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Log.e(TAG, "Login failed: " + error);
                        Toast.makeText(LoginActivity.this, "Login failed: " + error, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }
}