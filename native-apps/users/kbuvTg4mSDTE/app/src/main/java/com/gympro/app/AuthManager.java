package com.gympro.app;

import android.content.Context;
import android.content.SharedPreferences;

public class AuthManager {
    private static final String PREF_NAME = "GymProAuth";
    private static final String KEY_TOKEN = "auth_token";
    private static final String KEY_USER_EMAIL = "user_email";

    private SharedPreferences prefs;

    public AuthManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveSession(String token, String email) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_USER_EMAIL, email)
            .apply();
    }

    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    public String getUserEmail() {
        return prefs.getString(KEY_USER_EMAIL, "");
    }

    public boolean isLoggedIn() {
        return getToken() != null && !getToken().isEmpty();
    }

    public void clearSession() {
        prefs.edit().clear().apply();
    }
}