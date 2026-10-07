package com.shozicapremor.app;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class ImageLoader {

    private static final Map<String, Bitmap> memoryCache = new HashMap<>();
    private static final Handler uiHandler = new Handler(Looper.getMainLooper());

    public static void displayImage(final String imageUrl, final ImageView imageView) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            return;
        }

        if (memoryCache.containsKey(imageUrl)) {
            imageView.setImageBitmap(memoryCache.get(imageUrl));
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(imageUrl);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setDoInput(true);
                    conn.connect();
                    InputStream input = conn.getInputStream();
                    final Bitmap bitmap = BitmapFactory.decodeStream(input);
                    input.close();

                    if (bitmap != null) {
                        memoryCache.put(imageUrl, bitmap);
                        uiHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                imageView.setImageBitmap(bitmap);
                            }
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}