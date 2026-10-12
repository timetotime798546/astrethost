package com.notesapp.app;

import android.content.Context;
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

    public interface ApiCallback {
        void onSuccess(JSONObject response);
        void onError(String error);
    }

    public BackendApi(Context context) {
        this.context = context.getApplicationContext();
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
            return obj.optString("app_id", "notesapp");
        } catch (Exception e) {
            return "notesapp";
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

                    NetworkResult res = sendPost(BASE_URL + "/register", body.toString(), null);
                    if (res.isSuccess()) {
                        JSONObject jsonRes = new JSONObject(res.responseStr);
                        callback.onSuccess(jsonRes);
                    } else {
                        callback.onError(res.errorMessage);
                    }
                } catch (Exception e) {
                    callback.onError("Registration error: " + e.getMessage());
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

                    NetworkResult res = sendPost(BASE_URL + "/login", body.toString(), null);
                    if (res.isSuccess()) {
                        JSONObject jsonRes = new JSONObject(res.responseStr);
                        callback.onSuccess(jsonRes);
                    } else {
                        callback.onError(res.errorMessage);
                    }
                } catch (Exception e) {
                    callback.onError("Login error: " + e.getMessage());
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

                    NetworkResult res = sendPost(BASE_URL + "/request-otp", body.toString(), null);
                    if (res.isSuccess()) {
                        JSONObject jsonRes = new JSONObject(res.responseStr);
                        callback.onSuccess(jsonRes);
                    } else {
                        callback.onError(res.errorMessage);
                    }
                } catch (Exception e) {
                    callback.onError("OTP Request error: " + e.getMessage());
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

                    NetworkResult res = sendPost(BASE_URL + "/reset-password", body.toString(), null);
                    if (res.isSuccess()) {
                        JSONObject jsonRes = new JSONObject(res.responseStr);
                        callback.onSuccess(jsonRes);
                    } else {
                        callback.onError(res.errorMessage);
                    }
                } catch (Exception e) {
                    callback.onError("Password Reset error: " + e.getMessage());
                }
            }
        }).start();
    }

    public void fetchNotes(final String token, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    NetworkResult res = sendGet(BASE_URL + "/data?collection=notes", token);
                    if (res.isSuccess()) {
                        JSONObject jsonRes = new JSONObject(res.responseStr);
                        callback.onSuccess(jsonRes);
                    } else {
                        callback.onError(res.errorMessage);
                    }
                } catch (Exception e) {
                    callback.onError("Fetch error: " + e.getMessage());
                }
            }
        }).start();
    }

    public void createNote(final String token, final JSONObject noteData, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject body = new JSONObject();
                    body.put("collection", "notes");
                    body.put("data", noteData);

                    NetworkResult res = sendPost(BASE_URL + "/data", body.toString(), token);
                    if (res.isSuccess()) {
                        JSONObject jsonRes = new JSONObject(res.responseStr);
                        callback.onSuccess(jsonRes);
                    } else {
                        callback.onError(res.errorMessage);
                    }
                } catch (Exception e) {
                    callback.onError("Create error: " + e.getMessage());
                }
            }
        }).start();
    }

    public void updateNote(final String token, final String recordId, final JSONObject noteData, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject body = new JSONObject();
                    body.put("id", recordId);
                    body.put("data", noteData);

                    NetworkResult res = sendRequest(BASE_URL + "/data", "PUT", body.toString(), token);
                    if (res.isSuccess()) {
                        JSONObject jsonRes = new JSONObject(res.responseStr);
                        callback.onSuccess(jsonRes);
                    } else {
                        callback.onError(res.errorMessage);
                    }
                } catch (Exception e) {
                    callback.onError("Update error: " + e.getMessage());
                }
            }
        }).start();
    }

    public void deleteNote(final String token, final String recordId, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    NetworkResult res = sendRequest(BASE_URL + "/data?id=" + recordId, "DELETE", null, token);
                    if (res.isSuccess()) {
                        JSONObject jsonRes = new JSONObject(res.responseStr);
                        callback.onSuccess(jsonRes);
                    } else {
                        callback.onError(res.errorMessage);
                    }
                } catch (Exception e) {
                    callback.onError("Delete error: " + e.getMessage());
                }
            }
        }).start();
    }

    private NetworkResult sendPost(String urlStr, String jsonBody, String token) {
        return sendRequest(urlStr, "POST", jsonBody, token);
    }

    private NetworkResult sendGet(String urlStr, String token) {
        return sendRequest(urlStr, "GET", null, token);
    }

    private NetworkResult sendRequest(String urlStr, String method, String jsonBody, String token) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(method);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setRequestProperty("Accept", "application/json");

            if (token != null && !token.isEmpty()) {
                conn.setRequestProperty("Authorization", "Bearer " + token);
            }

            if (jsonBody != null && (method.equals("POST") || method.equals("PUT"))) {
                conn.setDoOutput(true);
                OutputStream os = conn.getOutputStream();
                os.write(jsonBody.getBytes("UTF-8"));
                os.flush();
                os.close();
            }

            int status = conn.getResponseCode();
            InputStream is = (status >= 200 && status < 300) ? conn.getInputStream() : conn.getErrorStream();
            
            StringBuilder sb = new StringBuilder();
            if (is != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();
            }

            String resp = sb.toString();
            if (status >= 200 && status < 300) {
                return new NetworkResult(true, resp, null);
            } else {
                String errMsg = "Request failed (" + status + ")";
                try {
                    JSONObject errObj = new JSONObject(resp);
                    if (errObj.has("error")) errMsg = errObj.getString("error");
                    else if (errObj.has("message")) errMsg = errObj.getString("message");
                } catch (Exception ignored) {}
                return new NetworkResult(false, null, errMsg);
            }
        } catch (Exception e) {
            return new NetworkResult(false, null, "Network failure: " + e.getMessage());
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static class NetworkResult {
        boolean success;
        String responseStr;
        String errorMessage;

        NetworkResult(boolean success, String responseStr, String errorMessage) {
            this.success = success;
            this.responseStr = responseStr;
            this.errorMessage = errorMessage;
        }

        boolean isSuccess() { return success; }
    }
}