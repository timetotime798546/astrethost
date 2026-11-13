package com.bgtubeplayer.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

public class TrialPaywallManager {

    private static final String PREF_NAME = "bg_tube_paywall_prefs";
    private static final String WORKER_BASE_URL = "https://bgtubeapizeedude.newtimenew3689-fd2.workers.dev/?device_id=";

    public static final String KEY_DEVICE_ID = "device_id";
    public static final String KEY_IS_REGISTERED = "is_registered";
    public static final String KEY_IS_PREMIUM = "is_premium";
    public static final String KEY_CACHED_STATUS = "cached_status";
    public static final String KEY_TRIAL_EXPIRES_AT = "trial_expires_at";
    public static final String KEY_WHATSAPP_NUMBER = "whatsapp_number";
    public static final String KEY_UPI_ID = "upi_id";
    public static final String KEY_PREMIUM_AMOUNT = "premium_amount";

    public static final int STATUS_GRANTED = 1;
    public static final int STATUS_EXPIRED = 2;

    public interface StatusCallback {
        void onStatusResult(int status, String message);
    }

    private SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public String getDeviceId(Context context) {
        SharedPreferences prefs = getPrefs(context);
        String deviceId = prefs.getString(KEY_DEVICE_ID, null);
        if (deviceId == null || deviceId.trim().isEmpty()) {
            try {
                deviceId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (deviceId == null || deviceId.trim().isEmpty() || "9774d56d682e549c".equals(deviceId)) {
                deviceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            }
            prefs.edit().putString(KEY_DEVICE_ID, deviceId).apply();
        }
        return deviceId;
    }

    public boolean isPremium(Context context) {
        return getPrefs(context).getBoolean(KEY_IS_PREMIUM, false);
    }

    public boolean isAccessGranted(Context context) {
        SharedPreferences prefs = getPrefs(context);
        boolean isPremium = prefs.getBoolean(KEY_IS_PREMIUM, false);
        if (isPremium) {
            return true;
        }

        String cachedStatus = prefs.getString(KEY_CACHED_STATUS, "");
        if ("expired".equals(cachedStatus)) {
            return false;
        }

        if ("trial".equals(cachedStatus) || "active".equals(cachedStatus)) {
            long trialExpiresAt = prefs.getLong(KEY_TRIAL_EXPIRES_AT, 0);
            if (trialExpiresAt > 0 && System.currentTimeMillis() > trialExpiresAt) {
                prefs.edit().putString(KEY_CACHED_STATUS, "expired").apply();
                return false;
            }
            return true;
        }

        return false;
    }

    public String getWhatsappNumber(Context context) {
        return getPrefs(context).getString(KEY_WHATSAPP_NUMBER, "919876543210");
    }

    public String getUpiId(Context context) {
        return getPrefs(context).getString(KEY_UPI_ID, "bgtube@upi");
    }

    public String getPremiumAmount(Context context) {
        return getPrefs(context).getString(KEY_PREMIUM_AMOUNT, "₹199");
    }

    public void checkAccessStatus(final Context context, final StatusCallback callback) {
        final SharedPreferences prefs = getPrefs(context);

        // 1. LOCAL CACHE & STRICT EXPIRY CHECK FIRST
        boolean isPremium = prefs.getBoolean(KEY_IS_PREMIUM, false);
        if (isPremium) {
            notifyCallback(callback, STATUS_GRANTED, "Lifetime Access Granted");
            return;
        }

        String cachedStatus = prefs.getString(KEY_CACHED_STATUS, "");
        if ("expired".equals(cachedStatus)) {
            notifyCallback(callback, STATUS_EXPIRED, "Trial Expired");
            return;
        }

        if ("trial".equals(cachedStatus) || "active".equals(cachedStatus)) {
            long trialExpiresAt = prefs.getLong(KEY_TRIAL_EXPIRES_AT, 0);
            if (trialExpiresAt > 0 && System.currentTimeMillis() > trialExpiresAt) {
                prefs.edit().putString(KEY_CACHED_STATUS, "expired").apply();
                notifyCallback(callback, STATUS_EXPIRED, "Trial Expired");
                return;
            }
        }

        // 2. CONDITIONAL NETWORK CALL
        boolean isRegistered = prefs.getBoolean(KEY_IS_REGISTERED, false);
        if (!isRegistered || "active".equals(cachedStatus) || "trial".equals(cachedStatus) || cachedStatus.isEmpty()) {
            queryWorkerServer(context, callback);
        } else {
            notifyCallback(callback, STATUS_GRANTED, "Trial Active");
        }
    }

    public void recheckAccessStatus(final Context context, final StatusCallback callback) {
        queryWorkerServer(context, callback);
    }

    private void queryWorkerServer(final Context context, final StatusCallback callback) {
        final String deviceId = getDeviceId(context);
        final SharedPreferences prefs = getPrefs(context);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String urlStr = WORKER_BASE_URL + Uri.encode(deviceId);
                    URL url = new URL(urlStr);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(9000);
                    conn.setReadTimeout(9000);

                    int responseCode = conn.getResponseCode();
                    if (responseCode == 200) {
                        InputStream is = conn.getInputStream();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            sb.append(line);
                        }
                        reader.close();
                        is.close();

                        JSONObject json = new JSONObject(sb.toString());
                        boolean isPremium = json.optBoolean("is_premium", false);
                        boolean isTrial = json.optBoolean("is_trial", false);
                        long expiresAt = json.optLong("expires_at", 0);
                        long remainingSeconds = json.optLong("remaining_seconds", 0);
                        String whatsappNumber = json.optString("whatsapp_number", "919876543210");
                        String upiId = json.optString("upi_id", "bgtube@upi");
                        String premiumAmount = json.optString("premium_amount", "₹199");

                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putBoolean(KEY_IS_REGISTERED, true);
                        if (whatsappNumber != null && !whatsappNumber.trim().isEmpty()) {
                            editor.putString(KEY_WHATSAPP_NUMBER, whatsappNumber);
                        }
                        if (upiId != null && !upiId.trim().isEmpty()) {
                            editor.putString(KEY_UPI_ID, upiId);
                        }
                        if (premiumAmount != null && !premiumAmount.trim().isEmpty()) {
                            editor.putString(KEY_PREMIUM_AMOUNT, premiumAmount);
                        }

                        if (isPremium) {
                            editor.putBoolean(KEY_IS_PREMIUM, true);
                            editor.putString(KEY_CACHED_STATUS, "paid");
                            editor.apply();
                            notifyCallback(callback, STATUS_GRANTED, "Lifetime Access Unlocked");
                            return;
                        }

                        if (isTrial) {
                            long expiresAtMs = 0;
                            if (expiresAt > 0) {
                                expiresAtMs = (expiresAt < 2000000000L) ? (expiresAt * 1000L) : expiresAt;
                            } else if (remainingSeconds > 0) {
                                expiresAtMs = System.currentTimeMillis() + (remainingSeconds * 1000L);
                            } else {
                                expiresAtMs = System.currentTimeMillis() + (24 * 60 * 60 * 1000L);
                            }

                            editor.putLong(KEY_TRIAL_EXPIRES_AT, expiresAtMs);
                            editor.putString(KEY_CACHED_STATUS, "active");
                            editor.apply();

                            if (System.currentTimeMillis() > expiresAtMs) {
                                editor.putString(KEY_CACHED_STATUS, "expired").apply();
                                notifyCallback(callback, STATUS_EXPIRED, "Trial Expired");
                            } else {
                                notifyCallback(callback, STATUS_GRANTED, "Trial Active");
                            }
                            return;
                        }

                        editor.putString(KEY_CACHED_STATUS, "expired");
                        editor.apply();
                        notifyCallback(callback, STATUS_EXPIRED, "Trial Expired");

                    } else {
                        handleOfflineFallback(context, prefs, callback);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    handleOfflineFallback(context, prefs, callback);
                }
            }
        }).start();
    }

    private void handleOfflineFallback(Context context, SharedPreferences prefs, StatusCallback callback) {
        boolean isPremium = prefs.getBoolean(KEY_IS_PREMIUM, false);
        if (isPremium) {
            notifyCallback(callback, STATUS_GRANTED, "Lifetime Access Granted");
            return;
        }

        String cachedStatus = prefs.getString(KEY_CACHED_STATUS, "");
        if ("expired".equals(cachedStatus)) {
            notifyCallback(callback, STATUS_EXPIRED, "Trial Expired");
            return;
        }

        if ("trial".equals(cachedStatus) || "active".equals(cachedStatus)) {
            long trialExpiresAt = prefs.getLong(KEY_TRIAL_EXPIRES_AT, 0);
            if (trialExpiresAt > 0 && System.currentTimeMillis() > trialExpiresAt) {
                prefs.edit().putString(KEY_CACHED_STATUS, "expired").apply();
                notifyCallback(callback, STATUS_EXPIRED, "Trial Expired");
            } else {
                notifyCallback(callback, STATUS_GRANTED, "Trial Active (Offline)");
            }
            return;
        }

        notifyCallback(callback, STATUS_EXPIRED, "Trial Expired");
    }

    private void notifyCallback(final StatusCallback callback, final int status, final String message) {
        if (callback != null) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    callback.onStatusResult(status, message);
                }
            });
        }
    }
}