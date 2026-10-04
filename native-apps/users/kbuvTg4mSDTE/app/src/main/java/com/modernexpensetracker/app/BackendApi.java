package com.modernexpensetracker.app;

import android.content.Context;
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

    public interface ApiCallback {
        void onSuccess(String response);
        void onError(String error);
    }

    private static final String BASE_URL = "https://trumpledroid-api.cloudbeta28624.workers.dev";
    private String appId;

    public BackendApi(Context context) {
        this.appId = loadAppIdFromAssets(context);
    }

    private String loadAppIdFromAssets(Context context) {
        try {
            InputStream is = context.getAssets().open("app-meta.json");
            BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            br.close();
            JSONObject obj = new JSONObject(sb.toString());
            return obj.optString("package_name", "com.modernexpensetracker.app");
        } catch (Exception e) {
            return "com.modernexpensetracker.app";
        }
    }

    public void register(String email, String password, ApiCallback callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("app_id", appId);
            body.put("email", email);
            body.put("password", password);
            executeRequest(BASE_URL + "/register", "POST", body.toString(), null, callback);
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    public void login(String email, String password, ApiCallback callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("app_id", appId);
            body.put("email", email);
            body.put("password", password);
            executeRequest(BASE_URL + "/login", "POST", body.toString(), null, callback);
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    public void requestOtp(String email, ApiCallback callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("app_id", appId);
            body.put("email", email);
            executeRequest(BASE_URL + "/request-otp", "POST", body.toString(), null, callback);
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    public void resetPassword(String email, String otp, String newPassword, ApiCallback callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("app_id", appId);
            body.put("email", email);
            body.put("otp", otp);
            body.put("new_password", newPassword);
            executeRequest(BASE_URL + "/reset-password", "POST", body.toString(), null, callback);
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    public void logout(String token, ApiCallback callback) {
        executeRequest(BASE_URL + "/logout", "POST", null, token, callback);
    }

    public void createTransaction(String token, String type, double amount, String category, String date, String note, ApiCallback callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("collection", "transactions");
            
            JSONObject recordData = new JSONObject();
            recordData.put("type", type);
            recordData.put("amount", amount);
            recordData.put("category", category);
            recordData.put("date", date);
            recordData.put("note", note);
            
            body.put("data", recordData);
            executeRequest(BASE_URL + "/data", "POST", body.toString(), token, callback);
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    public void getTransactions(String token, ApiCallback callback) {
        executeRequest(BASE_URL + "/data?collection=transactions", "GET", null, token, callback);
    }

    public void deleteTransaction(String token, String recordId, ApiCallback callback) {
        executeRequest(BASE_URL + "/data?id=" + recordId, "DELETE", null, token, callback);
    }

    private void executeRequest(final String urlString, final String method, final String jsonBody, final String token, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection conn = null;
                try {
                    URL url = new URL(urlString);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod(method);
                    conn.setConnectTimeout(15000);
                    conn.setReadTimeout(15000);
                    conn.setRequestProperty("Content-Type", "application/json");
                    if (token != null && !token.isEmpty()) {
                        conn.setRequestProperty("Authorization", "Bearer " + token);
                    }
                    
                    if (jsonBody != null) {
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

                    BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    reader.close();
                    final String response = sb.toString();

                    if (responseCode >= 200 && responseCode < 300) {
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess(response);
                            }
                        });
                    } else {
                        String errMsg = "Error " + responseCode;
                        try {
                            JSONObject obj = new JSONObject(response);
                            if (obj.has("error")) {
                                errMsg = obj.getString("error");
                            } else if (obj.has("message")) {
                                errMsg = obj.getString("message");
                            }
                        } catch (Exception ignored) {}
                        final String finalErr = errMsg;
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onError(finalErr);
                            }
                        });
                    }

                } catch (final Exception e) {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onError(e.getMessage());
                        }
                    });
                } finally {
                    if (conn != null) {
                        conn.disconnect();
                    }
                }
            }
        }).start();
    }
}