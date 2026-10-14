package com.expenseconverterapp.app;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONObject;

public class LoginActivity extends Activity {

    private EditText etEmail, etPassword;
    private Button btnLogin, btnRegister;
    private BackendApi backendApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        backendApi = new BackendApi(this);

        if (backendApi.getToken() != null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = etEmail.getText().toString().trim();
                String pass = etPassword.getText().toString().trim();
                if (email.isEmpty() || pass.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "Please enter email and password", Toast.LENGTH_SHORT).show();
                    return;
                }
                new LoginTask().execute(email, pass);
            }
        });

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = etEmail.getText().toString().trim();
                String pass = etPassword.getText().toString().trim();
                if (email.isEmpty() || pass.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "Please enter email and password", Toast.LENGTH_SHORT).show();
                    return;
                }
                new RegisterTask().execute(email, pass);
            }
        });
    }

    private class LoginTask extends AsyncTask<String, Void, JSONObject> {
        @Override
        protected JSONObject doInBackground(String... params) {
            return backendApi.login(params[0], params[1]);
        }

        @Override
        protected void onPostExecute(JSONObject result) {
            if (result != null && result.optBoolean("success", false)) {
                Toast.makeText(LoginActivity.this, "Login Successful", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                finish();
            } else {
                String err = result != null ? result.optString("error", "Login failed") : "Network error";
                Toast.makeText(LoginActivity.this, err, Toast.LENGTH_LONG).show();
            }
        }
    }

    private class RegisterTask extends AsyncTask<String, Void, JSONObject> {
        private String emailStr;
        private String passStr;

        @Override
        protected JSONObject doInBackground(String... params) {
            emailStr = params[0];
            passStr = params[1];
            return backendApi.register(emailStr, passStr);
        }

        @Override
        protected void onPostExecute(JSONObject result) {
            if (result != null && result.optBoolean("success", false)) {
                Toast.makeText(LoginActivity.this, "Registration successful! Logging in...", Toast.LENGTH_SHORT).show();
                new LoginTask().execute(emailStr, passStr);
            } else {
                String err = result != null ? result.optString("error", "Registration failed") : "Network error";
                Toast.makeText(LoginActivity.this, err, Toast.LENGTH_LONG).show();
            }
        }
    }
}