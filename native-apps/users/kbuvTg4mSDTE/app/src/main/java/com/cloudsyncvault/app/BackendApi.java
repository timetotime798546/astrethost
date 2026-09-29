package com.cloudsyncvault.app;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONObject;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class BackendApi {
    private static final String BASE_URL = "https://trumpledroid-api.cloudbeta28624.workers.dev";
    private final SharedPreferences prefs;

    public BackendApi(Context context) {
        prefs = context.getSharedPreferences("vault_prefs", Context.MODE_PRIVATE);
    }

    public String getToken() {
        return prefs.getString("token", null);
    }

    public void setToken(String token) {
        prefs.edit().putString("token", token).apply();
    }

    public JSONObject request(String method, String endpoint, JSONObject body, boolean auth) throws Exception {
        URL url = new URL(BASE_URL + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setRequestProperty("Content-Type", "application/json");
        if (auth) conn.setRequestProperty("Authorization", "Bearer " + getToken());
        
        if (body != null) {
            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.toString().getBytes("UTF-8"));
            }
        }
        
        int code = conn.getResponseCode();
        try (Scanner scanner = new Scanner(code < 400 ? conn.getInputStream() : conn.getErrorStream())) {
            String response = scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "{}";
            return new JSONObject(response);
        }
    }
}