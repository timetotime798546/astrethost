package com.studyplanner.app;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
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
    
    private String appId;
    private final Context context;
    private final Handler mainHandler;

    public interface ApiCallback {
        void onSuccess(String response);
        void onError(String error);
    }

    public BackendApi(Context context) {
        this.context = context.getApplicationContext();
        this.mainHandler = new Handler(Looper.getMainLooper());
        loadAppId();
    }

    private void loadAppId() {
        try {
            InputStream is = context.getAssets().open("app-meta.json");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            is.close();
            JSONObject json = new JSONObject(sb.toString());
            this.appId = json.optString("app_id", "studyplanner");
        } catch (Exception e) {
            Log.e(TAG, "Failed to load app_id from app-meta.json", e);
            this.appId = "studyplanner"; // fallback
        }
    }

    public String getAppId() {
        return appId;
    }

    private void runOnBackground(final Runnable runnable) {
        new Thread(runnable).start();
    }

    private void sendSuccess(final ApiCallback callback, final String response) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onSuccess(response);
            }
        });
    }

    private void sendError(final ApiCallback callback, final String error) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onError(error);
            }
        });
    }

    public void register(final String email, final String password, final ApiCallback callback) {
        runOnBackground(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(BASE_URL + "/register");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);

                    JSONObject body = new JSONObject();
                    body.put("app_id", appId);
                    body.put("email", email);
                    body.put("password", password);

                    OutputStream os = conn.getOutputStream();
                    os.write(body.toString().getBytes("UTF-8"));
                    os.close();

                    int responseCode = conn.getResponseCode();
                    InputStream is = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream();
                    BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }
                    br.close();

                    if (responseCode == 200 || responseCode == 201) {
                        sendSuccess(callback, response.toString());
                    } else {
                        JSONObject errorJson = new JSONObject(response.toString());
                        sendError(callback, errorJson.optString("error", "Registration failed"));
                    }
                } catch (Exception e) {
                    sendError(callback, e.getMessage());
                }
            }
        });
    }

    public void login(final String email, final String password, final ApiCallback callback) {
        runOnBackground(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(BASE_URL + "/login");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);

                    JSONObject body = new JSONObject();
                    body.put("app_id", appId);
                    body.put("email", email);
                    body.put("password", password);

                    OutputStream os = conn.getOutputStream();
                    os.write(body.toString().getBytes("UTF-8"));
                    os.close();

                    int responseCode = conn.getResponseCode();
                    InputStream is = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream();
                    BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }
                    br.close();

                    if (responseCode == 200) {
                        sendSuccess(callback, response.toString());
                    } else {
                        JSONObject errorJson = new JSONObject(response.toString());
                        sendError(callback, errorJson.optString("error", "Login failed"));
                    }
                } catch (Exception e) {
                    sendError(callback, e.getMessage());
                }
            }
        });
    }

    public void requestOtp(final String email, final ApiCallback callback) {
        runOnBackground(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(BASE_URL + "/request-otp");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);

                    JSONObject body = new JSONObject();
                    body.put("app_id", appId);
                    body.put("email", email);

                    OutputStream os = conn.getOutputStream();
                    os.write(body.toString().getBytes("UTF-8"));
                    os.close();

                    int responseCode = conn.getResponseCode();
                    InputStream is = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream();
                    BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }
                    br.close();

                    if (responseCode == 200) {
                        sendSuccess(callback, response.toString());
                    } else {
                        JSONObject errorJson = new JSONObject(response.toString());
                        sendError(callback, errorJson.optString("error", "OTP Request failed"));
                    }
                } catch (Exception e) {
                    sendError(callback, e.getMessage());
                }
            }
        });
    }

    public void resetPassword(final String email, final String otp, final String newPassword, final ApiCallback callback) {
        runOnBackground(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(BASE_URL + "/reset-password");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);

                    JSONObject body = new JSONObject();
                    body.put("app_id", appId);
                    body.put("email", email);
                    body.put("otp", otp);
                    body.put("new_password", newPassword);

                    OutputStream os = conn.getOutputStream();
                    os.write(body.toString().getBytes("UTF-8"));
                    os.close();

                    int responseCode = conn.getResponseCode();
                    InputStream is = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream();
                    BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }
                    br.close();

                    if (responseCode == 200) {
                        sendSuccess(callback, response.toString());
                    } else {
                        JSONObject errorJson = new JSONObject(response.toString());
                        sendError(callback, errorJson.optString("error", "Reset password failed"));
                    }
                } catch (Exception e) {
                    sendError(callback, e.getMessage());
                }
            }
        });
    }

    public void createRecord(final String token, final String collection, final JSONObject data, final ApiCallback callback) {
        runOnBackground(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(BASE_URL + "/data");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setRequestProperty("Authorization", "Bearer " + token);
                    conn.setDoOutput(true);

                    JSONObject body = new JSONObject();
                    body.put("collection", collection);
                    body.put("data", data);

                    OutputStream os = conn.getOutputStream();
                    os.write(body.toString().getBytes("UTF-8"));
                    os.close();

                    int responseCode = conn.getResponseCode();
                    InputStream is = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream();
                    BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }
                    br.close();

                    if (responseCode == 200 || responseCode == 201) {
                        sendSuccess(callback, response.toString());
                    } else {
                        JSONObject errorJson = new JSONObject(response.toString());
                        sendError(callback, errorJson.optString("error", "Creation failed"));
                    }
                } catch (Exception e) {
                    sendError(callback, e.getMessage());
                }
            }
        });
    }

    public void readRecords(final String token, final String collection, final ApiCallback callback) {
        runOnBackground(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(BASE_URL + "/data?collection=" + collection);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("Authorization", "Bearer " + token);

                    int responseCode = conn.getResponseCode();
                    InputStream is = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream();
                    BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }
                    br.close();

                    if (responseCode == 200) {
                        sendSuccess(callback, response.toString());
                    } else {
                        JSONObject errorJson = new JSONObject(response.toString());
                        sendError(callback, errorJson.optString("error", "Fetch failed"));
                    }
                } catch (Exception e) {
                    sendError(callback, e.getMessage());
                }
            }
        });
    }

    public void updateRecord(final String token, final String id, final JSONObject data, final ApiCallback callback) {
        runOnBackground(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(BASE_URL + "/data");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("PUT");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setRequestProperty("Authorization", "Bearer " + token);
                    conn.setDoOutput(true);

                    JSONObject body = new JSONObject();
                    body.put("id", id);
                    body.put("data", data);

                    OutputStream os = conn.getOutputStream();
                    os.write(body.toString().getBytes("UTF-8"));
                    os.close();

                    int responseCode = conn.getResponseCode();
                    InputStream is = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream();
                    BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }
                    br.close();

                    if (responseCode == 200) {
                        sendSuccess(callback, response.toString());
                    } else {
                        JSONObject errorJson = new JSONObject(response.toString());
                        sendError(callback, errorJson.optString("error", "Update failed"));
                    }
                } catch (Exception e) {
                    sendError(callback, e.getMessage());
                }
            }
        });
    }

    public void deleteRecord(final String token, final String id, final ApiCallback callback) {
        runOnBackground(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(BASE_URL + "/data?id=" + id);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("DELETE");
                    conn.setRequestProperty("Authorization", "Bearer " + token);

                    int responseCode = conn.getResponseCode();
                    InputStream is = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream();
                    BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }
                    br.close();

                    if (responseCode == 200) {
                        sendSuccess(callback, response.toString());
                    } else {
                        JSONObject errorJson = new JSONObject(response.toString());
                        sendError(callback, errorJson.optString("error", "Deletion failed"));
                    }
                } catch (Exception e) {
                    sendError(callback, e.getMessage());
                }
            }
        });
    }
}