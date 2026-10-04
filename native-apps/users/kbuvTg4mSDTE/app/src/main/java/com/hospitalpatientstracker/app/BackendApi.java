package com.hospitalpatientstracker.app;

import android.os.AsyncTask;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONObject;

public class BackendApi {
    private final String baseUrl;
    private final String appId;
    private String token;

    public interface ApiCallback {
        void onSuccess(String rawResponse);
        void onError(String errorMsg);
    }

    public BackendApi(String baseUrl, String appId) {
        this.baseUrl = baseUrl;
        this.appId = appId;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getToken() {
        return this.token;
    }

    public void register(final String email, final String password, final ApiCallback callback) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("app_id", appId);
            payload.put("email", email);
            payload.put("password", password);
            executeTask("/register", "POST", payload.toString(), false, callback);
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    public void login(final String email, final String password, final ApiCallback callback) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("app_id", appId);
            payload.put("email", email);
            payload.put("password", password);
            executeTask("/login", "POST", payload.toString(), false, callback);
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    public void logout(final ApiCallback callback) {
        executeTask("/logout", "POST", null, true, callback);
    }

    public void requestOtp(final String email, final ApiCallback callback) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("app_id", appId);
            payload.put("email", email);
            executeTask("/request-otp", "POST", payload.toString(), false, callback);
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    public void resetPassword(final String email, final String otp, final String newPassword, final ApiCallback callback) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("app_id", appId);
            payload.put("email", email);
            payload.put("otp", otp);
            payload.put("new_password", newPassword);
            executeTask("/reset-password", "POST", payload.toString(), false, callback);
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    public void createRecord(final String collection, final JSONObject data, final ApiCallback callback) {
        try {
            JSONObject outer = new JSONObject();
            outer.put("collection", collection);
            outer.put("data", data);
            executeTask("/data", "POST", outer.toString(), true, callback);
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    public void readRecords(final String collection, final ApiCallback callback) {
        executeTask("/data?collection=" + collection, "GET", null, true, callback);
    }

    public void updateRecord(final String recordId, final JSONObject data, final ApiCallback callback) {
        try {
            JSONObject outer = new JSONObject();
            outer.put("id", recordId);
            outer.put("data", data);
            executeTask("/data", "PUT", outer.toString(), true, callback);
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    public void deleteRecord(final String recordId, final ApiCallback callback) {
        executeTask("/data?id=" + recordId, "DELETE", null, true, callback);
    }

    private void executeTask(final String endpoint, final String method, final String payload, final boolean useToken, final ApiCallback callback) {
        new AsyncTask<Void, Void, String>() {
            private String error = null;

            @Override
            protected String doInBackground(Void... voids) {
                HttpURLConnection conn = null;
                try {
                    URL url = new URL(baseUrl + endpoint);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod(method);
                    conn.setConnectTimeout(20000);
                    conn.setReadTimeout(20000);
                    conn.setDoInput(true);

                    if (useToken && token != null) {
                        conn.setRequestProperty("Authorization", "Bearer " + token);
                    }

                    if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)) {
                        conn.setDoOutput(true);
                        conn.setRequestProperty("Content-Type", "application/json");
                        if (payload != null) {
                            OutputStream os = conn.getOutputStream();
                            os.write(payload.getBytes("UTF-8"));
                            os.flush();
                            os.close();
                        }
                    }

                    int responseCode = conn.getResponseCode();
                    InputStream is;
                    if (responseCode >= 200 && responseCode < 300) {
                        is = conn.getInputStream();
                    } else {
                        is = conn.getErrorStream();
                    }

                    if (is == null) {
                        throw new Exception("Server response stream empty, status: " + responseCode);
                    }

                    BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    reader.close();
                    is.close();

                    String raw = sb.toString();
                    if (responseCode >= 200 && responseCode < 300) {
                        return raw;
                    } else {
                        try {
                            JSONObject errJson = new JSONObject(raw);
                            if (errJson.has("error")) {
                                throw new Exception(errJson.getString("error"));
                            } else if (errJson.has("message")) {
                                throw new Exception(errJson.getString("message"));
                            }
                        } catch (Exception ex) {
                            // ignore, fail over to generic responseCode exception
                        }
                        throw new Exception("HTTP failure: " + responseCode + " - " + raw);
                    }

                } catch (Exception e) {
                    error = e.getMessage();
                    return null;
                } finally {
                    if (conn != null) {
                        conn.disconnect();
                    }
                }
            }

            @Override
            protected void onPostExecute(String s) {
                if (error != null) {
                    callback.onError(error);
                } else {
                    callback.onSuccess(s);
                }
            }
        }.execute();
    }
}