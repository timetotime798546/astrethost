package com.studentmanager.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class BackendApi {
    private final Context context;
    private final String apiBaseUrl = "https://trumpledroid-api.cloudbeta28624.workers.dev";
    private String appId;

    public BackendApi(Context context) {
        this.context = context;
        this.appId = readAppIdFromAssets(context);
    }

    private String readAppIdFromAssets(Context ctx) {
        try {
            InputStream is = ctx.getAssets().open("app-meta.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String jsonStr = new String(buffer, "UTF-8");
            JSONObject json = new JSONObject(jsonStr);
            String foundId = json.optString("package_name", "");
            if (foundId.isEmpty()) {
                foundId = json.optString("app_id", "com.studentmanager.app");
            }
            return foundId;
        } catch (Exception e) {
            e.printStackTrace();
            return "com.studentmanager.app";
        }
    }

    private SharedPreferences getPrefs() {
        return context.getSharedPreferences("StudentPrefs", Context.MODE_PRIVATE);
    }

    public void saveToken(String token) {
        getPrefs().edit().putString("auth_token", token).apply();
    }

    public String getStoredToken() {
        return getPrefs().getString("auth_token", null);
    }

    public void clearToken() {
        getPrefs().edit().remove("auth_token").apply();
    }

    public boolean isLoggedIn() {
        String token = getStoredToken();
        return token != null && !token.isEmpty();
    }

    private void performRequest(final String endpoint, final String method, final String jsonBody, final boolean authenticated, final ApiCallback<String> callback) {
        final Handler mainHandler = new Handler(Looper.getMainLooper());
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection conn = null;
                try {
                    URL url = new URL(apiBaseUrl + endpoint);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod(method);
                    conn.setConnectTimeout(15000);
                    conn.setReadTimeout(15000);
                    conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                    conn.setRequestProperty("Accept", "application/json");

                    if (authenticated) {
                        String token = getStoredToken();
                        if (token != null && !token.isEmpty()) {
                            conn.setRequestProperty("Authorization", "Bearer " + token);
                        }
                    }

                    if (jsonBody != null && !jsonBody.isEmpty()) {
                        conn.setDoOutput(true);
                        OutputStream os = conn.getOutputStream();
                        os.write(jsonBody.getBytes("UTF-8"));
                        os.close();
                    }

                    final int responseCode = conn.getResponseCode();
                    InputStream is;
                    if (responseCode >= 200 && responseCode < 300) {
                        is = conn.getInputStream();
                    } else {
                        is = conn.getErrorStream();
                    }

                    if (is == null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onError("Server returned code " + responseCode + " with no details.");
                            }
                        });
                        return;
                    }

                    BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    reader.close();
                    is.close();

                    final String result = sb.toString();

                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                JSONObject jsonResponse = new JSONObject(result);
                                boolean success = jsonResponse.optBoolean("success", false);
                                if (success || responseCode < 300) {
                                    callback.onSuccess(result);
                                } else {
                                    String errorMsg = jsonResponse.optString("error", jsonResponse.optString("message", "An error occurred."));
                                    callback.onError(errorMsg);
                                }
                            } catch (Exception e) {
                                callback.onError("Parsing error or action failed: " + e.getMessage());
                            }
                        }
                    });

                } catch (final Exception e) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onError("Connection error: " + e.getLocalizedMessage());
                        }
                    });
                } finally {
                    if (conn != null) {
                        conn.disconnect();
                    }
                }
            }
        }).start();
    }

    public void register(String email, String password, final ApiCallback<String> callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("app_id", appId);
            body.put("email", email);
            body.put("password", password);

            performRequest("/register", "POST", body.toString(), false, callback);
        } catch (Exception e) {
            callback.onError("Failed to build request: " + e.getMessage());
        }
    }

    public void login(final String email, final String password, final ApiCallback<String> callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("app_id", appId);
            body.put("email", email);
            body.put("password", password);

            performRequest("/login", "POST", body.toString(), false, new ApiCallback<String>() {
                @Override
                public void onSuccess(String result) {
                    try {
                        JSONObject json = new JSONObject(result);
                        String token = json.optString("token", "");
                        if (!token.isEmpty()) {
                            saveToken(token);
                            callback.onSuccess(result);
                        } else {
                            callback.onError("Login succeeded but no token received.");
                        }
                    } catch (Exception e) {
                        callback.onError("Failed to parse login response.");
                    }
                }

                @Override
                public void onError(String errorMsg) {
                    callback.onError(errorMsg);
                }
            });
        } catch (Exception e) {
            callback.onError("Failed to build request: " + e.getMessage());
        }
    }

    public void logout(final ApiCallback<String> callback) {
        performRequest("/logout", "POST", "{}", true, new ApiCallback<String>() {
            @Override
            public void onSuccess(String result) {
                clearToken();
                callback.onSuccess(result);
            }

            @Override
            public void onError(String errorMsg) {
                clearToken();
                callback.onError(errorMsg);
            }
        });
    }

    public void createStudent(Student student, final ApiCallback<Student> callback) {
        try {
            JSONObject dataObj = new JSONObject();
            dataObj.put("name", student.getName());
            dataObj.put("roll_no", student.getRollNo());
            dataObj.put("grade", student.getGrade());
            dataObj.put("attendance_present", student.getAttendancePresent());
            dataObj.put("attendance_total", student.getAttendanceTotal());
            dataObj.put("marks_obtained", student.getMarksObtained());
            dataObj.put("marks_total", student.getMarksTotal());

            JSONObject body = new JSONObject();
            body.put("collection", "students");
            body.put("data", dataObj);

            performRequest("/data", "POST", body.toString(), true, new ApiCallback<String>() {
                @Override
                public void onSuccess(String result) {
                    try {
                        JSONObject json = new JSONObject(result);
                        JSONObject record = json.getJSONObject("record");
                        String id = record.getString("id");
                        JSONObject data = record.getJSONObject("data");
                        
                        Student s = new Student(
                            id,
                            data.getString("name"),
                            data.getString("roll_no"),
                            data.getString("grade"),
                            data.getInt("attendance_present"),
                            data.getInt("attendance_total"),
                            data.getInt("marks_obtained"),
                            data.getInt("marks_total")
                        );
                        callback.onSuccess(s);
                    } catch (Exception e) {
                        callback.onError("Parse failure: " + e.getMessage());
                    }
                }

                @Override
                public void onError(String errorMsg) {
                    callback.onError(errorMsg);
                }
            });
        } catch (Exception e) {
            callback.onError("Failed to prepare create request: " + e.getMessage());
        }
    }

    public void getStudents(final ApiCallback<List<Student>> callback) {
        performRequest("/data?collection=students", "GET", null, true, new ApiCallback<String>() {
            @Override
            public void onSuccess(String result) {
                try {
                    JSONObject json = new JSONObject(result);
                    JSONArray records = json.getJSONArray("records");
                    List<Student> studentsList = new ArrayList<>();
                    for (int i = 0; i < records.length(); i++) {
                        JSONObject record = records.getJSONObject(i);
                        String id = record.getString("id");
                        JSONObject data = record.getJSONObject("data");

                        Student s = new Student(
                            id,
                            data.optString("name", ""),
                            data.optString("roll_no", ""),
                            data.optString("grade", ""),
                            data.optInt("attendance_present", 0),
                            data.optInt("attendance_total", 0),
                            data.optInt("marks_obtained", 0),
                            data.optInt("marks_total", 0)
                        );
                        studentsList.add(s);
                    }
                    callback.onSuccess(studentsList);
                } catch (Exception e) {
                    callback.onError("Parse failure: " + e.getMessage());
                }
            }

            @Override
            public void onError(String errorMsg) {
                callback.onError(errorMsg);
            }
        });
    }

    public void updateStudent(Student student, final ApiCallback<String> callback) {
        try {
            JSONObject dataObj = new JSONObject();
            dataObj.put("name", student.getName());
            dataObj.put("roll_no", student.getRollNo());
            dataObj.put("grade", student.getGrade());
            dataObj.put("attendance_present", student.getAttendancePresent());
            dataObj.put("attendance_total", student.getAttendanceTotal());
            dataObj.put("marks_obtained", student.getMarksObtained());
            dataObj.put("marks_total", student.getMarksTotal());

            JSONObject body = new JSONObject();
            body.put("id", student.getId());
            body.put("data", dataObj);

            performRequest("/data", "PUT", body.toString(), true, new ApiCallback<String>() {
                @Override
                public void onSuccess(String result) {
                    try {
                        JSONObject json = new JSONObject(result);
                        String id = json.getString("id");
                        callback.onSuccess(id);
                    } catch (Exception e) {
                        callback.onError("Parse failure: " + e.getMessage());
                    }
                }

                @Override
                public void onError(String errorMsg) {
                    callback.onError(errorMsg);
                }
            });
        } catch (Exception e) {
            callback.onError("Failed to prepare update request: " + e.getMessage());
        }
    }

    public void deleteStudent(String studentId, final ApiCallback<String> callback) {
        performRequest("/data?id=" + studentId, "DELETE", null, true, new ApiCallback<String>() {
            @Override
            public void onSuccess(String result) {
                try {
                    JSONObject json = new JSONObject(result);
                    String id = json.getString("id");
                    callback.onSuccess(id);
                } catch (Exception e) {
                    callback.onError("Parse failure: " + e.getMessage());
                }
            }

            @Override
            public void onError(String errorMsg) {
                callback.onError(errorMsg);
            }
        });
    }
}