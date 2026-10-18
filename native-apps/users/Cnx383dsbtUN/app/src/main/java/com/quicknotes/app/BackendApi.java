package com.quicknotes.app;

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

public class BackendApi {
    private static final String API_BASE_URL = "https://trumpledroid-api.cloudbeta28624.workers.dev";
    private final Context context;
    private final Handler mainHandler;

    public interface ApiCallback {
        void onSuccess(JSONObject response);
        void onError(String message);
    }

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
            return json.optString("app_id", "quicknotes_cloud_app");
        } catch (Exception e) {
            return "quicknotes_cloud_app";
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

                    NetworkResult result = executeRequest("POST", "/register", body.toString(), null);
                    if (result.isSuccess()) {
                        JSONObject json = new JSONObject(result.body);
                        boolean success = json.optBoolean("success", true);
                        if (success) {
                            postSuccess(callback, json);
                        } else {
                            postError(callback, json.optString("error", "Registration failed"));
                        }
                    } else {
                        postError(callback, parseErrorMessage(result.body, "Registration failed with code " + result.code));
                    }
                } catch (final Exception e) {
                    postError(callback, e.getMessage());
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

                    NetworkResult result = executeRequest("POST", "/login", body.toString(), null);
                    if (result.isSuccess()) {
                        JSONObject json = new JSONObject(result.body);
                        if (json.optBoolean("success", false) || json.has("token")) {
                            postSuccess(callback, json);
                        } else {
                            postError(callback, json.optString("error", "Invalid login response"));
                        }
                    } else {
                        postError(callback, parseErrorMessage(result.body, "Login failed"));
                    }
                } catch (final Exception e) {
                    postError(callback, e.getMessage());
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

                    NetworkResult result = executeRequest("POST", "/request-otp", body.toString(), null);
                    if (result.isSuccess()) {
                        JSONObject json = new JSONObject(result.body);
                        postSuccess(callback, json);
                    } else {
                        postError(callback, parseErrorMessage(result.body, "Failed to send OTP"));
                    }
                } catch (Exception e) {
                    postError(callback, e.getMessage());
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

                    NetworkResult result = executeRequest("POST", "/reset-password", body.toString(), null);
                    if (result.isSuccess()) {
                        JSONObject json = new JSONObject(result.body);
                        postSuccess(callback, json);
                    } else {
                        postError(callback, parseErrorMessage(result.body, "Failed to reset password"));
                    }
                } catch (Exception e) {
                    postError(callback, e.getMessage());
                }
            }
        }).start();
    }

    public void createNote(final String token, final JSONObject noteData, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject body = new JSONObject();
                    body.put("collection", "notes");
                    body.put("data", noteData);

                    NetworkResult result = executeRequest("POST", "/data", body.toString(), token);
                    if (result.isSuccess()) {
                        JSONObject json = new JSONObject(result.body);
                        if (json.optBoolean("success", false) && json.has("record")) {
                            postSuccess(callback, json);
                        } else {
                            postError(callback, json.optString("error", "Failed to create note"));
                        }
                    } else {
                        postError(callback, parseErrorMessage(result.body, "Creation error"));
                    }
                } catch (Exception e) {
                    postError(callback, e.getMessage());
                }
            }
        }).start();
    }

    public void getNotes(final String token, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    NetworkResult result = executeRequest("GET", "/data?collection=notes", null, token);
                    if (result.isSuccess()) {
                        JSONObject json = new JSONObject(result.body);
                        if (json.optBoolean("success", false)) {
                            postSuccess(callback, json);
                        } else {
                            postError(callback, json.optString("error", "Failed to retrieve notes"));
                        }
                    } else {
                        postError(callback, parseErrorMessage(result.body, "Fetch error"));
                    }
                } catch (Exception e) {
                    postError(callback, e.getMessage());
                }
            }
        }).start();
    }

    public void updateNote(final String token, final String recordId, final JSONObject noteData, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject body = new JSONObject();
                    body.put("id", recordId);
                    body.put("data", noteData);

                    NetworkResult result = executeRequest("PUT", "/data", body.toString(), token);
                    if (result.isSuccess()) {
                        JSONObject json = new JSONObject(result.body);
                        if (json.optBoolean("success", false)) {
                            postSuccess(callback, json);
                        } else {
                            postError(callback, json.optString("error", "Update failed"));
                        }
                    } else {
                        postError(callback, parseErrorMessage(result.body, "Update error"));
                    }
                } catch (Exception e) {
                    postError(callback, e.getMessage());
                }
            }
        }).start();
    }

    public void deleteNote(final String token, final String recordId, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    NetworkResult result = executeRequest("DELETE", "/data?id=" + recordId, null, token);
                    if (result.isSuccess()) {
                        JSONObject json = new JSONObject(result.body);
                        if (json.optBoolean("success", false)) {
                            postSuccess(callback, json);
                        } else {
                            postError(callback, json.optString("error", "Delete failed"));
                        }
                    } else {
                        postError(callback, parseErrorMessage(result.body, "Delete error"));
                    }
                } catch (Exception e) {
                    postError(callback, e.getMessage());
                }
            }
        }).start();
    }

    private NetworkResult executeRequest(String method, String endpoint, String jsonBody, String token) throws Exception {
        URL url = new URL(API_BASE_URL + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");

        if (token != null && !token.trim().isEmpty()) {
            conn.setRequestProperty("Authorization", "Bearer " + token);
        }

        if (jsonBody != null && (method.equals("POST") || method.equals("PUT"))) {
            conn.setDoOutput(true);
            OutputStream os = conn.getOutputStream();
            os.write(jsonBody.getBytes("UTF-8"));
            os.flush();
            os.close();
        }

        int statusCode = conn.getResponseCode();
        InputStream is = (statusCode >= 200 && statusCode < 400) ? conn.getInputStream() : conn.getErrorStream();

        StringBuilder sb = new StringBuilder();
        if (is != null) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
        }

        conn.disconnect();
        return new NetworkResult(statusCode, sb.toString());
    }

    private String parseErrorMessage(String rawBody, String defaultMsg) {
        if (rawBody == null || rawBody.trim().isEmpty()) {
            return defaultMsg;
        }
        try {
            JSONObject obj = new JSONObject(rawBody);
            if (obj.has("error")) return obj.getString("error");
            if (obj.has("message")) return obj.getString("message");
        } catch (Exception ignored) {
        }
        return defaultMsg;
    }

    private void postSuccess(final ApiCallback callback, final JSONObject json) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (callback != null) callback.onSuccess(json);
            }
        });
    }

    private void postError(final ApiCallback callback, final String message) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (callback != null) callback.onError(message);
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