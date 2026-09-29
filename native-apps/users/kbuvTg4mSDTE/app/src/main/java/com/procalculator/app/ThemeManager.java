package com.procalculator.app;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

public class ThemeManager {
    private static final String PREF_NAME = "calc_prefs";
    private static final String KEY_THEME = "app_theme";

    public static void applyTheme(Activity activity) {
        SharedPreferences prefs = activity.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        boolean isDark = prefs.getBoolean(KEY_THEME, false);
        if (isDark) {
            // FIXED: Using underscore notation for Android framework style resources
            activity.setTheme(android.R.style.Theme_Material);
        } else {
            // FIXED: Using underscore notation for Android framework style resources
            activity.setTheme(android.R.style.Theme_Material_Light);
        }
    }

    public static void toggleTheme(Activity activity) {
        SharedPreferences prefs = activity.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        boolean current = prefs.getBoolean(KEY_THEME, false);
        prefs.edit().putBoolean(KEY_THEME, !current).apply();
        activity.recreate();
    }

    public static boolean isDarkMode(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getBoolean(KEY_THEME, false);
    }
}