package com.barbercraft.app;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BackendApi {
    private static final String BASE_URL = "https://trumpledroid-api.cloudbeta28624.workers.dev";
    private static final String PREF_NAME = "barbercraft_auth";
    private static final String KEY_TOKEN = "jwt_token";
    private static final String KEY_USER_EMAIL = "user_email";

    private final Context context;
    private final SharedPreferences prefs;
    private final ExecutorService executor;
    private final Handler mainHandler;
    private String appId;

    public interface ApiCallback {
        void onSuccess(JSONObject response);
        void onError(String errorMessage);
    }

    public BackendApi(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.appId = loadAppIdFromAssets();
    }

    private String loadAppIdFromAssets() {
        try {
            InputStream is = context.getAssets().open("app-meta.json");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            JSONObject json = new JSONObject(sb.toString());
            return json.optString("app_id", "barbercraft_app_id");
        } catch (Exception e) {
            return "barbercraft_app_id";
        }
    }

    public void setAuthToken(String token, String email) {
        prefs.edit().putString(KEY_TOKEN, token).putString(KEY_USER_EMAIL, email).apply();
    }

    public String getAuthToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    public String getUserEmail() {
        return prefs.getString(KEY_USER_EMAIL, "");
    }

    public boolean isLoggedIn() {
        return getAuthToken() != null && !getAuthToken().trim().isEmpty();
    }

    public void clearAuth() {
        prefs.edit().clear().apply();
    }

    public void register(final String email, final String password, final ApiCallback callback) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject body = new JSONObject();
                    body.put("app_id", appId);
                    body.put("email", email);
                    body.put("password", password);
                    executeRequest("POST", "/register", body.toString(), null, callback);
                } catch (Exception e) {
                    dispatchError(callback, e.getMessage());
                }
            }
        });
    }

    public void login(final String email, final String password, final ApiCallback callback) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject body = new JSONObject();
                    body.put("app_id", appId);
                    body.put("email", email);
                    body.put("password", password);
                    executeRequest("POST", "/login", body.toString(), null, new ApiCallback() {
                        @Override
                        public void onSuccess(JSONObject response) {
                            String token = response.optString("token", "");
                            if (!token.isEmpty()) {
                                setAuthToken(token, email);
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
        });
    }

    public void logout(final ApiCallback callback) {
        final String token = getAuthToken();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    executeRequest("POST", "/logout", null, token, new ApiCallback() {
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
                    dispatchError(callback, e.getMessage());
                }
            }
        });
    }

    public void requestOtp(final String email, final ApiCallback callback) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject body = new JSONObject();
                    body.put("app_id", appId);
                    body.put("email", email);
                    executeRequest("POST", "/request-otp", body.toString(), null, callback);
                } catch (Exception e) {
                    dispatchError(callback, e.getMessage());
                }
            }
        });
    }

    public void resetPassword(final String email, final String otp, final String newPassword, final ApiCallback callback) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject body = new JSONObject();
                    body.put("app_id", appId);
                    body.put("email", email);
                    body.put("otp", otp);
                    body.put("new_password", newPassword);
                    executeRequest("POST", "/reset-password", body.toString(), null, callback);
                } catch (Exception e) {
                    dispatchError(callback, e.getMessage());
                }
            }
        });
    }

    public void createData(final String collection, final JSONObject data, final ApiCallback callback) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject body = new JSONObject();
                    body.put("collection", collection);
                    body.put("data", data);
                    executeRequest("POST", "/data", body.toString(), getAuthToken(), callback);
                } catch (Exception e) {
                    dispatchError(callback, e.getMessage());
                }
            }
        });
    }

    public void readData(final String collection, final ApiCallback callback) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String endpoint = "/data?collection=" + collection;
                    executeRequest("GET", endpoint, null, getAuthToken(), callback);
                } catch (Exception e) {
                    dispatchError(callback, e.getMessage());
                }
            }
        });
    }

    public void deleteData(final String id, final ApiCallback callback) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String endpoint = "/data?id=" + id;
                    executeRequest("DELETE", endpoint, null, getAuthToken(), callback);
                } catch (Exception e) {
                    dispatchError(callback, e.getMessage());
                }
            }
        });
    }

    private void executeRequest(String method, String path, String jsonBody, String token, ApiCallback callback) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(BASE_URL + path);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(method);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("Content-Type", "application/json");

            if (token != null && !token.trim().isEmpty()) {
                conn.setRequestProperty("Authorization", "Bearer " + token);
            }

            if (jsonBody != null && !jsonBody.isEmpty() && (method.equals("POST") || method.equals("PUT"))) {
                conn.setDoOutput(true);
                OutputStream os = conn.getOutputStream();
                os.write(jsonBody.getBytes("UTF-8"));
                os.flush();
                os.close();
            }

            int responseCode = conn.getResponseCode();
            InputStream is = (responseCode >= 200 && responseCode < 400) ? conn.getInputStream() : conn.getErrorStream();

            StringBuilder sb = new StringBuilder();
            if (is != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();
            }

            String responseString = sb.toString();
            JSONObject jsonResp;
            try {
                jsonResp = new JSONObject(responseString);
            } catch (Exception e) {
                jsonResp = new JSONObject();
                jsonResp.put("message", responseString);
            }

            if (responseCode >= 200 && responseCode < 400) {
                dispatchSuccess(callback, jsonResp);
            } else {
                String errorMsg = jsonResp.optString("error", jsonResp.optString("message", "HTTP error " + responseCode));
                dispatchError(callback, errorMsg);
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
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (callback != null) {
                    callback.onSuccess(response);
                }
            }
        });
    }

    private void dispatchError(final ApiCallback callback, final String errorMessage) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (callback != null) {
                    callback.onError(errorMessage);
                }
            }
        });
    }
}