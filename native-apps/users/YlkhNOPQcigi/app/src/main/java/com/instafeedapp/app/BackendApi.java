package com.instafeedapp.app;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class BackendApi {
    private static final String BASE_URL = "https://trumpledroid-api.cloudbeta28624.workers.dev";
    private static final String PREF_NAME = "InstaFeedPrefs";
    private static final String KEY_TOKEN = "auth_token";

    private final Context context;

    public interface ApiCallback {
        void onSuccess(JSONObject response);
        void onError(String error);
    }

    public BackendApi(Context context) {
        this.context = context;
    }

    public static String getAppId(Context context) {
        try {
            InputStream is = context.getAssets().open("app-meta.json");
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();
            String jsonStr = new String(buffer, "UTF-8");
            JSONObject obj = new JSONObject(jsonStr);
            return obj.optString("app_id", "insta_feed_app_v1");
        } catch (Exception e) {
            return "insta_feed_app_v1";
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
        return getToken() != null && !getToken().isEmpty();
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

                    makeRequest("/register", "POST", body.toString(), false, new ApiCallback() {
                        @Override
                        public void onSuccess(JSONObject response) {
                            login(email, password, callback);
                        }

                        @Override
                        public void onError(String error) {
                            callback.onError(error);
                        }
                    });
                } catch (Exception e) {
                    callback.onError(e.getMessage());
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

                    makeRequest("/login", "POST", body.toString(), false, new ApiCallback() {
                        @Override
                        public void onSuccess(JSONObject response) {
                            String token = response.optString("token", "");
                            if (!token.isEmpty()) {
                                saveToken(token);
                                callback.onSuccess(response);
                            } else {
                                callback.onError("Login succeeded but no token was returned.");
                            }
                        }

                        @Override
                        public void onError(String error) {
                            callback.onError(error);
                        }
                    });
                } catch (Exception e) {
                    callback.onError(e.getMessage());
                }
            }
        }).start();
    }

    public void fetchPosts(final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                makeRequest("/data?collection=insta_posts", "GET", null, true, callback);
            }
        }).start();
    }

    public void createPost(final String username, final String caption, final String imageUrl, final String location, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject postData = new JSONObject();
                    postData.put("username", username);
                    postData.put("caption", caption);
                    postData.put("image_url", imageUrl);
                    postData.put("location", location);
                    postData.put("likes", 0);

                    JSONObject body = new JSONObject();
                    body.put("collection", "insta_posts");
                    body.put("data", postData);

                    makeRequest("/data", "POST", body.toString(), true, callback);
                } catch (Exception e) {
                    callback.onError(e.getMessage());
                }
            }
        }).start();
    }

    public void updateLikes(final String recordId, final JSONObject updatedFields, final ApiCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject body = new JSONObject();
                    body.put("id", recordId);
                    body.put("data", updatedFields);

                    makeRequest("/data", "PUT", body.toString(), true, callback);
                } catch (Exception e) {
                    callback.onError(e.getMessage());
                }
            }
        }).start();
    }

    private void makeRequest(String endpoint, String method, String jsonBody, boolean requireAuth, ApiCallback callback) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(BASE_URL + endpoint);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(method);
            conn.setRequestProperty("Content-Type", "application/json");

            if (requireAuth && isLoggedIn()) {
                conn.setRequestProperty("Authorization", "Bearer " + getToken());
            }

            if (jsonBody != null && !jsonBody.isEmpty()) {
                conn.setDoOutput(true);
                OutputStream os = conn.getOutputStream();
                os.write(jsonBody.getBytes("UTF-8"));
                os.flush();
                os.close();
            }

            int responseCode = conn.getResponseCode();
            InputStream is = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream();

            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();

            JSONObject responseJson = new JSONObject(sb.toString());

            if (responseCode >= 200 && responseCode < 300 && responseJson.optBoolean("success", true)) {
                callback.onSuccess(responseJson);
            } else {
                String err = responseJson.optString("error", responseJson.optString("message", "HTTP Error " + responseCode));
                callback.onError(err);
            }
        } catch (Exception e) {
            callback.onError(e.getMessage());
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}