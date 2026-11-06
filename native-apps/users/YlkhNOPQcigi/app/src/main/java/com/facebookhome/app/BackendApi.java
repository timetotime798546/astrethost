package com.facebookhome.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class BackendApi {

    private static final String API_BASE_URL = "https://trumpledroid-api.cloudbeta28624.workers.dev";
    private static final String PREF_NAME = "FBHomePrefs";
    private static final String KEY_TOKEN = "auth_token";
    private static final String KEY_EMAIL = "user_email";

    private Context context;
    private Handler mainHandler;

    public interface ApiCallback {
        void onSuccess(JSONObject response);
        void onError(String errorMessage);
    }

    public BackendApi(Context context) {
        this.context = context.getApplicationContext();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public static String getAppId(Context context) {
        try {
            InputStream is = context.getAssets().open("app-meta.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String jsonStr = new String(buffer, "UTF-8");
            JSONObject obj = new JSONObject(jsonStr);
            return obj.optString("app_id", "app_6aeddf63bedb3");
        } catch (Exception e) {
            return "app_6aeddf63bedb3";
        }
    }

    public void saveAuthToken(String token, String email) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_TOKEN, token).putString(KEY_EMAIL, email).apply();
    }

    public String getAuthToken() {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_TOKEN, null);
    }

    public String getUserEmail() {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_EMAIL, "Facebook User");
    }

    public void clearAuth() {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
    }

    public void register(final String email, final String password, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject body = new JSONObject();
                    body.put("app_id", getAppId(context));
                    body.put("email", email);
                    body.put("password", password);

                    String responseStr = makeHttpRequest("/register", "POST", body.toString(), false);
                    final JSONObject responseJson = new JSONObject(responseStr);

                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (responseJson.optBoolean("success", false)) {
                                callback.onSuccess(responseJson);
                            } else {
                                callback.onError(responseJson.optString("error", "Registration failed"));
                            }
                        }
                    });
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

    public void login(final String email, final String password, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject body = new JSONObject();
                    body.put("app_id", getAppId(context));
                    body.put("email", email);
                    body.put("password", password);

                    String responseStr = makeHttpRequest("/login", "POST", body.toString(), false);
                    final JSONObject responseJson = new JSONObject(responseStr);

                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (responseJson.optBoolean("success", false)) {
                                String token = responseJson.optString("token", "");
                                if (!token.isEmpty()) {
                                    saveAuthToken(token, email);
                                }
                                callback.onSuccess(responseJson);
                            } else {
                                callback.onError(responseJson.optString("error", "Login failed"));
                            }
                        }
                    });
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

    public void getPosts(final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String responseStr = makeHttpRequest("/data?collection=posts", "GET", null, true);
                    final JSONObject responseJson = new JSONObject(responseStr);

                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (responseJson.optBoolean("success", false)) {
                                callback.onSuccess(responseJson);
                            } else {
                                callback.onError(responseJson.optString("error", "Failed to fetch posts"));
                            }
                        }
                    });
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

    public void createPost(final String author, final String content, final String imageUrl, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject dataObj = new JSONObject();
                    dataObj.put("author", author);
                    dataObj.put("content", content);
                    dataObj.put("timestamp", "Just now");
                    dataObj.put("likes", 0);
                    dataObj.put("image_url", imageUrl);

                    JSONObject body = new JSONObject();
                    body.put("collection", "posts");
                    body.put("data", dataObj);

                    String responseStr = makeHttpRequest("/data", "POST", body.toString(), true);
                    final JSONObject responseJson = new JSONObject(responseStr);

                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (responseJson.optBoolean("success", false)) {
                                callback.onSuccess(responseJson);
                            } else {
                                callback.onError(responseJson.optString("error", "Failed to create post"));
                            }
                        }
                    });
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

    public void updatePostLikes(final String recordId, final JSONObject updatedData, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject body = new JSONObject();
                    body.put("id", recordId);
                    body.put("data", updatedData);

                    String responseStr = makeHttpRequest("/data", "PUT", body.toString(), true);
                    final JSONObject responseJson = new JSONObject(responseStr);

                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (responseJson.optBoolean("success", false)) {
                                callback.onSuccess(responseJson);
                            } else {
                                callback.onError(responseJson.optString("error", "Failed to update post"));
                            }
                        }
                    });
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

    public void deletePost(final String recordId, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String responseStr = makeHttpRequest("/data?id=" + recordId, "DELETE", null, true);
                    final JSONObject responseJson = new JSONObject(responseStr);

                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (responseJson.optBoolean("success", false)) {
                                callback.onSuccess(responseJson);
                            } else {
                                callback.onError(responseJson.optString("error", "Failed to delete post"));
                            }
                        }
                    });
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

    private String makeHttpRequest(String path, String method, String payload, boolean useAuth) throws Exception {
        URL url = new URL(API_BASE_URL + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setRequestProperty("Content-Type", "application/json");

        if (useAuth) {
            String token = getAuthToken();
            if (token != null && !token.isEmpty()) {
                conn.setRequestProperty("Authorization", "Bearer " + token);
            }
        }

        if (payload != null && (method.equals("POST") || method.equals("PUT"))) {
            conn.setDoOutput(true);
            OutputStream os = conn.getOutputStream();
            os.write(payload.getBytes("UTF-8"));
            os.flush();
            os.close();
        }

        int responseCode = conn.getResponseCode();
        InputStream is = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream();

        BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        br.close();
        conn.disconnect();
        return sb.toString();
    }
}