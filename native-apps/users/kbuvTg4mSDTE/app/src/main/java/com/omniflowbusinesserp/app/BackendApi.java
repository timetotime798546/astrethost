package com.omniflowbusinesserp.app;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONObject;

public class BackendApi {

    private static final String BASE_URL = "https://trumpledroid-api.cloudbeta28624.workers.dev";
    private final String appId;
    private final Handler mainHandler;

    public interface ApiCallback {
        void onSuccess(String response);
        void onError(String errorMessage);
    }

    public BackendApi(Context context) {
        this.appId = loadAppId(context);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    private String loadAppId(Context context) {
        try {
            InputStream is = context.getAssets().open("app-meta.json");
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();
            JSONObject json = new JSONObject(new String(buffer, "UTF-8"));
            if (json.has("app_id")) {
                return json.getString("app_id");
            } else if (json.has("app_name")) {
                return json.getString("app_name").toLowerCase().replaceAll("[^a-z0-9]", "");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "omniflowbusinesserp";
    }

    public String getAppId() {
        return appId;
    }

    private void runOnMainThread(final ApiCallback callback, final boolean success, final String result) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (success) {
                    callback.onSuccess(result);
                } else {
                    callback.onError(result);
                }
            }
        });
    }

    public void register(final String email, final String password, final ApiCallback callback) {
        new Thread(new Runnable() {
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
                    os.flush();
                    os.close();

                    int responseCode = conn.getResponseCode();
                    InputStream is = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream();
                    BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                    }
                    br.close();

                    if (responseCode >= 200 && responseCode < 300) {
                        runOnMainThread(callback, true, sb.toString());
                    } else {
                        String errMsg = parseErrorMessage(sb.toString(), "Registration failed");
                        runOnMainThread(callback, false, errMsg);
                    }
                } catch (Exception e) {
                    runOnMainThread(callback, false, e.getMessage());
                }
            }
        }).start();
    }

    public void login(final String email, final String password, final ApiCallback callback) {
        new Thread(new Runnable() {
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
                    os.flush();
                    os.close();

                    int responseCode = conn.getResponseCode();
                    InputStream is = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream();
                    BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                    }
                    br.close();

                    if (responseCode >= 200 && responseCode < 300) {
                        runOnMainThread(callback, true, sb.toString());
                    } else {
                        String errMsg = parseErrorMessage(sb.toString(), "Login failed");
                        runOnMainThread(callback, false, errMsg);
                    }
                } catch (Exception e) {
                    runOnMainThread(callback, false, e.getMessage());
                }
            }
        }).start();
    }

    public void requestOtp(final String email, final ApiCallback callback) {
        new Thread(new Runnable() {
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
                    os.flush();
                    os.close();

                    int responseCode = conn.getResponseCode();
                    InputStream is = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream();
                    BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                    }
                    br.close();

                    if (responseCode >= 200 && responseCode < 300) {
                        runOnMainThread(callback, true, sb.toString());
                    } else {
                        runOnMainThread(callback, false, parseErrorMessage(sb.toString(), "OTP Request failed"));
                    }
                } catch (Exception e) {
                    runOnMainThread(callback, false, e.getMessage());
                }
            }
        }).start();
    }

    public void resetPassword(final String email, final String otp, final String newPassword, final ApiCallback callback) {
        new Thread(new Runnable() {
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
                    os.flush();
                    os.close();

                    int responseCode = conn.getResponseCode();
                    InputStream is = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream();
                    BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                    }
                    br.close();

                    if (responseCode >= 200 && responseCode < 300) {
                        runOnMainThread(callback, true, sb.toString());
                    } else {
                        runOnMainThread(callback, false, parseErrorMessage(sb.toString(), "Password reset failed"));
                    }
                } catch (Exception e) {
                    runOnMainThread(callback, false, e.getMessage());
                }
            }
        }).start();
    }

    public void createData(final String token, final String collection, final JSONObject dataObject, final ApiCallback callback) {
        new Thread(new Runnable() {
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
                    body.put("data", dataObject);

                    OutputStream os = conn.getOutputStream();
                    os.write(body.toString().getBytes("UTF-8"));
                    os.flush();
                    os.close();

                    int responseCode = conn.getResponseCode();
                    InputStream is = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream();
                    BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                    }
                    br.close();

                    if (responseCode >= 200 && responseCode < 300) {
                        runOnMainThread(callback, true, sb.toString());
                    } else {
                        runOnMainThread(callback, false, parseErrorMessage(sb.toString(), "Create record failed"));
                    }
                } catch (Exception e) {
                    runOnMainThread(callback, false, e.getMessage());
                }
            }
        }).start();
    }

    public void readData(final String token, final String collection, final ApiCallback callback) {
        new Thread(new Runnable() {
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
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                    }
                    br.close();

                    if (responseCode >= 200 && responseCode < 300) {
                        runOnMainThread(callback, true, sb.toString());
                    } else {
                        runOnMainThread(callback, false, parseErrorMessage(sb.toString(), "Read failed"));
                    }
                } catch (Exception e) {
                    runOnMainThread(callback, false, e.getMessage());
                }
            }
        }).start();
    }

    public void updateData(final String token, final String recordId, final JSONObject dataObject, final ApiCallback callback) {
        new Thread(new Runnable() {
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
                    body.put("id", recordId);
                    body.put("data", dataObject);

                    OutputStream os = conn.getOutputStream();
                    os.write(body.toString().getBytes("UTF-8"));
                    os.flush();
                    os.close();

                    int responseCode = conn.getResponseCode();
                    InputStream is = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream();
                    BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                    }
                    br.close();

                    if (responseCode >= 200 && responseCode < 300) {
                        runOnMainThread(callback, true, sb.toString());
                    } else {
                        runOnMainThread(callback, false, parseErrorMessage(sb.toString(), "Update record failed"));
                    }
                } catch (Exception e) {
                    runOnMainThread(callback, false, e.getMessage());
                }
            }
        }).start();
    }

    public void deleteData(final String token, final String recordId, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(BASE_URL + "/data?id=" + recordId);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("DELETE");
                    conn.setRequestProperty("Authorization", "Bearer " + token);

                    int responseCode = conn.getResponseCode();
                    InputStream is = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream();
                    BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                    }
                    br.close();

                    if (responseCode >= 200 && responseCode < 300) {
                        runOnMainThread(callback, true, sb.toString());
                    } else {
                        runOnMainThread(callback, false, parseErrorMessage(sb.toString(), "Delete record failed"));
                    }
                } catch (Exception e) {
                    runOnMainThread(callback, false, e.getMessage());
                }
            }
        }).start();
    }

    private String parseErrorMessage(String raw, String defaultMsg) {
        try {
            JSONObject obj = new JSONObject(raw);
            if (obj.has("error")) {
                return obj.getString("error");
            }
            if (obj.has("message")) {
                return obj.getString("message");
            }
        } catch (Exception e) {
            // Ignore parse exception
        }
        return defaultMsg;
    }
}