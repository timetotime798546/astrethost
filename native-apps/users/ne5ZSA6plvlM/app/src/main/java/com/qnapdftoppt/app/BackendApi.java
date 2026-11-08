package com.qnapdftoppt.app;

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
import java.util.ArrayList;
import java.util.List;

public class BackendApi {

    private static final String BASE_URL = "https://trumpledroid-api.cloudbeta28624.workers.dev";
    private static final String ADMIN_CONFIG_URL = "https://trumpledroid-apps-admin-47at498aq98hrg9633fugqd3.cloudbeta28624.workers.dev/get-app-config";
    private static final String PREF_NAME = "qna_ppt_prefs";

    private final Context context;
    private final Handler mainHandler;

    public interface ApiCallback {
        void onSuccess(String responseText, JSONObject jsonResponse);
        void onError(String errorMessage);
    }

    public interface ConfigCallback {
        void onConfigLoaded(String appTitle, String watermark);
    }

    public BackendApi(Context context) {
        this.context = context.getApplicationContext();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public String getAppId() {
        try {
            InputStream is = context.getAssets().open("app-meta.json");
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();
            JSONObject json = new JSONObject(new String(buffer, "UTF-8"));
            return json.optString("app_id", "com_qnapdftoppt_app");
        } catch (Exception e) {
            return "com_qnapdftoppt_app";
        }
    }

    public void saveToken(String token) {
        SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        pref.edit().putString("auth_token", token).apply();
    }

    public String getToken() {
        SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return pref.getString("auth_token", "");
    }

    public void logout() {
        SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        pref.edit().remove("auth_token").apply();
    }

    public boolean isLoggedIn() {
        return getToken() != null && !getToken().isEmpty();
    }

    // 1-Hour Cache implementation for Admin Settings
    public void fetchAdminConfig(final ConfigCallback callback) {
        final SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        long lastFetch = pref.getLong("admin_config_time", 0);
        long now = System.currentTimeMillis();

        if (now - lastFetch < 3600 * 1000) {
            String cachedTitle = pref.getString("admin_app_title", "Q&A PDF to PPT Converter");
            String cachedWatermark = pref.getString("admin_watermark", "Created with Q&A PDF to PPT Maker");
            if (callback != null) {
                callback.onConfigLoaded(cachedTitle, cachedWatermark);
            }
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String appId = getAppId();
                    URL url = new URL(ADMIN_CONFIG_URL + "?app_id=" + appId);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(8000);
                    conn.setReadTimeout(8000);

                    if (conn.getResponseCode() == 200) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) sb.append(line);
                        reader.close();

                        JSONObject obj = new JSONObject(sb.toString());
                        JSONObject config = obj.optJSONObject("config");
                        final String title = config != null ? config.optString("app_title", "Q&A PDF to PPT Converter") : "Q&A PDF to PPT Converter";
                        final String watermark = config != null ? config.optString("slide_footer_watermark", "Created with Q&A PDF to PPT Maker") : "Created with Q&A PDF to PPT Maker";

                        pref.edit()
                                .putString("admin_app_title", title)
                                .putString("admin_watermark", watermark)
                                .putLong("admin_config_time", System.currentTimeMillis())
                                .apply();

                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (callback != null) callback.onConfigLoaded(title, watermark);
                            }
                        });
                        return;
                    }
                } catch (Exception ignored) {
                }

                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (callback != null) {
                            callback.onConfigLoaded("Q&A PDF to PPT Converter", "Created with Q&A PDF to PPT Maker");
                        }
                    }
                });
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

                    makeRequest("/register", "POST", body.toString(), false, callback);
                } catch (Exception e) {
                    sendError(callback, "Registration error: " + e.getMessage());
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

                    makeRequest("/login", "POST", body.toString(), false, new ApiCallback() {
                        @Override
                        public void onSuccess(String responseText, JSONObject jsonResponse) {
                            if (jsonResponse != null && jsonResponse.has("token")) {
                                saveToken(jsonResponse.optString("token"));
                            }
                            if (callback != null) callback.onSuccess(responseText, jsonResponse);
                        }

                        @Override
                        public void onError(String errorMessage) {
                            if (callback != null) callback.onError(errorMessage);
                        }
                    });
                } catch (Exception e) {
                    sendError(callback, "Login error: " + e.getMessage());
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
                    sendError(callback, e.getMessage());
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
                    sendError(callback, e.getMessage());
                }
            }
        }).start();
    }

    public void saveConversionRecord(final String title, final int qCount, final String fileName, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject data = new JSONObject();
                    data.put("title", title);
                    data.put("question_count", qCount);
                    data.put("file_name", fileName);
                    data.put("created_at", String.valueOf(System.currentTimeMillis()));

                    JSONObject body = new JSONObject();
                    body.put("collection", "conversions");
                    body.put("data", data);

                    makeRequest("/data", "POST", body.toString(), true, callback);
                } catch (Exception e) {
                    sendError(callback, e.getMessage());
                }
            }
        }).start();
    }

    public void fetchConversionHistory(final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                makeRequest("/data?collection=conversions", "GET", null, true, callback);
            }
        }).start();
    }

    private void makeRequest(String endpoint, String method, String jsonBody, boolean useAuth, final ApiCallback callback) {
        try {
            URL url = new URL(BASE_URL + endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(method);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            if (useAuth) {
                String token = getToken();
                if (token != null && !token.isEmpty()) {
                    conn.setRequestProperty("Authorization", "Bearer " + token);
                }
            }

            if (jsonBody != null && !jsonBody.isEmpty()) {
                conn.setDoOutput(true);
                OutputStream os = conn.getOutputStream();
                os.write(jsonBody.getBytes("UTF-8"));
                os.flush();
                os.close();
            }

            int code = conn.getResponseCode();
            InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();

            final String responseStr = sb.toString();
            JSONObject json = null;
            try {
                json = new JSONObject(responseStr);
            } catch (Exception ignored) {}

            final JSONObject finalJson = json;
            if (code >= 200 && code < 300) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (callback != null) callback.onSuccess(responseStr, finalJson);
                    }
                });
            } else {
                final String err = json != null ? json.optString("error", json.optString("message", "Request failed (" + code + ")")) : "Request failed with code " + code;
                sendError(callback, err);
            }
        } catch (final Exception e) {
            sendError(callback, e.getMessage());
        }
    }

    private void sendError(final ApiCallback callback, final String message) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (callback != null) callback.onError(message != null ? message : "Network connection error");
            }
        });
    }
}