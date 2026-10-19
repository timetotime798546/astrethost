package com.tapcoinarcade.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class BackendApi {

    public interface ApiCallback {
        void onSuccess(JSONObject response);
        void onError(String errorMessage);
    }

    private static final String API_BASE_URL = "https://trumpledroid-api.cloudbeta28624.workers.dev";
    private static final String ADMIN_CONFIG_URL = "https://trumpledroid-apps-admin-47at498aq98hrg9633fugqd3.cloudbeta28624.workers.dev/get-app-config";
    private static final String PREFS_NAME = "tap_arcade_prefs";
    private static final String KEY_TOKEN = "auth_token";
    private static final String KEY_EMAIL = "logged_email";
    private static final String KEY_ADMIN_CONFIG = "cached_admin_config";
    private static final String KEY_ADMIN_CONFIG_TIME = "cached_admin_config_time";
    private static final long CACHE_DURATION_MS = 3600 * 1000L; // 1-Hour Admin Caching Rule

    private final Context context;
    private final Handler mainHandler;

    public BackendApi(Context context) {
        this.context = context.getApplicationContext();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public String getAppId() {
        try {
            InputStream is = context.getAssets().open("app-meta.json");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            JSONObject json = new JSONObject(sb.toString());
            return json.optString("app_id", "tap_coin_arcade_game_v1");
        } catch (Exception e) {
            return "tap_coin_arcade_game_v1";
        }
    }

    public void saveAuthSession(String token, String email) {
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        sp.edit().putString(KEY_TOKEN, token).putString(KEY_EMAIL, email).apply();
    }

    public String getAuthToken() {
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return sp.getString(KEY_TOKEN, null);
    }

    public String getLoggedEmail() {
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return sp.getString(KEY_EMAIL, null);
    }

    public void clearAuthSession() {
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        sp.edit().remove(KEY_TOKEN).remove(KEY_EMAIL).apply();
    }

    public boolean isLoggedIn() {
        String token = getAuthToken();
        return token != null && !token.trim().isEmpty();
    }

    // POST /register
    public void register(final String email, final String password, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject body = new JSONObject();
                    body.put("app_id", getAppId());
                    body.put("email", email);
                    body.put("password", password);
                    executeRequest(API_BASE_URL + "/register", "POST", body.toString(), null, callback);
                } catch (Exception e) {
                    dispatchError(callback, "Registration error: " + e.getMessage());
                }
            }
        }).start();
    }

    // POST /login
    public void login(final String email, final String password, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject body = new JSONObject();
                    body.put("app_id", getAppId());
                    body.put("email", email);
                    body.put("password", password);
                    executeRequest(API_BASE_URL + "/login", "POST", body.toString(), null, callback);
                } catch (Exception e) {
                    dispatchError(callback, "Login error: " + e.getMessage());
                }
            }
        }).start();
    }

    // POST /logout
    public void logout(final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String token = getAuthToken();
                executeRequest(API_BASE_URL + "/logout", "POST", "{}", token, new ApiCallback() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        clearAuthSession();
                        if (callback != null) callback.onSuccess(response);
                    }

                    @Override
                    public void onError(String errorMessage) {
                        clearAuthSession();
                        if (callback != null) callback.onError(errorMessage);
                    }
                });
            }
        }).start();
    }

    // POST /request-otp (Password Reset Step 1)
    public void requestOtp(final String email, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject body = new JSONObject();
                    body.put("app_id", getAppId());
                    body.put("email", email);
                    executeRequest(API_BASE_URL + "/request-otp", "POST", body.toString(), null, callback);
                } catch (Exception e) {
                    dispatchError(callback, "OTP error: " + e.getMessage());
                }
            }
        }).start();
    }

    // POST /reset-password (Password Reset Step 2)
    public void resetPassword(final String email, final String otp, final String newPassword, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject body = new JSONObject();
                    body.put("app_id", getAppId());
                    body.put("email", email);
                    body.put("otp", otp);
                    body.put("new_password", newPassword);
                    executeRequest(API_BASE_URL + "/reset-password", "POST", body.toString(), null, callback);
                } catch (Exception e) {
                    dispatchError(callback, "Reset error: " + e.getMessage());
                }
            }
        }).start();
    }

    // SECURE ACTION-BASED BALANCE UPDATE: POST /app-action
    public void executeAppAction(final String action, final double amount, final String purpose, final String collectionName, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject body = new JSONObject();
                    body.put("action", action);
                    body.put("amount", amount);
                    body.put("purpose", purpose);
                    body.put("collection_name", collectionName);
                    executeRequest(API_BASE_URL + "/app-action", "POST", body.toString(), getAuthToken(), callback);
                } catch (Exception e) {
                    dispatchError(callback, "Action error: " + e.getMessage());
                }
            }
        }).start();
    }

    // Dynamic Admin Config with 1-Hour Caching Rule
    public void fetchAdminConfig(final ApiCallback callback) {
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long lastTime = sp.getLong(KEY_ADMIN_CONFIG_TIME, 0);
        String cachedJson = sp.getString(KEY_ADMIN_CONFIG, null);
        long now = System.currentTimeMillis();

        if (cachedJson != null && (now - lastTime < CACHE_DURATION_MS)) {
            try {
                JSONObject cachedObj = new JSONObject(cachedJson);
                dispatchSuccess(callback, cachedObj);
                return;
            } catch (Exception ignored) {
            }
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                String url = ADMIN_CONFIG_URL + "?app_id=" + getAppId();
                executeRequest(url, "GET", null, null, new ApiCallback() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                        sp.edit()
                                .putString(KEY_ADMIN_CONFIG, response.toString())
                                .putLong(KEY_ADMIN_CONFIG_TIME, System.currentTimeMillis())
                                .apply();
                        dispatchSuccess(callback, response);
                    }

                    @Override
                    public void onError(String errorMessage) {
                        dispatchError(callback, errorMessage);
                    }
                });
            }
        }).start();
    }

    private void executeRequest(String endpoint, String method, String jsonBody, String token, ApiCallback callback) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(endpoint);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(method);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");

            if (token != null && !token.trim().isEmpty()) {
                conn.setRequestProperty("Authorization", "Bearer " + token.trim());
            }

            if (jsonBody != null && (method.equals("POST") || method.equals("PUT"))) {
                conn.setDoOutput(true);
                OutputStream os = conn.getOutputStream();
                os.write(jsonBody.getBytes("UTF-8"));
                os.flush();
                os.close();
            }

            int responseCode = conn.getResponseCode();
            InputStream is = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream();

            StringBuilder sb = new StringBuilder();
            if (is != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();
            }

            String responseString = sb.toString().trim();
            JSONObject jsonResponse;
            if (responseString.startsWith("{")) {
                jsonResponse = new JSONObject(responseString);
            } else if (responseString.startsWith("[")) {
                jsonResponse = new JSONObject();
                jsonResponse.put("array", new JSONArray(responseString));
                jsonResponse.put("success", true);
            } else {
                jsonResponse = new JSONObject();
                jsonResponse.put("success", responseCode >= 200 && responseCode < 300);
                jsonResponse.put("message", responseString);
            }

            if (responseCode >= 200 && responseCode < 300) {
                dispatchSuccess(callback, jsonResponse);
            } else {
                String errorMsg = jsonResponse.optString("error", jsonResponse.optString("message", "HTTP Error: " + responseCode));
                dispatchError(callback, errorMsg);
            }

        } catch (Exception e) {
            dispatchError(callback, "Network request failed: " + e.getMessage());
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private void dispatchSuccess(final ApiCallback callback, final JSONObject response) {
        if (callback == null) return;
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onSuccess(response);
            }
        });
    }

    private void dispatchError(final ApiCallback callback, final String message) {
        if (callback == null) return;
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onError(message);
            }
        });
    }
}