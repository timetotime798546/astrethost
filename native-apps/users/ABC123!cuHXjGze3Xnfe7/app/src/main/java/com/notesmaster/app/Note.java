package com.notesmaster.app;

public class Note {
    private int id;
    private String title;
    private String content;
    private String category;
    private String timestamp;

    public Note(int id, String title, String content, String category, String timestamp) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.category = category;
        this.timestamp = timestamp;
    }

    public int getId() {
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

    public String getTimestamp() {
        return timestamp;
    }
}