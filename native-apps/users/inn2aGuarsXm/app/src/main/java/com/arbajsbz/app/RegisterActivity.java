package com.arbajsbz.app;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONObject;

public class RegisterActivity extends Activity {

    private EditText etEmail;
    private EditText etPassword;
    private Button btnRegister;
    private Button btnBackLogin;
    private BackendApi backendApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        backendApi = new BackendApi(this);

        etEmail = (EditText) findViewById(R.id.et_reg_email);
        etPassword = (EditText) findViewById(R.id.et_reg_password);
        btnRegister = (Button) findViewById(R.id.btn_submit_register);
        btnBackLogin = (Button) findViewById(R.id.btn_back_to_login);

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performRegistration();
            }
        });

        btnBackLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void performRegistration() {
        final String email = etEmail.getText().toString().trim();
        final String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        final ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Creating ArbajSbz account & granting ₹50 bonus...");
        pd.setCancelable(false);
        pd.show();

        // 1. Call Register (does not return token)
        backendApi.register(email, password, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                // 2. Auto-login using exact /login endpoint to extract Bearer token
                backendApi.login(email, password, new BackendApi.ApiCallback() {
                    @Override
                    public void onSuccess(JSONObject loginResp) {
                        // 3. Initialize Wallet with ₹50 bonus
                        try {
                            JSONObject wallet = new JSONObject();
                            wallet.put("email", email);
                            wallet.put("deposited_balance", 0.0);
                            wallet.put("bonus_balance", 50.0);
                            wallet.put("total_winnings", 0.0);
                            wallet.put("is_admin", email.contains("admin"));

                            backendApi.createRecord("wallets", wallet, new BackendApi.ApiCallback() {
                                @Override
                                public void onSuccess(JSONObject resp) {
                                    pd.dismiss();
                                    Toast.makeText(RegisterActivity.this, "Registered! ₹50 Bonus Credited!", Toast.LENGTH_LONG).show();
                                    startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                                    finish();
                                }

                                @Override
                                public void onError(String msg) {
                                    pd.dismiss();
                                    startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                                    finish();
                                }
                            });
                        } catch (Exception e) {
                            pd.dismiss();
                            startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                            finish();
                        }
                    }

                    @Override
                    public void onError(String message) {
                        pd.dismiss();
                        Toast.makeText(RegisterActivity.this, "Registered. Please login.", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
            }

            @Override
            public void onError(String message) {
                pd.dismiss();
                Toast.makeText(RegisterActivity.this, "Registration failed: " + message, Toast.LENGTH_LONG).show();
            }
        });
    }
}