package com.studentmanager.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class SplashActivity extends Activity {

    private static final String TAG = "SplashActivity";
    private static final String PREF_NAME = "AppPrefs";
    private static final String PREF_AUTH_TOKEN = "authToken";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        final TextView statusTextView = findViewById(R.id.statusTextView);

        // Simulate app initialization time
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    // 1. Read app_id from app-meta.json
                    String appMetaJson = loadJsonFromAsset(SplashActivity.this, "app-meta.json");
                    JSONObject appMeta = new JSONObject(appMetaJson);
                    String app_id = appMeta.getString("app_name").toLowerCase().replace(" ", ""); // app_id is derived from app_name for consistency in this system

                    // Store app_id in SharedPreferences for later use
                    SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("appId", app_id);
                    editor.apply();

                    statusTextView.setText("App ID Loaded: " + app_id);
                    Log.d(TAG, "App ID: " + app_id);

                    // 2. Check for existing auth token
                    String authToken = prefs.getString(PREF_AUTH_TOKEN, null);

                    Intent nextIntent;
                    if (authToken != null && !authToken.isEmpty()) {
                        Log.d(TAG, "Auth token found, navigating to MainActivity.");
                        statusTextView.setText("Session found, redirecting...");
                        nextIntent = new Intent(SplashActivity.this, MainActivity.class);
                    } else {
                        Log.d(TAG, "No auth token found, navigating to LoginActivity.");
                        statusTextView.setText("No session, redirecting to login...");
                        nextIntent = new Intent(SplashActivity.this, LoginActivity.class);
                    }
                    startActivity(nextIntent);
                    finish();

                } catch (IOException e) {
                    Log.e(TAG, "Error reading app-meta.json: " + e.getMessage());
                    statusTextView.setText("Error: Could not load app data.");
                    // Handle critical error, maybe display an alert and close the app
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing app-meta.json: " + e.getMessage());
                    statusTextView.setText("Error: Could not parse app configuration.");
                    // Handle critical error
                }
            }
        }, 2000); // 2 seconds delay
    }

    private String loadJsonFromAsset(Context context, String filename) throws IOException {
        AssetManager assetManager = context.getAssets();
        InputStream is = assetManager.open(filename);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();
        is.close();
        return sb.toString();
    }
}