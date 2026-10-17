package com.turbo3dracer.app;

import android.content.Context;
import android.content.SharedPreferences;

public class ScoreHelper {
    private static final String PREF_NAME = "turbo_racer_prefs";
    private static final String KEY_HIGH_SCORE = "high_score";
    private static final String KEY_COINS = "total_coins";
    private static final String KEY_DISTANCE = "max_distance";
    private static final String KEY_SELECTED_CAR = "selected_car_idx";
    private static final String KEY_SELECTED_TRACK = "selected_track_idx";

    public static int calculateScore(int distanceMeters, int coinsCollected, float averageSpeedKmh) {
        // Safe, native Java offline score formula
        float speedMultiplier = Math.max(1.0f, averageSpeedKmh / 80.0f);
        int distScore = (int) (distanceMeters * speedMultiplier);
        int coinScore = coinsCollected * 50;
        return distScore + coinScore;
    }

    public static int getHighScore(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sp.getInt(KEY_HIGH_SCORE, 0);
    }

    public static boolean saveScoreIfRecord(Context context, int newScore) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        int current = sp.getInt(KEY_HIGH_SCORE, 0);
        if (newScore > current) {
            sp.edit().putInt(KEY_HIGH_SCORE, newScore).apply();
            return true;
        }
        return false;
    }

    public static int getCoins(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sp.getInt(KEY_COINS, 250); // Starter credits
    }

    public static void addCoins(Context context, int amount) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        int current = sp.getInt(KEY_COINS, 250);
        sp.edit().putInt(KEY_COINS, Math.max(0, current + amount)).apply();
    }

    public static int getSelectedCarIndex(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sp.getInt(KEY_SELECTED_CAR, 0);
    }

    public static void setSelectedCarIndex(Context context, int idx) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sp.edit().putInt(KEY_SELECTED_CAR, idx).apply();
    }

    public static int getSelectedTrack(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sp.getInt(KEY_SELECTED_TRACK, 0);
    }

    public static void setSelectedTrack(Context context, int trackIdx) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sp.edit().putInt(KEY_SELECTED_TRACK, trackIdx).apply();
    }
}