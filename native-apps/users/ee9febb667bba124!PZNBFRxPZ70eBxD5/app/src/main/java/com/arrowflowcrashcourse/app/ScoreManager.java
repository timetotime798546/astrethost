package com.arrowflowcrashcourse.app;

import android.content.Context;
import android.content.SharedPreferences;

public class ScoreManager {
    private static final String PREFS_NAME = "ArrowFlowScorePrefs";
    private static final String KEY_HIGH_SCORE = "highScore";
    private static final String KEY_UNLOCKED_LEVEL = "unlockedLevel";

    private SharedPreferences prefs;

    public ScoreManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public int getHighScore() {
        return prefs.getInt(KEY_HIGH_SCORE, 0);
    }

    public void updateHighScore(int score) {
        int currentHigh = getHighScore();
        if (score > currentHigh) {
            prefs.edit().putInt(KEY_HIGH_SCORE, score).apply();
        }
    }

    public int getUnlockedLevel() {
        return prefs.getInt(KEY_UNLOCKED_LEVEL, 1);
    }

    public void unlockLevel(int level) {
        int currentUnlocked = getUnlockedLevel();
        if (level > currentUnlocked && level <= 20) {
            prefs.edit().putInt(KEY_UNLOCKED_LEVEL, level).apply();
        }
    }

    public void resetAll() {
        prefs.edit().clear().apply();
    }
}