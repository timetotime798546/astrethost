package com.procalculator.app;

import android.content.Context;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class BackendApi {
    private String baseUrl = "https://trumpledroid-api.cloudbeta28624.workers.dev";
    private String appId;
    private String token;

    public interface ApiCallback {
        void onSuccess(JSONObject result);
        void onError(String message);
    }

    public BackendApi(Context context) {
        this.appId = readAppId(context);
        this.token = context.getSharedPreferences("auth", Context.MODE_PRIVATE).getString("token", null);
    }

    private String readAppId(Context context) {
        try {
            InputStream is = context.getAssets().open("app-meta.json");
            Scanner s = new Scanner(is).useDelimiter("\\A");
            String json = s.hasNext() ? s.next() : "";
            // FIXED: Correctly reading 'app_id' as required by backend rules
            return new JSONObject(json).getString("app_id");
        } catch (Exception e) {
            return "app_6abbb783a9f92";
        }
    }

    public void register(final String email, final String password, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject body = new JSONObject();
                    body.put("app_id", appId);
                    body.put("email", email);
                    body.put("password", password);
                    executeRequest("POST", "/register", body, callback);
                } catch (Exception e) {
                    callback.onError(e.getMessage());
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
                    body.put("app_id", appId);
                    body.put("email", email);
                    body.put("password", password);
                    executeRequest("POST", "/login", body, callback);
                } catch (Exception e) {
                    callback.onError(e.getMessage());
                }
            }
        }).start();
    }

    public void saveHistory(final String expression, final String result, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject data = new JSONObject();
                    data.put("expression", expression);
                    data.put("result", result);
                    data.put("timestamp", String.valueOf(System.currentTimeMillis()));

                    JSONObject body = new JSONObject();
                    body.put("collection", "history");
                    body.put("data", data);
                    executeRequest("POST", "/data", body, callback);
                } catch (Exception e) {
                    callback.onError(e.getMessage());
                }
            }
        }).start();
    }

    public void getHistory(final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                executeRequest("GET", "/data?collection=history", null, callback);
            }
        }).start();
    }

    private void executeRequest(String method, String endpoint, JSONObject body, ApiCallback callback) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(baseUrl + endpoint);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(method);
            conn.setRequestProperty("Content-Type", "application/json");
            if (token != null) {
                conn.setRequestProperty("Authorization", "Bearer " + token);
            }

            if (body != null && !method.equals("GET")) {
                conn.setDoOutput(true);
                OutputStream os = conn.getOutputStream();
                os.write(body.toString().getBytes("UTF-8"));
                os.close();
            }

            int code = conn.getResponseCode();
            InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            
            JSONObject response = new JSONObject(sb.toString());
            if (response.optBoolean("success", false)) {
                callback.onSuccess(response);
            } else {
                callback.onError(response.optString("message", "Error occurred"));
            }
        } catch (Exception e) {
            callback.onError(e.getMessage());
        } finally {
            if (conn != null) conn.disconnect();
        }
    }
}