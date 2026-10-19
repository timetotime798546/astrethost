package com.quickpaywallet.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class AdminConfigManager {

    private static final String PREF_NAME = "quickpay_admin_config_cache";
    private static final String KEY_CACHED_JSON = "cached_config_json";
    private static final String KEY_TIMESTAMP = "cached_timestamp";
    private static final long CACHE_DURATION_MS = 3600 * 1000L; // 1 Hour Caching Rule

    private static final String ADMIN_CONFIG_URL =
            "https://trumpledroid-apps-admin-47at498aq98hrg9633fugqd3.cloudbeta28624.workers.dev/get-app-config?app_id=";

    public interface ConfigCallback {
        void onConfigLoaded(ConfigData config);
    }

    public static class ConfigData {
        public String merchantName = "QuickPay Official Merchant";
        public String merchantUpiId = "quickpay.merchant@upi";
        public String qrCodeUrl = "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?w=400&q=80";
        public double minDepositLimit = 10.0;
        public double maxDepositLimit = 10000.0;
        public double cashbackPercent = 2.5;

        public static ConfigData fromJson(JSONObject json) {
            ConfigData c = new ConfigData();
            if (json == null) return c;
            c.merchantName = json.optString("merchant_name", c.merchantName);
            c.merchantUpiId = json.optString("merchant_upi_id", c.merchantUpiId);
            c.qrCodeUrl = json.optString("qr_code_url", c.qrCodeUrl);
            c.minDepositLimit = json.optDouble("min_deposit_limit", c.minDepositLimit);
            c.maxDepositLimit = json.optDouble("max_deposit_limit", c.maxDepositLimit);
            c.cashbackPercent = json.optDouble("cashback_percent", c.cashbackPercent);
            return c;
        }
    }

    public static void loadConfig(final Context context, final ConfigCallback callback) {
        final SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        long lastFetchedTime = prefs.getLong(KEY_TIMESTAMP, 0L);
        long currentTime = System.currentTimeMillis();
        String cachedJson = prefs.getString(KEY_CACHED_JSON, null);

        // Enforce 1-Hour Cache Duration Rule
        if (cachedJson != null && (currentTime - lastFetchedTime) < CACHE_DURATION_MS) {
            try {
                JSONObject obj = new JSONObject(cachedJson);
                callback.onConfigLoaded(ConfigData.fromJson(obj));
                return;
            } catch (Exception ignored) {
            }
        }

        // Cache missing or expired: Fetch fresh config in background thread
        final String appId = BackendApi.getAppId(context);
        final Handler mainHandler = new Handler(Looper.getMainLooper());

        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection conn = null;
                ConfigData resultConfig = null;
                try {
                    URL url = new URL(ADMIN_CONFIG_URL + appId);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(8000);
                    conn.setReadTimeout(8000);

                    int code = conn.getResponseCode();
                    if (code == 200) {
                        InputStream in = conn.getInputStream();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            sb.append(line);
                        }
                        reader.close();

                        String responseBody = sb.toString();
                        JSONObject rootObj = new JSONObject(responseBody);
                        JSONObject valuesObj = rootObj.optJSONObject("data");
                        if (valuesObj == null) {
                            valuesObj = rootObj;
                        }

                        // Save fresh settings to cache
                        prefs.edit()
                                .putString(KEY_CACHED_JSON, valuesObj.toString())
                                .putLong(KEY_TIMESTAMP, System.currentTimeMillis())
                                .apply();

                        resultConfig = ConfigData.fromJson(valuesObj);
                    }
                } catch (Exception ignored) {
                } finally {
                    if (conn != null) conn.disconnect();
                }

                // If network fetch failed, fall back to cached data or default
                if (resultConfig == null) {
                    String existing = prefs.getString(KEY_CACHED_JSON, null);
                    if (existing != null) {
                        try {
                            resultConfig = ConfigData.fromJson(new JSONObject(existing));
                        } catch (Exception ignored) {
                            resultConfig = new ConfigData();
                        }
                    } else {
                        resultConfig = new ConfigData();
                    }
                }

                final ConfigData finalConfig = resultConfig;
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onConfigLoaded(finalConfig);
                    }
                });
            }
        }).start();
    }
}