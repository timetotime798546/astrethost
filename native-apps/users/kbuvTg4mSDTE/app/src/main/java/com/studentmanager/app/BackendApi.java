package com.studentmanager.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BackendApi {

    private static final String TAG = "BackendApi";
    private static final String API_BASE_URL = "https://trumpledroid-api.cloudbeta28624.workers.dev";
    private static final String PREF_NAME = "AppPrefs";
    private static final String PREF_AUTH_TOKEN = "authToken";
    private static final String PREF_APP_ID = "appId";

    private Context context;
    private SharedPreferences sharedPreferences;
    private String appId;
    private ExecutorService executorService;

    public BackendApi(Context context) {
        this.context = context;
        this.sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.appId = sharedPreferences.getString(PREF_APP_ID, null);
        this.executorService = Executors.newSingleThreadExecutor();

        if (this.appId == null) {
            Log.e(TAG, "app_id not found in SharedPreferences. Attempting to load from assets.");
            try {
                String appMetaJson = loadJsonFromAsset(context, "app-meta.json");
                JSONObject appMeta = new JSONObject(appMetaJson);
                this.appId = appMeta.getString("app_name").toLowerCase().replace(" ", "");
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(PREF_APP_ID, this.appId);
                editor.apply();
                Log.d(TAG, "app_id loaded from assets: " + this.appId);
            } catch (IOException e) {
                Log.e(TAG, "Error reading app-meta.json in BackendApi: " + e.getMessage());
            } catch (JSONException e) {
                Log.e(TAG, "Error parsing app-meta.json in BackendApi: " + e.getMessage());
            }
        }
    }

    public interface ApiCallback {
        void onSuccess(JSONObject response);
        void onError(String error);
    }

    private String getAuthToken() {
        return sharedPreferences.getString(PREF_AUTH_TOKEN, null);
    }

    private void saveAuthToken(String token) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(PREF_AUTH_TOKEN, token);
        editor.apply();
    }

    public void clearAuthToken() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(PREF_AUTH_TOKEN);
        editor.apply();
    }

    // --- Authentication Endpoints ---

    public void register(final String email, final String password, final ApiCallback callback) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject requestBody = new JSONObject();
                    requestBody.put("app_id", appId);
                    requestBody.put("email", email);
                    requestBody.put("password", password);

                    JSONObject response = makePostRequest("/register", requestBody, null);

                    if (response != null && response.optBoolean("success")) {
                        // IMPORTANT: /register does not return a token. A successful response only confirms account creation.
                        // If auto-login is desired, a separate /login call must be made.
                        callback.onSuccess(response);
                    } else if (response != null) {
                        callback.onError(response.optString("error", "Registration failed."));
                    } else {
                        callback.onError("Registration failed: No response from server.");
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Error creating JSON for register: " + e.getMessage());
                    callback.onError("Internal error: " + e.getMessage());
                } catch (IOException e) {
                    Log.e(TAG, "Network error during register: " + e.getMessage());
                    callback.onError("Network error: " + e.getMessage());
                }
            }
        });
    }

    public void login(final String email, final String password, final ApiCallback callback) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject requestBody = new JSONObject();
                    requestBody.put("app_id", appId);
                    requestBody.put("email", email);
                    requestBody.put("password", password);

                    JSONObject response = makePostRequest("/login", requestBody, null);

                    if (response != null && response.optBoolean("success")) {
                        String token = response.optString("token");
                        if (token != null && !token.isEmpty()) {
                            saveAuthToken(token);
                            callback.onSuccess(response);
                        } else {
                            Log.e(TAG, "Login successful but no token received: " + response.toString());
                            callback.onError("Login successful but no authentication token received.");
                        }
                    } else if (response != null) {
                        callback.onError(response.optString("error", "Login failed."));
                    } else {
                        callback.onError("Login failed: No response from server.");
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Error creating JSON for login: " + e.getMessage());
                    callback.onError("Internal error: " + e.getMessage());
                } catch (IOException e) {
                    Log.e(TAG, "Network error during login: " + e.getMessage());
                    callback.onError("Network error: " + e.getMessage());
                }
            }
        });
    }

    public void logout(final ApiCallback callback) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String token = getAuthToken();
                    if (token == null) {
                        callback.onError("No active session to logout.");
                        return;
                    }
                    JSONObject response = makePostRequest("/logout", new JSONObject(), token);

                    if (response != null && response.optBoolean("success")) {
                        clearAuthToken();
                        callback.onSuccess(response);
                    } else if (response != null) {
                        callback.onError(response.optString("error", "Logout failed."));
                    } else {
                        callback.onError("Logout failed: No response from server.");
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Network error during logout: " + e.getMessage());
                    callback.onError("Network error: " + e.getMessage());
                }
            }
        });
    }

    // --- CRUD Endpoints ---

    public void createRecord(final String collection, final JSONObject data, final ApiCallback callback) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject requestBody = new JSONObject();
                    requestBody.put("collection", collection);
                    requestBody.put("data", data);

                    String token = getAuthToken();
                    if (token == null) {
                        callback.onError("Authentication required for creating record.");
                        return;
                    }

                    JSONObject response = makePostRequest("/data", requestBody, token);

                    if (response != null && response.optBoolean("success")) {
                        if (response.has("record")) {
                            callback.onSuccess(response);
                        } else {
                            callback.onError("Create record success but 'record' object missing in response.");
                        }
                    } else if (response != null) {
                        callback.onError(response.optString("error", "Failed to create record."));
                    } else {
                        callback.onError("Failed to create record: No response from server.");
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Error creating JSON for create record: " + e.getMessage());
                    callback.onError("Internal error: " + e.getMessage());
                } catch (IOException e) {
                    Log.e(TAG, "Network error during create record: " + e.getMessage());
                    callback.onError("Network error: " + e.getMessage());
                }
            }
        });
    }

    public void readRecords(final String collection, final ApiCallback callback) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String token = getAuthToken();
                    if (token == null) {
                        callback.onError("Authentication required for reading records.");
                        return;
                    }

                    String url = "/data?collection=" + collection;
                    JSONObject response = makeGetRequest(url, token);

                    if (response != null && response.optBoolean("success")) {
                        if (response.has("records")) {
                            callback.onSuccess(response);
                        } else {
                            callback.onError("Read records success but 'records' array missing in response.");
                        }
                    } else if (response != null) {
                        callback.onError(response.optString("error", "Failed to read records."));
                    } else {
                        callback.onError("Failed to read records: No response from server.");
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Network error during read records: " + e.getMessage());
                    callback.onError("Network error: " + e.getMessage());
                }
            }
        });
    }

    public void updateRecord(final String recordId, final JSONObject data, final ApiCallback callback) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject requestBody = new JSONObject();
                    requestBody.put("id", recordId);
                    requestBody.put("data", data);

                    String token = getAuthToken();
                    if (token == null) {
                        callback.onError("Authentication required for updating record.");
                        return;
                    }

                    JSONObject response = makePutRequest("/data", requestBody, token);

                    if (response != null && response.optBoolean("success")) {
                        if (response.has("id")) {
                            callback.onSuccess(response);
                        } else {
                            callback.onError("Update record success but 'id' field missing in response.");
                        }
                    } else if (response != null) {
                        callback.onError(response.optString("error", "Failed to update record."));
                    } else {
                        callback.onError("Failed to update record: No response from server.");
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Error creating JSON for update record: " + e.getMessage());
                    callback.onError("Internal error: " + e.getMessage());
                } catch (IOException e) {
                    Log.e(TAG, "Network error during update record: " + e.getMessage());
                    callback.onError("Network error: " + e.getMessage());
                }
            }
        });
    }

    public void deleteRecord(final String recordId, final ApiCallback callback) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String token = getAuthToken();
                    if (token == null) {
                        callback.onError("Authentication required for deleting record.");
                        return;
                    }

                    String url = "/data?id=" + recordId;
                    JSONObject response = makeDeleteRequest(url, token);

                    if (response != null && response.optBoolean("success")) {
                        if (response.has("id")) {
                            callback.onSuccess(response);
                        } else {
                            callback.onError("Delete record success but 'id' field missing in response.");
                        }
                    } else if (response != null) {
                        callback.onError(response.optString("error", "Failed to delete record."));
                    } else {
                        callback.onError("Failed to delete record: No response from server.");
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Network error during delete record: " + e.getMessage());
                    callback.onError("Network error: " + e.getMessage());
                }
            }
        });
    }

    // --- HTTP Request Helpers ---

    private JSONObject makePostRequest(String path, JSONObject body, String token) throws IOException {
        return makeRequest("POST", path, body, token);
    }

    private JSONObject makeGetRequest(String path, String token) throws IOException {
        return makeRequest("GET", path, null, token);
    }

    private JSONObject makePutRequest(String path, JSONObject body, String token) throws IOException {
        return makeRequest("PUT", path, body, token);
    }

    private JSONObject makeDeleteRequest(String path, String token) throws IOException {
        return makeRequest("DELETE", path, null, token);
    }

    private JSONObject makeRequest(String method, String path, JSONObject body, String token) throws IOException {
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(API_BASE_URL + path);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod(method);
            urlConnection.setRequestProperty("Content-Type", "application/json; charset=utf-8");

            if (token != null && !token.isEmpty()) {
                urlConnection.setRequestProperty("Authorization", "Bearer " + token);
            }

            if (body != null && (method.equals("POST") || method.equals("PUT"))) {
                urlConnection.setDoOutput(true);
                OutputStream os = urlConnection.getOutputStream();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8));
                writer.write(body.toString());
                writer.flush();
                writer.close();
                os.close();
            }

            int responseCode = urlConnection.getResponseCode();
            Log.d(TAG, "Response Code for " + method + " " + path + ": " + responseCode);

            InputStream inputStream;
            if (responseCode >= 200 && responseCode < 300) {
                inputStream = urlConnection.getInputStream();
            } else {
                inputStream = urlConnection.getErrorStream();
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            String responseString = response.toString();
            Log.d(TAG, "Response: " + responseString);

            if (responseString.isEmpty()) {
                // Some APIs might return empty body on success (e.g., 204 No Content)
                // For this API, we expect a JSON success/error structure.
                return new JSONObject().put("success", (responseCode >= 200 && responseCode < 300));
            }

            return new JSONObject(responseString);

        } catch (JSONException e) {
            Log.e(TAG, "Error parsing JSON response: " + e.getMessage());
            try {
                return new JSONObject().put("success", false).put("error", "Invalid JSON response from server.");
            } catch (JSONException ex) {
                // Should not happen
                return null;
            }
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
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