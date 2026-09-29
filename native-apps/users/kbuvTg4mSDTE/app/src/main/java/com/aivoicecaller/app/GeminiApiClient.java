package com.aivoicecaller.app;

import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONArray;
import org.json.JSONObject;

public class GeminiApiClient {

    private static final String TAG = "GeminiApiClient";
    // NOTE: Replace with the actual Gemini API endpoint you intend to use.
    // This is a placeholder for a generic text generation endpoint.
    // For Google Gemini, the endpoint might look like:
    // https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent
    // Ensure you use the correct endpoint for your chosen Gemini model and region.
    private static final String GEMINI_API_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent?key=";

    public static String getGeminiResponse(String apiKey, String prompt) throws Exception {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalArgumentException("Gemini API Key is not set.");
        }
        if (prompt == null || prompt.isEmpty()) {
            return ""; // No prompt, no response needed
        }

        URL url = new URL(GEMINI_API_BASE_URL + apiKey);
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            // Constructing the JSON request body for Gemini API (generative models)
            // This structure is based on typical Gemini API requests for text generation
            JSONObject requestBody = new JSONObject();
            JSONArray contentsArray = new JSONArray();
            JSONObject contentObject = new JSONObject();
            JSONArray partsArray = new JSONArray();
            JSONObject partObject = new JSONObject();
            partObject.put("text", prompt);
            partsArray.put(partObject);
            contentObject.put("parts", partsArray);
            contentsArray.put(contentObject);
            requestBody.put("contents", contentsArray);
            
            // Add generation config for shorter, conversational responses
            JSONObject generationConfig = new JSONObject();
            generationConfig.put("temperature", 0.7);
            generationConfig.put("topK", 1);
            generationConfig.put("topP", 1);
            generationConfig.put("maxOutputTokens", 150); // Keep responses short
            requestBody.put("generationConfig", generationConfig);

            OutputStream os = connection.getOutputStream();
            os.write(requestBody.toString().getBytes("UTF-8"));
            os.flush();

            int responseCode = connection.getResponseCode();
            Log.d(TAG, "Gemini API Response Code: " + responseCode);

            BufferedReader in;
            if (responseCode >= 200 && responseCode < 300) {
                in = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
            } else {
                in = new BufferedReader(new InputStreamReader(connection.getErrorStream(), "UTF-8"));
            }

            StringBuilder response = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            Log.d(TAG, "Gemini API Raw Response: " + response.toString());

            if (responseCode >= 200 && responseCode < 300) {
                // Parse the response for Gemini models
                JSONObject jsonResponse = new JSONObject(response.toString());
                JSONArray candidates = jsonResponse.getJSONArray("candidates");
                if (candidates != null && candidates.length() > 0) {
                    JSONObject firstCandidate = candidates.getJSONObject(0);
                    JSONObject content = firstCandidate.getJSONObject("content");
                    JSONArray parts = content.getJSONArray("parts");
                    if (parts != null && parts.length() > 0) {
                        JSONObject firstPart = parts.getJSONObject(0);
                        return firstPart.getString("text");
                    }
                }
                return "No coherent response from AI.";

            } else {
                // Handle API error response
                JSONObject errorJson = new JSONObject(response.toString());
                String errorMessage = "Unknown API error";
                if (errorJson.has("error")) {
                    JSONObject errorDetails = errorJson.getJSONObject("error");
                    if (errorDetails.has("message")) {
                        errorMessage = errorDetails.getString("message");
                    }
                }
                throw new Exception("Gemini API Error (" + responseCode + "): " + errorMessage);
            }

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}