package com.docscanpro.app;

import org.json.JSONException;
import org.json.JSONObject;

public class DocumentItem {
    private String id;
    private String title;
    private String date;
    private String imagePath;
    private String pdfPath;
    private long fileSizeBytes;

    public DocumentItem(String id, String title, String date, String imagePath, String pdfPath, long fileSizeBytes) {
        this.id = id;
        this.title = title;
        this.date = date;
        this.imagePath = imagePath;
        this.pdfPath = pdfPath;
        this.fileSizeBytes = fileSizeBytes;
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

    public String getDate() {
        return date;
    }

    public String getImagePath() {
        return imagePath;
    }

    public String getPdfPath() {
        return pdfPath;
    }

    public long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public String getFormattedSize() {
        if (fileSizeBytes < 1024) {
            return fileSizeBytes + " B";
        } else if (fileSizeBytes < 1024 * 1024) {
            return String.format("%.1f KB", fileSizeBytes / 1024.0);
        } else {
            return String.format("%.2f MB", fileSizeBytes / (1024.0 * 1024.0));
        }
    }

    public JSONObject toJsonObject() {
        JSONObject json = new JSONObject();
        try {
            json.put("id", id);
            json.put("title", title);
            json.put("date", date);
            json.put("imagePath", imagePath);
            json.put("pdfPath", pdfPath);
            json.put("fileSizeBytes", fileSizeBytes);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    public static DocumentItem fromJsonObject(JSONObject json) {
        if (json == null) return null;
        String id = json.optString("id", String.valueOf(System.currentTimeMillis()));
        String title = json.optString("title", "Untitled Document");
        String date = json.optString("date", "");
        String imagePath = json.optString("imagePath", "");
        String pdfPath = json.optString("pdfPath", "");
        long size = json.optLong("fileSizeBytes", 0);
        return new DocumentItem(id, title, date, imagePath, pdfPath, size);
    }
}