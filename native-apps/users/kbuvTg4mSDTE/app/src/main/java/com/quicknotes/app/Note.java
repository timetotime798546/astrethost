package com.quicknotes.app;

import org.json.JSONException;
import org.json.JSONObject;

public class Note {
    private String id;
    private String title;
    private String content;
    private String category;
    private boolean isPinned;
    private long createdAt;
    private long updatedAt;

    public Note(String id, String title, String content, String category, boolean isPinned, long createdAt, long updatedAt) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.category = category;
        this.isPinned = isPinned;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public boolean isPinned() {
        return isPinned;
    }

    public void setPinned(boolean pinned) {
        isPinned = pinned;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public JSONObject toJsonObject() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("id", id);
            obj.put("title", title);
            obj.put("content", content);
            obj.put("category", category);
            obj.put("isPinned", isPinned);
            obj.put("createdAt", createdAt);
            obj.put("updatedAt", updatedAt);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return obj;
    }

    public static Note fromJsonObject(JSONObject obj) {
        if (obj == null) return null;
        String id = obj.optString("id", String.valueOf(System.currentTimeMillis()));
        String title = obj.optString("title", "");
        String content = obj.optString("content", "");
        String category = obj.optString("category", "General");
        boolean isPinned = obj.optBoolean("isPinned", false);
        long createdAt = obj.optLong("createdAt", System.currentTimeMillis());
        long updatedAt = obj.optLong("updatedAt", System.currentTimeMillis());
        return new Note(id, title, content, category, isPinned, createdAt, updatedAt);
    }
}