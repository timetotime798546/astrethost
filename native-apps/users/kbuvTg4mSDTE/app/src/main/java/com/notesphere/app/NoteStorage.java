package com.notesphere.app;

import android.content.Context;
import android.content.SharedPreferences;

public class NoteStorage {
    private static final String PREF_NAME = "notesphere_pref";
    private static final String KEY_TOKEN = "auth_token";
    private static final String KEY_EMAIL = "user_email";

    public static void saveToken(Context context, String token) {
        SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        pref.edit().putString(KEY_TOKEN, token).apply();
    }

    public static String getToken(Context context) {
        SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return pref.getString(KEY_TOKEN, null);
    }

    public static void saveEmail(Context context, String email) {
        SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        pref.edit().putString(KEY_EMAIL, email).apply();
    }

    public static String getEmail(Context context) {
        SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return pref.getString(KEY_EMAIL, null);
    }

    public static void clearSession(Context context) {
        SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        pref.edit().remove(KEY_TOKEN).remove(KEY_EMAIL).apply();
    }
}