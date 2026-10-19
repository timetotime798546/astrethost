package com.trirunner.app;

import android.content.Context;
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
    private final Context context;
    private String cachedAppId = null;

    public interface ApiCallback {
        void onSuccess(JSONObject response);
        void onError(String errorMessage);
    }

    public BackendApi(Context context) {
        this.context = context.getApplicationContext();
    }

    public String getAppId() {
        if (cachedAppId != null) {
            return cachedAppId;
        }
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
            cachedAppId = obj.optString("app_id", "trirunner_game");
            return cachedAppId;
        } catch (Exception e) {
            cachedAppId = "trirunner_game";
            return cachedAppId;
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
                    executeRequest("/register", "POST", body.toString(), null, callback);
                } catch (Exception e) {
                    notifyError(callback, e.getMessage());
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
                    executeRequest("/login", "POST", body.toString(), null, callback);
                } catch (Exception e) {
                    notifyError(callback, e.getMessage());
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
                    executeRequest("/request-otp", "POST", body.toString(), null, callback);
                } catch (Exception e) {
                    notifyError(callback, e.getMessage());
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
                    executeRequest("/reset-password", "POST", body.toString(), null, callback);
                } catch (Exception e) {
                    notifyError(callback, e.getMessage());
                }
            }
        }).start();
    }

    public void createData(final String token, final String collection, final JSONObject data, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject body = new JSONObject();
                    body.put("collection", collection);
                    body.put("data", data);
                    executeRequest("/data", "POST", body.toString(), token, callback);
                } catch (Exception e) {
                    notifyError(callback, e.getMessage());
                }
            }
        }).start();
    }

    public void getData(final String token, final String collection, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                executeRequest("/data?collection=" + collection, "GET", null, token, callback);
            }
        }).start();
    }

    public void updateData(final String token, final String recordId, final JSONObject data, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject body = new JSONObject();
                    body.put("id", recordId);
                    body.put("data", data);
                    executeRequest("/data", "PUT", body.toString(), token, callback);
                } catch (Exception e) {
                    notifyError(callback, e.getMessage());
                }
            }
        }).start();
    }

    private void executeRequest(String endpoint, String method, String jsonBody, String token, ApiCallback callback) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(BASE_URL + endpoint);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(method);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");

            if (token != null && !token.isEmpty()) {
                conn.setRequestProperty("Authorization", "Bearer " + token);
            }

            if (jsonBody != null && (method.equals("POST") || method.equals("PUT"))) {
                conn.setDoOutput(true);
                OutputStream os = conn.getOutputStream();
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
                os.flush();
                os.close();
            }

            int responseCode = conn.getResponseCode();
            InputStream is = (responseCode >= 200 && responseCode < 400) ? conn.getInputStream() : conn.getErrorStream();

            StringBuilder sb = new StringBuilder();
            if (is != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();
            }

            String raw = sb.toString();
            JSONObject json = new JSONObject(raw.isEmpty() ? "{}" : raw);

            if (responseCode >= 200 && responseCode < 300) {
                notifySuccess(callback, json);
            } else {
                String errMsg = json.optString("error", json.optString("message", "HTTP " + responseCode));
                notifyError(callback, errMsg);
            }
        } catch (Exception e) {
            notifyError(callback, e.getMessage() != null ? e.getMessage() : "Network error");
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private void notifySuccess(final ApiCallback callback, final JSONObject response) {
        if (callback != null) {
            callback.onSuccess(response);
        }
    }

    private void notifyError(final ApiCallback callback, final String message) {
        if (callback != null) {
            callback.onError(message);
        }
    }
}