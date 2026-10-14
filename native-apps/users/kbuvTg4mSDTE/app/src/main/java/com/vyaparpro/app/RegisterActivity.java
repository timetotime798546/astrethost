package com.vyaparpro.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONObject;

public class RegisterActivity extends Activity {

    private EditText etEmail;
    private EditText etPassword;
    private Button btnRegister;
    private ProgressBar progressBar;
    private TextView tvStatus;
    private BackendApi api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        api = new BackendApi(this);

        etEmail = (EditText) findViewById(R.id.etRegisterEmail);
        etPassword = (EditText) findViewById(R.id.etRegisterPassword);
        btnRegister = (Button) findViewById(R.id.btnSubmitRegister);
        progressBar = (ProgressBar) findViewById(R.id.progressBarRegister);
        tvStatus = (TextView) findViewById(R.id.tvRegisterStatus);

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performRegister();
            }
        });

        findViewById(R.id.btnBackToLogin).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void performRegister() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.length() < 6) {
            tvStatus.setText("Provide valid email & minimum 6 character password.");
            tvStatus.setVisibility(View.VISIBLE);
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnRegister.setEnabled(false);
        tvStatus.setVisibility(View.GONE);

        api.register(email, password, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                progressBar.setVisibility(View.GONE);
                btnRegister.setEnabled(true);
                // Registered and automatically logged in via BackendApi
                Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(String errorMessage) {
                progressBar.setVisibility(View.GONE);
                btnRegister.setEnabled(true);
                tvStatus.setText(errorMessage);
                tvStatus.setVisibility(View.VISIBLE);
            }
        });
    }
}