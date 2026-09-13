package com.edgeborderlight.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.WindowManager;

public class BorderLightService extends Service {

    public static boolean isRunning = false;
    private static final String CHANNEL_ID = "EdgeBorderLightChannel";
    private static final int NOTIFICATION_ID = 888;

    private WindowManager windowManager;
    private BorderLightView borderLightView;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        isRunning = true;
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, buildForegroundNotification());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int width = 12;
        int speed = 5;
        int radius = 45;
        int style = 0;

        if (intent != null) {
            width = intent.getIntExtra("width", 12);
            speed = intent.getIntExtra("speed", 5);
            radius = intent.getIntExtra("radius", 45);
            style = intent.getIntExtra("style", 0);
        }

        if (borderLightView == null) {
            borderLightView = new BorderLightView(this);
            windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

            int typeLayout;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                typeLayout = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
            } else {
                typeLayout = WindowManager.LayoutParams.TYPE_PHONE;
            }

            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    typeLayout,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT
            );
            params.gravity = Gravity.CENTER;

            try {
                windowManager.addView(borderLightView, params);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (borderLightView != null) {
            borderLightView.updateParams(width, speed, radius, style);
        }

        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Edge Border Light Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            serviceChannel.setDescription("Controls overlay rendering of live screen border lighting");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private Notification buildForegroundNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 
                0, 
                notificationIntent, 
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }

        return builder
                .setContentTitle("Edge Border Light is Active")
                .setContentText("Tap to change animation speed, colors, and border widths.")
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;
        if (windowManager != null && borderLightView != null) {
            try {
                windowManager.removeView(borderLightView);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}