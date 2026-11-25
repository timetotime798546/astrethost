package com.kkgstore.store;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class DemoLockManager {

    private static final String PREF_NAME = "kkg_store_demo_lock";
    private static final String KEY_INSTALL_TIME = "install_timestamp";
    private static final long FIFTEEN_MINUTES_MS = 15 * 60 * 1000L; // 15 Minutes

    public static void initInstallTimestamp(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        if (!prefs.contains(KEY_INSTALL_TIME)) {
            prefs.edit().putLong(KEY_INSTALL_TIME, System.currentTimeMillis()).apply();
        }
    }

    public static boolean isDemoExpired(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        long installTime = prefs.getLong(KEY_INSTALL_TIME, 0);
        if (installTime == 0) {
            initInstallTimestamp(context);
            return false;
        }
        long elapsedTime = System.currentTimeMillis() - installTime;
        return elapsedTime >= FIFTEEN_MINUTES_MS;
    }

    public static boolean checkAndEnforceLock(Activity activity) {
        initInstallTimestamp(activity);
        if (isDemoExpired(activity)) {
            if (!(activity instanceof DemoLockActivity)) {
                Intent intent = new Intent(activity, DemoLockActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                activity.startActivity(intent);
                activity.finish();
            }
            return true;
        }
        return false;
    }
}