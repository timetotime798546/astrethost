package com.localgeminibridge.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;

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

    private SimpleHttpServer localServer;
    private Thread serverThread;
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

        localServer = new SimpleHttpServer();
        serverThread = new Thread(localServer);
        isServerRunning = true;
        serverThread.start();
        updateStatusUI();
        appendLog("[SYSTEM]: Local Server started on port " + SERVER_PORT);
    }

    private synchronized void stopLocalServer() {
        if (!isServerRunning) return;

        if (localServer != null) {
            localServer.shutdown();
        }
        isServerRunning = false;
        updateStatusUI();
        appendLog("[SYSTEM]: Local Server stopped.");
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
            appendLog("[INTENT]: Launched Gemini app with payload.");
        } catch (Exception e) {
            appendLog("[WARNING]: Gemini Package (com.google.android.apps.bard) not found. Attempting generic action chooser...");
            try {
                Intent genericIntent = new Intent();
                genericIntent.setAction(Intent.ACTION_SEND);
                genericIntent.putExtra(Intent.EXTRA_TEXT, prompt);
                genericIntent.setType("text/plain");
                Intent chooser = Intent.createChooser(genericIntent, "Send Prompt to Gemini/AI Client");
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(chooser);
            } catch (Exception ex) {
                appendLog("[ERROR]: Failed to fire action intents: " + ex.getMessage());
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopLocalServer();
    }

    /**
     * Light Server Socket implementation targeting /send-prompt POST payloads
     */
    private class SimpleHttpServer implements Runnable {
        private ServerSocket mServerSocket;
        private boolean mIsActive = true;

        public void shutdown() {
            mIsActive = false;
            if (mServerSocket != null) {
                try {
                    mServerSocket.close();
                } catch (IOException e) {
                    // Fail silently
                }
            }
        }

        @Override
        public void run() {
            try {
                mServerSocket = new ServerSocket(SERVER_PORT);
                while (mIsActive) {
                    Socket clientSocket = mServerSocket.accept();
                    handleConnection(clientSocket);
                }
            } catch (IOException e) {
                if (mIsActive) {
                    appendLog("[ERROR]: Listener Socket failed: " + e.getMessage());
                }
            }
        }

        private void handleConnection(final Socket socket) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    BufferedReader in = null;
                    OutputStream out = null;
                    try {
                        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        out = socket.getOutputStream();

                        // Parse primary HTTP Request line
                        String requestLine = in.readLine();
                        if (requestLine == null) {
                            socket.close();
                            return;
                        }

                        appendLog("[HTTP REQ]: " + requestLine);

                        // Flags
                        boolean isPost = requestLine.toUpperCase().startsWith("POST");
                        boolean isSendPromptEndpoint = requestLine.contains("/send-prompt");

                        int contentLength = 0;
                        String line;
                        // Read headers
                        while ((line = in.readLine()) != null && !line.trim().isEmpty()) {
                            if (line.toLowerCase().startsWith("content-length:")) {
                                try {
                                    contentLength = Integer.parseInt(line.substring(15).trim());
                                } catch (Exception e) {
                                    contentLength = 0;
                                }
                            }
                        }

                        // Parse POST Body payload
                        String rawBody = "";
                        if (isPost && contentLength > 0) {
                            char[] buffer = new char[contentLength];
                            int totalRead = 0;
                            while (totalRead < contentLength) {
                                int readCount = in.read(buffer, totalRead, contentLength - totalRead);
                                if (readCount == -1) break;
                                totalRead += readCount;
                            }
                            rawBody = new String(buffer, 0, totalRead);
                        }

                        // Routing
                        if (isPost && isSendPromptEndpoint) {
                            String targetPrompt = parseParameter(rawBody, "prompt");
                            if (targetPrompt != null && !targetPrompt.isEmpty()) {
                                appendLog("[ROUTE SUCCESS]: Target Prompt received. Content: \"" + targetPrompt + "\"");
                                
                                final String finalPrompt = targetPrompt;
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        launchGeminiApp(finalPrompt);
                                    }
                                });

                                sendResponse(out, 200, "OK", "Prompt processed and launching client layout successfully.");
                            } else {
                                appendLog("[ROUTE ERROR]: Field 'prompt' is missing or blank inside raw request body.");
                                sendResponse(out, 400, "Bad Request", "Missing target prompt element. Ensure 'prompt' POST parameter is provided.");
                            }
                        } else {
                            sendResponse(out, 404, "Not Found", "Support endpoints: POST /send-prompt only.");
                        }

                    } catch (Exception e) {
                        appendLog("[SERVER EXCEPTION]: Connection handler encountered: " + e.getMessage());
                    } finally {
                        try {
                            if (in != null) in.close();
                            if (out != null) out.close();
                            socket.close();
                        } catch (IOException e) {
                            // Ignored
                        }
                    }
                }
            }).start();
        }

        private String parseParameter(String source, String key) {
            if (source == null || source.isEmpty()) return null;
            
            // Handle URL Form Encoded values: prompt=hello+world
            if (source.contains(key + "=")) {
                try {
                    int startIndex = source.indexOf(key + "=") + key.length() + 1;
                    int endIndex = source.indexOf("&", startIndex);
                    String rawVal = endIndex != -1 ? source.substring(startIndex, endIndex) : source.substring(startIndex);
                    return URLDecoder.decode(rawVal, "UTF-8");
                } catch (Exception e) {
                    return null;
                }
            }

            // Simple robust check for JSON formatted payload keys: {"prompt":"hello world"}
            String jsonKey = "\"" + key + "\"";
            if (source.contains(jsonKey)) {
                try {
                    int keyIndex = source.indexOf(jsonKey);
                    int colonIndex = source.indexOf(":", keyIndex);
                    int firstQuote = source.indexOf("\"", colonIndex);
                    int secondQuote = source.indexOf("\"", firstQuote + 1);
                    if (firstQuote != -1 && secondQuote != -1) {
                        String rawVal = source.substring(firstQuote + 1, secondQuote);
                        // Simple unicode escape decode check (like \u0020 or simple escaping)
                        return rawVal.replace("\\\"", "\"").replace("\\\\", "\\");
                    }
                } catch (Exception e) {
                    return null;
                }
            }
            return null;
        }

        private void sendResponse(OutputStream out, int status, String message, String details) throws IOException {
            String responseBody = "{\"success\": " + (status == 200 ? "true" : "false") + 
                    ", \"status\": " + status + 
                    ", \"message\": \"" + message + "\"" + 
                    ", \"details\": \"" + details + "\"}";

            byte[] bodyBytes = responseBody.getBytes("UTF-8");

            out.write(("HTTP/1.1 " + status + " " + message + "\r\n").getBytes());
            out.write("Content-Type: application/json; charset=utf-8\r\n".getBytes());
            out.write(("Content-Length: " + bodyBytes.length + "\r\n").getBytes());
            out.write("Access-Control-Allow-Origin: *\r\n".getBytes());
            out.write("Connection: close\r\n".getBytes());
            out.write("\r\n".getBytes());
            out.write(bodyBytes);
            out.flush();
        }
    }
}