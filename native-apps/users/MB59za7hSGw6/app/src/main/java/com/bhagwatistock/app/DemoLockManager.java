package com.bhagwatistock.app;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.Button;

public class DemoLockManager {

    private static final String PREF_NAME = "demo_lock_preferences";
    private static final String KEY_DEMO_START = "demo_start_timestamp";
    private static final long FIFTEEN_MINUTES_MS = 15 * 60 * 1000L; // 15 Minutes

    private static Dialog lockDialog = null;
    private static Handler checkHandler = null;
    private static Runnable checkRunnable = null;

    public static void initDemoTime(Context context) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        if (!prefs.contains(KEY_DEMO_START)) {
            prefs.edit().putLong(KEY_DEMO_START, System.currentTimeMillis()).apply();
        }
    }

    public static boolean isDemoExpired(Context context) {
        if (context == null) return false;
        initDemoTime(context);
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        long startTime = prefs.getLong(KEY_DEMO_START, System.currentTimeMillis());
        long elapsed = System.currentTimeMillis() - startTime;
        return elapsed >= FIFTEEN_MINUTES_MS;
    }

    public static void checkAndShowLock(final Activity activity) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        initDemoTime(activity);

        if (isDemoExpired(activity)) {
            showNonClosableOverlay(activity);
        } else {
            startPeriodicTimerCheck(activity);
        }
    }

    public static void startPeriodicTimerCheck(final Activity activity) {
        stopPeriodicTimerCheck();
        checkHandler = new Handler(Looper.getMainLooper());
        checkRunnable = new Runnable() {
            @Override
            public void run() {
                if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
                    return;
                }
                if (isDemoExpired(activity)) {
                    showNonClosableOverlay(activity);
                } else {
                    if (checkHandler != null) {
                        checkHandler.postDelayed(this, 3000); // Check every 3 seconds
                    }
                }
            }
        };
        checkHandler.postDelayed(checkRunnable, 3000);
    }

    public static void stopPeriodicTimerCheck() {
        if (checkHandler != null && checkRunnable != null) {
            checkHandler.removeCallbacks(checkRunnable);
            checkHandler = null;
            checkRunnable = null;
        }
    }

    private static void showNonClosableOverlay(final Activity activity) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        if (lockDialog != null && lockDialog.isShowing()) {
            return;
        }

        try {
            lockDialog = new Dialog(activity, android.R.style.Theme_Material_Light_NoActionBar_Fullscreen);
            lockDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            lockDialog.setContentView(R.layout.dialog_demo_expired);
            lockDialog.setCancelable(false);
            lockDialog.setCanceledOnTouchOutside(false);

            // Intercept Back Button so overlay cannot be closed
            lockDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
                @Override
                public boolean onKey(DialogInterface dialogInterface, int keyCode, KeyEvent keyEvent) {
                    return keyCode == KeyEvent.KEYCODE_BACK;
                }
            });

            Button btnWhatsapp = lockDialog.findViewById(R.id.btnWhatsappContact);
            if (btnWhatsapp != null) {
                btnWhatsapp.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        openWhatsAppChat(activity);
                    }
                });
            }

            lockDialog.show();
        } catch (Exception ignored) {}
    }

    private static void openWhatsAppChat(Activity activity) {
        String phoneNumber = "918171768360";
        try {
            String url = "https://wa.me/" + phoneNumber;
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            activity.startActivity(intent);
        } catch (Exception e) {
            try {
                String apiUrl = "https://api.whatsapp.com/send?phone=" + phoneNumber;
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(apiUrl));
                activity.startActivity(intent);
            } catch (Exception ignored) {}
        }
    }
}