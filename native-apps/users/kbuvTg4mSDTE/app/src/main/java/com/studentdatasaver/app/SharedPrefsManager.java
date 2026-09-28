package com.studentdatasaver.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.util.Log;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class SharedPrefsManager {

    private static final String PREF_NAME = "StudentDataSaverPrefs";
    private static final String KEY_AUTH_TOKEN = "authToken";
    private static final String KEY_APP_ID = "appId";
    private static final String TAG = "SharedPrefsManager";

    private SharedPreferences sharedPreferences;
    private Context context;

    public SharedPrefsManager(Context context) {
        this.context = context;
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        loadAppIdFromAssets();
    }

    private void loadAppIdFromAssets() {
        if (sharedPreferences.contains(KEY_APP_ID)) {
            // App ID already loaded and saved, no need to read from assets again
            return;
        }

        AssetManager assetManager = context.getAssets();
        try {
            InputStream is = assetManager.open("app-meta.json");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            Gson gson = new Gson();
            Map<String, String> appMeta = gson.fromJson(reader, HashMap.class);
            if (appMeta != null && appMeta.containsKey("app_id")) {
                String id = appMeta.get("app_id");
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(KEY_APP_ID, id);
                editor.apply();
                Log.d(TAG, "App ID loaded from assets: " + id);
            } else {
                Log.e(TAG, "app_id not found in app-meta.json");
            }
            is.close();
        } catch (IOException e) {
            Log.e(TAG, "Error reading app-meta.json from assets: " + e.getMessage(), e);
        }
    }

    public void saveAuthToken(String token) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_AUTH_TOKEN, token);
        editor.apply();
    }

    public String getAuthToken() {
        return sharedPreferences.getString(KEY_AUTH_TOKEN, null);
    }

    public void clearAuthToken() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(KEY_AUTH_TOKEN);
        editor.apply();
    }

    public boolean isLoggedIn() {
        return getAuthToken() != null && !getAuthToken().isEmpty();
    }

    public String getAppId() {
        return sharedPreferences.getString(KEY_APP_ID, "default_app_id"); // Fallback, though it should be loaded
    }
}