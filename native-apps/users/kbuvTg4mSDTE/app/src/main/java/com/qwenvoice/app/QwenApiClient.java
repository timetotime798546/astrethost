package com.qwenvoice.app;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

public class QwenApiClient {

    private static final String TAG = "QwenApiClient";

    public interface QwenApiResponseListener {
        void onResponse(String response);
        void onError(String error);
        void onConnectionTestResult(boolean success);
    }

    public void sendMessage(final String serverUrl, final List<Map<String, String>> conversationHistory, final int maxTokens, final QwenApiResponseListener listener) {
        new AsyncTask<Void, Void, String>() {
            private String errorMessage = null;

            @Override
            protected String doInBackground(Void... voids) {
                HttpURLConnection urlConnection = null;
                try {
                    URL url = new URL(serverUrl + "/v1/chat/completions");
                    urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setRequestMethod("POST");
                    urlConnection.setRequestProperty("Content-Type", "application/json");
                    urlConnection.setDoOutput(true);
                    urlConnection.setConnectTimeout(5000); // 5 seconds
                    urlConnection.setReadTimeout(15000); // 15 seconds

                    // Construct JSON body
                    StringBuilder jsonBody = new StringBuilder();
                    jsonBody.append("{");
                    jsonBody.append("\"messages\": [");

                    // Add system prompt
                    jsonBody.append("{\"role\": \"system\", \"content\": \"You are a friendly voice assistant. Give natural, concise spoken responses. Do not use markdown, emojis, or long explanations.\"},");

                    // Add conversation history
                    for (int i = 0; i < conversationHistory.size(); i++) {
                        Map<String, String> message = conversationHistory.get(i);
                        jsonBody.append("{\"role\": \"").append(escapeJson(message.get("role"))).append("\", \"content\": \"").append(escapeJson(message.get("content"))).append("\"}");
                        if (i < conversationHistory.size() - 1) {
                            jsonBody.append(",");
                        }
                    }

                    jsonBody.append("],");
                    jsonBody.append("\"temperature\": 0.7,");
                    jsonBody.append("\"max_tokens\": ").append(maxTokens).append(",");
                    jsonBody.append("\"stream\": false");
                    jsonBody.append("}");

                    Log.d(TAG, "Request JSON: " + jsonBody.toString());

                    OutputStream os = urlConnection.getOutputStream();
                    os.write(jsonBody.toString().getBytes("UTF-8"));
                    os.flush();
                    os.close();

                    int responseCode = urlConnection.getResponseCode();
                    Log.d(TAG, "Response Code: " + responseCode);

                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                        String inputLine;
                        StringBuilder response = new StringBuilder();
                        while ((inputLine = in.readLine()) != null) {
                            response.append(inputLine);
                        }
                        in.close();
                        return response.toString();
                    } else {
                        BufferedReader errorReader = new BufferedReader(new InputStreamReader(urlConnection.getErrorStream()));
                        String errorLine;
                        StringBuilder errorResponse = new StringBuilder();
                        while ((errorLine = errorReader.readLine()) != null) {
                            errorResponse.append(errorLine);
                        }
                        errorReader.close();
                        errorMessage = "Server error: " + responseCode + " - " + errorResponse.toString();
                        Log.e(TAG, errorMessage);
                    }

                } catch (java.net.SocketTimeoutException e) {
                    errorMessage = "Connection timed out. Check server URL and ensure Qwen is running.";
                    Log.e(TAG, "SocketTimeoutException: " + e.getMessage());
                } catch (java.net.ConnectException e) {
                    errorMessage = "Unable to connect to Qwen server. Is it running at " + serverUrl + "?";
                    Log.e(TAG, "ConnectException: " + e.getMessage());
                } catch (Exception e) {
                    errorMessage = "Network or JSON error: " + e.getMessage();
                    Log.e(TAG, "Error in sendMessage: " + e.getMessage(), e);
                } finally {
                    if (urlConnection != null) {
                        urlConnection.disconnect();
                    }
                }
                return null;
            }

            @Override
            protected void onPostExecute(String result) {
                if (result != null) {
                    listener.onResponse(result);
                } else {
                    if (errorMessage == null || errorMessage.isEmpty()) {
                        errorMessage = "An unknown error occurred.";
                    }
                    listener.onError(errorMessage);
                }
            }
        }.execute();
    }

    public void testConnection(final String serverUrl, final QwenApiResponseListener listener) {
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... voids) {
                HttpURLConnection urlConnection = null;
                try {
                    // Try a simple GET to the base URL or a known health endpoint
                    URL url = new URL(serverUrl);
                    urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setRequestMethod("GET");
                    urlConnection.setConnectTimeout(3000); // Shorter timeout for connection test
                    urlConnection.setReadTimeout(5000);

                    urlConnection.connect(); // Explicitly connect to catch connection errors sooner
                    int responseCode = urlConnection.getResponseCode();
                    Log.d(TAG, "Test Connection response code: " + responseCode);
                    // Any 2xx or 3xx response indicates the server is reachable and responding
                    return responseCode >= 200 && responseCode < 400;

                } catch (java.net.SocketTimeoutException e) {
                    Log.e(TAG, "Test connection timed out: " + e.getMessage());
                    return false;
                } catch (java.net.ConnectException e) {
                    Log.e(TAG, "Test connection failed (ConnectException): " + e.getMessage());
                    return false;
                } catch (Exception e) {
                    Log.e(TAG, "Test connection error: " + e.getMessage(), e);
                    return false;
                } finally {
                    if (urlConnection != null) {
                        urlConnection.disconnect();
                    }
                }
            }

            @Override
            protected void onPostExecute(Boolean success) {
                listener.onConnectionTestResult(success);
            }
        }.execute();
    }

    // Helper to escape JSON string content
    private String escapeJson(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}