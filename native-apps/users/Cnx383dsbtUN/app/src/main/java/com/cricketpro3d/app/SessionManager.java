package com.cricketpro3d.app;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF_NAME = "cricket_pro_prefs";
    private static final String KEY_TOKEN = "jwt_token";
    private static final String KEY_EMAIL = "player_email";
    private static final String KEY_RECORD_ID = "career_record_id";
    private static final String KEY_MATCHES = "stat_matches";
    private static final String KEY_WINS = "stat_wins";
    private static final String KEY_HIGH_SCORE = "stat_high_score";
    private static final String KEY_TOTAL_RUNS = "stat_total_runs";
    private static final String KEY_SIXES = "stat_sixes";
    private static final String KEY_FOURS = "stat_fours";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveSession(String token, String email) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_EMAIL, email)
            .apply();
    }

    public boolean isLoggedIn() {
        String token = getToken();
        return token != null && !token.trim().isEmpty();
    }

    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    public String getEmail() {
        return prefs.getString(KEY_EMAIL, "cricketer@game.com");
    }

    public void setCareerRecordId(String id) {
        prefs.edit().putString(KEY_RECORD_ID, id).apply();
    }

    public String getCareerRecordId() {
        return prefs.getString(KEY_RECORD_ID, null);
    }

    public void updateCareer(int matches, int wins, int highScore, int totalRuns, int sixes, int fours) {
        prefs.edit()
            .putInt(KEY_MATCHES, matches)
            .putInt(KEY_WINS, wins)
            .putInt(KEY_HIGH_SCORE, highScore)
            .putInt(KEY_TOTAL_RUNS, totalRuns)
            .putInt(KEY_SIXES, sixes)
            .putInt(KEY_FOURS, fours)
            .apply();
    }

    public int getMatches() { return prefs.getInt(KEY_MATCHES, 0); }
    public int getWins() { return prefs.getInt(KEY_WINS, 0); }
    public int getHighScore() { return prefs.getInt(KEY_HIGH_SCORE, 0); }
    public int getTotalRuns() { return prefs.getInt(KEY_TOTAL_RUNS, 0); }
    public int getSixes() { return prefs.getInt(KEY_SIXES, 0); }
    public int getFours() { return prefs.getInt(KEY_FOURS, 0); }

    public void clear() {
        prefs.edit().clear().apply();
    }
}