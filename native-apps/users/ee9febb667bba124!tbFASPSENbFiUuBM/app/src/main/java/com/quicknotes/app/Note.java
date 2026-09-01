package com.quicknotes.app;

public class Note {
    private long id;
    private String title;
    private String content;
    private String category;
    private long timestamp;

    public Note(long id, String title, String content, String category, long timestamp) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.category = category;
        this.timestamp = timestamp;
    }

    public long getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getCategory() { return category; }
    public long getTimestamp() { return timestamp; }
}