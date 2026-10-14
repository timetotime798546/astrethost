package com.expenseconverterapp.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class BackendApi {
    private static final String TAG = "BackendApi";
    private static final String BASE_URL = "https://trumpledroid-api.cloudbeta28624.workers.dev";
    private static final String PREF_NAME = "ExpenseAppPrefs";
    private static final String KEY_TOKEN = "auth_token";

    private final Context context;

    public BackendApi(Context context) {
        this.context = context.getApplicationContext();
    }

    public String getAppId() {
        try {
            String jsonStr = loadAssetFile("app-meta.json");
            if (jsonStr != null) {
                JSONObject obj = new JSONObject(jsonStr);
                return obj.optString("app_id", "app_6acf0d72877ad");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reading app_id", e);
        }
        return "app_6acf0d72877ad";
    }

    private String loadAssetFile(String filename) {
        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(context.getAssets().open(filename)));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    public void saveToken(String token) {
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

    public JSONObject register(String email, String password) {
        try {
            JSONObject body = new JSONObject();
            body.put("app_id", getAppId());
            body.put("email", email);
            body.put("password", password);

            String responseStr = makeRequest("POST", "/register", body.toString(), null);
            return new JSONObject(responseStr);
        } catch (Exception e) {
            Log.e(TAG, "Register error", e);
            return null;
        }
    }

    public JSONObject login(String email, String password) {
        try {
            JSONObject body = new JSONObject();
            body.put("app_id", getAppId());
            body.put("email", email);
            body.put("password", password);

            String responseStr = makeRequest("POST", "/login", body.toString(), null);
            JSONObject json = new JSONObject(responseStr);
            if (json.optBoolean("success", false)) {
                String token = json.optString("token", null);
                if (token != null) {
                    saveToken(token);
                }
            }
            return json;
        } catch (Exception e) {
            Log.e(TAG, "Login error", e);
            return null;
        }
    }

    public JSONObject logout() {
        try {
            String responseStr = makeRequest("POST", "/logout", null, getToken());
            clearToken();
            return new JSONObject(responseStr);
        } catch (Exception e) {
            clearToken();
            return null;
        }
    }

    public JSONObject requestOtp(String email) {
        try {
            JSONObject body = new JSONObject();
            body.put("app_id", getAppId());
            body.put("email", email);
            String responseStr = makeRequest("POST", "/request-otp", body.toString(), null);
            return new JSONObject(responseStr);
        } catch (Exception e) {
            Log.e(TAG, "Request OTP error", e);
            return null;
        }
    }

    public JSONObject resetPassword(String email, String otp, String newPassword) {
        try {
            JSONObject body = new JSONObject();
            body.put("app_id", getAppId());
            body.put("email", email);
            body.put("otp", otp);
            body.put("new_password", newPassword);
            String responseStr = makeRequest("POST", "/reset-password", body.toString(), null);
            return new JSONObject(responseStr);
        } catch (Exception e) {
            Log.e(TAG, "Reset Password error", e);
            return null;
        }
    }

    public JSONObject createData(String collection, JSONObject data) {
        try {
            JSONObject body = new JSONObject();
            body.put("collection", collection);
            body.put("data", data);

            String responseStr = makeRequest("POST", "/data", body.toString(), getToken());
            return new JSONObject(responseStr);
        } catch (Exception e) {
            Log.e(TAG, "CreateData error", e);
            return null;
        }
    }

    public JSONObject readData(String collection) {
        try {
            String responseStr = makeRequest("GET", "/data?collection=" + collection, null, getToken());
            return new JSONObject(responseStr);
        } catch (Exception e) {
            Log.e(TAG, "ReadData error", e);
            return null;
        }
    }

    public JSONObject deleteData(String recordId) {
        try {
            String responseStr = makeRequest("DELETE", "/data?id=" + recordId, null, getToken());
            return new JSONObject(responseStr);
        } catch (Exception e) {
            Log.e(TAG, "DeleteData error", e);
            return null;
        }
    }

    private String makeRequest(String method, String endpoint, String jsonBody, String token) throws Exception {
        URL url = new URL(BASE_URL + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);

        if (token != null && !token.isEmpty()) {
            conn.setRequestProperty("Authorization", "Bearer " + token);
        }

        if (jsonBody != null && ("POST".equals(method) || "PUT".equals(method))) {
            conn.setDoOutput(true);
            OutputStream os = conn.getOutputStream();
            os.write(jsonBody.getBytes("UTF-8"));
            os.flush();
            os.close();
        }

        int responseCode = conn.getResponseCode();
        BufferedReader reader;
        if (responseCode >= 200 && responseCode < 300) {
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
        } else {
            reader = new BufferedReader(new InputStreamReader(
                    conn.getErrorStream() != null ? conn.getErrorStream() : conn.getInputStream(), "UTF-8"));
        }

        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();
        return sb.toString();
    }
}