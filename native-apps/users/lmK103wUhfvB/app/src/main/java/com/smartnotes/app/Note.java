package com.smartnotes.app;

public class Note {
    private String id;
    private String title;
    private String content;
    private String category;
    private String updatedAt;

    public Note(String id, String title, String content, String category, String updatedAt) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.category = category;
        this.updatedAt = updatedAt;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getCategory() {
        return category;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }
}