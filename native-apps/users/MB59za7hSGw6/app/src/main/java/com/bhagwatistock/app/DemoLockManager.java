package com.bhagwatistock.app;

import android.app.Activity;
import android.content.Context;

public class DemoLockManager {

    /**
     * Lock system fully disabled.
     * The app functions as a full unlocked version permanently.
     */

    public static void initDemoTime(Context context) {
        // No-op: Lock timer disabled
    }

    public static boolean isDemoExpired(Context context) {
        return false; // Permanently unlocked
    }

    public static void checkAndShowLock(final Activity activity) {
        // No-op: Lock screen dialog disabled
    }

    public static void startPeriodicTimerCheck(final Activity activity) {
        // No-op: Background timer disabled
    }

    public static void stopPeriodicTimerCheck() {
        // No-op
    }
}