package com.studentmanagement.app;

import android.os.AsyncTask;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class BackendApi {

    private static final String BASE_URL = "https://trumpledroid-api.cloudbeta28624.workers.dev";
    private static final String APP_ID = "ai-student-management"; // Replace with actual app_id if provided by build system.

    public interface ApiCallback<T> {
        void onSuccess(T result);
        void onError(String error);
    }

    // --- Authentication ---

    public void login(String email, String password, ApiCallback<String> callback) {
        JSONObject postData = new JSONObject();
        try {
            postData.put("email", email);
            postData.put("password", password);
        } catch (JSONException e) {
            callback.onError(e.getMessage());
            return;
        }
        new HttpRequestTask(BASE_URL + "/auth/login?app_id=" + APP_ID, "POST", postData.toString(), null, new ApiCallback<String>() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject jsonResponse = new JSONObject(response);
                    String token = jsonResponse.getString("token");
                    callback.onSuccess(token);
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
        JSONObject postData = new JSONObject();
        try {
            postData.put("email", email);
            postData.put("password", password);
        } catch (JSONException e) {
            callback.onError(e.getMessage());
            return;
        }
        new HttpRequestTask(BASE_URL + "/auth/register?app_id=" + APP_ID, "POST", postData.toString(), null, new ApiCallback<String>() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject jsonResponse = new JSONObject(response);
                    String token = jsonResponse.getString("token");
                    callback.onSuccess(token);
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
        new HttpRequestTask(BASE_URL + "/collection/students?app_id=" + APP_ID, "GET", null, authToken, new ApiCallback<String>() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONArray jsonArray = new JSONArray(response);
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
        JSONObject postData = new JSONObject();
        try {
            postData.put("name", student.getName());
            postData.put("phone", student.getPhone());
            postData.put("className", student.getClassName());
            postData.put("rollNumber", student.getRollNumber());
        } catch (JSONException e) {
            callback.onError(e.getMessage());
            return;
        }
        new HttpRequestTask(BASE_URL + "/collection/students?app_id=" + APP_ID, "POST", postData.toString(), authToken, new ApiCallback<String>() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject jsonResponse = new JSONObject(response);
                    String id = jsonResponse.getString("_id");
                    student.setId(id); // Assign ID received from backend
                    callback.onSuccess(student);
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
        JSONObject putData = new JSONObject();
        try {
            putData.put("name", student.getName());
            putData.put("phone", student.getPhone());
            putData.put("className", student.getClassName());
            putData.put("rollNumber", student.getRollNumber());
        } catch (JSONException e) {
            callback.onError(e.getMessage());
            return;
        }
        new HttpRequestTask(BASE_URL + "/collection/students/" + student.getId() + "?app_id=" + APP_ID, "PUT", putData.toString(), authToken, new ApiCallback<String>() {
            @Override
            public void onSuccess(String response) {
                callback.onSuccess(student); // Assuming success means update was applied
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        }).execute();
    }

    public void deleteStudent(String authToken, String studentId, ApiCallback<Void> callback) {
        new HttpRequestTask(BASE_URL + "/collection/students/" + studentId + "?app_id=" + APP_ID, "DELETE", null, authToken, new ApiCallback<String>() {
            @Override
            public void onSuccess(String response) {
                callback.onSuccess(null); // No specific result needed
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

                int responseCode = urlConnection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
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
                    errorMessage = "HTTP Error " + responseCode + ": " + errorResponse.toString();
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
                callback.onError(errorMessage != null ? errorMessage : "Unknown error occurred.");
            }
        }
    }
}