package com.shopinventorysync.app;

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
import org.json.JSONObject;

public class LoginActivity extends Activity {

    private EditText etEmail, etPassword;
    private Button btnLogin, btnGoRegister;
    private TextView btnForgotPassword;
    private BackendApi backendApi;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Session check
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        String savedToken = prefs.getString("token", null);
        if (savedToken != null) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        backendApi = new BackendApi(this);

        etEmail = (EditText) findViewById(R.id.et_email);
        etPassword = (EditText) findViewById(R.id.et_password);
        btnLogin = (Button) findViewById(R.id.btn_login);
        btnGoRegister = (Button) findViewById(R.id.btn_go_register);
        btnForgotPassword = (TextView) findViewById(R.id.btn_forgot_password);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Logging in...");
        progressDialog.setCancelable(false);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptLogin();
            }
        });

        btnGoRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            }
        });

        btnForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class));
            }
        });
    }

    private void attemptLogin() {
        final String email = etEmail.getText().toString().trim();
        final String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.show();
        backendApi.login(email, password, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(final JSONObject response) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.dismiss();
                        try {
                            String token = response.getString("token");
                            SharedPreferences.Editor editor = getSharedPreferences("app_prefs", MODE_PRIVATE).edit();
                            editor.putString("token", token);
                            editor.putString("email", email);
                            editor.apply();

                            Toast.makeText(LoginActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finish();
                        } catch (Exception e) {
                            Toast.makeText(LoginActivity.this, "Response parsing error", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }

            @Override
            public void onError(final String error) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.dismiss();
                        Toast.makeText(LoginActivity.this, error, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }
}