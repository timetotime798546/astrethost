package com.notecraftnotes.app;

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
    private static final String BASE_URL = "https://trumpledroid-api.cloudbeta28624.workers.dev";
    private static final String PREF_NAME = "notecraft_prefs";
    private static final String KEY_TOKEN = "auth_token";

    private final Context context;
    private final Handler mainHandler;

    public interface ApiCallback {
        void onSuccess(JSONObject response);
        void onError(String error);
    }

    public BackendApi(Context context) {
        this.context = context.getApplicationContext();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public String getAppId() {
        try {
            InputStream is = context.getAssets().open("app-meta.json");
            byte[] buffer = new byte[is.available()];
            int read = is.read(buffer);
            is.close();
            if (read > 0) {
                String jsonStr = new String(buffer, 0, read, "UTF-8");
                JSONObject obj = new JSONObject(jsonStr);
                return obj.optString("app_id", "notecraft_notes_app");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "notecraft_notes_app";
    }

    public void saveToken(String token) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_TOKEN, token).apply();
    }

    public String getToken() {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_TOKEN, "");
    }

    public void clearToken() {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_TOKEN).apply();
    }

    public boolean isLoggedIn() {
        String token = getToken();
        return token != null && !token.trim().isEmpty();
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

                    NetworkResult res = executePost(BASE_URL + "/register", body.toString(), false);
                    if (res.isSuccess) {
                        JSONObject json = new JSONObject(res.responseStr);
                        notifySuccess(callback, json);
                    } else {
                        notifyError(callback, extractErrorMessage(res.responseStr, "Registration failed"));
                    }
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

                    NetworkResult res = executePost(BASE_URL + "/login", body.toString(), false);
                    if (res.isSuccess) {
                        JSONObject json = new JSONObject(res.responseStr);
                        String token = json.optString("token", "");
                        if (!token.isEmpty()) {
                            saveToken(token);
                        }
                        notifySuccess(callback, json);
                    } else {
                        notifyError(callback, extractErrorMessage(res.responseStr, "Login failed"));
                    }
                } catch (Exception e) {
                    notifyError(callback, e.getMessage());
                }
            }
        }).start();
    }

    public void logout(final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    NetworkResult res = executePost(BASE_URL + "/logout", "{}", true);
                    clearToken();
                    if (res.isSuccess) {
                        JSONObject json = new JSONObject(res.responseStr);
                        notifySuccess(callback, json);
                    } else {
                        JSONObject fallback = new JSONObject();
                        fallback.put("success", true);
                        notifySuccess(callback, fallback);
                    }
                } catch (Exception e) {
                    clearToken();
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

                    NetworkResult res = executePost(BASE_URL + "/request-otp", body.toString(), false);
                    if (res.isSuccess) {
                        JSONObject json = new JSONObject(res.responseStr);
                        notifySuccess(callback, json);
                    } else {
                        notifyError(callback, extractErrorMessage(res.responseStr, "Failed to send OTP"));
                    }
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

                    NetworkResult res = executePost(BASE_URL + "/reset-password", body.toString(), false);
                    if (res.isSuccess) {
                        JSONObject json = new JSONObject(res.responseStr);
                        notifySuccess(callback, json);
                    } else {
                        notifyError(callback, extractErrorMessage(res.responseStr, "Password reset failed"));
                    }
                } catch (Exception e) {
                    notifyError(callback, e.getMessage());
                }
            }
        }).start();
    }

    public void createData(final String collection, final JSONObject dataObj, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject body = new JSONObject();
                    body.put("collection", collection);
                    body.put("data", dataObj);

                    NetworkResult res = executePost(BASE_URL + "/data", body.toString(), true);
                    if (res.isSuccess) {
                        JSONObject json = new JSONObject(res.responseStr);
                        notifySuccess(callback, json);
                    } else {
                        notifyError(callback, extractErrorMessage(res.responseStr, "Failed to create entry"));
                    }
                } catch (Exception e) {
                    notifyError(callback, e.getMessage());
                }
            }
        }).start();
    }

    public void readData(final String collection, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String urlStr = BASE_URL + "/data?collection=" + collection;
                    NetworkResult res = executeGet(urlStr, true);
                    if (res.isSuccess) {
                        JSONObject json = new JSONObject(res.responseStr);
                        notifySuccess(callback, json);
                    } else {
                        notifyError(callback, extractErrorMessage(res.responseStr, "Failed to fetch entries"));
                    }
                } catch (Exception e) {
                    notifyError(callback, e.getMessage());
                }
            }
        }).start();
    }

    public void updateData(final String recordId, final JSONObject dataObj, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject body = new JSONObject();
                    body.put("id", recordId);
                    body.put("data", dataObj);

                    NetworkResult res = executePut(BASE_URL + "/data", body.toString(), true);
                    if (res.isSuccess) {
                        JSONObject json = new JSONObject(res.responseStr);
                        notifySuccess(callback, json);
                    } else {
                        notifyError(callback, extractErrorMessage(res.responseStr, "Failed to update entry"));
                    }
                } catch (Exception e) {
                    notifyError(callback, e.getMessage());
                }
            }
        }).start();
    }

    public void deleteData(final String recordId, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String urlStr = BASE_URL + "/data?id=" + recordId;
                    NetworkResult res = executeDelete(urlStr, true);
                    if (res.isSuccess) {
                        JSONObject json = new JSONObject(res.responseStr);
                        notifySuccess(callback, json);
                    } else {
                        notifyError(callback, extractErrorMessage(res.responseStr, "Failed to delete entry"));
                    }
                } catch (Exception e) {
                    notifyError(callback, e.getMessage());
                }
            }
        }).start();
    }

    private static class NetworkResult {
        boolean isSuccess;
        String responseStr;
        NetworkResult(boolean isSuccess, String responseStr) {
            this.isSuccess = isSuccess;
            this.responseStr = responseStr;
        }
    }

    private NetworkResult executePost(String urlStr, String jsonPayload, boolean useAuth) {
        return sendRequest(urlStr, "POST", jsonPayload, useAuth);
    }

    private NetworkResult executePut(String urlStr, String jsonPayload, boolean useAuth) {
        return sendRequest(urlStr, "PUT", jsonPayload, useAuth);
    }

    private NetworkResult executeGet(String urlStr, boolean useAuth) {
        return sendRequest(urlStr, "GET", null, useAuth);
    }

    private NetworkResult executeDelete(String urlStr, boolean useAuth) {
        return sendRequest(urlStr, "DELETE", null, useAuth);
    }

    private NetworkResult sendRequest(String urlStr, String method, String payload, boolean useAuth) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(method);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");

            if (useAuth) {
                String token = getToken();
                if (token != null && !token.isEmpty()) {
                    conn.setRequestProperty("Authorization", "Bearer " + token);
                }
            }

            if (payload != null && ("POST".equals(method) || "PUT".equals(method))) {
                conn.setDoOutput(true);
                OutputStream os = conn.getOutputStream();
                os.write(payload.getBytes("UTF-8"));
                os.flush();
                os.close();
            }

            int code = conn.getResponseCode();
            InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
            String response = readStream(is);

            return new NetworkResult(code >= 200 && code < 300, response);

        } catch (Exception e) {
            return new NetworkResult(false, "{\"error\":\"" + e.getMessage() + "\"}");
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private String readStream(InputStream is) {
        if (is == null) return "";
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private String extractErrorMessage(String responseStr, String fallback) {
        try {
            if (responseStr != null && !responseStr.trim().isEmpty()) {
                JSONObject obj = new JSONObject(responseStr);
                if (obj.has("error")) return obj.getString("error");
                if (obj.has("message")) return obj.getString("message");
            }
        } catch (Exception e) {
            // Ignore parse exception
        }
        return fallback;
    }

    private void notifySuccess(final ApiCallback callback, final JSONObject json) {
        if (callback == null) return;
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onSuccess(json);
            }
        });
    }

    private void notifyError(final ApiCallback callback, final String message) {
        if (callback == null) return;
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onError(message);
            }
        });
    }
}