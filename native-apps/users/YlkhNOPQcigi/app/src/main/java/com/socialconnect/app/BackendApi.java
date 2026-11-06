package com.socialconnect.app;

import android.content.Context;
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

    private static final String BASE_URL = "https://trumpledroid-api.cloudbeta28624.workers.dev";
    private Context context;
    private SessionManager sessionManager;
    private Handler mainHandler;

    public interface ApiCallback {
        void onSuccess(JSONObject response);
        void onError(String errorMessage);
    }

    public interface PostsCallback {
        void onSuccess(List<PostModel> posts);
        void onError(String errorMessage);
    }

    public BackendApi(Context context) {
        this.context = context;
        this.sessionManager = new SessionManager(context);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    private String getAppId() {
        try {
            InputStream is = context.getAssets().open("app-meta.json");
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();
            String jsonStr = new String(buffer, "UTF-8");
            JSONObject obj = new JSONObject(jsonStr);
            return obj.optString("app_id", "social_connect_app_v1");
        } catch (Exception e) {
            return "social_connect_app_v1";
        }
    }

    public void register(final String email, final String password, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject body = new JSONObject();
                    body.put("app_id", getAppId());
                    body.put("email", email);
                    body.put("password", password);

                    String result = executePostRequest(BASE_URL + "/register", body.toString(), null);
                    final JSONObject response = new JSONObject(result);
                    
                    if (response.optBoolean("success", false) || response.has("message") || response.has("user")) {
                        postSuccess(callback, response);
                    } else {
                        postError(callback, response.optString("error", "Registration failed"));
                    }
                } catch (Exception e) {
                    postError(callback, "Error: " + e.getLocalizedMessage());
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
                    body.put("app_id", getAppId());
                    body.put("email", email);
                    body.put("password", password);

                    String result = executePostRequest(BASE_URL + "/login", body.toString(), null);
                    final JSONObject response = new JSONObject(result);

                    if (response.optBoolean("success", false) && response.has("token")) {
                        String token = response.getString("token");
                        sessionManager.saveAuthToken(token);
                        sessionManager.saveUserDetail(email, "");
                        postSuccess(callback, response);
                    } else {
                        postError(callback, response.optString("error", "Invalid credentials"));
                    }
                } catch (Exception e) {
                    postError(callback, "Login error: " + e.getLocalizedMessage());
                }
            }
        }).start();
    }

    public void requestOtp(final String email, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject body = new JSONObject();
                    body.put("app_id", getAppId());
                    body.put("email", email);

                    String result = executePostRequest(BASE_URL + "/request-otp", body.toString(), null);
                    final JSONObject response = new JSONObject(result);
                    if (response.optBoolean("success", false)) {
                        postSuccess(callback, response);
                    } else {
                        postError(callback, response.optString("error", "Failed to send OTP"));
                    }
                } catch (Exception e) {
                    postError(callback, "OTP error: " + e.getLocalizedMessage());
                }
            }
        }).start();
    }

    public void resetPassword(final String email, final String otp, final String newPassword, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject body = new JSONObject();
                    body.put("app_id", getAppId());
                    body.put("email", email);
                    body.put("otp", otp);
                    body.put("new_password", newPassword);

                    String result = executePostRequest(BASE_URL + "/reset-password", body.toString(), null);
                    final JSONObject response = new JSONObject(result);
                    if (response.optBoolean("success", false)) {
                        postSuccess(callback, response);
                    } else {
                        postError(callback, response.optString("error", "Password reset failed"));
                    }
                } catch (Exception e) {
                    postError(callback, "Reset error: " + e.getLocalizedMessage());
                }
            }
        }).start();
    }

    public void createPost(final String content, final String imageUrl, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject dataObj = new JSONObject();
                    dataObj.put("author_name", sessionManager.getUserName());
                    dataObj.put("author_email", sessionManager.getUserEmail());
                    dataObj.put("content", content);
                    dataObj.put("image_url", imageUrl);
                    dataObj.put("likes", 0);
                    dataObj.put("timestamp", System.currentTimeMillis());

                    JSONObject body = new JSONObject();
                    body.put("collection", "posts");
                    body.put("data", dataObj);

                    String token = sessionManager.getAuthToken();
                    String result = executePostRequest(BASE_URL + "/data", body.toString(), token);
                    final JSONObject response = new JSONObject(result);

                    if (response.optBoolean("success", false)) {
                        postSuccess(callback, response);
                    } else {
                        postError(callback, response.optString("error", "Failed to create post"));
                    }
                } catch (Exception e) {
                    postError(callback, "Error creating post: " + e.getLocalizedMessage());
                }
            }
        }).start();
    }

    public void fetchPosts(final PostsCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String token = sessionManager.getAuthToken();
                    String result = executeGetRequest(BASE_URL + "/data?collection=posts", token);
                    JSONObject response = new JSONObject(result);

                    if (response.optBoolean("success", false)) {
                        JSONArray records = response.optJSONArray("records");
                        final List<PostModel> posts = new ArrayList<>();
                        if (records != null) {
                            for (int i = 0; i < records.length(); i++) {
                                JSONObject rec = records.getJSONObject(i);
                                String id = rec.optString("id", "");
                                JSONObject data = rec.optJSONObject("data");
                                if (data != null) {
                                    String author = data.optString("author_name", "Anonymous");
                                    String email = data.optString("author_email", "");
                                    String content = data.optString("content", "");
                                    String img = data.optString("image_url", "");
                                    int likes = data.optInt("likes", 0);
                                    long ts = data.optLong("timestamp", System.currentTimeMillis());
                                    posts.add(new PostModel(id, author, email, content, img, likes, ts));
                                }
                            }
                        }
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess(posts);
                            }
                        });
                    } else {
                        postPostsError(callback, response.optString("error", "Failed to load feed"));
                    }
                } catch (Exception e) {
                    postPostsError(callback, "Feed error: " + e.getLocalizedMessage());
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

                    String token = sessionManager.getAuthToken();
                    String result = executePutRequest(BASE_URL + "/data", body.toString(), token);
                    final JSONObject response = new JSONObject(result);

                    if (response.optBoolean("success", false)) {
                        postSuccess(callback, response);
                    } else {
                        postError(callback, response.optString("error", "Like update failed"));
                    }
                } catch (Exception e) {
                    postError(callback, "Update error: " + e.getLocalizedMessage());
                }
            }
        }).start();
    }

    private String executePostRequest(String urlStr, String jsonBody, String token) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        if (token != null && !token.isEmpty()) {
            conn.setRequestProperty("Authorization", "Bearer " + token);
        }
        conn.setDoOutput(true);

        OutputStream os = conn.getOutputStream();
        os.write(jsonBody.getBytes("UTF-8"));
        os.flush();
        os.close();

        return readStream(conn);
    }

    private String executeGetRequest(String urlStr, String token) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        if (token != null && !token.isEmpty()) {
            conn.setRequestProperty("Authorization", "Bearer " + token);
        }
        return readStream(conn);
    }

    private String executePutRequest(String urlStr, String jsonBody, String token) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("PUT");
        conn.setRequestProperty("Content-Type", "application/json");
        if (token != null && !token.isEmpty()) {
            conn.setRequestProperty("Authorization", "Bearer " + token);
        }
        conn.setDoOutput(true);

        OutputStream os = conn.getOutputStream();
        os.write(jsonBody.getBytes("UTF-8"));
        os.flush();
        os.close();

        return readStream(conn);
    }

    private String readStream(HttpURLConnection conn) throws Exception {
        int status = conn.getResponseCode();
        InputStream is = (status >= 200 && status < 300) ? conn.getInputStream() : conn.getErrorStream();
        if (is == null) {
            return "{\"success\":false,\"error\":\"No response stream\"}";
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();
        return sb.toString();
    }

    private void postSuccess(final ApiCallback callback, final JSONObject response) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onSuccess(response);
            }
        });
    }

    private void postError(final ApiCallback callback, final String message) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onError(message);
            }
        });
    }

    private void postPostsError(final PostsCallback callback, final String message) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onError(message);
            }
        });
    }
}