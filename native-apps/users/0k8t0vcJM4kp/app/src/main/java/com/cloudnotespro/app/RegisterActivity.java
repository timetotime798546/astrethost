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

public class RegisterActivity extends Activity {
    private EditText regEmail, regPassword;
    private Button btnRegister;
    private TextView btnBackToLogin;
    private BackendApi backendApi;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        backendApi = new BackendApi(this);

        regEmail = (EditText) findViewById(R.id.regEmail);
        regPassword = (EditText) findViewById(R.id.regPassword);
        btnRegister = (Button) findViewById(R.id.btnRegister);
        btnBackToLogin = (TextView) findViewById(R.id.btnBackToLogin);

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SoundHelper.playClick(RegisterActivity.this);
                performRegistration();
            }
        });

        btnBackToLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SoundHelper.playClick(RegisterActivity.this);
                finish();
            }
        });
    }

    private void performRegistration() {
        final String email = regEmail.getText().toString().trim();
        final String password = regPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            SoundHelper.playError(RegisterActivity.this);
            Toast.makeText(this, "Fields cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            SoundHelper.playError(RegisterActivity.this);
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog = ProgressDialog.show(this, "Registration", "Provisioning workspace...", true);

        backendApi.register(email, password, new BackendApi.ApiCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.setMessage("Initializing secure vault...");
                    }
                });

                // Authenticate automatically on successful sign up
                backendApi.login(email, password, new BackendApi.ApiCallback<String>() {
                    @Override
                    public void onSuccess(final String token) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progressDialog.dismiss();
                                SoundHelper.playSuccess(RegisterActivity.this);
                                SharedPreferences.Editor editor = getSharedPreferences("CloudNotesPrefs", MODE_PRIVATE).edit();
                                editor.putString("auth_token", token);
                                editor.putString("user_email", email);
                                editor.apply();

                                Toast.makeText(RegisterActivity.this, "Welcome! Registration completed.", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
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
                                SoundHelper.playError(RegisterActivity.this);
                                Toast.makeText(RegisterActivity.this, "Registration Succeeded, manual login required: " + errorMessage, Toast.LENGTH_LONG).show();
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
                        progressDialog.dismiss();
                        SoundHelper.playError(RegisterActivity.this);
                        Toast.makeText(RegisterActivity.this, "Registration Error: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }
}