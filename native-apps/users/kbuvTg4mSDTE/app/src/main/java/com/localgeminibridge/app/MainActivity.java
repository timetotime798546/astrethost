package com.localgeminibridge.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

    private MyNanoHTTPD localServer;
    private boolean isServerRunning = false;
    private final int SERVER_PORT = 8080;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
                appendLog("[MANUAL ACTION]: Pre-filling prompt to Gemini App.");
                launchGeminiApp(promptText);
            }
        });

        tvClearLogs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tvLogs.setText("--- Console Logs Cleared ---\n");
            }
        });
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
            // Start the server using standard NanoHTTPD socket runner
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
        try {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, prompt);
            sendIntent.setType("text/plain");
            sendIntent.setPackage("com.google.android.apps.bard");
            sendIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(sendIntent);
            appendLog("[INTENT]: Launched Gemini app with payload successfully.");
        } catch (Exception e) {
            appendLog("[WARNING]: Gemini app (com.google.android.apps.bard) not found. Triggering generic share selector...");
            try {
                Intent genericIntent = new Intent();
                genericIntent.setAction(Intent.ACTION_SEND);
                genericIntent.putExtra(Intent.EXTRA_TEXT, prompt);
                genericIntent.setType("text/plain");
                Intent chooser = Intent.createChooser(genericIntent, "Send Prompt to Gemini/AI Client");
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(chooser);
            } catch (Exception ex) {
                appendLog("[ERROR]: Failed to launch any share intent handlers: " + ex.getMessage());
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopLocalServer();
    }

    /**
     * NanoHTTPD Server wrapper implementation
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

            if (uri.equals("/send-prompt")) {
                try {
                    // Pre-parse the body of POST requests to populate parameters map
                    if (Method.POST.equals(method)) {
                        Map<String, String> files = new HashMap<String, String>();
                        try {
                            session.parseBody(files);
                        } catch (Exception e) {
                            appendLog("[WARNING]: Failed to parse body as form-data: " + e.getMessage());
                        }
                    }

                    // Get values from both query parameters or parsed POST forms
                    Map<String, String> params = session.getParms();
                    final String promptValue = params.get("prompt");

                    if (promptValue != null && !promptValue.trim().isEmpty()) {
                        appendLog("[ROUTE SUCCESS]: Target prompt read: \"" + promptValue + "\"");

                        // 1. Asynchronous Execution: Transition processing to the UI Thread smoothly
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                launchGeminiApp(promptValue);
                            }
                        });

                        // 2. Immediate HTTP Response: Send immediate plain text response back to client 
                        return newFixedLengthResponse(Response.Status.OK, "text/plain", "SUCCESS");
                    } else {
                        appendLog("[ROUTE ERROR]: Parameter 'prompt' not found or was empty.");
                        return newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "ERROR: Parameter 'prompt' is required");
                    }

                } catch (Exception e) {
                    appendLog("[ERROR]: Exception parsing payload contents: " + e.getMessage());
                    return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "ERROR: " + e.getMessage());
                }
            }

            return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "ERROR: Only POST /send-prompt is supported.");
        }
    }
}