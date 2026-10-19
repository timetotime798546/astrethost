package com.trirunner.app;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF_NAME = "trirunner_session";
    private static final String KEY_TOKEN = "auth_token";
    private static final String KEY_EMAIL = "auth_email";
    private static final String KEY_PROFILE_RECORD_ID = "profile_record_id";
    private static final String KEY_SCORE = "player_score";
    private static final String KEY_CRASHES = "player_crashes";
    private static final String KEY_HIGH_SCORE = "player_high_score";

    private final SharedPreferences pref;

    public SessionManager(Context context) {
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveSession(String token, String email) {
        pref.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_EMAIL, email)
            .apply();
    }

    public boolean isLoggedIn() {
        return getToken() != null && !getToken().isEmpty();
    }

    public String getToken() {
        return pref.getString(KEY_TOKEN, null);
    }

    public String getEmail() {
        return pref.getString(KEY_EMAIL, "");
    }

    public void setProfileRecordId(String id) {
        pref.edit().putString(KEY_PROFILE_RECORD_ID, id).apply();
    }

    public String getProfileRecordId() {
        return pref.getString(KEY_PROFILE_RECORD_ID, null);
    }

    public void updateLocalStats(int score, int crashes, int highScore) {
        pref.edit()
            .putInt(KEY_SCORE, score)
            .putInt(KEY_CRASHES, crashes)
            .putInt(KEY_HIGH_SCORE, highScore)
            .apply();
    }

    public int getScore() {
        return pref.getInt(KEY_SCORE, 100);
    }

    public int getCrashes() {
        return pref.getInt(KEY_CRASHES, 0);
    }

    public int getHighScore() {
        return pref.getInt(KEY_HIGH_SCORE, 0);
    }

    public void clear() {
        pref.edit().clear().apply();
    }
}