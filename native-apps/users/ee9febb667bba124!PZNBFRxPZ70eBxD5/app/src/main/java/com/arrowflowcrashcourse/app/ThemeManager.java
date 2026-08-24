package com.arrowflowcrashcourse.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

public class ThemeManager {
    private static final String PREFS_NAME = "ArrowFlowThemePrefs";
    private static final String KEY_DARK_MODE = "darkMode";

    public static boolean isDarkMode(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_DARK_MODE, true);
    }

    public static void setDarkMode(Context context, boolean dark) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_DARK_MODE, dark).apply();
    }

    public static int getBackgroundColor(boolean isDark) {
        return isDark ? Color.parseColor("#090D16") : Color.parseColor("#F5F7FB");
    }

    public static int getGridColor(boolean isDark) {
        return isDark ? Color.parseColor("#1B2336") : Color.parseColor("#E2E8F0");
    }

    public static int getPrimaryAccent(boolean isDark) {
        return isDark ? Color.parseColor("#38BDF8") : Color.parseColor("#1E3A8A");
    }

    public static int getSecondaryAccent(boolean isDark) {
        return isDark ? Color.parseColor("#FB923C") : Color.parseColor("#EA580C");
    }

    public static int getArrowColor(boolean isDark) {
        return isDark ? Color.parseColor("#60A5FA") : Color.parseColor("#2563EB");
    }

    public static int getTextColor(boolean isDark) {
        return isDark ? Color.parseColor("#F1F5F9") : Color.parseColor("#0F172A");
    }

    public static int getCardBgColor(boolean isDark) {
        return isDark ? Color.parseColor("#111827") : Color.parseColor("#FFFFFF");
    }
}