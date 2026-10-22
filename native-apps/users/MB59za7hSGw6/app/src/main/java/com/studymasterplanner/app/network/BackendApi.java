package com.studymasterplanner.app.network;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.studymasterplanner.app.utils.SessionManager;

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
    private Context context;
    private SessionManager sessionManager;
    private Handler mainHandler;

    public interface ApiCallback {
        void onSuccess(JSONObject response);
        void onError(String message);
    }

    public BackendApi(Context context) {
        this.context = context.getApplicationContext();
        this.sessionManager = new SessionManager(context);
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
            return obj.optString("app_id", "studymaster_planner_app");
        } catch (Exception e) {
            return "studymaster_planner_app";
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
                    makeRequest("/register", "POST", body.toString(), false, callback);
                } catch (Exception e) {
                    dispatchError(callback, e.getMessage());
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
                    makeRequest("/login", "POST", body.toString(), false, callback);
                } catch (Exception e) {
                    dispatchError(callback, e.getMessage());
                }
            }
        }).start();
    }

    public void createData(final String collection, final JSONObject data, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject payload = new JSONObject();
                    payload.put("collection", collection);
                    payload.put("data", data);
                    makeRequest("/data", "POST", payload.toString(), true, callback);
                } catch (Exception e) {
                    dispatchError(callback, e.getMessage());
                }
            }
        }).start();
    }

    public void getData(final String collection, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String path = "/data?collection=" + collection;
                    makeRequest(path, "GET", null, true, callback);
                } catch (Exception e) {
                    dispatchError(callback, e.getMessage());
                }
            }
        }).start();
    }

    public void updateData(final String recordId, final JSONObject data, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject payload = new JSONObject();
                    payload.put("id", recordId);
                    payload.put("data", data);
                    makeRequest("/data", "PUT", payload.toString(), true, callback);
                } catch (Exception e) {
                    dispatchError(callback, e.getMessage());
                }
            }
        }).start();
    }

    public void deleteData(final String recordId, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String path = "/data?id=" + recordId;
                    makeRequest(path, "DELETE", null, true, callback);
                } catch (Exception e) {
                    dispatchError(callback, e.getMessage());
                }
            }
        }).start();
    }

    private void makeRequest(String endpoint, String method, String payload, boolean authenticated, ApiCallback callback) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(BASE_URL + endpoint);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(method);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            if (authenticated) {
                String token = sessionManager.getToken();
                if (token != null && !token.isEmpty()) {
                    conn.setRequestProperty("Authorization", "Bearer " + token);
                }
            }

            if (payload != null && (method.equals("POST") || method.equals("PUT"))) {
                conn.setDoOutput(true);
                OutputStream os = conn.getOutputStream();
                os.write(payload.getBytes(StandardCharsets.UTF_8));
                os.flush();
                os.close();
            }

            int responseCode = conn.getResponseCode();
            InputStream is = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream();

            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            StringBuilder responseStr = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                responseStr.append(line);
            }
            reader.close();

            JSONObject responseJson = new JSONObject(responseStr.toString());
            if (responseCode >= 200 && responseCode < 300 && responseJson.optBoolean("success", true)) {
                dispatchSuccess(callback, responseJson);
            } else {
                String error = responseJson.optString("error", responseJson.optString("message", "Request failed"));
                dispatchError(callback, error);
            }

        } catch (Exception e) {
            dispatchError(callback, e.getMessage() != null ? e.getMessage() : "Network error");
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
                if (callback != null) callback.onSuccess(response);
            }
        });
    }

    private void dispatchError(final ApiCallback callback, final String message) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (callback != null) callback.onError(message);
            }
        });
    }
}