package com.morgbilling.app;

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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class BackendApi {

    public interface ApiCallback {
        void onSuccess(JSONObject jsonResponse);
        void onError(String errorMessage);
    }

    private static final String PREFS_NAME = "morg_billing_prefs";
    private static final String KEY_TOKEN = "auth_token";
    private static final String KEY_USER_EMAIL = "auth_email";

    private final Context context;
    private final Handler mainHandler;
    private String apiBaseUrl = "https://trumpledroid-api.cloudbeta28624.workers.dev";
    private String cachedAppId = null;

    public BackendApi(Context context) {
        this.context = context.getApplicationContext();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public synchronized String getAppId() {
        if (cachedAppId != null) {
            return cachedAppId;
        }
        try {
            InputStream is = context.getAssets().open("app-meta.json");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            JSONObject obj = new JSONObject(sb.toString());
            cachedAppId = obj.optString("app_id", "morg_billing_app");
        } catch (Exception e) {
            cachedAppId = "morg_billing_app";
        }
        return cachedAppId;
    }

    public void saveSession(String token, String email) {
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        sp.edit().putString(KEY_TOKEN, token).putString(KEY_USER_EMAIL, email).apply();
    }

    public void clearSession() {
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        sp.edit().remove(KEY_TOKEN).remove(KEY_USER_EMAIL).apply();
    }

    public String getToken() {
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return sp.getString(KEY_TOKEN, null);
    }

    public String getUserEmail() {
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return sp.getString(KEY_USER_EMAIL, "");
    }

    public boolean isLoggedIn() {
        String token = getToken();
        return token != null && !token.trim().isEmpty();
    }

    // Step 1: Register account - does not return token
    public void register(final String email, final String password, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject body = new JSONObject();
                    body.put("app_id", getAppId());
                    body.put("email", email);
                    body.put("password", password);

                    executeHttp("POST", "/register", body.toString(), null, callback);
                } catch (Exception e) {
                    dispatchError(callback, "Registration error: " + e.getMessage());
                }
            }
        }).start();
    }

    // Step 2: Login account - returns token
    public void login(final String email, final String password, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject body = new JSONObject();
                    body.put("app_id", getAppId());
                    body.put("email", email);
                    body.put("password", password);

                    executeHttp("POST", "/login", body.toString(), null, new ApiCallback() {
                        @Override
                        public void onSuccess(JSONObject jsonResponse) {
                            String token = jsonResponse.optString("token", "");
                            if (!token.isEmpty()) {
                                saveSession(token, email);
                            }
                            dispatchSuccess(callback, jsonResponse);
                        }

                        @Override
                        public void onError(String errorMessage) {
                            dispatchError(callback, errorMessage);
                        }
                    });
                } catch (Exception e) {
                    dispatchError(callback, "Login error: " + e.getMessage());
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
                    executeHttp("POST", "/logout", "{}", token, new ApiCallback() {
                        @Override
                        public void onSuccess(JSONObject jsonResponse) {
                            clearSession();
                            dispatchSuccess(callback, jsonResponse);
                        }

                        @Override
                        public void onError(String errorMessage) {
                            clearSession();
                            dispatchError(callback, errorMessage);
                        }
                    });
                } catch (Exception e) {
                    clearSession();
                    dispatchError(callback, e.getMessage());
                }
            }
        }).start();
    }

    // Password reset step 1: Request OTP
    public void requestOtp(final String email, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject body = new JSONObject();
                    body.put("app_id", getAppId());
                    body.put("email", email);
                    executeHttp("POST", "/request-otp", body.toString(), null, callback);
                } catch (Exception e) {
                    dispatchError(callback, e.getMessage());
                }
            }
        }).start();
    }

    // Password reset step 2: Reset with OTP
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
                    executeHttp("POST", "/reset-password", body.toString(), null, callback);
                } catch (Exception e) {
                    dispatchError(callback, e.getMessage());
                }
            }
        }).start();
    }

    // Generic Data CRUD
    public void createRecord(final String collection, final JSONObject dataObject, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject body = new JSONObject();
                    body.put("collection", collection);
                    body.put("data", dataObject);

                    executeHttp("POST", "/data", body.toString(), getToken(), callback);
                } catch (Exception e) {
                    dispatchError(callback, e.getMessage());
                }
            }
        }).start();
    }

    public void readRecords(final String collection, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String query = "?collection=" + URLEncoder.encode(collection, "UTF-8");
                    executeHttp("GET", "/data" + query, null, getToken(), callback);
                } catch (Exception e) {
                    dispatchError(callback, e.getMessage());
                }
            }
        }).start();
    }

    public void updateRecord(final String recordId, final JSONObject updatedData, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject body = new JSONObject();
                    body.put("id", recordId);
                    body.put("data", updatedData);

                    executeHttp("PUT", "/data", body.toString(), getToken(), callback);
                } catch (Exception e) {
                    dispatchError(callback, e.getMessage());
                }
            }
        }).start();
    }

    public void deleteRecord(final String recordId, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String query = "?id=" + URLEncoder.encode(recordId, "UTF-8");
                    executeHttp("DELETE", "/data" + query, null, getToken(), callback);
                } catch (Exception e) {
                    dispatchError(callback, e.getMessage());
                }
            }
        }).start();
    }

    private void executeHttp(String method, String endpoint, String jsonPayload, String bearerToken, final ApiCallback callback) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(apiBaseUrl + endpoint);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(method);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");

            if (bearerToken != null && !bearerToken.isEmpty()) {
                conn.setRequestProperty("Authorization", "Bearer " + bearerToken);
            }

            if (jsonPayload != null && ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method))) {
                conn.setDoOutput(true);
                OutputStream os = conn.getOutputStream();
                os.write(jsonPayload.getBytes(StandardCharsets.UTF_8));
                os.flush();
                os.close();
            }

            int responseCode = conn.getResponseCode();
            InputStream is = (responseCode >= 200 && responseCode < 400) ? conn.getInputStream() : conn.getErrorStream();

            StringBuilder responseSb = new StringBuilder();
            if (is != null) {
                BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                String line;
                while ((line = br.readLine()) != null) {
                    responseSb.append(line);
                }
                br.close();
            }

            String responseText = responseSb.toString();
            JSONObject jsonResponse = null;
            if (!responseText.trim().isEmpty()) {
                jsonResponse = new JSONObject(responseText);
            } else {
                jsonResponse = new JSONObject();
                jsonResponse.put("success", responseCode >= 200 && responseCode < 400);
            }

            if (responseCode >= 200 && responseCode < 400) {
                dispatchSuccess(callback, jsonResponse);
            } else {
                String errMsg = jsonResponse.optString("error", jsonResponse.optString("message", "HTTP " + responseCode));
                dispatchError(callback, errMsg);
            }

        } catch (final Exception e) {
            dispatchError(callback, e.getMessage() != null ? e.getMessage() : "Network communication error");
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private void dispatchSuccess(final ApiCallback callback, final JSONObject response) {
        if (callback == null) return;
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onSuccess(response);
            }
        });
    }

    private void dispatchError(final ApiCallback callback, final String message) {
        if (callback == null) return;
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onError(message);
            }
        });
    }
}