package com.payquick.app;

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
    private static ImageLoader sInstance;
    private final LruCache<String, Bitmap> mMemoryCache;
    private final Handler mMainHandler;

    private ImageLoader() {
        mMainHandler = new Handler(Looper.getMainLooper());
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        final int cacheSize = maxMemory / 8;
        mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getByteCount() / 1024;
            }
        };
    }

    public static synchronized ImageLoader getInstance() {
        if (sInstance == null) {
            sInstance = new ImageLoader();
        }
        return sInstance;
    }

    public void displayImage(final String imageUrl, final ImageView imageView) {
        if (imageUrl == null || imageUrl.trim().isEmpty() || imageView == null) {
            return;
        }

        final Bitmap cached = mMemoryCache.get(imageUrl);
        if (cached != null) {
            imageView.setImageBitmap(cached);
            return;
        }

        imageView.setTag(imageUrl);

        new Thread(new Runnable() {
            @Override
            public void run() {
                Bitmap bitmap = null;
                HttpURLConnection conn = null;
                InputStream is = null;
                try {
                    URL url = new URL(imageUrl);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(15000);
                    conn.setDoInput(true);
                    conn.connect();

                    if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        is = conn.getInputStream();
                        bitmap = BitmapFactory.decodeStream(is);
                    }
                } catch (Exception ignored) {
                } finally {
                    try {
                        if (is != null) is.close();
                    } catch (Exception ignored) {}
                    if (conn != null) conn.disconnect();
                }

                final Bitmap finalBitmap = bitmap;
                mMainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (finalBitmap != null) {
                            mMemoryCache.put(imageUrl, finalBitmap);
                            if (imageUrl.equals(imageView.getTag())) {
                                imageView.setImageBitmap(finalBitmap);
                            }
                        }
                    }
                });
            }
        }).start();
    }
}