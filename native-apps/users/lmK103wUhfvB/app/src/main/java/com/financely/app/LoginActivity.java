package com.financely.app;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
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
    private Button btnLogin;
    private TextView tvForgotPassword, tvSignUp;
    private BackendApi api;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        api = new BackendApi(this);

        etEmail = (EditText) findViewById(R.id.et_email);
        etPassword = (EditText) findViewById(R.id.et_password);
        btnLogin = (Button) findViewById(R.id.btn_login);
        tvForgotPassword = (TextView) findViewById(R.id.tv_forgot_password);
        tvSignUp = (TextView) findViewById(R.id.tv_sign_up);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Logging in...");
        progressDialog.setCancelable(false);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptLogin();
            }
        });

        tvSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(i);
            }
        });

        tvForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
                startActivity(i);
            }
        });
    }

    private void attemptLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.show();
        api.login(email, password, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                progressDialog.dismiss();
                try {
                    boolean success = response.optBoolean("success", false);
                    if (success) {
                        String token = response.optString("token", "");
                        if (!token.isEmpty()) {
                            SharedPreferences.Editor editor = getSharedPreferences("financely_prefs", Context.MODE_PRIVATE).edit();
                            editor.putString("token", token);
                            editor.putString("email", email);
                            editor.apply();

                            Toast.makeText(LoginActivity.this, "Welcome to Financely!", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(LoginActivity.this, "Error parsing server authorization key.", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        String msg = response.optString("message", "Authentication failed");
                        Toast.makeText(LoginActivity.this, msg, Toast.LENGTH_LONG).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(LoginActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onError(String errorMessage) {
                progressDialog.dismiss();
                Toast.makeText(LoginActivity.this, "Login Failed: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }
}