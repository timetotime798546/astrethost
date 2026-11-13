package com.bgtubeplayer.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class PaywallManager {

    private static final String PREFS_NAME = "bg_paywall_prefs";
    private static final String WORKER_BASE_URL = "https://bgtubeapizeedude.newtimenew3689-fd2.workers.dev/?device_id=";
    private static final long ONE_HOUR_MS = 3600000L;
    private static final int MAX_MANUAL_CHECKS_PER_HOUR = 3;

    public interface StatusCallback {
        void onStatusResult(boolean isPremium, boolean isExpired, PaywallConfig config);
    }

    public interface ImageCallback {
        void onImageLoaded(Bitmap bitmap);
    }

    public static class PaywallConfig {
        public String deviceId = "";
        public String whatsappNumber = "";
        public String upiId = "";
        public String premiumAmount = "";
        public long expiresAt = 0;
    }

    private Context context;
    private SharedPreferences prefs;
    private Handler mainHandler;

    public PaywallManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.mainHandler = new Handler(Looper.getMainLooper());

        // Fix 2: Initialize default preferences synchronously on first launch
        initFirstLaunchDefaults();
    }

    private void initFirstLaunchDefaults() {
        if (!prefs.contains("first_launch_done")) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("first_launch_done", true);
            editor.putLong("first_launch_time", System.currentTimeMillis());
            if (!prefs.contains("cached_status")) {
                editor.putString("cached_status", "pending");
            }
            editor.apply();
        }
        getDeviceId();
    }

    public String getDeviceId() {
        String deviceId = prefs.getString("device_id", "");
        if (deviceId.isEmpty()) {
            try {
                deviceId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
            } catch (Exception e) {
                deviceId = "";
            }
            if (deviceId == null || deviceId.isEmpty()) {
                deviceId = "UNKNOWN_DEVICE";
            }
            prefs.edit().putString("device_id", deviceId).apply();
        }
        return deviceId;
    }

    public PaywallConfig getCachedConfig() {
        PaywallConfig config = new PaywallConfig();
        config.deviceId = getDeviceId();
        config.whatsappNumber = prefs.getString("whatsapp_number", "");
        config.upiId = prefs.getString("upi_id", "");
        config.premiumAmount = prefs.getString("premium_amount", "");
        config.expiresAt = prefs.getLong("trial_expires_at", 0);
        return config;
    }

    public boolean canPerformManualCheck() {
        return getManualChecksLeft() > 0;
    }

    public int getManualChecksLeft() {
        long now = System.currentTimeMillis();
        String timestampsStr = prefs.getString("manual_check_timestamps", "");
        int count = 0;
        if (!timestampsStr.isEmpty()) {
            String[] parts = timestampsStr.split(",");
            for (int i = 0; i < parts.length; i++) {
                try {
                    long t = Long.parseLong(parts[i].trim());
                    if (now - t < ONE_HOUR_MS) {
                        count++;
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return Math.max(0, MAX_MANUAL_CHECKS_PER_HOUR - count);
    }

    public void recordManualCheck() {
        long now = System.currentTimeMillis();
        String timestampsStr = prefs.getString("manual_check_timestamps", "");
        StringBuilder sb = new StringBuilder();
        if (!timestampsStr.isEmpty()) {
            String[] parts = timestampsStr.split(",");
            for (int i = 0; i < parts.length; i++) {
                try {
                    long t = Long.parseLong(parts[i].trim());
                    if (now - t < ONE_HOUR_MS) {
                        if (sb.length() > 0) {
                            sb.append(",");
                        }
                        sb.append(t);
                    }
                } catch (Exception ignored) {
                }
            }
        }
        if (sb.length() > 0) {
            sb.append(",");
        }
        sb.append(now);
        prefs.edit().putString("manual_check_timestamps", sb.toString()).apply();
    }

    public void checkAccess(final boolean manualCheck, final StatusCallback callback) {
        final boolean isPremium = prefs.getBoolean("is_premium", false);
        
        if (isPremium) {
            if (callback != null) {
                callback.onStatusResult(true, false, getCachedConfig());
            }
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                fetchStatusFromServer(callback);
            }
        }).start();
    }

    private void fetchStatusFromServer(final StatusCallback callback) {
        HttpURLConnection connection = null;
        try {
            String deviceId = getDeviceId();
            URL url = new URL(WORKER_BASE_URL + deviceId);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                InputStream inputStream = connection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder builder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                }
                reader.close();

                JSONObject json = new JSONObject(builder.toString());
                boolean isTrial = json.optBoolean("is_trial", false);
                boolean isPremiumJson = json.optBoolean("is_premium", false);
                String statusStr = json.optString("status", "").toLowerCase();

                boolean isPaid = isPremiumJson || "paid".equalsIgnoreCase(statusStr) || "premium".equalsIgnoreCase(statusStr);

                long expiresAt = json.optLong("expires_at", 0);
                if (expiresAt > 0 && expiresAt < 20000000000L) {
                    expiresAt *= 1000;
                }
                String whatsapp = json.optString("whatsapp_number", "");
                String upiId = json.optString("upi_id", "");
                String amount = json.optString("premium_amount", "");

                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("is_registered", true);
                if (!whatsapp.isEmpty()) editor.putString("whatsapp_number", whatsapp);
                if (!upiId.isEmpty()) editor.putString("upi_id", upiId);
                if (!amount.isEmpty()) editor.putString("premium_amount", amount);

                final PaywallConfig config = new PaywallConfig();
                config.deviceId = deviceId;
                config.whatsappNumber = whatsapp;
                config.upiId = upiId;
                config.premiumAmount = amount;
                config.expiresAt = expiresAt;

                if (isPaid) {
                    editor.putBoolean("is_premium", true);
                    editor.putString("cached_status", "paid");
                    editor.apply();

                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (callback != null) callback.onStatusResult(true, false, config);
                        }
                    });
                } else {
                    editor.putBoolean("is_premium", false);
                    if ("trial".equals(statusStr) || isTrial) {
                        editor.putString("cached_status", "trial");
                    } else {
                        editor.putString("cached_status", "expired");
                    }
                    if (expiresAt > 0) {
                        editor.putLong("trial_expires_at", expiresAt);
                    }
                    editor.apply();

                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (callback != null) callback.onStatusResult(false, true, config);
                        }
                    });
                }
            } else {
                handleOfflineFallback(callback);
            }
        } catch (Exception e) {
            handleOfflineFallback(callback);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private void handleOfflineFallback(final StatusCallback callback) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                boolean isPremium = prefs.getBoolean("is_premium", false);
                if (isPremium) {
                    if (callback != null) callback.onStatusResult(true, false, getCachedConfig());
                } else {
                    if (callback != null) callback.onStatusResult(false, true, getCachedConfig());
                }
            }
        });
    }

    public static void fetchQrCodeBitmap(final String upiId, final String amount, final ImageCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection connection = null;
                try {
                    String qrUrlStr = "https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=upi://pay?pa="
                            + upiId + "%26pn=BGTube%26am=" + amount;
                    URL url = new URL(qrUrlStr);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setDoInput(true);
                    connection.connect();
                    InputStream input = connection.getInputStream();
                    final Bitmap bitmap = BitmapFactory.decodeStream(input);

                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            if (callback != null) callback.onImageLoaded(bitmap);
                        }
                    });
                } catch (Exception e) {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            if (callback != null) callback.onImageLoaded(null);
                        }
                    });
                } finally {
                    if (connection != null) {
                        connection.disconnect();
                    }
                }
            }
        }).start();
    }
}