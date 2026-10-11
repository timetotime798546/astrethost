package com.swiftnote.app;

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
    private static final String BASE_URL = "https://trumpledroid-api.cloudbeta28624.workers.dev";
    private final Context context;
    private final Handler mainHandler;

    public interface ApiCallback {
        void onSuccess(JSONObject response);
        void onError(String errorMessage);
    }

    public BackendApi(Context context) {
        this.context = context;
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
            return obj.optString("app_id", "swiftnote_production_app");
        } catch (Exception e) {
            return "swiftnote_production_app";
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

                    executeRequest("/register", "POST", null, body.toString(), callback);
                } catch (Exception e) {
                    dispatchError(callback, "Registration error: " + e.getMessage());
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

                    executeRequest("/login", "POST", null, body.toString(), callback);
                } catch (Exception e) {
                    dispatchError(callback, "Login error: " + e.getMessage());
                }
            }
        }).start();
    }

    public void logout(final String token, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                executeRequest("/logout", "POST", token, "{}", callback);
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
                    executeRequest("/request-otp", "POST", null, body.toString(), callback);
                } catch (Exception e) {
                    dispatchError(callback, "OTP request error: " + e.getMessage());
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
                    executeRequest("/reset-password", "POST", null, body.toString(), callback);
                } catch (Exception e) {
                    dispatchError(callback, "Password reset error: " + e.getMessage());
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
                    executeRequest("/data", "POST", token, body.toString(), callback);
                } catch (Exception e) {
                    dispatchError(callback, "Create error: " + e.getMessage());
                }
            }
        }).start();
    }

    public void readData(final String token, final String collection, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String path = "/data?collection=" + collection;
                executeRequest(path, "GET", token, null, callback);
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
                    executeRequest("/data", "PUT", token, body.toString(), callback);
                } catch (Exception e) {
                    dispatchError(callback, "Update error: " + e.getMessage());
                }
            }
        }).start();
    }

    public void deleteData(final String token, final String recordId, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String path = "/data?id=" + recordId;
                executeRequest(path, "DELETE", token, null, callback);
            }
        }).start();
    }

    private void executeRequest(String endpoint, String method, String token, String jsonBody, ApiCallback callback) {
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
                byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(input, 0, input.length);
                }
            }

            int responseCode = conn.getResponseCode();
            InputStream is = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream();

            StringBuilder response = new StringBuilder();
            if (is != null) {
                BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
                br.close();
            }

            String respStr = response.toString();
            JSONObject jsonResult;
            if (respStr.trim().startsWith("{")) {
                jsonResult = new JSONObject(respStr);
            } else {
                jsonResult = new JSONObject();
                jsonResult.put("raw", respStr);
            }

            if (responseCode >= 200 && responseCode < 300) {
                dispatchSuccess(callback, jsonResult);
            } else {
                String errorMsg = jsonResult.optString("error", jsonResult.optString("message", "HTTP " + responseCode));
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
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (callback != null) {
                    callback.onSuccess(response);
                }
            }
        });
    }

    private void dispatchError(final ApiCallback callback, final String message) {
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