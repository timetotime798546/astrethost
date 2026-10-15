package com.modernexpensetracker.app;

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

    public interface ApiCallback {
        void onSuccess(JSONObject response);
        void onError(String error);
    }

    private static final String BASE_URL = "https://trumpledroid-api.cloudbeta28624.workers.dev";
    private static final String PREF_NAME = "expense_tracker_prefs";
    private static final String KEY_TOKEN = "auth_token";
    private static final String KEY_USER_EMAIL = "user_email";

    private final Context context;
    private final Handler mainHandler;

    public BackendApi(Context context) {
        this.context = context.getApplicationContext();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public String getAppId() {
        try {
            InputStream is = context.getAssets().open("app-meta.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String jsonStr = new String(buffer, StandardCharsets.UTF_8);
            JSONObject obj = new JSONObject(jsonStr);
            return obj.optString("app_id", "modern-expense-tracker-v1");
        } catch (Exception e) {
            return "modern-expense-tracker-v1";
        }
    }

    public void saveToken(String token, String email) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sp.edit().putString(KEY_TOKEN, token).putString(KEY_USER_EMAIL, email).apply();
    }

    public String getToken() {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sp.getString(KEY_TOKEN, null);
    }

    public String getUserEmail() {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sp.getString(KEY_USER_EMAIL, "");
    }

    public void clearSession() {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sp.edit().clear().apply();
    }

    public boolean isLoggedIn() {
        return getToken() != null && !getToken().trim().isEmpty();
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

                    HttpResult res = makeHttpRequest("/register", "POST", body.toString(), null);
                    if (res.statusCode >= 200 && res.statusCode < 300) {
                        final JSONObject json = new JSONObject(res.responseBody);
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess(json);
                            }
                        });
                    } else {
                        final String errMsg = parseErrorMessage(res.responseBody, "Registration failed (" + res.statusCode + ")");
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onError(errMsg);
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

                    HttpResult res = makeHttpRequest("/login", "POST", body.toString(), null);
                    if (res.statusCode >= 200 && res.statusCode < 300) {
                        final JSONObject json = new JSONObject(res.responseBody);
                        String token = json.optString("token", "");
                        if (!token.isEmpty()) {
                            saveToken(token, email);
                        }
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess(json);
                            }
                        });
                    } else {
                        final String errMsg = parseErrorMessage(res.responseBody, "Invalid credentials (" + res.statusCode + ")");
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onError(errMsg);
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
                    if (token != null && !token.isEmpty()) {
                        makeHttpRequest("/logout", "POST", "{}", token);
                    }
                } catch (Exception ignored) {
                } finally {
                    clearSession();
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

    public void requestOtp(final String email, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject body = new JSONObject();
                    body.put("app_id", getAppId());
                    body.put("email", email);

                    HttpResult res = makeHttpRequest("/request-otp", "POST", body.toString(), null);
                    if (res.statusCode >= 200 && res.statusCode < 300) {
                        final JSONObject json = new JSONObject(res.responseBody);
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess(json);
                            }
                        });
                    } else {
                        final String errMsg = parseErrorMessage(res.responseBody, "Failed to request OTP (" + res.statusCode + ")");
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onError(errMsg);
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

                    HttpResult res = makeHttpRequest("/reset-password", "POST", body.toString(), null);
                    if (res.statusCode >= 200 && res.statusCode < 300) {
                        final JSONObject json = new JSONObject(res.responseBody);
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess(json);
                            }
                        });
                    } else {
                        final String errMsg = parseErrorMessage(res.responseBody, "Failed to reset password (" + res.statusCode + ")");
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onError(errMsg);
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
                }
            }
        }).start();
    }

    public void createData(final String collection, final JSONObject data, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject body = new JSONObject();
                    body.put("collection", collection);
                    body.put("data", data);

                    HttpResult res = makeHttpRequest("/data", "POST", body.toString(), getToken());
                    if (res.statusCode >= 200 && res.statusCode < 300) {
                        final JSONObject json = new JSONObject(res.responseBody);
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess(json);
                            }
                        });
                    } else {
                        final String errMsg = parseErrorMessage(res.responseBody, "Failed to save record (" + res.statusCode + ")");
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onError(errMsg);
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
                }
            }
        }).start();
    }

    public void getData(final String collection, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String path = "/data?collection=" + collection;
                    HttpResult res = makeHttpRequest(path, "GET", null, getToken());
                    if (res.statusCode >= 200 && res.statusCode < 300) {
                        final JSONObject json = new JSONObject(res.responseBody);
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess(json);
                            }
                        });
                    } else {
                        final String errMsg = parseErrorMessage(res.responseBody, "Failed to fetch data (" + res.statusCode + ")");
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onError(errMsg);
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
                }
            }
        }).start();
    }

    public void deleteData(final String recordId, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String path = "/data?id=" + recordId;
                    HttpResult res = makeHttpRequest(path, "DELETE", null, getToken());
                    if (res.statusCode >= 200 && res.statusCode < 300) {
                        final JSONObject json = new JSONObject(res.responseBody);
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess(json);
                            }
                        });
                    } else {
                        final String errMsg = parseErrorMessage(res.responseBody, "Failed to delete (" + res.statusCode + ")");
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onError(errMsg);
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
                }
            }
        }).start();
    }

    private static class HttpResult {
        int statusCode;
        String responseBody;

        HttpResult(int statusCode, String responseBody) {
            this.statusCode = statusCode;
            this.responseBody = responseBody;
        }
    }

    private HttpResult makeHttpRequest(String endpoint, String method, String jsonBody, String token) throws Exception {
        URL url = new URL(BASE_URL + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");

        if (token != null && !token.trim().isEmpty()) {
            conn.setRequestProperty("Authorization", "Bearer " + token);
        }

        if (jsonBody != null && (method.equals("POST") || method.equals("PUT"))) {
            conn.setDoOutput(true);
            byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(input, 0, input.length);
            }
        }

        int code = conn.getResponseCode();
        InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
        StringBuilder sb = new StringBuilder();
        if (is != null) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
        }
        conn.disconnect();
        return new HttpResult(code, sb.toString());
    }

    private String parseErrorMessage(String responseBody, String defaultMsg) {
        if (responseBody == null || responseBody.trim().isEmpty()) {
            return defaultMsg;
        }
        try {
            JSONObject obj = new JSONObject(responseBody);
            if (obj.has("error")) {
                return obj.getString("error");
            }
            if (obj.has("message")) {
                return obj.getString("message");
            }
        } catch (Exception ignored) {
        }
        return defaultMsg;
    }
}