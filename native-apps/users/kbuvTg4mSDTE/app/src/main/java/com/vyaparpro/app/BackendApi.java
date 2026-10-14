package com.vyaparpro.app;

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

    public static final String BASE_URL = "https://trumpledroid-api.cloudbeta28624.workers.dev";
    private static final String PREF_NAME = "VyaparProPrefs";
    private static final String KEY_TOKEN = "auth_token";
    private static final String KEY_EMAIL = "auth_email";

    private final Context context;
    private final Handler mainHandler;
    private final SharedPreferences prefs;

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
            return obj.optString("app_id", "vyaparpro_biz_hub");
        } catch (Exception e) {
            return "vyaparpro_biz_hub";
        }
    }

    public void setToken(String token) {
        prefs.edit().putString(KEY_TOKEN, token).apply();
    }

    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    public void setUserEmail(String email) {
        prefs.edit().putString(KEY_EMAIL, email).apply();
    }

    public String getUserEmail() {
        return prefs.getString(KEY_EMAIL, "");
    }

    public boolean isLoggedIn() {
        String token = getToken();
        return token != null && !token.trim().isEmpty();
    }

    public void logout(final ApiCallback callback) {
        final String token = getToken();
        prefs.edit().clear().apply();
        if (token == null) {
            if (callback != null) {
                callback.onSuccess(new JSONObject());
            }
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(BASE_URL + "/logout");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Authorization", "Bearer " + token);
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(10000);
                    conn.getResponseCode();
                    conn.disconnect();
                } catch (Exception ignored) {
                }
                if (callback != null) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onSuccess(new JSONObject());
                        }
                    });
                }
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

                    executeHttp("POST", BASE_URL + "/register", null, body.toString(), new ApiCallback() {
                        @Override
                        public void onSuccess(final JSONObject response) {
                            // Automatically log in after registration to acquire the bearer token
                            login(email, password, callback);
                        }

                        @Override
                        public void onError(final String errorMessage) {
                            postError(callback, errorMessage);
                        }
                    });
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

                    executeHttp("POST", BASE_URL + "/login", null, body.toString(), new ApiCallback() {
                        @Override
                        public void onSuccess(final JSONObject response) {
                            String token = response.optString("token", "");
                            if (!token.isEmpty()) {
                                setToken(token);
                                setUserEmail(email);
                                postSuccess(callback, response);
                            } else {
                                postError(callback, "Authentication succeeded but token was not returned.");
                            }
                        }

                        @Override
                        public void onError(final String errorMessage) {
                            postError(callback, errorMessage);
                        }
                    });
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
                    executeHttp("POST", BASE_URL + "/request-otp", null, body.toString(), callback);
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
                    body.put("app_id", getAppId());
                    body.put("email", email);
                    body.put("otp", otp);
                    body.put("new_password", newPassword);
                    executeHttp("POST", BASE_URL + "/reset-password", null, body.toString(), callback);
                } catch (Exception e) {
                    postError(callback, e.getMessage());
                }
            }
        }).start();
    }

    public void createData(final String collection, final JSONObject data, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject payload = new JSONObject();
                    payload.put("collection", collection);
                    payload.put("data", data);
                    executeHttp("POST", BASE_URL + "/data", getToken(), payload.toString(), callback);
                } catch (Exception e) {
                    postError(callback, e.getMessage());
                }
            }
        }).start();
    }

    public void getData(final String collection, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String endpoint = BASE_URL + "/data?collection=" + collection;
                executeHttp("GET", endpoint, getToken(), null, callback);
            }
        }).start();
    }

    public void updateData(final String recordId, final JSONObject updatedData, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject payload = new JSONObject();
                    payload.put("id", recordId);
                    payload.put("data", updatedData);
                    executeHttp("PUT", BASE_URL + "/data", getToken(), payload.toString(), callback);
                } catch (Exception e) {
                    postError(callback, e.getMessage());
                }
            }
        }).start();
    }

    public void deleteData(final String recordId, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String endpoint = BASE_URL + "/data?id=" + recordId;
                executeHttp("DELETE", endpoint, getToken(), null, callback);
            }
        }).start();
    }

    private void executeHttp(String method, String urlStr, String bearerToken, String jsonBody, final ApiCallback callback) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(method);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            if (bearerToken != null && !bearerToken.isEmpty()) {
                conn.setRequestProperty("Authorization", "Bearer " + bearerToken);
            }
            conn.setConnectTimeout(12000);
            conn.setReadTimeout(12000);

            if (jsonBody != null && !jsonBody.isEmpty()) {
                conn.setDoOutput(true);
                OutputStream os = conn.getOutputStream();
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
                os.flush();
                os.close();
            }

            int responseCode = conn.getResponseCode();
            InputStream is = (responseCode >= 200 && responseCode < 400) ? conn.getInputStream() : conn.getErrorStream();

            StringBuilder sb = new StringBuilder();
            if (is != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();
            }

            final String respString = sb.toString();
            JSONObject jsonResult;
            try {
                jsonResult = new JSONObject(respString);
            } catch (Exception e) {
                jsonResult = new JSONObject();
                jsonResult.put("message", respString);
            }

            if (responseCode >= 200 && responseCode < 300) {
                postSuccess(callback, jsonResult);
            } else {
                String errMsg = jsonResult.optString("error", jsonResult.optString("message", "Request failed with code: " + responseCode));
                postError(callback, errMsg);
            }
        } catch (final Exception e) {
            postError(callback, e.getMessage() != null ? e.getMessage() : "Network connection error");
        }
    }

    private void postSuccess(final ApiCallback callback, final JSONObject response) {
        if (callback != null) {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    callback.onSuccess(response);
                }
            });
        }
    }

    private void postError(final ApiCallback callback, final String message) {
        if (callback != null) {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    callback.onError(message);
                }
            });
        }
    }
}