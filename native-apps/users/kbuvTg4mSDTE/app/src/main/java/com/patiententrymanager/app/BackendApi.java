package com.patiententrymanager.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class BackendApi {

    private static final String TAG = "BackendApi";
    private static final String BASE_URL = "https://trumpledroid-api.cloudbeta28624.workers.dev";
    private static final String PREFS_NAME = "PatientEntryManagerPrefs";
    private static final String KEY_TOKEN = "auth_token";
    private static final String KEY_USER_EMAIL = "user_email";

    private final Context context;
    private final Handler mainHandler;
    private String appId;

    public interface ApiCallback {
        void onSuccess(JSONObject response);
        void onError(String errorMessage);
    }

    public BackendApi(Context context) {
        this.context = context.getApplicationContext();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.appId = loadAppIdFromAssets();
    }

    private String loadAppIdFromAssets() {
        try {
            InputStream is = context.getAssets().open("app-meta.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String jsonStr = new String(buffer, "UTF-8");
            JSONObject obj = new JSONObject(jsonStr);
            return obj.optString("app_id", "patiententrymanager");
        } catch (Exception e) {
            Log.e(TAG, "Error reading assets/app-meta.json", e);
            return "patiententrymanager";
        }
    }

    public String getAppId() {
        return appId;
    }

    public void saveAuthToken(String token, String email) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_TOKEN, token).putString(KEY_USER_EMAIL, email).apply();
    }

    public String getAuthToken() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_TOKEN, null);
    }

    public String getUserEmail() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_USER_EMAIL, "");
    }

    public void clearAuthToken() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
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

                    JSONObject response = makeHttpRequest("/register", "POST", body, false);
                    postSuccess(callback, response);
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
                    body.put("app_id", appId);
                    body.put("email", email);
                    body.put("password", password);

                    JSONObject response = makeHttpRequest("/login", "POST", body, false);
                    String token = null;
                    if (response.has("token")) {
                        token = response.optString("token");
                    } else if (response.has("data") && response.getJSONObject("data").has("token")) {
                        token = response.getJSONObject("data").optString("token");
                    } else if (response.has("session") && response.getJSONObject("session").has("token")) {
                        token = response.getJSONObject("session").optString("token");
                    }

                    if (token != null && !token.isEmpty()) {
                        saveAuthToken(token, email);
                    }
                    postSuccess(callback, response);
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
                    JSONObject response = makeHttpRequest("/logout", "POST", null, true);
                    clearAuthToken();
                    postSuccess(callback, response);
                } catch (Exception e) {
                    clearAuthToken();
                    postSuccess(callback, new JSONObject());
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

                    JSONObject response = makeHttpRequest("/request-otp", "POST", body, false);
                    postSuccess(callback, response);
                } catch (Exception e) {
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
                    body.put("app_id", appId);
                    body.put("email", email);
                    body.put("otp", otp);
                    body.put("new_password", newPassword);

                    JSONObject response = makeHttpRequest("/reset-password", "POST", body, false);
                    postSuccess(callback, response);
                } catch (Exception e) {
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

                    JSONObject response = makeHttpRequest("/data", "POST", body, true);
                    postSuccess(callback, response);
                } catch (Exception e) {
                    postError(callback, e.getMessage());
                }
            }
        }).start();
    }

    public void getRecords(final String collection, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String path = "/data?collection=" + collection;
                    JSONObject response = makeHttpRequest(path, "GET", null, true);
                    postSuccess(callback, response);
                } catch (Exception e) {
                    postError(callback, e.getMessage());
                }
            }
        }).start();
    }

    public void updateRecord(final String recordId, final JSONObject data, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject body = new JSONObject();
                    body.put("id", recordId);
                    body.put("data", data);

                    JSONObject response = makeHttpRequest("/data", "PUT", body, true);
                    postSuccess(callback, response);
                } catch (Exception e) {
                    postError(callback, e.getMessage());
                }
            }
        }).start();
    }

    public void deleteRecord(final String recordId, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String path = "/data?id=" + recordId;
                    JSONObject response = makeHttpRequest(path, "DELETE", null, true);
                    postSuccess(callback, response);
                } catch (Exception e) {
                    postError(callback, e.getMessage());
                }
            }
        }).start();
    }

    private JSONObject makeHttpRequest(String endpoint, String method, JSONObject jsonBody, boolean useAuth) throws Exception {
        URL url = new URL(BASE_URL + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);
        conn.setRequestProperty("Content-Type", "application/json; utf-8");
        conn.setRequestProperty("Accept", "application/json");

        if (useAuth) {
            String token = getAuthToken();
            if (token != null && !token.isEmpty()) {
                conn.setRequestProperty("Authorization", "Bearer " + token);
            }
        }

        if (jsonBody != null && (method.equals("POST") || method.equals("PUT"))) {
            conn.setDoOutput(true);
            OutputStream os = conn.getOutputStream();
            byte[] input = jsonBody.toString().getBytes("utf-8");
            os.write(input, 0, input.length);
            os.close();
        }

        int code = conn.getResponseCode();
        InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();

        BufferedReader br = new BufferedReader(new InputStreamReader(is, "utf-8"));
        StringBuilder responseSb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            responseSb.append(line.trim());
        }
        br.close();

        String respStr = responseSb.toString();
        JSONObject jsonResponse;
        if (respStr.startsWith("{")) {
            jsonResponse = new JSONObject(respStr);
        } else if (respStr.startsWith("[")) {
            jsonResponse = new JSONObject();
            jsonResponse.put("success", true);
            jsonResponse.put("records", new JSONArray(respStr));
        } else {
            jsonResponse = new JSONObject();
            jsonResponse.put("success", code >= 200 && code < 300);
            jsonResponse.put("message", respStr);
        }

        if (code >= 400) {
            String err = jsonResponse.optString("error", jsonResponse.optString("message", "Request failed with HTTP " + code));
            throw new Exception(err);
        }

        return jsonResponse;
    }

    private void postSuccess(final ApiCallback callback, final JSONObject response) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (callback != null) {
                    callback.onSuccess(response);
                }
            }
        });
    }

    private void postError(final ApiCallback callback, final String message) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (callback != null) {
                    callback.onError(message != null ? message : "An unknown error occurred");
                }
            }
        });
    }
}