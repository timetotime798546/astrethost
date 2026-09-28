package com.cctvrecordbook.app;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class BackendApi {
    private static final String PREFS_NAME = "cctv_prefs";
    private static final String KEY_TOKEN = "auth_token";
    
    private Context mContext;
    private String mAppId;
    private String mToken;
    private static final String BASE_URL = "https://trumpledroid-api.cloudbeta28624.workers.dev";

    public BackendApi(Context context) {
        this.mContext = context.getApplicationContext();
        this.mAppId = loadAppId(this.mContext);
        this.mToken = loadToken();
    }

    private String loadAppId(Context context) {
        try {
            InputStream is = context.getAssets().open("app-meta.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json = new String(buffer, "UTF-8");
            JSONObject obj = new JSONObject(json);
            return obj.optString("app_id", obj.optString("package_name", "com.cctvrecordbook.app"));
        } catch (Exception e) {
            e.printStackTrace();
            return "com.cctvrecordbook.app";
        }
    }

    private String loadToken() {
        SharedPreferences prefs = mContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_TOKEN, null);
    }

    public void saveToken(String token) {
        this.mToken = token;
        SharedPreferences prefs = mContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_TOKEN, token).apply();
    }

    public void clearToken() {
        this.mToken = null;
        SharedPreferences prefs = mContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_TOKEN).apply();
    }

    public boolean isLoggedIn() {
        return mToken != null;
    }

    private String executeRequest(String endpoint, String method, String jsonBody, boolean useToken) throws Exception {
        URL url = new URL(BASE_URL + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");
        
        if (useToken && mToken != null) {
            conn.setRequestProperty("Authorization", "Bearer " + mToken);
        }
        
        if (jsonBody != null) {
            conn.setDoOutput(true);
            OutputStream os = conn.getOutputStream();
            os.write(jsonBody.getBytes("UTF-8"));
            os.flush();
            os.close();
        }
        
        int responseCode = conn.getResponseCode();
        InputStream is;
        if (responseCode >= 200 && responseCode < 300) {
            is = conn.getInputStream();
        } else {
            is = conn.getErrorStream();
        }
        
        if (is == null) {
            throw new Exception("Network connection error");
        }
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();
        is.close();
        
        return sb.toString();
    }

    public JSONObject register(String email, String password) throws Exception {
        JSONObject body = new JSONObject();
        body.put("app_id", mAppId);
        body.put("email", email);
        body.put("password", password);
        String resp = executeRequest("/register", "POST", body.toString(), false);
        return new JSONObject(resp);
    }

    public JSONObject login(String email, String password) throws Exception {
        JSONObject body = new JSONObject();
        body.put("app_id", mAppId);
        body.put("email", email);
        body.put("password", password);
        String resp = executeRequest("/login", "POST", body.toString(), false);
        JSONObject obj = new JSONObject(resp);
        if (obj.optBoolean("success")) {
            String token = obj.optString("token");
            if (token != null && !token.isEmpty()) {
                saveToken(token);
            }
        }
        return obj;
    }

    public void logout() {
        try {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        executeRequest("/logout", "POST", null, true);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        } catch (Exception e) {}
        clearToken();
    }

    public JSONObject createRecord(String collection, JSONObject data) throws Exception {
        JSONObject body = new JSONObject();
        body.put("collection", collection);
        body.put("data", data);
        String resp = executeRequest("/data", "POST", body.toString(), true);
        return new JSONObject(resp);
    }

    public JSONObject readRecords(String collection) throws Exception {
        String resp = executeRequest("/data?collection=" + collection, "GET", null, true);
        return new JSONObject(resp);
    }

    public JSONObject updateRecord(String id, JSONObject data) throws Exception {
        JSONObject body = new JSONObject();
        body.put("id", id);
        body.put("data", data);
        String resp = executeRequest("/data", "PUT", body.toString(), true);
        return new JSONObject(resp);
    }

    public JSONObject deleteRecord(String id) throws Exception {
        String resp = executeRequest("/data?id=" + id, "DELETE", null, true);
        return new JSONObject(resp);
    }

    public JSONObject verifyLogic(String logicName, JSONObject values, double result) throws Exception {
        JSONObject body = new JSONObject();
        body.put("logic", logicName);
        body.put("values", values);
        body.put("result", result);
        String resp = executeRequest("/verify", "POST", body.toString(), true);
        return new JSONObject(resp);
    }
}