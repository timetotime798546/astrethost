package com.instafeedapp.app;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class ImageLoader {
    private static ImageLoader instance;
    private final LruCache<String, Bitmap> memoryCache;
    private final Handler mainHandler;

    private ImageLoader() {
        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        int cacheSize = maxMemory / 8;
        memoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getByteCount() / 1024;
            }
        };
        mainHandler = new Handler(Looper.getMainLooper());
    }

    public static synchronized ImageLoader getInstance() {
        if (instance == null) {
            instance = new ImageLoader();
        }
        return instance;
    }

    public void loadImage(final String urlString, final ImageView imageView) {
        if (urlString == null || urlString.trim().isEmpty()) {
            return;
        }

        imageView.setTag(urlString);

        Bitmap cachedBitmap = memoryCache.get(urlString);
        if (cachedBitmap != null) {
            imageView.setImageBitmap(cachedBitmap);
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                final Bitmap bitmap = downloadBitmap(urlString);
                if (bitmap != null) {
                    memoryCache.put(urlString, bitmap);
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (imageView.getTag() != null && imageView.getTag().equals(urlString)) {
                                imageView.setImageBitmap(bitmap);
                            }
                        }
                    });
                }
            }
        }).start();
    }

    private Bitmap downloadBitmap(String urlString) {
        HttpURLConnection connection = null;
        InputStream input = null;
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(8000);
            connection.connect();
            input = connection.getInputStream();
            return BitmapFactory.decodeStream(input);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                if (input != null) input.close();
                if (connection != null) connection.disconnect();
            } catch (Exception ignored) {}
        }
    }
}