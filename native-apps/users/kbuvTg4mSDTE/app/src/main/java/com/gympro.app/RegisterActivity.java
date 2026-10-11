package com.gympro.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONObject;

public class RegisterActivity extends Activity {
    private EditText etEmail, etPassword, etConfirm;
    private Button btnRegister;
    private TextView tvBackToLogin, tvStatus;
    private ProgressBar pbReg;
    private BackendApi api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        api = new BackendApi(this);

        etEmail = findViewById(R.id.etRegEmail);
        etPassword = findViewById(R.id.etRegPassword);
        etConfirm = findViewById(R.id.etRegConfirm);
        btnRegister = findViewById(R.id.btnRegister);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);
        tvStatus = findViewById(R.id.tvRegStatus);
        pbReg = findViewById(R.id.pbReg);

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performRegister();
            }
        });

        tvBackToLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void performRegister() {
        final String email = etEmail.getText().toString().trim();
        final String password = etPassword.getText().toString().trim();
        String confirm = etConfirm.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Please complete all fields");
            return;
        }

        if (!password.equals(confirm)) {
            showError("Passwords do not match");
            return;
        }

        pbReg.setVisibility(View.VISIBLE);
        btnRegister.setEnabled(false);
        tvStatus.setVisibility(View.GONE);

        api.register(email, password, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                api.login(email, password, new BackendApi.ApiCallback() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        pbReg.setVisibility(View.GONE);
                        btnRegister.setEnabled(true);
                        finish();
                    }

                    @Override
                    public void onError(String errorMessage) {
                        pbReg.setVisibility(View.GONE);
                        btnRegister.setEnabled(true);
                        showError("Account created! Please sign in.");
                    }
                });
            }

            @Override
            public void onError(String errorMessage) {
                pbReg.setVisibility(View.GONE);
                btnRegister.setEnabled(true);
                showError(errorMessage);
            }
        });
    }

    private void showError(String msg) {
        tvStatus.setText(msg);
        tvStatus.setVisibility(View.VISIBLE);
    }
}