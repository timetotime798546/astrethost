package com.notesphere.app;

import org.json.JSONException;
import org.json.JSONObject;

public class Note {
    private String id;
    private String title;
    private String content;
    private String category;
    private String timestamp;

    public Note(String id, String title, String content, String category, String timestamp) {
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
    public String getTimestamp() { return timestamp; }

    public void setId(String id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setContent(String content) { this.content = content; }
    public void setCategory(String category) { this.category = category; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public JSONObject toJSONData() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("title", title);
        obj.put("content", content);
        obj.put("category", category);
        obj.put("timestamp", timestamp);
        return obj;
    }

    public static Note fromRecordJSON(JSONObject recordObj) throws JSONException {
        String id = recordObj.getString("id");
        JSONObject data = recordObj.getJSONObject("data");
        String title = data.optString("title", "");
        String content = data.optString("content", "");
        String category = data.optString("category", "General");
        String timestamp = data.optString("timestamp", String.valueOf(System.currentTimeMillis()));
        return new Note(id, title, content, category, timestamp);
    }
}