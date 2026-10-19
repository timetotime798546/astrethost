package com.quickpaywallet.app;

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

    public static final String API_BASE_URL = "https://trumpledroid-api.cloudbeta28624.workers.dev";
    private static final String PREF_NAME = "quickpay_auth_pref";
    private static final String KEY_TOKEN = "auth_token";
    private static final String KEY_EMAIL = "auth_email";

    private final Context context;
    private final Handler mainHandler;

    public interface ApiCallback {
        void onSuccess(JSONObject response);
        void onError(String errorMessage);
    }

    public BackendApi(Context context) {
        this.context = context.getApplicationContext();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Reads dynamic app_id from assets/app-meta.json
     */
    public static String getAppId(Context ctx) {
        try {
            InputStream is = ctx.getAssets().open("app-meta.json");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            JSONObject obj = new JSONObject(sb.toString());
            return obj.optString("app_id", "app_6ad5d5a73b5c4");
        } catch (Exception e) {
            return "app_6ad5d5a73b5c4";
        }
    }

    public void saveSession(String token, String email) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sp.edit().putString(KEY_TOKEN, token).putString(KEY_EMAIL, email).apply();
    }

    public String getToken() {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getString(KEY_TOKEN, null);
    }

    public String getSavedEmail() {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getString(KEY_EMAIL, "");
    }

    public boolean isLoggedIn() {
        String token = getToken();
        return token != null && !token.trim().isEmpty();
    }

    public void logout(final ApiCallback callback) {
        final String token = getToken();
        executeRequest("POST", "/logout", null, token, new ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                clearSession();
                if (callback != null) callback.onSuccess(response);
            }

            @Override
            public void onError(String errorMessage) {
                clearSession();
                if (callback != null) callback.onError(errorMessage);
            }
        });
    }

    public void clearSession() {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sp.edit().clear().apply();
    }

    /**
     * Account Registration via POST /register (Does NOT return token)
     */
    public void register(String email, String password, final ApiCallback callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("app_id", getAppId(context));
            body.put("email", email);
            body.put("password", password);
            executeRequest("POST", "/register", body.toString(), null, callback);
        } catch (Exception e) {
            callback.onError("Registration error: " + e.getMessage());
        }
    }

    /**
     * User Login via POST /login (Returns token)
     */
    public void login(final String email, String password, final ApiCallback callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("app_id", getAppId(context));
            body.put("email", email);
            body.put("password", password);
            executeRequest("POST", "/login", body.toString(), null, new ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    String token = response.optString("token", "");
                    if (token.isEmpty()) {
                        JSONObject data = response.optJSONObject("data");
                        if (data != null) {
                            token = data.optString("token", "");
                        }
                    }
                    if (!token.isEmpty()) {
                        saveSession(token, email);
                    }
                    callback.onSuccess(response);
                }

                @Override
                public void onError(String errorMessage) {
                    callback.onError(errorMessage);
                }
            });
        } catch (Exception e) {
            callback.onError("Login error: " + e.getMessage());
        }
    }

    /**
     * Step 1: Request OTP for password reset
     */
    public void requestOtp(String email, final ApiCallback callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("app_id", getAppId(context));
            body.put("email", email);
            executeRequest("POST", "/request-otp", body.toString(), null, callback);
        } catch (Exception e) {
            callback.onError("OTP Request error: " + e.getMessage());
        }
    }

    /**
     * Step 2: Reset password using OTP
     */
    public void resetPassword(String email, String otp, String newPassword, final ApiCallback callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("app_id", getAppId(context));
            body.put("email", email);
            body.put("otp", otp);
            body.put("new_password", newPassword);
            executeRequest("POST", "/reset-password", body.toString(), null, callback);
        } catch (Exception e) {
            callback.onError("Reset Password error: " + e.getMessage());
        }
    }

    /**
     * Create Record via POST /data
     */
    public void createData(String collection, JSONObject data, final ApiCallback callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("collection", collection);
            body.put("data", data);
            executeRequest("POST", "/data", body.toString(), getToken(), callback);
        } catch (Exception e) {
            callback.onError("Create record error: " + e.getMessage());
        }
    }

    /**
     * Read Records via GET /data?collection={name}
     */
    public void readData(String collection, final ApiCallback callback) {
        executeRequest("GET", "/data?collection=" + collection, null, getToken(), callback);
    }

    /**
     * Delete Record via DELETE /data?id={record_id}
     */
    public void deleteData(String recordId, final ApiCallback callback) {
        executeRequest("DELETE", "/data?id=" + recordId, null, getToken(), callback);
    }

    private void executeRequest(final String method, final String endpoint, final String jsonBody,
                                final String token, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection conn = null;
                try {
                    URL url = new URL(API_BASE_URL + endpoint);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod(method);
                    conn.setConnectTimeout(12000);
                    conn.setReadTimeout(12000);
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setRequestProperty("Accept", "application/json");

                    if (token != null && !token.trim().isEmpty()) {
                        conn.setRequestProperty("Authorization", "Bearer " + token);
                    }

                    if (jsonBody != null && (method.equals("POST") || method.equals("PUT"))) {
                        conn.setDoOutput(true);
                        OutputStream os = conn.getOutputStream();
                        os.write(jsonBody.getBytes("UTF-8"));
                        os.flush();
                        os.close();
                    }

                    int responseCode = conn.getResponseCode();
                    InputStream stream = (responseCode >= 200 && responseCode < 400)
                            ? conn.getInputStream()
                            : conn.getErrorStream();

                    String responseString = "";
                    if (stream != null) {
                        BufferedReader br = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = br.readLine()) != null) {
                            sb.append(line);
                        }
                        br.close();
                        responseString = sb.toString();
                    }

                    final JSONObject parsedJson = (responseString.trim().startsWith("{"))
                            ? new JSONObject(responseString)
                            : new JSONObject();

                    if (responseCode >= 200 && responseCode < 300) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess(parsedJson);
                            }
                        });
                    } else {
                        String errMsg = parsedJson.optString("error", "");
                        if (errMsg.isEmpty()) {
                            errMsg = parsedJson.optString("message", "Request failed with HTTP " + responseCode);
                        }
                        final String finalMsg = errMsg;
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onError(finalMsg);
                            }
                        });
                    }

                } catch (final Exception e) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onError("Network error: " + e.getMessage());
                        }
                    });
                } finally {
                    if (conn != null) {
                        conn.disconnect();
                    }
                }
            }
        }).start();
    }
}