package com.grandstayhotelresort.app;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONObject;

public class RegisterActivity extends Activity {
    private EditText etRegEmail, etRegPassword, etRegPasswordConfirm;
    private Button btnRegister;
    private TextView tvBackToLogin;
    private BackendApi api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        api = new BackendApi(this);

        etRegEmail = (EditText) findViewById(R.id.etRegEmail);
        etRegPassword = (EditText) findViewById(R.id.etRegPassword);
        etRegPasswordConfirm = (EditText) findViewById(R.id.etRegPasswordConfirm);
        btnRegister = (Button) findViewById(R.id.btnRegister);
        tvBackToLogin = (TextView) findViewById(R.id.tvBackToLogin);

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptRegister();
            }
        });

        tvBackToLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void attemptRegister() {
        final String email = etRegEmail.getText().toString().trim();
        final String password = etRegPassword.getText().toString().trim();
        String confirm = etRegPasswordConfirm.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            Toast.makeText(this, "All credentials fields are mandatory.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirm)) {
            Toast.makeText(this, "Passwords do not match.", Toast.LENGTH_SHORT).show();
            return;
        }

        final ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Initiating guest record...");
        pd.setCancelable(false);
        pd.show();

        api.register(email, password, new BackendApi.ApiCallback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject result) {
                // Register does not contain token, proceed to automated login.
                api.login(email, password, new BackendApi.ApiCallback<String>() {
                    @Override
                    public void onSuccess(String token) {
                        pd.dismiss();
                        Toast.makeText(RegisterActivity.this, "Welcome to GrandStay Resort!", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    }

                    @Override
                    public void onError(String error) {
                        pd.dismiss();
                        Toast.makeText(RegisterActivity.this, "Account verified. Please sign in.", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
            }

            @Override
            public void onError(String error) {
                pd.dismiss();
                Toast.makeText(RegisterActivity.this, error, Toast.LENGTH_LONG).show();
            }
        });
    }
}