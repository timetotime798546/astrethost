package com.quicknotes.app;

import org.json.JSONException;
import org.json.JSONObject;

public class NoteModel {
    private String id;
    private String title;
    private String content;
    private String category;
    private String color;
    private boolean pinned;
    private String updatedAt;

    public NoteModel() {
        this.id = "";
        this.title = "";
        this.content = "";
        this.category = "General";
        this.color = "#FFFFFF";
        this.pinned = false;
        this.updatedAt = "";
    }

    public static NoteModel fromJson(String id, JSONObject dataObj) {
        NoteModel model = new NoteModel();
        model.setId(id);
        if (dataObj != null) {
            model.setTitle(dataObj.optString("title", "Untitled"));
            model.setContent(dataObj.optString("content", ""));
            model.setCategory(dataObj.optString("category", "General"));
            model.setColor(dataObj.optString("color", "#FFFFFF"));
            model.setPinned(dataObj.optBoolean("pinned", false));
            model.setUpdatedAt(dataObj.optString("updated_at", ""));
        }
        return model;
    }

    public JSONObject toDataJson() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("title", title);
            obj.put("content", content);
            obj.put("category", category);
            obj.put("color", color);
            obj.put("pinned", pinned);
            obj.put("updated_at", updatedAt);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return obj;
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

    public boolean isPinned() { return pinned; }
    public void setPinned(boolean pinned) { this.pinned = pinned; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}