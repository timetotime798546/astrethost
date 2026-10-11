package com.productimagemanager.app;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class ImageWorkerApi {

    public static final String IMAGE_BASE_URL = "https://trumpledroid-image.cloudbeta28624.workers.dev";

    public interface UploadCallback {
        void onSuccess(String imageId);
        void onError(String error);
    }

    public interface ImageDownloadCallback {
        void onSuccess(Bitmap bitmap);
        void onError(String error);
    }

    public interface DeleteCallback {
        void onSuccess();
        void onError(String error);
    }

    public static void uploadImage(final byte[] imageBytes, final String token, final UploadCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (token == null || token.trim().isEmpty()) {
                        callback.onError("Authentication token is required for upload");
                        return;
                    }
                    if (imageBytes == null || imageBytes.length == 0) {
                        callback.onError("No image data provided for upload");
                        return;
                    }
                    if (imageBytes.length > 200 * 1024) {
                        callback.onError("Image exceeds 200 KB limit (" + (imageBytes.length / 1024) + " KB)");
                        return;
                    }

                    String boundary = "----TrumpleDroidBoundary" + System.currentTimeMillis();
                    String lineEnd = "\r\n";
                    String twoHyphens = "--";

                    URL url = new URL(IMAGE_BASE_URL + "/upload");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setDoInput(true);
                    conn.setDoOutput(true);
                    conn.setUseCaches(false);
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Connection", "Keep-Alive");
                    conn.setRequestProperty("Authorization", "Bearer " + token);
                    conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                    conn.setConnectTimeout(25000);
                    conn.setReadTimeout(25000);

                    DataOutputStream dos = new DataOutputStream(conn.getOutputStream());

                    dos.writeBytes(twoHyphens + boundary + lineEnd);
                    dos.writeBytes("Content-Disposition: form-data; name=\"image\"; filename=\"product.jpg\"" + lineEnd);
                    dos.writeBytes("Content-Type: image/jpeg" + lineEnd);
                    dos.writeBytes(lineEnd);
                    dos.write(imageBytes);
                    dos.writeBytes(lineEnd);
                    dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
                    dos.flush();
                    dos.close();

                    int responseCode = conn.getResponseCode();
                    InputStream is = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream();

                    if (is == null) {
                        callback.onError("Upload failed with HTTP " + responseCode);
                        return;
                    }

                    BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    reader.close();
                    conn.disconnect();

                    JSONObject json = new JSONObject(sb.toString());
                    if (json.optBoolean("success", false) || json.has("id")) {
                        String id = json.optString("id", json.optString("imageId", ""));
                        if (!id.isEmpty()) {
                            callback.onSuccess(id);
                        } else {
                            callback.onError("Upload response missing image ID");
                        }
                    } else {
                        String err = json.optString("error", json.optString("message", "Upload failed"));
                        callback.onError(err);
                    }
                } catch (Exception e) {
                    callback.onError("Upload failed: " + e.getMessage());
                }
            }
        }).start();
    }

    public static void fetchImage(final String imageId, final String token, final ImageDownloadCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (imageId == null || imageId.trim().isEmpty()) {
                        callback.onError("Invalid image ID");
                        return;
                    }
                    if (token == null || token.trim().isEmpty()) {
                        callback.onError("Authentication token missing");
                        return;
                    }

                    URL url = new URL(IMAGE_BASE_URL + "/image?id=" + imageId);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("Authorization", "Bearer " + token);
                    conn.setConnectTimeout(20000);
                    conn.setReadTimeout(20000);

                    int responseCode = conn.getResponseCode();
                    if (responseCode == 200) {
                        InputStream is = conn.getInputStream();
                        Bitmap bitmap = BitmapFactory.decodeStream(is);
                        is.close();
                        conn.disconnect();
                        if (bitmap != null) {
                            callback.onSuccess(bitmap);
                        } else {
                            callback.onError("Failed to decode image bytes");
                        }
                    } else {
                        InputStream errorStream = conn.getErrorStream();
                        String errMsg = "HTTP " + responseCode;
                        if (errorStream != null) {
                            BufferedReader r = new BufferedReader(new InputStreamReader(errorStream));
                            errMsg = r.readLine();
                            errorStream.close();
                        }
                        conn.disconnect();
                        callback.onError("Image load failed: " + errMsg);
                    }
                } catch (Exception e) {
                    callback.onError("Image fetch error: " + e.getMessage());
                }
            }
        }).start();
    }

    public static void deleteImage(final String imageId, final String token, final DeleteCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (imageId == null || imageId.trim().isEmpty()) {
                        // Nothing on worker to delete, treat as success
                        callback.onSuccess();
                        return;
                    }
                    if (token == null || token.trim().isEmpty()) {
                        callback.onError("Token required for image deletion");
                        return;
                    }

                    URL url = new URL(IMAGE_BASE_URL + "/image?id=" + imageId);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("DELETE");
                    conn.setRequestProperty("Authorization", "Bearer " + token);
                    conn.setConnectTimeout(20000);
                    conn.setReadTimeout(20000);

                    int responseCode = conn.getResponseCode();
                    InputStream is = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream();
                    String responseBody = "";
                    if (is != null) {
                        BufferedReader r = new BufferedReader(new InputStreamReader(is));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = r.readLine()) != null) {
                            sb.append(line);
                        }
                        r.close();
                        responseBody = sb.toString();
                    }
                    conn.disconnect();

                    if (responseCode >= 200 && responseCode < 300) {
                        callback.onSuccess();
                    } else {
                        String err = "Delete failed: " + responseCode;
                        try {
                            JSONObject json = new JSONObject(responseBody);
                            err = json.optString("error", json.optString("message", err));
                        } catch (Exception ignored) {
                        }
                        callback.onError(err);
                    }
                } catch (Exception e) {
                    callback.onError("Delete image error: " + e.getMessage());
                }
            }
        }).start();
    }
}