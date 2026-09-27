package com.expensemanager.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import org.json.JSONObject;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BackendApi {
    private static final String TAG = "BackendApi";
    private static final String BASE_URL = "https://trumpledroid-api.cloudbeta28624.workers.dev";
    private static final String PREF_NAME = "ExpensePrefs";
    private static final String KEY_TOKEN = "jwt_token";
    
    private final Context context;
    private final ExecutorService executor;
    private final Handler mainHandler;
    private String appId;

    public interface ApiCallback {
        void onSuccess(JSONObject response);
        void onError(String error);
    }

    public BackendApi(Context context) {
        this.context = context.getApplicationContext();
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.appId = loadAppId();
    }

    private String loadAppId() {
        try {
            InputStream is = context.getAssets().open("app-meta.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String jsonStr = new String(buffer, "UTF-8");
            JSONObject json = new JSONObject(jsonStr);
            return json.optString("app_id", "");
        } catch (Exception e) {
            Log.e(TAG, "Failed reading assets/app-meta.json", e);
            return "";
        }
    }

    public void setToken(String token) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_TOKEN, token).apply();
    }

    public String getToken() {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_TOKEN, null);
    }

    public void clearToken() {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_TOKEN).apply();
    }

    public boolean isLoggedIn() {
        return getToken() != null;
    }

    private void runOnMainThread(final ApiCallback callback, final JSONObject response, final String error) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (error != null) {
                    callback.onError(error);
                } else {
                    callback.onSuccess(response);
                }
            }
        });
    }

    private void makeRequest(final String method, final String endpoint, final JSONObject body, final boolean useToken, final ApiCallback callback) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection conn = null;
                try {
                    String urlStr = BASE_URL + endpoint;
                    URL url = new URL(urlStr);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod(method);
                    conn.setConnectTimeout(15000);
                    conn.setReadTimeout(15000);
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setRequestProperty("Accept", "application/json");

                    if (useToken) {
                        String token = getToken();
                        if (token != null) {
                            conn.setRequestProperty("Authorization", "Bearer " + token);
                        }
                    }

                    if (body != null && (method.equals("POST") || method.equals("PUT"))) {
                        conn.setDoOutput(true);
                        OutputStream os = conn.getOutputStream();
                        os.write(body.toString().getBytes("UTF-8"));
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
                        StringBuilder responseStr = new StringBuilder();
                        String line;
                        while ((line = br.readLine()) != null) {
                            responseStr.append(line);
                        }
                        br.close();

                        JSONObject responseJson = new JSONObject(responseStr.toString());
                        if (responseCode >= 200 && responseCode < 300) {
                            runOnMainThread(callback, responseJson, null);
                        } else {
                            String errMsg = responseJson.optString("error", responseJson.optString("message", "Request failed"));
                            runOnMainThread(callback, null, errMsg);
                        }
                    } else {
                        runOnMainThread(callback, null, "No response from server");
                    }

                } catch (Exception e) {
                    Log.e(TAG, "Request error", e);
                    runOnMainThread(callback, null, e.getMessage() != null ? e.getMessage() : "Network connection failed");
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
            JSONObject body = new JSONObject();
            body.put("app_id", appId);
            body.put("email", email);
            body.put("password", password);
            makeRequest("POST", "/register", body, false, callback);
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
            makeRequest("POST", "/login", body, false, callback);
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    public void logout(ApiCallback callback) {
        makeRequest("POST", "/logout", null, true, callback);
    }

    public void createExpense(String title, double amount, String category, String date, String note, ApiCallback callback) {
        try {
            JSONObject data = new JSONObject();
            data.put("title", title);
            data.put("amount", amount);
            data.put("category", category);
            data.put("date", date);
            data.put("note", note);

            JSONObject body = new JSONObject();
            body.put("collection", "expenses");
            body.put("data", data);

            makeRequest("POST", "/data", body, true, callback);
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    public void getExpenses(ApiCallback callback) {
        makeRequest("GET", "/data?collection=expenses", null, true, callback);
    }

    public void updateExpense(String recordId, String title, double amount, String category, String date, String note, ApiCallback callback) {
        try {
            JSONObject data = new JSONObject();
            data.put("title", title);
            data.put("amount", amount);
            data.put("category", category);
            data.put("date", date);
            data.put("note", note);

            JSONObject body = new JSONObject();
            body.put("id", recordId);
            body.put("data", data);

            makeRequest("PUT", "/data", body, true, callback);
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    public void deleteExpense(String recordId, ApiCallback callback) {
        makeRequest("DELETE", "/data?id=" + recordId, null, true, callback);
    }
}