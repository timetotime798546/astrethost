package com.caresyncpro.app;

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

    public interface ApiCallback {
        void onSuccess(JSONObject response);
        void onError(String errorMessage);
    }

    private static final String BASE_URL = "https://trumpledroid-api.cloudbeta28624.workers.dev";
    private final Context context;
    private final Handler mainHandler;

    public BackendApi(Context context) {
        this.context = context.getApplicationContext();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public String getAppId() {
        try {
            InputStream is = context.getAssets().open("app-meta.json");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            JSONObject obj = new JSONObject(sb.toString());
            return obj.optString("app_id", "caresyncpro_enterprise_ehr");
        } catch (Exception e) {
            return "caresyncpro_enterprise_ehr";
        }
    }

    // Step 1: POST /register
    public void register(final String email, final String password, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject payload = new JSONObject();
                    payload.put("app_id", getAppId());
                    payload.put("email", email);
                    payload.put("password", password);

                    performHttpRequest("POST", BASE_URL + "/register", null, payload.toString(), callback);
                } catch (Exception e) {
                    dispatchError(callback, "Register error: " + e.getMessage());
                }
            }
        }).start();
    }

    // Step 2: POST /login
    public void login(final String email, final String password, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject payload = new JSONObject();
                    payload.put("app_id", getAppId());
                    payload.put("email", email);
                    payload.put("password", password);

                    performHttpRequest("POST", BASE_URL + "/login", null, payload.toString(), callback);
                } catch (Exception e) {
                    dispatchError(callback, "Login error: " + e.getMessage());
                }
            }
        }).start();
    }

    // Step 3: POST /request-otp for password reset
    public void requestOtp(final String email, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject payload = new JSONObject();
                    payload.put("app_id", getAppId());
                    payload.put("email", email);
                    performHttpRequest("POST", BASE_URL + "/request-otp", null, payload.toString(), callback);
                } catch (Exception e) {
                    dispatchError(callback, "Request OTP error: " + e.getMessage());
                }
            }
        }).start();
    }

    // Step 4: POST /reset-password
    public void resetPassword(final String email, final String otp, final String newPassword, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject payload = new JSONObject();
                    payload.put("app_id", getAppId());
                    payload.put("email", email);
                    payload.put("otp", otp);
                    payload.put("new_password", newPassword);
                    performHttpRequest("POST", BASE_URL + "/reset-password", null, payload.toString(), callback);
                } catch (Exception e) {
                    dispatchError(callback, "Reset password error: " + e.getMessage());
                }
            }
        }).start();
    }

    // CREATE: POST /data
    public void createRecord(final String token, final String collection, final JSONObject data, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject payload = new JSONObject();
                    payload.put("collection", collection);
                    payload.put("data", data);
                    performHttpRequest("POST", BASE_URL + "/data", token, payload.toString(), callback);
                } catch (Exception e) {
                    dispatchError(callback, "Create record error: " + e.getMessage());
                }
            }
        }).start();
    }

    // READ: GET /data?collection=...
    public void readRecords(final String token, final String collection, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String url = BASE_URL + "/data?collection=" + collection;
                    performHttpRequest("GET", url, token, null, callback);
                } catch (Exception e) {
                    dispatchError(callback, "Read records error: " + e.getMessage());
                }
            }
        }).start();
    }

    // UPDATE: PUT /data
    public void updateRecord(final String token, final String recordId, final JSONObject data, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject payload = new JSONObject();
                    payload.put("id", recordId);
                    payload.put("data", data);
                    performHttpRequest("PUT", BASE_URL + "/data", token, payload.toString(), callback);
                } catch (Exception e) {
                    dispatchError(callback, "Update record error: " + e.getMessage());
                }
            }
        }).start();
    }

    // DELETE: DELETE /data?id=...
    public void deleteRecord(final String token, final String recordId, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String url = BASE_URL + "/data?id=" + recordId;
                    performHttpRequest("DELETE", url, token, null, callback);
                } catch (Exception e) {
                    dispatchError(callback, "Delete record error: " + e.getMessage());
                }
            }
        }).start();
    }

    private void performHttpRequest(String method, String urlStr, String token, String jsonBody, ApiCallback callback) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
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
                os.write(jsonBody.getBytes("UTF-8"));
                os.flush();
                os.close();
            }

            int responseCode = conn.getResponseCode();
            InputStream is = (responseCode >= 200 && responseCode < 400) ? conn.getInputStream() : conn.getErrorStream();

            StringBuilder sb = new StringBuilder();
            if (is != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();
            }

            String respStr = sb.toString();
            JSONObject jsonResponse;
            if (respStr.trim().startsWith("{")) {
                jsonResponse = new JSONObject(respStr);
            } else {
                jsonResponse = new JSONObject();
                jsonResponse.put("success", responseCode >= 200 && responseCode < 300);
                jsonResponse.put("raw_response", respStr);
            }

            if (responseCode >= 200 && responseCode < 300) {
                dispatchSuccess(callback, jsonResponse);
            } else {
                String errMsg = jsonResponse.optString("error", jsonResponse.optString("message", "HTTP Error " + responseCode));
                dispatchError(callback, errMsg);
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