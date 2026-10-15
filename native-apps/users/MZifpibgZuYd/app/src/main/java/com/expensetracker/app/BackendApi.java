package com.expensetracker.app;

import android.content.Context;
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
    private static final String API_BASE_URL = "https://trumpledroid-api.cloudbeta28624.workers.dev";
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
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            JSONObject obj = new JSONObject(sb.toString());
            return obj.optString("app_id", "expense-tracker-prod");
        } catch (Exception e) {
            return "expense-tracker-prod";
        }
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

                    NetworkResult res = executeRequest("POST", "/register", body.toString(), null);
                    handleResponse(res, callback, false);
                } catch (final Exception e) {
                    postError(callback, "Registration error: " + e.getMessage());
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

                    NetworkResult res = executeRequest("POST", "/login", body.toString(), null);
                    handleResponse(res, callback, true);
                } catch (final Exception e) {
                    postError(callback, "Login error: " + e.getMessage());
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

                    NetworkResult res = executeRequest("POST", "/request-otp", body.toString(), null);
                    handleResponse(res, callback, false);
                } catch (final Exception e) {
                    postError(callback, "OTP request failed: " + e.getMessage());
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

                    NetworkResult res = executeRequest("POST", "/reset-password", body.toString(), null);
                    handleResponse(res, callback, false);
                } catch (final Exception e) {
                    postError(callback, "Password reset failed: " + e.getMessage());
                }
            }
        }).start();
    }

    public void createTransaction(final String token, final JSONObject transactionData, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject body = new JSONObject();
                    body.put("collection", "transactions");
                    body.put("data", transactionData);

                    NetworkResult res = executeRequest("POST", "/data", body.toString(), token);
                    handleResponse(res, callback, false);
                } catch (final Exception e) {
                    postError(callback, "Failed to create transaction: " + e.getMessage());
                }
            }
        }).start();
    }

    public void getTransactions(final String token, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    NetworkResult res = executeRequest("GET", "/data?collection=transactions", null, token);
                    handleResponse(res, callback, false);
                } catch (final Exception e) {
                    postError(callback, "Failed to load transactions: " + e.getMessage());
                }
            }
        }).start();
    }

    public void deleteTransaction(final String token, final String recordId, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    NetworkResult res = executeRequest("DELETE", "/data?id=" + recordId, null, token);
                    handleResponse(res, callback, false);
                } catch (final Exception e) {
                    postError(callback, "Failed to delete transaction: " + e.getMessage());
                }
            }
        }).start();
    }

    private static class NetworkResult {
        int code;
        String body;

        NetworkResult(int code, String body) {
            this.code = code;
            this.body = body;
        }
    }

    private NetworkResult executeRequest(String method, String endpoint, String jsonPayload, String bearerToken) throws Exception {
        URL url = new URL(API_BASE_URL + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");

        if (bearerToken != null && !bearerToken.isEmpty()) {
            conn.setRequestProperty("Authorization", "Bearer " + bearerToken);
        }

        if (jsonPayload != null && ("POST".equals(method) || "PUT".equals(method))) {
            conn.setDoOutput(true);
            byte[] bytes = jsonPayload.getBytes(StandardCharsets.UTF_8);
            OutputStream os = conn.getOutputStream();
            os.write(bytes);
            os.flush();
            os.close();
        }

        int status = conn.getResponseCode();
        InputStream is = (status >= 200 && status < 300) ? conn.getInputStream() : conn.getErrorStream();
        StringBuilder sb = new StringBuilder();
        if (is != null) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
        }
        conn.disconnect();
        return new NetworkResult(status, sb.toString());
    }

    private void handleResponse(NetworkResult res, ApiCallback callback, boolean requireToken) {
        try {
            if (res.body == null || res.body.trim().isEmpty()) {
                postError(callback, "Empty server response (HTTP " + res.code + ")");
                return;
            }

            JSONObject json = new JSONObject(res.body);
            boolean isSuccess = json.optBoolean("success", res.code >= 200 && res.code < 300);

            if (isSuccess) {
                if (requireToken && !json.has("token")) {
                    postError(callback, "Login authentication token missing.");
                } else {
                    postSuccess(callback, json);
                }
            } else {
                String error = json.optString("error", json.optString("message", "Request failed"));
                postError(callback, error);
            }
        } catch (Exception e) {
            postError(callback, "Response format error: " + e.getMessage());
        }
    }

    private void postSuccess(final ApiCallback callback, final JSONObject json) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (callback != null) {
                    callback.onSuccess(json);
                }
            }
        });
    }

    private void postError(final ApiCallback callback, final String message) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (callback != null) {
                    callback.onError(message);
                }
            }
        });
    }
}