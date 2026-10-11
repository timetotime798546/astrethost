package com.gympro.app;

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

    private EditText etRegEmail;
    private EditText etRegPassword;
    private EditText etRegConfirm;
    private ProgressBar pbReg;
    private TextView tvRegStatus;
    private Button btnRegister;
    private TextView tvBackToLogin;

    private BackendApi backendApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        backendApi = new BackendApi(this);

        etRegEmail = (EditText) findViewById(R.id.etRegEmail);
        etRegPassword = (EditText) findViewById(R.id.etRegPassword);
        etRegConfirm = (EditText) findViewById(R.id.etRegConfirm);
        pbReg = (ProgressBar) findViewById(R.id.pbReg);
        tvRegStatus = (TextView) findViewById(R.id.tvRegStatus);
        btnRegister = (Button) findViewById(R.id.btnRegister);
        tvBackToLogin = (TextView) findViewById(R.id.tvBackToLogin);

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performRegistration();
            }
        });

        tvBackToLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void performRegistration() {
        final String email = etRegEmail.getText().toString().trim();
        final String password = etRegPassword.getText().toString().trim();
        String confirm = etRegConfirm.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            tvRegStatus.setText("Please enter all details");
            tvRegStatus.setVisibility(View.VISIBLE);
            return;
        }

        if (!password.equals(confirm)) {
            tvRegStatus.setText("Passwords do not match");
            tvRegStatus.setVisibility(View.VISIBLE);
            return;
        }

        if (password.length() < 6) {
            tvRegStatus.setText("Password must be at least 6 characters");
            tvRegStatus.setVisibility(View.VISIBLE);
            return;
        }

        tvRegStatus.setVisibility(View.GONE);
        pbReg.setVisibility(View.VISIBLE);
        btnRegister.setEnabled(false);

        backendApi.register(email, password, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                // Register succeeds without token -> call login to obtain session token
                backendApi.login(email, password, new BackendApi.ApiCallback() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        pbReg.setVisibility(View.GONE);
                        btnRegister.setEnabled(true);
                        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onError(String errorMessage) {
                        pbReg.setVisibility(View.GONE);
                        btnRegister.setEnabled(true);
                        tvRegStatus.setText("Registered, please sign in manually");
                        tvRegStatus.setVisibility(View.VISIBLE);
                    }
                });
            }

            @Override
            public void onError(String errorMessage) {
                pbReg.setVisibility(View.GONE);
                btnRegister.setEnabled(true);
                tvRegStatus.setText(errorMessage);
                tvRegStatus.setVisibility(View.VISIBLE);
            }
        });
    }
}