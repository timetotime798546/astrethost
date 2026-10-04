package com.cloudnotes.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class BackendApi {
    private static final String API_BASE_URL = "https://trumpledroid-api.cloudbeta28624.workers.dev";
    private static final String PREFS_NAME = "CloudNotesPrefs";
    private static final String KEY_TOKEN = "authToken";

    private final Context context;
    private final String appId;
    private String token;

    public interface ApiCallback {
        void onSuccess(String response);
        void onError(String errorMessage);
    }

    public BackendApi(Context context) {
        this.context = context.getApplicationContext();
        this.appId = loadAppId();
        this.token = loadToken();
    }

    private String loadAppId() {
        try {
            InputStream is = context.getAssets().open("app-meta.json");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            is.close();
            JSONObject json = new JSONObject(sb.toString());
            if (json.has("app_id")) {
                return json.getString("app_id");
            } else if (json.has("package_name")) {
                return json.getString("package_name");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "com.cloudnotes.app";
    }

    private String loadToken() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_TOKEN, null);
    }

    public void saveToken(String token) {
        this.token = token;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_TOKEN, token).apply();
    }

    public void clearToken() {
        this.token = null;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_TOKEN).apply();
    }

    public boolean hasSession() {
        return token != null && !token.trim().isEmpty();
    }

    public String getAppId() {
        return appId;
    }

    private void deliverSuccess(final ApiCallback callback, final String response) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                callback.onSuccess(response);
            }
        });
    }

    private void deliverError(final ApiCallback callback, final String error) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                callback.onError(error);
            }
        });
    }

    private void makeRequest(final String endpoint, final String method, final String jsonBody, final boolean authenticated, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection conn = null;
                try {
                    URL url = new URL(API_BASE_URL + endpoint);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod(method);
                    conn.setConnectTimeout(15000);
                    conn.setReadTimeout(15000);
                    conn.setDoInput(true);

                    if (authenticated && token != null) {
                        conn.setRequestProperty("Authorization", "Bearer " + token);
                    }

                    if (jsonBody != null && (method.equals("POST") || method.equals("PUT"))) {
                        conn.setRequestProperty("Content-Type", "application/json");
                        conn.setDoOutput(true);
                        OutputStream os = conn.getOutputStream();
                        os.write(jsonBody.getBytes("UTF-8"));
                        os.close();
                    }

                    int responseCode = conn.getResponseCode();
                    InputStream is;
                    if (responseCode >= 200 && responseCode < 300) {
                        is = conn.getInputStream();
                    } else {
                        is = conn.getErrorStream();
                    }

                    if (is != null) {
                        BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = br.readLine()) != null) {
                            sb.append(line);
                        }
                        br.close();
                        String response = sb.toString();

                        if (responseCode >= 200 && responseCode < 300) {
                            deliverSuccess(callback, response);
                        } else {
                            String errorMsg = "HTTP Error " + responseCode;
                            try {
                                JSONObject errObj = new JSONObject(response);
                                if (errObj.has("error")) {
                                    errorMsg = errObj.getString("error");
                                } else if (errObj.has("message")) {
                                    errorMsg = errObj.getString("message");
                                }
                            } catch (Exception ignored) {}
                            deliverError(callback, errorMsg);
                        }
                    } else {
                        deliverError(callback, "No response from server (Code: " + responseCode + ")");
                    }
                } catch (Exception e) {
                    deliverError(callback, e.getMessage() != null ? e.getMessage() : "Network error occurred");
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
            makeRequest("/register", "POST", body.toString(), false, callback);
        } catch (Exception e) {
            callback.onError("Error preparing signup: " + e.getMessage());
        }
    }

    public void login(String email, String password, ApiCallback callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("app_id", appId);
            body.put("email", email);
            body.put("password", password);
            makeRequest("/login", "POST", body.toString(), false, callback);
        } catch (Exception e) {
            callback.onError("Error preparing signin: " + e.getMessage());
        }
    }

    public void logout(ApiCallback callback) {
        makeRequest("/logout", "POST", null, true, callback);
    }

    public void requestOtp(String email, ApiCallback callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("app_id", appId);
            body.put("email", email);
            makeRequest("/request-otp", "POST", body.toString(), false, callback);
        } catch (Exception e) {
            callback.onError("Error preparing OTP request: " + e.getMessage());
        }
    }

    public void resetPassword(String email, String otp, String newPassword, ApiCallback callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("app_id", appId);
            body.put("email", email);
            body.put("otp", otp);
            body.put("new_password", newPassword);
            makeRequest("/reset-password", "POST", body.toString(), false, callback);
        } catch (Exception e) {
            callback.onError("Error preparing reset request: " + e.getMessage());
        }
    }

    public void createNote(String title, String content, String category, String date, ApiCallback callback) {
        try {
            JSONObject dataObj = new JSONObject();
            dataObj.put("title", title);
            dataObj.put("content", content);
            dataObj.put("category", category);
            dataObj.put("date", date);

            JSONObject body = new JSONObject();
            body.put("collection", "notes");
            body.put("data", dataObj);

            makeRequest("/data", "POST", body.toString(), true, callback);
        } catch (Exception e) {
            callback.onError("Error preparing data: " + e.getMessage());
        }
    }

    public void getNotes(ApiCallback callback) {
        makeRequest("/data?collection=notes", "GET", null, true, callback);
    }

    public void updateNote(String id, String title, String content, String category, String date, ApiCallback callback) {
        try {
            JSONObject dataObj = new JSONObject();
            dataObj.put("title", title);
            dataObj.put("content", content);
            dataObj.put("category", category);
            dataObj.put("date", date);

            JSONObject body = new JSONObject();
            body.put("id", id);
            body.put("data", dataObj);

            makeRequest("/data", "PUT", body.toString(), true, callback);
        } catch (Exception e) {
            callback.onError("Error updating data: " + e.getMessage());
        }
    }

    public void deleteNote(String id, ApiCallback callback) {
        makeRequest("/data?id=" + id, "DELETE", null, true, callback);
    }
}