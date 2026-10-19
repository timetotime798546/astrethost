package com.payquick.app;

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
    private static final String PREF_NAME = "PayQuickPrefs";
    private static final String KEY_TOKEN = "auth_token";
    private static final String KEY_USER_EMAIL = "user_email";

    private static final String PREF_ADMIN_CACHE = "PayQuickAdminConfigCache";
    private static final String KEY_CACHED_CONFIG = "cached_config_json";
    private static final String KEY_CACHE_TIMESTAMP = "cached_config_timestamp";
    private static final long ONE_HOUR_MS = 3600 * 1000L; // 1-Hour local caching rule

    private static final String ADMIN_WORKER_BASE = "https://trumpledroid-apps-admin-47at498aq98hrg9633fugqd3.cloudbeta28624.workers.dev";
    private static final String DATA_API_BASE = "https://trumpledroid-api.cloudbeta28624.workers.dev";

    private final Context mContext;
    private final Handler mMainHandler;
    private final String mAppId;

    public interface ApiCallback {
        void onSuccess(JSONObject response);
        void onError(String errorMessage);
    }

    public interface DataCallback {
        void onSuccess(JSONArray records);
        void onError(String errorMessage);
    }

    public BackendApi(Context context) {
        this.mContext = context.getApplicationContext();
        this.mMainHandler = new Handler(Looper.getMainLooper());
        this.mAppId = readAppIdFromAssets();
    }

    private String readAppIdFromAssets() {
        try {
            InputStream is = mContext.getAssets().open("app-meta.json");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            JSONObject json = new JSONObject(sb.toString());
            return json.optString("app_id", "app_6ad5ae7ce8bcc");
        } catch (Exception e) {
            return "app_6ad5ae7ce8bcc";
        }
    }

    public String getAppId() {
        return mAppId;
    }

    public void saveToken(String token, String email) {
        SharedPreferences prefs = mContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_TOKEN, token).putString(KEY_USER_EMAIL, email).apply();
    }

    public String getToken() {
        SharedPreferences prefs = mContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_TOKEN, "");
    }

    public String getUserEmail() {
        SharedPreferences prefs = mContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_USER_EMAIL, "User");
    }

    public boolean isLoggedIn() {
        return !getToken().trim().isEmpty();
    }

    public void clearSession() {
        SharedPreferences prefs = mContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
    }

    /**
     * Enforces the 1-Hour Local Cache Rule for Dynamic App Configuration:
     * 1. Inspects SharedPreferences before executing any network request.
     * 2. If cached settings exist and were fetched within the last 3600 seconds, load immediately.
     * 3. Only if cache has expired or does not exist, fetch via GET /get-app-config?app_id=<APP_ID>.
     * 4. Updates cache with System.currentTimeMillis() on successful fetch.
     */
    public void fetchAppConfig(final boolean forceRefresh, final ApiCallback callback) {
        final SharedPreferences cachePrefs = mContext.getSharedPreferences(PREF_ADMIN_CACHE, Context.MODE_PRIVATE);
        final long lastTimestamp = cachePrefs.getLong(KEY_CACHE_TIMESTAMP, 0L);
        final String cachedData = cachePrefs.getString(KEY_CACHED_CONFIG, null);
        long now = System.currentTimeMillis();

        if (!forceRefresh && cachedData != null && (now - lastTimestamp < ONE_HOUR_MS)) {
            try {
                final JSONObject cachedObj = new JSONObject(cachedData);
                mMainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onSuccess(cachedObj);
                    }
                });
                return;
            } catch (Exception ignored) {
            }
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String urlStr = ADMIN_WORKER_BASE + "/get-app-config?app_id=" + mAppId;
                    URL url = new URL(urlStr);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(15000);
                    conn.setRequestProperty("Accept", "application/json");

                    int code = conn.getResponseCode();
                    InputStream is = (code >= 200 && code < 400) ? conn.getInputStream() : conn.getErrorStream();

                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    reader.close();
                    conn.disconnect();

                    final JSONObject json = new JSONObject(sb.toString());
                    cachePrefs.edit()
                            .putString(KEY_CACHED_CONFIG, json.toString())
                            .putLong(KEY_CACHE_TIMESTAMP, System.currentTimeMillis())
                            .apply();

                    mMainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onSuccess(json);
                        }
                    });
                } catch (final Exception e) {
                    if (cachedData != null) {
                        try {
                            final JSONObject cachedObj = new JSONObject(cachedData);
                            mMainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    callback.onSuccess(cachedObj);
                                }
                            });
                            return;
                        } catch (Exception ignored) {
                        }
                    }
                    postError(callback, e.getMessage());
                }
            }
        }).start();
    }

    public JSONObject getCachedConfigImmediate() {
        try {
            SharedPreferences cachePrefs = mContext.getSharedPreferences(PREF_ADMIN_CACHE, Context.MODE_PRIVATE);
            String cachedData = cachePrefs.getString(KEY_CACHED_CONFIG, null);
            if (cachedData != null) {
                return new JSONObject(cachedData);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    // POST /register
    public void register(final String email, final String password, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject body = new JSONObject();
                    body.put("app_id", mAppId);
                    body.put("email", email);
                    body.put("password", password);

                    NetworkResult res = executeRequest("POST", "/register", body.toString(), null);
                    handleJsonResponse(res, callback);
                } catch (final Exception e) {
                    postError(callback, e.getMessage());
                }
            }
        }).start();
    }

    // POST /login
    public void login(final String email, final String password, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject body = new JSONObject();
                    body.put("app_id", mAppId);
                    body.put("email", email);
                    body.put("password", password);

                    NetworkResult res = executeRequest("POST", "/login", body.toString(), null);
                    handleJsonResponse(res, callback);
                } catch (final Exception e) {
                    postError(callback, e.getMessage());
                }
            }
        }).start();
    }

    // POST /logout
    public void logout(final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    NetworkResult res = executeRequest("POST", "/logout", null, getToken());
                    clearSession();
                    handleJsonResponse(res, callback);
                } catch (final Exception e) {
                    clearSession();
                    postError(callback, e.getMessage());
                }
            }
        }).start();
    }

    // POST /request-otp
    public void requestOtp(final String email, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject body = new JSONObject();
                    body.put("app_id", mAppId);
                    body.put("email", email);

                    NetworkResult res = executeRequest("POST", "/request-otp", body.toString(), null);
                    handleJsonResponse(res, callback);
                } catch (final Exception e) {
                    postError(callback, e.getMessage());
                }
            }
        }).start();
    }

    // POST /reset-password
    public void resetPassword(final String email, final String otp, final String newPassword, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject body = new JSONObject();
                    body.put("app_id", mAppId);
                    body.put("email", email);
                    body.put("otp", otp);
                    body.put("new_password", newPassword);

                    NetworkResult res = executeRequest("POST", "/reset-password", body.toString(), null);
                    handleJsonResponse(res, callback);
                } catch (final Exception e) {
                    postError(callback, e.getMessage());
                }
            }
        }).start();
    }

    // POST /data
    public void createRecord(final String collection, final JSONObject data, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject body = new JSONObject();
                    body.put("collection", collection);
                    body.put("data", data);

                    NetworkResult res = executeRequest("POST", "/data", body.toString(), getToken());
                    handleJsonResponse(res, callback);
                } catch (final Exception e) {
                    postError(callback, e.getMessage());
                }
            }
        }).start();
    }

    // GET /data?collection=...
    public void getRecords(final String collection, final DataCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String endpoint = "/data?collection=" + collection;
                    NetworkResult res = executeRequest("GET", endpoint, null, getToken());
                    if (res.statusCode >= 200 && res.statusCode < 300) {
                        JSONObject json = new JSONObject(res.responseBody);
                        if (json.optBoolean("success", false)) {
                            final JSONArray records = json.optJSONArray("records");
                            mMainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    callback.onSuccess(records != null ? records : new JSONArray());
                                }
                            });
                            return;
                        }
                    }
                    String err = "Failed to load records";
                    try {
                        JSONObject json = new JSONObject(res.responseBody);
                        err = json.optString("error", json.optString("message", err));
                    } catch (Exception ignored) {
                    }
                    final String finalErr = err;
                    mMainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onError(finalErr);
                        }
                    });
                } catch (final Exception e) {
                    mMainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onError(e.getMessage() != null ? e.getMessage() : "Network error");
                        }
                    });
                }
            }
        }).start();
    }

    private void handleJsonResponse(NetworkResult res, final ApiCallback callback) {
        try {
            final JSONObject json = new JSONObject(res.responseBody != null ? res.responseBody : "{}");
            boolean success = json.optBoolean("success", res.statusCode >= 200 && res.statusCode < 300);
            if (success) {
                mMainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onSuccess(json);
                    }
                });
            } else {
                final String err = json.optString("error", json.optString("message", "Request failed: code " + res.statusCode));
                mMainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onError(err);
                    }
                });
            }
        } catch (final Exception e) {
            postError(callback, "Response parse error: " + e.getMessage());
        }
    }

    private void postError(final ApiCallback callback, final String message) {
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onError(message != null ? message : "Network request failed");
            }
        });
    }

    private static class NetworkResult {
        int statusCode;
        String responseBody;
    }

    private NetworkResult executeRequest(String method, String endpoint, String jsonPayload, String bearerToken) throws Exception {
        URL url = new URL(DATA_API_BASE + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(20000);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");

        if (bearerToken != null && !bearerToken.trim().isEmpty()) {
            conn.setRequestProperty("Authorization", "Bearer " + bearerToken.trim());
        }

        if (jsonPayload != null && ("POST".equals(method) || "PUT".equals(method))) {
            conn.setDoOutput(true);
            OutputStream os = conn.getOutputStream();
            os.write(jsonPayload.getBytes("UTF-8"));
            os.flush();
            os.close();
        }

        NetworkResult result = new NetworkResult();
        result.statusCode = conn.getResponseCode();

        InputStream is = (result.statusCode >= 200 && result.statusCode < 400)
                ? conn.getInputStream() : conn.getErrorStream();

        if (is != null) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            result.responseBody = sb.toString();
        } else {
            result.responseBody = "{}";
        }
        conn.disconnect();
        return result;
    }
}