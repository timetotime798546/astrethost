package com.localservertunnel.app;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends Activity {

    private EditText portInput;
    private Button checkServerBtn;
    private TextView serverStatusText;
    
    private TextView binaryStatusText;
    private Button setupButton;
    private ProgressBar downloadProgress;
    
    private TextView tunnelStatusText;
    private TextView publicUrlText;
    private Button startBtn;
    private Button stopBtn;
    private Button copyBtn;
    private Button openBtn;
    
    private ScrollView logScroll;
    private TextView logText;

    private SharedPreferences prefs;

    private final BroadcastReceiver updateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateUI();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("tunnel_prefs", MODE_PRIVATE);

        portInput = (EditText) findViewById(R.id.port_input);
        checkServerBtn = (Button) findViewById(R.id.check_server_btn);
        serverStatusText = (TextView) findViewById(R.id.server_status_text);
        
        binaryStatusText = (TextView) findViewById(R.id.binary_status_text);
        setupButton = (Button) findViewById(R.id.setup_btn);
        downloadProgress = (ProgressBar) findViewById(R.id.download_progress);
        
        tunnelStatusText = (TextView) findViewById(R.id.tunnel_status_text);
        publicUrlText = (TextView) findViewById(R.id.public_url_text);
        startBtn = (Button) findViewById(R.id.start_btn);
        stopBtn = (Button) findViewById(R.id.stop_btn);
        copyBtn = (Button) findViewById(R.id.copy_btn);
        openBtn = (Button) findViewById(R.id.open_btn);
        
        logScroll = (ScrollView) findViewById(R.id.log_scroll);
        logText = (TextView) findViewById(R.id.log_text);

        // Load saved settings
        String savedPort = prefs.getString("last_port", "8089");
        portInput.setText(savedPort);

        // Track port changes and save configuration
        portInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String val = s.toString().trim();
                if (!val.isEmpty()) {
                    prefs.edit().putString("last_port", val).apply();
                }
            }
        });

        // Request runtime notification access for newer API releases
        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission("android.permission.POST_NOTIFICATIONS") != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 101);
            }
        }

        checkBinaryStatus();

        checkServerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int port = getPort();
                if (port > 0) {
                    testServerConnectivity(port);
                }
            }
        });

        setupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downloadBinary();
            }
        });

        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int port = getPort();
                if (port <= 0) return;

                File binaryFile = new File(getFilesDir(), "cloudflared");
                if (!binaryFile.exists()) {
                    Toast.makeText(MainActivity.this, "Please download cloudflared binary first", Toast.LENGTH_LONG).show();
                    return;
                }

                Intent serviceIntent = new Intent(MainActivity.this, TunnelService.class);
                serviceIntent.setAction(TunnelService.ACTION_START);
                serviceIntent.putExtra("port", port);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent);
                } else {
                    startService(serviceIntent);
                }
            }
        });

        stopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent serviceIntent = new Intent(MainActivity.this, TunnelService.class);
                serviceIntent.setAction(TunnelService.ACTION_STOP);
                startService(serviceIntent);
            }
        });

        copyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = publicUrlText.getText().toString();
                if (!url.equals("—") && !url.isEmpty()) {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("Cloudflare Public URL", url);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(MainActivity.this, "URL copied to clipboard", Toast.LENGTH_SHORT).show();
                }
            }
        });

        openBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = publicUrlText.getText().toString();
                if (!url.equals("—") && !url.isEmpty()) {
                    try {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        startActivity(browserIntent);
                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this, "Failed to open link", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        // Trigger immediate check of local server connectivity on start
        int initialPort = getPort();
        if (initialPort > 0) {
            testServerConnectivity(initialPort);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= 34) {
            registerReceiver(updateReceiver, new IntentFilter(TunnelService.ACTION_UPDATE), Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(updateReceiver, new IntentFilter(TunnelService.ACTION_UPDATE));
        }
        updateUI();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(updateReceiver);
    }

    private int getPort() {
        String input = portInput.getText().toString().trim();
        try {
            int port = Integer.parseInt(input);
            if (port < 1 || port > 65535) {
                Toast.makeText(this, "Port must be between 1 and 65535", Toast.LENGTH_SHORT).show();
                return -1;
            }
            return port;
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid port number", Toast.LENGTH_SHORT).show();
            return -1;
        }
    }

    private void checkBinaryStatus() {
        File file = new File(getFilesDir(), "cloudflared");
        if (file.exists()) {
            if (!file.canExecute()) {
                file.setExecutable(true, false);
            }
            if (file.canExecute()) {
                binaryStatusText.setText("Status: Binary Ready (Executable)");
                binaryStatusText.setTextColor(0xFF198754);
            } else {
                binaryStatusText.setText("Status: Ready but execution restricted");
                binaryStatusText.setTextColor(0xFFDC3545);
            }
        } else {
            binaryStatusText.setText("Status: Binary not found");
            binaryStatusText.setTextColor(0xFFDC3545);
        }
    }

    private void testServerConnectivity(final int port) {
        serverStatusText.setText("Testing local connectivity...");
        serverStatusText.setTextColor(0xFF6B7280);
        new Thread(new Runnable() {
            @Override
            public void run() {
                java.net.Socket socket = null;
                boolean reachable = false;
                try {
                    socket = new java.net.Socket();
                    socket.connect(new java.net.InetSocketAddress("127.0.0.1", port), 1000);
                    reachable = true;
                } catch (Exception e) {
                    reachable = false;
                } finally {
                    if (socket != null) {
                        try {
                            socket.close();
                        } catch (Exception e) {}
                    }
                }

                final boolean finalReachable = reachable;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (finalReachable) {
                            serverStatusText.setText("Local server detected on port " + port);
                            serverStatusText.setTextColor(0xFF198754);
                        } else {
                            serverStatusText.setText("No server running on port " + port);
                            serverStatusText.setTextColor(0xFFDC3545);
                        }
                    }
                });
            }
        }).start();
    }

    private void downloadBinary() {
        final String arch = System.getProperty("os.arch").toLowerCase();
        String url = null;

        if (arch.contains("aarch64") || arch.contains("arm64")) {
            url = "https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-arm64";
        } else if (arch.contains("arm")) {
            url = "https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-arm";
        } else if (arch.contains("x86_64") || arch.contains("amd64")) {
            url = "https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-amd64";
        } else if (arch.contains("i386") || arch.contains("x86")) {
            url = "https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-386";
        }

        if (url == null) {
            logText.append("\nUnsupported CPU architecture: " + arch + "\n");
            Toast.makeText(this, "Unsupported CPU architecture: " + arch, Toast.LENGTH_LONG).show();
            return;
        }

        final String downloadUrl = url;
        downloadProgress.setVisibility(View.VISIBLE);
        downloadProgress.setProgress(0);
        setupButton.setEnabled(false);
        logText.append("\nStarting binary download for " + arch + "...\n");

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    File targetFile = new File(getFilesDir(), "cloudflared");
                    URL urlObj = new URL(downloadUrl);
                    HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();
                    conn.setConnectTimeout(15000);
                    conn.setReadTimeout(15000);
                    int status = conn.getResponseCode();

                    int redirects = 0;
                    while ((status == HttpURLConnection.HTTP_MOVED_TEMP
                            || status == HttpURLConnection.HTTP_MOVED_PERM
                            || status == 307 || status == 308) && redirects < 5) {
                        String redirectUrl = conn.getHeaderField("Location");
                        conn.disconnect();
                        urlObj = new URL(redirectUrl);
                        conn = (HttpURLConnection) urlObj.openConnection();
                        conn.setConnectTimeout(15000);
                        conn.setReadTimeout(15000);
                        status = conn.getResponseCode();
                        redirects++;
                    }

                    if (status != HttpURLConnection.HTTP_OK) {
                        throw new java.io.IOException("Remote server returned code " + status);
                    }

                    int totalLength = conn.getContentLength();
                    InputStream in = conn.getInputStream();
                    FileOutputStream out = new FileOutputStream(targetFile);

                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    int cumulativeBytes = 0;

                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                        cumulativeBytes += bytesRead;
                        if (totalLength > 0) {
                            final int progressPercent = (int) (((float) cumulativeBytes / totalLength) * 100);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    downloadProgress.setProgress(progressPercent);
                                }
                            });
                        }
                    }

                    out.flush();
                    out.close();
                    in.close();

                    targetFile.setExecutable(true, false);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            downloadProgress.setVisibility(View.GONE);
                            setupButton.setEnabled(true);
                            checkBinaryStatus();
                            logText.append("Download complete. Target configured with execution privileges.\n");
                            Toast.makeText(MainActivity.this, "Configuration verified!", Toast.LENGTH_SHORT).show();
                        }
                    });

                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            downloadProgress.setVisibility(View.GONE);
                            setupButton.setEnabled(true);
                            logText.append("Configuration failed: " + e.getMessage() + "\n");
                            Toast.makeText(MainActivity.this, "Verification failure: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }).start();
    }

    private void updateUI() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                logText.setText(TunnelService.logHistory.toString());
                logScroll.post(new Runnable() {
                    @Override
                    public void run() {
                        logScroll.fullScroll(View.FOCUS_DOWN);
                    }
                });

                int state = TunnelService.currentState;
                if (state == TunnelService.STATE_CONNECTED) {
                    tunnelStatusText.setText("TUNNEL CONNECTED");
                    tunnelStatusText.setTextColor(0xFF198754);
                    publicUrlText.setText(TunnelService.currentUrl);
                    startBtn.setEnabled(false);
                    stopBtn.setEnabled(true);
                    copyBtn.setEnabled(true);
                    openBtn.setEnabled(true);
                } else if (state == TunnelService.STATE_CONNECTING) {
                    tunnelStatusText.setText("TUNNEL ESTABLISHING...");
                    tunnelStatusText.setTextColor(0xFFF38020);
                    publicUrlText.setText("Generating Link...");
                    startBtn.setEnabled(false);
                    stopBtn.setEnabled(true);
                    copyBtn.setEnabled(false);
                    openBtn.setEnabled(false);
                } else if (state == TunnelService.STATE_ERROR) {
                    tunnelStatusText.setText("TUNNEL SETUP ERROR");
                    tunnelStatusText.setTextColor(0xFFDC3545);
                    publicUrlText.setText("—");
                    startBtn.setEnabled(true);
                    stopBtn.setEnabled(false);
                    copyBtn.setEnabled(false);
                    openBtn.setEnabled(false);
                } else {
                    tunnelStatusText.setText("TUNNEL DISCONNECTED");
                    tunnelStatusText.setTextColor(0xFF6B7280);
                    publicUrlText.setText("—");
                    startBtn.setEnabled(true);
                    stopBtn.setEnabled(false);
                    copyBtn.setEnabled(false);
                    openBtn.setEnabled(false);
                }
            }
        });
    }
}