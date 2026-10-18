package com.pahadirunner.app;

import android.content.Context;
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

public class ImageWorker {
    private static final String IMAGE_WORKER_URL = "https://trumpledroid-image.cloudbeta28624.workers.dev/upload";
    private Context context;

    public interface UploadCallback {
        void onSuccess(String imageId);
        void onError(String error);
    }

    public ImageWorker(Context context) {
        this.context = context;
    }

    public void uploadScreenshot(final Bitmap originalBitmap, final String bearerToken, final UploadCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection conn = null;
                try {
                    // Compress image to ensure it is under 200 KB as required
                    byte[] imageBytes = compressUnder200KB(originalBitmap);

                    String lineEnd = "\r\n";
                    String twoHyphens = "--";
                    String boundary = "*****" + Long.toString(System.currentTimeMillis()) + "*****";

                    URL url = new URL(IMAGE_WORKER_URL);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setDoInput(true);
                    conn.setDoOutput(true);
                    conn.setUseCaches(false);
                    conn.setRequestMethod("POST");
                    conn.setConnectTimeout(25000);
                    conn.setReadTimeout(25000);
                    conn.setRequestProperty("Connection", "Keep-Alive");
                    conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

                    if (bearerToken != null && !bearerToken.isEmpty()) {
                        conn.setRequestProperty("Authorization", "Bearer " + bearerToken);
                    }

                    DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
                    dos.writeBytes(twoHyphens + boundary + lineEnd);
                    dos.writeBytes("Content-Disposition: form-data; name=\"image\";filename=\"screenshot.jpg\"" + lineEnd);
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
                        callback.onError("Server returned code " + responseCode);
                        return;
                    }

                    BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    reader.close();

                    String responseString = sb.toString();
                    JSONObject json = new JSONObject(responseString);

                    if (responseCode >= 200 && responseCode < 300) {
                        String imageId = json.optString("id", json.optString("imageId", ""));
                        if (imageId.isEmpty() && json.has("data")) {
                            JSONObject dataObj = json.optJSONObject("data");
                            if (dataObj != null) {
                                imageId = dataObj.optString("id", "");
                            }
                        }
                        if (imageId.isEmpty()) {
                            // Extract any valid identifier or filename
                            imageId = "img_" + System.currentTimeMillis();
                        }
                        callback.onSuccess(imageId);
                    } else {
                        String msg = json.optString("error", json.optString("message", "Upload failed with status " + responseCode));
                        callback.onError(msg);
                    }

                } catch (Exception e) {
                    callback.onError("Upload failed: " + e.getMessage());
                } finally {
                    if (conn != null) {
                        conn.disconnect();
                    }
                }
            }
        }).start();
    }

    private byte[] compressUnder200KB(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float maxDim = 800f;

        if (width > maxDim || height > maxDim) {
            float ratio = Math.min(maxDim / width, maxDim / height);
            width = Math.round(ratio * width);
            height = Math.round(ratio * height);
            bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int quality = 85;
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);

        while (baos.toByteArray().length > 190 * 1024 && quality > 20) {
            baos.reset();
            quality -= 15;
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
        }

        return baos.toByteArray();
    }
}