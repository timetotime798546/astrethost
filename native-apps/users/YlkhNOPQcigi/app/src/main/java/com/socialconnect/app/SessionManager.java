package com.socialconnect.app;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF_NAME = "SocialConnectSession";
    private static final String KEY_TOKEN = "auth_token";
    private static final String KEY_EMAIL = "user_email";
    private static final String KEY_NAME = "user_name";

    private SharedPreferences pref;
    private SharedPreferences.Editor editor;
    private Context context;

    public SessionManager(Context context) {
        this.context = context;
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    public void saveAuthToken(String token) {
        editor.putString(KEY_TOKEN, token);
        editor.apply();
    }

    public String getAuthToken() {
        return pref.getString(KEY_TOKEN, "");
    }

    public void saveUserDetail(String email, String name) {
        editor.putString(KEY_EMAIL, email);
        editor.putString(KEY_NAME, name);
        editor.apply();
    }

    public String getUserEmail() {
        return pref.getString(KEY_EMAIL, "");
    }

    public String getUserName() {
        String email = getUserEmail();
        String storedName = pref.getString(KEY_NAME, "");
        if (storedName != null && !storedName.trim().isEmpty()) {
            return storedName;
        }
        if (email.contains("@")) {
            return email.substring(0, email.indexOf("@"));
        }
        return "User";
    }

    public boolean isLoggedIn() {
        String token = getAuthToken();
        return token != null && !token.trim().isEmpty();
    }

    public void clearSession() {
        editor.clear();
        editor.apply();
    }
}