package com.notesapp.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BackendApi {

    private static final String TAG = "BackendApi";
    private static final String PREFS_NAME = "NotesAppPrefs";
    private static final String KEY_AUTH_TOKEN = "authToken";

    // Initialize with fallback values to prevent 'null' issues if asset loading fails
    private String apiBaseUrl = "https://trumpledroid-api.cloudbeta28624.workers.dev";
    private String appId = "default-app-id"; // App ID should ideally be unique per app deployment

    private Context context;
    private ExecutorService executorService;

    public interface BackendApiCallback {
        void onSuccess(JSONObject response);
        void onError(String error);
    }

    public interface BackendApiListCallback {
        void onSuccess(JSONArray response);
        void onError(String error);
    }

    public BackendApi(Context context) {
        this.context = context;
        this.executorService = Executors.newSingleThreadExecutor();
        loadApiConfig(); // Call config loading during construction
    }

    private void loadApiConfig() {
        AssetManager assetManager = context.getAssets();
        InputStream is = null;
        BufferedReader reader = null;
        try {
            // Attempt to load app-meta.json first
            try {
                is = assetManager.open("app-meta.json");
                reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                JSONObject appMeta = new JSONObject(sb.toString());
                this.appId = appMeta.optString("app_id", this.appId); // Update if found, else keep default
                Log.d(TAG, "App ID loaded from assets: " + this.appId);
            } catch (IOException | JSONException e) {
                Log.e(TAG, "Error loading/parsing app-meta.json from assets, using default: " + this.appId + ". Error: " + e.getMessage());
                // appId retains its initial default value
            } finally {
                if (reader != null) try { reader.close(); } catch (IOException e) { /* ignored */ }
                if (is != null) try { is.close(); } catch (IOException e) { /* ignored */ }
            }

            // Attempt to load api_schema.json
            try {
                is = assetManager.open("api_schema.json"); // Re-initialize 'is' and 'reader'
                reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                JSONObject apiSchema = new JSONObject(sb.toString());
                this.apiBaseUrl = apiSchema.optString("api_base_url", this.apiBaseUrl); // Update if found, else keep default
                Log.d(TAG, "API Base URL loaded from assets: " + this.apiBaseUrl);
            } catch (IOException | JSONException e) {
                Log.e(TAG, "Error loading/parsing api_schema.json from assets, using default: " + this.apiBaseUrl + ". Error: " + e.getMessage());
                // apiBaseUrl retains its initial default value
            } finally {
                if (reader != null) try { reader.close(); } catch (IOException e) { /* ignored */ }
                if (is != null) try { is.close(); } catch (IOException e) { /* ignored */ }
            }

        } catch (Exception e) { // Catch any other unexpected errors that might occur outside the specific file loads
            Log.e(TAG, "Unexpected error during API config loading: " + e.getMessage());
        }
    }

    private String getAuthToken() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_AUTH_TOKEN, null);
    }

    private void saveAuthToken(String token) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_AUTH_TOKEN, token);
        editor.apply();
    }

    public void clearAuthToken() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(KEY_AUTH_TOKEN);
        editor.apply();
    }

    public boolean isLoggedIn() {
        return getAuthToken() != null;
    }

    public void register(final String email, final String password, final BackendApiCallback callback) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                // Ensure apiBaseUrl is not null or empty before proceeding
                if (apiBaseUrl == null || apiBaseUrl.isEmpty()) {
                    callback.onError("API base URL not configured.");
                    return;
                }
                try {
                    URL url = new URL(apiBaseUrl + "/register");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);

                    JSONObject jsonBody = new JSONObject();
                    jsonBody.put("app_id", appId);
                    jsonBody.put("email", email);
                    jsonBody.put("password", password);

                    OutputStream os = conn.getOutputStream();
                    os.write(jsonBody.toString().getBytes());
                    os.flush();

                    int responseCode = conn.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        String inputLine;
                        StringBuilder response = new StringBuilder();
                        while ((inputLine = in.readLine()) != null) {
                            response.append(inputLine);
                        }
                        in.close();
                        final JSONObject jsonResponse = new JSONObject(response.toString());
                        if (jsonResponse.optBoolean("success", false)) {
                            // /register does not return an authentication token. Only /login does.
                            callback.onSuccess(jsonResponse);
                        } else {
                            callback.onError(jsonResponse.optString("message", "Registration failed"));
                        }
                    } else {
                        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                        String inputLine;
                        StringBuilder response = new StringBuilder();
                        while ((inputLine = in.readLine()) != null) {
                            response.append(inputLine);
                        }
                        in.close();
                        JSONObject errorJson = new JSONObject(response.toString());
                        callback.onError(errorJson.optString("message", "HTTP Error: " + responseCode));
                    }
                } catch (IOException e) {
                    callback.onError("Network error: " + e.getMessage());
                } catch (JSONException e) {
                    callback.onError("JSON error: " + e.getMessage());
                }
            }
        });
    }

    public void login(final String email, final String password, final BackendApiCallback callback) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                if (apiBaseUrl == null || apiBaseUrl.isEmpty()) {
                    callback.onError("API base URL not configured.");
                    return;
                }
                try {
                    URL url = new URL(apiBaseUrl + "/login");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);

                    JSONObject jsonBody = new JSONObject();
                    jsonBody.put("app_id", appId);
                    jsonBody.put("email", email);
                    jsonBody.put("password", password);

                    OutputStream os = conn.getOutputStream();
                    os.write(jsonBody.toString().getBytes());
                    os.flush();

                    int responseCode = conn.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        String inputLine;
                        StringBuilder response = new StringBuilder();
                        while ((inputLine = in.readLine()) != null) {
                            response.append(inputLine);
                        }
                        in.close();
                        final JSONObject jsonResponse = new JSONObject(response.toString());
                        if (jsonResponse.optBoolean("success", false)) {
                            String token = jsonResponse.optString("token");
                            if (token != null && !token.isEmpty()) {
                                saveAuthToken(token);
                                callback.onSuccess(jsonResponse);
                            } else {
                                callback.onError("Login failed: token not received.");
                            }
                        } else {
                            callback.onError(jsonResponse.optString("message", "Login failed"));
                        }
                    } else {
                        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                        String inputLine;
                        StringBuilder response = new StringBuilder();
                        while ((inputLine = in.readLine()) != null) {
                            response.append(inputLine);
                        }
                        in.close();
                        JSONObject errorJson = new JSONObject(response.toString());
                        callback.onError(errorJson.optString("message", "HTTP Error: " + responseCode));
                    }
                } catch (IOException e) {
                    callback.onError("Network error: " + e.getMessage());
                } catch (JSONException e) {
                    callback.onError("JSON error: " + e.getMessage());
                }
            }
        });
    }

    public void logout(final BackendApiCallback callback) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                if (apiBaseUrl == null || apiBaseUrl.isEmpty()) {
                    callback.onError("API base URL not configured.");
                    return;
                }
                try {
                    URL url = new URL(apiBaseUrl + "/logout");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    String authToken = getAuthToken();
                    if (authToken != null) {
                        conn.setRequestProperty("Authorization", "Bearer " + authToken);
                    }
                    conn.setDoOutput(true);

                    // Send an empty JSON body if the API expects one
                    OutputStream os = conn.getOutputStream();
                    os.write(new JSONObject().toString().getBytes()); // Send an empty JSON object
                    os.flush();

                    int responseCode = conn.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        String inputLine;
                        StringBuilder response = new StringBuilder();
                        while ((inputLine = in.readLine()) != null) {
                            response.append(inputLine);
                        }
                        in.close();
                        final JSONObject jsonResponse = new JSONObject(response.toString());
                        if (jsonResponse.optBoolean("success", false)) {
                            clearAuthToken();
                            callback.onSuccess(jsonResponse);
                        } else {
                            callback.onError(jsonResponse.optString("message", "Logout failed"));
                        }
                    } else {
                        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                        String inputLine;
                        StringBuilder response = new StringBuilder();
                        while ((inputLine = in.readLine()) != null) {
                            response.append(inputLine);
                        }
                        in.close();
                        JSONObject errorJson = new JSONObject(response.toString());
                        callback.onError(errorJson.optString("message", "HTTP Error: " + responseCode));
                    }
                } catch (IOException e) {
                    callback.onError("Network error: " + e.getMessage());
                } catch (JSONException e) {
                    callback.onError("JSON error: " + e.getMessage());
                }
            }
        });
    }


    public void createRecord(final String collection, final JSONObject data, final BackendApiCallback callback) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                if (apiBaseUrl == null || apiBaseUrl.isEmpty()) {
                    callback.onError("API base URL not configured.");
                    return;
                }
                try {
                    URL url = new URL(apiBaseUrl + "/data");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    String authToken = getAuthToken();
                    if (authToken != null) {
                        conn.setRequestProperty("Authorization", "Bearer " + authToken);
                    }
                    conn.setDoOutput(true);

                    JSONObject jsonBody = new JSONObject();
                    jsonBody.put("collection", collection);
                    jsonBody.put("data", data);

                    OutputStream os = conn.getOutputStream();
                    os.write(jsonBody.toString().getBytes());
                    os.flush();

                    int responseCode = conn.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        String inputLine;
                        StringBuilder response = new StringBuilder();
                        while ((inputLine = in.readLine()) != null) {
                            response.append(inputLine);
                        }
                        in.close();
                        final JSONObject jsonResponse = new JSONObject(response.toString());
                        if (jsonResponse.optBoolean("success", false) && jsonResponse.has("record")) {
                            callback.onSuccess(jsonResponse);
                        } else {
                            callback.onError(jsonResponse.optString("message", "Record creation failed or response malformed."));
                        }
                    } else {
                        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                        String inputLine;
                        StringBuilder response = new StringBuilder();
                        while ((inputLine = in.readLine()) != null) {
                            response.append(inputLine);
                        }
                        in.close();
                        JSONObject errorJson = new JSONObject(response.toString());
                        callback.onError(errorJson.optString("message", "HTTP Error: " + responseCode));
                    }
                } catch (IOException e) {
                    callback.onError("Network error: " + e.getMessage());
                } catch (JSONException e) {
                    callback.onError("JSON error: " + e.getMessage());
                }
            }
        });
    }

    public void readRecords(final String collection, final BackendApiListCallback callback) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                if (apiBaseUrl == null || apiBaseUrl.isEmpty()) {
                    callback.onError("API base URL not configured.");
                    return;
                }
                try {
                    URL url = new URL(apiBaseUrl + "/data?collection=" + collection);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    String authToken = getAuthToken();
                    if (authToken != null) {
                        conn.setRequestProperty("Authorization", "Bearer " + authToken);
                    }

                    int responseCode = conn.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        String inputLine;
                        StringBuilder response = new StringBuilder();
                        while ((inputLine = in.readLine()) != null) {
                            response.append(inputLine);
                        }
                        in.close();
                        final JSONObject jsonResponse = new JSONObject(response.toString());
                        if (jsonResponse.optBoolean("success", false) && jsonResponse.has("records")) {
                            callback.onSuccess(jsonResponse.getJSONArray("records"));
                        } else {
                            callback.onError(jsonResponse.optString("message", "Failed to retrieve records or response malformed."));
                        }
                    } else {
                        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                        String inputLine;
                        StringBuilder response = new StringBuilder();
                        while ((inputLine = in.readLine()) != null) {
                            response.append(inputLine);
                        }
                        in.close();
                        JSONObject errorJson = new JSONObject(response.toString());
                        callback.onError(errorJson.optString("message", "HTTP Error: " + responseCode));
                    }
                } catch (IOException e) {
                    callback.onError("Network error: " + e.getMessage());
                } catch (JSONException e) {
                    callback.onError("JSON error: " + e.getMessage());
                }
            }
        });
    }

    public void updateRecord(final String id, final JSONObject data, final BackendApiCallback callback) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                if (apiBaseUrl == null || apiBaseUrl.isEmpty()) {
                    callback.onError("API base URL not configured.");
                    return;
                }
                try {
                    URL url = new URL(apiBaseUrl + "/data");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("PUT");
                    conn.setRequestProperty("Content-Type", "application/json");
                    String authToken = getAuthToken();
                    if (authToken != null) {
                        conn.setRequestProperty("Authorization", "Bearer " + authToken);
                    }
                    conn.setDoOutput(true);

                    JSONObject jsonBody = new JSONObject();
                    jsonBody.put("id", id);
                    jsonBody.put("data", data);

                    OutputStream os = conn.getOutputStream();
                    os.write(jsonBody.toString().getBytes());
                    os.flush();

                    int responseCode = conn.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        String inputLine;
                        StringBuilder response = new StringBuilder();
                        while ((inputLine = in.readLine()) != null) {
                            response.append(inputLine);
                        }
                        in.close();
                        final JSONObject jsonResponse = new JSONObject(response.toString());
                        if (jsonResponse.optBoolean("success", false) && jsonResponse.has("id")) {
                            callback.onSuccess(jsonResponse);
                        } else {
                            callback.onError(jsonResponse.optString("message", "Record update failed or response malformed."));
                        }
                    } else {
                        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                        String inputLine;
                        StringBuilder response = new StringBuilder();
                        while ((inputLine = in.readLine()) != null) {
                            response.append(inputLine);
                        }
                        in.close();
                        JSONObject errorJson = new JSONObject(response.toString());
                        callback.onError(errorJson.optString("message", "HTTP Error: " + responseCode));
                    }
                } catch (IOException e) {
                    callback.onError("Network error: " + e.getMessage());
                } catch (JSONException e) {
                    callback.onError("JSON error: " + e.getMessage());
                }
            }
        });
    }

    public void deleteRecord(final String id, final BackendApiCallback callback) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                if (apiBaseUrl == null || apiBaseUrl.isEmpty()) {
                    callback.onError("API base URL not configured.");
                    return;
                }
                try {
                    URL url = new URL(apiBaseUrl + "/data?id=" + id);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("DELETE");
                    String authToken = getAuthToken();
                    if (authToken != null) {
                        conn.setRequestProperty("Authorization", "Bearer " + authToken);
                    }

                    int responseCode = conn.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        String inputLine;
                        StringBuilder response = new StringBuilder();
                        while ((inputLine = in.readLine()) != null) {
                            response.append(inputLine);
                        }
                        in.close();
                        final JSONObject jsonResponse = new JSONObject(response.toString());
                        if (jsonResponse.optBoolean("success", false) && jsonResponse.has("id")) {
                            callback.onSuccess(jsonResponse);
                        } else {
                            callback.onError(jsonResponse.optString("message", "Record deletion failed or response malformed."));
                        }
                    } else {
                        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                        String inputLine;
                        StringBuilder response = new StringBuilder();
                        while ((inputLine = in.readLine()) != null) {
                            response.append(inputLine);
                        }
                        in.close();
                        JSONObject errorJson = new JSONObject(response.toString());
                        callback.onError(errorJson.optString("message", "HTTP Error: " + responseCode));
                    }
                } catch (IOException e) {
                    callback.onError("Network error: " + e.getMessage());
                } catch (JSONException e) {
                    callback.onError("JSON error: " + e.getMessage());
                }
            }
        });
    }
}