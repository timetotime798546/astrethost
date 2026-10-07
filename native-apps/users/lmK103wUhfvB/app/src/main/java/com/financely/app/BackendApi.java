package com.financely.app;

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
    private String cachedAppId = null;

    public BackendApi(Context context) {
        this.context = context.getApplicationContext();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    private String getAppId() {
        if (cachedAppId != null) {
            return cachedAppId;
        }
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
            cachedAppId = meta.optString("app_id", "financely");
            return cachedAppId;
        } catch (Exception e) {
            return "financely";
        }
    }

    private void runOnMain(final ApiCallback callback, final boolean isSuccess, final JSONObject response, final String error) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (callback != null) {
                    if (isSuccess) {
                        callback.onSuccess(response);
                    } else {
                        callback.onError(error);
                    }
                }
            }
        });
    }

    private void performPostRequest(final String endpoint, final JSONObject payload, final String token, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection conn = null;
                try {
                    URL url = new URL(BASE_URL + endpoint);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                    conn.setRequestProperty("Accept", "application/json");
                    if (token != null && !token.isEmpty()) {
                        conn.setRequestProperty("Authorization", "Bearer " + token);
                    }
                    conn.setDoOutput(true);
                    conn.setConnectTimeout(15000);
                    conn.setReadTimeout(15000);

                    OutputStream os = conn.getOutputStream();
                    os.write(payload.toString().getBytes("UTF-8"));
                    os.flush();
                    os.close();

                    int responseCode = conn.getResponseCode();
                    InputStream is;
                    if (responseCode >= 200 && responseCode < 300) {
                        is = conn.getInputStream();
                    } else {
                        is = conn.getErrorStream();
                    }

                    if (is != null) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            sb.append(line);
                        }
                        reader.close();

                        JSONObject result = new JSONObject(sb.toString());
                        if (responseCode >= 200 && responseCode < 300) {
                            runOnMain(callback, true, result, null);
                        } else {
                            String err = result.optString("error", "Request failed with code " + responseCode);
                            runOnMain(callback, false, null, err);
                        }
                    } else {
                        runOnMain(callback, false, null, "Null response stream");
                    }

                } catch (Exception e) {
                    runOnMain(callback, false, null, "Error: " + e.getMessage());
                } finally {
                    if (conn != null) {
                        conn.disconnect();
                    }
                }
            }
        }).start();
    }

    private void performGetRequest(final String endpointWithParams, final String token, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection conn = null;
                try {
                    URL url = new URL(BASE_URL + endpointWithParams);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("Accept", "application/json");
                    if (token != null && !token.isEmpty()) {
                        conn.setRequestProperty("Authorization", "Bearer " + token);
                    }
                    conn.setConnectTimeout(15000);
                    conn.setReadTimeout(15000);

                    int responseCode = conn.getResponseCode();
                    InputStream is;
                    if (responseCode >= 200 && responseCode < 300) {
                        is = conn.getInputStream();
                    } else {
                        is = conn.getErrorStream();
                    }

                    if (is != null) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            sb.append(line);
                        }
                        reader.close();

                        JSONObject result = new JSONObject(sb.toString());
                        if (responseCode >= 200 && responseCode < 300) {
                            runOnMain(callback, true, result, null);
                        } else {
                            String err = result.optString("error", "Request failed with code " + responseCode);
                            runOnMain(callback, false, null, err);
                        }
                    } else {
                        runOnMain(callback, false, null, "Null response stream");
                    }

                } catch (Exception e) {
                    runOnMain(callback, false, null, "Error: " + e.getMessage());
                } finally {
                    if (conn != null) {
                        conn.disconnect();
                    }
                }
            }
        }).start();
    }

    private void performDeleteRequest(final String endpointWithParams, final String token, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection conn = null;
                try {
                    URL url = new URL(BASE_URL + endpointWithParams);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("DELETE");
                    conn.setRequestProperty("Accept", "application/json");
                    if (token != null && !token.isEmpty()) {
                        conn.setRequestProperty("Authorization", "Bearer " + token);
                    }
                    conn.setConnectTimeout(15000);
                    conn.setReadTimeout(15000);

                    int responseCode = conn.getResponseCode();
                    InputStream is;
                    if (responseCode >= 200 && responseCode < 300) {
                        is = conn.getInputStream();
                    } else {
                        is = conn.getErrorStream();
                    }

                    if (is != null) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            sb.append(line);
                        }
                        reader.close();

                        JSONObject result = new JSONObject(sb.toString());
                        if (responseCode >= 200 && responseCode < 300) {
                            runOnMain(callback, true, result, null);
                        } else {
                            String err = result.optString("error", "Request failed with code " + responseCode);
                            runOnMain(callback, false, null, err);
                        }
                    } else {
                        runOnMain(callback, false, null, "Null response stream");
                    }

                } catch (Exception e) {
                    runOnMain(callback, false, null, "Error: " + e.getMessage());
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
            body.put("app_id", getAppId());
            body.put("email", email);
            body.put("password", password);
            performPostRequest("/register", body, null, callback);
        } catch (Exception e) {
            callback.onError("JSON creation error: " + e.getMessage());
        }
    }

    public void login(String email, String password, ApiCallback callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("app_id", getAppId());
            body.put("email", email);
            body.put("password", password);
            performPostRequest("/login", body, null, callback);
        } catch (Exception e) {
            callback.onError("JSON creation error: " + e.getMessage());
        }
    }

    public void requestOtp(String email, ApiCallback callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("app_id", getAppId());
            body.put("email", email);
            performPostRequest("/request-otp", body, null, callback);
        } catch (Exception e) {
            callback.onError("JSON creation error: " + e.getMessage());
        }
    }

    public void resetPassword(String email, String otp, String newPassword, ApiCallback callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("app_id", getAppId());
            body.put("email", email);
            body.put("otp", otp);
            body.put("new_password", newPassword);
            performPostRequest("/reset-password", body, null, callback);
        } catch (Exception e) {
            callback.onError("JSON creation error: " + e.getMessage());
        }
    }

    public void logout(String token, ApiCallback callback) {
        performPostRequest("/logout", new JSONObject(), token, callback);
    }

    public void createTransaction(String token, String type, double amount, String category, String description, String date, ApiCallback callback) {
        try {
            JSONObject dataObj = new JSONObject();
            dataObj.put("type", type);
            dataObj.put("amount", amount);
            dataObj.put("category", category);
            dataObj.put("description", description);
            dataObj.put("date", date);

            JSONObject body = new JSONObject();
            body.put("collection", "transactions");
            body.put("data", dataObj);

            performPostRequest("/data", body, token, callback);
        } catch (Exception e) {
            callback.onError("JSON error: " + e.getMessage());
        }
    }

    public void getTransactions(String token, ApiCallback callback) {
        performGetRequest("/data?collection=transactions", token, callback);
    }

    public void deleteTransaction(String token, String recordId, ApiCallback callback) {
        performDeleteRequest("/data?id=" + recordId, token, callback);
    }
}