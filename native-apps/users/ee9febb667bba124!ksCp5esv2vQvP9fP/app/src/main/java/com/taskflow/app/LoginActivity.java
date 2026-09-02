package com.taskflow.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class LoginActivity extends Activity {

    private EditText etUsername, etPassword;
    private Button btnSubmit;
    private TextView tvRegisterLink;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        dbHelper = new DatabaseHelper(this);
        etUsername = (EditText) findViewById(R.id.etLoginUsername);
        etPassword = (EditText) findViewById(R.id.etLoginPassword);
        btnSubmit = (Button) findViewById(R.id.btnLoginSubmit);
        tvRegisterLink = (TextView) findViewById(R.id.tvLoginRegisterLink);

        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String userStr = etUsername.getText().toString().trim();
                String passStr = etPassword.getText().toString().trim();

                if (userStr.isEmpty() || passStr.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "Please fill all mandatory user credentials", Toast.LENGTH_SHORT).show();
                    return;
                }

                int loggedId = dbHelper.checkLogin(userStr, passStr);
                if (loggedId != -1) {
                    SharedPreferences prefs = getSharedPreferences("taskflow_session", MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putInt("logged_user_id", loggedId);
                    editor.apply();

                    dbHelper.logActivity(loggedId, "Login Session started", "User with nickname " + userStr + " successfully connected.");
                    Toast.makeText(LoginActivity.this, "Successfully Connected! Welcome.", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(LoginActivity.this, "Wrong username or key pairing. Verify and repeat!", Toast.LENGTH_LONG).show();
                }
            }
        });

        tvRegisterLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });
    }
}