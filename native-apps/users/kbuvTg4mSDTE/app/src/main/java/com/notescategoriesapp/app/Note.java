package com.notescategoriesapp.app;

public class Note {
    private long id;
    private String title;
    private String content;
    private String category;
    private long timestamp;

    public Note() {
        // Default constructor
    }

    public Note(long id, String title, String content, String category, long timestamp) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.category = category;
        this.timestamp = timestamp;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
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

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}