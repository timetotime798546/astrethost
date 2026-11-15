package com.lockplaytube.app;

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
    public static final String KEY_IS_PREMIUM = "is_premium";
    public static final String KEY_WHATSAPP_NUMBER = "whatsapp_number";
    public static final String KEY_UPI_ID = "upi_id";
    public static final String KEY_PREMIUM_AMOUNT = "premium_amount";
    public static final String KEY_MESSAGE = "message";

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
        return isPremium(context);
    }

    public String getWhatsappNumber(Context context) {
        return getPrefs(context).getString(KEY_WHATSAPP_NUMBER, "");
    }

    public String getUpiId(Context context) {
        return getPrefs(context).getString(KEY_UPI_ID, "");
    }

    public String getPremiumAmount(Context context) {
        return getPrefs(context).getString(KEY_PREMIUM_AMOUNT, "");
    }

    public String getWorkerMessage(Context context) {
        return getPrefs(context).getString(KEY_MESSAGE, "Premium Plan Required");
    }

    public void checkAccessStatus(final Context context, final StatusCallback callback) {
        boolean isPremium = isPremium(context);
        if (isPremium) {
            notifyCallback(callback, STATUS_GRANTED, "Lifetime Access Granted");
            return;
        }

        queryWorkerServer(context, callback);
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
                        String whatsappNumber = json.optString("whatsapp_number", "");
                        String upiId = json.optString("upi_id", "");
                        String premiumAmount = json.optString("premium_amount", "");
                        String message = json.optString("message", "Premium Plan Required");

                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putBoolean(KEY_IS_PREMIUM, isPremium);
                        if (whatsappNumber != null && !whatsappNumber.trim().isEmpty()) {
                            editor.putString(KEY_WHATSAPP_NUMBER, whatsappNumber);
                        }
                        if (upiId != null && !upiId.trim().isEmpty()) {
                            editor.putString(KEY_UPI_ID, upiId);
                        }
                        if (premiumAmount != null && !premiumAmount.trim().isEmpty()) {
                            editor.putString(KEY_PREMIUM_AMOUNT, premiumAmount);
                        }
                        if (message != null && !message.trim().isEmpty()) {
                            editor.putString(KEY_MESSAGE, message);
                        }
                        editor.apply();

                        if (isPremium) {
                            notifyCallback(callback, STATUS_GRANTED, "Lifetime Access Unlocked");
                        } else {
                            notifyCallback(callback, STATUS_EXPIRED, message);
                        }
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
        } else {
            notifyCallback(callback, STATUS_EXPIRED, "Premium Plan Required");
        }
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