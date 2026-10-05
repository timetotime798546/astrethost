package com.shopinventorysync.app;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class BackendApi {

    public interface ApiCallback {
        void onSuccess(JSONObject response);
        void onError(String error);
    }

    private static final String BASE_URL = "https://trumpledroid-api.cloudbeta28624.workers.dev";
    private String app_id;
    private String token;

    public BackendApi(Context context) {
        this.app_id = loadAppIdFromAssets(context);
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getToken() {
        return this.token;
    }

    private String loadAppIdFromAssets(Context context) {
        try {
            InputStream is = context.getAssets().open("app-meta.json");
            Scanner scanner = new Scanner(is);
            StringBuilder sb = new StringBuilder();
            while (scanner.hasNextLine()) {
                sb.append(scanner.nextLine());
            }
            scanner.close();
            is.close();
            JSONObject json = new JSONObject(sb.toString());
            // Safe check fallback in case dynamic app_id is passed as key
            if (json.has("app_id")) {
                return json.getString("app_id");
            } else {
                return "com.shopinventorysync.app";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "com.shopinventorysync.app";
        }
    }

    private void executeRequest(final String method, final String endpoint, final JSONObject payload, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection conn = null;
                try {
                    URL url = new URL(BASE_URL + endpoint);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod(method);
                    conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                    conn.setRequestProperty("Accept", "application/json");
                    conn.setConnectTimeout(15000);
                    conn.setReadTimeout(15000);

                    if (token != null && !token.isEmpty()) {
                        conn.setRequestProperty("Authorization", "Bearer " + token);
                    }

                    if (payload != null && (method.equals("POST") || method.equals("PUT"))) {
                        conn.setDoOutput(true);
                        OutputStream os = conn.getOutputStream();
                        os.write(payload.toString().getBytes("UTF-8"));
                        os.flush();
                        os.close();
                    }

                    int code = conn.getResponseCode();
                    InputStream is;
                    if (code >= 200 && code < 300) {
                        is = conn.getInputStream();
                    } else {
                        is = conn.getErrorStream();
                    }

                    if (is != null) {
                        Scanner scanner = new Scanner(is);
                        StringBuilder sb = new StringBuilder();
                        while (scanner.hasNextLine()) {
                            sb.append(scanner.nextLine());
                        }
                        scanner.close();
                        is.close();

                        final JSONObject response = new JSONObject(sb.toString());
                        if (code >= 200 && code < 300 && response.optBoolean("success", false)) {
                            callback.onSuccess(response);
                        } else {
                            final String errMessage = response.optString("error", response.optString("message", "Request failed. Code: " + code));
                            callback.onError(errMessage);
                        }
                    } else {
                        callback.onError("No response from server. Code: " + code);
                    }

                } catch (Exception e) {
                    callback.onError("Connection error: " + e.getLocalizedMessage());
                } finally {
                    if (conn != null) {
                        conn.disconnect();
                    }
                }
            }
        }).start();
    }

    public void register(String email, String password, ApiCallback callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("app_id", app_id);
            body.put("email", email);
            body.put("password", password);
            executeRequest("POST", "/register", body, callback);
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    public void login(String email, String password, ApiCallback callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("app_id", app_id);
            body.put("email", email);
            body.put("password", password);
            executeRequest("POST", "/login", body, callback);
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    public void logout(ApiCallback callback) {
        executeRequest("POST", "/logout", null, callback);
    }

    public void requestOtp(String email, ApiCallback callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("app_id", app_id);
            body.put("email", email);
            executeRequest("POST", "/request-otp", body, callback);
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    public void resetPassword(String email, String otp, String newPassword, ApiCallback callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("app_id", app_id);
            body.put("email", email);
            body.put("otp", otp);
            body.put("new_password", newPassword);
            executeRequest("POST", "/reset-password", body, callback);
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    public void createProduct(String name, double price, int quantity, ApiCallback callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("collection", "products");
            
            JSONObject data = new JSONObject();
            data.put("name", name);
            data.put("price", price);
            data.put("quantity", quantity);
            
            body.put("data", data);
            executeRequest("POST", "/data", body, callback);
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    public void getProducts(ApiCallback callback) {
        executeRequest("GET", "/data?collection=products", null, callback);
    }

    public void updateProduct(String id, String name, double price, int quantity, ApiCallback callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("id", id);
            
            JSONObject data = new JSONObject();
            data.put("name", name);
            data.put("price", price);
            data.put("quantity", quantity);
            
            body.put("data", data);
            executeRequest("PUT", "/data", body, callback);
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    public void deleteProduct(String id, ApiCallback callback) {
        executeRequest("DELETE", "/data?id=" + id, null, callback);
    }
}