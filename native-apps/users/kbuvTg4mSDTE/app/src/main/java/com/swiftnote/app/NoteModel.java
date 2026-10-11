package com.swiftnote.app;

import org.json.JSONException;
import org.json.JSONObject;

public class NoteModel {
    private String id;
    private String title;
    private String content;
    private String category;
    private boolean pinned;
    private String color;

    public NoteModel() {
        this.id = "";
        this.title = "";
        this.content = "";
        this.category = "Personal";
        this.pinned = false;
        this.color = "#FFFFFF";
    }

    public NoteModel(String id, String title, String content, String category, boolean pinned, String color) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.category = category;
        this.pinned = pinned;
        this.color = color;
    }

    public static NoteModel fromJson(String id, JSONObject dataObj) {
        NoteModel note = new NoteModel();
        note.id = id;
        if (dataObj != null) {
            note.title = dataObj.optString("title", "");
            note.content = dataObj.optString("content", "");
            note.category = dataObj.optString("category", "Personal");
            note.pinned = dataObj.optBoolean("pinned", false);
            note.color = dataObj.optString("color", "#FFFFFF");
        }
        return note;
    }

    public JSONObject toDataJson() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("title", this.title);
            obj.put("content", this.content);
            obj.put("category", this.category);
            obj.put("pinned", this.pinned);
            obj.put("color", this.color);
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

    public boolean isPinned() { return pinned; }
    public void setPinned(boolean pinned) { this.pinned = pinned; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
}