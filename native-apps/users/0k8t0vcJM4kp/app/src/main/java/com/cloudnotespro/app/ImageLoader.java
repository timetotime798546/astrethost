package com.cloudnotespro.app;

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
    private static LruCache<String, Bitmap> memoryCache;
    private static ExecutorService executorService;
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    static {
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        final int cacheSize = maxMemory / 8;

        memoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getByteCount() / 1024;
            }
        };
        executorService = Executors.newFixedThreadPool(4);
    }

    public static void loadImage(final String urlString, final ImageView imageView) {
        if (urlString == null || urlString.isEmpty()) {
            return;
        }

        imageView.setTag(urlString);

        Bitmap cachedBitmap = memoryCache.get(urlString);
        if (cachedBitmap != null) {
            imageView.setImageBitmap(cachedBitmap);
            return;
        }

        executorService.submit(new Runnable() {
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
        });
    }

    private static Bitmap downloadBitmap(String urlString) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.connect();
            InputStream is = conn.getInputStream();
            return BitmapFactory.decodeStream(is);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}