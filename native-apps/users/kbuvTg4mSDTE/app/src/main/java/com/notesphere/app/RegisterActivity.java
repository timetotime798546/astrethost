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

public class RegisterActivity extends Activity {

    private EditText editEmail;
    private EditText editPassword;
    private Button btnRegister;
    private TextView textGotoLogin;
    private BackendApi api;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        editEmail = (EditText) findViewById(R.id.edit_register_email);
        editPassword = (EditText) findViewById(R.id.edit_register_password);
        btnRegister = (Button) findViewById(R.id.button_register);
        textGotoLogin = (TextView) findViewById(R.id.text_goto_login);

        api = new BackendApi(this);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Creating account...");
        progressDialog.setCancelable(false);

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performRegister();
            }
        });

        textGotoLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void performRegister() {
        final String email = editEmail.getText().toString().trim();
        final String password = editPassword.getText().toString().trim();

        if (email.isEmpty()) {
            Toast.makeText(this, "Please enter email", Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.isEmpty()) {
            Toast.makeText(this, "Please enter password", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.show();
        api.register(email, password, new ApiCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                progressDialog.setMessage("Logging in...");
                api.login(email, password, new ApiCallback<String>() {
                    @Override
                    public void onSuccess(String token) {
                        progressDialog.dismiss();
                        Toast.makeText(RegisterActivity.this, "Account created & logged in", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onError(String error) {
                        progressDialog.dismiss();
                        Toast.makeText(RegisterActivity.this, "Registration Succeeded, please log in manually", Toast.LENGTH_LONG).show();
                        finish();
                    }
                });
            }

            @Override
            public void onError(String error) {
                progressDialog.dismiss();
                Toast.makeText(RegisterActivity.this, "Registration Error: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }
}