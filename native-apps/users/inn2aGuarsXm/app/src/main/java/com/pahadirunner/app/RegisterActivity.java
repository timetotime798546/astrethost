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

public class RegisterActivity extends Activity {

    private EditText etRegEmail, etRegPassword;
    private Button btnRegister;
    private TextView tvGoToLogin;
    private ProgressBar pbRegister;
    private BackendApi backendApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        backendApi = new BackendApi(this);

        etRegEmail = (EditText) findViewById(R.id.etRegEmail);
        etRegPassword = (EditText) findViewById(R.id.etRegPassword);
        btnRegister = (Button) findViewById(R.id.btnRegister);
        tvGoToLogin = (TextView) findViewById(R.id.tvGoToLogin);
        pbRegister = (ProgressBar) findViewById(R.id.pbRegister);

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performRegister();
            }
        });

        tvGoToLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    private void performRegister() {
        final String email = etRegEmail.getText().toString().trim();
        final String password = etRegPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "कृपया ईमेल और पासवर्ड भरें", Toast.LENGTH_SHORT).show();
            return;
        }

        pbRegister.setVisibility(View.VISIBLE);
        btnRegister.setEnabled(false);

        // POST /register does NOT return a token.
        // Confirm registration success, then call POST /login to obtain the token.
        backendApi.register(email, password, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(final JSONObject regResponse) {
                // Now perform login with the same credentials
                backendApi.login(email, password, new BackendApi.ApiCallback() {
                    @Override
                    public void onSuccess(final JSONObject loginResponse) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                pbRegister.setVisibility(View.GONE);
                                btnRegister.setEnabled(true);
                                String token = loginResponse.optString("token", "");
                                if (!token.isEmpty()) {
                                    backendApi.saveToken(token, email);
                                    Toast.makeText(RegisterActivity.this, "पंजीकरण सफल! लॉगिन हो गया।", Toast.LENGTH_SHORT).show();
                                    setResult(RESULT_OK);
                                    finish();
                                } else {
                                    Toast.makeText(RegisterActivity.this, "पंजीकरण सफल। कृपया लॉगिन करें।", Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                                    startActivity(intent);
                                    finish();
                                }
                            }
                        });
                    }

                    @Override
                    public void onError(final String error) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                pbRegister.setVisibility(View.GONE);
                                btnRegister.setEnabled(true);
                                Toast.makeText(RegisterActivity.this, "पंजीकरण सफल! कृपया मैन्युअली लॉगिन करें।", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                                startActivity(intent);
                                finish();
                            }
                        });
                    }
                });
            }

            @Override
            public void onError(final String error) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pbRegister.setVisibility(View.GONE);
                        btnRegister.setEnabled(true);
                        Toast.makeText(RegisterActivity.this, "पंजीकरण त्रुटि: " + error, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }
}