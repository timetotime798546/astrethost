package com.carepulseclinic.app;

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
import java.nio.charset.StandardCharsets;

public class BackendApi {

    private static final String BASE_URL = "https://trumpledroid-api.cloudbeta28624.workers.dev";
    private static final String PREF_NAME = "carepulse_prefs";
    private static final String KEY_TOKEN = "auth_token";
    private static final String KEY_EMAIL = "user_email";

    private final Context context;
    private final Handler mainHandler;

    public interface ApiCallback {
        void onSuccess(JSONObject response);
        void onError(String errorMessage);
    }

    public BackendApi(Context context) {
        this.context = context.getApplicationContext();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public String getAppId() {
        try {
            InputStream is = context.getAssets().open("app-meta.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String jsonStr = new String(buffer, StandardCharsets.UTF_8);
            JSONObject obj = new JSONObject(jsonStr);
            return obj.optString("app_id", "carepulse_clinic_2025");
        } catch (Exception e) {
            return "carepulse_clinic_2025";
        }
    }

    public void saveAuthToken(String token, String email) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_TOKEN, token).putString(KEY_EMAIL, email).apply();
    }

    public String getAuthToken() {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_TOKEN, null);
    }

    public String getUserEmail() {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_EMAIL, "");
    }

    public void clearAuth() {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
    }

    public void registerUser(final String email, final String password, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject body = new JSONObject();
                    body.put("app_id", getAppId());
                    body.put("email", email);
                    body.put("password", password);

                    NetworkResult result = httpRequest(BASE_URL + "/register", "POST", body.toString(), null);
                    if (result.isSuccess()) {
                        JSONObject json = new JSONObject(result.body);
                        sendSuccess(callback, json);
                    } else {
                        sendError(callback, extractError(result.body, "Registration failed"));
                    }
                } catch (Exception e) {
                    sendError(callback, e.getLocalizedMessage());
                }
            }
        }).start();
    }

    public void loginUser(final String email, final String password, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject body = new JSONObject();
                    body.put("app_id", getAppId());
                    body.put("email", email);
                    body.put("password", password);

                    NetworkResult result = httpRequest(BASE_URL + "/login", "POST", body.toString(), null);
                    if (result.isSuccess()) {
                        JSONObject json = new JSONObject(result.body);
                        if (json.optBoolean("success", false) || json.has("token")) {
                            String token = json.optString("token", "");
                            if (!token.isEmpty()) {
                                saveAuthToken(token, email);
                            }
                            sendSuccess(callback, json);
                        } else {
                            sendError(callback, extractError(result.body, "Invalid login credentials"));
                        }
                    } else {
                        sendError(callback, extractError(result.body, "Login failed"));
                    }
                } catch (Exception e) {
                    sendError(callback, e.getLocalizedMessage());
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

                    NetworkResult result = httpRequest(BASE_URL + "/request-otp", "POST", body.toString(), null);
                    if (result.isSuccess()) {
                        JSONObject json = new JSONObject(result.body);
                        sendSuccess(callback, json);
                    } else {
                        sendError(callback, extractError(result.body, "Failed to request OTP"));
                    }
                } catch (Exception e) {
                    sendError(callback, e.getLocalizedMessage());
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

                    NetworkResult result = httpRequest(BASE_URL + "/reset-password", "POST", body.toString(), null);
                    if (result.isSuccess()) {
                        JSONObject json = new JSONObject(result.body);
                        sendSuccess(callback, json);
                    } else {
                        sendError(callback, extractError(result.body, "Failed to reset password"));
                    }
                } catch (Exception e) {
                    sendError(callback, e.getLocalizedMessage());
                }
            }
        }).start();
    }

    public void createAppointment(final JSONObject data, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject payload = new JSONObject();
                    payload.put("collection", "appointments");
                    payload.put("data", data);

                    NetworkResult result = httpRequest(BASE_URL + "/data", "POST", payload.toString(), getAuthToken());
                    if (result.isSuccess()) {
                        JSONObject json = new JSONObject(result.body);
                        if (json.optBoolean("success", false) && json.has("record")) {
                            sendSuccess(callback, json);
                        } else {
                            sendError(callback, extractError(result.body, "Failed to save appointment"));
                        }
                    } else {
                        sendError(callback, extractError(result.body, "Error creating appointment"));
                    }
                } catch (Exception e) {
                    sendError(callback, e.getLocalizedMessage());
                }
            }
        }).start();
    }

    public void getAppointments(final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String url = BASE_URL + "/data?collection=appointments";
                    NetworkResult result = httpRequest(url, "GET", null, getAuthToken());
                    if (result.isSuccess()) {
                        JSONObject json = new JSONObject(result.body);
                        if (json.optBoolean("success", false)) {
                            sendSuccess(callback, json);
                        } else {
                            sendError(callback, extractError(result.body, "Failed to fetch appointments"));
                        }
                    } else {
                        sendError(callback, extractError(result.body, "Error fetching appointments"));
                    }
                } catch (Exception e) {
                    sendError(callback, e.getLocalizedMessage());
                }
            }
        }).start();
    }

    public void updateAppointment(final String recordId, final JSONObject data, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject payload = new JSONObject();
                    payload.put("id", recordId);
                    payload.put("data", data);

                    NetworkResult result = httpRequest(BASE_URL + "/data", "PUT", payload.toString(), getAuthToken());
                    if (result.isSuccess()) {
                        JSONObject json = new JSONObject(result.body);
                        if (json.optBoolean("success", false)) {
                            sendSuccess(callback, json);
                        } else {
                            sendError(callback, extractError(result.body, "Failed to update appointment"));
                        }
                    } else {
                        sendError(callback, extractError(result.body, "Error updating appointment"));
                    }
                } catch (Exception e) {
                    sendError(callback, e.getLocalizedMessage());
                }
            }
        }).start();
    }

    public void deleteAppointment(final String recordId, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String url = BASE_URL + "/data?id=" + recordId;
                    NetworkResult result = httpRequest(url, "DELETE", null, getAuthToken());
                    if (result.isSuccess()) {
                        JSONObject json = new JSONObject(result.body);
                        if (json.optBoolean("success", false)) {
                            sendSuccess(callback, json);
                        } else {
                            sendError(callback, extractError(result.body, "Failed to delete appointment"));
                        }
                    } else {
                        sendError(callback, extractError(result.body, "Error deleting appointment"));
                    }
                } catch (Exception e) {
                    sendError(callback, e.getLocalizedMessage());
                }
            }
        }).start();
    }

    private NetworkResult httpRequest(String urlStr, String method, String jsonBody, String token) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setConnectTimeout(12000);
        conn.setReadTimeout(12000);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");

        if (token != null && !token.isEmpty()) {
            conn.setRequestProperty("Authorization", "Bearer " + token);
        }

        if (jsonBody != null && (method.equals("POST") || method.equals("PUT"))) {
            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
        }

        int statusCode = conn.getResponseCode();
        InputStream is = (statusCode >= 200 && statusCode < 300) ? conn.getInputStream() : conn.getErrorStream();

        String responseBody = "";
        if (is != null) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            responseBody = sb.toString();
        }

        return new NetworkResult(statusCode, responseBody);
    }

    private String extractError(String responseBody, String defaultMsg) {
        try {
            if (responseBody != null && !responseBody.isEmpty()) {
                JSONObject json = new JSONObject(responseBody);
                if (json.has("error")) {
                    return json.getString("error");
                }
                if (json.has("message")) {
                    return json.getString("message");
                }
            }
        } catch (Exception ignored) {
        }
        return defaultMsg;
    }

    private void sendSuccess(final ApiCallback callback, final JSONObject response) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onSuccess(response);
            }
        });
    }

    private void sendError(final ApiCallback callback, final String message) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onError(message);
            }
        });
    }

    private static class NetworkResult {
        int code;
        String body;

        NetworkResult(int code, String body) {
            this.code = code;
            this.body = body;
        }

        boolean isSuccess() {
            return code >= 200 && code < 300;
        }
    }
}