package com.cloudnotespro.app;

import org.json.JSONObject;

public class Note {
    private String id;
    private String title;
    private String description;
    private String date;
    private String status;
    private String type;
    private String imageUrl;

    public Note() {}

    public Note(String id, String title, String description, String date, String status, String type, String imageUrl) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.date = date;
        this.status = status;
        this.type = type;
        this.imageUrl = imageUrl;
    }

    public static Note fromJsonRecord(JSONObject recordJson) {
        try {
            String recordId = recordJson.getString("id");
            JSONObject dataObj = recordJson.getJSONObject("data");
            
            String title = dataObj.optString("title", "");
            String description = dataObj.optString("description", "");
            String date = dataObj.optString("date", "");
            String status = dataObj.optString("status", "Active");
            String type = dataObj.optString("type", "Personal");
            String imageUrl = dataObj.optString("imageUrl", "");

            return new Note(recordId, title, description, date, status, type, imageUrl);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public JSONObject toDataJson() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("title", title);
            obj.put("description", description);
            obj.put("date", date);
            obj.put("status", status);
            obj.put("type", type);
            obj.put("imageUrl", imageUrl);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return obj;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}