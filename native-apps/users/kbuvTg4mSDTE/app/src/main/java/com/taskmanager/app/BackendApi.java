package com.taskmanager.app;

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

    private static final String PREF_NAME = "task_manager_prefs";
    private static final String KEY_TOKEN = "token";
    
    private final String apiBaseUrl = "https://trumpledroid-api.cloudbeta28624.workers.dev";
    private final String appId;
    private final Context context;
    private final Handler mainHandler;

    public interface ApiCallback<T> {
        void onSuccess(T result);
        void onError(String error);
    }

    public BackendApi(Context context) {
        this.context = context.getApplicationContext();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.appId = loadAppIdFromAssets();
    }

    private String loadAppIdFromAssets() {
        try {
            InputStream is = context.getAssets().open("app-meta.json");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            JSONObject json = new JSONObject(sb.toString());
            if (json.has("app_id")) {
                return json.getString("app_id");
            }
            return json.optString("package_name", "com.taskmanager.app");
        } catch (Exception e) {
            return "com.taskmanager.app";
        }
    }

    public void saveToken(String token) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_TOKEN, token).apply();
    }

    public String getToken() {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_TOKEN, null);
    }

    public void logout() {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_TOKEN).apply();
    }

    public boolean isLoggedIn() {
        return getToken() != null;
    }

    public void register(final String email, final String password, final ApiCallback<Boolean> callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(apiBaseUrl + "/register");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);

                    JSONObject body = new JSONObject();
                    body.put("app_id", appId);
                    body.put("email", email);
                    body.put("password", password);

                    OutputStream os = conn.getOutputStream();
                    os.write(body.toString().getBytes("UTF-8"));
                    os.flush();
                    os.close();

                    int responseCode = conn.getResponseCode();
                    InputStream is = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream();
                    BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    StringBuilder res = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        res.append(line);
                    }
                    br.close();

                    final JSONObject jsonResponse = new JSONObject(res.toString());
                    final boolean success = jsonResponse.optBoolean("success", false);

                    if (success) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess(true);
                            }
                        });
                    } else {
                        final String errorMsg = jsonResponse.optString("error", "Registration failed");
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onError(errorMsg);
                            }
                        });
                    }

                } catch (final Exception e) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onError("Network error: " + e.getMessage());
                        }
                    });
                }
            }
        }).start();
    }

    public void login(final String email, final String password, final ApiCallback<String> callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(apiBaseUrl + "/login");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);

                    JSONObject body = new JSONObject();
                    body.put("app_id", appId);
                    body.put("email", email);
                    body.put("password", password);

                    OutputStream os = conn.getOutputStream();
                    os.write(body.toString().getBytes("UTF-8"));
                    os.flush();
                    os.close();

                    int responseCode = conn.getResponseCode();
                    InputStream is = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream();
                    BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    StringBuilder res = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        res.append(line);
                    }
                    br.close();

                    final JSONObject jsonResponse = new JSONObject(res.toString());
                    final boolean success = jsonResponse.optBoolean("success", false);

                    if (success) {
                        final String token = jsonResponse.getString("token");
                        saveToken(token);
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess(token);
                            }
                        });
                    } else {
                        final String errorMsg = jsonResponse.optString("error", "Login failed");
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onError(errorMsg);
                            }
                        });
                    }

                } catch (final Exception e) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onError("Network error: " + e.getMessage());
                        }
                    });
                }
            }
        }).start();
    }

    public void requestOtp(final String email, final ApiCallback<Boolean> callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(apiBaseUrl + "/request-otp");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);

                    JSONObject body = new JSONObject();
                    body.put("app_id", appId);
                    body.put("email", email);

                    OutputStream os = conn.getOutputStream();
                    os.write(body.toString().getBytes("UTF-8"));
                    os.flush();
                    os.close();

                    int responseCode = conn.getResponseCode();
                    InputStream is = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream();
                    BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    StringBuilder res = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        res.append(line);
                    }
                    br.close();

                    final JSONObject jsonResponse = new JSONObject(res.toString());
                    final boolean success = jsonResponse.optBoolean("success", false);

                    if (success) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess(true);
                            }
                        });
                    } else {
                        final String errorMsg = jsonResponse.optString("error", "Failed to request OTP code");
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onError(errorMsg);
                            }
                        });
                    }

                } catch (final Exception e) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onError("Network error: " + e.getMessage());
                        }
                    });
                }
            }
        }).start();
    }

    public void resetPassword(final String email, final String otp, final String newPassword, final ApiCallback<Boolean> callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(apiBaseUrl + "/reset-password");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);

                    JSONObject body = new JSONObject();
                    body.put("app_id", appId);
                    body.put("email", email);
                    body.put("otp", otp);
                    body.put("new_password", newPassword);

                    OutputStream os = conn.getOutputStream();
                    os.write(body.toString().getBytes("UTF-8"));
                    os.flush();
                    os.close();

                    int responseCode = conn.getResponseCode();
                    InputStream is = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream();
                    BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    StringBuilder res = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        res.append(line);
                    }
                    br.close();

                    final JSONObject jsonResponse = new JSONObject(res.toString());
                    final boolean success = jsonResponse.optBoolean("success", false);

                    if (success) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess(true);
                            }
                        });
                    } else {
                        final String errorMsg = jsonResponse.optString("error", "Failed to reset password");
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onError(errorMsg);
                            }
                        });
                    }

                } catch (final Exception e) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onError("Network error: " + e.getMessage());
                        }
                    });
                }
            }
        }).start();
    }

    public void createTask(final String title, final ApiCallback<Task> callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(apiBaseUrl + "/data");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setRequestProperty("Authorization", "Bearer " + getToken());
                    conn.setDoOutput(true);

                    JSONObject taskData = new JSONObject();
                    taskData.put("title", title);
                    taskData.put("status", "pending");

                    JSONObject body = new JSONObject();
                    body.put("collection", "tasks");
                    body.put("data", taskData);

                    OutputStream os = conn.getOutputStream();
                    os.write(body.toString().getBytes("UTF-8"));
                    os.flush();
                    os.close();

                    int responseCode = conn.getResponseCode();
                    InputStream is = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream();
                    BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    StringBuilder res = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        res.append(line);
                    }
                    br.close();

                    final JSONObject jsonResponse = new JSONObject(res.toString());
                    final boolean success = jsonResponse.optBoolean("success", false);

                    if (success) {
                        JSONObject record = jsonResponse.getJSONObject("record");
                        String recordId = record.getString("id");
                        JSONObject innerData = record.getJSONObject("data");
                        String savedTitle = innerData.getString("title");
                        String savedStatus = innerData.getString("status");

                        final Task newTask = new Task(recordId, savedTitle, savedStatus);
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess(newTask);
                            }
                        });
                    } else {
                        final String errorMsg = jsonResponse.optString("error", "Failed to create task");
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onError(errorMsg);
                            }
                        });
                    }

                } catch (final Exception e) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onError("Network error: " + e.getMessage());
                        }
                    });
                }
            }
        }).start();
    }

    public void fetchTasks(final ApiCallback<List<Task>> callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(apiBaseUrl + "/data?collection=tasks");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("Authorization", "Bearer " + getToken());

                    int responseCode = conn.getResponseCode();
                    InputStream is = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream();
                    BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    StringBuilder res = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        res.append(line);
                    }
                    br.close();

                    final JSONObject jsonResponse = new JSONObject(res.toString());
                    final boolean success = jsonResponse.optBoolean("success", false);

                    if (success) {
                        JSONArray records = jsonResponse.getJSONArray("records");
                        final List<Task> taskList = new ArrayList<>();
                        for (int i = 0; i < records.length(); i++) {
                            JSONObject record = records.getJSONObject(i);
                            String recordId = record.getString("id");
                            JSONObject innerData = record.getJSONObject("data");
                            String title = innerData.optString("title", "No Title");
                            String status = innerData.optString("status", "pending");
                            taskList.add(new Task(recordId, title, status));
                        }

                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess(taskList);
                            }
                        });
                    } else {
                        final String errorMsg = jsonResponse.optString("error", "Failed to load tasks");
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onError(errorMsg);
                            }
                        });
                    }

                } catch (final Exception e) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onError("Network error: " + e.getMessage());
                        }
                    });
                }
            }
        }).start();
    }

    public void updateTaskStatus(final String taskId, final String title, final String status, final ApiCallback<Boolean> callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(apiBaseUrl + "/data");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("PUT");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setRequestProperty("Authorization", "Bearer " + getToken());
                    conn.setDoOutput(true);

                    JSONObject taskData = new JSONObject();
                    taskData.put("title", title);
                    taskData.put("status", status);

                    JSONObject body = new JSONObject();
                    body.put("id", taskId);
                    body.put("data", taskData);

                    OutputStream os = conn.getOutputStream();
                    os.write(body.toString().getBytes("UTF-8"));
                    os.flush();
                    os.close();

                    int responseCode = conn.getResponseCode();
                    InputStream is = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream();
                    BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    StringBuilder res = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        res.append(line);
                    }
                    br.close();

                    final JSONObject jsonResponse = new JSONObject(res.toString());
                    final boolean success = jsonResponse.optBoolean("success", false);

                    if (success) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess(true);
                            }
                        });
                    } else {
                        final String errorMsg = jsonResponse.optString("error", "Update failed");
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onError(errorMsg);
                            }
                        });
                    }

                } catch (final Exception e) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onError("Network error: " + e.getMessage());
                        }
                    });
                }
            }
        }).start();
    }

    public void deleteTask(final String taskId, final ApiCallback<Boolean> callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(apiBaseUrl + "/data?id=" + taskId);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("DELETE");
                    conn.setRequestProperty("Authorization", "Bearer " + getToken());

                    int responseCode = conn.getResponseCode();
                    InputStream is = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream();
                    BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    StringBuilder res = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        res.append(line);
                    }
                    br.close();

                    final JSONObject jsonResponse = new JSONObject(res.toString());
                    final boolean success = jsonResponse.optBoolean("success", false);

                    if (success) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess(true);
                            }
                        });
                    } else {
                        final String errorMsg = jsonResponse.optString("error", "Deletion failed");
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onError(errorMsg);
                            }
                        });
                    }

                } catch (final Exception e) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onError("Network error: " + e.getMessage());
                        }
                    });
                }
            }
        }).start();
    }
}