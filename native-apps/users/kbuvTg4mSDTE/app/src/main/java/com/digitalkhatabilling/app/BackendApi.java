package com.digitalkhatabilling.app;

import android.content.Context;
import org.json.JSONObject;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class BackendApi {
    private static final String API_BASE_URL = "https://trumpledroid-api.cloudbeta28624.workers.dev";
    private String appId;
    private String token;
    private Context context;

    public interface ApiCallback {
        void onSuccess(String response);
        void onFailure(String errorMessage);
    }

    public BackendApi(Context context) {
        this.context = context;
        this.appId = loadAppId(context);
        this.token = loadToken();
    }

    private String loadAppId(Context context) {
        try {
            InputStream is = context.getAssets().open("app-meta.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            JSONObject json = new JSONObject(new String(buffer, "UTF-8"));
            return json.optString("app_id", "digitalkhatabilling");
        } catch (Exception e) {
            e.printStackTrace();
            return "digitalkhatabilling";
        }
    }

    private String loadToken() {
        return context.getSharedPreferences("api_prefs", Context.MODE_PRIVATE).getString("token", null);
    }

    public void saveToken(String token) {
        this.token = token;
        context.getSharedPreferences("api_prefs", Context.MODE_PRIVATE).edit().putString("token", token).apply();
    }

    public void clearToken() {
        this.token = null;
        context.getSharedPreferences("api_prefs", Context.MODE_PRIVATE).edit().remove("token").apply();
    }

    public boolean isLoggedIn() {
        return token != null && !token.isEmpty();
    }

    private void makeRequest(final String method, final String endpoint, final String jsonBody, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection conn = null;
                try {
                    URL url = new URL(API_BASE_URL + endpoint);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod(method);
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setRequestProperty("Accept", "application/json");
                    if (token != null) {
                        conn.setRequestProperty("Authorization", "Bearer " + token);
                    }

                    if (jsonBody != null && (method.equals("POST") || method.equals("PUT"))) {
                        conn.setDoOutput(true);
                        OutputStream os = conn.getOutputStream();
                        os.write(jsonBody.getBytes("UTF-8"));
                        os.flush();
                        os.close();
                    }

                    int responseCode = conn.getResponseCode();
                    InputStream is;
                    if (responseCode >= 200 && responseCode < 300) {
                        is = conn.getInputStream();
                    } else {
                        is = conn.getErrorStream();
                    }

                    if (is == null) {
                        callback.onFailure("No response stream");
                        return;
                    }

                    BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                    }
                    br.close();

                    final String response = sb.toString();
                    if (responseCode >= 200 && responseCode < 300) {
                        callback.onSuccess(response);
                    } else {
                        String errMsg = "Error " + responseCode;
                        try {
                            JSONObject errJson = new JSONObject(response);
                            errMsg = errJson.optString("message", errJson.optString("error", errMsg));
                        } catch (Exception ignored) {}
                        callback.onFailure(errMsg);
                    }
                } catch (Exception e) {
                    callback.onFailure(e.getMessage() != null ? e.getMessage() : "Network error occurred");
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
            makeRequest("POST", "/register", body.toString(), callback);
        } catch (Exception e) {
            callback.onFailure(e.getMessage());
        }
    }

    public void login(String email, String password, ApiCallback callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("app_id", appId);
            body.put("email", email);
            body.put("password", password);
            makeRequest("POST", "/login", body.toString(), callback);
        } catch (Exception e) {
            callback.onFailure(e.getMessage());
        }
    }

    public void createRecord(String collection, JSONObject fields, ApiCallback callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("collection", collection);
            body.put("data", fields);
            makeRequest("POST", "/data", body.toString(), callback);
        } catch (Exception e) {
            callback.onFailure(e.getMessage());
        }
    }

    public void getRecords(String collection, ApiCallback callback) {
        makeRequest("GET", "/data?collection=" + collection, null, callback);
    }
}