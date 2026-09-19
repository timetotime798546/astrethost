package com.gmailsmtpmailer.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.gmailsmtpmailer.app.SmtpClient.SmtpListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final String PREFS_NAME = "SmtpPrefs";
    private static final String KEY_HOST = "smtp_host";
    private static final String KEY_PORT = "smtp_port";
    private static final String KEY_SSL = "smtp_ssl";
    private static final String KEY_USER = "smtp_user";
    private static final String KEY_PASS = "smtp_pass";

    private SharedPreferences prefs;

    private TextView tvCurrentSender;
    private EditText etRecipient;
    private EditText etSubject;
    private EditText etBody;
    private Button btnSend;
    private Button btnSettings;
    private ProgressBar progressBar;
    private TextView tvLogs;
    private ScrollView svLogContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Bind layout views
        tvCurrentSender = (TextView) findViewById(R.id.tvCurrentSender);
        etRecipient = (EditText) findViewById(R.id.etRecipient);
        etSubject = (EditText) findViewById(R.id.etSubject);
        etBody = (EditText) findViewById(R.id.etBody);
        btnSend = (Button) findViewById(R.id.btnSend);
        btnSettings = (Button) findViewById(R.id.btnSettings);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        tvLogs = (TextView) findViewById(R.id.tvLogs);
        svLogContainer = (ScrollView) findViewById(R.id.svLogContainer);

        updateSenderStatusDisplay();

        // Setup actions
        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSettingsDialog();
            }
        });

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptSendEmail();
            }
        });

        // Auto open setup dialog if details are empty
        if (getUserEmail().isEmpty() || getUserPassword().isEmpty()) {
            appendLog("System startup: Missing SMTP configurations. Opening setup wizard.");
            showSettingsDialog();
        }
    }

    private void updateSenderStatusDisplay() {
        String email = getUserEmail();
        if (email.isEmpty()) {
            tvCurrentSender.setText("Not configured (Tap 'SMTP Settings')");
            tvCurrentSender.setTextColor(0xFFE53935); // Red color
        } else {
            tvCurrentSender.setText(email + " (" + getHost() + ":" + getPort() + ")");
            tvCurrentSender.setTextColor(0xFF2E7D32); // Green color
        }
    }

    private void showSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_settings, null);
        builder.setView(dialogView);

        final EditText etHost = (EditText) dialogView.findViewById(R.id.etHost);
        final EditText etPort = (EditText) dialogView.findViewById(R.id.etPort);
        final CheckBox cbUseSsl = (CheckBox) dialogView.findViewById(R.id.cbUseSsl);
        final EditText etUsername = (EditText) dialogView.findViewById(R.id.etUsername);
        final EditText etPassword = (EditText) dialogView.findViewById(R.id.etPassword);
        Button btnCancel = (Button) dialogView.findViewById(R.id.btnCancel);
        Button btnSave = (Button) dialogView.findViewById(R.id.btnSave);

        // Prepopulate saved configurations
        etHost.setText(getHost());
        etPort.setText(String.valueOf(getPort()));
        cbUseSsl.setChecked(getUseSsl());
        etUsername.setText(getUserEmail());
        etPassword.setText(getUserPassword());

        final AlertDialog dialog = builder.create();

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String host = etHost.getText().toString().trim();
                String portStr = etPort.getText().toString().trim();
                boolean useSsl = cbUseSsl.isChecked();
                String user = etUsername.getText().toString().trim();
                String pass = etPassword.getText().toString().trim();

                if (host.isEmpty()) {
                    etHost.setError("Host is required");
                    return;
                }
                if (portStr.isEmpty()) {
                    etPort.setError("Port is required");
                    return;
                }
                if (user.isEmpty()) {
                    etUsername.setError("Sender address is required");
                    return;
                }
                if (pass.isEmpty()) {
                    etPassword.setError("App password is required");
                    return;
                }

                int port;
                try {
                    port = Integer.parseInt(portStr);
                } catch (NumberFormatException e) {
                    etPort.setError("Invalid port number");
                    return;
                }

                // Save parameters inside shared preferences
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(KEY_HOST, host);
                editor.putInt(KEY_PORT, port);
                editor.putBoolean(KEY_SSL, useSsl);
                editor.putString(KEY_USER, user);
                editor.putString(KEY_PASS, pass);
                editor.apply();

                dialog.dismiss();
                updateSenderStatusDisplay();
                appendLog("SMTP Configurations saved successfully!");
                Toast.makeText(MainActivity.this, "Settings Saved", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void attemptSendEmail() {
        final String host = getHost();
        final int port = getPort();
        final boolean useSsl = getUseSsl();
        final String username = getUserEmail();
        final String password = getUserPassword();

        final String to = etRecipient.getText().toString().trim();
        final String subject = etSubject.getText().toString().trim();
        final String body = etBody.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please configure SMTP Settings first!", Toast.LENGTH_LONG).show();
            showSettingsDialog();
            return;
        }

        if (to.isEmpty()) {
            etRecipient.setError("Recipient address is required");
            etRecipient.requestFocus();
            return;
        }

        if (subject.isEmpty()) {
            etSubject.setError("Subject line is required");
            etSubject.requestFocus();
            return;
        }

        if (body.isEmpty()) {
            etBody.setError("Body content cannot be empty");
            etBody.requestFocus();
            return;
        }

        // Prepare UI state for execution progress
        setUiSendingState(true);
        clearLogs();
        appendLog("Initiating outbound Gmail SMTP session...");

        SmtpClient.sendEmail(host, port, useSsl, username, password, to, subject, body, new SmtpListener() {
            @Override
            public void onLog(final String message) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        appendLog(message);
                    }
                });
            }

            @Override
            public void onSuccess() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setUiSendingState(false);
                        appendLog("[SUCCESS] Message delivered to target SMTP relay queue.");
                        Toast.makeText(MainActivity.this, "Email Sent Successfully!", Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onFailure(final String error) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setUiSendingState(false);
                        appendLog("[FAILURE ERROR] " + error);
                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                        builder.setTitle("SMTP Transfer Failed");
                        builder.setMessage(error + "\n\nHelpful Tips:\n1. Ensure you configured a 16-character 'App Password' from Google Account settings.\n2. Double check internet connectivity.");
                        builder.setPositiveButton("Close", null);
                        builder.show();
                    }
                });
            }
        });
    }

    private void setUiSendingState(boolean isSending) {
        btnSend.setEnabled(!isSending);
        btnSettings.setEnabled(!isSending);
        etRecipient.setEnabled(!isSending);
        etSubject.setEnabled(!isSending);
        etBody.setEnabled(!isSending);
        progressBar.setVisibility(isSending ? View.VISIBLE : View.GONE);
    }

    private void clearLogs() {
        tvLogs.setText("");
    }

    private void appendLog(String message) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault());
        String timestamp = sdf.format(new Date());
        String line = "[" + timestamp + "] " + message + "\n";
        tvLogs.append(line);
        svLogContainer.post(new Runnable() {
            @Override
            public void run() {
                svLogContainer.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }

    // Default getter credentials helpers
    private String getHost() {
        return prefs.getString(KEY_HOST, "smtp.gmail.com");
    }

    private int getPort() {
        return prefs.getInt(KEY_PORT, 465);
    }

    private boolean getUseSsl() {
        return prefs.getBoolean(KEY_SSL, true);
    }

    private String getUserEmail() {
        return prefs.getString(KEY_USER, "");
    }

    private String getUserPassword() {
        return prefs.getString(KEY_PASS, "");
    }
}