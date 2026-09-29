package com.edgelighting.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;

public class EdgeLightingService extends Service {

    private WindowManager windowManager;
    private EdgeLightingView edgeLightingView;
    private static final String CHANNEL_ID = "EdgeLightingServiceChannel";
    private static final int NOTIFICATION_ID = 1;
    private static boolean serviceRunning = false;

    public interface ServiceStateListener {
        void onStateChanged(boolean running);
    }

    private static ServiceStateListener stateListener;

    public static void setListener(ServiceStateListener listener) {
        stateListener = listener;
    }

    public static boolean isRunning() {
        return serviceRunning;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        serviceRunning = true;
        if (stateListener != null) {
            stateListener.onStateChanged(true);
        }
        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

        // Build notification channel for API 26+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Edge Lighting Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }

        Notification notification = builder
                .setContentTitle("Edge Lighting Active")
                .setContentText("Edge lighting is running screen overlay.")
                .setSmallIcon(R.drawable.icon)
                .build();

        // Start Foreground immediately to avoid OS-level service start timeout crashes
        startForeground(NOTIFICATION_ID, notification);

        edgeLightingView = new EdgeLightingView(this);

        WindowManager.LayoutParams params;
        int layoutType;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutType = WindowManager.LayoutParams.TYPE_PHONE;
        }

        // Layout limit flags with FLAG_LAYOUT_IN_SCREEN ensures accurate full-screen drawing coverage
        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        
        try {
            if (windowManager != null) {
                windowManager.addView(edgeLightingView, params);
                edgeLightingView.startAnimation();
            }
        } catch (Exception e) {
            e.printStackTrace();
            stopSelf();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        serviceRunning = false;
        if (stateListener != null) {
            stateListener.onStateChanged(false);
        }
        if (edgeLightingView != null && windowManager != null) {
            edgeLightingView.stopAnimation();
            try {
                windowManager.removeView(edgeLightingView);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        stopForeground(true);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private class EdgeLightingView extends View {

        private Paint paint;
        private Path path;
        private int screenWidth = 1080;
        private int screenHeight = 1920;
        private float cornerRadius = 70f;
        private float strokeWidth = 24f;
        private Handler handler;
        private Runnable animationRunnable;
        private int currentHue = 0;
        private final int ANIMATION_DELAY = 16; // Optimized at ~60fps for seamless transitions

        public EdgeLightingView(Context context) {
            super(context);
            paint = new Paint();
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(strokeWidth);
            paint.setAntiAlias(true);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
            
            path = new Path();
            
            // Collect fallback measurements instantly
            DisplayMetrics displayMetrics = new DisplayMetrics();
            if (windowManager != null) {
                windowManager.getDefaultDisplay().getMetrics(displayMetrics);
                if (displayMetrics.widthPixels > 0) {
                    screenWidth = displayMetrics.widthPixels;
                }
                if (displayMetrics.heightPixels > 0) {
                    screenHeight = displayMetrics.heightPixels;
                }
            }

            handler = new Handler();
            animationRunnable = new Runnable() {
                @Override
                public void run() {
                    currentHue = (currentHue + 3) % 360;
                    float[] hsv = {currentHue, 1f, 1f};
                    paint.setColor(Color.HSVToColor(hsv));
                    invalidate();
                    handler.postDelayed(this, ANIMATION_DELAY);
                }
            };
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            if (w > 0) {
                screenWidth = w;
            }
            if (h > 0) {
                screenHeight = h;
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            
            path.reset();
            float inset = strokeWidth / 2;
            RectF rect = new RectF(inset, inset, screenWidth - inset, screenHeight - inset);
            
            path.addRoundRect(rect, cornerRadius, cornerRadius, Path.Direction.CW);
            canvas.drawPath(path, paint);
        }

        public void startAnimation() {
            handler.removeCallbacks(animationRunnable);
            handler.post(animationRunnable);
        }

        public void stopAnimation() {
            handler.removeCallbacks(animationRunnable);
        }
    }
}