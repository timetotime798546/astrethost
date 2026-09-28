package com.studentdatasaver.app;

import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class BackendApi {

    private static final String TAG = "BackendApi";
    private static final String BASE_URL = "https://trumpledroid-api.cloudbeta28624.workers.dev";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private OkHttpClient client;
    private Gson gson;
    private String appId;
    private String authToken; // Can be null if not logged in

    public BackendApi(String appId, String authToken) {
        this.client = new OkHttpClient();
        this.gson = new Gson();
        this.appId = appId;
        this.authToken = authToken;
    }

    public void setAuthToken(String token) {
        this.authToken = token;
    }

    public String getAuthToken() {
        return authToken;
    }

    public interface BackendApiCallback {
        void onSuccess(String response);
        void onError(String error);
    }

    // --- Authentication Endpoints ---

    public void register(final String email, final String password, final BackendApiCallback callback) {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... voids) {
                Map<String, String> bodyMap = new HashMap<String, String>();
                bodyMap.put("app_id", appId);
                bodyMap.put("email", email);
                bodyMap.put("password", password);
                String jsonBody = gson.toJson(bodyMap);

                RequestBody requestBody = RequestBody.create(jsonBody, JSON);
                Request request = new Request.Builder()
                        .url(BASE_URL + "/register")
                        .post(requestBody)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        return response.body().string();
                    } else {
                        return "Error: " + response.code() + " - " + response.body().string();
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Register network error: " + e.getMessage(), e);
                    return "Network Error: " + e.getMessage();
                }
            }

            @Override
            protected void onPostExecute(String result) {
                if (result != null && result.startsWith("Error")) {
                    callback.onError(result);
                } else if (result != null && result.startsWith("Network Error")) {
                    callback.onError(result);
                } else {
                    callback.onSuccess(result);
                }
            }
        }.execute();
    }

    public void login(final String email, final String password, final BackendApiCallback callback) {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... voids) {
                Map<String, String> bodyMap = new HashMap<String, String>();
                bodyMap.put("app_id", appId);
                bodyMap.put("email", email);
                bodyMap.put("password", password);
                String jsonBody = gson.toJson(bodyMap);

                RequestBody requestBody = RequestBody.create(jsonBody, JSON);
                Request request = new Request.Builder()
                        .url(BASE_URL + "/login")
                        .post(requestBody)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        return response.body().string();
                    } else {
                        return "Error: " + response.code() + " - " + response.body().string();
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Login network error: " + e.getMessage(), e);
                    return "Network Error: " + e.getMessage();
                }
            }

            @Override
            protected void onPostExecute(String result) {
                if (result != null && result.startsWith("Error")) {
                    callback.onError(result);
                } else if (result != null && result.startsWith("Network Error")) {
                    callback.onError(result);
                } else {
                    callback.onSuccess(result);
                }
            }
        }.execute();
    }

    public void logout(final BackendApiCallback callback) {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... voids) {
                if (authToken == null || authToken.isEmpty()) {
                    return "Error: Not authenticated.";
                }
                Request request = new Request.Builder()
                        .url(BASE_URL + "/logout")
                        .post(RequestBody.create("{}", JSON)) // Empty body for POST logout
                        .header("Authorization", "Bearer " + authToken)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        return response.body().string();
                    } else {
                        return "Error: " + response.code() + " - " + response.body().string();
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Logout network error: " + e.getMessage(), e);
                    return "Network Error: " + e.getMessage();
                }
            }

            @Override
            protected void onPostExecute(String result) {
                if (result != null && result.startsWith("Error")) {
                    callback.onError(result);
                } else if (result != null && result.startsWith("Network Error")) {
                    callback.onError(result);
                } else {
                    callback.onSuccess(result);
                }
            }
        }.execute();
    }

    // --- Data Endpoints (CRUD) ---

    public void createData(final String collection, final Map<String, Object> data, final BackendApiCallback callback) {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... voids) {
                if (authToken == null || authToken.isEmpty()) {
                    return "Error: Not authenticated.";
                }
                Map<String, Object> bodyMap = new HashMap<String, Object>();
                bodyMap.put("collection", collection);
                bodyMap.put("data", data);
                String jsonBody = gson.toJson(bodyMap);

                RequestBody requestBody = RequestBody.create(jsonBody, JSON);
                Request request = new Request.Builder()
                        .url(BASE_URL + "/data")
                        .post(requestBody)
                        .header("Authorization", "Bearer " + authToken)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        return response.body().string();
                    } else {
                        return "Error: " + response.code() + " - " + response.body().string();
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Create data network error: " + e.getMessage(), e);
                    return "Network Error: " + e.getMessage();
                }
            }

            @Override
            protected void onPostExecute(String result) {
                if (result != null && result.startsWith("Error")) {
                    callback.onError(result);
                } else if (result != null && result.startsWith("Network Error")) {
                    callback.onError(result);
                } else {
                    callback.onSuccess(result);
                }
            }
        }.execute();
    }

    public void readData(final String collection, final BackendApiCallback callback) {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... voids) {
                if (authToken == null || authToken.isEmpty()) {
                    return "Error: Not authenticated.";
                }
                Request request = new Request.Builder()
                        .url(BASE_URL + "/data?collection=" + collection)
                        .get()
                        .header("Authorization", "Bearer " + authToken)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        return response.body().string();
                    } else {
                        return "Error: " + response.code() + " - " + response.body().string();
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Read data network error: " + e.getMessage(), e);
                    return "Network Error: " + e.getMessage();
                }
            }

            @Override
            protected void onPostExecute(String result) {
                if (result != null && result.startsWith("Error")) {
                    callback.onError(result);
                } else if (result != null && result.startsWith("Network Error")) {
                    callback.onError(result);
                } else {
                    callback.onSuccess(result);
                }
            }
        }.execute();
    }

    public void updateData(final String recordId, final Map<String, Object> data, final String collection, final BackendApiCallback callback) {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... voids) {
                if (authToken == null || authToken.isEmpty()) {
                    return "Error: Not authenticated.";
                }
                Map<String, Object> bodyMap = new HashMap<String, Object>();
                bodyMap.put("id", recordId);
                bodyMap.put("data", data);
                bodyMap.put("collection", collection); // Although backend doesn't explicitly require collection for PUT/DELETE, passing it for consistency if it ever changes.
                String jsonBody = gson.toJson(bodyMap);

                RequestBody requestBody = RequestBody.create(jsonBody, JSON);
                Request request = new Request.Builder()
                        .url(BASE_URL + "/data")
                        .put(requestBody)
                        .header("Authorization", "Bearer " + authToken)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        return response.body().string();
                    } else {
                        return "Error: " + response.code() + " - " + response.body().string();
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Update data network error: " + e.getMessage(), e);
                    return "Network Error: " + e.getMessage();
                }
            }

            @Override
            protected void onPostExecute(String result) {
                if (result != null && result.startsWith("Error")) {
                    callback.onError(result);
                } else if (result != null && result.startsWith("Network Error")) {
                    callback.onError(result);
                } else {
                    callback.onSuccess(result);
                }
            }
        }.execute();
    }

    public void deleteData(final String recordId, final BackendApiCallback callback) {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... voids) {
                if (authToken == null || authToken.isEmpty()) {
                    return "Error: Not authenticated.";
                }
                Request request = new Request.Builder()
                        .url(BASE_URL + "/data?id=" + recordId)
                        .delete()
                        .header("Authorization", "Bearer " + authToken)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        return response.body().string();
                    } else {
                        return "Error: " + response.code() + " - " + response.body().string();
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Delete data network error: " + e.getMessage(), e);
                    return "Network Error: " + e.getMessage();
                }
            }

            @Override
            protected void onPostExecute(String result) {
                if (result != null && result.startsWith("Error")) {
                    callback.onError(result);
                } else if (result != null && result.startsWith("Network Error")) {
                    callback.onError(result);
                } else {
                    callback.onSuccess(result);
                }
            }
        }.execute();
    }
}