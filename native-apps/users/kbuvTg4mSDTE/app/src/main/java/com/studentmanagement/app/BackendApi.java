package com.studentmanagement.app;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import org.json.JSONArray;
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
import java.util.ArrayList;
import java.util.List;

public class BackendApi {

    private static final String BASE_URL = "https://trumpledroid-api.cloudbeta28624.workers.dev";
    private String appId; // Will be read from assets

    public interface ApiCallback<T> {
        void onSuccess(T result);
        void onError(String error);
    }

    public BackendApi(Context context) {
        this.appId = readAppIdFromAssets(context);
        if (this.appId == null) {
            System.err.println("CRITICAL ERROR: app_id could not be loaded from assets!");
        }
    }

    private String readAppIdFromAssets(Context context) {
        String jsonString = null;
        try {
            AssetManager assetManager = context.getAssets();
            InputStream is = assetManager.open("app-meta.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            jsonString = new String(buffer, "UTF-8");
            JSONObject json = new JSONObject(jsonString);
            return json.getString("app_id");
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    // --- Authentication ---

    public void login(String email, String password, final ApiCallback<String> callback) {
        if (appId == null) {
            callback.onError("App ID not initialized. Cannot login.");
            return;
        }
        JSONObject postData = new JSONObject();
        try {
            postData.put("app_id", appId); // Add app_id to JSON body
            postData.put("email", email);
            postData.put("password", password);
        } catch (JSONException e) {
            callback.onError(e.getMessage());
            return;
        }
        // Correct endpoint: /login
        new HttpRequestTask(BASE_URL + "/login", "POST", postData.toString(), null, new ApiCallback<String>() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject jsonResponse = new JSONObject(response);
                    // Check for "success" field
                    if (jsonResponse.optBoolean("success", false)) {
                        String token = jsonResponse.optString("token", null);
                        if (token != null) {
                            callback.onSuccess(token);
                        } else {
                            callback.onError("Login response missing token.");
                        }
                    } else {
                        callback.onError(jsonResponse.optString("error", "Unknown login error."));
                    }
                } catch (JSONException e) {
                    callback.onError("Failed to parse login response: " + e.getMessage());
                }
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        }).execute();
    }

    public void register(final String email, final String password, final ApiCallback<String> callback) {
        if (appId == null) {
            callback.onError("App ID not initialized. Cannot register.");
            return;
        }
        JSONObject postData = new JSONObject();
        try {
            postData.put("app_id", appId); // Add app_id to JSON body
            postData.put("email", email);
            postData.put("password", password);
        } catch (JSONException e) {
            callback.onError(e.getMessage());
            return;
        }
        // Correct endpoint: /register
        new HttpRequestTask(BASE_URL + "/register", "POST", postData.toString(), null, new ApiCallback<String>() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject jsonResponse = new JSONObject(response);
                    // Confirm registration success without expecting a token in the registration response
                    if (jsonResponse.optBoolean("success", false)) {
                        // Registration succeeded. Perform automatic login immediately to obtain authentication token.
                        login(email, password, callback);
                    } else {
                        callback.onError(jsonResponse.optString("error", "Unknown registration error."));
                    }
                } catch (JSONException e) {
                    callback.onError("Failed to parse register response: " + e.getMessage());
                }
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        }).execute();
    }
    
    public void logout(String authToken, final ApiCallback<Void> callback) {
        // Correct endpoint: /logout
        new HttpRequestTask(BASE_URL + "/logout", "POST", null, authToken, new ApiCallback<String>() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject jsonResponse = new JSONObject(response);
                    if (jsonResponse.optBoolean("success", false)) {
                        callback.onSuccess(null);
                    } else {
                        callback.onError(jsonResponse.optString("error", "Unknown logout error."));
                    }
                } catch (JSONException e) {
                    callback.onError("Failed to parse logout response: " + e.getMessage());
                }
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        }).execute();
    }

    // --- Student CRUD Operations ---

    public void getStudents(String authToken, final ApiCallback<List<Student>> callback) {
        if (appId == null) {
            callback.onError("App ID not initialized. Cannot fetch students.");
            return;
        }
        // Correct endpoint: /data with collection name (no app_id parameter for /data requests)
        new HttpRequestTask(BASE_URL + "/data?collection=students", "GET", null, authToken, new ApiCallback<String>() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject jsonResponse = new JSONObject(response);
                    if (jsonResponse.optBoolean("success", false)) {
                        JSONArray jsonArray = jsonResponse.optJSONArray("records");
                        List<Student> students = new ArrayList<Student>();
                        if (jsonArray != null) {
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject jsonObject = jsonArray.getJSONObject(i);
                                String id = jsonObject.getString("id"); // Fixed: get "id" instead of "_id"
                                JSONObject data = jsonObject.getJSONObject("data"); // Fixed: fields are nested inside "data"
                                String name = data.getString("name");
                                String phone = data.getString("phone");
                                String className = data.getString("className");
                                int rollNumber = data.getInt("rollNumber");
                                students.add(new Student(id, name, phone, className, rollNumber));
                            }
                        }
                        callback.onSuccess(students);
                    } else {
                        callback.onError(jsonResponse.optString("error", "Unknown error fetching students."));
                    }
                } catch (JSONException e) {
                    callback.onError("Failed to parse student list: " + e.getMessage());
                }
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        }).execute();
    }

    public void createStudent(String authToken, final Student student, final ApiCallback<Student> callback) {
        if (appId == null) {
            callback.onError("App ID not initialized. Cannot create student.");
            return;
        }
        JSONObject postDataPayload = new JSONObject();
        try {
            postDataPayload.put("name", student.getName());
            postDataPayload.put("phone", student.getPhone());
            postDataPayload.put("className", student.getClassName());
            postDataPayload.put("rollNumber", student.getRollNumber());
        } catch (JSONException e) {
            callback.onError(e.getMessage());
            return;
        }

        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("collection", "students");
            requestBody.put("data", postDataPayload);
        } catch (JSONException e) {
            callback.onError(e.getMessage());
            return;
        }

        // Correct endpoint: /data
        new HttpRequestTask(BASE_URL + "/data", "POST", requestBody.toString(), authToken, new ApiCallback<String>() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject jsonResponse = new JSONObject(response);
                    if (jsonResponse.optBoolean("success", false)) {
                        JSONObject record = jsonResponse.optJSONObject("record");
                        if (record != null) {
                             String id = record.getString("id"); // Fixed: get "id" instead of "_id"
                             JSONObject data = record.getJSONObject("data"); // Parse nested student fields correctly
                             String name = data.getString("name");
                             String phone = data.getString("phone");
                             String className = data.getString("className");
                             int rollNumber = data.getInt("rollNumber");
                             
                             Student createdStudent = new Student(id, name, phone, className, rollNumber);
                             callback.onSuccess(createdStudent);
                        } else {
                            callback.onError("Create student response missing record data.");
                        }
                    } else {
                        callback.onError(jsonResponse.optString("error", "Unknown error creating student."));
                    }
                } catch (JSONException e) {
                    callback.onError("Failed to parse create student response: " + e.getMessage());
                }
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        }).execute();
    }

    public void updateStudent(String authToken, final Student student, final ApiCallback<Student> callback) {
        if (appId == null) {
            callback.onError("App ID not initialized. Cannot update student.");
            return;
        }
        JSONObject putDataPayload = new JSONObject();
        try {
            putDataPayload.put("name", student.getName());
            putDataPayload.put("phone", student.getPhone());
            putDataPayload.put("className", student.getClassName());
            putDataPayload.put("rollNumber", student.getRollNumber());
        } catch (JSONException e) {
            callback.onError(e.getMessage());
            return;
        }

        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("collection", "students");
            requestBody.put("id", student.getId());
            requestBody.put("data", putDataPayload);
        } catch (JSONException e) {
            callback.onError(e.getMessage());
            return;
        }
        // Correct endpoint: /data
        new HttpRequestTask(BASE_URL + "/data", "PUT", requestBody.toString(), authToken, new ApiCallback<String>() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject jsonResponse = new JSONObject(response);
                    if (jsonResponse.optBoolean("success", false)) {
                        callback.onSuccess(student); // Assuming success means update was applied
                    } else {
                        callback.onError(jsonResponse.optString("error", "Unknown error updating student."));
                    }
                } catch (JSONException e) {
                     callback.onError("Failed to parse update student response: " + e.getMessage());
                }
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        }).execute();
    }

    public void deleteStudent(String authToken, String studentId, final ApiCallback<Void> callback) {
        if (appId == null) {
            callback.onError("App ID not initialized. Cannot delete student.");
            return;
        }
        // Correct endpoint: /data with id (no app_id parameter for /data requests)
        new HttpRequestTask(BASE_URL + "/data?id=" + studentId, "DELETE", null, authToken, new ApiCallback<String>() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject jsonResponse = new JSONObject(response);
                    if (jsonResponse.optBoolean("success", false)) {
                        callback.onSuccess(null); // No specific result needed
                    } else {
                        callback.onError(jsonResponse.optString("error", "Unknown error deleting student."));
                    }
                } catch (JSONException e) {
                    callback.onError("Failed to parse delete student response: " + e.getMessage());
                }
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        }).execute();
    }


    // --- Generic HTTP Request Task ---

    private static class HttpRequestTask extends AsyncTask<Void, Void, String> {
        private String urlString;
        private String method;
        private String requestBody;
        private String authToken;
        private ApiCallback<String> callback;
        private String errorMessage;
        private int httpResponseCode; // Store response code

        public HttpRequestTask(String urlString, String method, String requestBody, String authToken, ApiCallback<String> callback) {
            this.urlString = urlString;
            this.method = method;
            this.requestBody = requestBody;
            this.authToken = authToken;
            this.callback = callback;
        }

        @Override
        protected String doInBackground(Void... voids) {
            HttpURLConnection urlConnection = null;
            try {
                URL url = new URL(urlString);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod(method);
                urlConnection.setRequestProperty("Content-Type", "application/json");

                if (authToken != null && !authToken.isEmpty()) {
                    urlConnection.setRequestProperty("Authorization", "Bearer " + authToken);
                }

                if (requestBody != null) {
                    urlConnection.setDoOutput(true);
                    OutputStream os = urlConnection.getOutputStream();
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                    writer.write(requestBody);
                    writer.flush();
                    writer.close();
                    os.close();
                }

                httpResponseCode = urlConnection.getResponseCode(); // Store the response code
                if (httpResponseCode >= 200 && httpResponseCode < 300) { // Check for 2xx success codes
                    BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    String inputLine;
                    StringBuffer response = new StringBuffer();
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();
                    return response.toString();
                } else {
                    BufferedReader errorReader = new BufferedReader(new InputStreamReader(urlConnection.getErrorStream()));
                    String errorLine;
                    StringBuffer errorResponse = new StringBuffer();
                    while ((errorLine = errorReader.readLine()) != null) {
                        errorResponse.append(errorLine);
                    }
                    errorReader.close();
                    String rawError = errorResponse.toString();
                    if (!rawError.isEmpty()) {
                        try {
                            JSONObject errorJson = new JSONObject(rawError);
                            errorMessage = "HTTP Error " + httpResponseCode + ": " + errorJson.optString("error", "Unknown API error.");
                        } catch (JSONException e) {
                            errorMessage = "HTTP Error " + httpResponseCode + ": " + rawError; // Fallback if error is not JSON
                        }
                    } else {
                        errorMessage = "HTTP Error " + httpResponseCode + ": No error message from server.";
                    }
                    return null;
                }
            } catch (Exception e) {
                errorMessage = "Network or parsing error: " + e.getMessage();
                return null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                callback.onSuccess(result);
            } else {
                callback.onError(errorMessage != null ? errorMessage : "Unknown error occurred.");
            }
        }
    }
}