package com.cloudnotes.app;

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

    public interface ApiCallback<T> {
        void onSuccess(T result);
        void onError(String error);
    }

    private String baseUrl;
    private String appId;
    private String token;
    private Handler handler;
    private Context context;

    public BackendApi(Context context) {
        this.context = context;
        this.handler = new Handler(Looper.getMainLooper());
        this.baseUrl = "https://trumpledroid-api.cloudbeta28624.workers.dev";
        this.appId = getAppId(context);

        SharedPreferences prefs = context.getSharedPreferences("CloudNotesPrefs", Context.MODE_PRIVATE);
        this.token = prefs.getString("auth_token", null);
    }

    public void setToken(String token) {
        this.token = token;
        SharedPreferences prefs = context.getSharedPreferences("CloudNotesPrefs", Context.MODE_PRIVATE);
        prefs.edit().putString("auth_token", token).apply();
    }

    public String getToken() {
        return this.token;
    }

    public void clearToken() {
        this.token = null;
        SharedPreferences prefs = context.getSharedPreferences("CloudNotesPrefs", Context.MODE_PRIVATE);
        prefs.edit().remove("auth_token").apply();
    }

    private static String getAppId(Context context) {
        try {
            InputStream is = context.getAssets().open("app-meta.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String jsonStr = new String(buffer, "UTF-8");
            JSONObject json = new JSONObject(jsonStr);
            return json.optString("app_id", json.optString("package_name", "com.cloudnotes.app"));
        } catch (Exception e) {
            e.printStackTrace();
            return "com.cloudnotes.app";
        }
    }

    private static String executeRequest(String urlStr, String method, String bodyJson, String token) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);
        conn.setDoInput(true);

        if (token != null && !token.isEmpty()) {
            conn.setRequestProperty("Authorization", "Bearer " + token);
        }

        if (bodyJson != null) {
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            OutputStream os = conn.getOutputStream();
            os.write(bodyJson.getBytes("UTF-8"));
            os.close();
        }

        int responseCode = conn.getResponseCode();
        InputStream is;
        if (responseCode >= 200 && responseCode < 300) {
            is = conn.getInputStream();
        } else {
            is = conn.getErrorStream();
        }

        if (is == null) {
            throw new Exception("HTTP Response: " + responseCode);
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();
        is.close();

        String response = sb.toString();
        if (responseCode >= 200 && responseCode < 300) {
            return response;
        } else {
            String errorMsg = "API Error: " + responseCode;
            try {
                JSONObject errJson = new JSONObject(response);
                if (errJson.has("error")) {
                    errorMsg = errJson.getString("error");
                } else if (errJson.has("message")) {
                    errorMsg = errJson.getString("message");
                }
            } catch (Exception e) {
                // Keep default
            }
            throw new Exception(errorMsg);
        }
    }

    public void register(final String email, final String password, final ApiCallback<Boolean> callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject body = new JSONObject();
                    body.put("app_id", appId);
                    body.put("email", email);
                    body.put("password", password);

                    String resStr = executeRequest(baseUrl + "/register", "POST", body.toString(), null);
                    final JSONObject resJson = new JSONObject(resStr);
                    final boolean success = resJson.optBoolean("success", false);

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (success) {
                                callback.onSuccess(true);
                            } else {
                                callback.onError(resJson.optString("message", "Registration failed"));
                            }
                        }
                    });
                } catch (final Exception e) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onError(e.getMessage());
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
                    JSONObject body = new JSONObject();
                    body.put("app_id", appId);
                    body.put("email", email);
                    body.put("password", password);

                    String resStr = executeRequest(baseUrl + "/login", "POST", body.toString(), null);
                    JSONObject resJson = new JSONObject(resStr);
                    final boolean success = resJson.optBoolean("success", false);
                    final String token = resJson.optString("token", "");

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (success && !token.isEmpty()) {
                                callback.onSuccess(token);
                            } else {
                                callback.onError("Invalid credentials or login failed.");
                            }
                        }
                    });
                } catch (final Exception e) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onError(e.getMessage());
                        }
                    });
                }
            }
        }).start();
    }

    public void createNote(final String title, final String content, final String category, final ApiCallback<Note> callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject innerData = new JSONObject();
                    innerData.put("title", title);
                    innerData.put("content", content);
                    innerData.put("category", category);

                    JSONObject body = new JSONObject();
                    body.put("collection", "notes");
                    body.put("data", innerData);

                    String resStr = executeRequest(baseUrl + "/data", "POST", body.toString(), token);
                    JSONObject resJson = new JSONObject(resStr);

                    if (resJson.optBoolean("success", false)) {
                        JSONObject record = resJson.getJSONObject("record");
                        String id = record.getString("id");
                        JSONObject dataObj = record.getJSONObject("data");

                        final Note note = new Note(
                                id,
                                dataObj.optString("title", ""),
                                dataObj.optString("content", ""),
                                dataObj.optString("category", "General")
                        );

                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess(note);
                            }
                        });
                    } else {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onError("Failed to create note.");
                            }
                        });
                    }
                } catch (final Exception e) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onError(e.getMessage());
                        }
                    });
                }
            }
        }).start();
    }

    public void getNotes(final ApiCallback<List<Note>> callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String resStr = executeRequest(baseUrl + "/data?collection=notes", "GET", null, token);
                    JSONObject resJson = new JSONObject(resStr);

                    if (resJson.optBoolean("success", false)) {
                        JSONArray records = resJson.getJSONArray("records");
                        final List<Note> notes = new ArrayList<>();
                        for (int i = 0; i < records.length(); i++) {
                            JSONObject record = records.getJSONObject(i);
                            String id = record.getString("id");
                            JSONObject dataObj = record.getJSONObject("data");

                            Note note = new Note(
                                    id,
                                    dataObj.optString("title", ""),
                                    dataObj.optString("content", ""),
                                    dataObj.optString("category", "General")
                            );
                            notes.add(note);
                        }

                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess(notes);
                            }
                        });
                    } else {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onError("Failed to load notes.");
                            }
                        });
                    }
                } catch (final Exception e) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onError(e.getMessage());
                        }
                    });
                }
            }
        }).start();
    }

    public void updateNote(final String id, final String title, final String content, final String category, final ApiCallback<Boolean> callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject innerData = new JSONObject();
                    innerData.put("title", title);
                    innerData.put("content", content);
                    innerData.put("category", category);

                    JSONObject body = new JSONObject();
                    body.put("id", id);
                    body.put("data", innerData);

                    String resStr = executeRequest(baseUrl + "/data", "PUT", body.toString(), token);
                    final JSONObject resJson = new JSONObject(resStr);
                    final boolean success = resJson.optBoolean("success", false);

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (success) {
                                callback.onSuccess(true);
                            } else {
                                callback.onError("Failed to update note.");
                            }
                        }
                    });
                } catch (final Exception e) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onError(e.getMessage());
                        }
                    });
                }
            }
        }).start();
    }

    public void deleteNote(final String id, final ApiCallback<Boolean> callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String resStr = executeRequest(baseUrl + "/data?id=" + id, "DELETE", null, token);
                    final JSONObject resJson = new JSONObject(resStr);
                    final boolean success = resJson.optBoolean("success", false);

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (success) {
                                callback.onSuccess(true);
                            } else {
                                callback.onError("Failed to delete note.");
                            }
                        }
                    });
                } catch (final Exception e) {
                    handler.post(new Runnable() {
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