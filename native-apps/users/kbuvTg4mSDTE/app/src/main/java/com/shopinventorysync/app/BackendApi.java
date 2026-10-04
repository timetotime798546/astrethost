package com.shopinventorysync.app;

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

public class BackendApi {

    private static final String PREFS_NAME = "AuthPrefs";
    private static final String KEY_TOKEN = "bearer_token";
    private static final String BASE_URL = "https://trumpledroid-api.cloudbeta28624.workers.dev";

    private final Context context;
    private final String appId;
    private final Handler mainHandler;

    public interface ApiCallback<T> {
        void onSuccess(T result);
        void onError(String error);
    }

    public BackendApi(Context context) {
        this.context = context.getApplicationContext();
        this.appId = "com.shopinventorysync.app"; // Matches package & app-meta
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    private void saveToken(String token) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_TOKEN, token).apply();
    }

    public String getToken() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_TOKEN, "");
    }

    public void logout() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_TOKEN).apply();
    }

    public boolean isLoggedIn() {
        return !getToken().isEmpty();
    }

    // AUTH: Register
    public void register(final String email, final String password, final ApiCallback<String> callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(BASE_URL + "/register");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);

                    JSONObject payload = new JSONObject();
                    payload.put("app_id", appId);
                    payload.put("email", email);
                    payload.put("password", password);

                    OutputStream os = conn.getOutputStream();
                    os.write(payload.toString().getBytes("UTF-8"));
                    os.close();

                    int responseCode = conn.getResponseCode();
                    InputStream is = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    reader.close();

                    final JSONObject responseJson = new JSONObject(sb.toString());
                    if (responseCode >= 200 && responseCode < 300 && responseJson.optBoolean("success", false)) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess("Registration Successful!");
                            }
                        });
                    } else {
                        final String errMsg = responseJson.optString("error", "Registration failed");
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onError(errMsg);
                            }
                        });
                    }
                } catch (final Exception e) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onError(e.getMessage());
                        }
                    });
                }
            }
        }).start();
    }

    // AUTH: Login
    public void login(final String email, final String password, final ApiCallback<String> callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(BASE_URL + "/login");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);

                    JSONObject payload = new JSONObject();
                    payload.put("app_id", appId);
                    payload.put("email", email);
                    payload.put("password", password);

                    OutputStream os = conn.getOutputStream();
                    os.write(payload.toString().getBytes("UTF-8"));
                    os.close();

                    int responseCode = conn.getResponseCode();
                    InputStream is = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    reader.close();

                    final JSONObject responseJson = new JSONObject(sb.toString());
                    if (responseCode >= 200 && responseCode < 300 && responseJson.optBoolean("success", false)) {
                        final String token = responseJson.optString("token", "");
                        if (!token.isEmpty()) {
                            saveToken(token);
                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    callback.onSuccess(token);
                                }
                            });
                        } else {
                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    callback.onError("No token returned from server.");
                                }
                            });
                        }
                    } else {
                        final String errMsg = responseJson.optString("error", "Authentication failed");
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onError(errMsg);
                            }
                        });
                    }
                } catch (final Exception e) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onError(e.getMessage());
                        }
                    });
                }
            }
        }).start();
    }

    // OTP: Request OTP
    public void requestOtp(final String email, final ApiCallback<String> callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(BASE_URL + "/request-otp");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);

                    JSONObject payload = new JSONObject();
                    payload.put("app_id", appId);
                    payload.put("email", email);

                    OutputStream os = conn.getOutputStream();
                    os.write(payload.toString().getBytes("UTF-8"));
                    os.close();

                    int responseCode = conn.getResponseCode();
                    InputStream is = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    reader.close();

                    final JSONObject responseJson = new JSONObject(sb.toString());
                    if (responseCode >= 200 && responseCode < 300 && responseJson.optBoolean("success", false)) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess("OTP code sent to email successfully!");
                            }
                        });
                    } else {
                        final String errMsg = responseJson.optString("error", "Failed to send OTP code.");
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onError(errMsg);
                            }
                        });
                    }
                } catch (final Exception e) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onError(e.getMessage());
                        }
                    });
                }
            }
        }).start();
    }

    // OTP: Reset Password
    public void resetPassword(final String email, final String otp, final String newPassword, final ApiCallback<String> callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(BASE_URL + "/reset-password");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);

                    JSONObject payload = new JSONObject();
                    payload.put("app_id", appId);
                    payload.put("email", email);
                    payload.put("otp", otp);
                    payload.put("new_password", newPassword);

                    OutputStream os = conn.getOutputStream();
                    os.write(payload.toString().getBytes("UTF-8"));
                    os.close();

                    int responseCode = conn.getResponseCode();
                    InputStream is = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    reader.close();

                    final JSONObject responseJson = new JSONObject(sb.toString());
                    if (responseCode >= 200 && responseCode < 300 && responseJson.optBoolean("success", false)) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess("Password reset successfully. Please log in.");
                            }
                        });
                    } else {
                        final String errMsg = responseJson.optString("error", "Failed to reset password.");
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onError(errMsg);
                            }
                        });
                    }
                } catch (final Exception e) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onError(e.getMessage());
                        }
                    });
                }
            }
        }).start();
    }

    // CRUD: Create Record
    public void createRecord(final String collection, final JSONObject dataFields, final ApiCallback<JSONObject> callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(BASE_URL + "/data");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setRequestProperty("Authorization", "Bearer " + getToken());
                    conn.setDoOutput(true);

                    JSONObject payload = new JSONObject();
                    payload.put("collection", collection);
                    payload.put("data", dataFields);

                    OutputStream os = conn.getOutputStream();
                    os.write(payload.toString().getBytes("UTF-8"));
                    os.close();

                    int responseCode = conn.getResponseCode();
                    InputStream is = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    reader.close();

                    final JSONObject responseJson = new JSONObject(sb.toString());
                    if (responseCode >= 200 && responseCode < 300 && responseJson.optBoolean("success", false)) {
                        final JSONObject record = responseJson.getJSONObject("record");
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess(record);
                            }
                        });
                    } else {
                        final String error = responseJson.optString("error", "Error creating record");
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onError(error);
                            }
                        });
                    }
                } catch (final Exception e) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onError(e.getMessage());
                        }
                    });
                }
            }
        }).start();
    }

    // CRUD: Read Records
    public void getRecords(final String collection, final ApiCallback<JSONArray> callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(BASE_URL + "/data?collection=" + collection);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("Authorization", "Bearer " + getToken());

                    int responseCode = conn.getResponseCode();
                    InputStream is = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    reader.close();

                    final JSONObject responseJson = new JSONObject(sb.toString());
                    if (responseCode >= 200 && responseCode < 300 && responseJson.optBoolean("success", false)) {
                        final JSONArray records = responseJson.getJSONArray("records");
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess(records);
                            }
                        });
                    } else {
                        final String error = responseJson.optString("error", "Error reading records");
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onError(error);
                            }
                        });
                    }
                } catch (final Exception e) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onError(e.getMessage());
                        }
                    });
                }
            }
        }).start();
    }

    // CRUD: Update Record
    public void updateRecord(final String recordId, final JSONObject dataFields, final ApiCallback<String> callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(BASE_URL + "/data");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("PUT");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setRequestProperty("Authorization", "Bearer " + getToken());
                    conn.setDoOutput(true);

                    JSONObject payload = new JSONObject();
                    payload.put("id", recordId);
                    payload.put("data", dataFields);

                    OutputStream os = conn.getOutputStream();
                    os.write(payload.toString().getBytes("UTF-8"));
                    os.close();

                    int responseCode = conn.getResponseCode();
                    InputStream is = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    reader.close();

                    final JSONObject responseJson = new JSONObject(sb.toString());
                    if (responseCode >= 200 && responseCode < 300 && responseJson.optBoolean("success", false)) {
                        final String updatedId = responseJson.optString("id", recordId);
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess(updatedId);
                            }
                        });
                    } else {
                        final String error = responseJson.optString("error", "Error updating record");
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onError(error);
                            }
                        });
                    }
                } catch (final Exception e) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onError(e.getMessage());
                        }
                    });
                }
            }
        }).start();
    }

    // CRUD: Delete Record
    public void deleteRecord(final String recordId, final ApiCallback<String> callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(BASE_URL + "/data?id=" + recordId);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("DELETE");
                    conn.setRequestProperty("Authorization", "Bearer " + getToken());

                    int responseCode = conn.getResponseCode();
                    InputStream is = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    reader.close();

                    final JSONObject responseJson = new JSONObject(sb.toString());
                    if (responseCode >= 200 && responseCode < 300 && responseJson.optBoolean("success", false)) {
                        final String deletedId = responseJson.optString("id", recordId);
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess(deletedId);
                            }
                        });
                    } else {
                        final String error = responseJson.optString("error", "Error deleting record");
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onError(error);
                            }
                        });
                    }
                } catch (final Exception e) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onError(e.getMessage());
                        }
                    });
                }
            }
        }).start();
    }
}