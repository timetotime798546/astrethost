package com.saloonbooking.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

public class MainActivity extends Activity {

    private boolean isLoginMode = true;
    private BackendApi api;

    private TextView tvAppTitle, tvAppSubtitle, tvFormTitle, tvSwitchAuth;
    private EditText etEmail, etPassword;
    private Button btnSubmit, btnLangToggle;
    private ProgressBar progressLoading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        api = new BackendApi(this);

        // Bind layouts
        tvAppTitle = (TextView) findViewById(R.id.tv_app_title);
        tvAppSubtitle = (TextView) findViewById(R.id.tv_app_subtitle);
        tvFormTitle = (TextView) findViewById(R.id.tv_form_title);
        tvSwitchAuth = (TextView) findViewById(R.id.tv_switch_auth);
        etEmail = (EditText) findViewById(R.id.et_email);
        etPassword = (EditText) findViewById(R.id.et_password);
        btnSubmit = (Button) findViewById(R.id.btn_submit);
        btnLangToggle = (Button) findViewById(R.id.btn_lang_toggle);
        progressLoading = (ProgressBar) findViewById(R.id.progress_loading);

        // Load saved session if exists
        SharedPreferences prefs = getSharedPreferences("salon_prefs", MODE_PRIVATE);
        String savedToken = prefs.getString("token", "");
        String currentLang = prefs.getString("lang", "en");
        
        Lang.setLanguage(currentLang);
        updateLocalizationUI();

        if (!savedToken.isEmpty()) {
            api.setToken(savedToken);
            goToBookingActivity();
            return;
        }

        // Action listeners
        btnLangToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newLang = "en".equals(Lang.getLanguage()) ? "hi" : "en";
                Lang.setLanguage(newLang);
                
                SharedPreferences.Editor editor = getSharedPreferences("salon_prefs", MODE_PRIVATE).edit();
                editor.putString("lang", newLang);
                editor.apply();

                updateLocalizationUI();
            }
        });

        tvSwitchAuth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isLoginMode = !isLoginMode;
                updateLocalizationUI();
            }
        });

        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String email = etEmail.getText().toString().trim();
                final String pwd = etPassword.getText().toString().trim();

                if (email.isEmpty() || pwd.isEmpty()) {
                    showToast(Lang.get("fill_all"));
                    return;
                }

                setLoading(true);

                if (isLoginMode) {
                    // Login mode flow
                    api.login(email, pwd, new BackendApi.ApiCallback() {
                        @Override
                        public void onSuccess(final JSONObject response) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    setLoading(false);
                                    String token = response.optString("token", "");
                                    if (!token.isEmpty()) {
                                        saveSession(token, email);
                                        api.setToken(token);
                                        goToBookingActivity();
                                    } else {
                                        showToast(Lang.get("auth_failed"));
                                    }
                                }
                            });
                        }

                        @Override
                        public void onError(final String error) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    setLoading(false);
                                    showToast(error);
                                }
                            });
                        }
                    });
                } else {
                    // Registration sequence with automatic auto-login
                    api.register(email, pwd, new BackendApi.ApiCallback() {
                        @Override
                        public void onSuccess(final JSONObject registerResponse) {
                            // Proceed immediately to login for token capture
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    showToast(Lang.get("reg_success_login"));
                                }
                            });

                            api.login(email, pwd, new BackendApi.ApiCallback() {
                                @Override
                                public void onSuccess(final JSONObject loginResponse) {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            setLoading(false);
                                            String token = loginResponse.optString("token", "");
                                            if (!token.isEmpty()) {
                                                saveSession(token, email);
                                                api.setToken(token);
                                                goToBookingActivity();
                                            } else {
                                                showToast(Lang.get("auth_failed"));
                                            }
                                        }
                                    });
                                }

                                @Override
                                public void onError(final String loginErr) {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            setLoading(false);
                                            showToast(loginErr);
                                        }
                                    });
                                }
                            });
                        }

                        @Override
                        public void onError(final String regErr) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    setLoading(false);
                                    showToast(regErr);
                                }
                            });
                        }
                    });
                }
            }
        });
    }

    private void updateLocalizationUI() {
        // Toggle dynamic configuration display labels
        tvAppTitle.setText(Lang.get("app_name"));
        etEmail.setHint(Lang.get("email_hint"));
        etPassword.setHint(Lang.get("password_hint"));

        if (isLoginMode) {
            tvFormTitle.setText(Lang.get("login_title"));
            btnSubmit.setText(Lang.get("btn_login"));
            tvSwitchAuth.setText(Lang.get("switch_to_signup"));
        } else {
            tvFormTitle.setText(Lang.get("signup_title"));
            btnSubmit.setText(Lang.get("btn_signup"));
            tvSwitchAuth.setText(Lang.get("switch_to_login"));
        }

        btnLangToggle.setText("en".equalsIgnoreCase(Lang.getLanguage()) ? "हिंदी (HINDI)" : "ENGLISH");
    }

    private void setLoading(boolean isLoading) {
        progressLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnSubmit.setEnabled(!isLoading);
        btnLangToggle.setEnabled(!isLoading);
    }

    private void saveSession(String token, String email) {
        SharedPreferences.Editor editor = getSharedPreferences("salon_prefs", MODE_PRIVATE).edit();
        editor.putString("token", token);
        editor.putString("email", email);
        editor.apply();
    }

    private void goToBookingActivity() {
        Intent intent = new Intent(MainActivity.this, BookingActivity.class);
        startActivity(intent);
        finish();
    }

    private void showToast(String msg) {
        Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();
    }
}