package com.expensetracker.app;

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
    private static final String PREF_NAME = "expense_tracker_prefs";
    private static final String KEY_TOKEN = "auth_token";
    private static final String KEY_EMAIL = "auth_email";
    private static final String BASE_URL = "https://trumpledroid-api.cloudbeta28624.workers.dev";

    private final Context context;
    private final Handler mainHandler;
    private final SharedPreferences prefs;
    private String appIdCached = null;

    public interface ApiCallback {
        void onSuccess(JSONObject response);
        void onError(String errorMessage);
    }

    public BackendApi(Context context) {
        this.context = context.getApplicationContext();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.prefs = this.context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public String getAppId() {
        if (appIdCached != null) {
            return appIdCached;
        }
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
            appIdCached = obj.optString("app_id", "expensetracker");
        } catch (Exception e) {
            appIdCached = "expensetracker";
        }
        return appIdCached;
    }

    public void saveAuthToken(String token, String email) {
        prefs.edit().putString(KEY_TOKEN, token).putString(KEY_EMAIL, email).apply();
    }

    public String getAuthToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    public String getUserEmail() {
        return prefs.getString(KEY_EMAIL, "");
    }

    public boolean isLoggedIn() {
        String token = getAuthToken();
        return token != null && !token.trim().isEmpty();
    }

    public void clearAuth() {
        prefs.edit().remove(KEY_TOKEN).remove(KEY_EMAIL).apply();
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
                    makeRequest("/register", "POST", body.toString(), null, callback);
                } catch (Exception e) {
                    dispatchError(callback, e.getMessage());
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
                    makeRequest("/login", "POST", body.toString(), null, new ApiCallback() {
                        @Override
                        public void onSuccess(JSONObject response) {
                            String token = response.optString("token", "");
                            if (!token.isEmpty()) {
                                saveAuthToken(token, email);
                            }
                            dispatchSuccess(callback, response);
                        }

                        @Override
                        public void onError(String errorMessage) {
                            dispatchError(callback, errorMessage);
                        }
                    });
                } catch (Exception e) {
                    dispatchError(callback, e.getMessage());
                }
            }
        }).start();
    }

    public void logout(final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    makeRequest("/logout", "POST", null, getAuthToken(), new ApiCallback() {
                        @Override
                        public void onSuccess(JSONObject response) {
                            clearAuth();
                            dispatchSuccess(callback, response);
                        }

                        @Override
                        public void onError(String errorMessage) {
                            clearAuth();
                            dispatchSuccess(callback, new JSONObject());
                        }
                    });
                } catch (Exception e) {
                    clearAuth();
                    dispatchSuccess(callback, new JSONObject());
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
                    makeRequest("/request-otp", "POST", body.toString(), null, callback);
                } catch (Exception e) {
                    dispatchError(callback, e.getMessage());
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
                    makeRequest("/reset-password", "POST", body.toString(), null, callback);
                } catch (Exception e) {
                    dispatchError(callback, e.getMessage());
                }
            }
        }).start();
    }

    public void createTransaction(final TransactionModel model, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject body = new JSONObject();
                    body.put("collection", "transactions");
                    body.put("data", model.toJsonObject());
                    makeRequest("/data", "POST", body.toString(), getAuthToken(), callback);
                } catch (Exception e) {
                    dispatchError(callback, e.getMessage());
                }
            }
        }).start();
    }

    public void getTransactions(final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    makeRequest("/data?collection=transactions", "GET", null, getAuthToken(), callback);
                } catch (Exception e) {
                    dispatchError(callback, e.getMessage());
                }
            }
        }).start();
    }

    public void deleteTransaction(final String recordId, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    makeRequest("/data?id=" + recordId, "DELETE", null, getAuthToken(), callback);
                } catch (Exception e) {
                    dispatchError(callback, e.getMessage());
                }
            }
        }).start();
    }

    private void makeRequest(String endpoint, String method, String postData, String bearerToken, ApiCallback callback) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(BASE_URL + endpoint);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(method);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");

            if (bearerToken != null && !bearerToken.isEmpty()) {
                conn.setRequestProperty("Authorization", "Bearer " + bearerToken);
            }

            if (postData != null && ("POST".equals(method) || "PUT".equals(method))) {
                conn.setDoOutput(true);
                OutputStream os = conn.getOutputStream();
                os.write(postData.getBytes("UTF-8"));
                os.flush();
                os.close();
            }

            int responseCode = conn.getResponseCode();
            InputStream is = (responseCode >= 200 && responseCode < 400) ? conn.getInputStream() : conn.getErrorStream();

            StringBuilder sb = new StringBuilder();
            if (is != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();
            }

            String responseString = sb.toString();
            JSONObject json = null;
            try {
                json = new JSONObject(responseString);
            } catch (Exception ex) {
                json = new JSONObject();
                json.put("raw", responseString);
            }

            boolean isSuccess = responseCode >= 200 && responseCode < 300 && json.optBoolean("success", true);

            if (isSuccess) {
                dispatchSuccess(callback, json);
            } else {
                String errMsg = json.optString("error", json.optString("message", "Request failed with HTTP " + responseCode));
                dispatchError(callback, errMsg);
            }

        } catch (Exception e) {
            dispatchError(callback, e.getMessage() != null ? e.getMessage() : "Network error");
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

    private void dispatchError(final ApiCallback callback, final String errorMessage) {
        if (callback == null) return;
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onError(errorMessage);
            }
        });
    }
}