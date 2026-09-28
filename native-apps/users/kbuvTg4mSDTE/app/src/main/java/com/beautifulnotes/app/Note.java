package com.beautifulnotes.app;

import org.json.JSONException;
import org.json.JSONObject;

public class Note {
    private String id;
    private String title;
    private String content;
    private String color;
    private String updatedAt;

    public Note(String id, String title, String content, String color, String updatedAt) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.color = color == null || color.trim().isEmpty() ? "#FFFFFF" : color;
        this.updatedAt = updatedAt;
    }

    public static Note fromJsonRecord(JSONObject recordJson) {
        try {
            String id = recordJson.getString("id");
            JSONObject dataObj = recordJson.getJSONObject("data");
            String title = dataObj.optString("title", "");
            String content = dataObj.optString("content", "");
            String color = dataObj.optString("color", "#FFFFFF");
            String updatedAt = dataObj.optString("updated_at", "");
            return new Note(id, title, content, color, updatedAt);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public JSONObject toDataJson() {
        JSONObject dataObj = new JSONObject();
        try {
            dataObj.put("title", title);
            dataObj.put("content", content);
            dataObj.put("color", color);
            dataObj.put("updated_at", updatedAt);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return dataObj;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getColor() {
        return color;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    // Dynamic, Offline Word Calculation Logic
    public int getWordCount() {
        if (content == null || content.trim().isEmpty()) {
            return 0;
        }
        String[] words = content.trim().split("\\s+");
        return words.length;
    }
}