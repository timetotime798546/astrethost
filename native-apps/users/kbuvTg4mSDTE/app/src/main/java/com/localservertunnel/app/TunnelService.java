package com.localservertunnel.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TunnelService extends Service {

    public static final String ACTION_START = "com.localservertunnel.app.START";
    public static final String ACTION_STOP = "com.localservertunnel.app.STOP";
    public static final String ACTION_UPDATE = "com.localservertunnel.app.UPDATE";

    public static final int STATE_DISCONNECTED = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;
    public static final int STATE_ERROR = 3;

    public static int currentState = STATE_DISCONNECTED;
    public static String currentUrl = "";
    public static String errorMessage = "";
    public static StringBuilder logHistory = new StringBuilder();

    private static final String CHANNEL_ID = "tunnel_service_channel";
    private static final int NOTIFICATION_ID = 404;

    private ExecutorService executor;
    private Process currentProcess;

    @Override
    public void onCreate() {
        super.onCreate();
        executor = Executors.newSingleThreadExecutor();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_START.equals(action)) {
                int port = intent.getIntExtra("port", 8089);
                startTunnel(port);
            } else if (ACTION_STOP.equals(action)) {
                stopTunnel();
            }
        }
        return START_NOT_STICKY;
    }

    private void startTunnel(final int port) {
        if (currentState == STATE_CONNECTED || currentState == STATE_CONNECTING) {
            appendLog("Tunnel is already running or connecting.");
            return;
        }

        currentState = STATE_CONNECTING;
        currentUrl = "";
        errorMessage = "";
        logHistory.setLength(0); // Clean old logs on fresh connection session
        appendLog("System checking: Local HTTP server on port " + port);
        notifyUpdate();

        Notification notification = buildNotification("Initiating proxy pipeline...");
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }

        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    File binary = new File(getFilesDir(), "cloudflared");
                    if (!binary.exists() || !binary.canExecute()) {
                        throw new Exception("Executable binary missing or non-executable.");
                    }

                    // Strict argument isolation to prevent any possible shell-command injection vector
                    ProcessBuilder pb = new ProcessBuilder(
                        binary.getAbsolutePath(),
                        "tunnel",
                        "--url",
                        "http://127.0.0.1:" + port
                    );
                    pb.redirectErrorStream(true);
                    currentProcess = pb.start();

                    BufferedReader reader = new BufferedReader(new InputStreamReader(currentProcess.getInputStream()));
                    String line;
                    Pattern pattern = Pattern.compile("https://[a-zA-Z0-9\\-]+\\.trycloudflare\\.com");

                    while (currentProcess != null && (line = reader.readLine()) != null) {
                        appendLog(line);
                        Matcher matcher = pattern.matcher(line);
                        if (matcher.find()) {
                            currentUrl = matcher.group();
                            currentState = STATE_CONNECTED;
                            appendLog("\n==========================================");
                            appendLog("CONNECTED!");
                            appendLog("Public Tunnel: " + currentUrl);
                            appendLog("==========================================\n");
                            updateNotification();
                            notifyUpdate();
                        }
                    }

                    int exitCode = currentProcess.waitFor();
                    appendLog("\nProcess completed with system code: " + exitCode);
                    if (exitCode != 0 && exitCode != 137) { // 137 is SIGKILL
                        currentState = STATE_ERROR;
                        errorMessage = "Process exited with code " + exitCode;
                    } else {
                        currentState = STATE_DISCONNECTED;
                    }

                } catch (Exception e) {
                    currentState = STATE_ERROR;
                    errorMessage = e.getMessage();
                    appendLog("Operation Error: " + e.getMessage());
                    
                    if (e.getMessage() != null && e.getMessage().contains("Permission denied")) {
                        appendLog("\n--- SECURITY POLICY EXCEPTION INTERPRETED ---");
                        appendLog("Android 10+ restricts execution of binaries within standard writable data directories.");
                        appendLog("If execution failed, confirm if device execution is restricted on this SDK platform.");
                        appendLog("Workaround: Execute cloudflared inside an accessible terminal context (e.g. Termux)");
                        appendLog("and proxy loopback traffic securely.");
                        appendLog("-----------------------------------------------\n");
                    }
                } finally {
                    cleanupProcess();
                    notifyUpdate();
                    stopSelf();
                }
            }
        });
    }

    private void stopTunnel() {
        appendLog("Halting Tunnel process manually...");
        cleanupProcess();
        currentState = STATE_DISCONNECTED;
        notifyUpdate();
        stopSelf();
    }

    private void cleanupProcess() {
        if (currentProcess != null) {
            try {
                currentProcess.destroy();
            } catch (Exception e) {}
            currentProcess = null;
        }
    }

    private void appendLog(String line) {
        logHistory.append(line).append("\n");
        notifyUpdate();
    }

    private void notifyUpdate() {
        Intent intent = new Intent(ACTION_UPDATE);
        if (Build.VERSION.SDK_INT >= 34) {
            intent.setPackage(getPackageName());
        }
        sendBroadcast(intent);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Tunnel Connectivity Status",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Shows running tunnel instances and exposed links.");
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification buildNotification(String text) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        } else {
            pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        Intent stopIntent = new Intent(this, TunnelService.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent stopPendingIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            stopPendingIntent = PendingIntent.getService(this, 1, stopIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        } else {
            stopPendingIntent = PendingIntent.getService(this, 1, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }

        builder.setContentTitle("Local Server Tunnel")
               .setContentText(text)
               .setSmallIcon(android.R.drawable.stat_sys_phone_call)
               .setContentIntent(pendingIntent)
               .setOngoing(true)
               .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop Tunnel", stopPendingIntent);

        return builder.build();
    }

    private void updateNotification() {
        String text = "Tunnel is active";
        if (currentState == STATE_CONNECTED && !currentUrl.isEmpty()) {
            text = "Public Link: " + currentUrl;
        } else if (currentState == STATE_CONNECTING) {
            text = "Connecting to Quick Tunnel...";
        }

        NotificationNotificationUpdate(text);
    }

    private void NotificationNotificationUpdate(String text) {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, buildNotification(text));
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cleanupProcess();
        if (executor != null) {
            executor.shutdownNow();
        }
        stopForeground(true);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}