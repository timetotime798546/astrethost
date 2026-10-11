package com.patiententrymanager.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class ImageWorkerApi {

    private static final String TAG = "ImageWorkerApi";
    private static final String WORKER_URL = "https://trumpledroid-image.cloudbeta28624.workers.dev/";
    private final Context context;
    private final BackendApi backendApi;
    private final Handler mainHandler;

    public interface UploadCallback {
        void onSuccess(String imageId);
        void onError(String errorMessage);
    }

    public interface ImageFetchCallback {
        void onSuccess(Bitmap bitmap);
        void onError(String errorMessage);
    }

    public interface SimpleCallback {
        void onSuccess();
        void onError(String errorMessage);
    }

    public ImageWorkerApi(Context context, BackendApi backendApi) {
        this.context = context.getApplicationContext();
        this.backendApi = backendApi;
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void uploadImage(final Bitmap sourceBitmap, final UploadCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    byte[] imageBytes = compressBitmapToUnder200KB(sourceBitmap);

                    String lineEnd = "\r\n";
                    String twoHyphens = "--";
                    String boundary = "*****" + System.currentTimeMillis() + "*****";

                    URL url = new URL(WORKER_URL + "upload");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setDoInput(true);
                    conn.setDoOutput(true);
                    conn.setUseCaches(false);
                    conn.setRequestMethod("POST");
                    conn.setConnectTimeout(20000);
                    conn.setReadTimeout(20000);

                    String token = backendApi.getAuthToken();
                    if (token != null && !token.isEmpty()) {
                        conn.setRequestProperty("Authorization", "Bearer " + token);
                    }

                    conn.setRequestProperty("Connection", "Keep-Alive");
                    conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                    DataOutputStream dos = new DataOutputStream(conn.getOutputStream());

                    // Field: app_id
                    dos.writeBytes(twoHyphens + boundary + lineEnd);
                    dos.writeBytes("Content-Disposition: form-data; name=\"app_id\"" + lineEnd);
                    dos.writeBytes(lineEnd);
                    dos.writeBytes(backendApi.getAppId() + lineEnd);

                    // Field: image
                    dos.writeBytes(twoHyphens + boundary + lineEnd);
                    dos.writeBytes("Content-Disposition: form-data; name=\"image\"; filename=\"patient_photo.jpg\"" + lineEnd);
                    dos.writeBytes("Content-Type: image/jpeg" + lineEnd);
                    dos.writeBytes(lineEnd);

                    dos.write(imageBytes);
                    dos.writeBytes(lineEnd);

                    dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
                    dos.flush();
                    dos.close();

                    int responseCode = conn.getResponseCode();
                    InputStream is = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream();

                    BufferedReader br = new BufferedReader(new InputStreamReader(is, "utf-8"));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                    }
                    br.close();

                    String responseStr = sb.toString();
                    JSONObject json = new JSONObject(responseStr);

                    if (responseCode >= 200 && responseCode < 300 && (json.optBoolean("success", true))) {
                        String imageId = json.optString("id", null);
                        if (imageId == null || imageId.isEmpty()) {
                            if (json.has("image") && json.getJSONObject("image").has("id")) {
                                imageId = json.getJSONObject("image").getString("id");
                            } else if (json.has("data") && json.getJSONObject("data").has("id")) {
                                imageId = json.getJSONObject("data").getString("id");
                            }
                        }

                        if (imageId != null && !imageId.isEmpty()) {
                            final String finalId = imageId;
                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    callback.onSuccess(finalId);
                                }
                            });
                        } else {
                            postUploadError(callback, "Failed to parse image ID from upload response");
                        }
                    } else {
                        String err = json.optString("error", json.optString("message", "Image upload failed"));
                        postUploadError(callback, err);
                    }

                } catch (Exception e) {
                    Log.e(TAG, "Image upload error", e);
                    postUploadError(callback, e.getMessage());
                }
            }
        }).start();
    }

    public void fetchImage(final String imageId, final ImageFetchCallback callback) {
        if (imageId == null || imageId.trim().isEmpty()) {
            postFetchError(callback, "Invalid Image ID");
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(WORKER_URL + "image?id=" + imageId);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(15000);
                    conn.setReadTimeout(15000);

                    String token = backendApi.getAuthToken();
                    if (token != null && !token.isEmpty()) {
                        conn.setRequestProperty("Authorization", "Bearer " + token);
                    }

                    int responseCode = conn.getResponseCode();
                    if (responseCode == 200) {
                        InputStream is = conn.getInputStream();
                        final Bitmap bitmap = BitmapFactory.decodeStream(is);
                        is.close();

                        if (bitmap != null) {
                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    callback.onSuccess(bitmap);
                                }
                            });
                        } else {
                            postFetchError(callback, "Could not decode image data");
                        }
                    } else {
                        postFetchError(callback, "Failed to fetch image: HTTP " + responseCode);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Fetch image error", e);
                    postFetchError(callback, e.getMessage());
                }
            }
        }).start();
    }

    public void deleteImage(final String imageId, final SimpleCallback callback) {
        if (imageId == null || imageId.trim().isEmpty()) {
            if (callback != null) callback.onSuccess();
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(WORKER_URL + "image?id=" + imageId);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("DELETE");
                    conn.setConnectTimeout(15000);
                    conn.setReadTimeout(15000);

                    String token = backendApi.getAuthToken();
                    if (token != null && !token.isEmpty()) {
                        conn.setRequestProperty("Authorization", "Bearer " + token);
                    }

                    int responseCode = conn.getResponseCode();
                    if (responseCode >= 200 && responseCode < 300) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (callback != null) callback.onSuccess();
                            }
                        });
                    } else {
                        postSimpleError(callback, "Delete failed HTTP " + responseCode);
                    }
                } catch (Exception e) {
                    postSimpleError(callback, e.getMessage());
                }
            }
        }).start();
    }

    private byte[] compressBitmapToUnder200KB(Bitmap bitmap) {
        int quality = 90;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);

        while (baos.toByteArray().length > 200 * 1024 && quality > 10) {
            baos.reset();
            quality -= 10;
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
        }

        if (baos.toByteArray().length > 200 * 1024) {
            Bitmap scaled = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth() / 2, bitmap.getHeight() / 2, true);
            baos.reset();
            scaled.compress(Bitmap.CompressFormat.JPEG, 70, baos);
        }

        return baos.toByteArray();
    }

    private void postUploadError(final UploadCallback callback, final String msg) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (callback != null) callback.onError(msg != null ? msg : "Upload failed");
            }
        });
    }

    private void postFetchError(final ImageFetchCallback callback, final String msg) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (callback != null) callback.onError(msg != null ? msg : "Image fetch failed");
            }
        });
    }

    private void postSimpleError(final SimpleCallback callback, final String msg) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (callback != null) callback.onError(msg != null ? msg : "Operation failed");
            }
        });
    }
}