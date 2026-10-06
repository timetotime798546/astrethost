package com.notesphere.app;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
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
    private static final String TAG = "BackendApi";
    private String apiBaseUrl = "https://trumpledroid-api.cloudbeta28624.workers.dev";
    private String appId = "";
    private Context context;
    private Handler mainHandler;

    public BackendApi(Context context) {
        this.context = context;
        this.mainHandler = new Handler(Looper.getMainLooper());
        loadAppId();
    }

    private void loadAppId() {
        try {
            InputStream is = context.getAssets().open("app-meta.json");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            JSONObject obj = new JSONObject(sb.toString());
            appId = obj.getString("app_id");
        } catch (Exception e) {
            Log.e(TAG, "Error loading app-meta.json", e);
            appId = "app_6ac4699f89f58"; // fallback
        }
    }

    private void sendRequest(final String method, final String endpoint, final String jsonBody, final boolean authenticated, final ApiCallback<String> callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection conn = null;
                try {
                    URL url = new URL(apiBaseUrl + endpoint);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod(method);
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setConnectTimeout(15000);
                    conn.setReadTimeout(15000);

                    if (authenticated) {
                        String token = NoteStorage.getToken(context);
                        if (token != null) {
                            conn.setRequestProperty("Authorization", "Bearer " + token);
                        }
                    }

                    if (jsonBody != null) {
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

                    if (is != null) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            sb.append(line);
                        }
                        reader.close();
                        final String response = sb.toString();

                        if (responseCode >= 200 && responseCode < 300) {
                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    callback.onSuccess(response);
                                }
                            });
                        } else {
                            String errorMsg = "Server returned error: " + responseCode;
                            try {
                                JSONObject errObj = new JSONObject(response);
                                if (errObj.has("error")) {
                                    errorMsg = errObj.getString("error");
                                } else if (errObj.has("message")) {
                                    errorMsg = errObj.getString("message");
                                }
                            } catch (Exception ignored) {}
                            
                            final String finalError = errorMsg;
                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    callback.onError(finalError);
                                }
                            });
                        }
                    } else {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onError("Empty response from server. Code: " + responseCode);
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
                } finally {
                    if (conn != null) {
                        conn.disconnect();
                    }
                }
            }
        }).start();
    }

    public void register(final String email, final String password, final ApiCallback<Void> callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("app_id", appId);
            body.put("email", email);
            body.put("password", password);

            sendRequest("POST", "/register", body.toString(), false, new ApiCallback<String>() {
                @Override
                public void onSuccess(String result) {
                    callback.onSuccess(null);
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

    public void login(final String email, final String password, final ApiCallback<String> callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("app_id", appId);
            body.put("email", email);
            body.put("password", password);

            sendRequest("POST", "/login", body.toString(), false, new ApiCallback<String>() {
                @Override
                public void onSuccess(String result) {
                    try {
                        JSONObject obj = new JSONObject(result);
                        if (obj.has("token")) {
                            String token = obj.getString("token");
                            NoteStorage.saveToken(context, token);
                            NoteStorage.saveEmail(context, email);
                            callback.onSuccess(token);
                        } else {
                            callback.onError("Response does not contain token.");
                        }
                    } catch (Exception e) {
                        callback.onError("JSON Parse Error: " + e.getMessage());
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

    public void requestOtp(final String email, final ApiCallback<Void> callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("app_id", appId);
            body.put("email", email);

            sendRequest("POST", "/request-otp", body.toString(), false, new ApiCallback<String>() {
                @Override
                public void onSuccess(String result) {
                    callback.onSuccess(null);
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

    public void resetPassword(final String email, final String otp, final String newPassword, final ApiCallback<Void> callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("app_id", appId);
            body.put("email", email);
            body.put("otp", otp);
            body.put("new_password", newPassword);

            sendRequest("POST", "/reset-password", body.toString(), false, new ApiCallback<String>() {
                @Override
                public void onSuccess(String result) {
                    callback.onSuccess(null);
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

    public void logout(final ApiCallback<Void> callback) {
        sendRequest("POST", "/logout", null, true, new ApiCallback<String>() {
            @Override
            public void onSuccess(String result) {
                NoteStorage.clearSession(context);
                callback.onSuccess(null);
            }

            @Override
            public void onError(String error) {
                NoteStorage.clearSession(context);
                callback.onSuccess(null);
            }
        });
    }

    public void createNote(Note note, final ApiCallback<Note> callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("collection", "notes");
            body.put("data", note.toJSONData());

            sendRequest("POST", "/data", body.toString(), true, new ApiCallback<String>() {
                @Override
                public void onSuccess(String result) {
                    try {
                        JSONObject obj = new JSONObject(result);
                        if (obj.optBoolean("success", false)) {
                            JSONObject record = obj.getJSONObject("record");
                            Note createdNote = Note.fromRecordJSON(record);
                            callback.onSuccess(createdNote);
                        } else {
                            callback.onError("Operation unsuccessful");
                        }
                    } catch (Exception e) {
                        callback.onError("Parse failure: " + e.getMessage());
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

    public void getNotes(final ApiCallback<List<Note>> callback) {
        sendRequest("GET", "/data?collection=notes", null, true, new ApiCallback<String>() {
            @Override
            public void onSuccess(String result) {
                try {
                    JSONObject obj = new JSONObject(result);
                    if (obj.optBoolean("success", false)) {
                        JSONArray records = obj.getJSONArray("records");
                        List<Note> list = new ArrayList<Note>();
                        for (int i = 0; i < records.length(); i++) {
                            JSONObject record = records.getJSONObject(i);
                            list.add(Note.fromRecordJSON(record));
                        }
                        callback.onSuccess(list);
                    } else {
                        callback.onError("Could not fetch list");
                    }
                } catch (Exception e) {
                    callback.onError("Parse failure: " + e.getMessage());
                }
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    public void updateNote(Note note, final ApiCallback<Void> callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("id", note.getId());
            body.put("data", note.toJSONData());

            sendRequest("PUT", "/data", body.toString(), true, new ApiCallback<String>() {
                @Override
                public void onSuccess(String result) {
                    try {
                        JSONObject obj = new JSONObject(result);
                        if (obj.optBoolean("success", false)) {
                            callback.onSuccess(null);
                        } else {
                            callback.onError("Update failed");
                        }
                    } catch (Exception e) {
                        callback.onError("Parse error: " + e.getMessage());
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

    public void deleteNote(String id, final ApiCallback<Void> callback) {
        sendRequest("DELETE", "/data?id=" + id, null, true, new ApiCallback<String>() {
            @Override
            public void onSuccess(String result) {
                try {
                    JSONObject obj = new JSONObject(result);
                    if (obj.optBoolean("success", false)) {
                        callback.onSuccess(null);
                    } else {
                        callback.onError("Delete failed");
                    }
                } catch (Exception e) {
                    callback.onError("Parse error: " + e.getMessage());
                }
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }
}