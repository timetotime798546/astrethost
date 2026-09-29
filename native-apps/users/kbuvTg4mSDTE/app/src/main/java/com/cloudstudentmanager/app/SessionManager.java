package com.cloudstudentmanager.app;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF_NAME = "CloudStudentSession";
    private static final String KEY_TOKEN = "auth_token";
    private static final String KEY_EMAIL = "user_email";
    
    private SharedPreferences pref;
    private SharedPreferences.Editor editor;
    private Context context;

    public SessionManager(Context context) {
        this.context = context;
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    public void createSession(String token, String email) {
        editor.putString(KEY_TOKEN, token);
        editor.putString(KEY_EMAIL, email);
        editor.apply();
    }

    public boolean isLoggedIn() {
        return pref.getString(KEY_TOKEN, null) != null;
    }

    public String getToken() {
        return pref.getString(KEY_TOKEN, null);
    }

    public String getEmail() {
        return pref.getString(KEY_EMAIL, "User");
    }

    public void logout() {
        editor.clear();
        editor.apply();
    }
}