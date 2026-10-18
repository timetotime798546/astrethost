package com.aviatorpro.app;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class BackendApi {
    private static final String BASE_URL = "https://trumpledroid-api.cloudbeta28624.workers.dev";
    private static final String PREF_NAME = "aviator_backend_prefs";
    private static final String KEY_TOKEN = "auth_token";
    private static final String KEY_USER_EMAIL = "user_email";

    private Context context;

    public interface ApiCallback {
        void onSuccess(JSONObject response);
        void onError(String error);
    }

    public BackendApi(Context context) {
        this.context = context;
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
            return obj.optString("app_id", "app_6ad4b0e7afe7b");
        } catch (Exception e) {
            return "app_6ad4b0e7afe7b";
        }
    }

    public void saveToken(String token, String email) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sp.edit().putString(KEY_TOKEN, token).putString(KEY_USER_EMAIL, email).apply();
    }

    public String getToken() {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sp.getString(KEY_TOKEN, "");
    }

    public String getUserEmail() {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sp.getString(KEY_USER_EMAIL, "guest@aviator.local");
    }

    public void clearAuth() {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sp.edit().remove(KEY_TOKEN).remove(KEY_USER_EMAIL).apply();
    }

    public boolean isLoggedIn() {
        return !getToken().isEmpty();
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
                    notifyError(callback, e.getMessage());
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
                    notifyError(callback, e.getMessage());
                }
            }
        }).start();
    }

    public void requestOtp(final String email, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject body = new JSONObject();
                    body.put("app_id", getAppId());
                    body.put("email", email);
                    makeRequest("/request-otp", "POST", body.toString(), false, callback);
                } catch (Exception e) {
                    notifyError(callback, e.getMessage());
                }
            }
        }).start();
    }

    public void resetPassword(final String email, final String otp, final String newPassword, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject body = new JSONObject();
                    body.put("app_id", getAppId());
                    body.put("email", email);
                    body.put("otp", otp);
                    body.put("new_password", newPassword);
                    makeRequest("/reset-password", "POST", body.toString(), false, callback);
                } catch (Exception e) {
                    notifyError(callback, e.getMessage());
                }
            }
        }).start();
    }

    public void createRecord(final String collection, final JSONObject data, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject body = new JSONObject();
                    body.put("collection", collection);
                    body.put("data", data);
                    makeRequest("/data", "POST", body.toString(), true, callback);
                } catch (Exception e) {
                    notifyError(callback, e.getMessage());
                }
            }
        }).start();
    }

    public void getRecords(final String collection, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    makeRequest("/data?collection=" + collection, "GET", null, true, callback);
                } catch (Exception e) {
                    notifyError(callback, e.getMessage());
                }
            }
        }).start();
    }

    public void updateRecord(final String recordId, final JSONObject data, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject body = new JSONObject();
                    body.put("id", recordId);
                    body.put("data", data);
                    makeRequest("/data", "PUT", body.toString(), true, callback);
                } catch (Exception e) {
                    notifyError(callback, e.getMessage());
                }
            }
        }).start();
    }

    private void makeRequest(String endpoint, String method, String jsonBody, boolean authRequired, ApiCallback callback) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(BASE_URL + endpoint);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(method);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setRequestProperty("Accept", "application/json");

            if (authRequired) {
                String token = getToken();
                if (!token.isEmpty()) {
                    conn.setRequestProperty("Authorization", "Bearer " + token);
                }
            }

            if (jsonBody != null && (method.equals("POST") || method.equals("PUT"))) {
                conn.setDoOutput(true);
                OutputStream os = conn.getOutputStream();
                os.write(jsonBody.getBytes("UTF-8"));
                os.flush();
                os.close();
            }

            int responseCode = conn.getResponseCode();
            InputStream is = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream();

            if (is == null) {
                notifyError(callback, "Server returned " + responseCode);
                return;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();

            JSONObject resJson;
            try {
                resJson = new JSONObject(sb.toString());
            } catch (Exception ex) {
                resJson = new JSONObject();
                resJson.put("raw", sb.toString());
                resJson.put("success", responseCode >= 200 && responseCode < 300);
            }

            if (responseCode >= 200 && responseCode < 300) {
                notifySuccess(callback, resJson);
            } else {
                notifyError(callback, resJson.optString("error", resJson.optString("message", "Error " + responseCode)));
            }
        } catch (Exception e) {
            notifyError(callback, "Network error: " + e.getMessage());
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private void notifySuccess(final ApiCallback cb, final JSONObject json) {
        if (cb != null) cb.onSuccess(json);
    }

    private void notifyError(final ApiCallback cb, final String error) {
        if (cb != null) cb.onError(error);
    }
}