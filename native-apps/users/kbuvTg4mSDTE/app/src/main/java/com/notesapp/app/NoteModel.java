package com.notesapp.app;

import org.json.JSONException;
import org.json.JSONObject;

public class NoteModel {
    private String id;
    private String title;
    private String content;
    private String category;
    private String color;
    private boolean isPinned;

    public NoteModel() {
        this.id = "";
        this.title = "";
        this.content = "";
        this.category = "Personal";
        this.color = "#4CAF50";
        this.isPinned = false;
    }

    public NoteModel(String id, String title, String content, String category, String color, boolean isPinned) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.category = category;
        this.color = color;
        this.isPinned = isPinned;
    }

    public static NoteModel fromRecordJson(JSONObject recordObj) {
        NoteModel model = new NoteModel();
        if (recordObj == null) return model;
        model.setId(recordObj.optString("id", ""));
        JSONObject dataObj = recordObj.optJSONObject("data");
        if (dataObj != null) {
            model.setTitle(dataObj.optString("title", ""));
            model.setContent(dataObj.optString("content", ""));
            model.setCategory(dataObj.optString("category", "Personal"));
            model.setColor(dataObj.optString("color", "#4CAF50"));
            model.setPinned(dataObj.optBoolean("is_pinned", false));
        }
        return model;
    }

    public JSONObject toDataJson() {
        JSONObject data = new JSONObject();
        try {
            data.put("title", title);
            data.put("content", content);
            data.put("category", category);
            data.put("color", color);
            data.put("is_pinned", isPinned);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return data;
    }

    public int getWordCount() {
        if (content == null || content.trim().isEmpty()) {
            return 0;
        }
        String[] words = content.trim().split("\\s+");
        return words.length;
    }

    public int getCharCount() {
        if (content == null) {
            return 0;
        }
        return content.length();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public boolean isPinned() { return isPinned; }
    public void setPinned(boolean pinned) { isPinned = pinned; }
}