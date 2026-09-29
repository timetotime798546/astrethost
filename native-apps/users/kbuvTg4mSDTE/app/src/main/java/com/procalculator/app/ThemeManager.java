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
            activity.setTheme(android.R.style.Theme.Material);
        } else {
            activity.setTheme(android.R.style.Theme.Material.Light);
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