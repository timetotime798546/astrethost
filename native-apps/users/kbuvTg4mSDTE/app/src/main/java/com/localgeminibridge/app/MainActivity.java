package com.localgeminibridge.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class MainActivity extends Activity {

    private TextView tvStatus;
    private TextView tvIp;
    private TextView tvEndpoint;
    private TextView tvLogs;
    private ScrollView scrollerLogs;
    private Button btnToggleServer;
    private Button btnRefreshIp;
    private EditText etTestPrompt;
    private Button btnTestSend;
    private TextView tvClearLogs;

    // Accessibility configuration elements
    private TextView tvAccessibilityStatus;
    private Button btnEnableAccessibility;

    private MyNanoHTTPD localServer;
    private boolean isServerRunning = false;
    private final int SERVER_PORT = 8080;

    // Static instance used to securely route logs from background service back to UI Console
    private static MainActivity instance = null;

    public static void logBridge(final String message) {
        if (instance != null) {
            instance.appendLog(message);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;
        setContentView(R.layout.activity_main);

        // Bind UI Elements
        tvStatus = (TextView) findViewById(R.id.tv_status);
        tvIp = (TextView) findViewById(R.id.tv_ip);
        tvEndpoint = (TextView) findViewById(R.id.tv_endpoint);
        tvLogs = (TextView) findViewById(R.id.tv_logs);
        scrollerLogs = (ScrollView) findViewById(R.id.scroller_logs);
        btnToggleServer = (Button) findViewById(R.id.btn_toggle_server);
        btnRefreshIp = (Button) findViewById(R.id.btn_refresh_ip);
        etTestPrompt = (EditText) findViewById(R.id.et_test_prompt);
        btnTestSend = (Button) findViewById(R.id.btn_test_send);
        tvClearLogs = (TextView) findViewById(R.id.tv_clear_logs);
        tvAccessibilityStatus = (TextView) findViewById(R.id.tv_accessibility_status);
        btnEnableAccessibility = (Button) findViewById(R.id.btn_enable_accessibility);

        // Init UI States
        updateNetworkInfo();
        updateStatusUI();

        // Register Action Listeners
        btnToggleServer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isServerRunning) {
                    stopLocalServer();
                } else {
                    startLocalServer();
                }
            }
        });

        btnRefreshIp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateNetworkInfo();
                appendLog("Network configuration refreshed manually.");
            }
        });

        btnTestSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String promptText = etTestPrompt.getText().toString().trim();
                if (promptText.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please write a prompt first!", Toast.LENGTH_SHORT).show();
                    return;
                }
                appendLog("[MANUAL ACTION]: Pre-filling prompt & requesting auto-submit.");
                launchGeminiApp(promptText);
            }
        });

        btnEnableAccessibility.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                    startActivity(intent);
                    Toast.makeText(MainActivity.this, "Select 'Local Gemini Bridge' and turn it ON", Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Could not open Accessibility settings: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });

        tvClearLogs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tvLogs.setText("--- Console Logs Cleared ---\n");
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateAccessibilityStatusUI();
    }

    private void updateAccessibilityStatusUI() {
        if (isAccessibilityServiceEnabled(this)) {
            tvAccessibilityStatus.setText("Accessibility Service: ENABLED");
            tvAccessibilityStatus.setTextColor(Color.parseColor("#4CAF50")); // Green
            btnEnableAccessibility.setText("ACCESSIBILITY CONFIGURED");
            btnEnableAccessibility.setEnabled(false);
            btnEnableAccessibility.setBackgroundColor(Color.parseColor("#9E9E9E"));
        } else {
            tvAccessibilityStatus.setText("Accessibility Service: DISABLED");
            tvAccessibilityStatus.setTextColor(Color.parseColor("#D32F2F")); // Red
            btnEnableAccessibility.setText("ENABLE AUTO-SUBMIT");
            btnEnableAccessibility.setEnabled(true);
            btnEnableAccessibility.setBackgroundColor(Color.parseColor("#FF9800"));
        }
    }

    private boolean isAccessibilityServiceEnabled(Context context) {
        int accessibilityEnabled = 0;
        final String servicePath = context.getPackageName() + "/" + GeminiAutoSubmitService.class.getName();
        try {
            accessibilityEnabled = Settings.Secure.getInt(
                    context.getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_ENABLED);
        } catch (Settings.SettingNotFoundException e) {
            // Ignore
        }
        
        TextUtils.SimpleStringSplitter colonSplitter = new TextUtils.SimpleStringSplitter(':');

        if (accessibilityEnabled == 1) {
            String settingValue = Settings.Secure.getString(
                    context.getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (settingValue != null) {
                colonSplitter.setString(settingValue);
                while (colonSplitter.hasNext()) {
                    String service = colonSplitter.next();
                    if (service.equalsIgnoreCase(servicePath)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void updateNetworkInfo() {
        String ip = getLocalIpAddress();
        tvIp.setText(ip);
        tvEndpoint.setText("POST http://" + ip + ":" + SERVER_PORT + "/send-prompt");
    }

    private void updateStatusUI() {
        if (isServerRunning) {
            tvStatus.setText("Status: RUNNING");
            tvStatus.setTextColor(Color.parseColor("#4CAF50")); // Green
            btnToggleServer.setText("Stop Server");
            btnToggleServer.setBackgroundColor(Color.parseColor("#F44336")); // Red
        } else {
            tvStatus.setText("Status: STOPPED");
            tvStatus.setTextColor(Color.parseColor("#F44336")); // Red
            btnToggleServer.setText("Start Server");
            btnToggleServer.setBackgroundColor(Color.parseColor("#4CAF50")); // Green
        }
    }

    private synchronized void startLocalServer() {
        if (isServerRunning) return;

        try {
            localServer = new MyNanoHTTPD(SERVER_PORT);
            localServer.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
            isServerRunning = true;
            updateStatusUI();
            appendLog("[SYSTEM]: Local HTTP server started on port " + SERVER_PORT);
        } catch (IOException e) {
            appendLog("[ERROR]: Failed to start NanoHTTPD Server: " + e.getMessage());
        }
    }

    private synchronized void stopLocalServer() {
        if (!isServerRunning) return;

        if (localServer != null) {
            localServer.stop();
            localServer = null;
        }
        isServerRunning = false;
        updateStatusUI();
        appendLog("[SYSTEM]: Local HTTP server stopped.");
    }

    private String getLocalIpAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (!addr.isLoopbackAddress() && addr.getHostAddress().indexOf(':') < 0) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception ex) {
            appendLog("[ERROR]: Error fetching IP address: " + ex.getMessage());
        }
        return "127.0.0.1";
    }

    private void appendLog(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String timestamp = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
                tvLogs.append("[" + timestamp + "] " + message + "\n");
                scrollerLogs.post(new Runnable() {
                    @Override
                    public void run() {
                        scrollerLogs.fullScroll(View.FOCUS_DOWN);
                    }
                });
            }
        });
    }

    private void launchGeminiApp(String prompt) {
        // Arm the accessibility service to auto-submit once Gemini displays
        GeminiAutoSubmitService.shouldTriggerSubmit = true;

        try {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, prompt);
            sendIntent.setType("text/plain");
            sendIntent.setPackage("com.google.android.apps.bard");
            sendIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(sendIntent);
            appendLog("[INTENT]: Launched Gemini app. Waiting for Accessibility to auto-submit.");
        } catch (Exception e) {
            appendLog("[WARNING]: Gemini (com.google.android.apps.bard) not installed. Prompting generic share sheet...");
            try {
                Intent genericIntent = new Intent();
                genericIntent.setAction(Intent.ACTION_SEND);
                genericIntent.putExtra(Intent.EXTRA_TEXT, prompt);
                genericIntent.setType("text/plain");
                Intent chooser = Intent.createChooser(genericIntent, "Send Prompt to Gemini Client");
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(chooser);
            } catch (Exception ex) {
                appendLog("[ERROR]: Failed to fire generic share selector: " + ex.getMessage());
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopLocalServer();
        if (instance == this) {
            instance = null;
        }
    }

    /**
     * NanoHTTPD Server Wrapper supporting CORS and standard routing logic
     */
    private class MyNanoHTTPD extends NanoHTTPD {
        
        public MyNanoHTTPD(int port) {
            super(port);
        }

        @Override
        public Response serve(IHTTPSession session) {
            String uri = session.getUri();
            Method method = session.getMethod();
            appendLog("[HTTP " + method + "]: request received for uri: " + uri);

            // Handle pre-flight OPTIONS request for seamless cross-origin web integration
            if (Method.OPTIONS.equals(method)) {
                Response response = newFixedLengthResponse(Response.Status.OK, "text/plain", "OK");
                response.addHeader("Access-Control-Allow-Origin", "*");
                response.addHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS, PUT, DELETE");
                response.addHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, x-requested-with");
                return response;
            }

            if (uri.equals("/send-prompt")) {
                try {
                    if (Method.POST.equals(method)) {
                        Map<String, String> files = new HashMap<String, String>();
                        try {
                            session.parseBody(files);
                        } catch (Exception e) {
                            appendLog("[WARNING]: Failed to parse body as form-data: " + e.getMessage());
                        }
                    }

                    // Get values from query parameter or parsed POST forms
                    Map<String, String> params = session.getParms();
                    final String promptValue = params.get("prompt");

                    if (promptValue != null && !promptValue.trim().isEmpty()) {
                        appendLog("[ROUTE SUCCESS]: Target prompt read: \"" + promptValue + "\"");

                        // Transition UI interaction safely to the UI thread
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                launchGeminiApp(promptValue);
                            }
                        });

                        Response response = newFixedLengthResponse(Response.Status.OK, "text/plain", "SUCCESS");
                        response.addHeader("Access-Control-Allow-Origin", "*");
                        response.addHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
                        return response;
                    } else {
                        appendLog("[ROUTE ERROR]: Parameter 'prompt' not found or was empty.");
                        Response response = newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "ERROR: Parameter 'prompt' is required");
                        response.addHeader("Access-Control-Allow-Origin", "*");
                        return response;
                    }

                } catch (Exception e) {
                    appendLog("[ERROR]: Exception parsing payload: " + e.getMessage());
                    Response response = newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "ERROR: " + e.getMessage());
                    response.addHeader("Access-Control-Allow-Origin", "*");
                    return response;
                }
            }

            Response response = newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "ERROR: Only POST /send-prompt is supported.");
            response.addHeader("Access-Control-Allow-Origin", "*");
            return response;
        }
    }
}