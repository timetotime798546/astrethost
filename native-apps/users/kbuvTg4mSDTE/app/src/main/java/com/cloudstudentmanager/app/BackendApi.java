package com.cloudstudentmanager.app;

import android.content.Context;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class BackendApi {
    private String baseUrl = "https://trumpledroid-api.cloudbeta28624.workers.dev";
    private String appId;
    private String token;

    public BackendApi(Context context) {
        this.appId = getAppIdFromAssets(context);
    }

    public void setToken(String token) {
        this.token = token;
    }

    private String getAppIdFromAssets(Context context) {
        try {
            InputStream is = context.getAssets().open("app-meta.json");
            Scanner s = new Scanner(is).useDelimiter("\\A");
            String result = s.hasNext() ? s.next() : "";
            JSONObject json = new JSONObject(result);
            return json.getString("package_name");
        } catch (Exception e) {
            return "com.cloudstudentmanager.app";
        }
    }

    public interface ApiCallback {
        void onSuccess(JSONObject response);
        void onError(String message);
    }

    private void performRequest(final String method, final String path, final JSONObject body, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection conn = null;
                try {
                    URL url = new URL(baseUrl + path);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod(method);
                    conn.setRequestProperty("Content-Type", "application/json");
                    if (token != null) {
                        conn.setRequestProperty("Authorization", "Bearer " + token);
                    }

                    if (body != null && (method.equals("POST") || method.equals("PUT"))) {
                        conn.setDoOutput(true);
                        OutputStream os = conn.getOutputStream();
                        os.write(body.toString().getBytes("UTF-8"));
                        os.close();
                    }

                    int responseCode = conn.getResponseCode();
                    InputStream is;
                    if (responseCode >= 200 && responseCode < 300) {
                        is = conn.getInputStream();
                    } else {
                        is = conn.getErrorStream();
                    }

                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    final JSONObject jsonResponse = new JSONObject(response.toString());
                    if (jsonResponse.optBoolean("success", false)) {
                        callback.onSuccess(jsonResponse);
                    } else {
                        callback.onError(jsonResponse.optString("error", "Unknown error"));
                    }

                } catch (final Exception e) {
                    callback.onError(e.getMessage());
                } finally {
                    if (conn != null) conn.disconnect();
                }
            }
        }).start();
    }

    public void register(String email, String password, ApiCallback callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("app_id", appId);
            body.put("email", email);
            body.put("password", password);
            performRequest("POST", "/register", body, callback);
        } catch (Exception e) { callback.onError(e.getMessage()); }
    }

    public void login(String email, String password, ApiCallback callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("app_id", appId);
            body.put("email", email);
            body.put("password", password);
            performRequest("POST", "/login", body, callback);
        } catch (Exception e) { callback.onError(e.getMessage()); }
    }

    public void createStudent(String name, String rollNo, String course, String phone, String email, ApiCallback callback) {
        try {
            JSONObject data = new JSONObject();
            data.put("name", name);
            data.put("roll_no", rollNo);
            data.put("course", course);
            data.put("phone", phone);
            data.put("email", email);

            JSONObject body = new JSONObject();
            body.put("collection", "students");
            body.put("data", data);
            performRequest("POST", "/data", body, callback);
        } catch (Exception e) { callback.onError(e.getMessage()); }
    }

    public void getStudents(ApiCallback callback) {
        performRequest("GET", "/data?collection=students", null, callback);
    }

    public void updateStudent(String id, String name, String rollNo, String course, String phone, String email, ApiCallback callback) {
        try {
            JSONObject data = new JSONObject();
            data.put("name", name);
            data.put("roll_no", rollNo);
            data.put("course", course);
            data.put("phone", phone);
            data.put("email", email);

            JSONObject body = new JSONObject();
            body.put("id", id);
            body.put("data", data);
            performRequest("PUT", "/data", body, callback);
        } catch (Exception e) { callback.onError(e.getMessage()); }
    }

    public void deleteStudent(String id, ApiCallback callback) {
        performRequest("DELETE", "/data?id=" + id, null, callback);
    }
}