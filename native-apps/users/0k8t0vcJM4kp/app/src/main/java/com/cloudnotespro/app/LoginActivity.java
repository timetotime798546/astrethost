package com.cloudnotespro.app;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class LoginActivity extends Activity {
    private EditText editEmail, editPassword;
    private Button btnLogin;
    private TextView btnForgotPassword, btnGoToRegister;
    private BackendApi backendApi;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences("CloudNotesPrefs", MODE_PRIVATE);
        String token = prefs.getString("auth_token", "");
        if (!token.isEmpty()) {
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        backendApi = new BackendApi(this);

        editEmail = (EditText) findViewById(R.id.editEmail);
        editPassword = (EditText) findViewById(R.id.editPassword);
        btnLogin = (Button) findViewById(R.id.btnLogin);
        btnForgotPassword = (TextView) findViewById(R.id.btnForgotPassword);
        btnGoToRegister = (TextView) findViewById(R.id.btnGoToRegister);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SoundHelper.playClick(LoginActivity.this);
                performLogin();
            }
        });

        btnGoToRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SoundHelper.playClick(LoginActivity.this);
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });

        btnForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SoundHelper.playClick(LoginActivity.this);
                Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
                startActivity(intent);
            }
        });
    }

    private void performLogin() {
        final String email = editEmail.getText().toString().trim();
        final String password = editPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            SoundHelper.playError(LoginActivity.this);
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog = ProgressDialog.show(this, "Authenticating", "Accessing your personal vault...", true);

        backendApi.login(email, password, new BackendApi.ApiCallback<String>() {
            @Override
            public void onSuccess(final String token) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.dismiss();
                        SoundHelper.playSuccess(LoginActivity.this);
                        
                        SharedPreferences.Editor editor = getSharedPreferences("CloudNotesPrefs", MODE_PRIVATE).edit();
                        editor.putString("auth_token", token);
                        editor.putString("user_email", email);
                        editor.apply();

                        Toast.makeText(LoginActivity.this, "Successfully Logged In!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    }
                });
            }

            @Override
            public void onError(final String errorMessage) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.dismiss();
                        SoundHelper.playError(LoginActivity.this);
                        Toast.makeText(LoginActivity.this, "Authentication Failed: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }
}