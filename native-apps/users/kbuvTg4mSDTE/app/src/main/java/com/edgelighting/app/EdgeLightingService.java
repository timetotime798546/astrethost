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

import androidx.annotation.Nullable;

public class EdgeLightingService extends Service {

    private WindowManager windowManager;
    private EdgeLightingView edgeLightingView;
    private static final String CHANNEL_ID = "EdgeLightingServiceChannel";
    private static final int NOTIFICATION_ID = 1;
    private static boolean serviceRunning = false;

    public static boolean isRunning() {
        return serviceRunning;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        serviceRunning = true;
        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

        // Create notification channel for Android O and above
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

        Notification notification = new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Edge Lighting")
                .setContentText("Edge lighting is active.")
                .setSmallIcon(R.drawable.icon)
                .build();

        startForeground(NOTIFICATION_ID, notification);

        edgeLightingView = new EdgeLightingView(this);

        WindowManager.LayoutParams params;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, // Allows drawing outside of screen bounds slightly if needed
                    PixelFormat.TRANSLUCENT);
        } else {
            params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_PHONE, // TYPE_PHONE for older APIs
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT);
        }
        
        // Add the view to the window manager
        try {
            if (windowManager != null) {
                windowManager.addView(edgeLightingView, params);
                edgeLightingView.startAnimation();
            }
        } catch (WindowManager.BadTokenException e) {
            // Handle error, e.g., permission not granted, service trying to add view multiple times
            // This usually means the SYSTEM_ALERT_WINDOW permission was not granted.
            stopSelf(); // Stop the service if we can't add the view
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY; // Service will restart if killed by system
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        serviceRunning = false;
        if (edgeLightingView != null && windowManager != null) {
            edgeLightingView.stopAnimation();
            windowManager.removeView(edgeLightingView);
        }
        stopForeground(true);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private class EdgeLightingView extends View {

        private Paint paint;
        private Path path;
        private int screenWidth, screenHeight;
        private float cornerRadius = 60f; // Radius for rounded corners
        private float strokeWidth = 15f; // Width of the lighting effect
        private Handler handler;
        private Runnable animationRunnable;
        private int currentHue = 0; // For HSV color cycling
        private final int ANIMATION_DELAY = 30; // Milliseconds between frames

        public EdgeLightingView(Context context) {
            super(context);
            paint = new Paint();
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(strokeWidth);
            paint.setAntiAlias(true);
            
            path = new Path();
            
            // Get screen dimensions
            DisplayMetrics displayMetrics = new DisplayMetrics();
            if (windowManager != null) {
                windowManager.getDefaultDisplay().getMetrics(displayMetrics);
                screenWidth = displayMetrics.widthPixels;
                screenHeight = displayMetrics.heightPixels;
            }

            handler = new Handler();
            animationRunnable = new Runnable() {
                @Override
                public void run() {
                    currentHue = (currentHue + 2) % 360; // Cycle hue from 0 to 359
                    float[] hsv = {currentHue, 1f, 1f}; // Full saturation and value
                    paint.setColor(Color.HSVToColor(hsv));
                    invalidate(); // Redraw the view
                    handler.postDelayed(this, ANIMATION_DELAY); // Schedule next frame
                }
            };
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            
            path.reset();
            
            // Define the rectangle for the edge effect, slightly inset
            float inset = strokeWidth / 2 + 5; // A small buffer from the actual screen edge
            RectF rect = new RectF(inset, inset, screenWidth - inset, screenHeight - inset);
            
            path.addRoundRect(rect, cornerRadius, cornerRadius, Path.Direction.CW);
            
            canvas.drawPath(path, paint);
        }

        public void startAnimation() {
            handler.removeCallbacks(animationRunnable); // Ensure no duplicate animations
            handler.post(animationRunnable);
        }

        public void stopAnimation() {
            handler.removeCallbacks(animationRunnable);
        }
    }
}