package com.pahadirunner.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONObject;

public class LoginActivity extends Activity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvForgotPassword, tvGoToRegister;
    private ProgressBar pbLogin;
    private BackendApi backendApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        backendApi = new BackendApi(this);

        etEmail = (EditText) findViewById(R.id.etEmail);
        etPassword = (EditText) findViewById(R.id.etPassword);
        btnLogin = (Button) findViewById(R.id.btnLogin);
        tvForgotPassword = (TextView) findViewById(R.id.tvForgotPassword);
        tvGoToRegister = (TextView) findViewById(R.id.tvGoToRegister);
        pbLogin = (ProgressBar) findViewById(R.id.pbLogin);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performLogin();
            }
        });

        tvForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
                startActivity(intent);
            }
        });

        tvGoToRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    private void performLogin() {
        final String email = etEmail.getText().toString().trim();
        final String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "कृपया ईमेल और पासवर्ड भरें", Toast.LENGTH_SHORT).show();
            return;
        }

        pbLogin.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);

        backendApi.login(email, password, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(final JSONObject response) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pbLogin.setVisibility(View.GONE);
                        btnLogin.setEnabled(true);
                        String token = response.optString("token", "");
                        if (!token.isEmpty()) {
                            backendApi.saveToken(token, email);
                            Toast.makeText(LoginActivity.this, "लॉगिन सफल!", Toast.LENGTH_SHORT).show();
                            setResult(RESULT_OK);
                            finish();
                        } else {
                            Toast.makeText(LoginActivity.this, "लॉगिन असफल: टोकन प्राप्त नहीं हुआ", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }

            @Override
            public void onError(final String error) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pbLogin.setVisibility(View.GONE);
                        btnLogin.setEnabled(true);
                        Toast.makeText(LoginActivity.this, "त्रुटि: " + error, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }
}