package com.bizflowpro.app;

import android.content.Context;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONObject;

public class BackendApi {
    private String apiBaseUrl = "https://trumpledroid-api.cloudbeta28624.workers.dev";
    private String appId;
    private String token;

    public BackendApi(Context context) {
        this.appId = loadAppId(context);
    }

    private String loadAppId(Context context) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(context.getAssets().open("app-meta.json")));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            JSONObject json = new JSONObject(sb.toString());
            return json.optString("package_name", "com.bizflowpro.app");
        } catch (Exception e) {
            return "com.bizflowpro.app";
        }
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getToken() {
        return this.token;
    }

    public String getAppId() {
        return this.appId;
    }

    private String executeRequest(String endpoint, String method, String payload) throws Exception {
        URL url = new URL(apiBaseUrl + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setRequestProperty("Content-Type", "application/json");
        if (token != null && !token.isEmpty()) {
            conn.setRequestProperty("Authorization", "Bearer " + token);
        }
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);

        if (payload != null) {
            conn.setDoOutput(true);
            OutputStream os = conn.getOutputStream();
            os.write(payload.getBytes("UTF-8"));
            os.flush();
            os.close();
        }

        int responseCode = conn.getResponseCode();
        BufferedReader in;
        if (responseCode >= 200 && responseCode < 300) {
            in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        } else {
            in = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
        }

        String inputLine;
        StringBuilder response = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        if (responseCode < 200 || responseCode >= 300) {
            throw new Exception("Error (" + responseCode + "): " + response.toString());
        }

        return response.toString();
    }

    public String register(String email, String password) throws Exception {
        JSONObject body = new JSONObject();
        body.put("app_id", appId);
        body.put("email", email);
        body.put("password", password);
        return executeRequest("/register", "POST", body.toString());
    }

    public String login(String email, String password) throws Exception {
        JSONObject body = new JSONObject();
        body.put("app_id", appId);
        body.put("email", email);
        body.put("password", password);
        return executeRequest("/login", "POST", body.toString());
    }

    public String logout() throws Exception {
        return executeRequest("/logout", "POST", null);
    }

    public String createData(String collection, JSONObject data) throws Exception {
        JSONObject body = new JSONObject();
        body.put("collection", collection);
        body.put("data", data);
        return executeRequest("/data", "POST", body.toString());
    }

    public String readData(String collection) throws Exception {
        return executeRequest("/data?collection=" + collection, "GET", null);
    }

    public String updateData(String id, JSONObject data) throws Exception {
        JSONObject body = new JSONObject();
        body.put("id", id);
        body.put("data", data);
        return executeRequest("/data", "PUT", body.toString());
    }

    public String deleteData(String id) throws Exception {
        return executeRequest("/data?id=" + id, "DELETE", null);
    }
}