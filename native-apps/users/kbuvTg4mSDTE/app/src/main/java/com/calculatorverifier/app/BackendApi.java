package com.calculatorverifier.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONObject;

public class BackendApi {
    private static final String TAG = "BackendApi";
    private static final String PREFS_NAME = "calculator_verifier_prefs";
    private static final String KEY_TOKEN = "auth_token";
    private static final String KEY_EMAIL = "auth_email";
    
    private final Context context;
    private final SharedPreferences prefs;
    private final String apiBaseUrl = "https://trumpledroid-api.cloudbeta28624.workers.dev";

    public interface ApiCallback {
        void onSuccess(JSONObject response);
        void onFailure(String error);
    }

    public BackendApi(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    public String getSavedEmail() {
        return prefs.getString(KEY_EMAIL, "");
    }

    public void saveSession(String token, String email) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_EMAIL, email)
            .apply();
    }

    public void clearSession() {
        prefs.edit()
            .remove(KEY_TOKEN)
            .remove(KEY_EMAIL)
            .apply();
    }

    public String getAppId() {
        try {
            InputStream is = context.getAssets().open("app-meta.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String jsonStr = new String(buffer, "UTF-8");
            JSONObject json = new JSONObject(jsonStr);
            return json.optString("app_id", "com.calculatorverifier.app");
        } catch (Exception e) {
            Log.e(TAG, "Error reading assets app-meta.json", e);
            return "com.calculatorverifier.app";
        }
    }

    public void register(final String email, final String password, final ApiCallback callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("app_id", getAppId());
            body.put("email", email);
            body.put("password", password);

            performRequest("/register", body, null, new ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    callback.onSuccess(response);
                }

                @Override
                public void onFailure(String error) {
                    callback.onFailure(error);
                }
            });
        } catch (Exception e) {
            callback.onFailure("JSON structure error: " + e.getMessage());
        }
    }

    public void login(final String email, final String password, final ApiCallback callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("app_id", getAppId());
            body.put("email", email);
            body.put("password", password);

            performRequest("/login", body, null, new ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    try {
                        if (response.optBoolean("success", false)) {
                            String token = response.optString("token", "");
                            if (!token.isEmpty()) {
                                saveSession(token, email);
                                callback.onSuccess(response);
                            } else {
                                callback.onFailure("Authentication response missing token");
                            }
                        } else {
                            callback.onFailure(response.optString("message", "Login rejected"));
                        }
                    } catch (Exception e) {
                        callback.onFailure("Response evaluation error: " + e.getMessage());
                    }
                }

                @Override
                public void onFailure(String error) {
                    callback.onFailure(error);
                }
            });
        } catch (Exception e) {
            callback.onFailure("JSON login error: " + e.getMessage());
        }
    }

    public void logout(final ApiCallback callback) {
        try {
            String token = getToken();
            JSONObject body = new JSONObject();
            clearSession();
            
            performRequest("/logout", body, token, new ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    callback.onSuccess(response);
                }

                @Override
                public void onFailure(String error) {
                    // Force success locally even if connection state logs failed
                    JSONObject backup = new JSONObject();
                    try {
                        backup.put("success", true);
                    } catch (Exception ignored) {}
                    callback.onSuccess(backup);
                }
            });
        } catch (Exception e) {
            clearSession();
            callback.onFailure(e.getMessage());
        }
    }

    public void verify(final String logicName, final double a, final double b, final double result, final ApiCallback callback) {
        try {
            String token = getToken();
            if (token == null || token.isEmpty()) {
                callback.onFailure("User session expired. Please log in again.");
                return;
            }

            JSONObject values = new JSONObject();
            values.put("a", a);
            values.put("b", b);

            JSONObject body = new JSONObject();
            body.put("logic", logicName);
            body.put("values", values);
            body.put("result", result);

            performRequest("/verify", body, token, callback);
        } catch (Exception e) {
            callback.onFailure("Verification package setup failed: " + e.getMessage());
        }
    }

    private void performRequest(final String endpoint, final JSONObject body, final String token, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection conn = null;
                try {
                    URL url = new URL(apiBaseUrl + endpoint);
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
                    os.write(body.toString().getBytes("UTF-8"));
                    os.close();

                    int responseCode = conn.getResponseCode();
                    InputStream is;
                    if (responseCode >= 200 && responseCode < 300) {
                        is = conn.getInputStream();
                    } else {
                        is = conn.getErrorStream();
                    }

                    if (is == null) {
                        throw new Exception("Null connection stream (HTTP Code: " + responseCode + ")");
                    }

                    BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                    }
                    br.close();
                    is.close();

                    final String responseStr = sb.toString();
                    final JSONObject json = new JSONObject(responseStr);

                    new android.os.Handler(android.os.Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onSuccess(json);
                        }
                    });

                } catch (final Exception e) {
                    Log.e(TAG, "HTTP execution failure on endpoint: " + endpoint, e);
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onFailure(e.getMessage() != null ? e.getMessage() : "Network communication failure");
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