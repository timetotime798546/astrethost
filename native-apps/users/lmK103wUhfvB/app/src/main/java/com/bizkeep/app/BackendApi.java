package com.bizkeep.app;

import android.content.Context;
import android.content.SharedPreferences;
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
    private static final String PREFS_NAME = "BizKeepPrefs";
    private static final String KEY_TOKEN = "auth_token";

    private String appId = "bizkeep";
    private Context context;
    private SharedPreferences prefs;

    public interface ApiCallback<T> {
        void onSuccess(T result);
        void onError(String error);
    }

    public BackendApi(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.appId = loadAppId(context);
    }

    private String loadAppId(Context context) {
        try {
            InputStream is = context.getAssets().open("app-meta.json");
            byte[] buffer = new byte[is.available()];
            int count = is.read(buffer);
            is.close();
            if (count > 0) {
                JSONObject obj = new JSONObject(new String(buffer, "UTF-8"));
                String parsedId = obj.optString("app_id", "");
                if (parsedId.isEmpty()) {
                    parsedId = obj.optString("app_name", "bizkeep");
                }
                return parsedId.toLowerCase().replaceAll("[^a-z0-9]", "");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading app-meta.json assets", e);
        }
        return "bizkeep";
    }

    public void saveToken(String token) {
        prefs.edit().putString(KEY_TOKEN, token).apply();
    }

    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    public void clearToken() {
        prefs.edit().remove(KEY_TOKEN).apply();
    }

    public boolean isLoggedIn() {
        return getToken() != null;
    }

    // AUTH: Register Endpoint
    public void register(final String email, final String password, final ApiCallback<String> callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject body = new JSONObject();
                    body.put("app_id", appId);
                    body.put("email", email);
                    body.put("password", password);

                    String response = executePost(BASE_URL + "/register", body.toString(), null);
                    JSONObject respJson = new JSONObject(response);
                    boolean success = respJson.optBoolean("success", false);

                    if (success) {
                        callback.onSuccess("Registration Successful");
                    } else {
                        callback.onError(respJson.optString("error", "Registration failed"));
                    }
                } catch (Exception e) {
                    callback.onError(e.getMessage());
                }
            }
        }).start();
    }

    // AUTH: Login Endpoint
    public void login(final String email, final String password, final ApiCallback<String> callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject body = new JSONObject();
                    body.put("app_id", appId);
                    body.put("email", email);
                    body.put("password", password);

                    String response = executePost(BASE_URL + "/login", body.toString(), null);
                    JSONObject respJson = new JSONObject(response);
                    boolean success = respJson.optBoolean("success", false);

                    if (success) {
                        String token = respJson.optString("token", "");
                        if (!token.isEmpty()) {
                            saveToken(token);
                            callback.onSuccess(token);
                        } else {
                            callback.onError("Login succeeded but no token returned.");
                        }
                    } else {
                        callback.onError(respJson.optString("error", "Invalid credentials"));
                    }
                } catch (Exception e) {
                    callback.onError(e.getMessage());
                }
            }
        }).start();
    }

    // AUTH: Logout Endpoint
    public void logout(final ApiCallback<Boolean> callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String token = getToken();
                    if (token == null) {
                        clearToken();
                        callback.onSuccess(true);
                        return;
                    }

                    String response = executePost(BASE_URL + "/logout", "{}", token);
                    clearToken();
                    callback.onSuccess(true);
                } catch (Exception e) {
                    clearToken();
                    callback.onError(e.getMessage());
                }
            }
        }).start();
    }

    // OTP PASSWORD RESET: Step 1 Request
    public void requestOtp(final String email, final ApiCallback<String> callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject body = new JSONObject();
                    body.put("app_id", appId);
                    body.put("email", email);

                    String response = executePost(BASE_URL + "/request-otp", body.toString(), null);
                    JSONObject respJson = new JSONObject(response);
                    boolean success = respJson.optBoolean("success", false);

                    if (success) {
                        callback.onSuccess("OTP code successfully sent via email.");
                    } else {
                        callback.onError(respJson.optString("error", "Failed requesting OTP code"));
                    }
                } catch (Exception e) {
                    callback.onError(e.getMessage());
                }
            }
        }).start();
    }

    // OTP PASSWORD RESET: Step 2 Reset
    public void resetPassword(final String email, final String otp, final String newPassword, final ApiCallback<String> callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject body = new JSONObject();
                    body.put("app_id", appId);
                    body.put("email", email);
                    body.put("otp", otp);
                    body.put("new_password", newPassword);

                    String response = executePost(BASE_URL + "/reset-password", body.toString(), null);
                    JSONObject respJson = new JSONObject(response);
                    boolean success = respJson.optBoolean("success", false);

                    if (success) {
                        callback.onSuccess("Password successfully changed.");
                    } else {
                        callback.onError(respJson.optString("error", "Invalid OTP or request parameters."));
                    }
                } catch (Exception e) {
                    callback.onError(e.getMessage());
                }
            }
        }).start();
    }

    // CRUD: Create record
    public void createRecord(final String collection, final JSONObject dataObj, final ApiCallback<JSONObject> callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject body = new JSONObject();
                    body.put("collection", collection);
                    body.put("data", dataObj);

                    String response = executePost(BASE_URL + "/data", body.toString(), getToken());
                    JSONObject respJson = new JSONObject(response);
                    boolean success = respJson.optBoolean("success", false);

                    if (success) {
                        JSONObject record = respJson.getJSONObject("record");
                        callback.onSuccess(record);
                    } else {
                        callback.onError(respJson.optString("error", "Error creating entry"));
                    }
                } catch (Exception e) {
                    callback.onError(e.getMessage());
                }
            }
        }).start();
    }

    // CRUD: Read records
    public void readRecords(final String collection, final ApiCallback<JSONArray> callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String url = BASE_URL + "/data?collection=" + collection;
                    String response = executeGet(url, getToken());
                    JSONObject respJson = new JSONObject(response);
                    boolean success = respJson.optBoolean("success", false);

                    if (success) {
                        JSONArray records = respJson.optJSONArray("records");
                        if (records == null) {
                            records = new JSONArray();
                        }
                        callback.onSuccess(records);
                    } else {
                        callback.onError(respJson.optString("error", "Error loading ledger logs"));
                    }
                } catch (Exception e) {
                    callback.onError(e.getMessage());
                }
            }
        }).start();
    }

    // CRUD: Update record
    public void updateRecord(final String recordId, final JSONObject dataObj, final ApiCallback<String> callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject body = new JSONObject();
                    body.put("id", recordId);
                    body.put("data", dataObj);

                    String response = executePut(BASE_URL + "/data", body.toString(), getToken());
                    JSONObject respJson = new JSONObject(response);
                    boolean success = respJson.optBoolean("success", false);

                    if (success) {
                        callback.onSuccess(respJson.optString("id", recordId));
                    } else {
                        callback.onError(respJson.optString("error", "Error updating ledger item"));
                    }
                } catch (Exception e) {
                    callback.onError(e.getMessage());
                }
            }
        }).start();
    }

    // CRUD: Delete record
    public void deleteRecord(final String recordId, final ApiCallback<String> callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String url = BASE_URL + "/data?id=" + recordId;
                    String response = executeDelete(url, getToken());
                    JSONObject respJson = new JSONObject(response);
                    boolean success = respJson.optBoolean("success", false);

                    if (success) {
                        callback.onSuccess(respJson.optString("id", recordId));
                    } else {
                        callback.onError(respJson.optString("error", "Error deleting ledger item"));
                    }
                } catch (Exception e) {
                    callback.onError(e.getMessage());
                }
            }
        }).start();
    }

    // Helper: Execute POST request
    private String executePost(String urlStr, String jsonBody, String token) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);
        conn.setDoOutput(true);

        if (token != null) {
            conn.setRequestProperty("Authorization", "Bearer " + token);
        }

        OutputStream os = conn.getOutputStream();
        os.write(jsonBody.getBytes("UTF-8"));
        os.flush();
        os.close();

        int responseCode = conn.getResponseCode();
        BufferedReader br;
        if (responseCode >= 200 && responseCode < 300) {
            br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
        } else {
            br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "UTF-8"));
        }

        StringBuilder response = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            response.append(line);
        }
        br.close();
        conn.disconnect();
        return response.toString();
    }

    // Helper: Execute GET request
    private String executeGet(String urlStr, String token) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);

        if (token != null) {
            conn.setRequestProperty("Authorization", "Bearer " + token);
        }

        int responseCode = conn.getResponseCode();
        BufferedReader br;
        if (responseCode >= 200 && responseCode < 300) {
            br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
        } else {
            br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "UTF-8"));
        }

        StringBuilder response = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            response.append(line);
        }
        br.close();
        conn.disconnect();
        return response.toString();
    }

    // Helper: Execute PUT request
    private String executePut(String urlStr, String jsonBody, String token) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("PUT");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);
        conn.setDoOutput(true);

        if (token != null) {
            conn.setRequestProperty("Authorization", "Bearer " + token);
        }

        OutputStream os = conn.getOutputStream();
        os.write(jsonBody.getBytes("UTF-8"));
        os.flush();
        os.close();

        int responseCode = conn.getResponseCode();
        BufferedReader br;
        if (responseCode >= 200 && responseCode < 300) {
            br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
        } else {
            br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "UTF-8"));
        }

        StringBuilder response = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            response.append(line);
        }
        br.close();
        conn.disconnect();
        return response.toString();
    }

    // Helper: Execute DELETE request
    private String executeDelete(String urlStr, String token) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("DELETE");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);

        if (token != null) {
            conn.setRequestProperty("Authorization", "Bearer " + token);
        }

        int responseCode = conn.getResponseCode();
        BufferedReader br;
        if (responseCode >= 200 && responseCode < 300) {
            br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
        } else {
            br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "UTF-8"));
        }

        StringBuilder response = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            response.append(line);
        }
        br.close();
        conn.disconnect();
        return response.toString();
    }
}