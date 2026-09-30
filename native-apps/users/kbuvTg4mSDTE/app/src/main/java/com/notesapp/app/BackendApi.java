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
    private static final String KEY_APP_ID = "appId";

    private String apiBaseUrl;
    private String appId;
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
        loadApiConfig();
    }

    private void loadApiConfig() {
        AssetManager assetManager = context.getAssets();
        try {
            InputStream is = assetManager.open("app-meta.json");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            is.close();
            JSONObject appMeta = new JSONObject(sb.toString());
            this.appId = appMeta.optString("app_id", "default-app-id"); // The build system places the final app-meta.json, including app_id, here.

            is = assetManager.open("api_schema.json");
            reader = new BufferedReader(new InputStreamReader(is));
            sb = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            is.close();
            JSONObject apiSchema = new JSONObject(sb.toString());
            this.apiBaseUrl = apiSchema.optString("api_base_url", "https://trumpledroid-api.cloudbeta28624.workers.dev");

            Log.d(TAG, "API Base URL: " + apiBaseUrl);
            Log.d(TAG, "App ID: " + appId);

        } catch (IOException e) {
            Log.e(TAG, "Error reading app-meta.json or api_schema.json: " + e.getMessage());
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing app-meta.json or api_schema.json: " + e.getMessage());
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
                try {
                    URL url = new URL(apiBaseUrl + "/logout");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    String authToken = getAuthToken();
                    if (authToken != null) {
                        conn.setRequestProperty("Authorization", "Bearer " + authToken);
                    }
                    conn.setDoOutput(true); // Indicate there will be a request body, even if empty

                    // Send an empty JSON body if the API expects one, or just close output stream
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