package com.omniflow.erp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class BackendApi {
    private static final String TAG = "BackendApi";
    private static final String PREFS_NAME = "OmniFlowPrefs";
    private static final String KEY_TOKEN = "auth_token";
    private static final String KEY_EMAIL = "auth_email";

    private final Context context;
    private final Handler mainHandler;
    private String baseUrl = "https://trumpledroid-api.cloudbeta28624.workers.dev";
    private String appId = "";

    public interface ApiCallback {
        void onSuccess(JSONObject response);
        void onError(String message);
    }

    public BackendApi(Context context) {
        this.context = context.getApplicationContext();
        this.mainHandler = new Handler(Looper.getMainLooper());
        loadAppMetadata();
    }

    private void loadAppMetadata() {
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
            this.appId = json.optString("app_id", json.optString("package_name", "com.omniflow.erp"));
            Log.d(TAG, "Loaded app metadata. appId: " + appId);
        } catch (Exception e) {
            Log.e(TAG, "Failed to load app metadata from assets", e);
            this.appId = "com.omniflow.erp";
        }
    }

    public void setToken(String token) {
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putString(KEY_TOKEN, token);
        editor.apply();
    }

    public String getToken() {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_TOKEN, null);
    }

    public void setEmail(String email) {
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putString(KEY_EMAIL, email);
        editor.apply();
    }

    public String getEmail() {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_EMAIL, "User");
    }

    public void clearSession() {
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.remove(KEY_TOKEN);
        editor.remove(KEY_EMAIL);
        editor.apply();
    }

    public boolean isLoggedIn() {
        return getToken() != null;
    }

    private void executeRequest(final String endpoint, final String method, final JSONObject payload, final boolean authenticated, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection conn = null;
                try {
                    URL url = new URL(baseUrl + endpoint);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod(method);
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setRequestProperty("Accept", "application/json");
                    conn.setConnectTimeout(15000);
                    conn.setReadTimeout(15000);

                    if (authenticated) {
                        String token = getToken();
                        if (token != null) {
                            conn.setRequestProperty("Authorization", "Bearer " + token);
                        }
                    }

                    if (payload != null && (method.equals("POST") || method.equals("PUT"))) {
                        conn.setDoOutput(true);
                        OutputStream os = conn.getOutputStream();
                        os.write(payload.toString().getBytes("UTF-8"));
                        os.close();
                    }

                    int responseCode = conn.getResponseCode();
                    InputStream is;
                    if (responseCode >= 200 && responseCode < 300) {
                        is = conn.getInputStream();
                    } else {
                        is = conn.getErrorStream();
                    }

                    final StringBuilder response = new StringBuilder();
                    if (is != null) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                        reader.close();
                    }

                    final String resStr = response.toString();
                    Log.d(TAG, "Response from " + endpoint + " [" + responseCode + "]: " + resStr);

                    if (responseCode >= 200 && responseCode < 300) {
                        final JSONObject jsonRes = new JSONObject(resStr);
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess(jsonRes);
                            }
                        });
                    } else {
                        String errMsg = "Error " + responseCode;
                        try {
                            JSONObject jsonRes = new JSONObject(resStr);
                            errMsg = jsonRes.optString("error", jsonRes.optString("message", errMsg));
                        } catch (Exception ignored) {}
                        final String finalErr = errMsg;
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onError(finalErr);
                            }
                        });
                    }
                } catch (final Exception e) {
                    Log.e(TAG, "Request failed", e);
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onError(e.getMessage() != null ? e.getMessage() : "Network error");
                        }
                    });
                } finally {
                    if (conn != null) {
                        conn.disconnect();
                    }
                }
            }
        }).start();
    }

    public void register(String email, String password, ApiCallback callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("app_id", appId);
            body.put("email", email);
            body.put("password", password);
            executeRequest("/register", "POST", body, false, callback);
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    public void login(String email, String password, ApiCallback callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("app_id", appId);
            body.put("email", email);
            body.put("password", password);
            executeRequest("/login", "POST", body, false, callback);
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    public void requestOtp(String email, ApiCallback callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("app_id", appId);
            body.put("email", email);
            executeRequest("/request-otp", "POST", body, false, callback);
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    public void resetPassword(String email, String otp, String newPassword, ApiCallback callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("app_id", appId);
            body.put("email", email);
            body.put("otp", otp);
            body.put("new_password", newPassword);
            executeRequest("/reset-password", "POST", body, false, callback);
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    public void logout(ApiCallback callback) {
        executeRequest("/logout", "POST", null, true, callback);
    }

    public void createRecord(String collection, JSONObject data, ApiCallback callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("collection", collection);
            body.put("data", data);
            executeRequest("/data", "POST", body, true, callback);
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    public void readRecords(String collection, ApiCallback callback) {
        String endpoint = "/data?collection=" + collection;
        executeRequest(endpoint, "GET", null, true, callback);
    }

    public void updateRecord(String id, JSONObject data, ApiCallback callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("id", id);
            body.put("data", data);
            executeRequest("/data", "PUT", body, true, callback);
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    public void deleteRecord(String id, ApiCallback callback) {
        String endpoint = "/data?id=" + id;
        executeRequest(endpoint, "DELETE", null, true, callback);
    }
}