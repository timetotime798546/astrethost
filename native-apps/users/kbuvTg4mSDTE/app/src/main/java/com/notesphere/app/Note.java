package com.notesphere.app;

import org.json.JSONException;
import org.json.JSONObject;

public class Note {
    private String id;
    private String title;
    private String content;
    private String category;
    private long timestamp;

    public Note(String id, String title, String content, String category, long timestamp) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.category = category;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getCategory() { return category; }
    public long getTimestamp() { return timestamp; }

    public void setTitle(String title) { this.title = title; }
    public void setContent(String content) { this.content = content; }
    public void setCategory(String category) { this.category = category; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public JSONObject toJSONObject() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("id", id);
        obj.put("title", title);
        obj.put("content", content);
        obj.put("category", category);
        obj.put("timestamp", timestamp);
        return obj;
    }

    public static Note fromJSONObject(JSONObject obj) throws JSONException {
        return new Note(
            obj.getString("id"),
            obj.getString("title"),
            obj.getString("content"),
            obj.getString("category"),
            obj.getLong("timestamp")
        );
    }
}