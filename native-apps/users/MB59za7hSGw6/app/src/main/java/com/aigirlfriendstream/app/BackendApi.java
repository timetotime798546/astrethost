package com.aigirlfriendstream.app;

import android.content.Context;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class BackendApi {

    private static final String BASE_URL = "https://trumpledroid-api.cloudbeta28624.workers.dev";
    private final Context context;

    public interface Callback {
        void onSuccess(String response);
        void onError(String error);
    }

    public BackendApi(Context context) {
        this.context = context;
    }

    public String getAppId() {
        try {
            InputStream is = context.getAssets().open("app-meta.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String jsonStr = new String(buffer, "UTF-8");
            JSONObject obj = new JSONObject(jsonStr);
            return obj.optString("app_id", "ai_girlfriend_stream_v1");
        } catch (Exception e) {
            return "ai_girlfriend_stream_v1";
        }
    }

    public void createRecord(final String collection, final JSONObject dataJson, final Callback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(BASE_URL + "/data");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);

                    JSONObject body = new JSONObject();
                    body.put("collection", collection);
                    body.put("data", dataJson);

                    OutputStream os = conn.getOutputStream();
                    os.write(body.toString().getBytes("UTF-8"));
                    os.close();

                    int responseCode = conn.getResponseCode();
                    InputStream is = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    reader.close();

                    final String resStr = sb.toString();
                    if (responseCode >= 200 && responseCode < 300) {
                        callback.onSuccess(resStr);
                    } else {
                        callback.onError("Server error " + responseCode + ": " + resStr);
                    }
                } catch (final Exception e) {
                    callback.onError("Network error: " + e.getLocalizedMessage());
                }
            }
        }).start();
    }
}