package com.studymasterplanner.app.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF_NAME = "StudyMasterSession";
    private static final String KEY_TOKEN = "auth_token";
    private static final String KEY_EMAIL = "user_email";
    private static final String KEY_OFFLINE_CACHE = "cached_tasks_json";

    private SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveAuthToken(String token, String email) {
        prefs.edit().putString(KEY_TOKEN, token).putString(KEY_EMAIL, email).apply();
    }

    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    public String getEmail() {
        return prefs.getString(KEY_EMAIL, "");
    }

    public boolean isLoggedIn() {
        return getToken() != null && !getToken().isEmpty();
    }

    public void logout() {
        prefs.edit().remove(KEY_TOKEN).remove(KEY_EMAIL).apply();
    }

    public void saveLocalTasksCache(String json) {
        prefs.edit().putString(KEY_OFFLINE_CACHE, json).apply();
    }

    public String getLocalTasksCache() {
        return prefs.getString(KEY_OFFLINE_CACHE, "[]");
    }
}