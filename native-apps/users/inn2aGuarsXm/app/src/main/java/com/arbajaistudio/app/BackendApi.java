package com.arbajaistudio.app;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class BackendApi {
    private static final String BASE_URL = "https://trumpledroid-api.cloudbeta28624.workers.dev";
    private static final String PREF_NAME = "ARBAJ_AI_PREFS";
    private static final String KEY_TOKEN = "auth_token";
    private static final String KEY_USER_EMAIL = "user_email";
    private final Context context;

    public interface ApiCallback {
        void onSuccess(JSONObject response);
        void onError(String errorMessage);
    }

    public BackendApi(Context context) {
        this.context = context.getApplicationContext();
    }

    public String getAppId() {
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
            return obj.optString("app_id", "arbaj_ai_studio_001");
        } catch (Exception e) {
            return "arbaj_ai_studio_001";
        }
    }

    public void setAuthToken(String token) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_TOKEN, token).apply();
    }

    public String getAuthToken() {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_TOKEN, null);
    }

    public void setUserEmail(String email) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_USER_EMAIL, email).apply();
    }

    public String getUserEmail() {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_USER_EMAIL, "");
    }

    public boolean isLoggedIn() {
        String token = getAuthToken();
        return token != null && !token.trim().isEmpty();
    }

    public void logout(final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String token = getAuthToken();
                    if (token != null) {
                        executeRequest("/logout", "POST", null, token);
                    }
                    setAuthToken(null);
                    setUserEmail("");
                    if (callback != null) {
                        JSONObject res = new JSONObject();
                        res.put("success", true);
                        callback.onSuccess(res);
                    }
                } catch (Exception e) {
                    setAuthToken(null);
                    if (callback != null) {
                        callback.onError(e.getMessage());
                    }
                }
            }
        }).start();
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

                    JSONObject response = executeRequest("/register", "POST", body.toString(), null);
                    boolean success = response.optBoolean("success", false);
                    if (success) {
                        callback.onSuccess(response);
                    } else {
                        String errMsg = response.optString("message", response.optString("error", "Registration failed"));
                        callback.onError(errMsg);
                    }
                } catch (Exception e) {
                    callback.onError(e.getMessage() != null ? e.getMessage() : "Network error");
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

                    JSONObject response = executeRequest("/login", "POST", body.toString(), null);
                    boolean success = response.optBoolean("success", false);
                    if (success) {
                        String token = response.optString("token", "");
                        if (!token.isEmpty()) {
                            setAuthToken(token);
                            setUserEmail(email);
                            callback.onSuccess(response);
                        } else {
                            callback.onError("Login response missing token");
                        }
                    } else {
                        String errMsg = response.optString("message", response.optString("error", "Invalid credentials"));
                        callback.onError(errMsg);
                    }
                } catch (Exception e) {
                    callback.onError(e.getMessage() != null ? e.getMessage() : "Network error");
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
                    JSONObject response = executeRequest("/request-otp", "POST", body.toString(), null);
                    if (response.optBoolean("success", false)) {
                        callback.onSuccess(response);
                    } else {
                        callback.onError(response.optString("message", response.optString("error", "Failed to send OTP")));
                    }
                } catch (Exception e) {
                    callback.onError(e.getMessage() != null ? e.getMessage() : "Connection error");
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
                    JSONObject response = executeRequest("/reset-password", "POST", body.toString(), null);
                    if (response.optBoolean("success", false)) {
                        callback.onSuccess(response);
                    } else {
                        callback.onError(response.optString("message", response.optString("error", "Failed to reset password")));
                    }
                } catch (Exception e) {
                    callback.onError(e.getMessage() != null ? e.getMessage() : "Connection error");
                }
            }
        }).start();
    }

    public void createData(final String collection, final JSONObject dataObject, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String token = getAuthToken();
                    if (token == null) {
                        callback.onError("Not authenticated");
                        return;
                    }
                    JSONObject payload = new JSONObject();
                    payload.put("collection", collection);
                    payload.put("data", dataObject);

                    JSONObject response = executeRequest("/data", "POST", payload.toString(), token);
                    if (response.optBoolean("success", false) && response.has("record")) {
                        callback.onSuccess(response);
                    } else {
                        callback.onError(response.optString("message", response.optString("error", "Failed to save record")));
                    }
                } catch (Exception e) {
                    callback.onError(e.getMessage() != null ? e.getMessage() : "Creation error");
                }
            }
        }).start();
    }

    public void readData(final String collection, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String token = getAuthToken();
                    if (token == null) {
                        callback.onError("Not authenticated");
                        return;
                    }
                    String endpoint = "/data?collection=" + collection;
                    JSONObject response = executeRequest(endpoint, "GET", null, token);
                    if (response.optBoolean("success", false) && response.has("records")) {
                        callback.onSuccess(response);
                    } else {
                        callback.onError(response.optString("message", response.optString("error", "Failed to read records")));
                    }
                } catch (Exception e) {
                    callback.onError(e.getMessage() != null ? e.getMessage() : "Loading error");
                }
            }
        }).start();
    }

    public void deleteData(final String recordId, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String token = getAuthToken();
                    if (token == null) {
                        callback.onError("Not authenticated");
                        return;
                    }
                    String endpoint = "/data?id=" + recordId;
                    JSONObject response = executeRequest(endpoint, "DELETE", null, token);
                    if (response.optBoolean("success", false)) {
                        callback.onSuccess(response);
                    } else {
                        callback.onError(response.optString("message", response.optString("error", "Failed to delete record")));
                    }
                } catch (Exception e) {
                    callback.onError(e.getMessage() != null ? e.getMessage() : "Delete failed");
                }
            }
        }).start();
    }

    private JSONObject executeRequest(String endpoint, String method, String jsonBody, String token) throws Exception {
        URL url = new URL(BASE_URL + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");

        if (token != null && !token.trim().isEmpty()) {
            conn.setRequestProperty("Authorization", "Bearer " + token);
        }

        if (jsonBody != null && !jsonBody.isEmpty() && (method.equals("POST") || method.equals("PUT"))) {
            conn.setDoOutput(true);
            OutputStream os = conn.getOutputStream();
            os.write(jsonBody.getBytes("UTF-8"));
            os.flush();
            os.close();
        }

        int code = conn.getResponseCode();
        InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
        if (is == null) {
            JSONObject fallback = new JSONObject();
            fallback.put("success", false);
            fallback.put("error", "HTTP " + code);
            return fallback;
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        br.close();
        conn.disconnect();

        String raw = sb.toString().trim();
        if (raw.startsWith("{")) {
            return new JSONObject(raw);
        } else {
            JSONObject res = new JSONObject();
            res.put("success", code >= 200 && code < 300);
            res.put("raw", raw);
            return res;
        }
    }
}