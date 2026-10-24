package com.offlinenotes.app;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Note {
    private String id;
    private String title;
    private String content;
    private String category;
    private String colorHex;
    private boolean pinned;
    private long createdAt;
    private long updatedAt;

    public Note(String id, String title, String content, String category, String colorHex, boolean pinned, long createdAt, long updatedAt) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.category = category;
        this.colorHex = colorHex;
        this.pinned = pinned;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Note fromJson(JSONObject obj) {
        String id = obj.optString("id", String.valueOf(System.currentTimeMillis()));
        String title = obj.optString("title", "");
        String content = obj.optString("content", "");
        String category = obj.optString("category", "Personal");
        String colorHex = obj.optString("colorHex", "#FFFFFF");
        boolean pinned = obj.optBoolean("pinned", false);
        long createdAt = obj.optLong("createdAt", System.currentTimeMillis());
        long updatedAt = obj.optLong("updatedAt", System.currentTimeMillis());
        return new Note(id, title, content, category, colorHex, pinned, createdAt, updatedAt);
    }

    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("id", id);
            obj.put("title", title);
            obj.put("content", content);
            obj.put("category", category);
            obj.put("colorHex", colorHex);
            obj.put("pinned", pinned);
            obj.put("createdAt", createdAt);
            obj.put("updatedAt", updatedAt);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return obj;
    }

    public String getId() {
        return id;
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

    public String getColorHex() {
        return colorHex;
    }

    public void setColorHex(String colorHex) {
        this.colorHex = colorHex;
    }

    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getFormattedDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
        return sdf.format(new Date(updatedAt));
    }

    public int calculateWordCount() {
        if (content == null || content.trim().isEmpty()) {
            return 0;
        }
        String trimmed = content.trim();
        String[] words = trimmed.split("\\s+");
        return words.length;
    }

    public int calculateCharCount() {
        if (content == null) {
            return 0;
        }
        return content.length();
    }
}