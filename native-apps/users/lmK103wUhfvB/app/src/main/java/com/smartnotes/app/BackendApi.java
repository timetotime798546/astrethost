package com.smartnotes.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

public class BackendApi {

    private static final String API_BASE_URL = "https://trumpledroid-api.cloudbeta28624.workers.dev";
    private static final String PREFS_NAME = "SmartNotesPrefs";
    private static final String KEY_TOKEN = "auth_token";

    private final Context context;
    private final Handler mainHandler;
    private String app_id;

    public interface ApiCallback<T> {
        void onSuccess(T result);
        void onError(String error);
    }

    public BackendApi(Context context) {
        this.context = context.getApplicationContext();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.app_id = loadAppId();
    }

    private String loadAppId() {
        try {
            java.io.InputStream is = context.getAssets().open("app-meta.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String jsonStr = new String(buffer, "UTF-8");
            JSONObject json = new JSONObject(jsonStr);
            return json.getString("package_name");
        } catch (Exception e) {
            e.printStackTrace();
            return "com.smartnotes.app";
        }
    }

    public String getAppId() {
        return this.app_id;
    }

    public void setToken(String token) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_TOKEN, token).apply();
    }

    public String getToken() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_TOKEN, null);
    }

    public void clearToken() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_TOKEN).apply();
    }

    public boolean isLoggedIn() {
        return getToken() != null;
    }

    // Register - never expects or parses token from registration response
    public void register(final String email, final String password, final ApiCallback<String> callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(API_BASE_URL + "/register");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json; utf-8");
                    conn.setDoOutput(true);

                    JSONObject payload = new JSONObject();
                    payload.put("app_id", app_id);
                    payload.put("email", email);
                    payload.put("password", password);

                    OutputStream os = conn.getOutputStream();
                    os.write(payload.toString().getBytes("utf-8"));
                    os.flush();
                    os.close();

                    int responseCode = conn.getResponseCode();
                    BufferedReader in = new BufferedReader(new InputStreamReader(
                            responseCode >= 400 ? conn.getErrorStream() : conn.getInputStream()
                    ));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }
                    in.close();

                    final JSONObject jsonResponse = new JSONObject(response.toString());
                    if (responseCode == 200 || responseCode == 201) {
                        boolean success = jsonResponse.optBoolean("success", true);
                        if (success) {
                            sendSuccess(callback, "Registration successful");
                        } else {
                            sendError(callback, jsonResponse.optString("error", "Registration failed"));
                        }
                    } else {
                        sendError(callback, jsonResponse.optString("error", "Error " + responseCode));
                    }
                } catch (final Exception e) {
                    sendError(callback, e.getMessage());
                }
            }
        }).start();
    }

    // Login - extracts and stores the session token
    public void login(final String email, final String password, final ApiCallback<String> callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(API_BASE_URL + "/login");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json; utf-8");
                    conn.setDoOutput(true);

                    JSONObject payload = new JSONObject();
                    payload.put("app_id", app_id);
                    payload.put("email", email);
                    payload.put("password", password);

                    OutputStream os = conn.getOutputStream();
                    os.write(payload.toString().getBytes("utf-8"));
                    os.flush();
                    os.close();

                    int responseCode = conn.getResponseCode();
                    BufferedReader in = new BufferedReader(new InputStreamReader(
                            responseCode >= 400 ? conn.getErrorStream() : conn.getInputStream()
                    ));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }
                    in.close();

                    final JSONObject jsonResponse = new JSONObject(response.toString());
                    if (responseCode == 200) {
                        boolean success = jsonResponse.optBoolean("success", true);
                        if (success) {
                            String token = jsonResponse.getString("token");
                            setToken(token);
                            sendSuccess(callback, token);
                        } else {
                            sendError(callback, jsonResponse.optString("error", "Login failed"));
                        }
                    } else {
                        sendError(callback, jsonResponse.optString("error", "Invalid credentials"));
                    }
                } catch (final Exception e) {
                    sendError(callback, e.getMessage());
                }
            }
        }).start();
    }

    // Request OTP via Gmail
    public void requestOtp(final String email, final ApiCallback<String> callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(API_BASE_URL + "/request-otp");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json; utf-8");
                    conn.setDoOutput(true);

                    JSONObject payload = new JSONObject();
                    payload.put("app_id", app_id);
                    payload.put("email", email);

                    OutputStream os = conn.getOutputStream();
                    os.write(payload.toString().getBytes("utf-8"));
                    os.flush();
                    os.close();

                    int responseCode = conn.getResponseCode();
                    BufferedReader in = new BufferedReader(new InputStreamReader(
                            responseCode >= 400 ? conn.getErrorStream() : conn.getInputStream()
                    ));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }
                    in.close();

                    JSONObject jsonResponse = new JSONObject(response.toString());
                    if (responseCode == 200 && jsonResponse.optBoolean("success", true)) {
                        sendSuccess(callback, "OTP sent successfully to Gmail");
                    } else {
                        sendError(callback, jsonResponse.optString("error", "Could not send OTP"));
                    }
                } catch (final Exception e) {
                    sendError(callback, e.getMessage());
                }
            }
        }).start();
    }

    // Reset Password with OTP via Gmail
    public void resetPassword(final String email, final String otp, final String newPassword, final ApiCallback<String> callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(API_BASE_URL + "/reset-password");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json; utf-8");
                    conn.setDoOutput(true);

                    JSONObject payload = new JSONObject();
                    payload.put("app_id", app_id);
                    payload.put("email", email);
                    payload.put("otp", otp);
                    payload.put("new_password", newPassword);

                    OutputStream os = conn.getOutputStream();
                    os.write(payload.toString().getBytes("utf-8"));
                    os.flush();
                    os.close();

                    int responseCode = conn.getResponseCode();
                    BufferedReader in = new BufferedReader(new InputStreamReader(
                            responseCode >= 400 ? conn.getErrorStream() : conn.getInputStream()
                    ));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }
                    in.close();

                    JSONObject jsonResponse = new JSONObject(response.toString());
                    if (responseCode == 200 && jsonResponse.optBoolean("success", true)) {
                        sendSuccess(callback, "Password updated successfully");
                    } else {
                        sendError(callback, jsonResponse.optString("error", "Reset failed. Verify OTP."));
                    }
                } catch (final Exception e) {
                    sendError(callback, e.getMessage());
                }
            }
        }).start();
    }

    // Logout
    public void logout(final ApiCallback<String> callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(API_BASE_URL + "/logout");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Authorization", "Bearer " + getToken());

                    int responseCode = conn.getResponseCode();
                    clearToken();
                    sendSuccess(callback, "Logged out");
                } catch (final Exception e) {
                    clearToken();
                    sendError(callback, e.getMessage());
                }
            }
        }).start();
    }

    // Create Note (POST /data)
    public void createNote(final String title, final String content, final String category, final ApiCallback<Note> callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(API_BASE_URL + "/data");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json; utf-8");
                    conn.setRequestProperty("Authorization", "Bearer " + getToken());
                    conn.setDoOutput(true);

                    JSONObject dataObj = new JSONObject();
                    dataObj.put("title", title);
                    dataObj.put("content", content);
                    dataObj.put("category", category);
                    dataObj.put("updated_at", String.valueOf(System.currentTimeMillis()));

                    JSONObject payload = new JSONObject();
                    payload.put("collection", "notes");
                    payload.put("data", dataObj);

                    OutputStream os = conn.getOutputStream();
                    os.write(payload.toString().getBytes("utf-8"));
                    os.flush();
                    os.close();

                    int responseCode = conn.getResponseCode();
                    BufferedReader in = new BufferedReader(new InputStreamReader(
                            responseCode >= 400 ? conn.getErrorStream() : conn.getInputStream()
                    ));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }
                    in.close();

                    JSONObject jsonResponse = new JSONObject(response.toString());
                    if (responseCode == 200 || responseCode == 201) {
                        boolean success = jsonResponse.optBoolean("success", true);
                        if (success) {
                            JSONObject record = jsonResponse.getJSONObject("record");
                            String id = record.getString("id");
                            JSONObject rData = record.getJSONObject("data");
                            Note note = new Note(
                                    id,
                                    rData.optString("title"),
                                    rData.optString("content"),
                                    rData.optString("category"),
                                    rData.optString("updated_at")
                            );
                            sendSuccess(callback, note);
                        } else {
                            sendError(callback, "Could not save notes dynamic entry");
                        }
                    } else {
                        sendError(callback, "Error " + responseCode);
                    }
                } catch (final Exception e) {
                    sendError(callback, e.getMessage());
                }
            }
        }).start();
    }

    // Read Notes (GET /data?collection=notes)
    public void readNotes(final ApiCallback<List<Note>> callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(API_BASE_URL + "/data?collection=notes");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("Authorization", "Bearer " + getToken());

                    int responseCode = conn.getResponseCode();
                    BufferedReader in = new BufferedReader(new InputStreamReader(
                            responseCode >= 400 ? conn.getErrorStream() : conn.getInputStream()
                    ));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }
                    in.close();

                    JSONObject jsonResponse = new JSONObject(response.toString());
                    if (responseCode == 200) {
                        boolean success = jsonResponse.optBoolean("success", true);
                        if (success) {
                            JSONArray records = jsonResponse.getJSONArray("records");
                            List<Note> list = new ArrayList<>();
                            for (int i = 0; i < records.length(); i++) {
                                JSONObject record = records.getJSONObject(i);
                                String id = record.getString("id");
                                JSONObject rData = record.getJSONObject("data");
                                list.add(new Note(
                                        id,
                                        rData.optString("title"),
                                        rData.optString("content"),
                                        rData.optString("category"),
                                        rData.optString("updated_at")
                                ));
                            }
                            sendSuccess(callback, list);
                        } else {
                            sendError(callback, "Read failed");
                        }
                    } else {
                        sendError(callback, "Response error " + responseCode);
                    }
                } catch (final Exception e) {
                    sendError(callback, e.getMessage());
                }
            }
        }).start();
    }

    // Update Note (PUT /data)
    public void updateNote(final String id, final String title, final String content, final String category, final ApiCallback<String> callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(API_BASE_URL + "/data");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("PUT");
                    conn.setRequestProperty("Content-Type", "application/json; utf-8");
                    conn.setRequestProperty("Authorization", "Bearer " + getToken());
                    conn.setDoOutput(true);

                    JSONObject dataObj = new JSONObject();
                    dataObj.put("title", title);
                    dataObj.put("content", content);
                    dataObj.put("category", category);
                    dataObj.put("updated_at", String.valueOf(System.currentTimeMillis()));

                    JSONObject payload = new JSONObject();
                    payload.put("id", id);
                    payload.put("data", dataObj);

                    OutputStream os = conn.getOutputStream();
                    os.write(payload.toString().getBytes("utf-8"));
                    os.flush();
                    os.close();

                    int responseCode = conn.getResponseCode();
                    BufferedReader in = new BufferedReader(new InputStreamReader(
                            responseCode >= 400 ? conn.getErrorStream() : conn.getInputStream()
                    ));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }
                    in.close();

                    JSONObject jsonResponse = new JSONObject(response.toString());
                    if (responseCode == 200 && jsonResponse.optBoolean("success", true)) {
                        sendSuccess(callback, jsonResponse.getString("id"));
                    } else {
                        sendError(callback, "Update error details from platform");
                    }
                } catch (final Exception e) {
                    sendError(callback, e.getMessage());
                }
            }
        }).start();
    }

    // Delete Note (DELETE /data?id=<id>)
    public void deleteNote(final String id, final ApiCallback<String> callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(API_BASE_URL + "/data?id=" + id);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("DELETE");
                    conn.setRequestProperty("Authorization", "Bearer " + getToken());

                    int responseCode = conn.getResponseCode();
                    BufferedReader in = new BufferedReader(new InputStreamReader(
                            responseCode >= 400 ? conn.getErrorStream() : conn.getInputStream()
                    ));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }
                    in.close();

                    JSONObject jsonResponse = new JSONObject(response.toString());
                    if (responseCode == 200 && jsonResponse.optBoolean("success", true)) {
                        sendSuccess(callback, jsonResponse.getString("id"));
                    } else {
                        sendError(callback, "Delete request failed");
                    }
                } catch (final Exception e) {
                    sendError(callback, e.getMessage());
                }
            }
        }).start();
    }

    private <T> void sendSuccess(final ApiCallback<T> callback, final T result) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (callback != null) {
                    callback.onSuccess(result);
                }
            }
        });
    }

    private <T> void sendError(final ApiCallback<T> callback, final String error) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (callback != null) {
                    callback.onError(error);
                }
            }
        });
    }
}