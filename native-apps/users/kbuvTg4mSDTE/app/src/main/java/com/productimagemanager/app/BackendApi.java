package com.productimagemanager.app;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class BackendApi {

    public static final String BASE_URL = "https://trumpledroid-api.cloudbeta28624.workers.dev";
    private static final String PREF_NAME = "auth_prefs";
    private static final String KEY_TOKEN = "auth_token";
    private static final String KEY_EMAIL = "auth_email";

    private final Context context;

    public interface ApiCallback {
        void onSuccess(String response);
        void onError(String error);
    }

    public BackendApi(Context context) {
        this.context = context.getApplicationContext();
    }

    public String getAppId() {
        try {
            InputStream is = context.getAssets().open("app-meta.json");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            JSONObject obj = new JSONObject(sb.toString());
            return obj.optString("app_id", "product-image-manager");
        } catch (Exception e) {
            return "product-image-manager";
        }
    }

    public void saveSession(String token, String email) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sp.edit().putString(KEY_TOKEN, token).putString(KEY_EMAIL, email).apply();
    }

    public String getToken() {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sp.getString(KEY_TOKEN, "");
    }

    public String getEmail() {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sp.getString(KEY_EMAIL, "");
    }

    public boolean isLoggedIn() {
        String token = getToken();
        return token != null && !token.trim().isEmpty();
    }

    public void clearSession() {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sp.edit().clear().apply();
    }

    public void register(final String email, final String password, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject body = new JSONObject();
                    body.put("app_id", getAppId());
                    body.put("email", email);
                    body.put("password", password);

                    String response = executePostJson(BASE_URL + "/register", body.toString(), null);
                    JSONObject respJson = new JSONObject(response);
                    if (respJson.optBoolean("success", false)) {
                        callback.onSuccess(response);
                    } else {
                        String err = respJson.optString("error", respJson.optString("message", "Registration failed"));
                        callback.onError(err);
                    }
                } catch (Exception e) {
                    callback.onError("Registration error: " + e.getMessage());
                }
            }
        }).start();
    }

    public void login(final String email, final String password, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject body = new JSONObject();
                    body.put("app_id", getAppId());
                    body.put("email", email);
                    body.put("password", password);

                    String response = executePostJson(BASE_URL + "/login", body.toString(), null);
                    JSONObject respJson = new JSONObject(response);
                    if (respJson.optBoolean("success", false) || respJson.has("token")) {
                        String token = respJson.optString("token", "");
                        if (!token.isEmpty()) {
                            saveSession(token, email);
                            callback.onSuccess(response);
                        } else {
                            callback.onError("Login response missing token");
                        }
                    } else {
                        String err = respJson.optString("error", respJson.optString("message", "Invalid credentials"));
                        callback.onError(err);
                    }
                } catch (Exception e) {
                    callback.onError("Login error: " + e.getMessage());
                }
            }
        }).start();
    }

    public void logout(final ApiCallback callback) {
        final String token = getToken();
        clearSession();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (!token.isEmpty()) {
                        executePostJson(BASE_URL + "/logout", "{}", token);
                    }
                    callback.onSuccess("Logged out");
                } catch (Exception e) {
                    callback.onSuccess("Logged out locally");
                }
            }
        }).start();
    }

    public void requestOtp(final String email, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject body = new JSONObject();
                    body.put("app_id", getAppId());
                    body.put("email", email);

                    String response = executePostJson(BASE_URL + "/request-otp", body.toString(), null);
                    JSONObject respJson = new JSONObject(response);
                    if (respJson.optBoolean("success", false)) {
                        callback.onSuccess(response);
                    } else {
                        String err = respJson.optString("error", respJson.optString("message", "Failed to send OTP"));
                        callback.onError(err);
                    }
                } catch (Exception e) {
                    callback.onError("Request OTP error: " + e.getMessage());
                }
            }
        }).start();
    }

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

                    String response = executePostJson(BASE_URL + "/reset-password", body.toString(), null);
                    JSONObject respJson = new JSONObject(response);
                    if (respJson.optBoolean("success", false)) {
                        callback.onSuccess(response);
                    } else {
                        String err = respJson.optString("error", respJson.optString("message", "Failed to reset password"));
                        callback.onError(err);
                    }
                } catch (Exception e) {
                    callback.onError("Reset Password error: " + e.getMessage());
                }
            }
        }).start();
    }

    private String executePostJson(String targetUrl, String jsonBody, String authToken) throws Exception {
        URL url = new URL(targetUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);
        conn.setDoOutput(true);

        if (authToken != null && !authToken.trim().isEmpty()) {
            conn.setRequestProperty("Authorization", "Bearer " + authToken);
        }

        OutputStream os = conn.getOutputStream();
        os.write(jsonBody.getBytes("UTF-8"));
        os.flush();
        os.close();

        int responseCode = conn.getResponseCode();
        InputStream is = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream();

        if (is == null) {
            throw new Exception("HTTP error code: " + responseCode);
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();
        conn.disconnect();

        return sb.toString();
    }
}