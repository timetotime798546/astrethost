package com.gympro.app;

import android.content.Context;
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
    private Context context;
    private AuthManager authManager;
    private Handler mainHandler;

    public interface ApiCallback {
        void onSuccess(JSONObject response);
        void onError(String errorMessage);
    }

    public BackendApi(Context context) {
        this.context = context.getApplicationContext();
        this.authManager = new AuthManager(context);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public String getAppId() {
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
            return obj.optString("app_id", "com.gympro.app");
        } catch (Exception e) {
            return "com.gympro.app";
        }
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

                    final JSONObject result = executePost("/register", body, false);
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (result.optBoolean("success", false)) {
                                callback.onSuccess(result);
                            } else {
                                callback.onError(result.optString("error", "Registration failed"));
                            }
                        }
                    });
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

                    final JSONObject result = executePost("/login", body, false);
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (result.optBoolean("success", false) && result.has("token")) {
                                String token = result.optString("token");
                                authManager.saveSession(token, email);
                                callback.onSuccess(result);
                            } else {
                                callback.onError(result.optString("error", "Invalid credentials"));
                            }
                        }
                    });
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

    public void requestOtp(final String email, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject body = new JSONObject();
                    body.put("app_id", getAppId());
                    body.put("email", email);

                    final JSONObject result = executePost("/request-otp", body, false);
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (result.optBoolean("success", false)) {
                                callback.onSuccess(result);
                            } else {
                                callback.onError(result.optString("error", "Failed to send OTP"));
                            }
                        }
                    });
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

                    final JSONObject result = executePost("/reset-password", body, false);
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (result.optBoolean("success", false)) {
                                callback.onSuccess(result);
                            } else {
                                callback.onError(result.optString("error", "Password reset failed"));
                            }
                        }
                    });
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
                    final JSONObject result = executePost("/logout", new JSONObject(), true);
                    authManager.clearSession();
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onSuccess(result);
                        }
                    });
                } catch (final Exception e) {
                    authManager.clearSession();
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

    public void getCollection(final String collection, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String endpoint = "/data?collection=" + collection;
                    final JSONObject result = executeGet(endpoint, true);
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (result.optBoolean("success", false)) {
                                callback.onSuccess(result);
                            } else {
                                callback.onError(result.optString("error", "Failed to load data"));
                            }
                        }
                    });
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

    public void createRecord(final String collection, final JSONObject data, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject body = new JSONObject();
                    body.put("collection", collection);
                    body.put("data", data);

                    final JSONObject result = executePost("/data", body, true);
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (result.optBoolean("success", false)) {
                                callback.onSuccess(result);
                            } else {
                                callback.onError(result.optString("error", "Failed to create record"));
                            }
                        }
                    });
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

    public void updateRecord(final String id, final JSONObject data, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject body = new JSONObject();
                    body.put("id", id);
                    body.put("data", data);

                    final JSONObject result = executePut("/data", body, true);
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (result.optBoolean("success", false)) {
                                callback.onSuccess(result);
                            } else {
                                callback.onError(result.optString("error", "Failed to update record"));
                            }
                        }
                    });
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

    public void deleteRecord(final String id, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String endpoint = "/data?id=" + id;
                    final JSONObject result = executeDelete(endpoint, true);
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (result.optBoolean("success", false)) {
                                callback.onSuccess(result);
                            } else {
                                callback.onError(result.optString("error", "Failed to delete record"));
                            }
                        }
                    });
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

    private JSONObject executePost(String path, JSONObject body, boolean useAuth) throws Exception {
        URL url = new URL(BASE_URL + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);

        if (useAuth && authManager.isLoggedIn()) {
            conn.setRequestProperty("Authorization", "Bearer " + authManager.getToken());
        }

        conn.setDoOutput(true);
        OutputStream os = conn.getOutputStream();
        os.write(body.toString().getBytes("UTF-8"));
        os.flush();
        os.close();

        return readResponse(conn);
    }

    private JSONObject executePut(String path, JSONObject body, boolean useAuth) throws Exception {
        URL url = new URL(BASE_URL + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("PUT");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);

        if (useAuth && authManager.isLoggedIn()) {
            conn.setRequestProperty("Authorization", "Bearer " + authManager.getToken());
        }

        conn.setDoOutput(true);
        OutputStream os = conn.getOutputStream();
        os.write(body.toString().getBytes("UTF-8"));
        os.flush();
        os.close();

        return readResponse(conn);
    }

    private JSONObject executeGet(String path, boolean useAuth) throws Exception {
        URL url = new URL(BASE_URL + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);

        if (useAuth && authManager.isLoggedIn()) {
            conn.setRequestProperty("Authorization", "Bearer " + authManager.getToken());
        }

        return readResponse(conn);
    }

    private JSONObject executeDelete(String path, boolean useAuth) throws Exception {
        URL url = new URL(BASE_URL + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("DELETE");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);

        if (useAuth && authManager.isLoggedIn()) {
            conn.setRequestProperty("Authorization", "Bearer " + authManager.getToken());
        }

        return readResponse(conn);
    }

    private JSONObject readResponse(HttpURLConnection conn) throws Exception {
        int status = conn.getResponseCode();
        InputStream is = (status >= 200 && status < 300) ? conn.getInputStream() : conn.getErrorStream();

        if (is == null) {
            JSONObject err = new JSONObject();
            err.put("success", false);
            err.put("error", "Server returned HTTP status " + status);
            return err;
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line.trim());
        }
        reader.close();

        if (response.length() == 0) {
            JSONObject err = new JSONObject();
            err.put("success", status >= 200 && status < 300);
            return err;
        }

        return new JSONObject(response.toString());
    }
}