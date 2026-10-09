package com.grandstayhotelresort.app;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONObject;

public class ForgotPasswordActivity extends Activity {
    private EditText etForgotEmail, etOtpCode, etNewPassword;
    private Button btnSendOtp, btnResetPassword;
    private LinearLayout layoutReset;
    private TextView tvBack;
    private BackendApi api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        api = new BackendApi(this);

        etForgotEmail = (EditText) findViewById(R.id.etForgotEmail);
        etOtpCode = (EditText) findViewById(R.id.etOtpCode);
        etNewPassword = (EditText) findViewById(R.id.etNewPassword);
        btnSendOtp = (Button) findViewById(R.id.btnSendOtp);
        btnResetPassword = (Button) findViewById(R.id.btnResetPassword);
        layoutReset = (LinearLayout) findViewById(R.id.layoutReset);
        tvBack = (TextView) findViewById(R.id.tvBack);

        btnSendOtp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestOtpCode();
            }
        });

        btnResetPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performPasswordReset();
            }
        });

        tvBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void requestOtpCode() {
        String email = etForgotEmail.getText().toString().trim();
        if (email.isEmpty()) {
            Toast.makeText(this, "Please enter your email address.", Toast.LENGTH_SHORT).show();
            return;
        }

        final ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Sending code...");
        pd.setCancelable(false);
        pd.show();

        api.requestOtp(email, new BackendApi.ApiCallback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject result) {
                pd.dismiss();
                Toast.makeText(ForgotPasswordActivity.this, "OTP sent successfully!", Toast.LENGTH_SHORT).show();
                layoutReset.setVisibility(View.VISIBLE);
                btnSendOtp.setVisibility(View.GONE);
                etForgotEmail.setEnabled(false);
            }

            @Override
            public void onError(String error) {
                pd.dismiss();
                Toast.makeText(ForgotPasswordActivity.this, error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void performPasswordReset() {
        String email = etForgotEmail.getText().toString().trim();
        String otp = etOtpCode.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();

        if (otp.isEmpty() || newPassword.isEmpty()) {
            Toast.makeText(this, "Complete code and password parameters.", Toast.LENGTH_SHORT).show();
            return;
        }

        final ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Updating credentials...");
        pd.setCancelable(false);
        pd.show();

        api.resetPassword(email, otp, newPassword, new BackendApi.ApiCallback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject result) {
                pd.dismiss();
                Toast.makeText(ForgotPasswordActivity.this, "Password updated. Proceed to Sign In.", Toast.LENGTH_LONG).show();
                finish();
            }

            @Override
            public void onError(String error) {
                pd.dismiss();
                Toast.makeText(ForgotPasswordActivity.this, error, Toast.LENGTH_LONG).show();
            }
        });
    }
}