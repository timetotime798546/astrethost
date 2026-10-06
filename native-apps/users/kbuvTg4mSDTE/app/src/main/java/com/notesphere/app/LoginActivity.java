package com.notesphere.app;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class LoginActivity extends Activity {

    private EditText editEmail;
    private EditText editPassword;
    private Button btnLogin;
    private TextView textGotoRegister;
    private TextView textForgotPassword;
    private BackendApi api;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if user session already exists
        if (NoteStorage.getToken(this) != null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        editEmail = (EditText) findViewById(R.id.edit_login_email);
        editPassword = (EditText) findViewById(R.id.edit_login_password);
        btnLogin = (Button) findViewById(R.id.button_login);
        textGotoRegister = (TextView) findViewById(R.id.text_goto_register);
        textForgotPassword = (TextView) findViewById(R.id.text_forgot_password);

        api = new BackendApi(this);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Logging in...");
        progressDialog.setCancelable(false);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performLogin();
            }
        });

        textGotoRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            }
        });

        textForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class));
            }
        });
    }

    private void performLogin() {
        String email = editEmail.getText().toString().trim();
        String password = editPassword.getText().toString().trim();

        if (email.isEmpty()) {
            Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.isEmpty()) {
            Toast.makeText(this, "Please enter your password", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.show();
        api.login(email, password, new ApiCallback<String>() {
            @Override
            public void onSuccess(String token) {
                progressDialog.dismiss();
                Toast.makeText(LoginActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                finish();
            }

            @Override
            public void onError(String error) {
                progressDialog.dismiss();
                Toast.makeText(LoginActivity.this, "Login Error: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }
}