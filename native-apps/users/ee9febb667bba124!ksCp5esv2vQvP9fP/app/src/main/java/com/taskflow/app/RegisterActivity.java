package com.taskflow.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class RegisterActivity extends Activity {

    private EditText etUser, etEmail, etPhone, etPass;
    private Button btnSubmit;
    private TextView tvLoginLink;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        dbHelper = new DatabaseHelper(this);

        etUser = (EditText) findViewById(R.id.etRegUsername);
        etEmail = (EditText) findViewById(R.id.etRegEmail);
        etPhone = (EditText) findViewById(R.id.etRegPhone);
        etPass = (EditText) findViewById(R.id.etRegPassword);
        btnSubmit = (Button) findViewById(R.id.btnRegSubmit);
        tvLoginLink = (TextView) findViewById(R.id.tvRegLoginLink);

        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String u = etUser.getText().toString().trim();
                String em = etEmail.getText().toString().trim();
                String ph = etPhone.getText().toString().trim();
                String pa = etPass.getText().toString().trim();

                if (u.isEmpty() || em.isEmpty() || ph.isEmpty() || pa.isEmpty()) {
                    Toast.makeText(RegisterActivity.this, "Please fill all validation entries!", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (pa.length() < 4) {
                    Toast.makeText(RegisterActivity.this, "Password criteria error: Minimum 4 characters", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    boolean isOk = dbHelper.registerUser(u, em, ph, pa);
                    if (isOk) {
                        Toast.makeText(RegisterActivity.this, "User successfully created. Please sign in!", Toast.LENGTH_LONG).show();
                        Intent loginInt = new Intent(RegisterActivity.this, LoginActivity.class);
                        startActivity(loginInt);
                        finish();
                    } else {
                        Toast.makeText(RegisterActivity.this, "Error: Nickname already exists! Choose another nickname.", Toast.LENGTH_LONG).show();
                    }
                } catch (Exception err) {
                    Toast.makeText(RegisterActivity.this, "Db constraint issue: unique username is violation.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        tvLoginLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}