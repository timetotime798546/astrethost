package com.barbercraft.app;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImageLoader {
    private static ImageLoader instance;
    private final LruCache<String, Bitmap> memoryCache;
    private final ExecutorService executor;
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
        executor = Executors.newFixedThreadPool(4);
        mainHandler = new Handler(Looper.getMainLooper());
    }

    public static synchronized ImageLoader getInstance() {
        if (instance == null) {
            instance = new ImageLoader();
        }
        return instance;
    }

    public void displayImage(final String urlString, final ImageView imageView) {
        if (urlString == null || urlString.trim().isEmpty() || imageView == null) {
            return;
        }

        Bitmap cached = memoryCache.get(urlString);
        if (cached != null) {
            imageView.setImageBitmap(cached);
            return;
        }

        imageView.setTag(urlString);
        executor.execute(new Runnable() {
            @Override
            public void run() {
                Bitmap downloadedBitmap = downloadBitmap(urlString);
                if (downloadedBitmap != null) {
                    memoryCache.put(urlString, downloadedBitmap);
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            String currentTag = (String) imageView.getTag();
                            if (currentTag != null && currentTag.equals(urlString)) {
                                imageView.setImageBitmap(downloadedBitmap);
                            }
                        }
                    });
                }
            }
        });
    }

    private Bitmap downloadBitmap(String urlString) {
        HttpURLConnection conn = null;
        InputStream is = null;
        try {
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(12000);
            conn.setDoInput(true);
            conn.connect();
            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                is = conn.getInputStream();
                return BitmapFactory.decodeStream(is);
            }
        } catch (Exception ignored) {
        } finally {
            try {
                if (is != null) is.close();
                if (conn != null) conn.disconnect();
            } catch (Exception ignored) {
            }
        }
        return null;
    }
}