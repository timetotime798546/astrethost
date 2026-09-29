package com.cloudsyncvault.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import org.json.JSONObject;

public class LoginActivity extends Activity {
    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_login);
        
        findViewById(R.id.btnLogin).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                login();
            }
        });
    }

    private void login() {
        final String email = ((EditText)findViewById(R.id.etEmail)).getText().toString();
        final String pass = ((EditText)findViewById(R.id.etPass)).getText().toString();
        new Thread(new Runnable() {
            public void run() {
                try {
                    BackendApi api = new BackendApi(LoginActivity.this);
                    JSONObject body = new JSONObject();
                    body.put("app_id", "com.cloudsyncvault.app");
                    body.put("email", email);
                    body.put("password", pass);
                    JSONObject res = api.request("POST", "/login", body, false);
                    if (res.has("token")) {
                        api.setToken(res.getString("token"));
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish();
                    }
                } catch (Exception e) {
                    runOnUiThread(() -> Toast.makeText(LoginActivity.this, "Login failed", Toast.LENGTH_SHORT).show());
                }
            }
        }).start();
    }
}