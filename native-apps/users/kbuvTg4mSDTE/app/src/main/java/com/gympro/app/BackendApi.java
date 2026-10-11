package com.gympro.app;

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

public class BackendApi {

    private static final String BASE_URL = "https://trumpledroid-api.cloudbeta28624.workers.dev";
    private static final String PREFS_NAME = "gympro_prefs";
    private static final String KEY_TOKEN = "auth_token";

    private Context context;
    private String appId;
    private Handler mainHandler;

    public interface ApiCallback {
        void onSuccess(String responseStr, JSONObject jsonResponse);
        void onError(String errorMessage);
    }

    public BackendApi(Context context) {
        this.context = context.getApplicationContext();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.appId = loadAppId();
    }

    private String loadAppId() {
        try {
            InputStream is = context.getAssets().open("app-meta.json");
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();
            String jsonStr = new String(buffer, "UTF-8");
            JSONObject obj = new JSONObject(jsonStr);
            return obj.optString("app_id", "gympro_app_2025");
        } catch (Exception e) {
            return "gympro_app_2025";
        }
    }

    public String getAppId() {
        return appId;
    }

    public void saveToken(String token) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_TOKEN, token).apply();
    }

    public String getToken() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_TOKEN, "");
    }

    public void clearToken() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_TOKEN).apply();
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
                    body.put("app_id", appId);
                    body.put("email", email);
                    body.put("password", password);

                    sendHttpRequest("/register", "POST", body.toString(), false, callback);
                } catch (Exception e) {
                    postError(callback, e.getLocalizedMessage());
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
                    body.put("app_id", appId);
                    body.put("email", email);
                    body.put("password", password);

                    sendHttpRequest("/login", "POST", body.toString(), false, new ApiCallback() {
                        @Override
                        public void onSuccess(String responseStr, JSONObject jsonResponse) {
                            if (jsonResponse != null) {
                                String token = jsonResponse.optString("token", "");
                                if (!token.isEmpty()) {
                                    saveToken(token);
                                }
                            }
                            if (callback != null) {
                                callback.onSuccess(responseStr, jsonResponse);
                            }
                        }

                        @Override
                        public void onError(String errorMessage) {
                            if (callback != null) {
                                callback.onError(errorMessage);
                            }
                        }
                    });
                } catch (Exception e) {
                    postError(callback, e.getLocalizedMessage());
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
                    body.put("app_id", appId);
                    body.put("email", email);

                    sendHttpRequest("/request-otp", "POST", body.toString(), false, callback);
                } catch (Exception e) {
                    postError(callback, e.getLocalizedMessage());
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
                    body.put("app_id", appId);
                    body.put("email", email);
                    body.put("otp", otp);
                    body.put("new_password", newPassword);

                    sendHttpRequest("/reset-password", "POST", body.toString(), false, callback);
                } catch (Exception e) {
                    postError(callback, e.getLocalizedMessage());
                }
            }
        }).start();
    }

    public void logout(final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                sendHttpRequest("/logout", "POST", null, true, new ApiCallback() {
                    @Override
                    public void onSuccess(String responseStr, JSONObject jsonResponse) {
                        clearToken();
                        if (callback != null) {
                            callback.onSuccess(responseStr, jsonResponse);
                        }
                    }

                    @Override
                    public void onError(String errorMessage) {
                        clearToken();
                        if (callback != null) {
                            callback.onError(errorMessage);
                        }
                    }
                });
            }
        }).start();
    }

    public void createRecord(final String collection, final JSONObject dataJson, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject body = new JSONObject();
                    body.put("collection", collection);
                    body.put("data", dataJson);

                    sendHttpRequest("/data", "POST", body.toString(), true, callback);
                } catch (Exception e) {
                    postError(callback, e.getLocalizedMessage());
                }
            }
        }).start();
    }

    public void readRecords(final String collection, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String endpoint = "/data?collection=" + collection;
                sendHttpRequest(endpoint, "GET", null, true, callback);
            }
        }).start();
    }

    public void updateRecord(final String id, final JSONObject dataJson, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject body = new JSONObject();
                    body.put("id", id);
                    body.put("data", dataJson);

                    sendHttpRequest("/data", "PUT", body.toString(), true, callback);
                } catch (Exception e) {
                    postError(callback, e.getLocalizedMessage());
                }
            }
        }).start();
    }

    public void deleteRecord(final String id, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String endpoint = "/data?id=" + id;
                sendHttpRequest(endpoint, "DELETE", null, true, callback);
            }
        }).start();
    }

    private void sendHttpRequest(String endpoint, String method, String jsonBody, boolean sendAuth, final ApiCallback callback) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(BASE_URL + endpoint);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(method);
            conn.setConnectTimeout(12000);
            conn.setReadTimeout(12000);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setRequestProperty("Accept", "application/json");

            if (sendAuth) {
                String token = getToken();
                if (token != null && !token.isEmpty()) {
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

            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();

            final String responseString = sb.toString();
            JSONObject jsonRes = null;
            try {
                jsonRes = new JSONObject(responseString);
            } catch (Exception e) {
                // Not a direct JSON object
            }

            if (responseCode >= 200 && responseCode < 300) {
                postSuccess(callback, responseString, jsonRes);
            } else {
                String errMessage = "Server returned error " + responseCode;
                if (jsonRes != null) {
                    if (jsonRes.has("error")) errMessage = jsonRes.optString("error");
                    else if (jsonRes.has("message")) errMessage = jsonRes.optString("message");
                }
                postError(callback, errMessage);
            }

        } catch (Exception e) {
            postError(callback, "Network error: " + e.getLocalizedMessage());
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private void postSuccess(final ApiCallback callback, final String responseStr, final JSONObject jsonResponse) {
        if (callback == null) return;
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onSuccess(responseStr, jsonResponse);
            }
        });
    }

    private void postError(final ApiCallback callback, final String errorMsg) {
        if (callback == null) return;
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onError(errorMsg);
            }
        });
    }
}