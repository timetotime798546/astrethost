package com.pahadirunner.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import org.json.JSONObject;

public class ForgotPasswordActivity extends Activity {

    private EditText etForgotEmail, etOtp, etNewPassword;
    private Button btnSendOtp, btnResetPassword;
    private ProgressBar pbForgot;
    private BackendApi backendApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        backendApi = new BackendApi(this);

        etForgotEmail = (EditText) findViewById(R.id.etForgotEmail);
        etOtp = (EditText) findViewById(R.id.etOtp);
        etNewPassword = (EditText) findViewById(R.id.etNewPassword);
        btnSendOtp = (Button) findViewById(R.id.btnSendOtp);
        btnResetPassword = (Button) findViewById(R.id.btnResetPassword);
        pbForgot = (ProgressBar) findViewById(R.id.pbForgot);

        btnSendOtp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendOtp();
            }
        });

        btnResetPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetPasswordWithOtp();
            }
        });
    }

    private void sendOtp() {
        String email = etForgotEmail.getText().toString().trim();
        if (email.isEmpty()) {
            Toast.makeText(this, "कृपया ईमेल दर्ज करें", Toast.LENGTH_SHORT).show();
            return;
        }

        pbForgot.setVisibility(View.VISIBLE);
        btnSendOtp.setEnabled(false);

        backendApi.requestOtp(email, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pbForgot.setVisibility(View.GONE);
                        btnSendOtp.setEnabled(true);
                        Toast.makeText(ForgotPasswordActivity.this, "OTP आपके ईमेल पर भेज दिया गया है!", Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onError(final String error) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pbForgot.setVisibility(View.GONE);
                        btnSendOtp.setEnabled(true);
                        Toast.makeText(ForgotPasswordActivity.this, "त्रुटि: " + error, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private void resetPasswordWithOtp() {
        String email = etForgotEmail.getText().toString().trim();
        String otp = etOtp.getText().toString().trim();
        String newPass = etNewPassword.getText().toString().trim();

        if (email.isEmpty() || otp.isEmpty() || newPass.isEmpty()) {
            Toast.makeText(this, "कृपया सभी फ़ील्ड भरें", Toast.LENGTH_SHORT).show();
            return;
        }

        pbForgot.setVisibility(View.VISIBLE);
        btnResetPassword.setEnabled(false);

        backendApi.resetPassword(email, otp, newPass, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pbForgot.setVisibility(View.GONE);
                        btnResetPassword.setEnabled(true);
                        Toast.makeText(ForgotPasswordActivity.this, "पासवर्ड सफलतापूर्वक बदला गया! कृपया लॉगिन करें।", Toast.LENGTH_LONG).show();
                        finish();
                    }
                });
            }

            @Override
            public void onError(final String error) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pbForgot.setVisibility(View.GONE);
                        btnResetPassword.setEnabled(true);
                        Toast.makeText(ForgotPasswordActivity.this, "त्रुटि: " + error, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }
}