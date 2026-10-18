package com.arbajsbz.app;

import android.content.Context;
import android.content.SharedPreferences;
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
import java.nio.charset.StandardCharsets;

public class BackendApi {
    private static final String BASE_URL = "https://trumpledroid-api.cloudbeta28624.workers.dev";
    private static final String PREF_NAME = "arbajsbz_auth_prefs";
    private static final String KEY_TOKEN = "auth_token";
    private static final String KEY_USER_EMAIL = "user_email";

    private final Context context;
    private final Handler mainHandler;

    public interface ApiCallback {
        void onSuccess(JSONObject response);
        void onError(String message);
    }

    public interface ReadCallback {
        void onSuccess(JSONArray records);
        void onError(String message);
    }

    public BackendApi(Context context) {
        this.context = context.getApplicationContext();
        this.mainHandler = new Handler(Looper.getMainLooper());
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
            return json.optString("app_id", "app_6ad4f1f9c22cb");
        } catch (Exception e) {
            return "app_6ad4f1f9c22cb";
        }
    }

    public void saveSession(String email, String token) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sp.edit().putString(KEY_USER_EMAIL, email).putString(KEY_TOKEN, token).apply();
    }

    public String getToken() {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sp.getString(KEY_TOKEN, "");
    }

    public String getUserEmail() {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sp.getString(KEY_USER_EMAIL, "");
    }

    public boolean isLoggedIn() {
        return !getToken().isEmpty();
    }

    public void clearSession() {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sp.edit().clear().apply();
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

                    HttpResult res = makeHttpRequest("POST", "/register", body.toString(), null);
                    if (res.isSuccess()) {
                        JSONObject json = new JSONObject(res.body);
                        postSuccess(callback, json);
                    } else {
                        String errMsg = parseErrorMessage(res.body, "Registration failed (" + res.code + ")");
                        postError(callback, errMsg);
                    }
                } catch (final Exception e) {
                    postError(callback, e.getMessage());
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

                    HttpResult res = makeHttpRequest("POST", "/login", body.toString(), null);
                    if (res.isSuccess()) {
                        JSONObject json = new JSONObject(res.body);
                        String token = json.optString("token", "");
                        if (!token.isEmpty()) {
                            saveSession(email, token);
                        }
                        postSuccess(callback, json);
                    } else {
                        String errMsg = parseErrorMessage(res.body, "Login failed (" + res.code + ")");
                        postError(callback, errMsg);
                    }
                } catch (final Exception e) {
                    postError(callback, e.getMessage());
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

                    HttpResult res = makeHttpRequest("POST", "/request-otp", body.toString(), null);
                    if (res.isSuccess()) {
                        JSONObject json = new JSONObject(res.body);
                        postSuccess(callback, json);
                    } else {
                        postError(callback, parseErrorMessage(res.body, "Failed to request OTP"));
                    }
                } catch (final Exception e) {
                    postError(callback, e.getMessage());
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

                    HttpResult res = makeHttpRequest("POST", "/reset-password", body.toString(), null);
                    if (res.isSuccess()) {
                        JSONObject json = new JSONObject(res.body);
                        postSuccess(callback, json);
                    } else {
                        postError(callback, parseErrorMessage(res.body, "Password reset failed"));
                    }
                } catch (final Exception e) {
                    postError(callback, e.getMessage());
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
                    clearSession();
                    if (!token.isEmpty()) {
                        makeHttpRequest("POST", "/logout", "{}", token);
                    }
                    JSONObject res = new JSONObject();
                    res.put("success", true);
                    postSuccess(callback, res);
                } catch (final Exception e) {
                    postError(callback, e.getMessage());
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

                    HttpResult res = makeHttpRequest("POST", "/data", body.toString(), getToken());
                    if (res.isSuccess()) {
                        JSONObject json = new JSONObject(res.body);
                        postSuccess(callback, json);
                    } else {
                        postError(callback, parseErrorMessage(res.body, "Create record failed"));
                    }
                } catch (final Exception e) {
                    postError(callback, e.getMessage());
                }
            }
        }).start();
    }

    public void readRecords(final String collection, final ReadCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String path = "/data?collection=" + collection;
                    HttpResult res = makeHttpRequest("GET", path, null, getToken());
                    if (res.isSuccess()) {
                        JSONObject json = new JSONObject(res.body);
                        final JSONArray records = json.optJSONArray("records");
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess(records != null ? records : new JSONArray());
                            }
                        });
                    } else {
                        final String errMsg = parseErrorMessage(res.body, "Fetch failed");
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onError(errMsg);
                            }
                        });
                    }
                } catch (final Exception e) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onError(e.getMessage());
                        }
                    });
                }
            }
        }).start();
    }

    public void updateRecord(final String id, final JSONObject data, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject body = new JSONObject();
                    body.put("id", id);
                    body.put("data", data);

                    HttpResult res = makeHttpRequest("PUT", "/data", body.toString(), getToken());
                    if (res.isSuccess()) {
                        JSONObject json = new JSONObject(res.body);
                        postSuccess(callback, json);
                    } else {
                        postError(callback, parseErrorMessage(res.body, "Update failed"));
                    }
                } catch (final Exception e) {
                    postError(callback, e.getMessage());
                }
            }
        }).start();
    }

    private HttpResult makeHttpRequest(String method, String endpoint, String jsonBody, String token) throws Exception {
        URL url = new URL(BASE_URL + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setConnectTimeout(12000);
        conn.setReadTimeout(12000);
        conn.setRequestProperty("Content-Type", "application/json");

        if (token != null && !token.isEmpty()) {
            conn.setRequestProperty("Authorization", "Bearer " + token);
        }

        if (jsonBody != null && (method.equals("POST") || method.equals("PUT"))) {
            conn.setDoOutput(true);
            OutputStream os = conn.getOutputStream();
            os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            os.flush();
            os.close();
        }

        int code = conn.getResponseCode();
        InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
        StringBuilder sb = new StringBuilder();
        if (is != null) {
            BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            br.close();
        }
        return new HttpResult(code, sb.toString());
    }

    private String parseErrorMessage(String responseBody, String defaultMsg) {
        try {
            JSONObject obj = new JSONObject(responseBody);
            if (obj.has("error")) return obj.getString("error");
            if (obj.has("message")) return obj.getString("message");
        } catch (Exception ignored) {}
        return defaultMsg;
    }

    private void postSuccess(final ApiCallback callback, final JSONObject json) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onSuccess(json);
            }
        });
    }

    private void postError(final ApiCallback callback, final String message) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onError(message);
            }
        });
    }

    private static class HttpResult {
        final int code;
        final String body;

        HttpResult(int code, String body) {
            this.code = code;
            this.body = body != null ? body : "";
        }

        boolean isSuccess() {
            return code >= 200 && code < 300;
        }
    }
}