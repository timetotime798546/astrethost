package com.shozicapremor.app;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class BackendApi {

    private static final String PREF_NAME = "ShozicaPrefs";
    private static final String KEY_TOKEN = "auth_token";
    private static final String KEY_EMAIL = "auth_email";

    private final Context context;
    private final String baseUrl = "https://trumpledroid-api.cloudbeta28624.workers.dev";
    private String appId = "shozicapremor"; // Default fallback

    public interface ApiCallback<T> {
        void onSuccess(T result);
        void onError(String error);
    }

    public BackendApi(Context context) {
        this.context = context;
        loadAppIdFromMeta();
    }

    private void loadAppIdFromMeta() {
        try {
            InputStream is = context.getAssets().open("app-meta.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String jsonStr = new String(buffer, StandardCharsets.UTF_8);
            JSONObject obj = new JSONObject(jsonStr);
            this.appId = obj.getString("app_id");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getSavedEmail() {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_EMAIL, "customer@shozica.com");
    }

    public void saveAuth(String email, String token) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_EMAIL, email).putString(KEY_TOKEN, token).apply();
    }

    public void clearAuth() {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
    }

    public boolean isUserLoggedIn() {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.contains(KEY_TOKEN);
    }

    private String getSavedToken() {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_TOKEN, "");
    }

    public void registerUser(final String email, final String password, final ApiCallback<JSONObject> callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(baseUrl + "/register");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);

                    JSONObject payload = new JSONObject();
                    payload.put("app_id", appId);
                    payload.put("email", email);
                    payload.put("password", password);

                    OutputStream os = conn.getOutputStream();
                    os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
                    os.flush();
                    os.close();

                    int responseCode = conn.getResponseCode();
                    if (responseCode == 200 || responseCode == 201) {
                        InputStream is = conn.getInputStream();
                        byte[] buffer = new byte[8192];
                        int read;
                        StringBuilder sb = new StringBuilder();
                        while ((read = is.read(buffer)) != -1) {
                            sb.append(new String(buffer, 0, read, StandardCharsets.UTF_8));
                        }
                        is.close();
                        callback.onSuccess(new JSONObject(sb.toString()));
                    } else {
                        callback.onError("Registration failure. HTTP Code: " + responseCode);
                    }
                } catch (Exception e) {
                    callback.onError(e.getMessage());
                }
            }
        }).start();
    }

    public void loginUser(final String email, final String password, final ApiCallback<String> callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(baseUrl + "/login");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);

                    JSONObject payload = new JSONObject();
                    payload.put("app_id", appId);
                    payload.put("email", email);
                    payload.put("password", password);

                    OutputStream os = conn.getOutputStream();
                    os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
                    os.flush();
                    os.close();

                    int responseCode = conn.getResponseCode();
                    if (responseCode == 200) {
                        InputStream is = conn.getInputStream();
                        byte[] buffer = new byte[8192];
                        int read;
                        StringBuilder sb = new StringBuilder();
                        while ((read = is.read(buffer)) != -1) {
                            sb.append(new String(buffer, 0, read, StandardCharsets.UTF_8));
                        }
                        is.close();
                        JSONObject response = new JSONObject(sb.toString());
                        if (response.optBoolean("success", false) || response.has("token")) {
                            String token = response.getString("token");
                            saveAuth(email, token);
                            callback.onSuccess(token);
                        } else {
                            callback.onError("Incorrect parameters or mismatch.");
                        }
                    } else {
                        callback.onError("Login invalid. Credentials error.");
                    }
                } catch (Exception e) {
                    callback.onError(e.getMessage());
                }
            }
        }).start();
    }

    public void requestOtp(final String email, final ApiCallback<String> callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(baseUrl + "/request-otp");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);

                    JSONObject payload = new JSONObject();
                    payload.put("app_id", appId);
                    payload.put("email", email);

                    OutputStream os = conn.getOutputStream();
                    os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
                    os.flush();
                    os.close();

                    int responseCode = conn.getResponseCode();
                    if (responseCode == 200 || responseCode == 201) {
                        callback.onSuccess("OTP verification code successfully dispatched.");
                    } else {
                        callback.onError("No active account for specified email.");
                    }
                } catch (Exception e) {
                    callback.onError(e.getMessage());
                }
            }
        }).start();
    }

    public void resetPassword(final String email, final String otp, final String newPassword, final ApiCallback<String> callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(baseUrl + "/reset-password");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);

                    JSONObject payload = new JSONObject();
                    payload.put("app_id", appId);
                    payload.put("email", email);
                    payload.put("otp", otp);
                    payload.put("new_password", newPassword);

                    OutputStream os = conn.getOutputStream();
                    os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
                    os.flush();
                    os.close();

                    int responseCode = conn.getResponseCode();
                    if (responseCode == 200 || responseCode == 201) {
                        callback.onSuccess("Password updated successfully.");
                    } else {
                        callback.onError("Wrong OTP token or link error.");
                    }
                } catch (Exception e) {
                    callback.onError(e.getMessage());
                }
            }
        }).start();
    }

    public void saveOrder(final JSONObject orderDetails, final ApiCallback<JSONObject> callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(baseUrl + "/data");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setRequestProperty("Authorization", "Bearer " + getSavedToken());
                    conn.setDoOutput(true);

                    JSONObject payload = new JSONObject();
                    payload.put("collection", "orders");
                    payload.put("data", orderDetails);

                    OutputStream os = conn.getOutputStream();
                    os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
                    os.flush();
                    os.close();

                    int responseCode = conn.getResponseCode();
                    if (responseCode == 200 || responseCode == 201) {
                        InputStream is = conn.getInputStream();
                        byte[] buffer = new byte[8192];
                        int read;
                        StringBuilder sb = new StringBuilder();
                        while ((read = is.read(buffer)) != -1) {
                            sb.append(new String(buffer, 0, read, StandardCharsets.UTF_8));
                        }
                        is.close();
                        callback.onSuccess(new JSONObject(sb.toString()));
                    } else {
                        callback.onError("Database synchronization failed.");
                    }
                } catch (Exception e) {
                    callback.onError(e.getMessage());
                }
            }
        }).start();
    }

    public void readOrders(final ApiCallback<JSONArray> callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(baseUrl + "/data?collection=orders");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("Authorization", "Bearer " + getSavedToken());

                    int responseCode = conn.getResponseCode();
                    if (responseCode == 200) {
                        InputStream is = conn.getInputStream();
                        byte[] buffer = new byte[8192];
                        int read;
                        StringBuilder sb = new StringBuilder();
                        while ((read = is.read(buffer)) != -1) {
                            sb.append(new String(buffer, 0, read, StandardCharsets.UTF_8));
                        }
                        is.close();
                        JSONObject resObj = new JSONObject(sb.toString());
                        if (resObj.optBoolean("success", false)) {
                            callback.onSuccess(resObj.getJSONArray("records"));
                        } else {
                            callback.onError("API reported unsuccess.");
                        }
                    } else {
                        callback.onError("Failed with status: " + responseCode);
                    }
                } catch (Exception e) {
                    callback.onError(e.getMessage());
                }
            }
        }).start();
    }
}