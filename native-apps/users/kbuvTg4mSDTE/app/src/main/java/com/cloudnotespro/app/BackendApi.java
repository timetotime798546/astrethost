package com.cloudnotespro.app;

import android.content.Context;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class BackendApi {

    private static final String TAG = "BackendApi";
    private static final String BASE_URL = "https://trumpledroid-api.cloudbeta28624.workers.dev";
    private final Context context;

    public BackendApi(Context context) {
        this.context = context.getApplicationContext();
    }

    private String getAppId() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(context.getAssets().open("app-meta.json")));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            JSONObject obj = new JSONObject(sb.toString());
            return obj.optString("app_id", "app_6acdfe0c4ebcf");
        } catch (Exception e) {
            Log.e(TAG, "Error reading app_id from assets", e);
            return "app_6acdfe0c4ebcf";
        }
    }

    public JSONObject register(String email, String password) {
        try {
            URL url = new URL(BASE_URL + "/register");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            JSONObject body = new JSONObject();
            body.put("app_id", getAppId());
            body.put("email", email);
            body.put("password", password);

            OutputStream os = conn.getOutputStream();
            os.write(body.toString().getBytes("UTF-8"));
            os.flush();
            os.close();

            int responseCode = conn.getResponseCode();
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    responseCode >= 400 ? conn.getErrorStream() : conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            br.close();

            String raw = sb.toString();
            JSONObject response;
            try {
                response = new JSONObject(raw);
            } catch (Exception parseEx) {
                response = new JSONObject();
                response.put("success", responseCode >= 200 && responseCode < 300);
                response.put("message", raw);
            }
            response.put("http_code", responseCode);
            if (!response.has("success")) {
                response.put("success", responseCode >= 200 && responseCode < 300);
            }
            return response;
        } catch (Exception e) {
            Log.e(TAG, "Register exception", e);
            try {
                JSONObject err = new JSONObject();
                err.put("success", false);
                err.put("error", e.getMessage());
                return err;
            } catch (Exception ex) {
                return null;
            }
        }
    }

    public JSONObject login(String email, String password) {
        try {
            URL url = new URL(BASE_URL + "/login");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            JSONObject body = new JSONObject();
            body.put("app_id", getAppId());
            body.put("email", email);
            body.put("password", password);

            OutputStream os = conn.getOutputStream();
            os.write(body.toString().getBytes("UTF-8"));
            os.flush();
            os.close();

            int responseCode = conn.getResponseCode();
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    responseCode >= 400 ? conn.getErrorStream() : conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            br.close();

            String raw = sb.toString();
            JSONObject response;
            try {
                response = new JSONObject(raw);
            } catch (Exception parseEx) {
                response = new JSONObject();
                response.put("success", responseCode >= 200 && responseCode < 300);
                response.put("message", raw);
            }
            response.put("http_code", responseCode);
            if (!response.has("success")) {
                response.put("success", responseCode >= 200 && responseCode < 300);
            }
            return response;
        } catch (Exception e) {
            Log.e(TAG, "Login exception", e);
            try {
                JSONObject err = new JSONObject();
                err.put("success", false);
                err.put("error", e.getMessage());
                return err;
            } catch (Exception ex) {
                return null;
            }
        }
    }

    public JSONObject logout(String token) {
        try {
            URL url = new URL(BASE_URL + "/logout");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setDoOutput(true);

            int responseCode = conn.getResponseCode();
            JSONObject response = new JSONObject();
            response.put("success", responseCode >= 200 && responseCode < 300);
            response.put("http_code", responseCode);
            return response;
        } catch (Exception e) {
            Log.e(TAG, "Logout exception", e);
            try {
                JSONObject err = new JSONObject();
                err.put("success", false);
                err.put("error", e.getMessage());
                return err;
            } catch (Exception ex) {
                return null;
            }
        }
    }

    public JSONObject createData(String token, String collection, JSONObject data) {
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
            os.flush();
            os.close();

            int responseCode = conn.getResponseCode();
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    responseCode >= 400 ? conn.getErrorStream() : conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            br.close();

            String raw = sb.toString();
            JSONObject response;
            try {
                response = new JSONObject(raw);
            } catch (Exception parseEx) {
                response = new JSONObject();
                response.put("success", responseCode >= 200 && responseCode < 300);
            }
            response.put("http_code", responseCode);
            if (!response.has("success")) {
                response.put("success", responseCode >= 200 && responseCode < 300);
            }
            return response;
        } catch (Exception e) {
            Log.e(TAG, "CreateData exception", e);
            try {
                JSONObject err = new JSONObject();
                err.put("success", false);
                err.put("error", e.getMessage());
                return err;
            } catch (Exception ex) {
                return null;
            }
        }
    }

    public JSONObject readData(String token, String collection) {
        try {
            URL url = new URL(BASE_URL + "/data?collection=" + collection);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + token);

            int responseCode = conn.getResponseCode();
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    responseCode >= 400 ? conn.getErrorStream() : conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            br.close();

            String raw = sb.toString();
            JSONObject response;
            try {
                response = new JSONObject(raw);
            } catch (Exception parseEx) {
                response = new JSONObject();
                response.put("success", responseCode >= 200 && responseCode < 300);
            }
            response.put("http_code", responseCode);
            if (!response.has("success")) {
                response.put("success", responseCode >= 200 && responseCode < 300);
            }
            return response;
        } catch (Exception e) {
            Log.e(TAG, "ReadData exception", e);
            try {
                JSONObject err = new JSONObject();
                err.put("success", false);
                err.put("error", e.getMessage());
                return err;
            } catch (Exception ex) {
                return null;
            }
        }
    }

    public JSONObject updateData(String token, String id, JSONObject data) {
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
            os.flush();
            os.close();

            int responseCode = conn.getResponseCode();
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    responseCode >= 400 ? conn.getErrorStream() : conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            br.close();

            String raw = sb.toString();
            JSONObject response;
            try {
                response = new JSONObject(raw);
            } catch (Exception parseEx) {
                response = new JSONObject();
                response.put("success", responseCode >= 200 && responseCode < 300);
            }
            response.put("http_code", responseCode);
            if (!response.has("success")) {
                response.put("success", responseCode >= 200 && responseCode < 300);
            }
            return response;
        } catch (Exception e) {
            Log.e(TAG, "UpdateData exception", e);
            try {
                JSONObject err = new JSONObject();
                err.put("success", false);
                err.put("error", e.getMessage());
                return err;
            } catch (Exception ex) {
                return null;
            }
        }
    }

    public JSONObject deleteData(String token, String id) {
        try {
            URL url = new URL(BASE_URL + "/data?id=" + id);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("DELETE");
            conn.setRequestProperty("Authorization", "Bearer " + token);

            int responseCode = conn.getResponseCode();
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    responseCode >= 400 ? conn.getErrorStream() : conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            br.close();

            String raw = sb.toString();
            JSONObject response;
            try {
                response = new JSONObject(raw);
            } catch (Exception parseEx) {
                response = new JSONObject();
                response.put("success", responseCode >= 200 && responseCode < 300);
            }
            response.put("http_code", responseCode);
            if (!response.has("success")) {
                response.put("success", responseCode >= 200 && responseCode < 300);
            }
            return response;
        } catch (Exception e) {
            Log.e(TAG, "DeleteData exception", e);
            try {
                JSONObject err = new JSONObject();
                err.put("success", false);
                err.put("error", e.getMessage());
                return err;
            } catch (Exception ex) {
                return null;
            }
        }
    }
}