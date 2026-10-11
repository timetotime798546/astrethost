package com.imagevault.app;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ImageWorkerApi {
    private static final String BASE_URL = "https://trumpledroid-image.cloudbeta28624.workers.dev";
    private static final String BOUNDARY = "----ImageVaultBoundary" + System.currentTimeMillis();
    private static final String LINE_FEED = "\r\n";

    public interface ImageListCallback {
        void onSuccess(List<String> imageIds);
        void onError(String message);
    }

    public interface UploadCallback {
        void onSuccess(String imageId);
        void onError(String message);
    }

    public interface BitmapCallback {
        void onSuccess(Bitmap bitmap);
        void onError(String message);
    }

    public static void listImages(final String token, final ImageListCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(BASE_URL + "/images");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(15000);
                    conn.setReadTimeout(20000);
                    conn.setRequestProperty("Authorization", "Bearer " + token);

                    int code = conn.getResponseCode();
                    InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    reader.close();
                    conn.disconnect();

                    if (code >= 200 && code < 300) {
                        List<String> imageIds = new ArrayList<String>();
                        String res = sb.toString().trim();
                        if (res.startsWith("[")) {
                            JSONArray arr = new JSONArray(res);
                            for (int i = 0; i < arr.length(); i++) {
                                Object item = arr.get(i);
                                if (item instanceof JSONObject) {
                                    JSONObject obj = (JSONObject) item;
                                    String id = obj.optString("id", obj.optString("image_id", ""));
                                    if (!id.isEmpty()) imageIds.add(id);
                                } else {
                                    imageIds.add(String.valueOf(item));
                                }
                            }
                        } else {
                            JSONObject root = new JSONObject(res);
                            JSONArray arr = root.optJSONArray("images");
                            if (arr == null) arr = root.optJSONArray("records");
                            if (arr != null) {
                                for (int i = 0; i < arr.length(); i++) {
                                    Object item = arr.get(i);
                                    if (item instanceof JSONObject) {
                                        JSONObject obj = (JSONObject) item;
                                        String id = obj.optString("id", obj.optString("image_id", ""));
                                        if (!id.isEmpty()) imageIds.add(id);
                                    } else {
                                        imageIds.add(String.valueOf(item));
                                    }
                                }
                            }
                        }
                        callback.onSuccess(imageIds);
                    } else {
                        callback.onError("Failed to fetch image list (HTTP " + code + ")");
                    }
                } catch (Exception e) {
                    callback.onError("Error loading images: " + e.getMessage());
                }
            }
        }).start();
    }

    public static void uploadImage(final String token, final byte[] imageBytes, final UploadCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(BASE_URL + "/upload");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setUseCaches(false);
                    conn.setDoOutput(true);
                    conn.setDoInput(true);
                    conn.setRequestMethod("POST");
                    conn.setConnectTimeout(20000);
                    conn.setReadTimeout(30000);
                    conn.setRequestProperty("Authorization", "Bearer " + token);
                    conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + BOUNDARY);

                    DataOutputStream request = new DataOutputStream(conn.getOutputStream());

                    request.writeBytes("--" + BOUNDARY + LINE_FEED);
                    request.writeBytes("Content-Disposition: form-data; name=\"image\"; filename=\"photo.jpg\"" + LINE_FEED);
                    request.writeBytes("Content-Type: image/jpeg" + LINE_FEED);
                    request.writeBytes(LINE_FEED);
                    request.write(imageBytes);
                    request.writeBytes(LINE_FEED);
                    request.writeBytes("--" + BOUNDARY + "--" + LINE_FEED);
                    request.flush();
                    request.close();

                    int code = conn.getResponseCode();
                    InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    reader.close();
                    conn.disconnect();

                    if (code >= 200 && code < 300) {
                        JSONObject obj = new JSONObject(sb.toString());
                        String id = obj.optString("id", obj.optString("image_id", ""));
                        if (id.isEmpty() && obj.has("data")) {
                            JSONObject d = obj.optJSONObject("data");
                            if (d != null) id = d.optString("id", d.optString("image_id", ""));
                        }
                        if (id.isEmpty()) {
                            id = "img_" + System.currentTimeMillis();
                        }
                        callback.onSuccess(id);
                    } else {
                        callback.onError("Upload failed (HTTP " + code + "): " + sb.toString());
                    }
                } catch (Exception e) {
                    callback.onError("Upload exception: " + e.getMessage());
                }
            }
        }).start();
    }

    public static void fetchImage(final String token, final String imageId, final BitmapCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(BASE_URL + "/image?id=" + imageId);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(15000);
                    conn.setReadTimeout(25000);
                    conn.setRequestProperty("Authorization", "Bearer " + token);

                    int code = conn.getResponseCode();
                    if (code >= 200 && code < 300) {
                        InputStream is = conn.getInputStream();
                        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                        byte[] data = new byte[8192];
                        int nRead;
                        while ((nRead = is.read(data, 0, data.length)) != -1) {
                            buffer.write(data, 0, nRead);
                        }
                        buffer.flush();
                        byte[] bytes = buffer.toByteArray();
                        is.close();
                        conn.disconnect();

                        Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        if (bmp != null) {
                            callback.onSuccess(bmp);
                        } else {
                            callback.onError("Could not decode image bytes");
                        }
                    } else {
                        conn.disconnect();
                        callback.onError("Fetch error code " + code);
                    }
                } catch (Exception e) {
                    callback.onError("Error loading image: " + e.getMessage());
                }
            }
        }).start();
    }
}