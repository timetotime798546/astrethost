package com.barberloyalty.app;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BackendApi {

    private String apiBaseUrl = "https://trumpledroid-api.cloudbeta28624.workers.dev";
    private String appId = "";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private String userToken = null;

    public interface ApiCallback {
        void onSuccess(JSONObject response);
        void onError(String errorMessage);
    }

    public BackendApi(Context context) {
        readAppMeta(context);
    }

    private void readAppMeta(Context context) {
        try {
            InputStream is = context.getAssets().open("app-meta.json");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            JSONObject meta = new JSONObject(sb.toString());
            this.appId = meta.optString("app_id", "barber_loyalty_unique_id");
        } catch (Exception e) {
            this.appId = "barber_loyalty_unique_id";
        }
    }

    public void setToken(String token) {
        this.userToken = token;
    }

    private void makeHttpRequest(final String endpoint, final String method, final JSONObject payload, final ApiCallback callback) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection conn = null;
                try {
                    URL url = new URL(apiBaseUrl + endpoint);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod(method);
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setRequestProperty("Accept", "application/json");

                    if (userToken != null && !userToken.isEmpty()) {
                        conn.setRequestProperty("Authorization", "Bearer " + userToken);
                    }

                    if (payload != null && (method.equals("POST") || method.equals("PUT"))) {
                        conn.setDoOutput(true);
                        OutputStream os = conn.getOutputStream();
                        os.write(payload.toString().getBytes("UTF-8"));
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

                    BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    StringBuilder responseBuilder = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        responseBuilder.append(line);
                    }
                    br.close();

                    final String responseString = responseBuilder.toString();
                    final JSONObject responseJson = new JSONObject(responseString);

                    if (responseCode >= 200 && responseCode < 300) {
                        callback.onSuccess(responseJson);
                    } else {
                        final String errMsg = responseJson.optString("error", "Network execution failed with status: " + responseCode);
                        callback.onError(errMsg);
                    }
                } catch (final Exception e) {
                    callback.onError(e.getMessage());
                } finally {
                    if (conn != null) {
                        conn.disconnect();
                    }
                }
            }
        });
    }

    public void register(String email, String password, ApiCallback callback) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("app_id", appId);
            payload.put("email", email);
            payload.put("password", password);
            makeHttpRequest("/register", "POST", payload, callback);
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    public void login(String email, String password, ApiCallback callback) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("app_id", appId);
            payload.put("email", email);
            payload.put("password", password);
            makeHttpRequest("/login", "POST", payload, callback);
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    public void requestOtp(String email, ApiCallback callback) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("app_id", appId);
            payload.put("email", email);
            makeHttpRequest("/request-otp", "POST", payload, callback);
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    public void resetPassword(String email, String otp, String newPassword, ApiCallback callback) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("app_id", appId);
            payload.put("email", email);
            payload.put("otp", otp);
            payload.put("new_password", newPassword);
            makeHttpRequest("/reset-password", "POST", payload, callback);
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    public void logout(ApiCallback callback) {
        makeHttpRequest("/logout", "POST", null, callback);
    }

    public void createRecord(String collection, JSONObject data, ApiCallback callback) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("collection", collection);
            payload.put("data", data);
            makeHttpRequest("/data", "POST", payload, callback);
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    public void readRecords(String collection, ApiCallback callback) {
        makeHttpRequest("/data?collection=" + collection, "GET", null, callback);
    }

    public void updateRecord(String recordId, JSONObject data, ApiCallback callback) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("id", recordId);
            payload.put("data", data);
            makeHttpRequest("/data", "PUT", payload, callback);
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }
}