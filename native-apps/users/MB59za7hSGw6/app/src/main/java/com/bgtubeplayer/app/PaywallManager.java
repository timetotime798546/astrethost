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

    public void checkAccess(final boolean forceNetwork, final StatusCallback callback) {
        final boolean isPremium = prefs.getBoolean("is_premium", false);
        if (isPremium) {
            if (callback != null) {
                callback.onStatusResult(true, false, getCachedConfig());
            }
            return;
        }

        String cachedStatus = prefs.getString("cached_status", "");
        long trialExpiresAt = prefs.getLong("trial_expires_at", 0);
        boolean isRegistered = prefs.getBoolean("is_registered", false);

        if (!forceNetwork) {
            if ("expired".equals(cachedStatus)) {
                if (callback != null) {
                    callback.onStatusResult(false, true, getCachedConfig());
                }
                return;
            }

            if ("trial".equals(cachedStatus) || "active".equals(cachedStatus)) {
                if (trialExpiresAt > 0 && System.currentTimeMillis() > trialExpiresAt) {
                    prefs.edit().putString("cached_status", "expired").apply();
                    if (callback != null) {
                        callback.onStatusResult(false, true, getCachedConfig());
                    }
                    return;
                }
            }
        }

        if (isRegistered && !forceNetwork && ("trial".equals(cachedStatus) || "active".equals(cachedStatus))) {
            if (callback != null) {
                callback.onStatusResult(false, false, getCachedConfig());
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
                boolean isPremium = json.optBoolean("is_premium", false);
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

                if (isPremium) {
                    editor.putBoolean("is_premium", true);
                    editor.putString("cached_status", "paid");
                    editor.apply();

                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (callback != null) callback.onStatusResult(true, false, config);
                        }
                    });
                } else if (isTrial) {
                    editor.putLong("trial_expires_at", expiresAt);
                    editor.putString("cached_status", "active");
                    editor.apply();

                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (callback != null) callback.onStatusResult(false, false, config);
                        }
                    });
                } else {
                    editor.putString("cached_status", "expired");
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
                String cachedStatus = prefs.getString("cached_status", "");
                long trialExpiresAt = prefs.getLong("trial_expires_at", 0);

                if (isPremium) {
                    if (callback != null) callback.onStatusResult(true, false, getCachedConfig());
                } else if ("expired".equals(cachedStatus) || (trialExpiresAt > 0 && System.currentTimeMillis() > trialExpiresAt)) {
                    prefs.edit().putString("cached_status", "expired").apply();
                    if (callback != null) callback.onStatusResult(false, true, getCachedConfig());
                } else {
                    if (callback != null) callback.onStatusResult(false, false, getCachedConfig());
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