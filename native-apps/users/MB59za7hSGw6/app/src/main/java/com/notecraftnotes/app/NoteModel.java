package com.notecraftnotes.app;

import org.json.JSONObject;

public class NoteModel {
    private String id;
    private String title;
    private String content;
    private String category;
    private String date;

    public NoteModel() {
        this.id = "";
        this.title = "";
        this.content = "";
        this.category = "General";
        this.date = "";
    }

    public NoteModel(String id, String title, String content, String category, String date) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.category = category;
        this.date = date;
    }

    public static NoteModel fromRecordJson(JSONObject recordObj) {
        if (recordObj == null) return null;
        String recId = recordObj.optString("id", "");
        JSONObject dataObj = recordObj.optJSONObject("data");
        if (dataObj == null) dataObj = new JSONObject();

        String t = dataObj.optString("title", "");
        String c = dataObj.optString("content", "");
        String cat = dataObj.optString("category", "General");
        String d = dataObj.optString("date", "");

        return new NoteModel(recId, t, c, cat, d);
    }

    public JSONObject toDataJson() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("title", title);
            obj.put("content", content);
            obj.put("category", category);
            obj.put("date", date);
        } catch (Exception e) {
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

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    // Offline native Java calculations
    public int getWordCount() {
        if (content == null || content.trim().isEmpty()) {
            return 0;
        }
        String[] words = content.trim().split("\\s+");
        return words.length;
    }

    public int getCharacterCount() {
        return content != null ? content.length() : 0;
    }
}