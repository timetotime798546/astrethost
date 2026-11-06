package com.instagramhome.app;

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
    private LruCache<String, Bitmap> memoryCache;
    private ExecutorService executorService;
    private Handler mainHandler;

    private ImageLoader() {
        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        int cacheSize = maxMemory / 8;
        if (cacheSize <= 0) {
            cacheSize = 1024 * 8;
        }
        memoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getByteCount() / 1024;
            }
        };
        executorService = Executors.newFixedThreadPool(4);
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

        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(urlString);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setDoInput(true);
                    conn.setConnectTimeout(8000);
                    conn.setReadTimeout(8000);
                    conn.connect();

                    InputStream input = conn.getInputStream();
                    final Bitmap bitmap = BitmapFactory.decodeStream(input);
                    input.close();

                    if (bitmap != null) {
                        memoryCache.put(urlString, bitmap);
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (urlString.equals(imageView.getTag())) {
                                    imageView.setImageBitmap(bitmap);
                                }
                            }
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}