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
    private Context context;
    private String appId;

    public interface ApiCallback<T> {
        void onSuccess(T result);
        void onError(String error);
    }

    public BackendApi(Context context) {
        this.context = context;
        loadAppId();
    }

    private void loadAppId() {
        if (appId != null) {
            return; // Already loaded
        }
        AssetManager assetManager = context.getAssets();
        try {
            InputStream is = assetManager.open("app-meta.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String jsonString = new String(buffer, "UTF-8");
            JSONObject jsonObject = new JSONObject(jsonString);
            appId = jsonObject.getString("app_id");
        } catch (IOException e) {
            // Log this error, as the app_id is crucial.
            e.printStackTrace();
            appId = null; // Indicate app_id loading failed
        } catch (JSONException e) {
            e.printStackTrace();
            appId = null; // Indicate app_id parsing failed
        }
    }

    // --- Authentication ---

    public void login(String email, String password, ApiCallback<String> callback) {
        if (appId == null) {
            callback.onError("App ID not loaded. Cannot perform login.");
            return;
        }

        JSONObject postData = new JSONObject();
        try {
            postData.put("app_id", appId);
            postData.put("email", email);
            postData.put("password", password);
        } catch (JSONException e) {
            callback.onError(e.getMessage());
            return;
        }
        new HttpRequestTask(BASE_URL + "/login", "POST", postData.toString(), null, new ApiCallback<String>() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject jsonResponse = new JSONObject(response);
                    if (jsonResponse.optBoolean("success", false)) {
                        String token = jsonResponse.getString("token");
                        callback.onSuccess(token);
                    } else {
                        callback.onError(jsonResponse.optString("error", "Login failed: Unknown error."));
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

    public void register(String email, String password, ApiCallback<String> callback) {
        if (appId == null) {
            callback.onError("App ID not loaded. Cannot perform registration.");
            return;
        }

        JSONObject postData = new JSONObject();
        try {
            postData.put("app_id", appId);
            postData.put("email", email);
            postData.put("password", password);
        } catch (JSONException e) {
            callback.onError(e.getMessage());
            return;
        }
        new HttpRequestTask(BASE_URL + "/register", "POST", postData.toString(), null, new ApiCallback<String>() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject jsonResponse = new JSONObject(response);
                    if (jsonResponse.optBoolean("success", false)) {
                        String token = jsonResponse.getString("token");
                        callback.onSuccess(token);
                    } else {
                        callback.onError(jsonResponse.optString("error", "Registration failed: Unknown error."));
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

    // --- Student CRUD Operations ---

    public void getStudents(String authToken, ApiCallback<List<Student>> callback) {
        new HttpRequestTask(BASE_URL + "/data?collection=students", "GET", null, authToken, new ApiCallback<String>() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject jsonResponse = new JSONObject(response);
                    if (jsonResponse.optBoolean("success", false)) {
                        JSONArray jsonArray = jsonResponse.getJSONArray("records");
                        List<Student> students = new ArrayList<Student>();
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            String id = jsonObject.getString("_id");
                            String name = jsonObject.getString("name");
                            String phone = jsonObject.getString("phone");
                            String className = jsonObject.getString("className");
                            int rollNumber = jsonObject.getInt("rollNumber");
                            students.add(new Student(id, name, phone, className, rollNumber));
                        }
                        callback.onSuccess(students);
                    } else {
                        callback.onError(jsonResponse.optString("error", "Failed to retrieve students: Unknown error."));
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

    public void createStudent(String authToken, Student student, ApiCallback<Student> callback) {
        if (appId == null) {
            callback.onError("App ID not loaded. Cannot create student.");
            return;
        }

        JSONObject studentData = new JSONObject();
        try {
            studentData.put("name", student.getName());
            studentData.put("phone", student.getPhone());
            studentData.put("className", student.getClassName());
            studentData.put("rollNumber", student.getRollNumber());

            JSONObject postData = new JSONObject();
            postData.put("app_id", appId);
            postData.put("collection", "students");
            postData.put("data", studentData);

        } catch (JSONException e) {
            callback.onError(e.getMessage());
            return;
        }
        new HttpRequestTask(BASE_URL + "/data", "POST", studentData.toString(), authToken, new ApiCallback<String>() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject jsonResponse = new JSONObject(response);
                    if (jsonResponse.optBoolean("success", false)) {
                        JSONObject record = jsonResponse.getJSONObject("record");
                        String id = record.getString("_id");
                        student.setId(id); // Assign ID received from backend
                        callback.onSuccess(student);
                    } else {
                        callback.onError(jsonResponse.optString("error", "Failed to create student: Unknown error."));
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

    public void updateStudent(String authToken, Student student, ApiCallback<Student> callback) {
        if (appId == null) {
            callback.onError("App ID not loaded. Cannot update student.");
            return;
        }
        
        JSONObject studentData = new JSONObject();
        try {
            studentData.put("name", student.getName());
            studentData.put("phone", student.getPhone());
            studentData.put("className", student.getClassName());
            studentData.put("rollNumber", student.getRollNumber());

            JSONObject putData = new JSONObject();
            putData.put("app_id", appId);
            putData.put("collection", "students");
            putData.put("id", student.getId());
            putData.put("data", studentData);

        } catch (JSONException e) {
            callback.onError(e.getMessage());
            return;
        }
        new HttpRequestTask(BASE_URL + "/data", "PUT", studentData.toString(), authToken, new ApiCallback<String>() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject jsonResponse = new JSONObject(response);
                    if (jsonResponse.optBoolean("success", false)) {
                        callback.onSuccess(student); // Assuming success means update was applied
                    } else {
                        callback.onError(jsonResponse.optString("error", "Failed to update student: Unknown error."));
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

    public void deleteStudent(String authToken, String studentId, ApiCallback<Void> callback) {
        new HttpRequestTask(BASE_URL + "/data?id=" + studentId + "&collection=students", "DELETE", null, authToken, new ApiCallback<String>() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject jsonResponse = new JSONObject(response);
                    if (jsonResponse.optBoolean("success", false)) {
                        callback.onSuccess(null); // No specific result needed
                    } else {
                        callback.onError(jsonResponse.optString("error", "Failed to delete student: Unknown error."));
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
        private int responseCode;

        public HttpRequestTask(String urlString, String method, String requestBody, String authToken, ApiCallback<String> callback) {
            this.urlString = urlString;
            this.method = method;
            this.requestBody = requestBody;
            this.authToken = authToken;
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

                responseCode = urlConnection.getResponseCode();
                if (responseCode >= 200 && responseCode < 300) { // HTTP_OK, HTTP_CREATED, etc.
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
                    
                    try {
                        JSONObject errorJson = new JSONObject(rawError);
                        errorMessage = "HTTP Error " + responseCode + ": " + errorJson.optString("error", "Unknown API error.");
                    } catch (JSONException e) {
                        errorMessage = "HTTP Error " + responseCode + ": " + rawError;
                    }
                    return null;
                }
            } catch (Exception e) {
                errorMessage = e.getMessage();
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
                callback.onError(errorMessage != null ? errorMessage : "Unknown network error occurred.");
            }
        }
    }
}