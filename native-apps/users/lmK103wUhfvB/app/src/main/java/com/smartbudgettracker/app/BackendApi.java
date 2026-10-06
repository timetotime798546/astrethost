package com.smartbudgettracker.app;

import android.content.Context;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class BackendApi {
    private static final String TAG = "BackendApi";
    private static final String BASE_URL = "https://trumpledroid-api.cloudbeta28624.workers.dev";
    
    private Context context;
    private String appId;
    private String userToken;

    public interface ApiCallback {
        void onSuccess(String response);
        void onError(String errorMessage);
    }

    public BackendApi(Context context) {
        this.context = context.getApplicationContext();
        this.appId = readAppIdFromAssets();
        this.userToken = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE).getString("auth_token", null);
    }

    public String getAppId() {
        return appId;
    }

    public void setToken(String token) {
        this.userToken = token;
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
               .edit()
               .putString("auth_token", token)
               .apply();
    }

    public String getToken() {
        return userToken;
    }

    public void clearSession() {
        this.userToken = null;
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
               .edit()
               .remove("auth_token")
               .apply();
    }

    private String readAppIdFromAssets() {
        try {
            InputStream is = context.getAssets().open("app-meta.json");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            JSONObject obj = new JSONObject(sb.toString());
            return obj.optString("app_id", "smartbudgettracker");
        } catch (Exception e) {
            Log.e(TAG, "Error loading app-meta.json assets", e);
            return "smartbudgettracker";
        }
    }

    private void executePost(final String path, final JSONObject payload, final boolean useToken, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection conn = null;
                try {
                    URL url = new URL(BASE_URL + path);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);
                    conn.setConnectTimeout(15000);
                    conn.setReadTimeout(15000);

                    if (useToken && userToken != null) {
                        conn.setRequestProperty("Authorization", "Bearer " + userToken);
                    }

                    OutputStream os = conn.getOutputStream();
                    os.write(payload.toString().getBytes("UTF-8"));
                    os.close();

                    int responseCode = conn.getResponseCode();
                    InputStream is;
                    if (responseCode >= 200 && responseCode < 300) {
                        is = conn.getInputStream();
                    } else {
                        is = conn.getErrorStream();
                    }

                    BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }
                    br.close();

                    final String finalResponse = response.toString();
                    if (responseCode >= 200 && responseCode < 300) {
                        callback.onSuccess(finalResponse);
                    } else {
                        String errMsg = "Error response (" + responseCode + ")";
                        try {
                            JSONObject errJson = new JSONObject(finalResponse);
                            if (errJson.has("error")) errMsg = errJson.getString("error");
                            else if (errJson.has("message")) errMsg = errJson.getString("message");
                        } catch (Exception ignored) {}
                        callback.onError(errMsg);
                    }
                } catch (final Exception e) {
                    callback.onError(e.getMessage() != null ? e.getMessage() : "Network connection error");
                } finally {
                    if (conn != null) conn.disconnect();
                }
            }
        }).start();
    }

    private void executeGet(final String queryPath, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection conn = null;
                try {
                    URL url = new URL(BASE_URL + queryPath);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setConnectTimeout(15000);
                    conn.setReadTimeout(15000);

                    if (userToken != null) {
                        conn.setRequestProperty("Authorization", "Bearer " + userToken);
                    }

                    int responseCode = conn.getResponseCode();
                    InputStream is;
                    if (responseCode >= 200 && responseCode < 300) {
                        is = conn.getInputStream();
                    } else {
                        is = conn.getErrorStream();
                    }

                    BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }
                    br.close();

                    final String finalResponse = response.toString();
                    if (responseCode >= 200 && responseCode < 300) {
                        callback.onSuccess(finalResponse);
                    } else {
                        String errMsg = "Error response (" + responseCode + ")";
                        try {
                            JSONObject errJson = new JSONObject(finalResponse);
                            if (errJson.has("error")) errMsg = errJson.getString("error");
                            else if (errJson.has("message")) errMsg = errJson.getString("message");
                        } catch (Exception ignored) {}
                        callback.onError(errMsg);
                    }
                } catch (final Exception e) {
                    callback.onError(e.getMessage() != null ? e.getMessage() : "Network connection error");
                } finally {
                    if (conn != null) conn.disconnect();
                }
            }
        }).start();
    }

    private void executeDelete(final String queryPath, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection conn = null;
                try {
                    URL url = new URL(BASE_URL + queryPath);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("DELETE");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setConnectTimeout(15000);
                    conn.setReadTimeout(15000);

                    if (userToken != null) {
                        conn.setRequestProperty("Authorization", "Bearer " + userToken);
                    }

                    int responseCode = conn.getResponseCode();
                    InputStream is;
                    if (responseCode >= 200 && responseCode < 300) {
                        is = conn.getInputStream();
                    } else {
                        is = conn.getErrorStream();
                    }

                    BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }
                    br.close();

                    final String finalResponse = response.toString();
                    if (responseCode >= 200 && responseCode < 300) {
                        callback.onSuccess(finalResponse);
                    } else {
                        String errMsg = "Error response (" + responseCode + ")";
                        try {
                            JSONObject errJson = new JSONObject(finalResponse);
                            if (errJson.has("error")) errMsg = errJson.getString("error");
                            else if (errJson.has("message")) errMsg = errJson.getString("message");
                        } catch (Exception ignored) {}
                        callback.onError(errMsg);
                    }
                } catch (final Exception e) {
                    callback.onError(e.getMessage() != null ? e.getMessage() : "Network connection error");
                } finally {
                    if (conn != null) conn.disconnect();
                }
            }
        }).start();
    }

    private void executePut(final String path, final JSONObject payload, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection conn = null;
                try {
                    URL url = new URL(BASE_URL + path);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("PUT");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);
                    conn.setConnectTimeout(15000);
                    conn.setReadTimeout(15000);

                    if (userToken != null) {
                        conn.setRequestProperty("Authorization", "Bearer " + userToken);
                    }

                    OutputStream os = conn.getOutputStream();
                    os.write(payload.toString().getBytes("UTF-8"));
                    os.close();

                    int responseCode = conn.getResponseCode();
                    InputStream is;
                    if (responseCode >= 200 && responseCode < 300) {
                        is = conn.getInputStream();
                    } else {
                        is = conn.getErrorStream();
                    }

                    BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }
                    br.close();

                    final String finalResponse = response.toString();
                    if (responseCode >= 200 && responseCode < 300) {
                        callback.onSuccess(finalResponse);
                    } else {
                        String errMsg = "Error response (" + responseCode + ")";
                        try {
                            JSONObject errJson = new JSONObject(finalResponse);
                            if (errJson.has("error")) errMsg = errJson.getString("error");
                            else if (errJson.has("message")) errMsg = errJson.getString("message");
                        } catch (Exception ignored) {}
                        callback.onError(errMsg);
                    }
                } catch (final Exception e) {
                    callback.onError(e.getMessage() != null ? e.getMessage() : "Network connection error");
                } finally {
                    if (conn != null) conn.disconnect();
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
            executePost("/register", body, false, callback);
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
            executePost("/login", body, false, callback);
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    public void requestOtp(String email, ApiCallback callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("app_id", appId);
            body.put("email", email);
            executePost("/request-otp", body, false, callback);
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
            executePost("/reset-password", body, false, callback);
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    public void logout(ApiCallback callback) {
        try {
            JSONObject emptyPayload = new JSONObject();
            executePost("/logout", emptyPayload, true, callback);
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    public void createTransaction(String title, double amount, String type, String category, String date, ApiCallback callback) {
        try {
            JSONObject data = new JSONObject();
            data.put("title", title);
            data.put("amount", amount);
            data.put("type", type);
            data.put("category", category);
            data.put("date", date);

            JSONObject wrapper = new JSONObject();
            wrapper.put("collection", "transactions");
            wrapper.put("data", data);

            executePost("/data", wrapper, true, callback);
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    public void fetchTransactions(ApiCallback callback) {
        executeGet("/data?collection=transactions", callback);
    }

    public void updateTransaction(String id, String title, double amount, String type, String category, String date, ApiCallback callback) {
        try {
            JSONObject data = new JSONObject();
            data.put("title", title);
            data.put("amount", amount);
            data.put("type", type);
            data.put("category", category);
            data.put("date", date);

            JSONObject wrapper = new JSONObject();
            wrapper.put("id", id);
            wrapper.put("data", data);

            executePut("/data", wrapper, callback);
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    public void deleteTransaction(String id, ApiCallback callback) {
        executeDelete("/data?id=" + id, callback);
    }
}