package com.arbajsbz.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.json.JSONObject;

public class LoginActivity extends Activity {

    private EditText etEmail;
    private EditText etPassword;
    private Button btnLogin;
    private Button btnForgotPassword;
    private Button btnGotoRegister;
    private BackendApi backendApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        backendApi = new BackendApi(this);

        etEmail = (EditText) findViewById(R.id.et_login_email);
        etPassword = (EditText) findViewById(R.id.et_login_password);
        btnLogin = (Button) findViewById(R.id.btn_submit_login);
        btnForgotPassword = (Button) findViewById(R.id.btn_forgot_password);
        btnGotoRegister = (Button) findViewById(R.id.btn_goto_register);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performLogin();
            }
        });

        btnForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showForgotPasswordDialog();
            }
        });

        btnGotoRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            }
        });
    }

    private void performLogin() {
        final String email = etEmail.getText().toString().trim();
        final String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        final ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Authenticating with ArbajSbz server...");
        pd.setCancelable(false);
        pd.show();

        backendApi.login(email, password, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                pd.dismiss();
                Toast.makeText(LoginActivity.this, "Welcome back, Pilot!", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                finish();
            }

            @Override
            public void onError(String message) {
                pd.dismiss();
                Toast.makeText(LoginActivity.this, "Error: " + message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showForgotPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Reset Password (OTP)");

        final EditText inputEmail = new EditText(this);
        inputEmail.setHint("Enter your registered email");
        inputEmail.setText(etEmail.getText().toString().trim());
        builder.setView(inputEmail);

        builder.setPositiveButton("Request OTP", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final String email = inputEmail.getText().toString().trim();
                if (email.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "Please provide an email", Toast.LENGTH_SHORT).show();
                    return;
                }
                requestPasswordOtp(email);
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void requestPasswordOtp(final String email) {
        final ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Sending OTP code...");
        pd.show();

        backendApi.requestOtp(email, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                pd.dismiss();
                Toast.makeText(LoginActivity.this, "OTP sent to your email!", Toast.LENGTH_LONG).show();
                showResetPasswordDialog(email);
            }

            @Override
            public void onError(String message) {
                pd.dismiss();
                Toast.makeText(LoginActivity.this, "OTP Error: " + message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showResetPasswordDialog(final String email) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter OTP & New Password");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 16, 32, 16);

        final EditText inputOtp = new EditText(this);
        inputOtp.setHint("Enter OTP code");
        layout.addView(inputOtp);

        final EditText inputNewPass = new EditText(this);
        inputNewPass.setHint("Enter new password");
        inputNewPass.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(inputNewPass);

        builder.setView(layout);

        builder.setPositiveButton("Reset Password", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String otp = inputOtp.getText().toString().trim();
                String newPass = inputNewPass.getText().toString().trim();

                if (otp.isEmpty() || newPass.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "All fields are required", Toast.LENGTH_SHORT).show();
                    return;
                }

                performReset(email, otp, newPass);
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void performReset(String email, String otp, String newPass) {
        final ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Verifying OTP and resetting password...");
        pd.show();

        backendApi.resetPassword(email, otp, newPass, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                pd.dismiss();
                Toast.makeText(LoginActivity.this, "Password reset successfully! Please login.", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(String message) {
                pd.dismiss();
                Toast.makeText(LoginActivity.this, "Reset Failed: " + message, Toast.LENGTH_LONG).show();
            }
        });
    }
}