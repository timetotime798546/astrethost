package com.expenseconverterapp.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

public class LoginActivity extends Activity {

    private EditText etEmail, etPassword;
    private Button btnLogin, btnRegister;
    private TextView tvForgotPassword;
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

        etEmail = (EditText) findViewById(R.id.etEmail);
        etPassword = (EditText) findViewById(R.id.etPassword);
        btnLogin = (Button) findViewById(R.id.btnLogin);
        btnRegister = (Button) findViewById(R.id.btnRegister);
        tvForgotPassword = (TextView) findViewById(R.id.tvForgotPassword);

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

        tvForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showForgotPasswordDialog();
            }
        });
    }

    private void showForgotPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Reset Password");

        final EditText inputEmail = new EditText(this);
        inputEmail.setHint("Enter your registered email");
        inputEmail.setText(etEmail.getText().toString().trim());
        builder.setView(inputEmail);

        builder.setPositiveButton("Send OTP", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String email = inputEmail.getText().toString().trim();
                if (email.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "Email is required", Toast.LENGTH_SHORT).show();
                    return;
                }
                new RequestOtpTask().execute(email);
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showEnterOtpDialog(final String email) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter OTP & New Password");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);

        final EditText inputOtp = new EditText(this);
        inputOtp.setHint("6-digit OTP");
        layout.addView(inputOtp);

        final EditText inputNewPass = new EditText(this);
        inputNewPass.setHint("New Password");
        layout.addView(inputNewPass);

        builder.setView(layout);

        builder.setPositiveButton("Reset Password", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String otp = inputOtp.getText().toString().trim();
                String newPass = inputNewPass.getText().toString().trim();
                if (otp.isEmpty() || newPass.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "OTP and new password are required", Toast.LENGTH_SHORT).show();
                    return;
                }
                new ResetPasswordTask().execute(email, otp, newPass);
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private class RequestOtpTask extends AsyncTask<String, Void, JSONObject> {
        private String targetEmail;

        @Override
        protected JSONObject doInBackground(String... params) {
            targetEmail = params[0];
            return backendApi.requestOtp(targetEmail);
        }

        @Override
        protected void onPostExecute(JSONObject result) {
            if (result != null && result.optBoolean("success", false)) {
                Toast.makeText(LoginActivity.this, "OTP sent to your email", Toast.LENGTH_LONG).show();
                showEnterOtpDialog(targetEmail);
            } else {
                String err = result != null ? result.optString("error", "Failed to send OTP") : "Network error";
                Toast.makeText(LoginActivity.this, err, Toast.LENGTH_LONG).show();
            }
        }
    }

    private class ResetPasswordTask extends AsyncTask<String, Void, JSONObject> {
        @Override
        protected JSONObject doInBackground(String... params) {
            return backendApi.resetPassword(params[0], params[1], params[2]);
        }

        @Override
        protected void onPostExecute(JSONObject result) {
            if (result != null && result.optBoolean("success", false)) {
                Toast.makeText(LoginActivity.this, "Password reset successfully! Please login.", Toast.LENGTH_LONG).show();
            } else {
                String err = result != null ? result.optString("error", "Reset failed") : "Network error";
                Toast.makeText(LoginActivity.this, err, Toast.LENGTH_LONG).show();
            }
        }
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