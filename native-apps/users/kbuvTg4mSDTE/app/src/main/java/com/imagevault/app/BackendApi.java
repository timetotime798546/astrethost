package com.imagevault.app;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class BackendApi {
    private static final String PREFS_NAME = "imagevault_prefs";
    private static final String KEY_TOKEN = "auth_token";
    private static final String KEY_EMAIL = "auth_email";
    private static final String BASE_URL = "https://trumpledroid-api.cloudbeta28624.workers.dev";

    private final Context context;
    private final SharedPreferences prefs;

    public interface ApiCallback {
        void onSuccess(JSONObject response);
        void onError(String errorMessage);
    }

    public BackendApi(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public String getAppId() {
        try {
            InputStream is = context.getAssets().open("app-meta.json");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            JSONObject json = new JSONObject(sb.toString());
            return json.optString("app_id", "imagevault_default");
        } catch (Exception e) {
            return "imagevault_default";
        }
    }

    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    public void setToken(String token) {
        prefs.edit().putString(KEY_TOKEN, token).apply();
    }

    public String getSavedEmail() {
        return prefs.getString(KEY_EMAIL, "");
    }

    public void setSavedEmail(String email) {
        prefs.edit().putString(KEY_EMAIL, email).apply();
    }

    public void clearAuth() {
        prefs.edit().remove(KEY_TOKEN).remove(KEY_EMAIL).apply();
    }

    public boolean isLoggedIn() {
        String token = getToken();
        return token != null && !token.trim().isEmpty();
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

                    HttpResponse response = executeRequest(BASE_URL + "/register", "POST", body.toString(), null);
                    if (response.code >= 200 && response.code < 300) {
                        JSONObject obj = new JSONObject(response.body);
                        callback.onSuccess(obj);
                    } else {
                        String errMsg = extractError(response.body, "Registration failed (" + response.code + ")");
                        callback.onError(errMsg);
                    }
                } catch (Exception e) {
                    callback.onError("Network error: " + e.getMessage());
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

                    HttpResponse response = executeRequest(BASE_URL + "/login", "POST", body.toString(), null);
                    if (response.code >= 200 && response.code < 300) {
                        JSONObject obj = new JSONObject(response.body);
                        String token = obj.optString("token", "");
                        if (!token.isEmpty()) {
                            setToken(token);
                            setSavedEmail(email);
                            callback.onSuccess(obj);
                        } else {
                            callback.onError("Authentication token missing in login response");
                        }
                    } else {
                        String errMsg = extractError(response.body, "Login failed (" + response.code + ")");
                        callback.onError(errMsg);
                    }
                } catch (Exception e) {
                    callback.onError("Network error: " + e.getMessage());
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

                    HttpResponse response = executeRequest(BASE_URL + "/request-otp", "POST", body.toString(), null);
                    if (response.code >= 200 && response.code < 300) {
                        callback.onSuccess(new JSONObject(response.body));
                    } else {
                        callback.onError(extractError(response.body, "OTP request failed"));
                    }
                } catch (Exception e) {
                    callback.onError("Network error: " + e.getMessage());
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

                    HttpResponse response = executeRequest(BASE_URL + "/reset-password", "POST", body.toString(), null);
                    if (response.code >= 200 && response.code < 300) {
                        callback.onSuccess(new JSONObject(response.body));
                    } else {
                        callback.onError(extractError(response.body, "Password reset failed"));
                    }
                } catch (Exception e) {
                    callback.onError("Network error: " + e.getMessage());
                }
            }
        }).start();
    }

    public void logout(final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String token = getToken();
                    executeRequest(BASE_URL + "/logout", "POST", "{}", token);
                } catch (Exception ignored) {
                } finally {
                    clearAuth();
                    if (callback != null) {
                        callback.onSuccess(new JSONObject());
                    }
                }
            }
        }).start();
    }

    private static class HttpResponse {
        int code;
        String body;
        HttpResponse(int c, String b) {
            code = c;
            body = b;
        }
    }

    private HttpResponse executeRequest(String targetUrl, String method, String payload, String authToken) throws Exception {
        URL url = new URL(targetUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(20000);
        conn.setRequestProperty("Content-Type", "application/json");

        if (authToken != null && !authToken.trim().isEmpty()) {
            conn.setRequestProperty("Authorization", "Bearer " + authToken);
        }

        if (payload != null && (method.equals("POST") || method.equals("PUT"))) {
            conn.setDoOutput(true);
            OutputStream os = conn.getOutputStream();
            os.write(payload.getBytes(StandardCharsets.UTF_8));
            os.flush();
            os.close();
        }

        int code = conn.getResponseCode();
        InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
        StringBuilder sb = new StringBuilder();
        if (is != null) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
        }
        conn.disconnect();
        return new HttpResponse(code, sb.toString());
    }

    private String extractError(String jsonBody, String fallback) {
        try {
            JSONObject obj = new JSONObject(jsonBody);
            if (obj.has("error")) return obj.getString("error");
            if (obj.has("message")) return obj.getString("message");
        } catch (Exception ignored) {
        }
        return fallback;
    }
}