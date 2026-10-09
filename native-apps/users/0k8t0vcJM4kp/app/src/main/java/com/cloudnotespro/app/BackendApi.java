package com.cloudnotespro.app;

import android.content.Context;
import android.util.Log;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.json.JSONArray;
import org.json.JSONObject;

public class BackendApi {
    private static final String TAG = "BackendApi";
    private static final String BASE_URL = "https://trumpledroid-api.cloudbeta28624.workers.dev";
    
    private String app_id = null;
    private final Context context;

    public BackendApi(Context context) {
        this.context = context.getApplicationContext();
        loadAppId();
    }

    private synchronized void loadAppId() {
        if (app_id != null) return;
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
            app_id = obj.optString("app_id", "");
            if (app_id.isEmpty()) {
                app_id = obj.optString("package_name", "com.cloudnotespro.app");
            }
            Log.d(TAG, "Loaded dynamic app_id: " + app_id);
        } catch (Exception e) {
            Log.e(TAG, "Error loading app-meta.json", e);
            app_id = "com.cloudnotespro.app"; // Default safety fallback
        }
    }

    public String getAppId() {
        if (app_id == null) {
            loadAppId();
        }
        return app_id;
    }

    public interface ApiCallback<T> {
        void onSuccess(T result);
        void onError(String errorMessage);
    }

    // AUTH - REGISTER
    public void register(final String email, final String password, final ApiCallback<Boolean> callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject body = new JSONObject();
                    body.put("app_id", getAppId());
                    body.put("email", email);
                    body.put("password", password);

                    String responseStr = makeHttpRequest(BASE_URL + "/register", "POST", body.toString(), null);
                    if (responseStr == null) {
                        callback.onError("Connection failed or server error");
                        return;
                    }

                    JSONObject response = new JSONObject(responseStr);
                    boolean success = response.optBoolean("success", false);
                    if (success) {
                        callback.onSuccess(true);
                    } else {
                        callback.onError(response.optString("error", response.optString("message", "Registration failed")));
                    }
                } catch (Exception e) {
                    callback.onError("Error: " + e.getMessage());
                }
            }
        }).start();
    }

    // AUTH - LOGIN
    public void login(final String email, final String password, final ApiCallback<String> callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject body = new JSONObject();
                    body.put("app_id", getAppId());
                    body.put("email", email);
                    body.put("password", password);

                    String responseStr = makeHttpRequest(BASE_URL + "/login", "POST", body.toString(), null);
                    if (responseStr == null) {
                        callback.onError("Connection failed or incorrect credentials");
                        return;
                    }

                    JSONObject response = new JSONObject(responseStr);
                    boolean success = response.optBoolean("success", false);
                    if (success) {
                        String token = response.optString("token", "");
                        if (!token.isEmpty()) {
                            callback.onSuccess(token);
                        } else {
                            callback.onError("Server response did not provide an authentication token.");
                        }
                    } else {
                        callback.onError(response.optString("error", response.optString("message", "Login failed")));
                    }
                } catch (Exception e) {
                    callback.onError("Error: " + e.getMessage());
                }
            }
        }).start();
    }

    // AUTH - LOGOUT
    public void logout(final String token, final ApiCallback<Boolean> callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String responseStr = makeHttpRequest(BASE_URL + "/logout", "POST", "{}", token);
                    if (responseStr == null) {
                        callback.onError("Logout call failed");
                        return;
                    }
                    JSONObject response = new JSONObject(responseStr);
                    boolean success = response.optBoolean("success", false);
                    callback.onSuccess(success);
                } catch (Exception e) {
                    callback.onError(e.getMessage());
                }
            }
        }).start();
    }

    // PASSWORD RESET - REQUEST OTP
    public void requestOtp(final String email, final ApiCallback<Boolean> callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject body = new JSONObject();
                    body.put("app_id", getAppId());
                    body.put("email", email);

                    String responseStr = makeHttpRequest(BASE_URL + "/request-otp", "POST", body.toString(), null);
                    if (responseStr == null) {
                        callback.onError("OTP request failed");
                        return;
                    }
                    JSONObject response = new JSONObject(responseStr);
                    boolean success = response.optBoolean("success", false);
                    if (success) {
                        callback.onSuccess(true);
                    } else {
                        callback.onError(response.optString("error", response.optString("message", "Failed to send OTP")));
                    }
                } catch (Exception e) {
                    callback.onError(e.getMessage());
                }
            }
        }).start();
    }

    // PASSWORD RESET - VERIFY & RESET
    public void resetPassword(final String email, final String otp, final String newPassword, final ApiCallback<Boolean> callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject body = new JSONObject();
                    body.put("app_id", getAppId());
                    body.put("email", email);
                    body.put("otp", otp);
                    body.put("new_password", newPassword);

                    String responseStr = makeHttpRequest(BASE_URL + "/reset-password", "POST", body.toString(), null);
                    if (responseStr == null) {
                        callback.onError("Password reset failed");
                        return;
                    }
                    JSONObject response = new JSONObject(responseStr);
                    boolean success = response.optBoolean("success", false);
                    if (success) {
                        callback.onSuccess(true);
                    } else {
                        callback.onError(response.optString("error", response.optString("message", "OTP verify / reset failed")));
                    }
                } catch (Exception e) {
                    callback.onError(e.getMessage());
                }
            }
        }).start();
    }

    // CRUD - CREATE
    public void createNote(final String token, final JSONObject noteData, final ApiCallback<JSONObject> callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject body = new JSONObject();
                    body.put("collection", "notes");
                    body.put("data", noteData);

                    String responseStr = makeHttpRequest(BASE_URL + "/data", "POST", body.toString(), token);
                    if (responseStr == null) {
                        callback.onError("Failed to save note to cloud");
                        return;
                    }
                    JSONObject response = new JSONObject(responseStr);
                    boolean success = response.optBoolean("success", false);
                    if (success && response.has("record")) {
                        callback.onSuccess(response.getJSONObject("record"));
                    } else {
                        callback.onError(response.optString("error", "Failed to parse saved note"));
                    }
                } catch (Exception e) {
                    callback.onError(e.getMessage());
                }
            }
        }).start();
    }

    // CRUD - READ
    public void getNotes(final String token, final ApiCallback<JSONArray> callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String url = BASE_URL + "/data?collection=notes";
                    String responseStr = makeHttpRequest(url, "GET", null, token);
                    if (responseStr == null) {
                        callback.onError("Failed to load notes from cloud");
                        return;
                    }
                    JSONObject response = new JSONObject(responseStr);
                    boolean success = response.optBoolean("success", false);
                    if (success && response.has("records")) {
                        callback.onSuccess(response.getJSONArray("records"));
                    } else {
                        callback.onError(response.optString("error", "No notes found or access denied"));
                    }
                } catch (Exception e) {
                    callback.onError(e.getMessage());
                }
            }
        }).start();
    }

    // CRUD - UPDATE
    public void updateNote(final String token, final String recordId, final JSONObject noteData, final ApiCallback<Boolean> callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject body = new JSONObject();
                    body.put("id", recordId);
                    body.put("data", noteData);

                    String responseStr = makeHttpRequest(BASE_URL + "/data", "PUT", body.toString(), token);
                    if (responseStr == null) {
                        callback.onError("Failed to update note");
                        return;
                    }
                    JSONObject response = new JSONObject(responseStr);
                    boolean success = response.optBoolean("success", false);
                    callback.onSuccess(success);
                } catch (Exception e) {
                    callback.onError(e.getMessage());
                }
            }
        }).start();
    }

    // CRUD - DELETE
    public void deleteNote(final String token, final String recordId, final ApiCallback<Boolean> callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String url = BASE_URL + "/data?id=" + recordId;
                    String responseStr = makeHttpRequest(url, "DELETE", null, token);
                    if (responseStr == null) {
                        callback.onError("Failed to delete note");
                        return;
                    }
                    JSONObject response = new JSONObject(responseStr);
                    boolean success = response.optBoolean("success", false);
                    callback.onSuccess(success);
                } catch (Exception e) {
                    callback.onError(e.getMessage());
                }
            }
        }).start();
    }

    // Raw HTTP helper
    private String makeHttpRequest(String urlString, String method, String payload, String token) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(method);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            conn.setDoInput(true);

            if (token != null) {
                conn.setRequestProperty("Authorization", "Bearer " + token);
            }

            if (payload != null && (method.equals("POST") || method.equals("PUT"))) {
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json");
                byte[] outputBytes = payload.getBytes(StandardCharsets.UTF_8);
                OutputStream os = conn.getOutputStream();
                os.write(outputBytes);
                os.flush();
                os.close();
            }

            int status = conn.getResponseCode();
            InputStream is;
            if (status >= 200 && status < 300) {
                is = conn.getInputStream();
            } else {
                is = conn.getErrorStream();
            }

            if (is == null) return null;

            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            return sb.toString();
        } catch (Exception e) {
            Log.e(TAG, "HTTP Request Error (" + method + ") " + urlString, e);
            return null;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}